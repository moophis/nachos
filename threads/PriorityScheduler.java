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
		System.out.println("Use Prority Scheduler!");
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
	 * Self-test. 
	 * 
	 * XXX: After finish it, delete this code snippet.
	 */
	public static void selfTest() {
		boolean oldP;
		final Lock lock1 = new Lock();
    	final Lock lock2 = new Lock();
    	
    	// low thread
    	KThread lowKt1 = new KThread(new Runnable() {
    		public void run() {
    			lock1.acquire();
    			
    			System.out.println("--------Low thread 1 acquired lock1");
    			
    			for(int i=1; i <=3; i++) {
    				System.out.println("--------Low thread 1 running "+i+" times ...");
    				KThread.yield();
    			}
    			
    			System.out.println("--------Low thread 1 releasing lock1 ...");
    			
    			lock1.release();
    			KThread.yield();
    			
    			System.err.println("--------Low thread 1 running AFTER releasing the lock 1...");
    		}
    	}).setName("Low Thread 1");
    	oldP = Machine.interrupt().disable();
    	ThreadedKernel.scheduler.setPriority(lowKt1, 1);
    	Machine.interrupt().restore(oldP);
    	
    	// middle thread
    	KThread midKt1 = new KThread(new Runnable() {
    		public void run() {
    			lock2.acquire();
    			
    			System.out.println("--------Middle thread 1 has acquired lock2 ...");
    			
    			for (int i = 0; i < 3; i++) {
    				System.out.println("--------Middle thread 1 running "+i+" times ...");
    				KThread.yield();
    			}
    			
    			System.out.println("--------Middle thread 1 releasing lock2 ...");
    			
    			lock2.release();
    			KThread.yield();
    			
    			System.err.println("--------Middle thread 1 running AFTER releasing the lock 2...");
    		}
    	}).setName("Middle Thread 1");
    	oldP = Machine.interrupt().disable();
    	ThreadedKernel.scheduler.setPriority(midKt1, 1);
    	Machine.interrupt().restore(oldP);

    	// high thread 
    	KThread highKt1 = new KThread(new Runnable() {
    		public void run() {
    			lock1.acquire();
    			lock2.acquire();
    			
    			System.out.println("--------High thread 1 get lock 1, 2, now yield");
    			KThread.yield();
    			
    			for (int i = 0; i < 3; i++) {
    				System.out.println("--------High thread 1 running "+i+" times ...");
    				KThread.yield();
    			}
    			
    			System.out.println("--------High thread 1 releasing lock 1, 2, now yield");
    			lock2.release();
    			lock1.release();
    			KThread.yield();
    			
    			System.err.println("--------High thread 1 running AFTER releasing the lock 1,2...");
    		}
    	}).setName("High Thread 1");
    	oldP = Machine.interrupt().disable();
    	ThreadedKernel.scheduler.setPriority(highKt1, 5);
    	Machine.interrupt().restore(oldP);
    	
    	// high thread 
    	KThread highKt2 = new KThread(new Runnable() {
    		public void run() {
    			lock2.acquire();
    			
    			System.out.println("--------High thread 2 get lock 2");

    			for (int i = 0; i < 3; i++) {
    				System.out.println("--------High thread 1 running "+i+" times ...");
    				KThread.yield();
    			}
    			System.out.println("--------High thread 2 releasing lock 2, now yield");
    			
    			lock2.release();
    			KThread.yield();
    			
    			System.err.println("--------High thread 2 running AFTER releasing the lock 2...");
    		}
    	}).setName("High Thread 2");
    	oldP = Machine.interrupt().disable();
    	ThreadedKernel.scheduler.setPriority(highKt2, 5);
    	Machine.interrupt().restore(oldP);
    	
    	lowKt1.fork();
    	KThread.yield();
    	
    	midKt1.fork();
    	KThread.yield();
    	
    	highKt1.fork();
    	highKt2.fork();
    	KThread.yield();
    	
    	highKt2.join();
    	highKt1.join();
    	midKt1.join();
    	lowKt1.join();
	}
	
	public static void selfTest1() {
		System.out.println("---------PriorityScheduler test---------------------");
		PriorityScheduler s = new PriorityScheduler();
		ThreadQueue queue = s.newThreadQueue(true);
		ThreadQueue queue2 = s.newThreadQueue(true);
		ThreadQueue queue3 = s.newThreadQueue(true);

		KThread thread1 = new KThread();
		KThread thread2 = new KThread();
		KThread thread3 = new KThread();
		KThread thread4 = new KThread();
		KThread thread5 = new KThread();
		thread1.setName("thread1");
		thread2.setName("thread2");
		thread3.setName("thread3");
		thread4.setName("thread4");
		thread5.setName("thread5");


		boolean intStatus = Machine.interrupt().disable();

		queue3.acquire(thread1);
		queue.acquire(thread1);
		queue.waitForAccess(thread2);
		queue2.acquire(thread4);
		queue2.waitForAccess(thread1);
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());

		s.getThreadState(thread2).setPriority(3);

		System.out.println("After setting thread2's EP=3:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());

		queue.waitForAccess(thread3);
		s.getThreadState(thread3).setPriority(5);

		System.out.println("After adding thread3 with EP=5:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread3 EP="+s.getThreadState(thread3).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());

		s.getThreadState(thread3).setPriority(2);

		System.out.println("After setting thread3 EP=2:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread3 EP="+s.getThreadState(thread3).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());

		System.out.println("Thread1 acquires queue and queue3");

		Machine.interrupt().restore(intStatus);
		System.out.println("--------End PriorityScheduler test------------------");
	}
	
	public static void selfTest2() {
		boolean oldP;
    	final Lock lock1 = new Lock();
    	final Lock lock2 = new Lock();
    	
    	// low priority thread
    	KThread lowKt1 = new KThread(new Runnable() {
    		public void run() {
    			lock1.acquire();
    			
    			System.out.println("!!!Low thread 1 acquired lock1");
    			
    			for(int i=1; i <=3; i++) {
    				System.out.println("!!!Low thread 1 running "+i+" times ...");
    				KThread.yield();
    			}
    			
    			System.out.println("!!!Low thread 1 releasing lock1 ...");
    			
    			lock1.release();
    			KThread.yield();
    			
    			System.err.println("!!!Low thread 1 running AFTER releasing the lock ...");
    		}
    	}).setName("Low Thread 1");
    	
    	oldP = Machine.interrupt().disable();
    	ThreadedKernel.scheduler.setPriority(lowKt1, 1);
    	Machine.interrupt().restore(oldP);
    	
    	// low priority thread
    	KThread lowKt2 = new KThread(new Runnable() {
    		public void run() {
    			lock2.acquire();
    			
    			System.out.println("!!!Low thread 2 acquired lock2");
    			
    			for(int i=1; i <=3; i++) {
    				System.out.println("!!!Low thread 2 running "+i+" times ...");
    				KThread.yield();
    			}
    			
    			System.out.println("!!!Low thread 2 releasing lock2 ...");
    			
    			lock2.release();
    			KThread.yield();
    			
    			System.err.println("!!!Low thread 2 running AFTER releasing the lock ...");
    		}
    	}).setName("Low Thread 2");
    	
    	oldP = Machine.interrupt().disable();
    	ThreadedKernel.scheduler.setPriority(lowKt2, 1);
    	Machine.interrupt().restore(oldP);
    	
    	// high priority thread
    	KThread highKt = new KThread(new Runnable() {
    		public void run() {
    			lock1.acquire();
    			
    			System.out.println("!!!High thread acquired lock1");
    			
    			lock1.release();
    			
    			System.out.println("!!!High thread released lock1");
    			
    			lock2.acquire();
    			
    			System.out.println("!!!High thread acquired lock2");
    			
    			lock2.release();
    			
    			System.out.println("!!!High thread released lock2");
    		}
    	}).setName("High Thread");
    	
    	oldP = Machine.interrupt().disable();
    	ThreadedKernel.scheduler.setPriority(highKt, 6);
    	Machine.interrupt().restore(oldP);
    	
    	// middle priority thread
    	KThread middleKt = new KThread(new Runnable() {
    		public void run() {    			
    			for(int i=1;i<=3;i++) {
	    			System.out.println("!!!Middle thread running "+i+" times ...");

	    			KThread.yield();
    			}
    		}
    	}).setName("Middle Thread");
    	
        oldP = Machine.interrupt().disable();
    	ThreadedKernel.scheduler.setPriority(middleKt, 4);
    	Machine.interrupt().restore(oldP);
    	
    	lowKt1.fork();
    	lowKt2.fork();
    	
    	//start low thread, let it acquire lock1
    	KThread.yield();
    	
    	middleKt.fork();
    	highKt.fork();
    	
    	KThread.yield();
    	
    	highKt.join();    	
    	middleKt.join();
    	lowKt1.join();
    	lowKt2.join();
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
			System.out.println("### In nextThread()" + " transport priority? " 
		                       + this.transferPriority + " holder: " 
					           + (resHolder == null ? "none" : getHolder().thread));
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			ThreadState ts = pickNextThread();
			
			if (ts == null)
				return null;
//			System.out.println("   ### DEQUE " + ts.thread + ", EP=" + ts.effectivePriority +
//					", enqueue time: " + ts.enqueuingTime);
			
			priorityQueue.poll();	// dequeue
			
//			if (priorityQueue.isEmpty())
				ts.acquire(this);
			
			ts.currentWait = null;
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
//				System.out.println("#### In pickNextThread(): next: "
//						+ priorityQueue.peek().thread);
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
				return priorityQueue.peek();
			} else {
				return null;
			}
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
			System.out.println("/*********** Print current queue, transferPriority ? "
						+ transferPriority);
			for (ThreadState t : priorityQueue) {
				System.out.println(t.thread + ": P = " + t.priority + ", EP = " 
							+ t.effectivePriority + ", enqueued @" + t.enqueuingTime);
			}
			System.out.println("**********************************************************/");
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
//			System.out.println("### In " + this.thread.toString() 
//					           + "--> setPriority(" + priority + ") ");

			if (this.priority == priority)
				return;
			
			this.priority = priority;

			// implement me
