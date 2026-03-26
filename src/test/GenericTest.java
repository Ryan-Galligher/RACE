package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;



import main.exceptions.IllegalCredentialsException;
import main.dtos.RemoteArgs;
import main.RemoteCommand;

public class GenericTest {



	public static void main(String[] args) 
	{
		String me = "myExecute";
		String key = "HelloMello";
		RemoteCommand remote = new RemoteCommand();
		TestAsym myasym = new TestAsym(remote);
		TestAsym asymmetry = new TestAsym(remote);
		TestAsym asymmetry1 = new TestAsym(remote);
		TestAsym asymmetry2 = new TestAsym(remote);
		ArrayList<RemoteArgs> array = new ArrayList<RemoteArgs>();
		ArrayList<RemoteArgs> array1 = new ArrayList<RemoteArgs>();
		ArrayList<RemoteArgs> array2 = new ArrayList<RemoteArgs>();
		ArrayList<RemoteArgs> array3 = new ArrayList<RemoteArgs>();
		array.add(new RemoteArgs("cmd /c dir \\windows\\system32\\drivers\\etc", false));
		array1.add(new RemoteArgs("echo chicked nuggets are delicious", false));
		array2.add(new RemoteArgs("df -k", false));
		array3.add(new RemoteArgs("ls -la", false));

		

		//for(int i = 0; i < 5; i++)

		//{
			try
			{

				//for(int a = 0; a < 10; a++)
				//{
					//remote.executeAsym(myasym, array, "ANDEVER");

				//}

				

				//remote.executeAsym(asymmetry, array, "10.20.3.154");

				remote.executeAsym(myasym, array1, "192.168.174.129");

				//System.out.println("" + Thread.currentThread() +": ASYMMETRY MODE HAS BEEN INITIALIZED! PREPARE TO DIE");

				//System.out.println("" + Thread.currentThread() +": ASYMMETRY MODE NUMBER 2 HAS BEEN INITIALIZED! WHAT HAVE I DONE? WHAT HAVE YOU DONE!!!");
				String key1 = null;
				try
				{
					key1 = remote.execute(array2, "192.168.174.129");
				}
				catch (Exception e)
				{
					//do something
				}
					
				remote.getOut(key1);
				/*System.out.println("" + Thread.currentThread() +": I got past Thread 1!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

				//remote.killAllNow();

				remote.execute(key, array, "DeathByComputers");

				System.out.println("" + Thread.currentThread() +": I got past Thread 2!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

				//remote.executeAsym(AsymCalling, "cd /etc/sudoers.d", "WhyAmIHere.lol");

				System.out.println(remote.getStatus(me));

				System.out.println(remote.getError(key));

				System.out.println(remote.getOut(me));

				 */

				throw new NullPointerException();

				

			} catch (IllegalStateException /*| IllegalCredentialsException | InterruptedException  | TimeoutException*/ e) {

				e.printStackTrace();

			}
			//throw new InterruptedException();

	//	}

		try 

		{
			Thread.currentThread();
			Thread.sleep(1000);

		} catch (InterruptedException e) {

			e.printStackTrace();

		}
	}	
}