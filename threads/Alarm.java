package nachos.threads;

import nachos.machine.*;

import java.util.*;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        wSet = new ArrayList<>();
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        boolean intS = Machine.interrupt().disable();
        Iterator<WThread> iter = wSet.iterator();
        while (iter.hasNext()) {
            WThread entry = iter.next();
            if (Machine.timer().getTime() > entry.t()) {
                entry.thread().ready();
                iter.remove();
            } else {
                break;
            }
        }
        KThread.currentThread().yield();
        Machine.interrupt().restore(intS);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)

	long wakeTime = Machine.timer().getTime() + x;

        if (wakeTime > Machine.timer().getTime()) {
            boolean intS = Machine.interrupt().disable();
            wSet.add(new WThread(KThread.currentThread(), wakeTime));
            KThread.sleep();
            Machine.interrupt().restore(intS);
        }
    }

    public class WThread implements Comparable<WThread>{

        private KThread thread;
        private long t;


        public WThread(KThread thread, long t) {
            super();
            this.thread = thread;
            this.t= t;
        }

        public KThread thread() {
            return thread;
        }

        public long t() {
            return t;
        }

        @Override
        public int compareTo(WThread w) {
            int ans = 0;
            if (this.t > w.t()) ans = 1;
            if (this.t < w.t()) ans = 0;
            return ans;
        }
    }

  //  public TreeSet<WThread> waitingThreadSet;
    public List<WThread> wSet;
}
