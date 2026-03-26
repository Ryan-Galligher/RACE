package main.exceptions;

public class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
{

	public void uncaughtException(Thread t, Throwable e)
	{
			System.out.println("Thread " + t + " has died from " + e);
			e.printStackTrace();
	}

}
