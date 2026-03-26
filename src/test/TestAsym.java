package test;

import main.interfaces.AsymCalling;
import main.RemoteCommand;

public class TestAsym implements AsymCalling
{
	RemoteCommand remote;
	public TestAsym(RemoteCommand rem)
	{
		remote = rem;
	}
	
	public void failed(Exception e)
	{
		System.out.println("I was told by a little birdie that my thread failed. WHAT HAVE YOU DONE?");
		e.printStackTrace();
	}

	public void finished(Object callMeBackWithThis)
	{
		System.out.println("I was told that everything seems to be in order. This pleases me.");
		System.out.println("The callback is: " + callMeBackWithThis);
		System.out.println("The Error is: " + remote.getError(callMeBackWithThis));
		System.out.println("The Out is: " + remote.getOut(callMeBackWithThis));
		System.out.println("The Status is: " + remote.getStatus(callMeBackWithThis));
		//remote.killAllNow();
		//System.out.println("EVERYTHING SHOULD STOP NOW!! WHY IS IT NOT WORKING!! PAY ATTENTION TO ME!!");
	}

}
