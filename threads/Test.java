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
                System.out.println(+which+" going to sleep");
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

    private static void test(){
        System.out.println("----- Condition Variable TEST -----");
        StringBuilder sharedString = new StringBuilder();
        Lock textLock = new Lock();
        Condition2 textReady = new Condition2(textLock);
        new KThread(new Tester(0,textReady, textLock, sharedString, 0, 6)).fork();
        ThreadedKernel.alarm.waitUntil(10);
        new KThread(new Tester(2,textReady, textLock, sharedString, 2, 0)).fork();
        ThreadedKernel.alarm.waitUntil(10000);
        new KThread(new Tester(3,textReady, textLock, sharedString, 2, 0)).fork();
        ThreadedKernel.alarm.waitUntil(1000000);
    }

    public static void selfTest(){
        test();
    }


    private Lock conditionLock;
    private ThreadQueue waitQueue =
            ThreadedKernel.scheduler.newThreadQueue(false);
}