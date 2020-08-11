package nachos.threads;
import nachos.machine.*;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param        transferPriority        <tt>true</tt> if this queue should
     *                                        transfer priority from waiting threads
     *                                        to the owning thread.
     * @return        a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        if( enableAsserts && showWarn) {
            System.out.println("\n********** WARNING: Asserts are enabled in PriorityScheduler ******************\n");
            showWarn = false; 
        }
        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(priority >= priorityMinimum &&
                   priority <= priorityMaximum);
        
        ThreadState ts = getThreadState(thread);
        int oldPriority = ts.getEffectivePriority();
        if(priority == ts.getPriority()) { return; }
        ts.setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();
        KThread thread = KThread.currentThread();
        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority+1);
        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();
                       
        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param        thread        the thread whose scheduling state to return.
     * @return        the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);
        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {

        protected LinkedList<ThreadState>[] arrayOfQueues; // An array of queues of different priorities

        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
            arrayOfQueues = new LinkedList[priorityMaximum-priorityMinimum+1];
            for(int i=priorityMinimum; i<=priorityMaximum; i++)
                arrayOfQueues[i] = new LinkedList<ThreadState>();
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        /** Dump contents of this queue */
       @Override
       public void print() {
            String p0String, p1String, p2String, p4String, p3String, p5String, p6String, p7String;
            p1String = p0String = p2String = p3String = p4String = p5String = p6String = p7String = "";
               
            for(ThreadState ts : arrayOfQueues[0])
                p0String += "-->"+ts.thread+"("+ts.getPriority()+"/"+ts.getEffectivePriority()+")"; 
            for(ThreadState ts : arrayOfQueues[1])
                p1String += "-->"+ts.thread+"("+ts.getPriority()+"/"+ts.getEffectivePriority()+")"; 
            for(ThreadState ts : arrayOfQueues[2])
                p2String += "-->"+ts.thread+"("+ts.getPriority()+"/"+ts.getEffectivePriority()+")"; 
            for(ThreadState ts : arrayOfQueues[3])
                p3String += "-->"+ts.thread+ "("+ts.getPriority()+"/"+ts.getEffectivePriority()+")"; 
            for(ThreadState ts : arrayOfQueues[4])
                p4String += "-->"+ts.thread+ "("+ts.getPriority()+"/"+ts.getEffectivePriority()+")"; 
            for(ThreadState ts : arrayOfQueues[5])
                p5String += "-->"+ts.thread+ "("+ts.getPriority()+"/"+ts.getEffectivePriority()+")"; 
            for(ThreadState ts : arrayOfQueues[6])
                p6String += "-->"+ts.thread+ "("+ts.getPriority()+"/"+ts.getEffectivePriority()+")"; 
            for(ThreadState ts : arrayOfQueues[7])
                p7String += "-->"+ts.thread+ "("+ts.getPriority()+"/"+ts.getEffectivePriority()+")"; 

            String dumpString = "\n-Queue: "+queueID+"-";
        
            if(queueID == KThread.getReadyQueueID())
                dumpString += "(READY QUEUE)";
            dumpString += "-------------------\n";
            dumpString += "transferPriority? "+transferPriority+" "; 
            if(resourceHolder != null)            
                dumpString += "resourceHolder: "+resourceHolder.thread;
            else
                dumpString += "resourceHolder: NULL"; 

            dumpString += " Next thread out will be: ";
                if(pickNextThread() != null)
                    dumpString += ""+pickNextThread().thread+"("+pickNextThread().getPriority()+"/"
                        +pickNextThread().getEffectivePriority()+")"; 
                else
                    dumpString += " the NULL thread";
                dumpString += ""+"\nP0:"+p0String+"\nP1: "+p1String+"\nP2: "+p2String+"\nP3: "+p3String+"\nP4: "
                +p4String+"\nP5: "+p5String+"\nP6: "+p6String+"\nP7: "+p7String+"\n----------------------------";
                
                Lib.debug(dbgPSched, dumpString);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            resourceHolder = getThreadState(thread); 
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            int i = priorityMaximum;

            /* First index to the highest priority non-empty queue */
            ThreadState outThread = null;
            ThreadState sanityThread = pickNextThread();
            while(arrayOfQueues[i].size() == 0 && i != priorityMinimum) { i--; }
            
            /* Next, some consistency checks on the thread we're returning. If there are bugs, this is a good place to catch them */
            {

            /* If all queues in this PQ are empty, pickNextThread better be NULL */
            if(!(arrayOfQueues[i].size() > 0))
                Lib.assertTrue(sanityThread == null);
            
            if(arrayOfQueues[i].size() > 0)
            {
                /* We better not have received a donation from threads on this queue if transferPriority is false */
                if(!transferPriority) 
                    for(ThreadState.DonationTracker dt : sanityThread.donationManagementDB)
                        Lib.assertTrue(dt.queueDonorCameFrom != this, "ERROR: "+sanityThread.thread+" received a donation"
                            +" (or thinks it did) on a queue ("+queueID+") in which tranosferPriority is "+transferPriority);

                /* Ensure the next thread's donation tracking data makes sense */
                if(sanityThread.currentBestOffer == INVALID_EFFECTIVE_PRIORITY)
                    Lib.assertTrue(sanityThread.donationManagementDB.size() == 0 && sanityThread.currentBestDonor == null,
                        "ERROR: nextThread "+sanityThread.thread+" has inconsistent databases");
                if(sanityThread.donationManagementDB.size() == 0) 
                    Lib.assertTrue(sanityThread.currentBestOffer == INVALID_EFFECTIVE_PRIORITY && sanityThread.currentBestDonor==null,
                        "ERROR: nextThread "+sanityThread.thread+" has inconsistency databases");
                if(sanityThread.currentBestDonor == null)
                    Lib.assertTrue(sanityThread.currentBestOffer == INVALID_EFFECTIVE_PRIORITY &&
                        sanityThread.donationManagementDB.size() == 0, "ERROR: nextThread "+sanityThread.thread+" data inconsistent");
                Lib.assertTrue(sanityThread.internalChecks(), "ERROR: " +sanityThread.thread + " failed internalChecks() in nextThread");

                /* Ensure every thread this thread has a donation from knows about it and check transferPriority consistency for them */
                for(ThreadState.DonationTracker dt : sanityThread.donationManagementDB) {
                    Lib.assertTrue(dt.donor.threadsDonatedTo.contains(sanityThread), "ERROR sanity check for "+sanityThread+" FAILED"
                        +". Donor: "+dt.donor.thread+" doesn't know it's a donor");
                    Lib.assertTrue(dt.queueDonorCameFrom.transferPriority, "ERROR sanity check for "+sanityThread+" FAILED"
                        +". Received donation from queue "+dt.queueDonorCameFrom.queueID+" but transferPriority is false");
                }

                /* Ensure sanityThread came from a queue of the right effective priority */
                Lib.assertTrue(sanityThread.getEffectivePriority() == i, "ERROR: "+sanityThread+" has an EP of "+
                    sanityThread.getEffectivePriority()+" but nextThread grabbed it from queue of priority "+i+
                     "for queueID: "+queueID);

                /* Ensure all donations match getEffectivePriority() for this thread */
                for(ThreadState ts : sanityThread.threadsDonatedTo) {
                    for(ThreadState.DonationTracker dt : ts.donationManagementDB) {
                        if(dt.donor == sanityThread)
                            Lib.assertTrue(dt.donation == sanityThread.getEffectivePriority());
                    }
                }

            }


            } /* END OF CONSISTENCY CHECKING FOR THE NEXT THREAD */
           
            if(arrayOfQueues[i].size() > 0) 
            {
                /* First duplicate donationManagementDB, since we'll be iterating over it while potentially changing its size */
                HashSet<ThreadState.DonationTracker> dummyTracker = new HashSet<ThreadState.DonationTracker>();
                
                if(resourceHolder != null && resourceHolder != sanityThread)
                {
                    resourceHolder.internalChecks();

                    for(ThreadState.DonationTracker dt : resourceHolder.donationManagementDB)
                        dummyTracker.add(dt);
               
                    Lib.debug(dbgPSched, "[ nextThread ]: "+resourceHolder.thread+" is leaving "+queueID+". Trying to revoke"
                        +" priority donation made by "+resourceHolder.donationManagementDB.size()+" donors"); 

                    /*
                     * For every thread that has donated to the current resourceHolder of this queue, if this was the 
                     * last (or only) resource the donor was waiting on resourceHolder for, then revoke the donation. 
                     * Otherwise, leave it unchanged since it must be the case that the donor is still waiting for  
                     * resourceHolder on some other queue. 
                     */
                    for(ThreadState.DonationTracker donor : dummyTracker)
                    {
                        boolean okayToRevoke = true;
                        for(PriorityQueue queue : donor.donor.queuesThisThreadIsOn)
                            okayToRevoke = okayToRevoke && (queue.resourceHolder != resourceHolder || queue == this);

                        if(okayToRevoke)
                        {
                            Lib.debug(dbgPSched, "[ nextThread]: "+donor.donor.thread+" is not waiting on any other"
                                + " resources held by "+resourceHolder.thread+" so revoking donation");
                            resourceHolder.revokeDonation(donor.donor);
                        }
                        else
                            Lib.debug(dbgPSched, "[ nextThread ]: Tried to revoke priority for next thread "+resourceHolder.thread
                                +" but "+donor.donor.thread+" is still waiting for it somewhere");
                    }
                }
                outThread = arrayOfQueues[i].removeFirst();
                Lib.assertTrue(sanityThread == outThread);

                /* outThread has been chosen from this queue so remove this queue from outThread's database of queues that it's on */
                outThread.deleteQueueFromThreadDB(this);
                String dumpString = "";

                /* More debugging output, ignore this */
                {
                    dumpString += "[ nextThread ]: Returning " + outThread.thread+" ("+outThread.getPriority()
                        +"/"+outThread.getEffectivePriority()+")" + " on queue " + queueID;
                    if(queueID == KThread.getReadyQueueID()) { dumpString += " (READY QUEUE) "; }
                    Lib.debug(dbgPSched, dumpString);
                }
                /* End of ignoring this */

                ThreadState oldResourceHolder = resourceHolder;
                resourceHolder = outThread;
            
                // Now that the resourceHolder has been updated, notify the remaining threads waiting for this resource
                // so that they may make a priority donation to the new resourceHolder if appropriate.
                for(ThreadState.DonationTracker oldDonor : dummyTracker)
                    if(oldDonor.donor.queuesThisThreadIsOn.contains(this)) {
                        Lib.assertTrue(arrayOfQueues[oldDonor.donor.getEffectivePriority()].contains(oldDonor.donor),
                            "ERROR: "+oldDonor.donor.thread+" thinks its on "+queueID+" but the queue thinks differently");
                        oldDonor.donor.checkIfDonationRequired(this);
                    }
                return outThread.thread;
            }
            Lib.debug(dbgPSched, "[ PriorityQueue.nextThread]: Asked for nextThread on " + queueID + 
                " but thread queue was empty, returning NULL");
            resourceHolder = null;
            return null;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return        the next thread that <tt>nextThread()</tt> would
         *                return.
         */
        protected ThreadState pickNextThread() {
            int i = priorityMaximum;
            while(arrayOfQueues[i].size() == 0 && i != priorityMinimum) { i--; }
            return arrayOfQueues[i].peek(); 
        }
        
        /* PriorityQueue configuration parameters */
        public boolean transferPriority;
        private ThreadState resourceHolder;
        protected final int queueID = hashCode()%10000;

    } // End of PriorityQueue class

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see        nachos.threads.KThread#schedulingState
     */
    protected class ThreadState { 
        /**
         * DonationTracker class maintains an association of threads that have donated
         * priority and which queues the donee owned when the donation was made
         * 
         * Equality Symantics: object1.equals(object2) iff object1.donor == object2.donor
         * hashCode Symantics (used in HashSet for membership testing): object1.hashCode()
         * == object2.hashCode() iff object1.donor.hashCode() == object2.donor.hashCode(). 
         */
        protected class DonationTracker
        {
            /* Class properties */
            ThreadState donor;
            PriorityQueue queueDonorCameFrom;
            int donation;

            DonationTracker(int inOffer, ThreadState inDonor, PriorityQueue inQueue)
            {
                donor = inDonor;
                queueDonorCameFrom = inQueue;
                donation = inOffer;
            }
            @Override
            public int hashCode() {
                return donor.hashCode();
            }
            @Override
            public boolean equals(Object inObject) {
                return donor == ((DonationTracker)inObject).donor;
            }
        }

        /** Perform database consistency checks for this ThreadState and it's associated ThreadStates */
        protected boolean internalChecks()
        {
            boolean outValue = true;
            boolean subValue = false; 

            /* Make sure currentBestDonor is actually in the donationManagementDB datbase (i.e., the cache is still valid) */
            if(currentBestDonor != null)
                outValue = outValue && donationManagementDB.contains(new DonationTracker(0,currentBestDonor, null));
            if(!outValue)
                System.out.println("ERROR: currentBestDonor is not in donationManagementDB for"+thread);
    
            /* Ensure currentBestOffer matches currentBestDonor.getEffectivePriority() */
            if(currentBestDonor != null)
                outValue = outValue && (currentBestOffer == currentBestDonor.getEffectivePriority());
            if(!outValue)
                System.out.println("ERROR: currentBestOffer doesn't match currenBestDonor's EP");
               
            // Ensure all outstanding priority donations are to threads we're still waiting for resources from
            // and that the donation matches getEffectivePriority() for this thread
            for(ThreadState ts : threadsDonatedTo)
                for(PriorityQueue queue : queuesThisThreadIsOn) {
                    subValue = subValue || (queue.resourceHolder == ts);
               
                    /* Find the donation we made and make sure it matches the current effective priority of this thread */ 
                    if(queue.resourceHolder == ts)
                        for(DonationTracker dt : ts.donationManagementDB)
                            if(dt.donor == this)
                                subValue = subValue && dt.donation == getEffectivePriority();
                }

            if(threadsDonatedTo.size() > 0)
                outValue = outValue && subValue;

            if(!outValue && threadsDonatedTo.size() > 0)
            {
                System.out.println("ERROR: "+thread+" has an outstanding donation to a thread it is not waiting for."
                    +" Use -p to see threadDump()");
                threadDump();
            }
            return outValue;
        }

        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param        thread        the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
            donationManagementDB = new HashSet<DonationTracker>();
            threadsDonatedTo = new HashSet<ThreadState>();
            queuesThisThreadIsOn = new HashSet<PriorityQueue>();
            currentBestOffer = INVALID_EFFECTIVE_PRIORITY;
            currentBestDonor = null;
            setPriority(priorityDefault);
        }

        /** Dump useful information about this ThreadState to the debug console (visible with -p debug flag) */ 
        public void threadDump()
        {
            String outString = "";
            String outString2 = "";
            String donatedString = "";
            int i = 0; 
            for(DonationTracker td : donationManagementDB) {
                i++;
                outString += "\n\t"+ i + ") Donor: " + td.donor.thread + "(EP: "+td.donor.getEffectivePriority()+")"
                    + ", Donation: " + td.donation + ", QueueID: " + td.queueDonorCameFrom.queueID;
            }
            if(i == 0) { outString += "NONE"; }
            i = 0; 
            for(PriorityQueue queue : queuesThisThreadIsOn) {
                i++;
                outString2 += ""+ i + ") QueueID: " + queue.queueID; 
                if( queue.queueID == KThread.getReadyQueueID()) { outString2 += " (READY QUEUE) ";  }
                if(queue != null)  
                {
                    outString2 += " resourceHolder: ";
                    if(queue.resourceHolder != null)
                        outString2 += queue.resourceHolder.thread;
                    else
                        outString2 += "NONE";
                }

                if(i % 4 == 0)
                    outString2 += "\n";
            }
            if(i == 0) { outString2 += "NONE"; }
            i=0;
            for(ThreadState ts : threadsDonatedTo) {
                i++;
                donatedString += i+") "+ts.thread +". ";
            }
            if(i==0) { donatedString += "NONE"; }


            Lib.debug(dbgPSched, "[ ThreadDump ]: ----------------------------------------------------------");
            Lib.debug(dbgPSched, "Thread: " + thread + ", P/EP " + getPriority() + "/" + getEffectivePriority());
            Lib.debug(dbgPSched, "donationManagementDB: " + outString);  
            Lib.debug(dbgPSched, "queuesThisThreadIsOn: "  + outString2);
            Lib.debug(dbgPSched, "threadsDonatedTo: "+donatedString);
            String dumpString = "Best offer: "+currentBestOffer+". Best donor: ";
            if(currentBestDonor != null) { 
                dumpString += currentBestDonor.thread+"("+currentBestDonor.getPriority()
                +"/"+currentBestDonor.getEffectivePriority()+")";
            }
            else
                dumpString += "NULL";
            Lib.debug(dbgPSched, dumpString+"\n----------------------------------------------");
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return        the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /* Revoke a donation from donor to this thread */
        public void revokeDonation(ThreadState donor) {
            Lib.debug(dbgPSched, "[ ThreadState.revokeDonation ]: Revoking donation to "+thread+" from "+donor.thread+
                " here's the thread dump before revokation"); 
            threadDump();
            
            /* First remove this donor from the database of donors */
            Lib.assertTrue(donationManagementDB.remove(new DonationTracker(0, donor, null)));

            /* Next remove this thread from the donor's database of threadsDonatedTo */
            Lib.assertTrue(donor.threadsDonatedTo.remove(this));

            /* If this was the current best donor, we need to recalculate effective priority */
            if( donor == currentBestDonor)  
                calculatePriorityDonation();
            else
            {
                Lib.debug(dbgPSched, "[ revokeDonation ]: A donation was revoked but no change in EP, here's a thread dump");
                threadDump();
            }
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return        the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            return Math.max(priority, currentBestOffer);
        }

        /**
         * Set the priority of the associated thread to the specified value.
         * This method must also ensure that if the new priority value alters this thread's effective 
         * priority that that change is propagated to all threads that have valid donations from this
         * thread via the propagatePriorityDonation() method. 
         *
         * @param        priority        the new priority.
         */
        public void setPriority(int inPriority) {
            Lib.assertTrue(Machine.interrupt().disabled());
            Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum, "Priority out of range");
            
            if (this.priority == inPriority)
                return;
            int currentPriority = getPriority();
            int currentEP = getEffectivePriority();
            priority = inPriority;
            
            if( currentEP != getEffectivePriority())
            {
                for(PriorityQueue queue : queuesThisThreadIsOn)
                {
                    // Make sure queues we're changing are actually changing 
                    // (i.e., if we remove a thread from a queue, it better have contained it originally and vice versa
                    Lib.assertTrue(queue.arrayOfQueues[currentEP].remove(this));
                    Lib.assertTrue(!queue.arrayOfQueues[getEffectivePriority()].remove(this));
                    queue.arrayOfQueues[getEffectivePriority()].add(this);
                } 
                propagatePriorityDonation();
            }
        }

        /**
         * receiveOffer() is responsible for processing all priority donations received by this thread. 
         * If the donated priority doesn't change this threads effective priority, just add the donation
         * to the database of donations received. If it does, then update effective priority 
         * via calculatePriorityDonation()
         */
        private void receiveOffer(int offer, ThreadState donor, PriorityQueue waitQueue) 
        {
            Lib.assertTrue(Machine.interrupt().disabled());
            Lib.assertTrue(waitQueue.transferPriority); 

            /* 
             * Create a new DonationTracker object to test membership in donationManagementDB database
             * Note that membership testing for the DonationTracker class is done based only on the
             * ThreadState 'donor'. See DonationTracker class for more details on symantics. 
             */
            DonationTracker donationObject = new DonationTracker(offer, donor, waitQueue);
            if(donationManagementDB.contains(donationObject))
                donationManagementDB.remove(donationObject);
            donationManagementDB.add(donationObject);
       
            if(offer > currentBestOffer || (donor.equals(currentBestDonor) && offer < currentBestOffer))   
                calculatePriorityDonation();
        }
  
        /**
         * calculatePriorityDonation() is responsible for computing effective priority of this thread.
         * This method will take the maximum value of this.priority and all priority donations received 
         * from other threads but asking all of those threads what their effective priorities are. 
         * This value is then stored as currentBestOffer for the highest donor currentBestDonor. This
         * caching is aimed at preventing needless (and time-consuming) recomputation.
         * 
         * If, as a result of this computation, this thread's effective priority changes, this method
         * calls propagatePriorityDonation() to notify all threads this thread has donated priority to
         * in the past that its effective priority has changed. 
         */
        private void calculatePriorityDonation()
        {
            Lib.assertTrue(Machine.interrupt().disabled());
           
            /* First we invalidate currentBestOffer */ 
            int oldEffectivePriority = getEffectivePriority(); 
            int bestOfferSeenSoFar = INVALID_EFFECTIVE_PRIORITY;
            ThreadState bestDonorSeenSoFar = null;            
        
            /* Find the best donor out of all donors in donationManagementDB database */    
            for(ThreadState.DonationTracker ts : donationManagementDB)
            {   
                /* Data consistency check. Donor thread better know it's still a donor thread */
                if(!ts.donor.threadsDonatedTo.contains(this)) {
                    System.out.println("ERROR: Donor/donnee data inconsistent:"
                    +" Donor: "+ts.donor.thread+" Donee: "+thread+". Enable debug output (-p) to see threadDump()s"); 
                    ts.donor.threadDump();
                    threadDump();
                    Lib.assertTrue(false);
                }                

                if(bestOfferSeenSoFar < ts.donation)
                {
                    bestOfferSeenSoFar = ts.donation;
                    bestDonorSeenSoFar = ts.donor; 
                }
            }
                
            /* This is just for debugging, you can ignore this stuff---------------- */ 
            if(bestOfferSeenSoFar != INVALID_EFFECTIVE_PRIORITY) {
                Lib.assertTrue(bestDonorSeenSoFar != null);
                Lib.debug(dbgPSched, "[ ThreadState.calculatePriorityDonation ]: Best offer found: " +
                    bestOfferSeenSoFar + ". Donor: " + bestDonorSeenSoFar.thread + ". To: " + thread 
                    + "(" + getPriority()+")");
            }
            else
                Lib.debug(dbgPSched, "[ ThreadState.calculatePriorityDonation ]: No valid donors found. Resetting EP"); 
            /* Okay, now start paying attention again-------------------------------- */
            
            currentBestOffer = bestOfferSeenSoFar;
            currentBestDonor = bestDonorSeenSoFar;

            /* If the thread's effective priority has changed, move the thread on all queues it's sitting on */
            if(oldEffectivePriority != getEffectivePriority())
            {
                for(PriorityQueue queue : queuesThisThreadIsOn)
                {
                    Lib.assertTrue(queue.arrayOfQueues[oldEffectivePriority].contains(this), "ERROR: "+
                        thread + " thinks it's on " + queue.queueID + " at priority " + oldEffectivePriority
                        + " but the queue feels differently");
                    queue.arrayOfQueues[oldEffectivePriority].remove(this);
                    queue.arrayOfQueues[getEffectivePriority()].add(this);
                }
                propagatePriorityDonation();

                /* More debugging */
                Lib.debug(dbgPSched, ""+ thread + " has been moved on it's queues. Here are the "+queuesThisThreadIsOn.size()
                    +" queues this thread thinks it's on...");
                for(PriorityQueue queue : queuesThisThreadIsOn)
                    queue.print();  
                /* END debugging */      

            }
            Lib.debug(dbgPSched, "[ calculatePriorityDonation ]: Thread may have been modified, here't the thread dump");
            threadDump();
        }
        
        /**
         * Remove waitQueue from this thread's queuesThisThreadIsOn database. This method should be called
         * whenever this thread is pulled from the waitQueue (e.g., as a result of a call to nextThread()
         */
        protected void deleteQueueFromThreadDB(ThreadQueue waitQueue) {
            if(!queuesThisThreadIsOn.remove(waitQueue))
            {
                Lib.assertTrue(false, "ERROR: Tried to remove " + ((PriorityQueue)waitQueue).queueID
                + " from thread " + thread + " but queuesThisThreadIsOn has no entry of this waitQueue. Here's a thread dump...");
                threadDump();
            }   
        }


        /**
         * Send an updated priority donation offer to all threads this thread has sent an offer to previously.
         * This method should be called whenever this thread's effective priority changes (e.g., in the
         * event this thread receives a better donation offer or it's setPriority() method is called with
         * a new value that changes its effective priority. 
         */
        private void propagatePriorityDonation()
        {
            Lib.assertTrue(Machine.interrupt().disabled());

            for(PriorityQueue queue : queuesThisThreadIsOn) {
                for(ThreadState ts : threadsDonatedTo) {
                    Lib.assertTrue(ts != null, "ERROR: threadsDonatedTo has an invalid or deleted ThreadState for " + thread);

                    /*
                     * For each queue this thread is still on, check if any threads that this thread
                     * has ever donated to are the resource holder of the queue (since this would 
                     * mean this thread is also still waiting for access to the queue) 
                     */
                    if(queue.transferPriority && queue.resourceHolder == ts) 
                        ts.receiveOffer(getEffectivePriority(), this, queue);
                }
            }
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param        waitQueue        the queue that the associated thread is
         *                                now waiting on.
         *
         * @see        nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
            Lib.assertTrue(Machine.interrupt().disabled());

            String dumpString = "[ waitForAccess ]: " + thread + " waiting on " + waitQueue.queueID;
            if(waitQueue.queueID == KThread.getReadyQueueID()) { dumpString += " (READY QUEUE) "; }
            Lib.debug(dbgPSched, dumpString);
          
            /* waitQueue should not already contain this thread, so assert that */ 
            Lib.assertTrue(!queuesThisThreadIsOn.contains(waitQueue), "ERROR: tried to waitForAccess on a queue"
                +"("+waitQueue.queueID+") that this thread ("+thread+") is already on");

            queuesThisThreadIsOn.add(waitQueue);
            checkIfDonationRequired(waitQueue);        

            /* Add this thread to the waitQueue at (effective) priority of thread.*/
            if(!waitQueue.arrayOfQueues[getEffectivePriority()].add(this))
            {
                Lib.debug(dbgPSched, "ERROR: Tried to add a thread to a queue in which it already existed. Enable debug to see dump");
                waitQueue.print();
                threadDump();
                Lib.assertTrue(false, "ERROR: Failed consistency check");
            }

            /* Dump debugging output */
            Lib.debug(dbgPSched, "[ waitForAccess ]: Dumping waitQueue and threadDump()");
            waitQueue.print();
            threadDump();

        }

        /**
         * Figure out whether or not to make a priority donation to the current resdource holder of the wait queue.
         * and if so, make it.
         */
        private void checkIfDonationRequired(PriorityQueue waitQueue)
        {
            Lib.assertTrue(Machine.interrupt().disabled());
            if( waitQueue.resourceHolder != null && waitQueue.resourceHolder != this && waitQueue.transferPriority) {
                Lib.debug(dbgPSched, "[ ThreadState.waitForAccess ]: " + thread + " sending donation to " 
                    + waitQueue.resourceHolder.thread + " of " + getEffectivePriority() + " on queue " + waitQueue.queueID );
                threadsDonatedTo.add(waitQueue.resourceHolder);
                waitQueue.resourceHolder.receiveOffer(getEffectivePriority(), this , waitQueue);
            }
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see        nachos.threads.ThreadQueue#acquire
         * @see        nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
            Lib.assertTrue(Machine.interrupt().disabled());
            for(int i=priorityMaximum; i<= priorityMinimum; i--)
                Lib.assertTrue(waitQueue.arrayOfQueues[i].size() == 0);
        }        

        /* ThreadState members */
        protected KThread thread;
        protected int priority;
        protected HashSet<DonationTracker> donationManagementDB;
        protected HashSet<PriorityQueue> queuesThisThreadIsOn;
        protected HashSet<ThreadState> threadsDonatedTo;
        protected int currentBestOffer;
        protected ThreadState currentBestDonor;
    } // End of ThreadState class

    /* PriorityScheduler members */
    private static final char dbgPSched = 'p'; 
    private static boolean enableAsserts = true; // Enable asserts. If this is set to false, calls to Lib.assertTrue do nothing. 
    private static final int INVALID_EFFECTIVE_PRIORITY = -1;
    private static boolean showWarn = true;

    public static void selfTest() {
        System.out.println("----------------------------------------\n  Running PriorityScheduler Self Tests\n"
            +"----------------------------------------");
        PrioritySchedulerTest.runall();   
    }

    /**
     * A wrapper class for nachos.machine.Lib. This wrapper allows easy disablement of assert functionality
     * via the global boolean PriorityScheduler.enableAsserts. If enableAsserts is set to true, this class
     * simply forwards any calls to Lib.x to nachos.machine.Lib.x for Lib method x.
     */
    protected static class Lib {
        protected static void assertTrue(boolean statement, String message) 
            { if(enableAsserts) { nachos.machine.Lib.assertTrue(statement, message); } }
           
        protected static void assertTrue(boolean statement) 
            { if(enableAsserts) { nachos.machine.Lib.assertTrue(statement); } }

        protected static void debug(char flag, String message)
            { nachos.machine.Lib.debug(flag, message); }
    } // End Lib class
} // End PriorityScheduler class
