package cli;

import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class CliTest {
	String inFile;

	// static variables so that they can be initialized in BeforeClass - before tests are executed
	static String inDir  = "." + File.separator + "Generated";
	static String outDir = "." + File.separator + "UnitTests";
	String inDirName;

	public CliTest(String inName) {
		this.inFile = inName; 
		this.inDirName = inDir + File.separator + inName;
	}

	@Parameterized.Parameters
	//public static String[] data() {
	public static Collection<Object[]> data() {
		//return new String[]{"test1.s2", "test2.s2", "test3.s2"};
		//return new String[]{"test2.s2"};

		return Arrays.asList(new Object[][]{{"generated.s2"}});
	}


	@BeforeClass
	public static void initialize() {
		assertTrue("testing existence of corect answers",new File(inDir).exists());
		// create the output directory for the files generated in these tests
		new File(outDir).mkdir();
		// note that 'inDir' should already be present or everything will fail
		// TODO: podobno kot beforeclass dodaj še after class in počisti cel outDir za sabo; ali pa morda čisti fajle sproti
	}

	@Test
	public void testInitialization() {
		File directory = new File(outDir);
		assertTrue(directory.exists());
	}

	@Test
	public void StatisticsTest()
	{

		Cli.main(new String[]{"-s" ,"-i", inDirName, "-o", outDir+File.separator+ "StatistikaOriginala.txt"});

		File correct = new File(inDir + File.separator + "generated.txt");
		File testing = new File(outDir + File.separator + "StatistikaOriginala.txt");

		try {
			boolean isTwoEqual = FileUtils.contentEquals(correct, testing);
			assertTrue(isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}



	}



	@Test
	public void OutCSVTest()
	{
		Cli.main(new String[]{"-r", "-i", inDirName, "-o", outDir + File.separator + "IzpisOriginala.csv"});

		File corect = new File(inDir + File.separator + "generated.csv");
		File testing = new File(outDir + File.separator + "IzpisOriginala.csv");
		boolean isTwoEqual = false;
		try {
			isTwoEqual = FileUtils.contentEquals(corect, testing);

		} catch (IOException e) {
			e.printStackTrace();
		}
		assertTrue(isTwoEqual);
	}

	@Test
	public void outS2Test()
	{

		checkCopy();
		//first 3 lines are struct definitions
		assertEquals("Testing singletons time 17",1+1, checkSingleton(17L));
		assertEquals("Testing singletons time 16",1+3, checkSingleton(16L));
		assertEquals("Testing singletons time 15",1+0, checkSingleton(15L));

		deleteUnimportant(false,"111");
		deleteUnimportant(true,"1000");
		deleteUnimportant(true,Integer.toString(-8));
		deleteUnimportant(true,"0");


	}


	private void checkCopy() {

		//do copy of original
		Cli.main(new String[]{"-c", "-i", inDirName, "-o", outDir +File.separator+ "KopijaOriginala.s2"});

		//read copy
		Cli.main(new String[]{"-r", "-i", outDir +File.separator+ "KopijaOriginala.s2",
				"-o", outDir +File.separator+ "IzpisKopije.csv"});

		//compare copy.csv with corect.csv
		File corect = new File(inDir + File.separator + "generated.csv");
		File testing = new File(outDir + File.separator + "IzpisKopije.csv");
		boolean isTwoEqual = false;
		try {
			isTwoEqual = FileUtils.contentEquals(corect, testing);

		} catch (IOException e) {
			e.printStackTrace();
		}
		assertTrue("Testing copy ", isTwoEqual);
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
		Cli.main(new String[]{"-c", "-i", inDirName, "-o", outDir +File.separator+ nameS2,
				"-t", Double.toString(cutTime*1E-9),Double.toString((cutTime+1)*1E-9), "true"});
		Cli.main(new String[]{"-r", "-i", outDir +File.separator+ nameS2, "-o", outDir +File.separator+ nameCSV});

		int lines = 0;
		try {
			BufferedReader bf = new BufferedReader(new FileReader(outDir + File.separator + nameCSV));

			while(bf.readLine() != null)
			{
				lines++;
			}
			bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return lines;
	}

	private void deleteUnimportant(boolean R,String d) {
		String name = "deletedUnimp";
		Cli.main(new String[]{"-c", "-i", inDirName, "-o", outDir +File.separator+  name+".s2","-d", d});
		Cli.main(new String[]{"-s", "-i", outDir +File.separator+ name+".s2", "-o", outDir +File.separator+ name+".txt" });

		try {
			BufferedReader bf = new BufferedReader(new FileReader(outDir + File.separator + name+".txt"));
			while(!bf.readLine().contains("Special messeges"))
			{

			}
			assertEquals("Testing keeping nonessential data ",R,bf.readLine().contains("Comments : 0"));
			bf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void buildTest()
	{

		divideAndConquer(0L); //additive identity of field
		divideAndConquer(2L); //before first data
		divideAndConquer(5L); //on first data
		divideAndConquer(10L);//betwen data
		divideAndConquer(17L);//on data
		divideAndConquer(19L);//on last data
		divideAndConquer(20L);//after data

		cutMiddle(6L,17L);

	}

	private void cutMiddle(long head, long tail) {
		Cli.main(new String[]{"-c", "-i", inDirName, "-t", "0", Double.toString(head*1E-9), "true",
				"-o", outDir +File.separator+ "head"+head+".s2"});
		//t is false
		Cli.main(new String[]{"-c", "-i", inDirName, "-t", Double.toString(tail*1E-9), "1", "false",
				"-o", outDir +File.separator+ "tail"+tail+".s2"});
		//Order of head/tail is deliberatly reversed.
		Cli.main(new String[]{"-m", "true", "-i", outDir +File.separator+ "tail"+tail+".s2",
				outDir +File.separator+ "head"+head+".s2",
				"-o", outDir +File.separator+ "cutedMiddle.s2"});

		Cli.main(new String[]{"-r", "-i", outDir +File.separator+ "cutedMiddle.s2",
				"-o", outDir +File.separator+ "cutedMiddle.csv"});

		File correct = new File(inDir + File.separator + "cutedMiddle.csv");
		File testing = new File(outDir + File.separator + "cutedMiddle.csv");


		try {
			boolean isTwoEqual = FileUtils.contentEquals(correct, testing);
			assertTrue("Testing if it cuts out middle correctly ", isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/** Cut original file at cutTime and reconstruct it again
	 * @param cutTime
	 */
	private void divideAndConquer(Long cutTime)
	{
		//cut S2 at 
		Cli.main(new String[]{"-c", "-i", inDirName, "-t", "0", Double.toString(cutTime*1E-9), "true",
				"-o", outDir +File.separator+ "izrezek1_"+cutTime+".s2"});

		Cli.main(new String[]{"-c", "-i", inDirName, "-t", Double.toString(cutTime*1E-9), "1", "true",
				"-o", outDir +File.separator+ "izrezek2_"+cutTime+".s2"});

		Cli.main(new String[]{"-m", "true", "-i", outDir +File.separator+ "izrezek1_"+cutTime+".s2",
				outDir +File.separator+ "izrezek2_"+cutTime+".s2",
				"-o", outDir +File.separator+ "Sestavljena"+cutTime+".s2"});

		Cli.main(new String[]{"-r", "-i", outDir +File.separator+ "Sestavljena"+cutTime+".s2",
				"-o", outDir +File.separator+ "IzpisSestavljene"+cutTime+".csv"});


		File correct = new File(inDir + File.separator + "generated.csv");
		File testing = new File(outDir + File.separator + "IzpisSestavljene"+cutTime+".csv");


		try {
			boolean isTwoEqual = FileUtils.contentEquals(correct, testing);
			assertTrue("Cutting at time " + cutTime + " ", isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//if something goes wrong we dont want to delete files

	@AfterClass
	public static void clean()
	{
		/*
		try {
			delete(new File(outDir));
		} catch (IOException e) {
			System.err.println("we canot delete " + outDir);
			e.printStackTrace();
		}*/
	}

	private static void delete(File file) throws IOException {

		for (File childFile : file.listFiles()) {

			if (childFile.isDirectory()) {
				delete(childFile);
			} else {
				if (!childFile.delete()) {
					throw new IOException();
				}
			}
		}

		if (!file.delete()) {
			throw new IOException();
		}
	}




}
