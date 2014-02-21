package nachos.threads;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		/**
		 * On timer interrupt, the handler checks whether there are pending
		 * threads to be woke up.
		 * @author liqiangw 
		 */
		long machineTime = Machine.timer().getTime();
		Lib.debug(dbgAlarm, "--- In timerInterrupt(): " + KThread.currentThread() 
					+ " @" + machineTime);
		WaitingThread wt = null;
		while ((wt = sleepQueue.peek()) != null 
				&& wt.getWakeTime() <= machineTime) {
			sleepQueue.poll();
			wt.getThread().ready();  // move this thread on the ready queue
			Lib.debug(dbgAlarm, "    " + wt.getThread() 
					+ " wakes up @" + Machine.timer().getTime()
					+ " (should wake up @" + wt.wakeTime + ")");
		}
		
		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 * 
	 * @author liqiangw 
	 */
	public void waitUntil(long x) {
		if (x <= 0)
			return;	// do nothing
		
		long wakeTime = Machine.timer().getTime() + x;
		Lib.debug(dbgAlarm, KThread.currentThread() + " waits @" 
					+ Machine.timer().getTime());
//		while (wakeTime > Machine.timer().getTime())
//			KThread.yield();
		
		/**
		 * Implement waitUntil() method by maintaining a linked list as a wait 
		 * queue so that the kernel can keep track of sleeping threads. 
		 */
		boolean intStatus = Machine.interrupt().disable();
		WaitingThread current = new WaitingThread(KThread.currentThread(), wakeTime);
		sleepQueue.add(current);
		
		KThread.sleep();	// have current thread relinquish its execution
		Machine.interrupt().restore(intStatus);
	}
	
	/**
	 * Data structure for waiting thread (= a KThread reference + its wake time) 
	 * @author liqiangw
	 */
	private class WaitingThread {
		private KThread kt = null;
		private long wakeTime = -1;
		
		public WaitingThread(KThread kt, long wakeTime) {
			this.kt = kt;
			this.wakeTime = wakeTime;
		}
		
		public long getWakeTime() {
			return wakeTime;
		}
		
		public KThread getThread() {
			return kt;
		}
	}
	
	/**
	 * The comparator for <tt>WaitingThread</tt>. 
	 * @author liqiangw
	 */
	private class WaitingThreadComparator implements Comparator<WaitingThread> {
		public WaitingThreadComparator() {
			
		}
		
		public int compare(WaitingThread t1, WaitingThread t2) {
			return t1.getWakeTime() < t2.getWakeTime() ? -1 : 1;
		}
	}
	
	/**
	 * A priority queue that stores the sleeping kernel threads which called
	 * <tt>waitUntil()</tt> along with the wake time. 
	 * @author liqiangw
	 */
	private PriorityBlockingQueue<WaitingThread> sleepQueue 
				= new PriorityBlockingQueue<WaitingThread>(10, new WaitingThreadComparator());
	
	private static final char dbgAlarm = 'A';
}
