package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	    super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
        super.initialize(args);

        freePhysicalPages = new PageQueue();
        for (int i = 0; i < Machine.processor().getNumPhysPages(); i++) {
            freePhysicalPages.push(i);
        }
        //freePhysicalPages.printVisualization();

        console = new SynchConsole(Machine.console());

        Machine.processor().setExceptionHandler(
                new Runnable() {
                    public void run() {
                        exceptionHandler();
                    }
                });
    }

    /**
     * Test the console device.
     */
    public void selfTest() {
        super.selfTest();

        System.out.println("Running LotteryScheduler Tests");
        LotterySchedulerTest.runall();

        /**

        System.out.println("Testing the console device. Typed characters");
        System.out.println("will be echoed until q is typed.");
        char c;
        do {
            c = (char) console.readByte(true);
            console.writeByte(c);
        }
        while (c != 'q');
        System.out.println(""); */
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
        if (!(KThread.currentThread() instanceof UThread))
            return null;

        return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
        Lib.assertTrue(KThread.currentThread() instanceof UThread);

        UserProcess process = ((UThread) KThread.currentThread()).process;
        int cause = Machine.processor().readRegister(Processor.regCause);
        process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
        super.run();
        UserProcess process = UserProcess.newUserProcess();
        String shellProgram = Machine.getShellProgramName();

        /*
         * Program arguments go here.
         * Note that all paths are relative to the test directory.
         */
        String source = "TEST_FILE.txt";
        String dest = "COPY_FILE.txt";
        // Need a dummy to represent the file (C convention)
        String dummy = "Placeholder";

        Lib.assertTrue(process.execute(
                shellProgram, new String[] {dummy, source, dest}));
        KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	    super.terminate();
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;

    // For part 2
    static PageQueue freePhysicalPages;

    static class PageQueue {
        PageQueueNode first;
        PageQueueNode last;
        Lock lock;
        int size;

        PageQueue() {
            first = null;
            last = null;
            lock = new Lock();
            size = 0;
        }

        void push(int page) {
            if (last == null) {
                last = new PageQueueNode(page, null);
                first = last;
            } else {
                last.next = new PageQueueNode(page, null);
                last = last.next;
            }
            size++;
        }

        int pop() {
            int page = first.page;
            first = first.next;
            if (first == null) {
                last = null;
            }
            size--;
            return page;
        }

        public String toString() {
            if (first != null) {
                return "PQ [" + first.toString();
            } else {
                return "PQ []";
            }
        }

        void printVisualization() {
            int numPhysPages = Machine.processor().getNumPhysPages();
            boolean[] physPagesFree = new boolean[numPhysPages];

            for (PageQueueNode node = first;
                 node != null; node = node.next) {
                physPagesFree[node.page] = true;
            }

            String freeVisualization = "FREE PHYS [";
            for (int i = 0; i < numPhysPages; i++) {
                if (physPagesFree[i]) {
                    freeVisualization += "_";
                } else {
                    freeVisualization += " ";
                }
            }
            freeVisualization += ']';
            System.out.println(freeVisualization);
        }

        class PageQueueNode {
            int page;
            PageQueueNode next;

            public String toString() {
                if (next != null) {
                    return page + " " + next.toString();
                } else {
                    return page + "]";
                }
            }

            PageQueueNode(int page, PageQueueNode next) {
                this.page = page;
                this.next = next;
            }
        }

        static void selfTest() {
            /* Checks queueing behavior, not lock behavior */
            PageQueue q = new PageQueue();
            int p;
            String failmsg = "PageQueue selfTest failed";

            // ACQUIRE LOCK

            Lib.assertTrue(q.toString().equals("PQ []"), failmsg);
            Lib.assertTrue(q.size == 0);

            q.push(0);
            Lib.assertTrue(q.toString().equals("PQ [0]"), failmsg);
            Lib.assertTrue(q.size == 1, failmsg);

            q.push(1);
            q.push(2);
            q.push(3);

            Lib.assertTrue(q.toString().equals("PQ [0 1 2 3]"), failmsg);
            Lib.assertTrue(q.size == 4);

            p = q.pop();
            Lib.assertTrue(p == 0, failmsg);
            Lib.assertTrue(q.toString().equals("PQ [1 2 3]"), failmsg);
            Lib.assertTrue(q.size == 3, failmsg);

            p = q.pop();
            Lib.assertTrue(p == 1, failmsg);
            Lib.assertTrue(q.toString().equals("PQ [2 3]"), failmsg);
            Lib.assertTrue(q.size == 2, failmsg);

            q.push(0);
            Lib.assertTrue(q.toString().equals("PQ [2 3 0]"), failmsg);
            Lib.assertTrue(q.size == 3, failmsg);

            q.push(5);
            Lib.assertTrue(q.toString().equals("PQ [2 3 0 5]"), failmsg);
            Lib.assertTrue(q.size == 4, failmsg);

            p = q.pop();
            Lib.assertTrue(p == 2, failmsg);
            Lib.assertTrue(q.toString().equals("PQ [3 0 5]"), failmsg);
            Lib.assertTrue(q.size == 3, failmsg);

            p = q.pop();
            p = q.pop();
            p = q.pop();
            Lib.assertTrue(p == 5, failmsg);
            Lib.assertTrue(q.toString().equals("PQ []"), failmsg);
            Lib.assertTrue(q.size == 0, failmsg);

            q.push(16);
            q.push(1);
            q.push(2);
            q.push(3);

            Lib.assertTrue(q.toString().equals("PQ [16 1 2 3]"), failmsg);
            Lib.assertTrue(q.size == 4);

            System.out.println("\nUserKernel.PageQueue self tests passed");
        }
    }
}
