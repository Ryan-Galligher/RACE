package main.runnables;

import java.util.Random;

import main.RemoteCommand;
import main.databases.CommonVariables;
import main.databases.HoldAllMaps;
import main.databases.HoldingQueue;
import main.dtos.Credentials;
import main.exceptions.IllegalCredentialsException;

public class Cleanup implements Runnable
{
	private Credentials credit;
	private HoldAllMaps maps;
	private HoldingQueue queues;

	private final long milliseconds = 30000;	//add 2 zeros after initial testing so it goes at normal pace - put back
	private final long allowedMapTime = 900000;
	private final long allowedCreditTime = 400000;

	private RemoteCommand remote;
	private CommonVariables common;
	
	private String[] keyList2Credit;
	private String[] keyList2Maps;
	
	private int threadsFoundDead = 0;
	
	public Cleanup(Credentials cre, HoldAllMaps hap, RemoteCommand rem, HoldingQueue que, CommonVariables com)	//can't verify here, cause still attached to main, and won't recognize when his own thread starts
	{
		credit = cre;
		maps = hap;
		queues = que;
		//Thread.currentThread().setDaemon(true);
		//Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
		remote = rem;
		common = com;
		System.out.println("Hello there! I'm the Cleanup Thread!");

		Random me = new Random();
		me.nextInt(10);
		
	}

	private void lookAtLogs(String[] obj, boolean isMap)	//looks at the log information, and if it is past a certain time, remove it
	{
		
		Thread.yield();
		try
		{
			for (int i = 0; i < obj.length; i++)	//for every item in the array
			{
				//System.out.println("Well, In cleanup Crew, the current time is: " + System.currentTimeMillis() + " , the length of the object array is: " + obj.length + " , i is: " + i + " , isStr is: " + true + " , and the array is: " + obj[i]);
				if(isMap)	//if it is the map
				{
					if (allowedMapTime < System.currentTimeMillis() - maps.getTime(obj[i]))
					{
						System.out.println("I removed something, I removed: " + obj[i]);
						maps.remove(obj[i]);
						i--;
					}
				}
				else	//if it is credentials
				{
					if (allowedCreditTime < System.currentTimeMillis() - credit.getTime(obj[i]))
					{
						System.out.println("I removed something, I removed: " + obj[i]);
						credit.remove(obj[i]);
						i--;
					}
				}
				
				Thread.yield();
			}
		}
		catch(InterruptedException e)
		{
			System.out.println("There was a problem, and the thread was interrupted");
		}
	}
	
	private void lookAtActiveThreads()
	//will go in and look at all of the threads that are supposed to be currently alive, and see if there are still threads there. If not, will set the open at that spot to 0
	{
		boolean isOpen;
		
		try
		{
			for(int i = 0; i < queues.getTotalRoom(); i++)
			{
				isOpen = queues.isOpen(i);
				if (!isOpen && common.isFutureDone(i))
				{
					System.out.println("CleanupThread!: found a thread that is supposibly dead, his ID is: " + i);
					threadsFoundDead++;
					queues.deleteThread(i, Thread.currentThread());
				}
				Thread.yield();
			}
		}
		catch (IllegalCredentialsException e)
		{
			System.out.println("Someone has gotten in and overriden the Cleanup Crew!");
			remote.killAllNow();
		}
		catch (InterruptedException a)
		{
			a.printStackTrace();
			return;
		}
	}
	
	public void run()
	{
		credit.verifyLooking(Thread.currentThread());
		maps.verifyLooking(Thread.currentThread());
		queues.verifyLooking(Thread.currentThread());
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY - Thread.MIN_PRIORITY);
		
		while(!Thread.currentThread().isInterrupted())
		{
			
			try
			{
				try
				{
					keyList2Credit = credit.findStrKeys(Thread.currentThread());
					keyList2Maps = maps.findStrKeys(Thread.currentThread());
				}
				catch (IllegalCredentialsException e)
				{
					System.out.println("Someone has gotten in and overriden the Cleanup Crew!");
				}
				
				Thread.yield();
				
				//System.out.println("keyList2Credit with false, true is up next");
				lookAtLogs(keyList2Credit, false);
				
				Thread.yield();
				
				//System.out.println("keyList2Map with false, false is up next");
				lookAtLogs(keyList2Maps, true);
				
				Thread.yield();
				
				lookAtActiveThreads();
				
				Thread.yield();
				
				common.tell();//the reason the cleanup crew will call this periodically is so that startExecute threads that sit and wait forever cause the CredentialLookup threads die can check to see if they are over the time limit
				
			}catch(NullPointerException a)
			{
				System.out.println(Thread.currentThread() + " almost died from " + a + " but did not");
				a.printStackTrace();
			}
			
			System.out.println(Thread.currentThread() + "Says: All Clean!");
			
			try
			{
				Thread.currentThread();
				Thread.sleep(milliseconds);
			}
			catch (Exception e)
			{
				return;
			}
		}
		
	}
	
	public int getThreadsFoundDead()
	{
		return threadsFoundDead;
	}



}
