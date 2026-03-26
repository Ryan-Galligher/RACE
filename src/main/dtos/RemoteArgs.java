package main.dtos;

public class RemoteArgs
{
	private String argument;
	private boolean inQuotes;
	
	public RemoteArgs(String argument, boolean isInQuotes)
	{
		this.argument = argument;
		inQuotes = isInQuotes;
	}

	public boolean isInQuotes()
	{
		return inQuotes;
	}


	public String getArgument()
	{
		return argument;
	}
	
	

}
