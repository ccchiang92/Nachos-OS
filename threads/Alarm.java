package nachos.threads;
 
import nachos.machine.*;


import java.util.PriorityQueue;
import java.lang.Long;


/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    boolean intStatus = Machine.interrupt().disable();
	long currentTime =Machine.timer().getTime();
	while (alarmQueue.peek()!=null){
	
		if (currentTime>=alarmQueue.peek().getfirst()){
			AlarmTuple temp=alarmQueue.poll();
			((KThread) temp.getsecond()).ready();
			}
		else{
			break;
			}
	}
		KThread.currentThread().yield();
	
	
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	//creates a alarmTuple that contains the thread and the time to wake up
    //then puts the tuple in the priority queue
    //and sleeps the current thread
    boolean intStatus = Machine.interrupt().disable();
	long wakeTime = Machine.timer().getTime() + x;
	AlarmTuple temp =new AlarmTuple(wakeTime, KThread.currentThread());
	alarmQueue.add(temp);
	KThread.sleep();
	Machine.interrupt().restore(intStatus);
	
    }
    
    
   
    //A Java priority queue to keep track and sort threads
    private static PriorityQueue<AlarmTuple> alarmQueue=new PriorityQueue<AlarmTuple>(); 
    
    //A helper class for priority queue
    //compareTo() is overrided to allow sorting
    private class AlarmTuple<Long, KThread> implements Comparable<AlarmTuple>{ 
    	  private long first; 
    	  private KThread second; 
    	  public AlarmTuple(long x, KThread y) { 
    	    this.first = x; 
    	    this.second = y; 
    	  }
    	  
    	  public int compare(AlarmTuple a, AlarmTuple b){
    		  return a.compareTo(b);
    	  }
    	  @Override
    	  public int compareTo( AlarmTuple comparing){
    		  if (this.first==comparing.getfirst()){
    			  return 0;
    		  }
    		  else{ if (this.first>comparing.getfirst()){
    			  return 1;
    		  }
    		  else{
    			  return -1;
    		  }
    		  }
    		  }
    	  public long getfirst(){
    		  return this.first;
    	  }
    	  public KThread getsecond(){
    		  return this.second;
    	  }
    }
    ///////////////////////////////////////////////////////////////////////////
    //the following are test code
    //Self tests for this class is implemented here, and this method is called in ThreadedKernal
    public static void selfTest(Alarm curAlarm){
    	int statusNew = 0;
    	int statusReady = 1;
    	int statusRunning = 2;
    	int statusBlocked = 3;
    	int statusFinished = 4;
    	
    	//first test
    	//test a single thread
    	//checks the thread is actually asleep(status==blocked==3) during the wait time
    	//and checks when it wakes up  current time is greater than wake up time
    	firstTestRun firstTest= new firstTestRun(curAlarm);
    	KThread firstThread= new KThread(firstTest);
    	firstThread.fork();
    	
    	//second test
    	//tests when 2 threads are schedule to wake at the same time
    	//both will wake at the same time
    	//e.g. when one thread runs the other's status!=blocked
    	secondTestRun secondTest= new secondTestRun(curAlarm);
    	KThread secondThread= new KThread(secondTest);
    	secondThread.fork();
    	
    	//third test
    	//when 2 threads are scheduled to wake up at times far apart (1000 ticks)
    	//when the first thread runs the second is still asleep
    	thirdTestRun thirdTest= new thirdTestRun(curAlarm);
    	KThread thirdThread= new KThread(thirdTest);
    	thirdThread.fork();

    }
}
class firstTestRun implements Runnable {
	private Alarm alarm;
	public firstTestRun(Alarm curAlarm){
		this.alarm=curAlarm;
	}
	public void run(){
		int originID=KThread.currentThread().getID();   	
    	long wakeTime = Machine.timer().getTime() + 1000;
    	statusCheckRun statusCheck= new statusCheckRun(KThread.currentThread(), wakeTime);
    	KThread checkThread= new KThread(statusCheck);
    	//another thread that checks during the wait time, this thread is actually asleep
    	checkThread.fork();
    	int checkThreadID=checkThread.getID();
    	Lib.assertTrue(checkThreadID!=originID);
    	alarm.waitUntil(1000);//current thread is put to sleep
    	
    	//actual test calls making sure this tread wakes up correctly	
    	Lib.assertTrue(KThread.currentThread().getID()==originID);
    	Lib.assertTrue(Machine.timer().getTime()>=wakeTime);
    	/* Alternatively can use print instead of assert
    	System.out.println("first");
    	System.out.println(Machine.timer().getTime());
    	System.out.println(wakeTime);
    	*/
    	
	}
	
}

