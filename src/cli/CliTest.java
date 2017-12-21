package cli;

import static org.junit.Assert.*;

import org.junit.Test;

public class CliTest {

	@Test
	public void testMain() {
		String[] what = new String[]{"-s","-c","-r","-b"};
		String[] options = new String[]{"-i"};
		String inputDir = "C:\\Users\\janez\\workspace\\S2_rw\\IO\\Original";
		String[] inFiles = new String[]{"test2.s2"};
		String outDir = "C:\\Users\\janez\\workspace\\S2_rw\\UnitTests";
		String[] outFiles = new String[]{"Junit.txt"};
		

		
		String[] args = new String[]{what[0],options[0],inputDir,inFiles[0],outDir,outFiles[0]};
		Cli.main(args);
		//fail("Not yet implemented");
	}

}
