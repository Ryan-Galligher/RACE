package main.dtos;

//import java.util.ArrayList;
import java.util.HashMap;

import main.interfaces.InformationHolders;

public class Credentials extends InformationHolders
{
	private HashMap<String, String> map2;
	private HashMap<String, String> paths;	//this is the hashmap where the "path" (username^-^sshpass/sshkey/windows... etc) is stored


	
	public Credentials()
	{
		map2 = new HashMap<String, String>();
		paths = new HashMap<String, String>();

	}
	
	public synchronized char get(String key)		//was protected
	{
		//System.out.println("The Key that will be used to find the credentials is: " + key);
		super.setTime(key);
		return map2.get(key).charAt(0);
	}
	public synchronized String getPath(String hostname) throws Exception	//was protected
	//takes the given hostname and returns the information on how to get to the host and what the host is (ex SSH;PathtoKey;Username;Password). throws exception if not there
	{
		try
		{
			super.setTime(hostname);
			String CityWok =  paths.get(hostname);
			if(CityWok != null)
				return CityWok;
			else
				throw new NullPointerException();
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	
	public synchronized void set(Object key, char verified) throws InterruptedException		//was protected
	{
		if(!Thread.currentThread().isInterrupted())
		{
			map2.put(key.toString(), "" + verified);
			super.setTime(key.toString());
			//keys.add(key);
		}
		else
			throw new InterruptedException();
	}

	public synchronized void setPath(HashMap<String,String> path, String hostname) throws InterruptedException		//was protected
	{
		if(!Thread.currentThread().isInterrupted())
		{
			paths.put(hostname, path.get(hostname));
			super.setTime(hostname);
		}
		else
			throw new InterruptedException();
	}
	
	public synchronized void remove(Object key)		//WAS protected
	{
		try
		{
			map2.remove(key);
		}
		catch(NullPointerException e)
		{
			paths.remove(key);
		}
		time2.remove(key.toString());
	}

}
