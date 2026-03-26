package main.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import main.RemoteCommand;
import main.databases.CommonVariables;
import main.databases.HoldAllMaps;
import main.databases.HoldMaps;
import main.databases.HoldingQueue;
import main.dtos.RemoteArgs;
import main.exceptions.IllegalCredentialsException;
import main.interfaces.Talker;

public class WindowsRunner extends Talker implements Runnable
{
	private String standardError = "";	//used to hold the standard Error
	private String standardOut = "";		//used to hold the output
	private int status = 1;				//used to hold the status
	private long waitTime = 1;	//wait time in minutes
	
	private ProcessBuilder pb;
	private String username = "";
	private String domainName = "";
	private String password = "";
	private String hostname = "";
	private String command = "";
	
	public WindowsRunner(ArrayList<RemoteArgs> argument, String hostname, String loginInformation, RemoteCommand remote, int threadID, Object obj, HoldMaps threadMap, HoldAllMaps all, HoldingQueue queue, CommonVariables comm, CountDownLatch count, boolean isStr)
	{
		super(argument, hostname, loginInformation, remote, threadID, obj, threadMap, all, queue, comm, count, isStr);
	}

	public void sendMessage() throws InterruptedException, IllegalCredentialsException, ConnectException		//was package protected
	{
		try
		{
			this.hostname = super.host;
			
			System.out.println("Breaking up the information given and sorting it");
			splitInfo();
			
			Thread.yield();
			
			convertArgumentToString();
			command = super.command;
			
			List<String> allTogether = new ArrayList<String>();	//sets up the List that is given to the ProcessBuilder to use to execute winexe
			allTogether.add("/mbs/sbin/winexe");
			allTogether.add("--debug-stderr");	//no say in matter, must stay. This makes sure that the Errors and the Outputs are separated
			allTogether.add("--uninstall");
			allTogether.add("-U");
			allTogether.add(domainName + "/" + username + "%" + password);
			System.out.println(allTogether.get(4));
			allTogether.add("//" + hostname);
			System.out.println(allTogether.get(5));
			allTogether.add(command);
			System.out.println(allTogether.get(6));
	
			pb = new ProcessBuilder(allTogether);	//creates the processBuilder
			pb.redirectErrorStream(false);	//makes sure that the Error and Output streams are not combined
			
			Process p = pb.start();	//starts the execution, and saves the currently executing thing as a Process
			if(!p.waitFor(waitTime, TimeUnit.MINUTES))	//waits for the process for a minute to finish. If not finished within that amount of time, assumed that it died in a horrible way
				throw new InterruptedException("The Command could not be executed within a minute");
			
			BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));	//gets a reader that can read the Out and error from the process
			BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			Thread.yield();
			
			String line;
			while((line = error.readLine()) != null)	//Keeps looking through the stuff in the pipe until there is nothing left to pull out
			{
				standardError += line;
			}
			error.close();
			
			Thread.yield();
			
			while((line = output.readLine()) != null)
			{
				standardOut += line;
			}
			output.close();
			
			Thread.yield();
		
			
			try
	        {
		        status = p.exitValue();	//tries to set the status. If can't get the real status, makes best guess
	        }
	        catch(NullPointerException e)
	        {
	        	e.printStackTrace();
	        	if(!standardOut.equals("") && standardError.equals(""))
					status = 0;
				else
					status = 1;
	        }
			
			
			System.out.println("Disk info: " + standardOut + " And the Error was: " + standardError + " And the Status is: " + status);
			super.setValue("Error", standardError);	//adds all the information into the Hashmap
			super.setValue("Out", standardOut);
			super.setValue("Status", "" + status);
		
		}
		catch(InterruptedException | IOException e)
		{
			e.printStackTrace();
			super.died(e);
		}
	}
	
	
	
	private void splitInfo()	//splits up the large string of information into small segments
	{
		String[] stuff = login.split("\\^");
		for(int i = 0; i < stuff.length; i++)
			System.out.println(stuff[i]);
		username = stuff[0];
		String[] otherStuff = stuff[2].split(":");
		domainName = otherStuff[1];
		password = otherStuff[2];
		System.out.println(stuff[0] + " " + domainName + " " + password);
		
	}

	public void run()
	{
		super.run();
	}

}
