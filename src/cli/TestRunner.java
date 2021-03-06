package cli;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;


/**
 * What is the purpose for this class ...
 */
public class TestRunner {

	public static void main(String[] args) {
		
		Result result = JUnitCore.runClasses(CliTest.class);
		
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}

		System.out.println("result was " + (result.wasSuccessful() ? "successful" : "not successful"));
	}

}
