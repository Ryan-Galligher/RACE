package main.databases;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import main.RemoteCommand;
import main.dtos.Credentials;
import main.dtos.RemoteArgs;
import main.exceptions.IllegalCredentialsException;
import main.exceptions.MyUncaughtExceptionHandler;
import main.remote.SSHRunner;
import main.remote.WindowsRunner;
import main.runnables.Cleanup;
import main.runnables.CredentialLookup;
import main.runnables.StartExecutions;
import main.interfaces.AsymCalling;

public class CommonVariables
{
	private ExecutorService exec;	//used to manage all the threads that go and connect to machines
	private ExecutorService lookUp;	//used to manage all the credentialLookups. will only allow one at a time to go
	private ExecutorService execExecutes;	//used to manage all of the threads checking on/creating other threads
	private Future<?>[] futures;	//used to still have references to all the threads, in case need to shut them all down quickly
	Thread cleanupCrew;		//the Cleanup Thread
	private HoldMaps threadMap;	//creates the method so all threads can access their needed value
	private HoldAllMaps allMaps;	//holds all the completed hashMaps
	private HoldingQueue queues;	//holds all the information on threads, from their id & whose running to how much space is left.
	private Credentials verify;		//holds all of the results from the credentials thread.
	private boolean interrupted = false;
	private RemoteCommand rem;
	private ArrayList<String> failed = new ArrayList<String>();
	
	private final int nThreads = 5;	//max number of threads allowed to execute at once
	private int nThreadsTrying = 0;	//current number of StartExecuting threads that are either waiting in line or currently executing
	private final int nThreadsAllowed = 15;	//number of StartExecuting threads allowed to be both executing and waiting in queue
	
 	public CommonVariables(RemoteCommand rem)	//initializes all of the classes used constantly throughout the code
	{
		threadMap = new HoldMaps();
		allMaps = new HoldAllMaps();
		queues = new HoldingQueue(rem);
		verify = new Credentials();
		this.rem = rem;
		
		cleanupCrew = new Thread(new Cleanup(verify, allMaps, rem, queues, this));
		exec = Executors.newFixedThreadPool(queues.getTotalRoom());
		futures  = new Future<?>[queues.getTotalRoom()];
		lookUp = Executors.newSingleThreadExecutor();	//used for CredentialLookup. Only allows 1 thread to be active and dealing with a database at a time
		execExecutes = Executors.newFixedThreadPool(nThreads);
		
		Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
		//cleanupCrew.setDaemon(true);
		cleanupCrew.start();
	}

 	public synchronized void executeExecutors(Object key, ArrayList<RemoteArgs> argument, String hostname, boolean isStr, RemoteCommand remoteCommand, CountDownLatch latch)		//was protected
 	//starts and manages the StartExecutings threads, which are the threads that go and check the credentials of 
 	{
 		try
 		{
	 		if(!Thread.currentThread().isInterrupted() && !interrupted)	//If the thread isn't interrupted and wasn't told to stop getting any more requests.
	 		{
	 			System.out.println("" + Thread.currentThread() +": I'm in the Common Variables");
	 			if(nThreadsTrying <= nThreadsAllowed)	//If there are less threads trying to execute code than the maximum allowed amount, try and fire up a StartExecute
	 			{
	 				nThreadsTrying++;
	 				execExecutes.execute(new StartExecutions(this, rem, latch, argument, hostname, isStr, key));
	 			}
	 			else	//If there are too many threads trying to start a remote connection at once, tell them they can't
	 			{
	 				System.out.println("NO, bad " + Thread.currentThread());
	 				throw new IllegalStateException("Too many people trying to execute remote commands at once");
	 			}
	 			latch.countDown();
	 		}
	 		else
	 			throw new InterruptedException();
 		}
 		catch (Exception e)	//if some Exception happened in the attempt to Start an Executor, try to notify those who need to know
 		{
 			if (!isStr)	//if the caller is from the async version, call the user and tell them it failed. If it is the blocking version, tell it that the execution failed and release it from the latch
 			{
 				callUser(key, hostname, e);
 			}
 			else
 				tellFail(key.toString(), latch);
 		}
 	}
 	
 	private void callUser(Object caller, String hostname, Exception e)	//if Async was used, then calls the caller back and tells them what exception killed their attempt
	{
		((AsymCalling) caller).failed(e);

	}
 	
