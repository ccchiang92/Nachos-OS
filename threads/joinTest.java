package nachos.threads;
/* You may have to add more imports here, depending on what you're testing */
import nachos.machine.*;
import nachos.threads.*;


/** A class specifically for testing join. */
public class joinTest {
	public static void runJoinTests(){
		int statusNew = 0;
		int statusReady = 1;
		int statusRunning = 2;
		int statusBlocked = 3;
		int statusFinished = 4;
		
		//first test
		//creates a printing Thread
		//and the kernel thread will join with this thread
		int curID=KThread.currentThread().getID();
		tobejoined simpleRun= new tobejoined();
		KThread simpleTest= new KThread(simpleRun);
		simpleTest.fork();
		simpleTest.join();
		Lib.assertTrue(simpleTest.getStatus()==statusFinished);//the joining thread finishes before the caller thread
		Lib.assertTrue(KThread.currentThread().getID()==curID);
		
		
		//second test
		//calls join on a finished thread
		//this test will crash the system as expected
		//so the join call is commented out
		Lib.assertTrue(simpleTest.getStatus()==statusFinished);
		/**simpleTest.join(); //this call crashes the system on purpose but is disabled**/
		

		//third test
		//instead of call join with the kernel thread
		//this test creates a caller thread-->inThread
		//inThread calls join with a toBeJoin runnable, which is just a printing thread
		joinRun joinInside = new joinRun();
		KThread inThread =new KThread(joinInside);
		inThread.fork();


		//fourth test
		//Test that two concurrent join method call is possible 
		//as long as there are 2 distinct thread pairs
		//the separate calls will no interfere with each other
		//this test is done by, when the joining threads runs, they immediately yield
		//when the kernel thread gains back control we check that both caller threads are asleep
		//and when the caller threads wakes, they test that the joining threads are finished
		
		joinRunWithYield twoRun1 = new joinRunWithYield();
		KThread twoThread1 =new KThread(twoRun1);
		joinRunWithYield twoRun2 = new joinRunWithYield();
		KThread twoThread2 =new KThread(twoRun2);
		twoThread1.fork();
		twoThread2.fork();
		boolean status = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(twoThread1, 1);
		ThreadedKernel.scheduler.setPriority(twoThread2, 2);
		Machine.interrupt().restore(status);
		KThread.yield();
		Lib.assertTrue(twoThread1.getStatus()==statusBlocked);
		Lib.assertTrue(twoThread1.getStatus()==statusBlocked);
		
		//fifth test
		//although the variable names are called double, its actually a triple join
		//kernel thread joins with doubleThread
		//and doubleThread again calls join on another thread
		//then this other thread calls join on a fourth thread
		//so in total there are 3 join calls
		//this tests if the threads finish in the right order
		DoublejoinRun DoubleJoin = new DoublejoinRun();
		KThread DoubleThread =new KThread(DoubleJoin);
		DoubleThread.fork();
		DoubleThread.join();
		Lib.assertTrue(DoubleThread.getStatus()==statusFinished);
		
		
		
		
	}



}
//////////////////////////////////////////////////
/**runnable classes used for testing**/

class joinRun implements Runnable {
	//a runnable that forks a new thread and joins with that new thread
	public joinRun(){
	}
	public void run() {
		//System.out.println("insideTest starts");
		int curID=KThread.currentThread().getID();
		tobejoined joiningRun= new tobejoined();
		KThread joiningThread= new KThread(joiningRun);
		joiningThread.fork();
		//System.out.println("join starts");
		joiningThread.join();
		Lib.assertTrue(joiningThread.getStatus()==4);//status finished
		Lib.assertTrue(KThread.currentThread().getID()==curID);


		//System.out.println("join ends");

	}
}
class tobejoined implements Runnable {
	//a printing thread, that currently does nothing to avoid clutter
	public void run() {
		//System.out.println("first call");
		for (int i=0; i<1; i++){
			//System.out.println("Im being joined" );
		}
		//System.out.println("done");



	}
}
//a runnable that immediately yields and give up control
class yieldThread implements Runnable {

	public yieldThread(){
	}
	public void run() {
		KThread.yield();
		//System.out.println("testing");
	}
}

class joinRunWithYield implements Runnable {
	//a runnable that forks a new thread and joins with that new thread

	public joinRunWithYield(){

	}
	public void run() {
		//System.out.println("insideTest starts");
		int curID=KThread.currentThread().getID();
		yieldThread joiningRun= new yieldThread();
		KThread joiningThread= new KThread(joiningRun);
		joiningThread.fork();
		//System.out.println("join starts");
		joiningThread.join();
		Lib.assertTrue(joiningThread.getStatus()==4);//status finished
		Lib.assertTrue(KThread.currentThread().getID()==curID);
		//System.out.println("join ends");

	}
}
class DoublejoinRun implements Runnable {
	//a runnable that forks a new thread and joins with that new thread
	public DoublejoinRun(){
	}
	public void run() {
		//System.out.println("insideTest starts");
		int curID=KThread.currentThread().getID();
		joinRun firstRun= new joinRun();
		KThread joiningThread= new KThread(firstRun);
		joiningThread.fork();
		//System.out.println("join starts");
		joiningThread.join();
		Lib.assertTrue(joiningThread.getStatus()==4);//status finished
		Lib.assertTrue(KThread.currentThread().getID()==curID);
    	//System.out.println("join ends");

	}
}