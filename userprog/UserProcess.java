package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {

    /** A counter that is used to generate unique process IDs. */
    static int IDCOUNTER = 0;

    /** A lock that makes incrementing the counter atomic. */
    static Lock IDCOUNTERLOCK = new Lock();

    /** Unique identification for this process. */
    private int processID;

    /** The parent of this process is identified when EXEC() is called. */
    private UserProcess parent;

    /** A data structure that keeps tracks of its children when this process
     *  calls EXEC(). */
    private ArrayList<UserProcess> childrenProcesses;

    /** List of File Descriptors that OPEN() and CLOSE() utilizes. */
    private HashMap<Integer, OpenFile> openFiles;

    /** A parent stores its child exit status in JOIN() when the child process
     *  is terminated . */
    private HashMap<Integer,Integer> childrenExitStatusTable;

    /** The thread pertaining to this process, since each process has one thread
     *  in Project 2. */
    private UThread MyThread;
    private boolean isJoining;
    private Lock joinLock;
    private Condition2 joinCondition;
    // part 2 visualization
    private boolean[] physPagesUsed;

    /**
     * Allocate a new process.
     */
    public UserProcess() {
        IDCOUNTERLOCK.acquire();
        this.processID = IDCOUNTER;
        IDCOUNTER++;
        IDCOUNTERLOCK.release();

        this.parent = null;
        this.childrenProcesses = new ArrayList();
        this.childrenExitStatusTable = new HashMap();
        isJoining =false;
        

        openFiles = new HashMap<Integer, OpenFile>();
        openFiles.put(new Integer(0), UserKernel.console.openForReading());
        openFiles.put(new Integer(1), UserKernel.console.openForWriting());

        int numPhysPages = Machine.processor().getNumPhysPages();
        physPagesUsed = new boolean[numPhysPages];
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
        {
            System.out.println("ERROR, Couldn't load file name");
            return false;
        }

        /** Since each process has only one thread, create a thread associated
         *  with this process. */
        new UThread(this).setName(name).fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength+1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0)
            {
                Lib.debug(dbgProcess, "Read string "
                        + new String(bytes, 0, length) + " from " + vaddr);
                return new String(bytes, 0, length);
            }
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                                 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0
                && offset + length <= data.length);
        byte[] memory = Machine.processor().getMemory();

        if (length == 0) { return 0; }

        int v_curpn, v_endpn, paddr, firstlength, lastlength;
        int copied = 0;
        TranslationEntry te;

        v_curpn = Processor.pageFromAddress(vaddr);
        v_endpn = Processor.pageFromAddress(vaddr + length - 1);

        if (v_curpn == v_endpn) {
            if (v_curpn >= numPages || v_curpn < 0) { return copied; }
            te = pageTable[v_curpn];
            te.used = true;
            paddr = Processor.makeAddress(
                    te.ppn, Processor.offsetFromAddress(vaddr));

            System.arraycopy(memory, paddr, data, offset, length);
            return length;
        } else {
            if (v_curpn >= numPages || v_curpn < 0) { return copied; }
            te = pageTable[v_curpn];
            te.used = true;
            paddr = Processor.makeAddress(
                    te.ppn, Processor.offsetFromAddress(vaddr));

            firstlength = pageSize - Processor.offsetFromAddress(vaddr);
            System.arraycopy(memory, paddr, data, offset, firstlength);
            copied = firstlength;

            v_curpn++;

            while (v_curpn < v_endpn) {
                if (v_curpn >= numPages || v_curpn < 0) { return copied; }
                te = pageTable[v_curpn];
                te.used = true;
                paddr = Processor.makeAddress(te.ppn, 0);

                System.arraycopy(memory, paddr, data, offset + copied, pageSize);
                copied += pageSize;

                v_curpn++;
            }

            if (v_curpn >= numPages || v_curpn < 0) { return copied; }
            te = pageTable[v_curpn];
            te.used = true;
            paddr = Processor.makeAddress(te.ppn, 0);

            lastlength = (length - firstlength - 1) % pageSize + 1;
            System.arraycopy(memory, paddr, data, offset + copied, lastlength);
            copied += lastlength;
            // assert copied == length
            return copied;
        }
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                                  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0
                && offset + length <= data.length);
        byte[] memory = Machine.processor().getMemory();

        if (length == 0) { return 0; }

        int v_curpn, v_endpn, paddr, firstlength, lastlength;
        int copied = 0;
        TranslationEntry te;

        v_curpn = Processor.pageFromAddress(vaddr);
        v_endpn = Processor.pageFromAddress(vaddr + length - 1);

        if (v_curpn == v_endpn) {
            if (v_curpn >= numPages || v_curpn < 0) { return copied; }
            te = pageTable[v_curpn];
            if (te.readOnly) { return copied; }
            te.used = true;
            te.dirty = true;
            paddr = Processor.makeAddress(
                    te.ppn, Processor.offsetFromAddress(vaddr));

            System.arraycopy(data, offset, memory, paddr, length);
            return length;
        } else {
            if (v_curpn >= numPages || v_curpn < 0) { return copied; }
            te = pageTable[v_curpn];
            if (te.readOnly) { return copied; }
            te.used = true;
            te.dirty = true;
            paddr = Processor.makeAddress(
                    te.ppn, Processor.offsetFromAddress(vaddr));

            firstlength = pageSize - Processor.offsetFromAddress(vaddr);
            System.arraycopy(data, offset, memory, paddr, firstlength);
            copied = firstlength;

            v_curpn++;

            while (v_curpn < v_endpn) {
                if (v_curpn >= numPages || v_curpn < 0) { return copied; }
                te = pageTable[v_curpn];
                if (te.readOnly) { return copied; }
                te.used = true;
                te.dirty = true;
                paddr = Processor.makeAddress(te.ppn, 0);

                System.arraycopy(data, offset + copied, memory, paddr, pageSize);
                copied += pageSize;

                v_curpn++;
            }

            if (v_curpn >= numPages || v_curpn < 0) { return copied; }
            te = pageTable[v_curpn];
            if (te.readOnly) { return copied; }
            te.used = true;
            te.dirty = true;
            paddr = Processor.makeAddress(te.ppn, 0);

            lastlength = (length - firstlength - 1) % pageSize + 1;
            System.arraycopy(data, offset + copied, memory, paddr, lastlength);
            copied += lastlength;
            // assert copied == length
            return copied;
        }
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        }
        catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i=0; i<args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages*pageSize;

        // and finally reserve 1 page for arguments
        numPages++;
        Lib.debug(dbgProcess, "Got "+numPages+" for coff in load()");

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages-1)*pageSize;
        int stringOffset = entryOffset + args.length*4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i=0; i<argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(
                    writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i])
                    == argv[i].length);
            Lib.debug(dbgProcess, "Wrote argument " + i + " ("
                    + new String(argv[i]) + ") to " + stringOffset);
            stringOffset += argv[i].length;
            Lib.assertTrue(
                    writeVirtualMemory(
                            stringOffset,new byte[] {
                            0 })
                            == 1);
            stringOffset += 1;
        }
        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        pageTable = new TranslationEntry[numPages];

        UserKernel.freePhysicalPages.lock.acquire();

        if (numPages > UserKernel.freePhysicalPages.size) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        int vpn = 0;
        int ppn;
        // load sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                ppn = UserKernel.freePhysicalPages.pop();
                physPagesUsed[ppn] = true;
                section.loadPage(i, ppn);

                pageTable[vpn] = new TranslationEntry(
                        vpn, ppn, true, section.isReadOnly(), false, false);

                vpn++;
            }
        }

        // allocate stackPages+1 pages for stack and args
        for (int i = 0; i < stackPages + 1; i++) {
            ppn = UserKernel.freePhysicalPages.pop();
            physPagesUsed[ppn] = true;

            pageTable[vpn] = new TranslationEntry(
                    vpn, ppn, true, false, false, false);

            vpn++;
        }

        //printVirtualMemoryVisualization();
        //UserKernel.freePhysicalPages.printVisualization();

        UserKernel.freePhysicalPages.lock.release();

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        UserKernel.freePhysicalPages.lock.acquire();

        for (int vpn = 0; vpn < numPages; vpn++) {
            int ppn = pageTable[vpn].ppn;
            UserKernel.freePhysicalPages.push(ppn);
        }
        //UserKernel.freePhysicalPages.printVisualization();

        UserKernel.freePhysicalPages.lock.release();
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i=0; i<processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }


    /* File system calls and Halt. These are implemented for Task 1 Project 2 */
    private int handleHalt() {
        if (this.processID != 0) {
            return -1;
        }
        Machine.halt();
        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    /**
     * Handle the handleCreat() system call.
     */
    private int handleCreat(int nameAddress) {
        int oldDescriptorCount = openFiles.size();
        int fileDescriptor = openFiles.size();
        if (fileDescriptor > 15 || nameAddress < 0) {
            return -1;
        }
        String fileName = readVirtualMemoryString(nameAddress, 256);
        Lib.debug(dbgProcess , "Trying to create file " + fileName);
        OpenFile file = ThreadedKernel.fileSystem.open(fileName, false);
        if (file == null) {
            file = ThreadedKernel.fileSystem.open(fileName, true);
            if (file == null) {
                return -1;
            }
        }
        Lib.assertTrue(openFiles.put(new Integer(fileDescriptor), file) == null,
                "ERROR: Tried to store a file descriptor where one already exists in handleCreat()");
        Lib.assertTrue(oldDescriptorCount == (openFiles.size() - 1));
        Lib.debug(dbgProcess, "Created file " + fileName + " with descriptor " + fileDescriptor);
        return fileDescriptor;
    }

    /**
     * Handle the handleOpen() system call.
     */
    private int handleOpen(int nameAddress) {
        int oldDescriptorCount = openFiles.size();
        int fileDescriptor = openFiles.size();
        if (fileDescriptor > 15 || nameAddress < 0) {
            return -1;
        }
        String fileName = readVirtualMemoryString(nameAddress, 256);
        Lib.debug(dbgProcess , "Trying to open file " + fileName);
        OpenFile file = ThreadedKernel.fileSystem.open(fileName, false);
        if (file == null) {
            return -1;
        }

        Lib.assertTrue(openFiles.put(new Integer(fileDescriptor), file) == null,
                "ERROR: Tried to store a file descriptor where one already exists in handleCreat()");
        Lib.assertTrue(oldDescriptorCount == (openFiles.size() - 1));
        Lib.debug(dbgProcess, "Opened file " + fileName + " with descriptor " + fileDescriptor);
        return fileDescriptor;
    }

    /**
     * Handle the handleRead() system call.
     */
    private int handleRead(int fileDescriptor, int bufferAddress, int count) {
        Lib.debug(dbgProcess,"PID: "+ processID + " Trying to read from " + fileDescriptor);
        OpenFile file = openFiles.get(fileDescriptor);
        if( file == null || count < 0)
            return -1;
        byte[] bufferData = new byte[count];
        int amountRead = file.read(bufferData, 0, count);
        if (amountRead == -1 || amountRead == 0) {
            return amountRead;
        }

        int amountWrittenToBuffer = writeVirtualMemory(bufferAddress, bufferData);
        return amountRead;
    }

    /**
     * Handle the handleWrite() system call.
     */
    private int handleWrite(int fileDescriptor, int bufferAddress, int count) {
        OpenFile file = openFiles.get(fileDescriptor);
        if( file == null || count < 0)
            return -1;
        byte[] bufferData = new byte[count];
        readVirtualMemory(bufferAddress, bufferData);
        Lib.debug(dbgProcess, "PID: " +processID + " handling write data: "
                + (new String(bufferData)) + " to descriptor " + fileDescriptor);
        int amountWrittenToFile = file.write(bufferData, 0, count);
        if (amountWrittenToFile < count) {
            return -1;
        }
        return amountWrittenToFile;
    }

    /**
     * Handler for the handleUnlink exception.
     *
     * @param addressOfName virtual address for where to find the file name string we're unlinkin.
     *
     * @return 0 if file is unlinked successfully or -1 if there is an error.
     */
    private int handleUnlink(int addressOfName)
    {
        Lib.debug(dbgProcess,"PID: "+ processID + " Trying to unlink address " + addressOfName);
        int num_virtual_pages;
        if(addressOfName == 0 || addressOfName < 0)
            return -1;
        FileSystem fs = ThreadedKernel.fileSystem;
        String fileName = readVirtualMemoryString(addressOfName, 256);
        if( fileName == null)
            return -1;
        if( fs.remove(fileName) )
            return 0;
        return -1;
    }

    /**
     * Handler for the handleClose exception.
     *
     * @param descriptor File descriptor integer for the open file handle. Valid range = [0, 15]
     *
     * @return 0 if file is closed successfully or -1 if there is an error.
     */
    private int handleClose(int descriptor)
    {
        Lib.debug(dbgProcess, processID + " Closing descriptor " + descriptor);
        if(descriptor > 15 || descriptor < 0 || (openFiles.get(new Integer(descriptor))) == null)
            return -1;
        OpenFile file = (OpenFile)openFiles.remove(descriptor);
        if(file == null)
            return -1;
        file.close();
        Lib.debug(dbgProcess, "Close was successful for descriptor "
                + descriptor + " Remaining open descriptors: " + openFiles.size());
        return 0;
    }


    /* End of file system calls and Task 1, Project 2 */

    /** Handles the EXIT() system call. Calling this handler should terminate
     *  the process and clean up all parts of the process when terminated (such
     *  as releasing memory, close open files, etc.).
     *  @param status : the status of this process that is passed to this
     *      process's parent. */
    private void handleExit(int status) {
        if (this.parent != null) {
            this.parent.childrenExitStatusTable.put(this.processID, status);
            if (this.isJoining){
            	this.joinLock.acquire();
            	this.joinCondition.wake();
            	this.joinLock.release();
            }
        }

        /* Issue: File descriptors in use are not always contiguous.
        for (int i=0;i<openFiles.size();i++){
            handleClose(i);
        } /**/

        /* This is a replacement for the for loop above */
        HashMap<Integer, OpenFile> copyMap =
                (HashMap<Integer, OpenFile>) openFiles.clone();
        for (Object i : copyMap.keySet()){
            handleClose(((Integer) i).intValue());
        }
        
        this.unloadSections();
        coff.close();
        if (!childrenProcesses.isEmpty()) {
            for (int i = 0; i < childrenProcesses.size(); i++) {
                childrenProcesses.get(i).parent = null;
            }
            childrenProcesses.clear();
        }
        if (this.processID == 0){
            Kernel.kernel.terminate();
        } else {
            KThread.finish();
        }
    }

    /** Handles the JOIN() system call. It sleeps/pauses this process and runs
     *  the child process whose ID is associated to the argument ID. This
     *  process resumes once its child is finished and joins with the child.
     *  @param : the ID of the child process that this process is suppose to
     *      join with.
     *  @param statusVMemAddr : the virtual memory address where the parent
     *      writes the status of its child exit status. It should be unsigned.
     *  @return : -1 if the process does not have a child with the process ID
     *      supplied in the argument. 1 if the child process exits normally.
     *      0 if the child process exits due to an exception. */
    private int handleJoin(int ID, int statusVMemAddr) {
        Lib.debug(dbgProcess, "handling Join()");
        UserProcess currentChild = null;
        if (!(childrenProcesses.isEmpty())) {
            for (int i = 0; i < childrenProcesses.size(); i++) {
                if (childrenProcesses.get(i).processID == ID) {
                    currentChild = childrenProcesses.get(i);
                    break;
                }
            }
        }
        if (currentChild == null) {
            return -1;
        }
        this.joinLock=new Lock();
       	this.joinCondition=new Condition2(joinLock);
       	currentChild.joinLock=this.joinLock;
       	currentChild.joinCondition=this.joinCondition;
       	currentChild.isJoining=true;
       	this.joinLock.acquire();
       	this.joinCondition.sleep();
       	this.joinLock.release();
        int childStatus =
                this.childrenExitStatusTable.get(currentChild.processID);
        boolean hasKey=childrenExitStatusTable.containsKey(currentChild.processID);
        this.childrenProcesses.remove(currentChild);
        this.childrenExitStatusTable.remove(currentChild.processID);
        currentChild.parent = null;

        if (hasKey) {
            byte[] statusByte;
            statusByte = Lib.bytesFromInt(childStatus);
            int bytesWrote = writeVirtualMemory(statusVMemAddr, statusByte);
            if (bytesWrote != 4) {
                return -1;
            }
            return 1;
        }
        return 0;
    }

    /** Handles the EXEC() system call. This process initializes the child
     *  process and passes the file to that child process along with the
     *  arguments for that file.
     *  @param pointerToFile : the virtual memory address of the file. It should
     *      be unsigned.
     *  @param argc : the amount of arguments supplied to the file. It should be
     *      unsigned.
     *  @param pointerToArgv : the virtual memory of the arguments supplied to
     *      file. It should be unsigned. Each argument is a byte.
     *  @return : -1 if HANDLEEXEC() fails. PROCESSID of the child process if
     *  HANDLEEXEC() succeeds. */
    private int handleExec(int pointerToFile, int argc, int pointerToArgv) {
        Lib.debug(dbgProcess, "In handleExec. Arg pointer given as "
                + pointerToArgv + " count " + argc);
        if (pointerToFile < 0 || pointerToArgv < 0 || argc < 0 || argc > 4) {
            return -1;
        }
        String FileToRun = readVirtualMemoryString(pointerToFile, 256);
        if (FileToRun == null) {
        	
            return -1;
        }
        if (!FileToRun.endsWith(".coff")) {
            return -1;
        }
        String[] programArgs = new String[argc];
        byte[] ArgumentAddress = new byte[4];
        int bytesRead;
        for (int i = 0; i < argc; i++) {
            bytesRead = readVirtualMemory(
                    pointerToArgv + (i * 4), ArgumentAddress);
            if (bytesRead != 4) {
                return -1;
            }
            
            programArgs[i] = readVirtualMemoryString(
                    Lib.bytesToInt(ArgumentAddress,0), 256);
            Lib.debug(dbgProcess, "Read in argument " + i + " of "
                    + programArgs[i]);
        }
        UserProcess newChild = newUserProcess();
        if (newChild.execute(FileToRun, programArgs)) {
            newChild.parent = this;
            this.childrenProcesses.add(newChild);
            Lib.debug(dbgProcess, "New process successfully forked");
            return newChild.processID;
        } else {
            return -1;
        }
    }
    /* End of file system calls for Task 3, Project 2 */

    private static final int
            syscallHalt = 0,
            syscallExit = 1,
            syscallExec = 2,
            syscallJoin = 3,
            syscallCreate = 4,
            syscallOpen = 5,
            syscallRead = 6,
            syscallWrite = 7,
            syscallClose = 8,
            syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     *
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallExit:
                handleExit(a0);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallOpen:
                return handleOpen(a0);
            case syscallCreate:
                return handleCreat(a0);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            case syscallClose:
                return handleClose(a0);
            case syscallUnlink:
                return handleUnlink(a0);
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(
                        processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0),
                        processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2),
                        processor.readRegister(Processor.regA3)
                );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            default:
                handleExit(-1);
                Lib.debug(dbgProcess, "Unexpected exception: " +
                        Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    private void printVirtualMemoryVisualization() {
        String usedVisualization = "[";
        for (boolean p : physPagesUsed) {
            if (p) {
                usedVisualization += "x";
            } else {
                usedVisualization += " ";
            }
        }
        usedVisualization += "]";
        System.out.printf("I USE: %2d %s\n", numPages, usedVisualization);
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
