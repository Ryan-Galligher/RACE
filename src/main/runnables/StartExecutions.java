package main.runnables;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import main.RemoteCommand;
import main.databases.CommonVariables;
import main.dtos.RemoteArgs;
import main.exceptions.IllegalCredentialsException;
import main.interfaces.AsymCalling;

//import java.util.concurrent.ExecutorService;
//mport java.util.concurrent.Future;

public class StartExecutions implements Runnable
{
	private CommonVariables common;
	private RemoteCommand rem;
	private CountDownLatch latch;
	//private boolean interrupted;
	
	private ArrayList<RemoteArgs> argument;
	private String hostname;
	private boolean isStr;
	private Object obj;
	
	public StartExecutions(CommonVariables com, RemoteCommand remote, CountDownLatch latch, ArrayList<RemoteArgs> argument2, String hostname, boolean isStr, Object key)
	{
		common = com;
		rem = remote;
		this.latch = latch;
		this.argument = argument2;
		this.hostname = hostname;
		this.isStr = isStr;
		obj = key;
	}

	
 	protected void startExecution () throws IllegalCredentialsException, IllegalStateException, InterruptedException
	{
		if (!Thread.currentThread().isInterrupted())	//if the code isn't supposed to die
		{
			boolean proceed = true;
			int tries = 0;	//represents the number of times that code has tried to add a new thread
			int threadID;
			boolean isSSH;
			String loginStuff = "";
			
			Thread.yield();
			
			if(getCredentials())	//if the credentials to the desired machine can be acquired
			{
				try		//try to figure out if it is SSH or Windows. has possibility of failing if deleted by another thread
				{
					isSSH = isSSH(hostname);	//tries to tell if the hostname is an ssh or not
					loginStuff = common.getCredentials().getPath(hostname);		//and tries to get the path to opening with the hostname
				} catch (Exception e1) {
					System.out.println("" + Thread.currentThread() +": Unknown error in checking if path is SSH or Windows.");
					throw new IllegalCredentialsException("Could not access the credentials to determine type of machine.");
				}
				do{	//will try and execute once, then will try again if it has to for up to 5 times. After that, it returns an exception saying that it is too busy
					tries++;
					
					if(Thread.currentThread().isInterrupted())
						throw new InterruptedException();
					
					Thread.yield();
					
					if(common.getQueue().canAddThread())	//quick check to see if it can add a thread
					{
						Thread.yield();
						try
						{
							threadID = common.getQueue().addThread(rem, obj);		//tries to add a thread. It is possible between then and now that something happened and can't add thread anymore, which is why in try-catch
							proceed = true;	//if it is able to successfully add the thread, makes sure that the loop is exitable and will not trigger an accidental exception on a successful connection
							tries = 0;
							common.executeThread(argument, hostname, loginStuff, threadID, obj, isSSH, latch, isStr);
							System.out.println("" + Thread.currentThread() +": Launching of the Threads Complete!");
						}
						catch (IndexOutOfBoundsException e)
						{
							proceed = false;
						}
					}
					
					Thread.yield();	//yields thread after either 1 successful or unsuccessful try at adding a thread.
					
				}while(!proceed && tries < 5);
				if(tries >= 5)
					throw new IllegalStateException("There is no room to take your request at this time.");
				
				latch.countDown();		//if the command was a blocking command, then it takes down the latch by 1 to show that it finished. Still needs the latch from the thread that this just spawned
			}
			else
				throw new IllegalCredentialsException("The authenticity of your destination is in question. Access Denied.");
		}
		else
			throw new InterruptedException();
	}
	

	private boolean getCredentials() throws InterruptedException
	//adds a key to Credentials and fills it as void and starts a CredentialLookup. Waits until notified and key has changed, and returns the change as true (yes) or false (no)
	{
		System.out.println("" + Thread.currentThread() +": Getting Credentials...");
		if(!Thread.currentThread().isInterrupted())
		{
			//System.out.println("" + Thread.currentThread() +": I'm in the if interrupted loop");
			try	//tries to grab the hostname and other information from the credentials. If they are there and it doesn't throw an error, return that there are credentials. If not, continue trying to verify
			{
				common.getCredentials().getPath(hostname);		
				System.out.println("" + Thread.currentThread() +": Hello, I'm past the verify without any exceptions for Thread: " + Thread.currentThread());
			}
			catch (Exception a)
			{
				System.out.println("" + Thread.currentThread() +": They aren't already here, creating new search... : ");
				Thread.yield();
				try
				{
					common.getCredentials().set(hostname, 'O');	//Tries to set the initial value that it will be checking for to O, so it won't have to deal with NullPointerException later on
					System.out.println("" + Thread.currentThread() +": Setting up Credentials thread now");
					Thread.yield();
					common.executeCredentials(hostname);	//will go and start the thread that looks for the credentials, and immediately goes and waits until it gets a response back
					if(common.getCredentials().get(hostname) == 'Y')	//if the results were that you can access that machine, return true. else, return false
						return true;
					else
						return false;
				}
				catch(InterruptedException e)	//if something interrupted the program, assume the answer is false
				{
					System.out.println("" + Thread.currentThread() +": Credentials were interrupted. Trying to Exit.");
					Thread.currentThread().interrupt();
					throw e;
				}
			}
			return true;	//if everything went right, return that everything is ok
		}
		else
			throw new InterruptedException();
	}

	private boolean isSSH(String hostname) throws Exception
	{
		if(common.getCredentials().getPath(hostname).contains("ssh"))	//if the keys to opening and accessing a machine includes the phrase SSH in it, executes as ssh
			return true;
		else
			return false;
	}
	
	
	public void run()
	{
		try
		{
			System.out.println("" + Thread.currentThread() +": Am I here for Symmetrical or Asymmetrical? (true=Sym, false=Asym): " + isStr);
			startExecution();
		}
		catch (IllegalStateException | IllegalCredentialsException | InterruptedException e)	//if there was a problem with starting up a new thread, tells someone that there was a major problem
		{
			e.printStackTrace();
			Thread.interrupted();	//this is here so that if the thread was interrupted, it will still be able to go and tell commonVariables that it is dead
			try
			{
				if(isStr)	//if it was a string, tell the commonVariables that it failed
					common.tellFail(obj.toString(), latch);
				else
					((AsymCalling) obj).failed(e);
			}
			catch(Exception a)		//if anything screws up, just print it out
			{
				a.printStackTrace();
			}
		}
	}
}
