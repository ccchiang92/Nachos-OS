package nachos.threads;

import nachos.machine.*;
import nachos.threads.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /** Create one lock for the conditions of this Communicator instance. */
    private Lock communicatorLock = new Lock();

    /** If a speaker is called with no waiting listeners, it waits.
     *  Otherwise, it is signaled to attempt to speak. */
    private Condition okayToSpeak = new Condition(communicatorLock);

    /** Waits for a speaker to speak. */
    private Condition okayToListen = new Condition(communicatorLock);

    /** Locks to preserve the transfer.
     *  Signals once the transfer is successful. */
    private Condition receivedTransfer = new Condition(communicatorLock);

    /** Count of waiting speakers. */
    private int waitSpeakers = 0;

    /** Count of waiting listeners. */
    private int waitListeners = 0;

    /** Boolean of whether an active speaker exists or not. */
    private boolean actSpeaker = false;

    /** Boolean of whether an active listener exists or not. */
    private boolean actListener = false;

    /** The word to be transferred from a speaker to a listener. */
    private int transferredWord = 0xFFFFFFFF;

    /** Debugger flag specifically for Communicator and CommunicatorTest. */
    final static char dbgComm = 'v';

    /** Total amount of pairs between a speaker and a listener.
     *  Used for debugging and testing purposes. */
    private int totalPairs = 0;

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param   word    the integer to transfer.
     */
    public void speak(int word) {
        communicatorLock.acquire();
        while (actSpeaker || waitListeners < 1) {
            waitSpeakers += 1;
            Lib.debug(dbgComm, "\t> Speaker is sleeping");
            okayToSpeak.sleep();
            waitSpeakers -= 1;
        }
        actSpeaker = true;
        transferredWord = word;
        Lib.debug(dbgComm,
                "\t> Awake Speaker spoke the word " + transferredWord);
        okayToListen.wake();
        receivedTransfer.sleep();

        actSpeaker = false;
        okayToSpeak.wake();
        communicatorLock.release();

        return;
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transferred.
     */
    public int listen() {
        communicatorLock.acquire();
        if (waitSpeakers > 0) {
            okayToSpeak.wake();
        }
        waitListeners += 1;
        Lib.debug(dbgComm, "\t> Listener is sleeping");
        okayToListen.sleep();
        waitListeners -= 1;

        actListener = true;
        int receivedWord = transferredWord;
        Lib.debug(dbgComm,
                "\t> Awake Listener received the word " + receivedWord);
        updateTotalPairs();
        receivedTransfer.wake();
        actListener = false;
        communicatorLock.release();

        return receivedWord;
    }

    /** If a speaker transfers a word to a listener, increment TOTALPAIR by 1.
     *  Used for debugging and testing purposes in CommunicatorTest. */
    private void updateTotalPairs() {
        if (actSpeaker && actListener) {
            totalPairs += 1;
        }
    }

    /** Tests for the Communicator class. The actual tests are located under
     *  in the class CommunicatorTest to provide less clutter for the
     *  Communicator class. */
    public static void selfTest() {
        CommunicatorTest.runAllCommunicatorTests();
    }

    /** GETTER methods for testing purposes. */

    /** Get number of waiting speakers.
     *  @return : WAITSPEAKEERS */
    public int getWaitSpeakers() {
        return waitSpeakers;
    }

    /** Get number of waiting listeners
     *  @return : WAITLISTENERS */
    public int getWaitListeners() {
        return waitListeners;
    }

    /** Get the total amount of pairs between a speaker and a listener.
     *  @return : TOTALPAIRS */
    public int getTotalPairs() {
        return totalPairs;
    }
}
