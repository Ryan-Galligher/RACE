package main.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import main.RemoteCommand;
import main.databases.CommonVariables;
import main.databases.HoldAllMaps;
import main.databases.HoldMaps;
import main.databases.HoldingQueue;
import main.dtos.RemoteArgs;
import main.exceptions.IllegalCredentialsException;
import main.interfaces.Talker;


public class SSHRunner extends Talker implements Runnable
{
	private String standardError;	//used to hold the standard Error
	private String standardOut;		//used to hold the output
	private int status;				//used to hold the status
	
	private String hostname;	//hold the hostname/IP address, whichever given
	private String username;	//holds username
	private String password;	//holds password
	private char[] privateKey;	//holds the contents of the private key, which will be needed to connect to SSH if using keys
	private String pathToKey;	//holds the path to the private key
	private String command = "";		//holds the command in String format, will have stuff added in from the ArrayList<RemoteArgs>
	
	private String authenticateUsing;	//tells program which version of authentication is being used: none, key or password
	private Connection connection;	//Creates the connection with the SSH and authenticates itself

	public SSHRunner(ArrayList<RemoteArgs> argument, String hostname, String loginInformation, RemoteCommand remote, int threadID, Object obj, HoldMaps threadMap, HoldAllMaps all, HoldingQueue queue, CommonVariables com, CountDownLatch count, boolean isStr)
	{
		super(argument, hostname, loginInformation, remote, threadID, obj, threadMap, all, queue, com, count, isStr);	//sets up the object, yet calls the higher up class so Don't have to do it twice for SSH and Windows
	}

	public void sendMessage() throws InterruptedException, IllegalCredentialsException, ConnectException	//was package protected
	{
		this.hostname = super.host;

		
		System.out.println("Breaking up the information given and sorting it");
		splitInfo();
		
		try	//if there is no password given, changes the password to null just to make sure it doesn't screw up the Ganymede code
		{
			if(password.equals(""))
				password = null;
			if(pathToKey.equals(""))
				pathToKey = null;
		}
		catch(NullPointerException e){}
		
		Thread.yield();
	
		System.out.println("Setting which type of authentication it is");
		if(password == null && pathToKey == null)	//if there is no key and is no password, so basically there is no authentication, set it to none
			authenticateUsing = "none";
		else	//but if there is either a password of a key file
		{
			if(pathToKey != null)	//if the key file is the one that's there, say you are authenticating with a key
				authenticateUsing = "key";
			else	//if not using a key, you have to be using a password
				authenticateUsing = "password";
		}
		
		Thread.yield();
		try
		{
			System.out.println("Converting argument to String, the ArrayList Says: " + super.arguments.toString());
			convertArgumentToString();	//converts the ArrayList argument to a string so it can be used by SSH, up  in talker parent
			System.out.println("The command that will be given to the SSH is: " + command);
			command = super.command;
			
			Thread.yield();

			if(connect())	//tries to connect to the SSH specified.
			{
				if(executeCommand(command))	//tries to run the command and add all the information necessary 
				{
					System.out.println("Disk info: " + standardOut + " And the Error was: " + standardError + " And the Status is: " + status);
					super.setValue("Error", standardError);
					super.setValue("Out", standardOut);
					super.setValue("Status", "" + status);
				}
			}
		}
		catch (Exception e)
		{
			super.died(e);
		}
		
	}
	