class statusCheckRun implements Runnable{
	//a thread that checks status of the input thread
	private KThread checkingThread;
	private long wakeTime;
	public statusCheckRun(KThread currentThread, long time){
		this.checkingThread= currentThread;
		this.wakeTime=time;
	}
	public void run(){
		if (Machine.timer().getTime()<=wakeTime){
			Lib.assertTrue(checkingThread.getStatus()==3);
			/* test prints
			System.out.println("blocked");
			System.out.println(Machine.timer().getTime());
			*/
		}
	}
}
class secondTestRun implements Runnable {
	private Alarm alarm;
	public secondTestRun(Alarm curAlarm){
		this.alarm=curAlarm;
	}
	public void run(){
		int originID=KThread.currentThread().getID();   	
    	long wakeTime = Machine.timer().getTime() + 1000;
    	sleepRun sleepThread= new sleepRun(alarm, wakeTime);
    	KThread sameTimeThread= new KThread(sleepThread);
    	//another thread that sleep itself until the waketime
    	sameTimeThread.fork();
    	int sameTimeID=sameTimeThread.getID();
    	Lib.assertTrue(sameTimeID!=originID);
    	alarm.waitUntil(1000);//current thread is put to sleep
    	
    	//actual test calls making sure this tread wakes up correctly
    	Lib.assertTrue(KThread.currentThread().getID()==originID);
    	Lib.assertTrue(Machine.timer().getTime()>=wakeTime);
    	Lib.assertTrue(sameTimeThread.getStatus()!=3);//make sure the other thread is awake
    	/* Alternatively can use print instead of assert
    	System.out.println("second");
    	System.out.println("callThread");
    	System.out.println(Machine.timer().getTime());
    	System.out.println(wakeTime);
    	*/
    		
	}
	
}
class sleepRun implements Runnable{
	//a thread that sleeps till the given time
	private Alarm alarm;
	private long wakeTime;
	public sleepRun(Alarm machineAlarm, long time){
		this.alarm=machineAlarm;
		this.wakeTime=time;
	}
	public void run(){
		alarm.waitUntil(wakeTime-Machine.timer().getTime());;
    	Lib.assertTrue(Machine.timer().getTime()>=wakeTime);
    	/*
    	System.out.println("sleepThread");
    	System.out.println(Machine.timer().getTime());
    	System.out.println(wakeTime);
    	*/
	
	}
}
class thirdTestRun implements Runnable {
	private Alarm alarm;
	public thirdTestRun(Alarm curAlarm){
		this.alarm=curAlarm;
	}
	public void run(){
		int originID=KThread.currentThread().getID();   	
    	long wakeTime = Machine.timer().getTime() + 20;
    	sleepRun sleepThread= new sleepRun(alarm, wakeTime+1000);
    	KThread sameTimeThread= new KThread(sleepThread);
    	//another thread that sleep itself until the waketime
    	sameTimeThread.fork();
    	int sameTimeID=sameTimeThread.getID();
    	Lib.assertTrue(sameTimeID!=originID);
    	alarm.waitUntil(20);//current thread is put to sleep
    	
    	//actual test calls making sure this tread wakes up correctly
    	Lib.assertTrue(KThread.currentThread().getID()==originID);
    	Lib.assertTrue(Machine.timer().getTime()>=wakeTime);
    	Lib.assertTrue(sameTimeThread.getStatus()==3);//make sure the other thread is asleep
    	/* Alternatively can use print instead of assert
    	System.out.println("third");
    	System.out.println("callThread");
    	System.out.println(Machine.timer().getTime());
    	System.out.println(wakeTime);
    	*/
    		
	}
	
}


