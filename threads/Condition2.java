package nachos.threads;

import nachos.machine.*;

import java.util.*;

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

        boolean intS = Machine.interrupt().disable();
        waitQueue.waitForAccess(KThread.currentThread());

        conditionLock.release();
        KThread.sleep();

        Machine.interrupt().restore(intS);

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

        boolean intS = Machine.interrupt().disable();

        KThread thread = waitQueue.nextThread();
        while (thread != null) {
            thread.ready();
            thread = waitQueue.nextThread();
        }

        Machine.interrupt().restore(intS);
    }

    /* Self Test */
    private static class Tester implements Runnable{
        Tester(int which, Condition2 data, Lock lock, StringBuilder cText,
               int numAdd, int numRead){

            this.lock = lock;
            this.data = data;
            this.which = which;
            this.cText = cText;
            this.numAdd = numAdd;
            this.numRead = numRead;
        }
        public void addToText(char content){
            lock.acquire();
            cText.append(content);
            data.wake();
            lock.release();
        }
        public char readText(){
            lock.acquire();
            while (cText.length()==0){
                System.out.println(+which+" going to sleep");
                data.sleep();
                System.out.println("*** Thread "+which+" Awake");
            }
            char firstSymbol = cText.charAt(0);
            cText.deleteCharAt(0);
            lock.release();
            return firstSymbol;
        }
        public void run(){
            for (int i=0; i<numAdd; i++){
                System.out.println("** Thread "+which+" ready to write "+(char)(65+i));
                addToText((char)(65+i));
                System.out.println("** Thread "+which+" wrote"+(char)(65+i));
            }
            for (int i=0; i<numRead; i++){
                char firstSymbol = readText();
                System.out.println("** Thread "+which+" has read "+firstSymbol);
            }
        }
        private int numAdd, numRead; //Number of chars to write and read, respectively
        private int which;
        private Lock lock;
        private Condition2 data;
        StringBuilder cText;
    }

    private static void test(){
        System.out.println("----- Condition Variable TEST -----");
        StringBuilder sharedString = new StringBuilder();
        Lock ock = new Lock();
        Condition2 text = new Condition2(ock);
        new KThread(new Tester(2,text, ock, sharedString, 1, 0)).fork();
        ThreadedKernel.alarm.waitUntil(10000);
        new KThread(new Tester(3,text, ock, sharedString, 2, 0)).fork();
        ThreadedKernel.alarm.waitUntil(1000000);
    }

    public static void selfTest(){
        test();
    }


    private Lock conditionLock;
    private ThreadQueue waitQueue =
            ThreadedKernel.scheduler.newThreadQueue(false);
}