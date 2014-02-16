package nachos.threads;

import java.util.ArrayList;
import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat {
	static BoatGrader bg;
	
	static private final char dbgBoat = 'B';

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

		begin(2, 3, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		Lib.debug(dbgBoat, "There are " + adults + " adults" + " and " + children + " children");
		Lib.assertTrue(adults >= 0 && children >= 2);

		childLocation = new ArrayList<Island>();
		adultLocation = new ArrayList<Island>();

		ArrayList<KThread> allThreads = new ArrayList<KThread>();

		for (int i = 0; i < adults; i++)
			adultLocation.add(Island.OAHU);
		for (int i = 0; i < children; i++)
			childLocation.add(Island.OAHU);
		
		if(adults > 0)
		{
			nonZeroAdults = true;
		}
		else
		{
			nonZeroAdults = false;
		}

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
		waitForSecondChild = new Condition(boatLock);
		waitToWake = new Condition(boatLock);
		finishTrip = new Condition(boatLock);
		boatIsland = Island.OAHU;
		isFinished = false;
		firstChild = true;
		lastTrip = false;
		numChildOahu = children;
		numChildMolokai = 0;
		numAdultOahu = adults;
		numAdultMolokai = 0;
		numOpenSeats = 2;
		
		//Create adult threads.
		for(int i = 0; i < numAdultThreads; i++)
		{
			final int index = i;
			Runnable r = new Runnable() {
				public void run() {
					adultItinerary(index);
				}
			};
			KThread t = new KThread(r);
			allThreads.add(t);
			t.setName("Adult Thread " + index);
			t.fork();
		}
		
		//Create child threads.
		for(int i = 0; i < numChildThreads; i++)
		{
			final int index = i;
			Runnable r = new Runnable() {
				public void run() {
					childItinerary(index);
				}
			};
			KThread t = new KThread(r);
			allThreads.add(t);
			t.setName("Child Thread " + index);
			t.fork();
		}

		boatLock.acquire();
		finishTrip.sleep();

		boatLock.release();
		
		isFinished = true;
		Lib.debug(dbgBoat, "End of Testing.");
	}
	
	public static void adultItinerary(int index) {
		boatLock.acquire();
		
		Lib.debug(dbgBoat, "In AdultItinerary(" + index + "): got the boat!");
		
		while (!isFinished) {
			if (adultLocation.get(index) == Island.MOLOKAI) {
				break;
			} 
			
			/** The adult is on Oahu. */
			Lib.assertTrue(adultLocation.get(index) == Island.OAHU);
			if (firstChild) {
				/** 
				 * Should be waken up by children threads after there
				 * are at least two children on Molokai. 
				 */
				waitAdultOnOahu.sleep(); 
			} else {
				if (boatIsland == Island.OAHU) {
					if (numOpenSeats == 2) {
						numAdultOahu--;
						bg.AdultRowToMolokai();
						boatIsland = Island.MOLOKAI;
						adultLocation.set(index, Island.MOLOKAI);
						numAdultMolokai++;
						
						Lib.debug(dbgBoat, "adult reaches Molokai, wake up a child here");
						waitChildOnMolokai.wake();
					} else {
						waitAdultOnOahu.sleep(); 
					}
				} else {
					Lib.assertTrue(boatIsland == Island.MOLOKAI);
					
					waitChildOnMolokai.wake();
					waitAdultOnOahu.sleep();
				}
			}
		} 
		
		boatLock.release();
	}
	
	public static void childItinerary(int index) {
		boatLock.acquire();
		
		Lib.debug(dbgBoat, "In ChildItinerary(" + index + "): got the boat!");

		while (!isFinished) {
			if (childLocation.get(index) == Island.OAHU) {
				if (boatIsland == Island.OAHU) {
					if (firstChild) 
						firstChild = false;
					
					if (numOpenSeats == 2) {
						numOpenSeats--;
						Lib.debug(dbgBoat, "child wait for second one (from Oahu ride to Molokai");
						waitChildOnOahu.wake();
						waitForSecondChild.sleep();  // will be waken up by another child.
						
						bg.ChildRideToMolokai();
						numChildOahu -= 2;
						numChildMolokai += 2;
						boatIsland = Island.MOLOKAI;
						childLocation.set(index, Island.MOLOKAI);
						
						/** Check out the termination case. */
						if (numChildOahu == 0 && numAdultOahu == 0) {
							isFinished = true;
							finishTrip.wake();
						} else {
							/** Reset and continue the next trip if possible. */
							numOpenSeats = 2;
							Lib.debug(dbgBoat, "wake a child on Molokai to row back");
							waitChildOnMolokai.wake();
						}
					} else if (numOpenSeats == 1) {
						numOpenSeats--;
						childLocation.set(index, Island.MOLOKAI);
						Lib.debug(dbgBoat, "now wake up another child to ride to Molokai");
						waitForSecondChild.wake();
						bg.ChildRowToMolokai();
					} else {
						Lib.debug(dbgBoat, "no available seats for more child on Oahu, wait");
						waitChildOnOahu.sleep();
					}
				} else {
					Lib.assertTrue(boatIsland == Island.MOLOKAI);
					
					Lib.debug(dbgBoat, "child waits on Oahu because boat is on Molokai");
					waitChildOnMolokai.wake();
					waitChildOnOahu.sleep();
				}
			} else {
				Lib.assertTrue(childLocation.get(index) == Island.MOLOKAI);
				
				if (boatIsland == Island.OAHU) {
					Lib.debug(dbgBoat, "child waits on Molokai because boat is on Oahu");
					waitChildOnOahu.wake();
					waitChildOnMolokai.sleep();
				} else {
					Lib.assertTrue(boatIsland == Island.MOLOKAI);
					
					if (numOpenSeats == 2) {
						numChildMolokai--;
						bg.ChildRowToOahu();
						boatIsland = Island.OAHU;
						childLocation.set(index, Island.OAHU);
						numChildOahu++;
						
						/** Always let children ride to Molokai if we can. */
						if (numChildOahu == 1 && numAdultOahu > 0) {
							Lib.debug(dbgBoat, "No enough children on Oahu, get adult in");
							waitAdultOnOahu.wake();
							waitChildOnOahu.sleep();
						} else {
							waitChildOnOahu.wake();
						}
					} else {
						Lib.debug(dbgBoat, "no available seats for more child on Molokai, wait"
								+ ". numOpenSeats = " + numOpenSeats);
						waitChildOnMolokai.sleep();
					}
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
		Lib.debug(dbgBoat, "\n ***Everyone piles on the boat and goes to Molokai***");
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
	
	private static Condition waitToWake = null;
	
	private static Condition finishTrip = null;

	//Island that boat is currently located
	private static Island boatIsland = Island.OAHU;

	private static boolean isFinished;
	
	/** It is true when one child is on Molokai. */
	private static boolean firstChild;
	
	public static boolean lastTrip;
	
	private static boolean nonZeroAdults;
    
    //Child takes up 1 seat, Adult takes up 2 seats
	private static int numOpenSeats;

	//Integers to track # in each of the groups
	private static int numChildOahu;
	private static int numChildMolokai;
	private static int numAdultOahu;
	private static int numAdultMolokai;
}