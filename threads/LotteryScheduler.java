package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;

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
		Lib.debug(dbgPS, "Using Lottery Scheduler!");
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
		return new LotteryQueue(transferPriority);
	}
	
	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;

	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 1;

	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = Integer.MAX_VALUE;
	
	/**
	 * Lottery Queue: extends Priority Queue. 
	 */
	protected class LotteryQueue extends PriorityQueue {
		public LotteryQueue(boolean transferPriority) {
			super(transferPriority);
		}
		
		@Override
		public KThread nextThread() {
			ThreadState lts = dequeueWinner();
			
			return (lts != null) ? lts.thread : null;
		}
		
		/**
		 * Randomly choose a thread to dequeue. 
		 */
		private ThreadState dequeueWinner() {
			if (!priorityQueue.isEmpty()) {
				/**
				 * Refresh the queue.
				 * XXX: This implementation is not efficient. 
				 */
				PriorityBlockingQueue<ThreadState> tmp = 
						new PriorityBlockingQueue<ThreadState>();
				while (!priorityQueue.isEmpty()) {
					tmp.add(priorityQueue.poll());
				}
				priorityQueue = tmp;
				print();

				// lottery scheduling part
				int ticketSum = 0;
				ArrayList<ThreadState> candidates = new ArrayList<ThreadState>();

				for (ThreadState ts : priorityQueue) {
					ticketSum += ts.getEffectivePriority();
					candidates.add(ts);
				}
				
				if (ticketSum > 0) {
					int currentTickets = 0;
					int pickedTickets = Lib.random(ticketSum);
					for (ThreadState ts : candidates) {
						currentTickets += ts.getEffectivePriority();
						if (pickedTickets < currentTickets) {
							priorityQueue.remove(ts);
							return ts;
						}
					}
				}
			} 
			
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
			Lib.assertTrue(currentWait != null);
			Lib.debug(dbgPS, "   *** -> In donate() of " + this.thread 
					          + " EP = " + this.getEffectivePriority());
			if (this.doneeList.isEmpty())
				return;
			
			for (ThreadState donee : doneeList) {
				Lib.debug(dbgPS, "      *** donee: " + donee.thread.toString());
			}
			
			for (ThreadState donee : doneeList) {
				int tickets = 0;
				/**
				 * Should consider the case when multiple threads wait
				 * on one thread. 
				 */
				for (ThreadState donator : donee.donatorList) {
					Lib.debug(dbgPS, "      " + donee.thread + " has donator: " + 
							donator.thread + " EP = " + donator.effectivePriority);
					tickets += donator.getEffectivePriority();
				}
				Lib.assertTrue(tickets >= 0);
				donee.effectivePriority = tickets + donee.priority;
				Lib.debug(dbgPS, "      -->" + donee.thread + " now has EP = " + 
										 donee.effectivePriority);
				
				/** Be careful: there is a recursive call. */
				donee.donate();
			}
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
			super.waitForAccess(waitQueue);
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
			Lib.debug(dbgPS, "### In " + this.thread.toString() + "--> acquire()"
			           + " = transport priority? " + waitQueue.transferPriority
			           + " currentWait = " + waitQueue);
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(waitQueue != null);
			
			currentWait = waitQueue;
		
			/**
			 * Find the thread whose effective priority should be
			 * set back to its own priority.
			 * 
			 * This is the case when a thread releases a resource
			 * and needs roll back its effective priority. 
			 */
			if (currentWait.transferPriority) {
				ThreadState holder;
				/**
				 * If the holder of the wait queue is not null or self,
				 * it must be the one who recently releases the 
				 * resource, so that this thread can acquire it. 
				 * That is how we can know who is the last holder.
				 */
				if ((holder = waitQueue.getHolder()) != null
						                   && holder != this) {
					/**
					 * Very important step, otherwise holder.currentWait = null. 
					 */
					holder.currentWait = waitQueue;
					
					/**
					 * Remove all the donators (from donatorList) who share the same
					 * current wait queue with the previous resource holder.
					 * 
					 * We should iterate through all the donatorList of the holder
					 * in order to handle the case when multiple threads may wait
					 * on this lock. If it is not appropriately dealt, the effective
					 * priority of the holder might be wrong.
					 * 
					 * The donators' doneeList should also be updated accordingly.
					 */
					Iterator<ThreadState> it = holder.donatorList.iterator();
					while (it.hasNext()) {
						ThreadState donator = it.next();
						Lib.debug(dbgPS, "holder waits on: " + holder.currentWait
								+ ", its donator waits on: " + donator.currentWait);
						if (donator.currentWait == holder.currentWait) {
							it.remove();
							if (donator.doneeList.contains(holder)) {
								donator.doneeList.remove(holder);
							}
						}
					}
					
					/**
					 * Re-calculate the effective priority for the
					 * last holder (including itself). 
					 */
					int tickets = holder.getPriority();
					for (ThreadState t : holder.donatorList) {
						if (t != this && t.currentWait != holder.currentWait) {
							Lib.debug(dbgPS, "holder has donator: " + t.thread);
							tickets += t.getEffectivePriority();
						}
					}
					Lib.assertTrue(tickets >= holder.getPriority());
					/**
					 * it will call donate() eventually.
					 */
					holder.setEffectivePriority(tickets); 
					Lib.debug(dbgPS, holder.thread + " has new EP = " + holder.effectivePriority);
				} 
				waitQueue.setHolder(this);
			}
		}
	}
}
