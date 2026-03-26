package main.exceptions;

import java.lang.Exception;

public class IllegalCredentialsException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 112926948408342889L;
	public IllegalCredentialsException() { super(); }
	public IllegalCredentialsException(String message) { super(message); }
	public IllegalCredentialsException(String message, Throwable cause) { super(message, cause); }
	public IllegalCredentialsException(Throwable cause) { super(cause); }

}
