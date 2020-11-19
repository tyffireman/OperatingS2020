package nachos.threads;

import nachos.machine.*;

import java.util.Random;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
        //super();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());
        
        getLThreadState(thread).setPriority(priority);
    }
    
    protected LotteryThreadState getLThreadState(KThread thread) {
        if (thread.schedulingState == null){
            //System.out.println("New LThreadState");
            thread.schedulingState = new LotteryThreadState(thread);
        }
        //thread.schedulingState = new LotteryThreadState(thread);
        return (LotteryThreadState) thread.schedulingState;
    }

    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	    // implement me
	    return new LotteryQueue(transferPriority);
    }

    protected class LotteryQueue extends ThreadQueue {
        LotteryQueue(boolean transferPriority){
            this.transferPriority = transferPriority;
        }   

        public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			LotteryThreadState ts = getLThreadState(thread);
			ts.waitForAccess(this);
			if (this.holder != null){
				//Donation
				waitingThreads.add(ts);
				if (transferPriority)
					holder.calcEffectivePriority();
			}else{
				waitingThreads.add(ts);
			}
			//if (verbose) print();
		}

        public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(waitingThreads.isEmpty());
			getLThreadState(thread).acquire(this);
			this.holder = getLThreadState(thread);
			if (verbose) print();
		}

        public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			LotteryThreadState nextT;
			LotteryThreadState lts = null;
            int total_lottery = 0;
            for (LotteryThreadState ts : waitingThreads){
                if (transferPriority){
                    total_lottery += ts.getEffectivePriority();
                }else{
                    total_lottery += ts.getPriority();
                }
            }
            Random rand = new Random();
            int choice = rand.nextInt(total_lottery)+1;
            int sub_sum = 0;
            for (LotteryThreadState ts : waitingThreads){
                if (transferPriority){
                    sub_sum += ts.getEffectivePriority();
                }else{
                    sub_sum += ts.getPriority();
                }
                if (sub_sum >= choice){
                    lts = ts;
                    break;
                }
            }
            nextT = lts;
			waitingThreads.remove(nextT);
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

        public void updateThreadPriority(LotteryThreadState st){
			Lib.assertTrue(Machine.interrupt().disabled());
			//Called when st changes
			if (transferPriority && this.holder!=null){
				this.holder.calcEffectivePriority();
			}				
		}

        public int getThreadPriority(){
			if (!transferPriority){
				return -1;
			}
			/* Alternative: iterate*/
			int total_priority = 0;
			for (LotteryThreadState ts : waitingThreads){
				int tempPrior;
				if (transferPriority){
					tempPrior = ts.getEffectivePriority();
				}else{
					tempPrior = ts.getPriority();
				}
                total_priority += tempPrior;
			}
			return total_priority;
			/* End*/
		}
        
        protected ThreadState pickNextThread() {
			// implement me
			/* Alternative: iterate*/
			return null;
			//return this.waitingThreads.peek();
		}

        public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
			for (LotteryThreadState ts : waitingThreads){
				System.out.print(ts.thread + " <"+ts.getPriority()+","+ts.getEffectivePriority()+"> ");
			}
			if (holder==null)
				System.out.println("Holder: null");
			else
				System.out.println("Holder: "+holder.thread+" <"+holder.getPriority()+","+holder.getEffectivePriority()+"> ");
		}

        public java.util.LinkedList<LotteryThreadState> waitingThreads = new java.util.LinkedList<LotteryThreadState>();
		public LotteryThreadState holder; //Current holder of resource

		/**
		* <tt>true</tt> if this queue should transfer priority from waiting
		* threads to the owning thread.
		*/
		public boolean transferPriority;
		public boolean verbose = false;

    }


    protected class LotteryThreadState{

        public LotteryThreadState(KThread thread) {
            //super(thread);
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
			int sumPriority = this.priority;
			for (LotteryQueue pq : acquiredResource){
				int tempPrior = pq.getThreadPriority();
				sumPriority += tempPrior;
			}
			if ((sumPriority != this.effectivePriority) && (this.waitingResource != null)){
				this.effectivePriority = sumPriority;
				//TODO: if increase, need to pass this to waitingResource
				this.waitingResource.updateThreadPriority(this);
			}else{
				this.effectivePriority = sumPriority;
			}	
			return sumPriority;
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

        public void waitForAccess(LotteryQueue waitQueue) {
			// implement me
			Lib.assertTrue(this.waitingResource == null);
			this.waitingResource = waitQueue;
		}

		public void acquire(LotteryQueue waitQueue) {
			// implement me
			this.acquiredResource.add(waitQueue);
			this.waitingResource = null;
			LotteryThreadState prev = waitQueue.holder;
			if (prev==null)
				return;
			this.calcEffectivePriority();
			//prev.calcEffectivePriority();
		}	

		public void relinquish(LotteryQueue acqQueue){
			this.acquiredResource.remove(acqQueue);
			this.calcEffectivePriority();
		}

		
		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		public int effectivePriority;
		protected int priority;
		
		protected HashSet<LotteryQueue> acquiredResource = new HashSet<LotteryQueue>(); //Acquired resources
		protected LotteryQueue waitingResource = null; //Resource currently waiting for
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
		System.out.println("----- LotteryQueue TEST1 -----");
		boolean intStatus = Machine.interrupt().disable();

		LotteryScheduler scheduler = new LotteryScheduler();
		ThreadQueue waitQueue = scheduler.newThreadQueue(true);
		KThread t1 = new KThread(new Tester(1)).setName("thread 1");
		KThread t2 = new KThread(new Tester(2)).setName("thread 2");
		KThread t3 = new KThread(new Tester(3)).setName("thread 3");
		KThread t4 = new KThread(new Tester(4)).setName("thread 4");
		KThread t5 = new KThread(new Tester(5)).setName("thread 5");
        scheduler.setPriority(t1,1);
		scheduler.setPriority(t2,20);
		scheduler.setPriority(t3,20);
		scheduler.setPriority(t4,300);
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
		//Test2: Test priority donation and transitivity
		System.out.println("----- PriorityQueue TEST2 -----");
		boolean intStatus = Machine.interrupt().disable();

		LotteryScheduler scheduler = new LotteryScheduler();
		ThreadQueue Q1 = scheduler.newThreadQueue(true);
		ThreadQueue Q2 = scheduler.newThreadQueue(true);
		KThread t1 = new KThread(new Tester(1)).setName("thread 1");
		KThread t2 = new KThread(new Tester(2)).setName("thread 2");
		KThread t3 = new KThread(new Tester(3)).setName("thread 3");
		KThread t4 = new KThread(new Tester(4)).setName("thread 4");
		KThread t5 = new KThread(new Tester(5)).setName("thread 5");
		scheduler.setPriority(t1,100);
		scheduler.setPriority(t2,200);
		scheduler.setPriority(t3,400);
		scheduler.setPriority(t4,6000);
		scheduler.setPriority(t5,7000);

		Q1.acquire(t1);
		Q1.print();
		Q2.acquire(t2);
		Q2.print();
		Q2.waitForAccess(t1);
		Q2.print();
		Q1.waitForAccess(t4);
		Q1.print();
		Q2.print();
		Q1.waitForAccess(t5);
		Q1.print();
		Q2.print();
		Q2.waitForAccess(t3);
		Q2.print();
		System.out.println(Q2.nextThread());
		Q2.print();


		Machine.interrupt().restore(intStatus);
	}

    public static void selfTest(){
        test1();
        test2();
    }
}