	protected synchronized HoldMaps getThreadMap() throws InterruptedException
	{
		if(!Thread.currentThread().isInterrupted() && !interrupted)	//if the thread hasn't been interrupted and wasn't told to no longer accept attempts
			return threadMap;	//returns the class that holds the ThreadLocal map variables
		else
			throw new InterruptedException();
	}
 	public synchronized boolean isFutureDone(int spot) throws InterruptedException		//was protected
	//returns if the thread that spawned that future is done and returns if it (true) is or isn't (false)
	{
 		if(!Thread.currentThread().isInterrupted() && !interrupted)	//if not interrupted and is still allowed to take requests.
 		{
	 		try
	 		{
	 			return futures[spot].isDone();
	 		}
	 		catch(NullPointerException e)	//If the Thread has been properly dealt with and has been removed from the futures spot, then say that it was taken care of
	 		{
	 			return true;
	 		}
 		}
 		else
			throw new InterruptedException();
	}

	public synchronized void tellFail(String obj, CountDownLatch latch)		//was protected
	//This is called when Either the StartExecuting, CredentialLookup or the Connection Threads fail, adds the object key to the failed category. Specifically for blocking version.
	{	//this does not check to see if the thread is interrupted, because if it was then it still needs to be registered as dead before it can die
			failed.add(obj);
			while(latch.getCount() > 0)
			{
				latch.countDown();
			}
			nThreadsTrying--;
	}
	public synchronized boolean hasFailed(String obj) throws InterruptedException		//was protected
	//Called when the Remote class want to know if thread(s) responsible have failed or not. removes them from the failed list if found.
	{
		if(!Thread.currentThread().isInterrupted() && !interrupted)
		{
			if(failed.contains(obj))
			{
				failed.remove(obj);
				return true;
			}
			else
				return false;
		}
		else
			throw new InterruptedException();
	}
 	
 	public synchronized void executeThread(ArrayList<RemoteArgs> argument, String hostname, String loginStuff, int threadID, Object obj, boolean isSSH, CountDownLatch latch, boolean isStr) throws InterruptedException		//was protected
 	//Called by the StartExecute Thread after it has confirmed that the host can be accessed by this program. Then starts up new Runner thread, based off of if it is connecting to SSH or Windows
 	{
 		if(!Thread.currentThread().isInterrupted() && !interrupted)
		{
 			if(isSSH) //if it is an SSH, starts it as an ssh. Else, starts it as a Windows
 				futures[threadID] = exec.submit(new SSHRunner(argument, hostname, loginStuff, rem, threadID, obj, threadMap, allMaps, queues, this, latch, isStr));
 			else
 				futures[threadID] = exec.submit(new WindowsRunner(argument, hostname, loginStuff, rem, threadID, obj, threadMap, allMaps, queues, this, latch, isStr));
 			nThreadsTrying--;	//since StartExecute is starting to shutDown now, say there is spot for one more
		}
 		else
			throw new InterruptedException();
	}
 	
 	public synchronized void executeCredentials(String hostname) throws InterruptedException		//was protected
 	//StartExecute threads call this code if there isn't already an instance of the needed credentials stored temporarily in Credentials. Fires off CredentialLookup thread, which will look up the credentials
 	{
		if(!Thread.currentThread().isInterrupted() && !interrupted)
		{
			lookUp.execute(new CredentialLookup(verify, this, hostname));	//starts a credential thread that will report back when finished
			waitingForCredentials(hostname);	//makes sure that the thread goes right into waiting for the response
		}
		else
			throw new InterruptedException();
 	}
 	protected synchronized void waitingForCredentials(String obj) throws InterruptedException
 	//StartExeccute threads call this right after they call an executeCredentials
 	{
 		long time = System.currentTimeMillis();
 		//System.out.println("The string I am using to look and see if the stuff is there is: " + obj);
 		while(verify.get(obj.toString()) == 'O' && 60000 > System.currentTimeMillis() - time)	//while waiting for the proper alert, wait. Will wait until it realizes that the thread has been waiting for a minute with no response
			wait();
 		if(60000 <= System.currentTimeMillis() - time)
 			throw new InterruptedException("The Credentials could not be found within a minute");
 	}
 	public synchronized void tell()		//was protected
 	//Used to notify all threads that are waiting in CommonVariables (currently only used while waiting for credentialLookup). can be called either by the CredentialsLookup or the Cleanup (so StartThread can eventually realize if past time if CredentialLookup dies prematurely)
 	{
 		notifyAll();
 	}
 	
 	
	public synchronized HoldAllMaps getMaps(RemoteCommand remote) throws IllegalCredentialsException		//was protected
	{
		if(remote.equals(rem) && !Thread.currentThread().isInterrupted() && !interrupted)
			return allMaps;
		else
			throw new IllegalCredentialsException();
	}
	public synchronized Credentials getCredentials()	//was protected
	{
		return verify;
	}
	public synchronized HoldingQueue getQueue()	//was protected
	{
		return queues;
	}