//			if (this.priority > this.getEffectivePriority()) {
//				this.setEffectivePriority(priority);
//			}
			this.setEffectivePriority(priority);
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
//					System.out.println("++ After donation(set EP), next thread: " 
//					                  + currentWait.pickNextThread().thread);
				}
			}
		}
		
		/**
		 * Called when the current waiting thread should update the effective 
		 * priorities of threads holding the resources. This method will
		 * recursively update all effective priorities that are influenced
		 * by the change of <tt>this.effectivePriority</tt>.
		 * 
		 * It will change the effective priorities of caller's donees.
		 * 
		 * @author liqiangw
		 */
		private void donate() {
			Lib.assertTrue(currentWait != null);
			System.out.println("   *** -> In donate() of " + this.thread 
					          + " EP = " + this.getEffectivePriority());
			if (this.doneeList.isEmpty())
				return;
			
			for (ThreadState donee : doneeList) {
				System.out.println("      *** donee: " + donee.thread.toString());
			}
			
			for (ThreadState donee : doneeList) {
				int ep = -1;
				/**
				 * Should consider the case when multiple threads wait
				 * on one thread. 
				 */
				for (ThreadState donator : donee.donatorList) {
					System.out.println("      " + donee.thread + " has donator: " + donator.thread);
					ep = Math.max(ep, donator.getEffectivePriority());
				}
				donee.effectivePriority = (ep >= donee.getEffectivePriority()) 
										 ? ep : donee.getPriority();
				
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
			System.out.println("### In " + this.thread.toString() + "--> waitForAccess()"
					          + " = transport priority? " + waitQueue.transferPriority);
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(waitQueue != null);
			
			enqueuingTime = Machine.timer().getTime(); // update enqueuing time
			waitQueue.priorityQueue.add(this);
			currentWait = waitQueue;
			
			/** Donate to the resource holder of this queue. */
			if (currentWait.transferPriority) {
				ThreadState holder;
				if ((holder = waitQueue.getHolder()) != null) {
					/**
					 * Add the holder to the doneeList whenever the holder's
					 * effective priority needs to be promoted. 
					 */
					if (!doneeList.contains(holder) 
							&& this.getEffectivePriority() > holder.getEffectivePriority()) {
						System.err.println("         ---> this ("+ getEffectivePriority() 
								       + ") add " + holder.thread + " (" + holder.getEffectivePriority()
								       + ") to doneeList");
						doneeList.add(holder);
						holder.donatorList.add(this);
					}
				}
				this.donate();
				System.out.println("++ After donation (wait), next thread: " 
		                  + currentWait.pickNextThread().thread);
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
			System.out.println("### In " + this.thread.toString() + "--> acquire()"
					           + " = transport priority? " + waitQueue.transferPriority);
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
					 * Remove certain records in donatorList. 
					 */
					if (holder.donatorList.contains(this)) {
						System.out.println("### ---> " + this.thread + 
								" is deleted from donatorList of " + holder.thread);
						holder.donatorList.remove(this);
					}
					
					/**
					 * Remove certain records in doneeList. 
					 */
					if (this.doneeList.contains(holder)) {
						System.out.println("### ---> " + holder.thread + 
								" is deleted from doneeList of " + this.thread);
						this.doneeList.remove(holder);
					}
					
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
					System.err.println(holder.thread + " has new EP = " + holder.effectivePriority);
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
