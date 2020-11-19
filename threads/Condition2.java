package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param   conditionLock   the lock associated with this condition
     *              variable. The current thread must hold this
     *              lock whenever it uses <tt>sleep()</tt>,
     *              <tt>wake()</tt>, or <tt>wakeAll()</tt>.
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

        boolean intStatus = Machine.interrupt().disable();
        waitQueue.waitForAccess(KThread.currentThread());

    conditionLock.release();
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

        KThread thread = waitQueue.nextThread();
        if (thread != null) {
            thread.ready();
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

        KThread thread = waitQueue.nextThread();
        while (thread != null) {
            thread.ready();
            thread = waitQueue.nextThread();
        }

        Machine.interrupt().restore(intStatus);
    }


    /* Self Test */
    private static class Tester implements Runnable{
        Tester(int which, Condition2 dataReady, Lock lock, StringBuilder cText,
                int numAdd, int numRead){
            
            this.lock = lock;
            this.dataReady = dataReady;
            this.which = which;
            this.cText = cText;
            this.numAdd = numAdd;
            this.numRead = numRead;
        }
        public void addToText(char content){
            lock.acquire();
            cText.append(content);
            dataReady.wake();
            lock.release();
        }
        public char readText(){
            lock.acquire();
            while (cText.length()==0){
                System.out.println("*** Thread "+which+" No text, going to sleep");
                dataReady.sleep();
                System.out.println("*** Thread "+which+" Awake");
            }
            char firstSymbol = cText.charAt(0);
            cText.deleteCharAt(0);
            lock.release();
            return firstSymbol;
        }
        public void run(){
            for (int i=0; i<numAdd; ++i){
                System.out.println("*** Thread "+which+" ready to write "+(char)(65+i));
                addToText((char)(65+i));
                System.out.println("*** Thread "+which+" has written"+(char)(65+i));
            }
            for (int i=0; i<numRead; ++i){
                char firstSymbol = readText();
                System.out.println("*** Thread "+which+" has read "+firstSymbol);
            }
        }        
        private int numAdd, numRead; //Number of chars to write and read, respectively
        private int which;
        private Lock lock;
        private Condition2 dataReady;
        StringBuilder cText;
    }

    private static void test1(){
        System.out.println("----- Condition Variable TEST1 -----");
        StringBuilder sharedString = new StringBuilder();
        Lock textLock = new Lock();
        Condition2 textReady = new Condition2(textLock);
        new KThread(new Tester(0,textReady, textLock, sharedString, 0, 5)).fork();
        ThreadedKernel.alarm.waitUntil(1000000);
        new KThread(new Tester(1,textReady, textLock, sharedString, 5, 0)).fork();
        ThreadedKernel.alarm.waitUntil(1000000);
    }

    private static void test2(){
        System.out.println("----- Condition Variable TEST2 -----");
        StringBuilder sharedString = new StringBuilder();
        Lock textLock = new Lock();
        Condition2 textReady = new Condition2(textLock);
        new KThread(new Tester(0,textReady, textLock, sharedString, 0, 6)).fork();
        ThreadedKernel.alarm.waitUntil(10000);
        new KThread(new Tester(1,textReady, textLock, sharedString, 2, 0)).fork();
        ThreadedKernel.alarm.waitUntil(10000);
        new KThread(new Tester(2,textReady, textLock, sharedString, 2, 0)).fork();
        ThreadedKernel.alarm.waitUntil(10000);
        new KThread(new Tester(3,textReady, textLock, sharedString, 2, 0)).fork();
        //new KThread(new Tester(2,textReady, textLock, sharedString, 4, 0)).fork();
        ThreadedKernel.alarm.waitUntil(1000000);
    }

    public static void selfTest(){
        test1();
        test2();
    }


    private Lock conditionLock;
    private ThreadQueue waitQueue =
        ThreadedKernel.scheduler.newThreadQueue(false);
}