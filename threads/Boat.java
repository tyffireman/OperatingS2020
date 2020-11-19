package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.*;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
//	System.out.println("\n ***Testing Boats with only 2 children***");
//	begin(0, 2, b);

	System.out.println("\n ***Testing Boats with ? children, ? adult***");
  	begin(4, 5, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(4, 2, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	/*Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();*/
	gameStartLock.acquire();
	Runnable adultR = new Runnable() {
	    public void run() {
                AdultItinerary();
            }
	};

	for (int i=0; i<adults; i++){
	    KThread adultThread=new KThread(adultR);
	    adultThread.setName("Adult "+String.valueOf(i));
	    adultThread.fork();
	}

	Runnable childR = new Runnable() {
	    public void run() {
                ChildItinerary();
            }
	};

	for (int i=0; i<children; i++){
	    KThread childThread=new KThread(childR);
	    childThread.setName("Child "+String.valueOf(i));
	    childThread.fork();
	}

	allAdult=adults;
	allChild=children;
	gameStartLock.release();
	System.out.println("game start");
	dummy.acquire();
	checkEnd.sleep();
	dummy.release();
	System.out.println("game end\n");
	    
    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
	gameStartLock.acquire();
	gameStartLock.release();
	aLock.acquire();
	checkState.acquire();
	while (qd1==false || qd2==false){
	    checkState.release();
	    KThread.yield();
	    checkState.acquire();
	}
	checkState.release();
	while (aok==false){
	    KThread.yield();
	}
	rideLock.acquire();
	bg.AdultRideToMolokai();
	rided=true;
	rideLock.release();
	refreshLock.acquire();
	refreshLock.release();
	aLock.release();
	
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
	gameStartLock.acquire();
	gameStartLock.release();
	int me=0;
	checkState.acquire();
	if (qd1==false){ qd1=true;me=1;}
	else if (qd2==false){ qd2=true;me=2;}
	while (qd2==false){
	    checkState.release();
	    KThread.yield();
	    checkState.acquire();
	}
	checkState.release();
	if (me==1){
	    while (allChild>2){
		refreshLock.acquire();
		rideLock.acquire();
		c1=true;
	        while (c2==false){
		    KThread.yield();
	        }
		bg.ChildRowToMolokai();
		while (rided==false){
		    rideLock.release();
		    KThread.yield();
		    rideLock.acquire();
		}
		rideLock.release();
		bg.ChildRowToOahu();
		boolean intStatus = Machine.interrupt().disable();
		c1=c2=rided=false;
		allChild-=1;
		Machine.interrupt().restore(intStatus);
		refreshLock.release();
	    }
	    
	    while (allAdult>0){
		rideLock.acquire();
		refreshLock.acquire();
		c1=true;
	        while (c2==false){
		    KThread.yield();
	        }
		bg.ChildRowToMolokai();
		rided=false;
		while (rided==false){//wait qd2 go
		    rideLock.release();
		    KThread.yield();
		    rideLock.acquire();
		}
		bg.ChildRowToOahu();
		rided=false;
		aok=true;//c1 yong aok pan
		while (rided==false){//wait A go
		    rideLock.release();
		    KThread.yield();
		    rideLock.acquire();
		}
		rided=false;
		aok=false;
		cok=true;
		while (rided==false){//wait qd2 ret
		    rideLock.release();
		    KThread.yield();
		    rideLock.acquire();
		}
		boolean intStatus = Machine.interrupt().disable();
		c1=c2=rided=aok=cok=false;//buxuyaole?
		allAdult-=1;
		Machine.interrupt().restore(intStatus);
		refreshLock.release();
		rideLock.release();
	    }
	    bg.ChildRowToMolokai();
	    canEnd=true;
	}

	if (me==2){
	    while (allChild>2){
		KThread.yield();
	    }
	    while (allAdult>0){
		c2=true;
		rideLock.acquire();
		while (c1==false){
		    rideLock.release();
		    KThread.yield();
		    rideLock.acquire();
	     	}
		bg.ChildRideToMolokai();
		rided=true;
		while (cok==false){
		    rideLock.release();
		    KThread.yield();
		    rideLock.acquire();
	     	}
		bg.ChildRowToOahu();
		rided=true;
		rideLock.release();
		refreshLock.acquire();
		refreshLock.release();
	    }
	    while (canEnd==false) KThread.yield();
	    bg.ChildRideToMolokai();
	    dummy.acquire();
	    checkEnd.wake();
	    dummy.release();
	}

	if (me==0){
	    cLock.acquire();
	    c2=true;
	    rideLock.acquire();
	    while (c1==false){
		rideLock.release();
		KThread.yield();
		rideLock.acquire();
	    }
	    bg.ChildRideToMolokai();
	    rided=true;
	    rideLock.release();
	    cLock.release();
	    refreshLock.acquire();
	    refreshLock.release();
	}
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	/*bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();*/
    }

    private static Lock checkState=new Lock();
    private static Lock qd1Lock=new Lock();
    private static Lock qd2Lock=new Lock();
    private static boolean qd1=false;
    private static boolean qd2=false;
    private static boolean aok=false;
    private static boolean cok=false;
    private static boolean c1=false;
    private static boolean c2=false;
    private static boolean rided=false;
    private static boolean canEnd=false;
    private static Lock aLock=new Lock();
    private static Lock cLock=new Lock();
    private static Lock gameStartLock=new Lock();
    private static Lock dummy=new Lock();
    private static Condition checkEnd=new Condition(dummy);
    private static Lock rideLock=new Lock();
    private static int allAdult=0;
    private static int allChild=0;
    private static Lock refreshLock=new Lock();

}
