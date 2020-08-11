package nachos.threads;
/* You may have to add more imports here, depending on what you're testing */
import nachos.machine.*;
import nachos.threads.*;
import java.util.Random;


/** A class specifically for testing Communicator. */
public class CommunicatorTest {

    /** Debugger flag specifically for Communicator and CommunicatorTest. */
    final static char dbgComm = 'v';

    /** Allows assertions to happen in this class.
     *  NOTE: MAKE SURE TO TURN OFF BEFORE SUBMITTING BY PASSING THE PARAMETER
     *  AS FALSE WHEN CONSTRUCTING ASSERTCOMMUNICATOR. */
    final static AssertCommunicator assertComm = new AssertCommunicator(false);

    /* Run all tests for the Communicator class. */
    public static void runAllCommunicatorTests() {
        Lib.debug(dbgComm,
                "\n** Running all tests concerning the Communicator class");
        Lib.debug(dbgComm, "Running commTest01()"); commTest01();
        Lib.debug(dbgComm, "Running commTest02()"); commTest02();
        Lib.debug(dbgComm, "Running commTest03()"); commTest03();
        Lib.debug(dbgComm, "Running commTest04()"); commTest04();
        Lib.debug(dbgComm, "Running commTest05()"); commTest05();
        Lib.debug(dbgComm, "Running commTest06()"); commTest06();
        Lib.debug(dbgComm, "Running commTest07()"); commTest07();
        Lib.debug(dbgComm, "Running commTest08()"); commTest08();
        Lib.debug(dbgComm, "Running commTest09()"); commTest09();
        Lib.debug(dbgComm, "Running commTest10()"); commTest10();
        Lib.debug(dbgComm, "Running commTest11()"); commTest11();
        Lib.debug(dbgComm, "Running commTest12()"); commTest12();
        Lib.debug(dbgComm, "Running commTest13()"); commTest13();
        Lib.debug(dbgComm, "Running commTest14()"); commTest14();
        Lib.debug(dbgComm, "Running commTest15()"); commTest15();
        Lib.debug(dbgComm, "Running commTest16()"); commTest16();
        Lib.debug(dbgComm, "Running commTest17()"); commTest17();
        Lib.debug(dbgComm, "Running commTest18()"); commTest18();
        Lib.debug(dbgComm, "Running commTest19()"); commTest19();
        Lib.debug(dbgComm, "Running commTest20()"); commTest20();
        Lib.debug(dbgComm, "Running commTest21()"); commTest21();
    }

