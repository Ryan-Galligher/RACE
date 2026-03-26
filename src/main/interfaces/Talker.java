package main.interfaces;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import main.RemoteCommand;
import main.databases.CommonVariables;
import main.databases.HoldAllMaps;
import main.databases.HoldMaps;
import main.databases.HoldingQueue;
import main.dtos.RemoteArgs;
import main.exceptions.IllegalCredentialsException;

import java.net.ConnectException;



public abstract class Talker		//was package protected

{
	private int threadID;

	private RemoteCommand rem;
	private CommonVariables common;

	private HoldingQueue queues;
	protected HoldMaps map;		//[filled with out, err and status as key]
	private HoldAllMaps maps;
	private Object caller;
	private boolean isObj;
	private CountDownLatch latch;
	private String Key;		//this is the key that is generated based on unique things between asym callers, given to them so they can find their stuff again
	protected String command = "";	//needs to be set in child class, The command that would be run, used for Key

	protected String host;
	protected String login;
	protected ArrayList<RemoteArgs> arguments;
	boolean died = false;	//boolean that remembers if you called the died() method once before. If you have, don't call it again
	boolean called = false;	//boolean thatj remembers if you told the user that their information was there or not, so it doesn't call a user to say the thread died unnaturally if they already got their information
	Exception KillerException;	//holds the exception that killed the thread if there is one

	

	

	public Talker(ArrayList<RemoteArgs> argument, String hostname, String loginInformation, RemoteCommand remote, int threadID, Object obj, HoldMaps threadMap, HoldAllMaps all, HoldingQueue queue, CommonVariables com, CountDownLatch count, boolean isStr)

	{

		rem = remote;
		this.threadID = threadID;
		caller = obj;
		isObj = !isStr;
		maps = all;
		map = threadMap;
		queues = queue;
		host = hostname;
		login = loginInformation;
		arguments = argument;
		common = com;
		latch = count;

		System.out.println("" + Thread.currentThread() +": I'm the Constructor for the new Thread. Here is everything: rem: +" + rem + " threadID: " + threadID + " caller: " + caller + " maps: " + maps + " map: " + map);

	}

	protected void convertArgumentToString() throws InterruptedException
	//converts the ArrayList<RemoteArgs> into a String based on if they need to have embedded quotes around them or not.
	{
		if(!Thread.currentThread().isInterrupted())	
		{
			for (int i = 0; i < arguments.size(); i++)
			{
				if(i != 0)	//so that the command that is sent to SSH or Windows is properly spaced out, there isn't an extra space at beginning or ending and all the separate arguments have spaces
					command += " ";
				if(!Thread.currentThread().isInterrupted())	//if the thread is interrupted, tell who needs to know that died and then kill self
				{
					if(!arguments.get(i).isInQuotes())
						command += arguments.get(i).getArgument();
					else
						command += ("\"" + arguments.get(i).getArgument() + "\"");
					Thread.yield();
				}
				else
					throw new InterruptedException();
			}
		}
		else
			throw new InterruptedException();
	}

	

	

	public abstract void sendMessage() throws InterruptedException, IllegalCredentialsException, ConnectException;		//goes out to SSH or Windows	//was package protected

	void Kill()

	//Try and End this Thread

	{
		Thread.currentThread().interrupt();
	}

	public void run()		//was protected
	//If blocking version, call output(String). Else, output(object). This runs everything
	{
		System.out.println("" + Thread.currentThread() +": Hello there! and welcome to the Thread that will be taking care of you today! My name is: " + threadID);
		try
		{
			map.startUpMap();	//starts up a map for the child, for some reason there is sometimes not a map when the child wants to add stuff, so makes sure there is one here
			System.out.println("" + Thread.currentThread() +": Sending Message...");
			Thread.yield();
			sendMessage();
			//System.out.println("Wonderful!");
			if(!died)
			{
				if(isObj)
					Output(caller);
				else
					Output(caller.toString());
			}
		}

		catch (InterruptedException e)	//if the code was interrupted during part of the execution

		{

			System.out.println("" + Thread.currentThread() +": There was a problem with the connection, terminating thread");
			died(e);
			return;

		}

		catch (IllegalCredentialsException e)	//if the credentials were incorrect and couldn't be verified

		{

			System.out.println("" + Thread.currentThread() +": There was a problem with the Authentication, terminating thread. Please try again.");
			died(e);
			return;

		}

		catch(ConnectException e)	//If there was a problem with the connection, say there was

		{

			System.out.println(Thread.currentThread() + ": There was a problem connecting to SSH. Terminating Thread");
			died(e);
			return;

		}

		catch(Exception e)	//if there is any other type of exception, say there was and then begin to kill the thread

		{
			System.out.println("" + Thread.currentThread() +": Other exception is trying to kill the thread");
			e.printStackTrace();
			died(e);
			return;
		}

	}

	

	

