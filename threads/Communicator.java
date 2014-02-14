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

	private  int tempword;
	private Lock comlock=new Lock();
	private boolean empty;

	
	public Communicator() {
		empty=true;
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
		
		while(empty==false) {
			KThread.currentThread().yield();
		}
		if(!comlock.isHeldByCurrentThread()) {
			comlock.acquire();
		}
		tempword=word;
		empty=false;
		comlock.release();
		while(empty==false) {
			KThread.currentThread().yield();
			//wait
		}
	}


	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		while(empty) {
			KThread.currentThread().yield();
			//wait
		}
		if(!comlock.isHeldByCurrentThread()) {
			comlock.acquire();
		}
		int temp=tempword;
		empty=true;
		comlock.release();
		return temp;
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
}

