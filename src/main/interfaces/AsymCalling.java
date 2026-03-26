package main.interfaces;

public interface AsymCalling
{

	void failed(Exception e);	//tells thread that RemoteCommand broke and there were problems, from illegal credentials, too much traffic to unable to connect to remote computer
	void finished(Object callMeBackWithThis);	//this tells thread that nothing in the Remote command code broke, doesn't mean the remote command worked

}