	protected void setValue(String type, String status)

	//takes either the out, the err or the status and add it to the HashMap

	{

		//System.out.println("What should be put in is: " + check(status));

		map.get().put(type, check(status));

		System.out.println("" + Thread.currentThread() +": Ok, Put in " + type + ", here's the map: " + map + " and here is the HashMap: " + map.get());

	}

	

	protected String check(String info) //[look for the \r and \n]

	// Will return the string of information given in line feed form (gets rid of \r than come from windows, not get rid of \n or \t

	{

		//System.out.println("before Replacing is: " + info);

		//System.out.println("After Replacing is: " + info.replace("\n", "").replace("\r", ""));

		return info.replace("\r", "");

	}



	protected void Output(String str) throws InterruptedException

	//Will put the HashMap into the HoldAllStringMaps, and use the String as the key. Will then go into tell() in main object {which notifies the object that the answer is ready}, Then calls endConnection(String) in main object, then calls Kill().

	{

		if(!Thread.currentThread().isInterrupted())
		{
			try
			{
				//System.out.println("Calling from String...");
				//System.out.println("" + Thread.currentThread() +": In the Thread, the futures think that my isDone is: " + common.isFutureDone(threadID));
				maps.set(str, map.get());
				System.out.println("" + Thread.currentThread() +": I set my Map, set as: " + str);
				//rem.tell();(since in this one the command is blocking, the thread must be able to go back to the RemoteCommand and tell it to wake up) *DEADLOCK* since countDownLatch is sitting there in RemoteCommand waiting for latch to hit 0, doesn't free up lock, so code sits here.
				latch.countDown();
				
				Thread.yield();
				
				queues.deleteThread(threadID, str);
				System.out.println("" + Thread.currentThread() +": And Scene");
				return;
			}
			catch(InterruptedException e)
			{
				System.out.println(Thread.currentThread() + ": Died Prematurely due to being interrupted.");
				died(e);
			}
		}
		else
			throw new InterruptedException();
	}

	

	protected void Output(Object obj) throws InterruptedException

	//Will put the HashMap into the HoldAllMaps, and use the caller as the key. Will then call in the main program the endConnection(caller), and should block until it gains access. Will then call the Kill()
	{
		if(!Thread.currentThread().isInterrupted())
		{
			try
			{
				//System.out.println("Calling from Object...");
				System.out.println("" + Thread.currentThread() +": In the Thread, the futures think I am: " + common.isFutureDone(threadID));
				Key = "" + host.hashCode() + command.hashCode();
				maps.set(Key, map.get());
				
				Thread.yield();
				
				//System.out.println("I set my map");
				callUser(true);
				latch.countDown();

				Thread.yield();

				queues.deleteThread(threadID, obj);
				System.out.println("" + Thread.currentThread() +": And Scene");
				return;
			}
			catch(InterruptedException e)
			{
				System.out.println(Thread.currentThread() + ": Died Prematurely due to being interrupted.");
				died(e);
			}
		}
		else
			throw new InterruptedException();

	}

	

	protected void died(Exception e)
	//If the Thread experiences any exceptions trying to do what it needs to do, then it tells whoever it needs to tell to say that it has died.
	{
		Thread.interrupted(); //this is here so that if the thread was interrupted, it will still be able to go and tell commonVariables that it is dead (if it is String)
		if(isObj)	//if it is a callback version, try and end it through calling back
		{
			try
			{
				died = true;
				KillerException = e;
				callUser(false);
				queues.deleteThread(threadID, caller);
				//Thread.currentThread().interrupt();
				return;
			}
			catch(Exception a)
			{
				a.printStackTrace();
				return;
			}
		}
		else	//else, tell the main part of the code that there was a serious problem and it needs to unblock and show there was a problem
		{
			try
			{
				died = true;
				common.tellFail(caller.toString(), latch);
				queues.deleteThread(threadID, caller.toString());
				//Thread.currentThread().interrupt();
				return;
			} catch (InterruptedException a) {
				a.printStackTrace();
				return;
			}
		}
	}

	

	private void callUser(boolean normal)
	{
		if(!called)	//if the user wasn't called before
		{
			if(normal)	//if the thread ended normally and everything is fine
				((AsymCalling) caller).finished(Key);
			else	//if not, then there was a serious problem and wasn't able to finish properly
				((AsymCalling) caller).failed(KillerException);
		}

		called = true;

	}

}