	private synchronized boolean isRemote(RemoteCommand remote)	//checks and makes sure that the RemoteCommand given is the proper instance of RemoteCommand
	{
		if(remote.equals(rem))
			return true;
		else
			return false;
			
	}
	
	public synchronized void end(Object key, RemoteCommand remote) throws IllegalCredentialsException, InterruptedException
	//ends the connection, closes down everything associated with this key
	{
		if(!Thread.currentThread().isInterrupted() && !interrupted)	//If everything is still allowed to take requests
		{
			if(isRemote(remote))	//If the caller is the correct remote class
			{
				try
				{
					int ID = queues.getThreadID(key);	//finds the ID of the thread that had tried to execute the command
					kill(ID, remote);	//if the thread isn't done yet, try and kill the thread
					queues.deleteThread(ID, key);	//make sure that the Thread spot has been opened up for other threads
					System.out.println("" + Thread.currentThread() +": Successfully removed Thread");
				}
				catch(NullPointerException e)
				{
					System.out.println("" + Thread.currentThread() +": There was nothing in the queues so it tried to die on me. NOPE.");
				}
				try
				{
					allMaps.remove(key.toString());	//Will go into the class that holds all of the out, err and status and remove those associated with this person
					verify.remove(key);	//removes the caller from credentials of allowed NOTE: THIS WILL ALWAYS CAUSE AN ERROR DUE TO WRONG TYPE OF KEY (NOT HOSTNAME)
					System.out.println("" + Thread.currentThread() +": Successfully removed Maps and Credentials");
				}
				catch(NullPointerException e)
				{
					System.out.println("" + Thread.currentThread() +": There was nothing in the Maps/Credentials so it tried to die on me. NOPE.");
				}
			}
			else	//If this wasn't called by someone with the correct instance of RemoteCommand
				throw new IllegalCredentialsException("" + Thread.currentThread() +": Please use the key that you gave to set up the connection");
		}
		else
			throw new InterruptedException();
	}


	public synchronized void kill(int threadID, RemoteCommand remote)	//Tries to kill the given thread		//was protected
	{
		if(isRemote(remote))
		{
			try
			{
				futures[threadID].cancel(true);
			}
			catch (NullPointerException e) {}	//If the thread isn't there, then capture the exception so it doesn't cause any further problems
		}
	}
	public synchronized void killAll(RemoteCommand remote)		//was protected
	//Tries to slowly kill all the threads by letting them get to a flag in code and exit properly
	{
		if(isRemote(remote))
		{
			exec.shutdown();
			lookUp.shutdown();
			execExecutes.shutdown();
			for(Future<?> fut : futures)
			{
				try	//tries and ends all of the threads that are currently running
				{
					fut.cancel(false);
				} catch(NullPointerException e){}	//if there is no thread in that spot, don't let it stop the code
			}
			System.out.println("Killing the CleanupCrew");
			cleanupCrew.interrupt();	//Stop the cleanupCrew
			interrupted = true;		//Don't allow any more requests
		}
	}
	public synchronized void killAllNow(RemoteCommand remote)	//Kills everything as soon as possible		//was protected
	{
		if(isRemote(remote))
		{
			interrupted = true;	//doesn't allow any more requests
			exec.shutdownNow();
			lookUp.shutdownNow();
			execExecutes.shutdownNow();
			for(Future<?> fut : futures)
			{
				try	//Shuts down everything harshly
				{
					fut.cancel(true);
				}
				catch(NullPointerException e) {}
			}
			cleanupCrew.interrupt();	//kills cleanupCrew
		}
	}

	
}
