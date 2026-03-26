package main.databases;

import java.util.HashMap;

public class HoldMaps
{
	private ThreadLocal<HashMap<String,String>> map;
	
	public HoldMaps()
	{
		map = new ThreadLocal<HashMap<String,String>>();
	}
	
	public synchronized void startUpMap()		//was protected
	{
		map.set(new HashMap<String, String>());
	}
	
	public synchronized HashMap<String,String> get()		//was protected
	{
		return map.get();
	}

	protected synchronized String get(String type)
	{
		return map.get().get(type);
	}
	
	protected synchronized void set(String type, String value)
	{
		map.get().put(type, value);
	}
}
