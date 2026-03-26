package main;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import main.databases.CommonVariables;
import main.dtos.RemoteArgs;
import main.exceptions.IllegalCredentialsException;
import main.interfaces.AsymCalling;


//Assumes that the client has a unique callback for each command that they run
//assumes that the client will intelligently give back the id that is given to them in the callback (if successful) to find the output of their code
//Currently can't SCP

public class RemoteCommand
{
	private CommonVariables common;
	private boolean interrupted = false;
	private CountDownLatch count = new CountDownLatch(3);
	private ThreadLocal<CountDownLatch> latch = new ThreadLocal<CountDownLatch>();
	
	private long defaultWaitTime = 30000;
	
	public RemoteCommand()
	{
		latch.set(count);
		common = new CommonVariables(this);
	}

	public synchronized void executeAsym(AsymCalling callBackClass, ArrayList<RemoteArgs> argument, String hostname)
	{
		if(!Thread.currentThread().isInterrupted() && !interrupted)	//if everything hasn't either already been interrupted or was just interrupted, continue
		{
			System.out.println("" + Thread.currentThread() +": Hello");
			
			int priority = Thread.currentThread().getPriority();		//These are here b/c if put into a while loop asking for Asym over and over and over, other internal connections can't happen, so code never able to reject some of these requests, and whole system deadlocks
			if(priority != Thread.MIN_PRIORITY)
				Thread.currentThread().setPriority((int)Math.round((float)priority / 2.0));		//if the current priority is not the lowest possible priority, then set the priority to half of what it currently is, rounded up.
			//System.out.println("The priority is: " + priority + " and (int)Math.round((float)priority / 2.0) equals: " + (int)Math.round((float)priority / 2.0));
			common.executeExecutors(callBackClass, argument, hostname, false, this, latch.get());	//tells CommonVariables to start 
			Thread.currentThread().setPriority(priority);	//resets the priority back to what it was
		}
		
	}
	
	public synchronized String execute(ArrayList<RemoteArgs> Argument, String hostname, long timeInMiliseconds) throws IllegalCredentialsException, IllegalStateException, InterruptedException, TimeoutException
	{
		String key = Thread.currentThread().getName();

		if(!Thread.currentThread().isInterrupted() && !interrupted)
		{
			System.out.println("" + Thread.currentThread() +": Hello");
			latch.set(new CountDownLatch(3));	//will reset the CountDownLatch, to make sure that this thread is properly notified;
			common.executeExecutors(key, Argument, hostname, true, this, latch.get());	//tells the CommonVariables to start an execution thread.
			latch.get().await(timeInMiliseconds, TimeUnit.MILLISECONDS);		//Since this is the blocking version, blocks (while holding the lock) for 3 min, or until the threads all complete their given task
	
			if(common.hasFailed(key))	//if the key is in the ArrayList of keys that had died prematurely, throws exception
				throw new InterruptedException("" + Thread.currentThread() +": Unable to complete request");
			System.out.println("" + Thread.currentThread() +": Expecting the key to be: " + key + " , and the command for finding keys says: " + common.getMaps(this).isKeyHere(key));
			if(!common.getMaps(this).isKeyHere(key))
				throw new TimeoutException();
		}
		else
			throw new IllegalStateException("" + Thread.currentThread() +": This session has been closed down");
		return key;
	}
	
	public synchronized String execute(ArrayList<RemoteArgs> Argument, String hostname) throws IllegalCredentialsException, IllegalStateException, InterruptedException, TimeoutException
	{
		return execute(Argument, hostname, defaultWaitTime);
	}
	
	protected synchronized void tell()	//to be called by another thread to tell any threads waiting in remote Command to wake up. Not used currently, using CountDownLatch for execute() currently
	{
		notifyAll();
	}
	
	
	//These are the getter methods. The ones that were asynchronous need the callMeBackWithThis (which they were given) and the blocking one requires the key that they put in.
	public synchronized String[] getError(Object callMeBackWithThis) throws IllegalStateException
	{
		try
		{
			return common.getMaps(this).get(callMeBackWithThis.toString(), "Error").split("\n");
		} catch (IllegalCredentialsException e)
		{
			e.printStackTrace();
			throw new IllegalStateException();
		}
	}
	public synchronized String[] getOut(Object callMeBackWithThis) throws IllegalStateException
	{
		try {
			return common.getMaps(this).get(callMeBackWithThis.toString(), "Out").split("\n");
		} catch (IllegalCredentialsException e) {
			e.printStackTrace();
			throw new IllegalStateException();
		}
	}
	public synchronized int getStatus(Object callMeBackWithThis) throws IllegalStateException
	{
		try {
			return Integer.parseInt(common.getMaps(this).get(callMeBackWithThis.toString(), "Status"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException();
		}
	}
	
	
	public synchronized void end(AsymCalling key) throws IllegalCredentialsException, InterruptedException
	//ends the connection, closes down everything associated with this key
	{
		common.end(key, this);
	}
	public synchronized void end(String key) throws IllegalCredentialsException, InterruptedException
	//ends the connection, closes down everything associated with this key
	{
		common.end(key, this);
	}

	
	protected synchronized void kill(int threadID)	//This method could be used by threads to kill a thread with it's id. Not currently used
	{
		common.kill(threadID, this);
	}
	public synchronized void killAll()	//To be called outside the project to tell the project to shut everything down as soon as possible
	{
		interrupted = true;
		common.killAll(this);
	}
	public synchronized void killAllNow()	//To be called from outside the project, tells Threads to all shut down now, and to throw exceptions to stop them
	{
		common.killAllNow(this);
		interrupted = true;
	}

}
