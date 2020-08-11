package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat
{
    static BoatGrader bg;

    // Global variables used for this problem
    static Lock lock;
    static Alarm alarm;
    static Condition counted;
    static Condition okToAdult;
    static Condition childrenMolokai;
    static Condition childrenOahu;
    static Condition isDone;
    static int childrenOnOahu = 0;
    static int adultsOnOahu = 0;
    static int childrenOnMolokai = 0;
    static int adultsOnMolokai = 0;
    static boolean pilotChosen = false;
    static boolean countPhaseDone = false;
    static boolean childrenPhaseDone = false;

    // TESTING PURPOSES
    static boolean slow1 = true;
    static boolean slow2 = true;
    static boolean slow3 = true;

    public static void selfTest()
    {
        // Most tests for this part were done through an external script.
        // See the README in proj1/boattest
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;

        // Instantiate global variables here
        lock = new Lock();
        alarm = new Alarm();
        okToAdult = new Condition(lock);
        childrenMolokai = new Condition(lock);
        childrenOahu = new Condition(lock);
        counted = new Condition(lock);
        isDone = new Condition(lock);
        childrenOnOahu = 0;
        adultsOnOahu = 0;
        childrenOnMolokai = 0;
        adultsOnMolokai = 0;
        pilotChosen = false;
        countPhaseDone = false;
        childrenPhaseDone = false;
        
        
        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        lock.acquire();
        for (int i = 0; i < adults; i++) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    AdultItinerary();
                }
            };
            KThread t = new KThread(r);
            t.setName("Adult Thread");
     
            t.fork();
        }

        for (int i = 0; i < children; i++) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ChildItinerary();
                }
            };
            KThread t = new KThread(r);
            t.setName("Child Thread");
       
            t.fork();
        }

        while (adultsOnMolokai != adults || childrenOnMolokai != children) {
            isDone.sleep();
        }

        
        /*Runnable r = new Runnable() {
            public void run() {
                    SampleItinerary();
                }
            };
            KThread t = new KThread(r);
            t.setName("Sample Boat Thread");
            t.fork();*/
    }

    static void overseeCounting() {
        int previousChildren = childrenOnOahu;
        int previousAdults = adultsOnOahu;
        lock.release();
        alarm.waitUntil(20);
        lock.acquire();
        while (previousAdults != adultsOnOahu || previousChildren != childrenOnOahu) {
            previousAdults = adultsOnOahu;
            previousChildren = childrenOnOahu;
            lock.release();
            alarm.waitUntil(20);
            lock.acquire();
        }
        countPhaseDone = true;
        counted.wakeAll(); 	
    }

    static void checkIfDone() {
        if (childrenOnOahu == 0 && adultsOnOahu == 0) {
            isDone.wake();
            lock.release();
            alarm.waitUntil(20);
            lock.acquire();
        }
    }
    
    // TESTING PURPOSES
    static void slowThread() {
        if (slow1 == true) {
    		slow1 = false;
    		alarm.waitUntil(20000);
    		System.out.println("SLOW THREAD COMMING IN");
        } else if (slow2 == true) {
        	slow2 = false;
    		alarm.waitUntil(20000);
    		System.out.println("SLOW THREAD COMMING IN");
        } else if (slow3 == true) {
        	slow3 = false;
    		alarm.waitUntil(20000);
    		System.out.println("SLOW THREAD COMMING IN");
        }
    }
    
    static void AdultItinerary()
    {
    	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
    	//DO NOT PUT ANYTHING ABOVE THIS LINE.  
    	
    	/* This is where you should put your solutions. Make calls
           to the BoatGrader to show that it is synchronized. For
           example:
               bg.AdultRowToMolokai();
           indicates that an adult has rowed the boat across to Molokai
        */

        // begin counting phase
        lock.acquire();
        if (childrenOnOahu == 0 && adultsOnOahu == 0 && childrenOnMolokai == 0 && adultsOnMolokai == 0) {
        	adultsOnOahu++;
            overseeCounting();
        } else {
        	adultsOnOahu++;
        }
        // end counting phase
        
        // begin adult phase
        okToAdult.sleep();
        bg.AdultRowToMolokai();
        adultsOnOahu--;
        adultsOnMolokai++;
        childrenMolokai.wake();
        lock.release();
        // end adult phase

    }


    
    static void ChildItinerary()
    {
    	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
    	//DO NOT PUT ANYTHING ABOVE THIS LINE.
    	
        // begin count phase
        lock.acquire();
        if (childrenOnOahu == 0 && adultsOnOahu == 0 && childrenOnMolokai == 0 && adultsOnMolokai == 0) {
        	childrenOnOahu++;
        	overseeCounting();
        } else {
            childrenOnOahu++;
        }

        if (!countPhaseDone) {
            counted.sleep();
        }
        lock.release();


        // begin child phase (adults will be sleeping in the child phase)
        boolean isOnOahu = true;
        while (true) {
        	lock.acquire();

        	// If for some reason thread never started this if condition should solve it
        	if (childrenPhaseDone == true) {
        		childrenOahu.sleep();
        		break;
        	}

            if (!pilotChosen) {
                pilotChosen = true;
                bg.ChildRowToMolokai();
                childrenOnOahu--;
                childrenOnMolokai++;
                isOnOahu = false;
            } else {
                bg.ChildRideToMolokai();
                bg.ChildRowToOahu();
                pilotChosen = false;
                isOnOahu = true;
            }

            // end case
            if (childrenOnOahu == 1 && isOnOahu) {
            	childrenPhaseDone = true;
                break;
            }

            if (!isOnOahu) {
                break;
            }

            lock.release();
        }

        // Sleep all the children not on Oahu (on Molokai)
        if (!isOnOahu) {
            childrenMolokai.sleep();
        }
        lock.release();
        // end child phase

        // begin adult phase
        while (true) {
        	lock.acquire();
            if (isOnOahu && childrenOnOahu == 1 && adultsOnOahu > 0) {
                okToAdult.wake();
                childrenOahu.sleep();
            } else if (isOnOahu && childrenOnOahu == 1 && adultsOnOahu == 0) { // Occurs only when there are no adults in problem
            	bg.ChildRowToMolokai();
            	childrenOnOahu--;
            	childrenOnMolokai++;
            	isOnOahu = false;
                checkIfDone();
                // not done
                bg.ChildRowToOahu();
                isOnOahu = true;
            	childrenOnOahu++;
            	childrenOnMolokai--;
            } else if (!isOnOahu) {
                bg.ChildRowToOahu();
                childrenOnMolokai--;
                childrenOnOahu++;
                isOnOahu = true;
            }

            if (!pilotChosen && childrenOnOahu >= 2 && isOnOahu) {
                pilotChosen = true;
                bg.ChildRowToMolokai();
                isOnOahu = false;
                childrenOahu.wake();
                childrenMolokai.sleep();
            } else if (childrenOnOahu >= 2 && isOnOahu) {
                bg.ChildRideToMolokai();
                pilotChosen = false;
                childrenOnOahu -= 2;
                childrenOnMolokai += 2;
                isOnOahu = false;

                checkIfDone();

                // not done
                bg.ChildRowToOahu();
                isOnOahu = true;
                childrenOnOahu++;
                childrenOnMolokai--;
            }

            lock.release();
        }
        // end adult phase


    }
    

    static void SampleItinerary()
    {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }
    
}
