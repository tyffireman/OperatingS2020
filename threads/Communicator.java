package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

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
        conditionLock = new Lock();
        speakQueue = new Condition2(conditionLock);
        listenQueue = new Condition2(conditionLock);
        speaking = new Condition2(conditionLock);
        listening = new Condition2(conditionLock);
        countSpeaker = 0;
        countListener = 0;
        nottransfer = true;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        conditionLock.acquire();
        // words.add(word);
        if (countListener == 0) {
            countSpeaker = countSpeaker + 1;
            speakQueue.sleep();
            countSpeaker = countSpeaker - 1;
        } else {
            listenQueue.wake();
        }
        while (!nottransfer) {
            speaking.sleep();
        }
        this.word = word;
        nottransfer = true;
        listening.wake();
        conditionLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
        int heard;
        conditionLock.acquire();
        if (countSpeaker == 0) {
            countListener = countListener + 1;
            listenQueue.sleep();
            countListener = countListener - 1;
        } else {
            speakQueue.wake();
        }
        while (nottransfer) {
            listening.sleep();
        }
        heard = word;
        nottransfer = true;
        speaking.wake();
        conditionLock.release();
        return heard;
    }

    private Lock conditionLock;
    // private int word;
    private Condition2 speakQueue;
    private Condition2 listenQueue;
    private Condition2 speaking;
    private Condition2 listening;
    private int countSpeaker;
    private int countListener;
    private int word;
    private boolean nottransfer;

    /**
     * Self Test
     */

    private static class Speaker implements Runnable{
        Speaker(int which, Communicator comm, int word){
            this.which = which;
            this.comm = comm;
            this.word = word;
        }
        Speaker(int which, Communicator comm, int word, KThread toJoin){
            this.which = which;
            this.comm = comm;
            this.tojoin = toJoin;
            this.word = word;
            this.ifJoin = true;
        }
        public void run(){
            if (ifJoin){
                tojoin.join();
            }
            System.out.println("*** Thread "+which+" ready to say "+word);
            comm.speak(word);
            System.out.println("*** Thread "+which+" has said "+word);
        }
        private boolean ifJoin = false;
        private KThread tojoin;
        private Communicator comm;
        private int which;
        private int word;
    }

    private static class Listener implements Runnable{
        Listener(int which, Communicator comm){
            this.which = which;
            this.comm = comm;
        }
        Listener(int which, Communicator comm, KThread toJoin){
            this.which = which;
            this.comm = comm;
            this.tojoin = toJoin;
            this.ifJoin = true;
        }
        public void run(){
            System.out.println("*** Thread "+which+" running");
            if (ifJoin){
                tojoin.join();
            }
            System.out.println("*** Thread "+which+" ready to listen");
            int word = comm.listen();
            System.out.println("*** Thread "+which+" has heard "+word);
        }
        private boolean ifJoin = false;
        private KThread tojoin;
        private Communicator comm;
        private int which;
    }


    private static void test1(){
        //Speaker first
        System.out.println("----- Communicator TEST1 -----");
        Communicator comm = new Communicator();
        KThread speaker1 = new KThread(new Speaker(0,comm,314)).setName("Speaker1");
        KThread speaker2 = new KThread(new Speaker(1,comm,271)).setName("Speaker2");
        KThread listener1 = new KThread(new Listener(2,comm)).setName("Listener1");
        KThread listener2 = new KThread(new Listener(3,comm)).setName("Listener2");
        speaker1.fork();
        listener2.fork();
        ThreadedKernel.alarm.waitUntil(1000000);
        speaker2.fork();
        listener1.fork();
        ThreadedKernel.alarm.waitUntil(10000000);
    }

    private static void test2(){
        //Listener first
        System.out.println("----- Communicator TEST2 -----");
        Communicator comm = new Communicator();
        KThread speaker1 = new KThread(new Speaker(0,comm,314)).setName("Speaker1");
        KThread speaker2 = new KThread(new Speaker(1,comm,271)).setName("Speaker2");
        KThread speaker3 = new KThread(new Speaker(2,comm,729)).setName("Speaker3");
        KThread listener1 = new KThread(new Listener(3,comm)).setName("Listener1");
        KThread listener2 = new KThread(new Listener(4,comm)).setName("Listener2");
        KThread listener3 = new KThread(new Listener(5,comm)).setName("Listener3");
        listener1.fork();
        listener2.fork();
        ThreadedKernel.alarm.waitUntil(1000000);
        speaker1.fork();
        listener3.fork();
        speaker2.fork();
        speaker3.fork();
        ThreadedKernel.alarm.waitUntil(10000000);
    }

    public static void selfTest(){
        test1();
        test2();
    }


}