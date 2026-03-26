package main.databases;

//import java.util.ArrayList;
import java.util.HashMap;
//import java.util.concurrent.ScheduledThreadPoolExecutor;
//import java.util.concurrent.ArrayBlockingQueue;

import main.RemoteCommand;
import main.exceptions.IllegalCredentialsException;

//import java.util.concurrent.Executor;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;

public class HoldingQueue
{
	private RemoteCommand rem;
	private Thread cleanupCrew;
	private int maxThreads = 20;
	private int usedThreads = 0;
	private int wiggleRoom = 5;
	//private int queueSpace = 5;
	private boolean[] open = new boolean[maxThreads + wiggleRoom];
	private HashMap<Object, Integer> holdingIDs = new HashMap<Object, Integer>();	//holds the thread ID for each thread that is currently running for a particular key/person
	private HashMap<Integer, Object> holdingPeople = new HashMap<Integer, Object>();	//holds the stored String/caller based on the ID. Used so Cleanup can get the key to the holdingIDS hashmap to clean it
	//private ArrayList<ArrayBlockingQueue<Integer>> mySpot = new ArrayList<ArrayBlockingQueue<Integer>>();	//this is important so the cleanup crew can realize if a thread has died prematurely and can try and fix the problem
	//private ScheduledThreadPoolExecutor me;

	public HoldingQueue(RemoteCommand remote)
	{
		rem = remote;
		for(int i = 0; i < open.length; i++)
		{
			open[i] = true;
		//	mySpot.add(new ArrayBlockingQueue<Integer>(queueSpace));
		}
	}
	
	public synchronized void verifyLooking(Thread thr)		//was protected
	{
		if (cleanupCrew == null)
		{
			cleanupCrew = thr;
		}
		//System.out.println("" + Thread.currentThread() +": I got the message from " + thr);
	}
	
	protected synchronized int getMax()
	//returns the maximum number of threads there should be allowed at one time
	{
		return maxThreads;
	}
	protected synchronized int getUsed()
	//returns the number of threads there are currently open
	{
		return usedThreads;
	}
	protected synchronized int getWiggle()
	//returns the amount of wiggle room that is built in before the code can't accept any more requests
	{
		return wiggleRoom;
	}
	public synchronized int getTotalRoom()		//was protected
	//returns all avalible room, wiggle and not, that is allowed before the code can't take any more requests
	{
		return wiggleRoom + maxThreads;
	}
	protected synchronized int getThreadID(Object obj)
	{
		return holdingIDs.get(obj);
	}
	
	/*protected synchronized ArrayBlockingQueue<Integer> getQueue(int spot)
	//returns the queue that is at the chosen spot.
	{
		return mySpot.get(spot);
	}
	
	protected synchronized boolean crossCheck(Thread sender, int spot) throws IllegalCredentialsException
	//goes and sees if thread has put into queue a dying message and the spot for it is still open. If both, sets that open spot to false and removes the message. returns true if dying is found, and false if none is found
	{
		if (sender.equals(cleanupCrew))
		{
			if (open[spot] && !mySpot.get(spot).isEmpty())
			{
				setOpen(false, spot, this);
				mySpot.get(spot).clear();
				return true;
			}
			else
				return false;
		}
		else
			throw new IllegalCredentialsException();
	}
	*/

	public synchronized boolean isOpen(int spot) throws IndexOutOfBoundsException		//was protected
	//returns if that spot is currently open. If the spot is outside the index, throws an exception
	{
		if(spot < open.length)
			return open[spot];
		else
			throw new IndexOutOfBoundsException();
	}
	protected synchronized void setOpen(boolean onOrOff, int spot, Object sender) throws IllegalCredentialsException, IndexOutOfBoundsException, InterruptedException
	//will set a thread to be currently on or off, should only ever need to be called by this class, and will throw an exception if it isn't
	{
		//System.out.println("" + Thread.currentThread() +": The Sender is: " + sender + " and I am: " + this);
		if(!Thread.currentThread().isInterrupted())
		{
			if(sender.equals(this))	//if this class was the caller
			{
				//System.out.println("" + Thread.currentThread() +": Well, Authentication wasn't the problem");
				System.out.println("" + Thread.currentThread() +": So, the spot is: " + spot + " and the length is: " + open.length);
				if(spot < open.length)	//if the spot given is a valid spot
					open[spot] = onOrOff;	//set spot to either on or off
				else
					throw new IndexOutOfBoundsException();
				//System.out.println("" + Thread.currentThread() +": It isn't even trying to add something to open! what could cause this much problems?");
			}
			else
				throw new IllegalCredentialsException();
			}
		else
			throw new InterruptedException();
	}
	protected synchronized void setOpen(boolean onOrOff, int spot, Thread sender) throws IllegalCredentialsException, IndexOutOfBoundsException
	//does the exact same thing as previous, but specifically for the cleanup Thread
	{
		if(sender.equals(cleanupCrew))	//if this class was the caller
		{
			if(spot < open.length)	//if the spot given is a valid spot
				open[spot] = onOrOff;	//set spot to either on or off
			else
				throw new IndexOutOfBoundsException();
		}
		throw new IllegalCredentialsException();
	}
	
