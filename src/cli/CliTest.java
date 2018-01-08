package cli;

import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*;

import org.junit.Test;

import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
		
		checkCopy();
		//first 3 lines are struct definitions
		assertEquals(3+1, checkSingleton(17L));
		assertEquals(3+3, checkSingleton(16L));
		assertEquals(3+0, checkSingleton(15L));
		

	}
	

	private void checkCopy() {
		
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

	
	/**
	 * For checking behavior of time interval
	 * @param cutTime
	 * @return number of lines in CSV of cutted file
	 */
	public int checkSingleton(long cutTime)
	{
		String nameS2 = "Singleton" + cutTime +".s2";
		String nameCSV = "Singleton" + cutTime +".csv";
		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-o", outDir, nameS2,
				"-t", Double.toString(cutTime*1E-9),Double.toString((cutTime+1)*1E-9)});
		Cli.main(new String[]{"-r", "-i", outDir, nameS2, "-o", outDir, nameCSV});
		
		int lines = 0;
		try {
			BufferedReader bf = new BufferedReader(new FileReader(outDir + File.separator + nameCSV));
			String line;
			while((line = bf.readLine()) != null)
			{
				lines++;
			}
			bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return lines;
	}

	@Test
	public void buildTest()
	{
		
		devideANDconcor(0L); //additive identity of field
		devideANDconcor(2L); //before first data
		devideANDconcor(5L); //on first data
		devideANDconcor(10L);//betwen data
		devideANDconcor(17L);//on data
		devideANDconcor(19L);//on last data
		devideANDconcor(20L);//after data
		
		cutMiddle(6L,17L);

	}

	private void cutMiddle(long head, long tail) {
		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-t", "0", Double.toString(head*1E-9),
				"-o", outDir, "head"+head+".s2"});
		
		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-t", Double.toString(tail*1E-9), "1",
				"-o", outDir, "tail"+tail+".s2"});
		
		Cli.main(new String[]{"-m", "true", "-i", outDir, "head"+head+".s2",
				"-v", outDir, "tail"+tail+".s2",
				"-o", outDir, "cutedMiddle.s2"});
		
		Cli.main(new String[]{"-r", "-i", outDir, "cutedMiddle.s2",
				"-o", outDir, "cutedMiddle.csv"});
		
		File corect = new File(inDir + File.separator + "cutedMiddle.csv");
		File testing = new File(outDir + File.separator + "cutedMiddle.csv");

		
		try {
			boolean isTwoEqual = FileUtils.contentEquals(corect, testing);
			assertEquals("Cuts out middle corectly " ,true, isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/** Cut original file at cutTime and reconstruct it again
	 * @param cutTime
	 */
	private void devideANDconcor(Long cutTime)
	{
		//cut S2 at 
		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-t", "0", Double.toString(cutTime*1E-9),
				"-o", outDir, "izrezek1_"+cutTime+".s2"});

		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-t", Double.toString(cutTime*1E-9), "1",
				"-o", outDir, "izrezek2_"+cutTime+".s2"});

		Cli.main(new String[]{"-m", "true", "-i", outDir, "izrezek1_"+cutTime+".s2",
				"-v", outDir, "izrezek2_"+cutTime+".s2",
				"-o", outDir, "Sestavljena"+cutTime+".s2"});

		Cli.main(new String[]{"-r", "-i", outDir, "Sestavljena"+cutTime+".s2",
				"-o", outDir, "IzpisSestavljene"+cutTime+".csv"});


		File corect = new File(inDir + File.separator + "generated.csv");
		File testing = new File(outDir + File.separator + "IzpisSestavljene"+cutTime+".csv");

		
		try {
			boolean isTwoEqual = FileUtils.contentEquals(corect, testing);
			assertEquals("Cuting at time " + cutTime + " ",true, isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
