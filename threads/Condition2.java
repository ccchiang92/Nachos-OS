package nachos.threads;


import java.util.ArrayList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	
	conditionLock.release();
	boolean intStatus = Machine.interrupt().disable();
	condition2Queue.add(KThread.currentThread());
	KThread.sleep();
	
	
	Machine.interrupt().restore(intStatus);
	conditionLock.acquire();
	
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	boolean intStatus = Machine.interrupt().disable();
	
	if (!(condition2Queue.isEmpty())){
		condition2Queue.remove(0).ready();
		
	}
	
	
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	boolean intStatus = Machine.interrupt().disable();
	while (!(condition2Queue.isEmpty())){
		condition2Queue.remove(0).ready();
		
	}
	
	
	Machine.interrupt().restore(intStatus);
    }

    private Lock conditionLock;
    private ArrayList<KThread> condition2Queue=new ArrayList<KThread>();
    ////////////////////////////////////////////////////////////////////////////////////////
    //the following are test code
    //Self tests for this class is implemented here, and this method is called in ThreadedKernal
    public static void selfTest(){
    	Lock testLock=new Lock();
    	Condition2 testCondition= new Condition2(testLock);
    	int statusNew = 0;
    	int statusReady = 1;
    	int statusRunning = 2;
    	int statusBlocked = 3;
    	int statusFinished = 4;
    	//System.out.println("condition2 tests");
    	//test 1
    	//edge case test, make sure waking empty conditions don't crash
    	testLock.acquire();
    	testCondition.wake();
    	testCondition.wakeAll();
    	testLock.release();
    	
    	//test2
    	//tests sleep and wake
    	//simply tests to make sure the condition variable actually sleeps and wakes threads
    	sleepThread firstRun = new sleepThread(testLock, testCondition);
    	KThread firstThread= new KThread(firstRun);
    	firstThread.fork();
    	boolean status = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(firstThread, 1);
		Machine.interrupt().restore(status);
    	KThread.yield();
    	Lib.assertTrue(firstThread.getStatus()==statusBlocked);//thread is asleep
    	
    	testLock.acquire();
    	testCondition.wake();
    	testLock.release();
    	Lib.assertTrue(firstThread.getStatus()==statusReady);//thread is awake
    	
    	//test3
    	//tests wakeAll
    	//make sure wakeAll() wakes up all the thread in the condition variable
    	
    	sleepThread allFirstRun = new sleepThread(testLock, testCondition);
    	KThread allThread1= new KThread(allFirstRun);
    	allThread1.fork();
    	sleepThread allSecondRun = new sleepThread(testLock, testCondition);
    	KThread allThread2= new KThread(allSecondRun);
    	allThread2.fork();
    	status = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(allThread1, 1);
		ThreadedKernel.scheduler.setPriority(allThread2, 2);
		Machine.interrupt().restore(status);
    	KThread.yield();
    	Lib.assertTrue(allThread1.getStatus()==statusBlocked);//threads are asleep
    	Lib.assertTrue(allThread2.getStatus()==statusBlocked);
    	testLock.acquire();
    	testCondition.wakeAll();
    	testLock.release();
    	Lib.assertTrue(allThread1.getStatus()==statusReady);//threads are awake
    	Lib.assertTrue(allThread2.getStatus()==statusReady);
    	
    	
    	//test4
    	//waking order
    	//make sure wake() wakes threads in the right order
    	//disable interrupts to prevent order getting messed up
    	
    	sleepThread orderFirstRun = new sleepThread(testLock, testCondition);
    	KThread orderThread1= new KThread(orderFirstRun);
    	orderThread1.fork();
    	sleepThread orderSecondRun = new sleepThread(testLock, testCondition);
    	KThread orderThread2= new KThread(orderSecondRun);
    	orderThread2.fork();
    	status = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(orderThread1, 1);
		ThreadedKernel.scheduler.setPriority(orderThread2, 2);
		Machine.interrupt().restore(status);
    	KThread.yield();
    	Lib.assertTrue(orderThread1.getStatus()==statusBlocked);
    	Lib.assertTrue(orderThread2.getStatus()==statusBlocked);
    	testLock.acquire();
    	testCondition.wake();
    	testLock.release();
    	if (orderThread1.getStatus()==statusReady){
    		Lib.assertTrue(orderThread2.getStatus()==statusBlocked);
    	}
    	else{
    	Lib.assertTrue(orderThread2.getStatus()==statusReady);
    	Lib.assertTrue(orderThread1.getStatus()==statusBlocked);
    	}
    	testLock.acquire();
    	testCondition.wake();
    	testLock.release();
    	Lib.assertTrue(orderThread1.getStatus()==statusReady);
    	Lib.assertTrue(orderThread2.getStatus()==statusReady);
    	
    	
    	
    }

}
class sleepThread implements Runnable{
	//a thread that sleeps till on the given condition variable 
	private Lock lock;
	private Condition2 cond2;
	public sleepThread(Lock testLock, Condition2 testCond){
		this.lock=testLock;
		this.cond2=testCond;
	}
	public void run(){
    	//System.out.println("sleepThread run starts");
    	lock.acquire();
    	cond2.sleep();
    	lock.release();
    	//System.out.println("sleepThread wakes");
	}
}