	public synchronized boolean canAddThread()		//was protected
	//determines if there is space to add another thread, however uses the usedThreads int, only returns true/false and can be called by anyone
	{
		if (usedThreads < getTotalRoom())
			return true;
		else
			return false;
	}
	protected synchronized int addThreadSpot(Object sender) throws IllegalCredentialsException
	//determines if there is any space to add a thread, and if there is it returns the spot and returns -1 if not, should only be called by this class
	{
		if(sender.equals(this))
		{
			for (int i = 0; i < open.length; i++)
			{
				if(open[i])
					return i;
			}
			return -1;
		}
		throw new IllegalCredentialsException();
	}
	public synchronized int addThread(RemoteCommand sender, Object key) throws IllegalCredentialsException, IndexOutOfBoundsException, InterruptedException		//was protected
	//Will change the information so that a thread is added. Should only be called by RemoteCommand. Returns spot if successful. should be called before actually creating a thread.
	{
		if(sender.equals(rem))		//if the class can be authenticated to be the proper RemoteCommand
		{
			if (canAddThread())	//if there is enough room to add another thread
			{
				int spot = 0;
				try	//tries to add a thread if possible. if some Authentication went wrong, will print that it did and then return that the addition failed
				{	
					//System.out.println("" + Thread.currentThread() +": I'm about to try and find if there is a thread spot");
					spot = addThreadSpot(this);
					System.out.println("" + Thread.currentThread() +": Apparently there is! it is: " + spot);
					if(spot != -1)	//if there is a spot in what is open to add a thread, adds a thread. and if not, returns that it failed
					{
						System.out.println("" + Thread.currentThread() +": Ok, doing all the necessary things");
						setOpen(false, spot, this);
						System.out.println("" + Thread.currentThread() +": Set open");
						usedThreads++;
						System.out.println("" + Thread.currentThread() +": Taken threads up by 1");
						holdingIDs.put(key, spot);
						System.out.println("" + Thread.currentThread() +": Added the ID, which is: " + spot);
						holdingPeople.put(spot, key);
						return spot;
					}
					else
						throw new IndexOutOfBoundsException("There is not enough room to execute at this time");
				}
				catch (IllegalCredentialsException e)
				{
					System.out.println("" + Thread.currentThread() +": Something went wrong with the authentication of adding new threads. Must say no.");
					throw e;
				}
				catch(InterruptedException e)
				{
					System.out.println(Thread.currentThread() + " has been interruped while trying to create another thread");
					throw e;
				}
			}
			else
				throw new IndexOutOfBoundsException("" + Thread.currentThread() +": There is not enough room to execute at this time");
				
		}
		throw new IllegalCredentialsException();
	}

	public synchronized void deleteThread(int spot, Object key) throws InterruptedException		//was protected
	//this will delete the thread from the queue
	{
		try
		{
			setOpen(true, spot, this);
			holdingIDs.remove(key);
			holdingPeople.remove(spot);
			usedThreads--;
		}
		catch (IllegalCredentialsException e)
		{
			System.out.println("" + Thread.currentThread() +": Authentication failure prevented deletion of thread.");
		}
		catch (InterruptedException a)
		{
			System.out.println(Thread.currentThread() + "died while trying to deleteThread");
			throw a;
		}
	}
	public synchronized void deleteThread(int ID, Thread cleaning) throws IllegalCredentialsException, InterruptedException		//was protected
	//this will delete the thread from the queue
	{
		if(cleaning.equals(cleanupCrew))
			try
			{
				setOpen(true, ID, this);
				holdingIDs.remove(holdingPeople.get(ID));
				holdingPeople.remove(ID);
				usedThreads--;
			}
			catch (IllegalCredentialsException e)
			{
				System.out.println("" + Thread.currentThread() +": Authentication failure prevented deletion of thread.");
			}
			catch (InterruptedException a)
			{
				System.out.println(Thread.currentThread() + "died while trying to deleteThread");
				throw a;
			}
		else
			throw new IllegalCredentialsException("" + Thread.currentThread() +": Authentication failure prevented deletion of thread.");
	}
}
