package nachos.threads;

import java.util.ArrayList;
import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat {
	static BoatGrader bg;
	
	public static enum Island {
		MOLOKAI, OAHU
	}
	
	/**
	 * These two lists denote the current location
	 * of each person.
	 */
	static ArrayList<Island> childLocation;
	static ArrayList<Island> adultLocation;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		begin(2, 5, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		System.out.println("There are " + adults + " adults" + " and " + children + " children");
		Lib.assertTrue(adults >= 0 && children >= 0);
		
		childLocation = new ArrayList<Island>();
		adultLocation = new ArrayList<Island>();
		
		ArrayList<KThread> allThreads = new ArrayList<KThread>();
		
		for (int i = 0; i < adults; i++)
			adultLocation.add(Island.OAHU);
		for (int i = 0; i < children; i++)
			childLocation.add(Island.OAHU);
		
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;
		int numAdultThreads = adults;
		int numChildThreads = children;
		
		// Instantiate global variables here
		boatLock = new Lock();
		waitChildOnOahu = new Condition(boatLock);
		waitChildOnMolokai = new Condition(boatLock);
		waitAdultOnOahu = new Condition(boatLock);
		waitAdultOnMolokai = new Condition(boatLock);
		boatIsland = Island.OAHU;
		isFinished = false;
		numChildOahu = 0;
		numChildMolokai = 0;
		numAdultOahu = 0;
		numAdultMolokai = 0;
		numOpenSeats = 2;
		
		//Create child threads.
		for(int i = 0; i < numChildThreads; i++)
		{
			final int index = i;
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary(index);
				}
			};
			KThread t = new KThread(r);
			allThreads.add(t);
			t.setName("Child Thread " + index);
			t.fork();
		}
		
		
		//Create adult threads.
		for(int i = 0; i < numAdultThreads; i++)
		{
			final int index = i;
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary(index);
				}
			};
			KThread t = new KThread(r);
			allThreads.add(t);
			t.setName("Adult Thread " + index);
			t.fork();
		}
		
		
		Runnable r = new Runnable() {
			public void run() {
				SampleItinerary();
			}
		};
		KThread t = new KThread(r);
		allThreads.add(t);
		t.setName("Sample Boat Thread");
		t.fork();
		
		System.out.println("All forked, waiting to be joined");
		for (KThread kt : allThreads)
			kt.join();
		
		System.out.println("End of Testing.");
	}

	public static void AdultItinerary(int index) {
		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
		 * across to Molokai
		 */

		boatLock.acquire();
		System.err.println("In AdultItinerary(" + index + "): got the boat!");
		numAdultOahu++;
		
		while(!isFinished) // FIXME: isFinished never is never set to be true
		{
			//Adult thread is on Oahu
			if(adultLocation.get(index) == Island.OAHU)
			{
				//Boat is on Oahu and no one is on boat => Adult takes boat to Molokai
				if((boatIsland == Island.OAHU) && (numOpenSeats == 2))
				{
					numAdultOahu--;
					bg.AdultRowToMolokai();
					boatIsland = Island.MOLOKAI;
					adultLocation.set(index, Island.MOLOKAI);
					numAdultMolokai++;
				}
				//Boat is on Oahu, but it is full => wait for boat to come back
				else if ((boatIsland == Island.OAHU) && (numOpenSeats < 2))
				{
					waitAdultOnOahu.sleep();
				}
				//Boat is on Molokai
				else
				{
					//Wait for boat to come back to Oahu
					waitAdultOnOahu.sleep();
					//Wake child to bring it back.
					waitChildOnMolokai.wake();
				}
			}
			//Adult thread is on Molokai
			else
			{
				/*Need to implement??*/
				//Boat is on Oahu
				if(boatIsland == Island.OAHU)
				{
					waitChildOnOahu.wake();
				}
				//Boat is on Molokai
				else
				{
					waitChildOnMolokai.wake();
				}
			}
		}
		boatLock.release();
	}

	public static void ChildItinerary(int index) {
		boatLock.acquire();
		System.err.println("In ChildItinerary(" + index + "): got the boat!");
		numChildOahu++;
		while(!isFinished)
		{
			System.out.println("Not finished");
			if(childLocation.get(index) == Island.OAHU)
			{
				//Case 1: Boat is on Oahu, more than one child on Oahu => two children take boat to Molokai
				if((boatIsland == Island.OAHU) && (numChildOahu > 1) && (numAdultOahu > 0))
				{ 
					//If this is the first child onto the boat
					if(numOpenSeats == 2)
					{	
						//decrement numOpenSeats because we just filled one
						numOpenSeats--;
						
						//wait for passenger to get on, children never go Oahu-Molokai alone
						waitForSecondChild.sleep();
						
						bg.ChildRowToMolokai();
						childLocation.set(index, Island.MOLOKAI);
					}
					//If this is the second child onto the boat
					else if(numOpenSeats == 1)
					{
						//decrement numOpenSeats because we just filled one
						numOpenSeats--;
						
						//wake up the driver who was waiting for second child to get on boat
						waitForSecondChild.wake();
	
						//now the boat is full, we can go to Molokai
						numChildOahu = numChildOahu - 2;
						bg.ChildRideToMolokai();
						boatIsland = Island.MOLOKAI;
						childLocation.set(index, Island.MOLOKAI);
						numChildMolokai = numChildMolokai + 2;
						
						//boat is empty again
						numOpenSeats = 2;
					}
					//If the boat is already full, numOpenSeats == 0
					else
					{
						waitChildOnOahu.sleep();
					}
				}
				//Boat is on Oahu, only one child on Oahu, more than 0 adults on Oahu => one adult takes boat to Molokai
				else if((boatIsland == Island.OAHU) && (numChildOahu == 1) && (numAdultOahu > 0))
				{
					//Adult is going to ride, so most of this should be handled in AdultItinerary
					waitAdultOnOahu.wake();
				}
				//Boat is on Oahu, two children on Oahu, no adults (FINISH CASE)
				else if((boatIsland == Island.OAHU) && (numChildOahu == 2) && (numAdultOahu == 0))
				{
					/* THIS IS THE FINISH CASE. STILL NEEDS TO BE WRITTEN. SHOULD BE ALMOST IDENTICAL TO INITIAL 'IF' IN THIS BLOCK */
				}
				//Boat is on Molokai
				else
				{
					waitChildOnOahu.sleep();
					waitChildOnMolokai.wake();
				}
			}
			//Child is on Molokai
			else
			{
				//Boat is on Molokai
				if(boatIsland == Island.MOLOKAI) 
				{	
					//Boat is empty => child will get in and ride to Oahu
					if(numOpenSeats == 2)
					{
						numChildMolokai--;
						bg.ChildRideToOahu();
						boatIsland = Island.OAHU;
						childLocation.set(index, Island.OAHU);
						numChildOahu++;
					}
					//Boat is not empty
					else
					{
						waitChildOnMolokai.sleep();
					}
				}
				//Boat is on Oahu
				else
				{
					waitChildOnMolokai.sleep();
					waitChildOnOahu.wake();
				}
			}
			
		}
		boatLock.release();
		
	}

	public static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}
	
	private static Lock boatLock = null;
	
	//Condition for Children waiting on Oahu
	private static Condition waitChildOnOahu = null;
	
	//Condition for Children waiting on Molokai
	private static Condition waitChildOnMolokai = null;
	
	//Condition for Adults waiting on Oahu;
	private static Condition waitAdultOnOahu = null;
	
	//Condition for Adults waiting on Molokia (should never be used)
	private static Condition waitAdultOnMolokai = null;
	
	//Condition to wait for a second child to come on boat
	private static Condition waitForSecondChild = null;
	
	//Island that boat is currently located
	private static Island boatIsland = Island.OAHU;
	
	private static boolean isFinished;
    
    //Child takes up 1 seat, Adult takes up 2 seats
	private static int numOpenSeats;
	
	//Integers to track # in each of the groups
	private static int numChildOahu;
	private static int numChildMolokai;
	private static int numAdultOahu;
	private static int numAdultMolokai;
}
