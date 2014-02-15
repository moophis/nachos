package nachos.threads;

import nachos.machine.Lib;

import java.util.*;

import nachos.threads.Lock;
import nachos.threads.KThread;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */

//	private  int tempword;
//	private Lock comlock=new Lock();
//	private boolean empty;
//
	private boolean hasData = false;
	private Lock conditionLock = new Lock();
	private Condition empty = new Condition(conditionLock);
	private Condition full = new Condition(conditionLock);
	private int buffer;
	
	private static final char dbgComm = 'c';
	
	public Communicator() {
//		empty=true;
	}
	
	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
			
	public void speak(int word) {
		
//		while(empty==false) {
//			KThread.currentThread().yield();
//		}
//		if(!comlock.isHeldByCurrentThread()) {
//			comlock.acquire();
//		}
//		tempword=word;
//		empty=false;
//		comlock.release();
//		while(empty==false) {
//			KThread.currentThread().yield();
//			//wait
//		}
		
		conditionLock.acquire();
		while (hasData) {
			empty.sleep();
		}
		Lib.debug(dbgComm, "speak(): ready to send " + word);
		buffer = word;
		hasData = true;
		full.wake();
		conditionLock.release();
	}


	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
//		while(empty) {
//			KThread.currentThread().yield();
//			//wait
//		}
//		if(!comlock.isHeldByCurrentThread()) {
//			comlock.acquire();
//		}
//		int temp=tempword;
//		empty=true;
//		comlock.release();
//		return temp;
		
		conditionLock.acquire();
		while (!hasData)
			full.sleep();
		int ret = buffer;
		hasData = false;
		empty.wake();
		conditionLock.release();
		Lib.debug(dbgComm, "listen(): receiving " + ret);
		return ret;
	}
	
	
	public static class Speak implements Runnable{
		int speakword;
		public static Communicator tempcom= new Communicator();
		public Speak(int tempword, Communicator comm) {
			speakword=tempword;
			tempcom=comm;
		}
		public void run() {
			tempcom.speak(speakword);
			//System.out.println("transmitting: "+speakword);
		}
	}
	
	public static class Listen implements Runnable{
		int listenword;
		public Listen() {
			listenword=0;
		}
		
		public void run() {
			listenword=Communicator.Speak.tempcom.listen();
			System.out.println("get: "+listenword);
		}
	}
	
	public static void selfTest() {
		Communicator communicator=new Communicator();
        System.out.println("test running start:");
	    
	    Runnable communi = new Communicator.Speak(6,communicator);
	    KThread thread1 = new KThread(communi);
	    thread1.fork();
	    System.out.println("Starting thread1 to speak...");
	    
	    Runnable communi2= new Communicator.Speak(9, communicator);
	    KThread thread5 = new KThread(communi2);
	    thread5.fork();
	    
	    Runnable bye = new Communicator.Listen();
	    System.out.println("thread2 try to listen");
	    KThread thread2 = new KThread(bye);
	    thread2.fork();
	    
	    Runnable bye1 = new Communicator.Listen();
	    System.out.println("thread4 try to listen");
	    KThread thread4 = new KThread(bye1);
	    thread4.fork();
	    
        
	    KThread.yield();
	    thread1.join();
	    thread5.join();
	    thread2.join();
	    thread4.join();
	}
}