	private void splitInfo()
	{
		String[] userAndOther = login.split("\\^-\\^");	//Splits the string into the username and the other information
		username = userAndOther[0];
		String[] infoSplit = userAndOther[1].split(":"); //Splits up the other half of the String into parts
		if(infoSplit[0].equals("sshpkey"))	//If the authentication requires a key, add key path and the password to the key
		{
			pathToKey = infoSplit[1];
			password = infoSplit[2];
		}
		else	//if not a key, then it should be a password
		{
			password = infoSplit[1];
		}
		
		
		/*for(int i = 0; i < infoSplit.length; i++)	//for each item in the String[], set the correct variable to that value
		{
			switch(i)
			{
				case 0: break;
				case 1: pathToKey = infoSplit[i]; break;
				case 2: password = infoSplit[i]; break;
			}
			System.out.println(infoSplit[i]);
			Thread.yield();
		}*/
	}

	
	private boolean connect() throws Exception	//tries to authenticate with SSH. If fails, an exception will be thrown. Must be used to start connection. see https://www.informit.com/guides/content.aspx?g=java&seqNum=489 for example
	{
		if(!Thread.currentThread().isInterrupted())
		{
			System.out.println("Trying to Connect. The hostname is: " + hostname);
			try
			{
				connection = new Connection(hostname);
				connection.connect();	//creates a connection with the SSH
				System.out.println("about to authenticate the connection");
				
				//All the AuthenticateWith return if they were successful in logging in or not. returns if they were successful in 
				boolean result = false;
				if (authenticateUsing.equals("password"))
					result = connection.authenticateWithPassword(username, password);
				if(authenticateUsing.equals("none"))
					result = connection.authenticateWithNone(username);
				if(authenticateUsing.equals("key"))
				{
					String contentsOfKey = "";
					try	//because for authenticating with a key, Ganymede expects the key to be passed in as a charArray. This goes into the computer and finds the file, reads all the data into a string, then gives Ganymede a char[] version
					{
						for(String line : Files.readAllLines(Paths.get(pathToKey)))
						{
							System.out.println(line);
							contentsOfKey += (line + "\n");	//needs to have embedded newLine character, else the guys code will throw an error.
						}
						System.out.println(contentsOfKey);
						privateKey = contentsOfKey.toCharArray();
					} catch (IOException e)
					{
						e.printStackTrace();
						super.died(e);
					}
					result = connection.authenticateWithPublicKey(username, privateKey , password);
				}
				System.out.println("Successful! The result is: " + result);
				return result;
			}
			catch( Exception e )
	        {
	            System.out.println("Exception in Connection"); 
	            e.printStackTrace();
	            throw e;
	        }
		}
		else
			throw new InterruptedException();
	}
	
	public boolean executeCommand(String com) throws Exception	//tries to execute the command, then code will take the output and put it into a readable string, and does the same thing for error. Will finally add the status
	{
		command = com;
		Session session = null;
		System.out.println("About to run command");
		try
		{
			session  = connection.openSession();
			 session.execCommand(command);
			
			 StringBuilder sb = new StringBuilder();
			 StringBuilder er = new StringBuilder();
	         InputStream stdout = new StreamGobbler( session.getStdout());
	         InputStream stderr = new StreamGobbler(session.getStderr());
	         BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
	         BufferedReader read = new BufferedReader(new InputStreamReader(stderr));
	         
	         System.out.println("About to add stuff to StringBuilders");
	         String line = br.readLine();
	         while( line != null )	//for every line in the BufferedReader, add it to a String Builder
	         {
	             sb.append( line + "\n" );
	             line = br.readLine();
	         }
	         String line1 = read.readLine();	//for every line in the other BufferedReader, add it to another String Builder
	         while( line1 != null )
	         {
	             er.append( line1 + "\n" );
	             line1 = read.readLine();
	         }
	         
	         br.close();	//closes all the things that are no longer needed
	         read.close();
	         session.close();
	         
	        standardOut = sb.toString();
	        standardError = er.toString();
	        
	        //Tries to add the status. Sometimes, the status will give a null-pointer exception. When it does, if there was output given and no errors, assumed that it completed fine
	        try
	        {
		        status = session.getExitStatus();
	        }
	        catch(NullPointerException e)
	        {
	        	e.printStackTrace();
	        	if(!standardOut.equals("") && standardError.equals(""))
					status = 0;
				else
					status = 1;
	        }
	        System.out.println("Finished Executing Successfully!");
	        return true;
		}
		catch (Exception e)	//If there was an uncaught exception, then say that the session couldn't be completed and begin shutting down thread
		{
			System.out.println("Session failed");
			e.printStackTrace();
			status = 1;
			
			try{session.close();}	//if there was a problem and the session didn't get to the part where it normally shuts down the Session, this makes sure that it is done
			catch(NullPointerException a){}
			
			throw e;
		}
	}

	public void run()	//runs the talker's version of run, since that deals with everything besides actually sending the message
	{
		super.run();
	}
}
