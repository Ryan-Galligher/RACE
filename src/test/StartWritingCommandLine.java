package test;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import main.exceptions.IllegalCredentialsException;
import main.dtos.RemoteArgs;
import main.RemoteCommand;

public class StartWritingCommandLine
{
		//Start writing basic version of CommandLine command. This commandLine command makes new remoteCommand, does blocking execute, and prints output
	public static void main(String[] args)
	{
		try
		{
			RemoteCommand remote = new RemoteCommand();
			ArrayList<RemoteArgs> arguments = new ArrayList<RemoteArgs>();
			String shouldBeHostname = args[0];
			
			for(int i = 0; i < args.length; i++)
			{
				arguments.add(new RemoteArgs(args[i], true));
			}
			
			String key = remote.execute(arguments, shouldBeHostname);
			
			System.err.println(remote.getError(key));
			System.out.println(remote.getOut(key));
			System.out.println("Execution Status for executed command was: " + remote.getStatus(key));
			remote.end(key);
			remote.killAll();
			
			System.exit(0);
			
		}
		catch (IllegalStateException | IllegalCredentialsException | InterruptedException | TimeoutException e)
		{
			System.err.println(e);
			System.exit(1);
		}
		
	}

}
