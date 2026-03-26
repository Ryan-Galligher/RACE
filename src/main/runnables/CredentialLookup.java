package main.runnables;

import java.util.HashMap;

import main.databases.CommonVariables;
import main.dtos.Credentials;

public class CredentialLookup implements Runnable
{
	Credentials credit;	//this is the reference to the Credentials where if the person is indeed verified or not is stored for the StartExecuting to see.
	boolean verified;
	CommonVariables common;	//this is the reference to common, which the StartExecuting thread is waiting inside of
	
	HashMap<String, String> hostnamesAndLogins = new HashMap<String, String>();
	private String myHostName; 	//what will be used as a key when adding it into the Credentials

	
	public CredentialLookup(Credentials cre, CommonVariables com, String name)
	{
		credit = cre;
		common = com;
		myHostName = name;
		hostnamesAndLogins.put("10.20.3.154", "ryang^-^windows:CT:Garitena");
		hostnamesAndLogins.put("192.168.174.129", "ryan^-^sshpass:H3LL0@");
	}
	
	private void verify(String hostname)	//This is the method that should go and connect to the database and figure out if the hostname is verified or not. Sets verified to true and grabs the login information if the host is verified
	{
		if(hostnamesAndLogins.containsKey(hostname))
		{
			verified = true;
			System.out.println("" + Thread.currentThread() +": I found the credentials! They are " + hostnamesAndLogins.get(hostname) + " And the hostName is: " + hostname);
		}
		else
			verified = false;
	}
	
	private synchronized void tell()	//reports back to commonVariables and the StartExecuting Thread (which should be waiting at this time) that the answer was found, either yes or no
	{
		try
		{
			if(verified)
			{
				credit.set(myHostName, 'Y');
				credit.setPath(hostnamesAndLogins, myHostName);
			}
			else
				credit.set(myHostName, 'N');
			
			common.tell();
		}
		catch(InterruptedException e)
		{
			return;
		}
	}

	public void run()	//called when the thread is started
	{
		verify(myHostName);
		tell();
	}

}