    /* Testing with one waiting speaker.
    *  NOTE: cannot call the JOIN() method due to the thread not being able to
    *  complete its task; call YIELD() instead to allow it to be scheduled. */
    public static void commTest01() {
        Communicator comm = new Communicator();
        KThread speaker = makeSpeakerThread(comm, 2);

        speaker.fork();

        KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 1,
                "COMMTEST01() FAILED: comm.getWaitSpeakers() should be 1.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST01() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 0,
                "COMMTEST01() FAILED: comm.getTotalPairs() should be 0.");
    }

    /* Testing with one waiting listener. */
    public static void commTest02() {
        Communicator comm = new Communicator();
        KThread listener = makeListenerThread(comm);

        listener.fork();

        KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST02() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 1,
                "COMMTEST02() FAILED: comm.getWaitListeners() should be 1.");
        assertComm.assertTrue(comm.getTotalPairs() == 0,
                "COMMTEST02() FAILED: comm.getTotalPairs() should be 0.");
    }

    /** Testing with two waiting speakers. */
    public static void commTest03() {
        Communicator comm = new Communicator();
        KThread speaker1 = makeSpeakerThread(comm, 1);
        KThread speaker2 = makeSpeakerThread(comm, 2);

        speaker1.fork();
        speaker2.fork();

        KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 2,
                "COMMTEST03() FAILED: comm.getWaitSpeakers() should be 2.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST03() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 0,
                "COMMTEST03() FAILED: comm.getTotalPairs() should be 0.");
    }

    /** Testing with two waiting listeners. */
    public static void commTest04() {
        Communicator comm = new Communicator();
        KThread listener1 = makeListenerThread(comm);
        KThread listener2 = makeListenerThread(comm);

        listener1.fork();
        listener2.fork();

        KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST04() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 2,
                "COMMTEST04() FAILED: comm.getWaitListeners() should be 2.");
        assertComm.assertTrue(comm.getTotalPairs() == 0,
                "COMMTEST04() FAILED: comm.getTotalPairs() should be 0.");
    }

    /** Testing with one speaker first and then one listener. */
    public static void commTest05() {
        Communicator comm = new Communicator();
        KThread speaker = makeSpeakerThread(comm, 17);
        KThread listener = makeListenerThread(comm);

        speaker.fork();
        listener.fork();

        speaker.join();
        listener.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST05() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST05() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 1,
                "COMMTEST05() FAILED: comm.getTotalPairs() should be 1.");
    }

    /** Testing with one listener first and then one speaker. */
    public static void commTest06() {
        Communicator comm = new Communicator();
        KThread listener = makeListenerThread(comm);
        KThread speaker = makeSpeakerThread(comm, 100);

        listener.fork();
        speaker.fork();

        listener.join();
        speaker.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST06() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST06() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 1,
                "COMMTEST06() FAILED: comm.getTotalPairs() should be 1.");
    }

    /** Testing with two speakers first, then one listener (ssl).
     *  Note: l is the char L, not the number one. */
    public static void commTest07() {
        Communicator comm = new Communicator();
        KThread speaker1 = makeSpeakerThread(comm, 2);
        KThread speaker2 = makeSpeakerThread(comm, 4);
        KThread listener3 = makeListenerThread(comm);

        speaker1.fork();
        speaker2.fork();
        listener3.fork();

        speaker1.join();
        //speaker2.join();
        listener3.join();

        KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 1,
                "COMMTEST07() FAILED: comm.getWaitSpeakers() should be 1.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST07() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 1,
                "COMMTEST07() FAILED: comm.getTotalPairs() should be 1.");
    }

    /** Testing with two listeners first, then one speaker (lls). */
    public static void commTest08() {
        Communicator comm = new Communicator();
        KThread listener1 = makeListenerThread(comm);
        KThread listener2 = makeListenerThread(comm);
        KThread speaker3 = makeSpeakerThread(comm, 25);

        listener1.fork();
        listener2.fork();
        speaker3.fork();

        listener1.join();
        //listener2.join();
        speaker3.join();

        KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST08() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 1,
                "COMMTEST08() FAILED: comm.getWaitListeners() should be 1.");
        assertComm.assertTrue(comm.getTotalPairs() == 1,
                "COMMTEST08() FAILED: comm.getTotalPairs() should be 1.");
    }

    /** Testing in order of sls. */
    public static void commTest09() {
        Communicator comm = new Communicator();
        KThread speaker1 = makeSpeakerThread(comm, 6);
        KThread listener2 = makeListenerThread(comm);
        KThread speaker3 = makeSpeakerThread(comm, 33);

        speaker1.fork();
        listener2.fork();
        speaker3.fork();

        //speaker1.join();
        listener2.join();
        speaker3.join();

        KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 1,
                "COMMTEST09() FAILED: comm.getWaitSpeakers() should be 1.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST09() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 1,
                "COMMTEST09() FAILED: comm.getTotalPairs() should be 1.");

    }

    /** Testing in order of l(s2)l.
     *  NOTE: SPEAKER2 has 2 in its third argument, which means that it runs
     *  COMM.SPEAK(WORD) twice (abbreviated as (s2)). */
    public static void commTest10() {
        Communicator comm = new Communicator();
        KThread listener1 = makeListenerThread(comm);
        KThread speaker2 = makeSpeakerThread(comm, 86, 2);
        KThread listener3 = makeListenerThread(comm);

        listener1.fork();
        speaker2.fork();
        listener3.fork();

        listener1.join();
        speaker2.join();
        listener3.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST10() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST10() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 2,
                "COMMTEST10() FAILED: comm.getTotalPairs() should be 2.");
    }

    /** Testing in order of (s2)ll. */
    public static void commTest11() {
        Communicator comm = new Communicator();
        KThread speaker1 = makeSpeakerThread(comm, 1113, 2);
        KThread listener2 = makeListenerThread(comm);
        KThread listener3 = makeListenerThread(comm);

        speaker1.fork();
        listener2.fork();
        listener3.fork();

        speaker1.join();
        listener2.join();
        listener3.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST11() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST11() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 2,
                "COMMTEST11() FAILED: comm.getTotalPairs() should be 2.");
    }

     /** Testing in order of (l2)ss.
      *  NOTE: LISTENER1 has 2 in its second argument, which means that it will
      *  run COMM.LISTEN() twice. */
    public static void commTest12() {
        Communicator comm = new Communicator();
        KThread listener1 = makeListenerThread(comm, 2);
        KThread speaker2 = makeSpeakerThread(comm, 32);
        KThread speaker3 = makeSpeakerThread(comm, 64);

        listener1.fork();
        speaker2.fork();
        speaker3.fork();

        listener1.join();
        speaker2.join();
        speaker3.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST12() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST12() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 2,
                "COMMTEST12() FAILED: comm.getTotalPairs() should be 2.");
    }

    /** Testing in order of slsl. */
    public static void commTest13() {
        Communicator comm = new Communicator();
        KThread speaker1 = makeSpeakerThread(comm, 3);
        KThread listener2 = makeListenerThread(comm);
        KThread speaker3 = makeSpeakerThread(comm, 9000);
        KThread listener4 = makeListenerThread(comm);

        speaker1.fork();
        listener2.fork();
        speaker3.fork();
        listener4.fork();

        speaker1.join();
        listener2.join();
        speaker3.join();
        listener4.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST13() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST13() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 2,
                "COMMTEST13() FAILED: comm.getTotalPairs() should be 2.");
    }

    /** Testing in order of lsls. */
    public static void commTest14() {
        Communicator comm = new Communicator();
        KThread listener1 = makeListenerThread(comm);
        KThread speaker2 = makeSpeakerThread(comm, 0xBABEFACE);
        KThread listener3 = makeListenerThread(comm);
        KThread speaker4 = makeSpeakerThread(comm, 0xABEEFDAD);

        listener1.fork();
        speaker2.fork();
        listener3.fork();
        speaker4.fork();

        listener1.join();
        speaker2.join();
        listener3.join();
        speaker4.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST14() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST14() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 2,
                "COMMTEST14() FAILED: comm.getTotalPairs() should be 2.");
    }

    /** Testing in order of ssll. */
    public static void commTest15() {
        Communicator comm = new Communicator();
        KThread speaker1 = makeSpeakerThread(comm, 1337);
        KThread speaker2 = makeSpeakerThread(comm, 101);
        KThread listener3 = makeListenerThread(comm);
        KThread listener4 = makeListenerThread(comm);

        speaker1.fork();
        speaker2.fork();
        listener3.fork();
        listener4.fork();

        speaker1.join();
        speaker2.join();
        listener3.join();
        listener4.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST15() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST15() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 2,
                "COMMTEST15() FAILED: comm.getTotalPairs() should be 2.");
    }

    /** Testing in order of llss. */
    public static void commTest16() {
        Communicator comm = new Communicator();
        KThread listener1 = makeListenerThread(comm);
        KThread listener2 = makeListenerThread(comm);
        KThread speaker3 = makeSpeakerThread(comm, 94704);
        KThread speaker4 = makeSpeakerThread(comm, 510);

        listener1.fork();
        listener2.fork();
        speaker3.fork();
        speaker4.fork();

        listener1.join();
        listener2.join();
        speaker3.join();
        speaker4.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST16() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST16() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 2,
                "COMMTEST16() FAILED: comm.getTotalPairs() should be 2.");
    }

    /** Testing in order of slls. */
    public static void commTest17() {
        Communicator comm = new Communicator();
        KThread speaker1 = makeSpeakerThread(comm, 0xB0B);
        KThread listener2 = makeListenerThread(comm);
        KThread listener3 = makeListenerThread(comm);
        KThread speaker4 = makeSpeakerThread(comm, 0xACE);

        speaker1.fork();
        listener2.fork();
        listener3.fork();
        speaker4.fork();

        speaker1.join();
        listener2.join();
        listener3.join();
        speaker4.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST17() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST17() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 2,
                "COMMTEST17() FAILED: comm.getTotalPairs() should be 2.");
    }


    /** Testing in order of lssl. */
    public static void commTest18() {
        Communicator comm = new Communicator();
        KThread listener1 = makeListenerThread(comm);
        KThread speaker2 = makeSpeakerThread(comm, 0xB0B);
        KThread speaker3 = makeSpeakerThread(comm, 0xACE);
        KThread listener4 = makeListenerThread(comm);

        listener1.fork();
        speaker2.fork();
        speaker3.fork();
        listener4.fork();

        listener1.join();
        speaker2.join();
        speaker3.join();
        listener4.join();

        //KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST18() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST18() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 2,
                "COMMTEST18() FAILED: comm.getTotalPairs() should be 2.");
    }

    /** Test case with interleaving ten speakers and ten listeners. */
    public static void commTest19() {
        Communicator comm = new Communicator();
        KThread speaker0 = makeSpeakerThread(comm, 0);
        KThread listener0 = makeListenerThread(comm);
        KThread speaker1 = makeSpeakerThread(comm, 1);
        KThread listener1 = makeListenerThread(comm);
        KThread speaker2 = makeSpeakerThread(comm, 2);
        KThread listener2 = makeListenerThread(comm);
        KThread speaker3  = makeSpeakerThread(comm, 3);
        KThread listener3 = makeListenerThread(comm);
        KThread speaker4 = makeSpeakerThread(comm, 4);
        KThread listener4 = makeListenerThread(comm);
        KThread speaker5 = makeSpeakerThread(comm, 5);
        KThread listener5 = makeListenerThread(comm);
        KThread speaker6 = makeSpeakerThread(comm, 6);
        KThread listener6 = makeListenerThread(comm);
        KThread speaker7 = makeSpeakerThread(comm, 7);
        KThread listener7 = makeListenerThread(comm);
        KThread speaker8 = makeSpeakerThread(comm, 8);
        KThread listener8 = makeListenerThread(comm);
        KThread speaker9 = makeSpeakerThread(comm, 9);
        KThread listener9 = makeListenerThread(comm);

        speaker0.fork();
        listener0.fork();
        speaker1.fork();
        listener1.fork();
        speaker2.fork();
        listener2.fork();
        speaker3.fork();
        listener3.fork();
        speaker4.fork();
        listener4.fork();
        speaker5.fork();
        listener5.fork();
        speaker6.fork();
        listener6.fork();
        speaker7.fork();
        listener7.fork();
        speaker8.fork();
        listener8.fork();
        speaker9.fork();
        listener9.fork();

        speaker0.join();
        listener0.join();
        speaker1.join();
        listener1.join();
        speaker2.join();
        listener2.join();
        speaker3.join();
        listener3.join();
        speaker4.join();
        listener4.join();
        speaker5.join();
        listener5.join();
        speaker6.join();
        listener6.join();
        speaker7.join();
        listener7.join();
        speaker8.join();
        listener8.join();
        speaker9.join();
        listener9.join();

        // KThread.yield();

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST19() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST19() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == 10,
                "COMMTEST19() FAILED: comm.getTotalPairs() should be 10.");
    }

    /** Test case with two different Communicators */
    public static void commTest20() {
        Communicator comm1 = new Communicator();
        Communicator comm2 = new Communicator();

        KThread speaker11 = makeSpeakerThread(comm1, 42);
        KThread listener12 = makeListenerThread(comm1, 2);
        KThread speaker13 = makeSpeakerThread(comm1, 24);

        KThread speaker21 = makeSpeakerThread(comm2, 161);
        KThread speaker22 = makeSpeakerThread(comm2, 162);
        KThread listener23 = makeListenerThread(comm2, 2);

        speaker11.fork();
        listener12.fork();
        speaker13.fork();

        speaker21.fork();
        speaker22.fork();
        listener23.fork();

        speaker11.join();
        listener12.join();
        speaker13.join();

        speaker21.join();
        speaker22.join();
        listener23.join();

        // KThread.yield();

        assertComm.assertTrue(comm1.getWaitSpeakers() == 0,
                "COMMTEST20() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm1.getWaitListeners() == 0,
                "COMMTEST20() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm1.getTotalPairs() == 2,
                "COMMTEST20() FAILED: comm.getTotalPairs() should be 2.");

        assertComm.assertTrue(comm2.getWaitSpeakers() == 0,
                "COMMTEST20() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm2.getWaitListeners() == 0,
                "COMMTEST20() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm2.getTotalPairs() == 2,
                "COMMTEST20() FAILED: comm.getTotalPairs() should be 2.");
    }

    /** Large random/fuzz test case. All integers in this method should be
     *  unsigned.
     *  Idea of generating random numbers with MIN and MAX and doubles from
     *  https://stackoverflow.com/questions/363681/
     *  generating-random-numbers-in-a-range-with-java*/
    public static void commTest21() {
        Communicator comm = new Communicator();
        int min = 10; int max = 40;
        int totalCalls = 1;

        assertComm.assertTrue(min >= 0,
                "COMMTEST21() ERROR: min cannot be negative.");
        assertComm.assertTrue(max >= 0,
                "COMMTEST21() ERROR: max cannot be negative.");

        /** TOTALCALLS must also be divisible by two (for ease of testing). */
        while (totalCalls % 2 != 0) {
            /** Generates a random integer in the range of [min, max]. */
            totalCalls = min + (int)(Math.random() * ((max - min) + 1));
        }

        assertComm.assertTrue(totalCalls >= 0,
                "COMMTEST21)() ERROR: totalCalls cannot be negative.");

        int half = (int)(0.5 * totalCalls);
        int numOfSpeakers = 0;
        int numOfListeners = 0;
        int chooseSpeaker = 0;
        int chooseListener = 1;
        int rand = 0;
        KThread[] arrayOfSL = new KThread[totalCalls];
        Random randomWord = new Random();
        int i = 0;

        /** Put random order of speakers and listeners in ARRAYOFSL. */
        while (numOfSpeakers < half || numOfListeners < half) {

            assertComm.assertTrue(numOfSpeakers <= half,
                    "COMMTEST21() ERROR: numOfSpeakers not less than or equal "
                            + "to half.");
            assertComm.assertTrue(numOfListeners <= half,
                    "COMMTEST21() ERROR: numOfListeners not less than or equal "
                            + "to half.");

            if (numOfSpeakers < half && numOfListeners < half) {
                /** Generates a random integer in the range of [0, 1]. */
                rand = (int)(Math.random() * 2);
            } else if (numOfSpeakers < half) {
                rand = chooseSpeaker;
            } else {
                rand = chooseListener;
            }

            if (rand == chooseSpeaker && numOfSpeakers < half) {
                arrayOfSL[i] = makeSpeakerThread(comm, randomWord.nextInt());
                numOfSpeakers += 1;
            } else {
                arrayOfSL[i] = makeListenerThread(comm);
                numOfListeners += 1;
            }
            i += 1;
        }

        /** Fork all the threads in ARRAYOFSL. */
        for (int j = 0; j < arrayOfSL.length; j += 1) {
            arrayOfSL[j].fork();
        }

        /** Join all the threads in ARRAYOFSL. */
        for (int k = 0; k < arrayOfSL.length; k += 1) {
            arrayOfSL[k].join();
        }

        assertComm.assertTrue(comm.getWaitSpeakers() == 0,
                "COMMTEST19() FAILED: comm.getWaitSpeakers() should be 0.");
        assertComm.assertTrue(comm.getWaitListeners() == 0,
                "COMMTEST19() FAILED: comm.getWaitListeners() should be 0.");
        assertComm.assertTrue(comm.getTotalPairs() == half,
                "COMMTEST19() FAILED: comm.getTotalPairs() should be "
                + half + ".");
    }

    /** Makes a KThread that runs Communicator.SPEAK(WORD) LOOP times.
     *  @param comm : the Communicator to be used.
     *  @param word : the WORD to be sent.
     *  @param loop : : the amount of times SPEAK(WORD) is to loop.
     *  @return : a KThread that speaks with the Communicator as a medium. */
    public static KThread makeSpeakerThread(
            Communicator comm, int word, int loop) {
        return new KThread(new speakRunnable(comm, word, loop));
    }

    /** Makes a KThread that runs Communicator.SPEAK(WORD) one time.
     *  @param comm : the Communicator to be used.
     *  @param word : the WORD to be sent.
     *  @return : a KThread that speaks with the Communicator as a medium. */
    public static KThread makeSpeakerThread(Communicator comm, int word) {
        return new KThread(new speakRunnable(comm, word));
    }

    /** Makes a KThread that runs Communicator.LISTEN() LOOP times.
     *  @param comm : the Communicator to be used.
     *  @param loop : : the amount of times LISTEN() is to loop.
     *  @return : a KThread that listens with the Communicator as a medium. */
    public static KThread makeListenerThread(Communicator comm, int loop) {
        return new KThread(new listenRunnable(comm, loop));
    }

    /** Makes a KThread that runs Communicator.LISTEN() one time.
     *  @param comm : the Communicator to be used.
     *  @return : a KThread that listens with the Communicator as a medium. */
    public static KThread makeListenerThread(Communicator comm) {
        return new KThread(new listenRunnable(comm));
    }

    /** A runnable class for a thread utilizing the SPEAK(WORD) method in a
     *  Communicator. */
    public static class speakRunnable implements Runnable {

        /** comm is the Communicator used for the speaker. */
        private Communicator comm;

        /** The WORD to be spoken. */
        private int word;

        /** How many times the SPEAK() method gets gets called. */
        private int loop;

        /** Constructor for the class speakRunnable. Runs as a KThread by
         *  calling fork() within KThread.
         *  @param comm : the Communicator to be used.
         *  @param word : the WORD to be sent.
         *  @param loop : the amount of times SPEAK(WORD) is to loop. */
        public speakRunnable(Communicator comm, int word, int loop) {
            this.comm = comm;
            this.word = word;
            this.loop = loop;
        }

        /** Default constructor.
         *  @param comm : the Communicator to be used.
         *  @param word : the WORD to be sent. */
        public speakRunnable(Communicator comm, int word) {
            this(comm, word, 1);
        }

        /** Now attempt to speak. */
        public void run() {
            for (int i = 0; i < loop; i += 1) {
                comm.speak(word);
            }
        }
    }

    /** A runnable class for a thread utilizing the LISTEN() method in a
     *  Communicator. */
    public static class listenRunnable implements Runnable {

        /** comm is the Communicator used for the listener */
        private Communicator comm;

        /** How many times the LISTEN() method gets gets called. */
        private int loop;

        /** Constructor for the class speakRunnable. Runs as a KThread by
             *  calling fork() within KThread.
             *  @param comm : the Communicator to be used.
             *  @param loop : the amount of times LISTEN() is to loop.*/
            public listenRunnable(Communicator comm, int loop) {
                this.comm = comm;
                this.loop = loop;
        }

        /** Default constructor.
         *  @param comm : the Communicator to be used. */
        public listenRunnable(Communicator comm) {
            this(comm, 1);
        }

        /** Now wait to listen. */
        public void run() {
            for (int i = 0; i < loop; i += 1) {
                comm.listen();
            }
        }
    }

    /** Ease of turning off Assertions from Lib.java, just in case assertions
     *  conflicts with the grader when submitting the project. */
    public static class AssertCommunicator {

        /** A flag to turn on Assertions. Off by default. */
        private static boolean flag = false;

        /** Constructor that wraps the assertions of Lib.java
         *  @param flag : turns on assertions or not. */
        public AssertCommunicator(boolean flag) {
            this.flag = flag;
        }

        /** Attempts to assert if flag == true. Throws an error if assertion
         *  is not fulfilled when the flag is turned on.
         *  Wraps Lib.ASSERTTRUE(BOOLEAN).
         *  @param expression : the expression to test and assert. */
        public static void assertTrue(boolean expression) {
            if (flag) {
                Lib.assertTrue(expression);
            }
            return;
        }

        /** The boolean that we're trying to assert if flag == true.
         *  Throws an error with user-configured message if assertion is not
         *  fulfilled when the flag is turned on.
         *  Wraps LIB.ASSERTTRUE(BOOLEAN, STRING).
         *  @param expression : the expression to test and assert.
         *  @param message : the user-configured message thrown when
         *      there is an error. */
        public static void assertTrue(boolean expression, String message) {
            if (flag) {
                Lib.assertTrue(expression, message);
            }
            return;
        }
    }
}