package cli;

import org.apache.commons.io.FileUtils;

import static org.junit.Assert.*;

import org.junit.Test;

import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.IOException;

@RunWith(Parameterized.class)
public class CliTest {
	String inFile;

	String inDir  = "." + File.separator + "Generated";
	String outDir = "." + File.separator + "UnitTests";


	public CliTest(String inFile)
	{
		this.inFile = inFile; 
	}

	@Parameterized.Parameters
	public static String[] data() {
		//return new String[]{"test1.s2", "test2.s2", "test3.s2"};
		//return new String[]{"test2.s2"};
		return new String[]{"generated.s2"};

	}
	
	@Test
	public void StatisticsTest()
	{
		Cli.main(new String[]{"-s", "-i", inDir, inFile, "-o", outDir, "StatistikaOriginala.txt"});
		
		File corect = new File(inDir + File.separator + "generated.txt");
		File testing = new File(outDir + File.separator + "StatistikaOriginala.txt");
		
		try {
			boolean isTwoEqual = FileUtils.contentEquals(corect, testing);
			assertEquals(true, isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		

	}
	
	

	@Test
	public void OutCSVTest()
	{
		Cli.main(new String[]{"-r", "-i", inDir, inFile, "-o", outDir, "IzpisOriginala.csv"});
		
		File corect = new File(inDir + File.separator + "generated.csv");
		File testing = new File(outDir + File.separator + "IzpisOriginala.csv");
		boolean isTwoEqual = false;
		try {
			isTwoEqual = FileUtils.contentEquals(corect, testing);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertEquals(true, isTwoEqual);
	}

	@Test
	public void outS2Test()
	{
		//do copy of original
		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-o", outDir, "KopijaOriginala.s2"});

		//read copy
		Cli.main(new String[]{"-r", "-i", outDir, "KopijaOriginala.s2",
				"-o", outDir, "IzpisKopije.csv"});
		
		//compare copy.csv with corect.csv
		File corect = new File(inDir + File.separator + "generated.csv");
		File testing = new File(outDir + File.separator + "IzpisKopije.csv");
		boolean isTwoEqual = false;
		try {
			isTwoEqual = FileUtils.contentEquals(corect, testing);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertEquals(true, isTwoEqual);
		
	}

	@Test
	public void buildTest()
	{
		//cut S2 at 
		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-t", "0", Double.toString(17*1E-9),
				"-o", outDir, "izrezek1.s2"});

		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-t", Double.toString(17*1E-9), "1",
				"-o", outDir, "izrezek2.s2"});

		Cli.main(new String[]{"-m", "true", "-i", outDir, "izrezek1.s2",
				"-v", outDir, "izrezek2.s2",
				"-o", outDir, "Sestavljena.s2"});

		Cli.main(new String[]{"-r", "-i", outDir, "Sestavljena.s2",
				"-o", outDir, "IzpisSestavljene.csv"});

		
		File corect = new File(inDir + File.separator + "generated.csv");
		File testing = new File(outDir + File.separator + "IzpisSestavljene.csv");
		
		
		try {
			boolean isTwoEqual = FileUtils.contentEquals(corect, testing);
			assertEquals(true, isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


}
