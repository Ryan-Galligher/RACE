package test;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import main.interfaces.AsymCalling;
import main.exceptions.IllegalCredentialsException;
import main.dtos.RemoteArgs;
import main.RemoteCommand;

public class StartWritingCommandLineAlreadyRemoteCommandOpen //implements AsymCalling
{
	//Start writing basic version of CommandLine command. This commandLine command makes new remoteCommand, does blocking execute, and prints output
	
	RemoteCommand remote = new RemoteCommand();
	AsymCalling key = new TestAsym(remote);
	
	public static void main(String[] args)
	{
		try
		{
			ArrayList<RemoteArgs> arguments = new ArrayList<RemoteArgs>();
			String shouldBeHostname = args[0];
			
			for(int i = 0; i < args.length; i++)
			{
				arguments.add(new RemoteArgs(args[i], true));
			}
			
			//remote.executeAsym(key, arguments, shouldBeHostname);

			
		}
		catch (IllegalStateException /*| IllegalCredentialsException | InterruptedException | TimeoutException*/ e)
		{
			System.err.println(e);
			System.exit(1);
		}
		
	}

/*	public void failed(Exception e)
	{
		System.err.println(e);
		System.exit(1);
	}

	public void finished(Object callMeBackWithThis)
	{
		try
		{
			
			System.err.println(remote.getError(key));
			System.out.println(remote.getOut(key));
			System.out.println("Execution Status for executed command was: " + remote.getStatus(key));
			remote.end(key);
			
			System.exit(0);
		}
		catch (Exception e)
		{
			failed(e);
		}
	}
	*/

}
