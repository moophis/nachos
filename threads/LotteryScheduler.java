package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

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
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
	}

	/**
	 * Allocate a new lottery thread queue.
	 * 
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * tickets from waiting threads to the owning thread.
	 * @return a new lottery thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		// implement me
		return null;
	}
	
	/**
	 * Lottery Queue: extends Priority Queue. 
	 */
	protected class LotteryQueue extends PriorityQueue {
		public LotteryQueue(boolean transferPriority) {
			super(transferPriority);
		}
		
		@Override
		public KThread nextThread() {
			return null;
		}
		
		@Override
		protected LotteryThreadState pickNextThread() {
			return null;
		}
	}
	
	/**
	 * Lottery Thread State: extends Thread State. 
	 */
	protected class LotteryThreadState extends ThreadState {
		public LotteryThreadState(KThread thread) {
			super(thread);
		}
		
		/**
		 * Called when the current waiting thread should update the effective 
		 * priorities of threads holding the resources. This method will
		 * recursively update all effective priorities that are influenced
		 * by the change of <tt>this.effectivePriority</tt>.
		 * <p>
		 * It will change the effective priorities of caller's donees.
		 * <p>
		 * Note: The inner algorithm differs with that function in ThreadState 
		 * class. The EP is updated by summing up all EPs from donators of the
		 * current thread.
		 * 
		 * @author liqiangw
		 */
		@Override
		protected void donate() {
			
		}
		
		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue the queue that the associated thread is now waiting
		 * on.
		 * Note: waitQueue is the queue specific to certain resources such as a
		 * lock.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		@Override
		public void waitForAccess(PriorityQueue waitQueue) {
			
		}
		
		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		@Override
		public void acquire(PriorityQueue waitQueue) {
			
		}
	}
}
