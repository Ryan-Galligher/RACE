package main.interfaces;

import java.util.HashMap;
//import java.util.Set;

import main.exceptions.IllegalCredentialsException;

public abstract class InformationHolders		//was package protected
{
	protected HashMap<String, Long> time2 = new HashMap<String, Long>();
	private Thread cleanupCrew;
	
	public synchronized void verifyLooking(Thread thr)		//was protected
	{
		if (cleanupCrew == null)
		{
			cleanupCrew = thr;
		}
		//System.out.println("" + Thread.currentThread() +": I got the message from " + thr);
	}

	public synchronized Long getTime(String key)		//was protected
	{
		//System.out.println("" + Thread.currentThread() +": I'm looking at the String HashMap");
		return time2.get(key);

	}
	
	public synchronized void setTime(String key)	//was protected
	{
		time2.put(key, System.currentTimeMillis());
	}

	public synchronized String[] findStrKeys(Thread thr) throws IllegalCredentialsException		//was protected
	{
		if (cleanupCrew.equals(thr))
			return time2.keySet().toArray(new String[0]);
		else
			throw new IllegalCredentialsException();
	}
	
}
