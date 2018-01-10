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
		
		//TODO it cant delete .s2 files
		/*
		if(result.wasSuccessful())
		{
			String outDir = "." + File.separator + "UnitTests";
			try {
				FileUtils.deleteDirectory(new File(outDir));
			} catch (IOException e) {
				System.err.println("we canot delete " + outDir);
				e.printStackTrace();
			}
		}*/
	
	}

}
