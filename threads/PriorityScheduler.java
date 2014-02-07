package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
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
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * priority from waiting threads to the owning thread.
	 * @return a new priority thread queue.
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

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			ret = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			ret = false;
		else
			setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return ret;
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
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
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
			priorityQueue = new PriorityBlockingQueue<ThreadState>();
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			ThreadState ts = pickNextThread();
			
			if (ts == null)
				return null;
			
			ts.currentWait = null;
			
			priorityQueue.poll();	// dequeue
			return ts.thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			// implement me
			if (!priorityQueue.isEmpty()) {
				return priorityQueue.peek();
			} else {
				return null;
			}
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}
		
		/** 
		 * Get the resource holder of this queue.
		 * 
		 * @return the resource holder.
		 */
		public ThreadState getHolder() {
			return resHolder;
		}
		
		/**
		 * Set a new resource holder of this queue.
		 * 
		 * @param holder The new holder to be set.
		 */
		public void setHolder(ThreadState holder) {
			resHolder = holder;
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		/** The scheduling priority queue */
		public PriorityBlockingQueue<ThreadState> priorityQueue = null;
		
		/** The resource (lock, semaphore, etc.) holder of this wait queue. */
		private ThreadState resHolder = null;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState implements Comparable<ThreadState> {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;

			doneeList = new ArrayList<ThreadState>();
			donatorList = new ArrayList<ThreadState>();
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 * @author liqiangw
		 */
		public int getEffectivePriority() {
			// implement me
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority the new priority.
		 * @author liqiangw
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			
			this.priority = priority;

			// implement me
			if (this.priority > this.getEffectivePriority()) {
				this.setEffectivePriority(priority);
			}
		}
		
		/**
		 * Set the effective priority of the associated thread to the 
		 * specified value.
		 * 
		 * @param priority the new effective priority.
		 * @author liqiangw
		 */
		public void setEffectivePriority(int priority) {
			if (priority <= priorityMaximum 
					&& priority >= priorityMinimum) {
				if (priority == getEffectivePriority())
					return;
				
				this.effectivePriority = priority;
			
				if (currentWait != null 
						&& currentWait.transferPriority) {
					this.donate();
				}
			}
		}
		
		/**
		 * Called when the current waiting thread should update the effective 
		 * priorities of threads holding the resources. This method will
		 * recursively update all effective priorities that are influenced
		 * by the change of this.effectivePriority.
		 * 
		 * It will change the effective priorities of caller's donees.
		 * 
		 * @author liqiangw
		 */
		private void donate() {
			if (doneeList.isEmpty())
				return;
			
			for (ThreadState donee : doneeList) {
				int ep = -1;
				for (ThreadState donator : donee.donatorList) {
					ep = Math.max(ep, donator.getEffectivePriority());
				}
				this.effectivePriority = ep;
				
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
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(waitQueue != null);
			
			enqueuingTime = Machine.timer().getTime(); // update enqueuing time
			waitQueue.priorityQueue.add(this);
			currentWait = waitQueue;
			
			/** Donate the resource holder of this queue. */
			if (currentWait.transferPriority) {
				ThreadState holder;
				if ((holder = waitQueue.getHolder()) != null) {
					if (!doneeList.contains(holder)) {
						doneeList.add(holder);
						holder.donatorList.add(this);
					}
				}
				this.donate();
			}
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
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(waitQueue != null);
			Lib.assertTrue(waitQueue.priorityQueue.isEmpty());
			
			currentWait = waitQueue;
		
			/**
			 * Find the thread whose effective priority should be
			 * set back to its own priority.
			 * 
			 * This is the case when a thread releases a resource
			 * and needs changing its effective priority. 
			 */
			if (currentWait.transferPriority) {
				ThreadState holder;
				/**
				 * If the holder of the wait queue is not null or self,
				 * it must be the one who recently release the 
				 * resource, so that this thread can acquire it. 
				 */
				if ((holder = waitQueue.getHolder()) != null
						                   && holder != this) {
					/**
					 * Re-calculate the effective priority for the
					 * last holder (including itself). 
					 */
					int p = -1;
					for (ThreadState t : holder.donatorList) {
						if (t != this)
							p = Math.max(p, t.getEffectivePriority());
					}
					if (p < priorityMinimum)
						p = holder.getPriority(); // its own priority
					/**
					 * it will call donate() eventually.
					 */
					holder.setEffectivePriority(p); 
					
					/**
					 * Remove certain records in donatorList. 
					 */
					if (holder.donatorList.contains(this)) {
						holder.donatorList.remove(this);
					}
				} 
				waitQueue.setHolder(this);
			}
		}
		
		/**
		 * The comparator of ThreadState. 
		 */
		public int compareTo(ThreadState that) {
			if (this.getEffectivePriority() > that.getEffectivePriority()) {
				return -1;
			} else if (this.getEffectivePriority() < that.getEffectivePriority()) {
				return 1;
			} else {
				if (this.enqueuingTime <= that.enqueuingTime) {
					return -1;
				} else {
					return 1;
				}
			}
		}

		/** The thread with which this object is associated. */
		protected KThread thread;

		/** The priority of the associated thread. */
		protected int priority = priorityDefault;
		
		/** The effective priority of the associated thread. */
		protected int effectivePriority = priorityDefault;
		
		/** The time staying in the waitQueue. */
		protected long enqueuingTime = Long.MAX_VALUE;
		
		/** The priority queue this thread currently wait on. */
		protected PriorityQueue currentWait = null;
		
		/** 
		 * The list of threads who are donated by this thread. 
		 * Note: It only contains direct donee(s).
		 */
		protected ArrayList<ThreadState> doneeList = null;
		
		/** The list of threads who once donated to this thread. */
		protected ArrayList<ThreadState> donatorList = null;
	}
}
