package nachos.threads;

import nachos.machine.*;

import java.util.Comparator;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.LinkedList;
//import java.util.PriorityQueue;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            ThreadState ts = getThreadState(thread);
            ts.waitForAccess(this);
            if ((this.holder != null) && (ts.effectivePriority > this.getThreadPriority())){
                //Donation
                waitingThreads.add(ts);
                if (transferPriority)
                    holder.calcEffectivePriority();
            }else{
                waitingThreads.add(ts);
            }

            if (verbose) print();
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());

            Lib.assertTrue(waitingThreads.isEmpty());
            getThreadState(thread).acquire(this);
            this.holder = getThreadState(thread);
            if (verbose) print();
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me

            //ThreadState nextT = waitingThreads.poll();//this.pickNextThread();
            ThreadState nextT;
            /* Alternative: iterate*/
            int maxPriority = 0;
            ThreadState argmax = null;
            int index = -1;
            for (ThreadState ts : waitingThreads){
                index++;
                int tempPrior;
                if (transferPriority){
                    tempPrior = ts.getEffectivePriority();
                }else{
                    tempPrior = ts.getPriority();
                }
                if (tempPrior > maxPriority){
                    maxPriority = tempPrior;
                    argmax = ts;
                }
            }
            nextT = argmax;
            waitingThreads.remove(argmax);
            /* End*/

            if (holder == null){
                //Strange, should call acquire() instead
            }else{
                Lib.assertTrue(this.holder != null);
                this.holder.relinquish(this);
            }


            if (nextT==null){
                this.holder = null;
                if (verbose) print();
                return null;
            }
            //nextT = this.waitingThreads.poll();

            //this.acquire(nextT.thread);
            nextT.acquire(this);
            this.holder = nextT;
            if (verbose) print();
            return nextT.thread;
        }

        public void updateThreadPriority(ThreadState st){
            Lib.assertTrue(Machine.interrupt().disabled());
            //Called when st changes

            //boolean removeFlag = waitingThreads.remove(st);
            //Lib.assertTrue(removeFlag);
            //waitingThreads.add(st);

            if (transferPriority && this.holder!=null){
                this.holder.calcEffectivePriority();
            }

        }

        public int getThreadPriority(){
            if (!transferPriority){
                return -1;
            }
            /* Alternative: iterate*/
            int maxPriority = -1;
            ThreadState argmax = null;
            for (ThreadState ts : waitingThreads){
                int tempPrior;
                if (transferPriority){
                    tempPrior = ts.getEffectivePriority();
                }else{
                    tempPrior = ts.getPriority();
                }
                if (tempPrior > maxPriority){
                    maxPriority = tempPrior;
                    argmax = ts;
                }
            }
            return maxPriority;
            /* End*/

            //ThreadState top = this.waitingThreads.peek();
            //if (top == null)
            //	return -1;
            //return top.effectivePriority;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return	the next thread that <tt>nextThread()</tt> would
         *		return.
         */
        protected ThreadState pickNextThread() {
            // implement me

            /* Alternative: iterate*/
            int maxPriority = 0;
            ThreadState argmax = null;
            for (ThreadState ts : waitingThreads){
                int tempPrior;
                if (transferPriority){
                    tempPrior = ts.getEffectivePriority();
                }else{
                    tempPrior = ts.getPriority();
                }
                if (tempPrior > maxPriority){
                    maxPriority = tempPrior;
                    argmax = ts;
                }
            }
            return argmax;
            //return this.waitingThreads.peek();
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
            for (ThreadState ts : waitingThreads){
                System.out.print(ts.thread + " <"+ts.getPriority()+","+ts.getEffectivePriority()+"> ");
            }
            if (holder==null)
                System.out.println("Holder: null");
            else
                System.out.println("Holder: "+holder.thread+" <"+holder.getPriority()+","+holder.getEffectivePriority()+"> ");
            //System.out.println("");
        }

        //public java.util.PriorityQueue<ThreadState> waitingThreads = new java.util.PriorityQueue<ThreadState>();
        public java.util.LinkedList<ThreadState> waitingThreads = new java.util.LinkedList<ThreadState>();
        public ThreadState holder; //Current holder of resource

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;

        public boolean verbose = false;
    }




    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState implements Comparable<ThreadState>{
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param	thread	the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;

            setPriority(priorityDefault);
            this.effectivePriority = priority;
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return	the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return	the effective priority of the associated thread.
         */
        public int calcEffectivePriority(){
            int maxPriority = priority;
            for (PriorityQueue pq : acquiredResource){
                int tempPrior = pq.getThreadPriority();
                if (tempPrior > maxPriority){
                    maxPriority = tempPrior;
                }
            }
            if ((maxPriority != this.effectivePriority) && (this.waitingResource != null)){
                this.effectivePriority = maxPriority;
                //TODO: if increase, need to pass this to waitingResource
                this.waitingResource.updateThreadPriority(this);
            }else{
                this.effectivePriority = maxPriority;
            }


            return maxPriority;
        }

        public int getEffectivePriority() {
            // implement me

            return effectivePriority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param	priority	the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;
            // implement me

            this.effectivePriority = calcEffectivePriority();
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param	waitQueue	the queue that the associated thread is
         *				now waiting on.
         *
         * @see	nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
            // implement me
            Lib.assertTrue(this.waitingResource == null);
            this.waitingResource = waitQueue;
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see	nachos.threads.ThreadQueue#acquire
         * @see	nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
            // implement me
            this.acquiredResource.add(waitQueue);
            this.waitingResource = null;
            ThreadState prev = waitQueue.holder;
            if (prev==null)
                return;
            this.calcEffectivePriority();
            //prev.calcEffectivePriority();
        }

        public void relinquish(PriorityQueue acqQueue){
            this.acquiredResource.remove(acqQueue);
            this.calcEffectivePriority();
        }

        @Override
        public int compareTo(ThreadState st){
            //TODO: Use effective priority -- done
            if (this.effectivePriority == st.effectivePriority)
                return 0;
            else
                return this.effectivePriority > st.effectivePriority ? -1 : 1;
            //return this.effectivePriority > st.effectivePriority ? 1 : -1;
        }

        @Override
        public boolean equals(Object obj){
            if (this==obj) return true;
            if (obj==null) return false;
            if (this.getClass() != obj.getClass()) return false;
            ThreadState ts = (ThreadState) obj;
            return this.thread.equals(ts.thread);
        }

        /** The thread with which this object is associated. */
        protected KThread thread;
        /** The priority of the associated thread. */
        public int effectivePriority;
        protected int priority;


        protected HashSet<PriorityQueue> acquiredResource = new HashSet<PriorityQueue>(); //Acquired resources
        protected PriorityQueue waitingResource = null; //Resource currently waiting for
    }


    /**
     * Self test for priority scheduler
     */
    private static class Tester implements Runnable{
        Tester(int which){
            this.which = which;
        }
        public void run(){
            System.out.println("*** Thread "+which+" running");
        }
        private int which;
    }

    private static void test1(){
        //Test1: Test whether schedule by priority or not; test tie breaking
        System.out.println("----- PriorityQueue TEST1 -----");
        boolean intStatus = Machine.interrupt().disable();

        PriorityScheduler scheduler = new PriorityScheduler();
        ThreadQueue waitQueue = scheduler.newThreadQueue(true);
        KThread t1 = new KThread(new Tester(1)).setName("thread 1");
        KThread t2 = new KThread(new Tester(2)).setName("thread 2");
        KThread t3 = new KThread(new Tester(3)).setName("thread 3");
        KThread t4 = new KThread(new Tester(4)).setName("thread 4");
        KThread t5 = new KThread(new Tester(5)).setName("thread 5");
        scheduler.setPriority(t1,1);
        scheduler.setPriority(t2,2);
        scheduler.setPriority(t3,2);
        scheduler.setPriority(t4,3);
        scheduler.setPriority(t5,1);
        waitQueue.acquire(t1);
        waitQueue.waitForAccess(t2);
        waitQueue.print();
        waitQueue.waitForAccess(t4);
        waitQueue.print();
        waitQueue.waitForAccess(t5);
        waitQueue.print();
        waitQueue.waitForAccess(t3);
        waitQueue.print();
        System.out.println(waitQueue.nextThread());
        waitQueue.print();
        System.out.println(waitQueue.nextThread());
        waitQueue.print();
        System.out.println(waitQueue.nextThread());
        waitQueue.print();
        System.out.println(waitQueue.nextThread());
        waitQueue.print();
        //waitQueue.waitForAccess(t3);
        //waitQueue.print();
        //System.out.println(waitQueue.nextThread());
        //System.out.println(waitQueue.nextThread());

        Machine.interrupt().restore(intStatus);
    }


    private static void test2(){
        // Test many-to-one donation
        System.out.println("----- PriorityQueue TEST2 -----");
        boolean intStatus = Machine.interrupt().disable();

        PriorityScheduler scheduler = new PriorityScheduler();
        ThreadQueue Q1 = scheduler.newThreadQueue(true);
        ThreadQueue Q2 = scheduler.newThreadQueue(true);
        ThreadQueue Q3 = scheduler.newThreadQueue(true);
        ThreadQueue Q4 = scheduler.newThreadQueue(true);
        ThreadQueue Q5 = scheduler.newThreadQueue(true);
        KThread t1 = new KThread(new Tester(1)).setName("thread 1");
        KThread t2 = new KThread(new Tester(2)).setName("thread 2");
        KThread t3 = new KThread(new Tester(3)).setName("thread 3");
        KThread t4 = new KThread(new Tester(4)).setName("thread 4");
        KThread t5 = new KThread(new Tester(5)).setName("thread 5");
        scheduler.setPriority(t1,1);
        scheduler.setPriority(t2,2);
        scheduler.setPriority(t3,3);
        scheduler.setPriority(t4,5);
        scheduler.setPriority(t5,4);

        Q1.acquire(t1);
        Q2.acquire(t1);
        Q3.acquire(t1);
        Q4.acquire(t5);
        Q4.waitForAccess(t1);
        Q1.print();
        Q2.print();
        Q3.print();
        Q4.print();
        Q1.waitForAccess(t2);
        Q1.print();
        Q2.print();
        Q3.print();
        Q4.print();
        Q2.waitForAccess(t3);
        Q1.print();
        Q2.print();
        Q3.print();
        Q4.print();
        Q3.waitForAccess(t4);
        Q1.print();
        Q2.print();
        Q3.print();
        Q4.print();

        Machine.interrupt().restore(intStatus);
    }



    public static void selfTest(){
        test1();
        test2();
    }
}
