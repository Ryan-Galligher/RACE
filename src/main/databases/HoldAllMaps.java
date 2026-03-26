package main.databases;

//import ramp.IllegalCredentialsException;
import java.util.HashMap;
//import java.util.ArrayList;

import main.interfaces.InformationHolders;

public class HoldAllMaps extends InformationHolders
{
	/*
	 * Hashmap<caller object, HashMap<out/error/status , information from that>> me
	 * Hashmap<caller object, time the hashmap was inserted (used for cleanup)> time
	 * ArrayList<keys that will be used to help the cleanup>
	 * 
	 */
	
	private HashMap<String, HashMap<String, String>> me2;
	
	public HoldAllMaps()
	{
		me2 = new HashMap<String, HashMap<String, String>>();
	}
	

	public synchronized String get(Object key, String type)		//was protected
	{
		return me2.get(key.toString()).get(type);
	}

	public synchronized boolean isKeyHere(Object key)		//was protected
	//looks and sees if the Hashmap for that key is here yet.
	{
		return me2.containsKey(key.toString());
	}
	
	protected synchronized void set(Object key, String type, String value) throws InterruptedException
	{
		if(!Thread.currentThread().isInterrupted())
		{
			me2.get(key.toString()).put(type, value);
			setTime(key.toString());	//used to set the time that the item was placed in the hashmap, used by the Cleanup thread so it knows when something can be removed
		}
		else
			throw new InterruptedException();
	}

	public synchronized void set(Object key, HashMap<String, String> map) throws InterruptedException		//was protected
	{
		if(!Thread.currentThread().isInterrupted())
		{
			System.out.println("" + Thread.currentThread() + ": Setting up your map in my map");
			me2.put(key.toString(), map);
			setTime(key.toString());
		}
		else
			throw new InterruptedException();
	}


	public synchronized void remove(Object key) throws InterruptedException		//was protected
	{
		if(!Thread.currentThread().isInterrupted())
		{
			me2.remove(key.toString());
			time2.remove(key.toString());
		}
		else
			throw new InterruptedException();
	}
}
