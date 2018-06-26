package cli;

import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;


/**
 * Unit test for Cli and consequently callbacks/filters
 * @author janez
 *
 */
public class CliTest {
	// static variables so that they can be initialized in BeforeClass - before tests are executed
	//static String inFile = "generated.s2";
	//static String inDir  = "." + File.separator + "Generated";

	/*
	static String inDir  = "." + File.separator + "Original";
	static String inFile = "test2.s2";
	static String inCSV = "izpis2.csv";
	static String inTXT = "info2.txt";
	static String inDirName = inDir +File.separator+ inFile;
	 */

	static String inDir  = "." + File.separator + "Generated";
	static String inFile = "generated.s2";
	static String inCSV = "generated.csv";
	static String inTXT = "generated.txt";
	static String inDelayed = "generatedDelayed.csv";
	static String inDirName = inDir +File.separator+ inFile;

	static String inDirNameSignal1 = "." + File.separator + "S2files" + File.separator+ "andrej1.s2";
	static String inDirNameSignal2 = "." + File.separator + "S2files" + File.separator+ "andrej2.s2";

	static String outDir = "." + File.separator + "UnitTests";



	//static String inDirNameO = "."+File.separator+"Original"+File.separator+"test2.s2";


	public static Collection<Object[]> data() {
		//return new String[]{"test1.s2", "test2.s2", "test3.s2"};
		//return new String[]{"test2.s2"};

		return Arrays.asList(new Object[][]{{"generated.s2"}});
	}


	@BeforeClass
	public static void initialize() {
		assertTrue("testing existence of directory of corect answers",new File(inDir).exists());

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

	
	//***********************             TEST PIPELINES                            *******************************
	
	
	
	@Test
	public void testChangeDateTime()
	{
		
	}
	
	
	
	//***********************            TEST CLI FUNCTIONALITIS                    *******************************
	
	
	@Test
	public void StatisticsTest()
	{

		Cli.start(new String[]{"-s" ,"-i", inDirName, "-o", outDir+File.separator+ "StatistikaOriginala.txt"});

		File correct = new File(inDir + File.separator + inTXT);
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
		{
			Cli.start(new String[]{"-r", "-i", inDirName, "-o", outDir + File.separator + "IzpisOriginala.csv"});
			File corect = new File(inDir + File.separator + inCSV);
			File testing = new File(outDir + File.separator + "IzpisOriginala.csv");
			boolean isTwoEqual = false;
			try {
				isTwoEqual = FileUtils.contentEquals(corect, testing);

			} catch (IOException e) {
				e.printStackTrace();
			}
			assertTrue("without time",isTwoEqual);
		}

		{
			Cli.start(new String[]{"-r", "-i", inDirName, "-o", outDir + File.separator + "IzpisOriginalaT.csv",
					"-t", "0", "1"});
			File corect = new File(inDir + File.separator + inCSV);
			File testing = new File(outDir + File.separator + "IzpisOriginala.csv");
			boolean isTwoEqual = false;
			try {
				isTwoEqual = FileUtils.contentEquals(corect, testing);

			} catch (IOException e) {
				e.printStackTrace();
			}
			assertTrue("with time",isTwoEqual);
		}

	}

	@Test
	public void outS2Test()
	{

		checkCopy();
		//first 3 lines are struct definitions

		//samo za generated
		if(inFile.equals("generated.s2"))
		{
			assertEquals("Testing singletons time 17",1+1, checkSingleton(17L));
			assertEquals("Testing singletons time 16",1+3, checkSingleton(16L));
			assertEquals("Testing singletons time 15",1+0, checkSingleton(15L));
		}
		else
		{
			long time = 8855834815L;
			assertEquals("Testing singletons time "+time,1+1, checkSingleton(time));
		}

		deleteUnimportant(false,"111");
		//deleteUnimportant(true,"100");


	}


	private void checkCopy() {

		//do copy of original
		Cli.start(new String[]{"-c", "-i", inDirName, "-o", outDir +File.separator+ "KopijaOriginala.s2"});

		//read copy
		Cli.start(new String[]{"-r", "-i", outDir +File.separator+ "KopijaOriginala.s2",
				"-o", outDir +File.separator+ "IzpisKopije.csv"});

		//compare copy.csv with corect.csv
		File corect = new File(inDir + File.separator + inCSV);
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
		Cli.start(new String[]{"-c", "-i", inDirName, "-o", outDir +File.separator+ nameS2,
				"-t", Double.toString(cutTime*1E-9),Double.toString((cutTime+1)*1E-9), "true"});
		Cli.start(new String[]{"-r", "-i", outDir +File.separator+ nameS2, "-o", outDir +File.separator+ nameCSV});

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
		Cli.start(new String[]{"-c", "-i", inDirName, "-o", outDir +File.separator+  name+".s2",
				"-d", d, "-t", "0", "1", "true"});
		Cli.start(new String[]{"-s", "-i", outDir +File.separator+ name+".s2",
				"-o", outDir +File.separator+ name+".txt" });

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
		divideAndConquer(14L);//just before timestamp
		divideAndConquer(15L);//on timestamp
		divideAndConquer(16L);//just after timestamp
		divideAndConquer(17L);//on middle data
		divideAndConquer(19L);//on last data
		divideAndConquer(20L);//after data

		divideAndConquer(200000000000L); // 200-ta sekunda

		//nesmiselno za pravo datoteko(ne da se mi rezat prave datoteke :D)
		//cutMiddle(6L,17L);

	}

	private void cutMiddle(long head, long tail) {
		Cli.start(new String[]{"-c", "-i", inDirName, "-t", "0", Double.toString(head*1E-9), "true",
				"-o", outDir +File.separator+ "head"+head+".s2"});
		//t is false
		Cli.start(new String[]{"-c", "-i", inDirName, "-t", Double.toString(tail*1E-9), "1", "false",
				"-o", outDir +File.separator+ "tail"+tail+".s2"});
		//Order of head/tail is deliberatly reversed.
		Cli.start(new String[]{"-m", "true", "-i", outDir +File.separator+ "tail"+tail+".s2",
				outDir +File.separator+ "head"+head+".s2",
				"-o", outDir +File.separator+ "cutedMiddle.s2"});

		Cli.start(new String[]{"-r", "-i", outDir +File.separator+ "cutedMiddle.s2",
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
	private void divideAndConquer(long cutTime)
	{
		//cut S2 at 
		Cli.start(new String[]{"-c", "-i", inDirName, "-t", "0", Double.toString(cutTime*1E-9), "true",
				"-o", outDir +File.separator+ "izrezek1_"+cutTime+".s2"});

		Cli.start(new String[]{"-c", "-i", inDirName, "-t", Double.toString(cutTime*1E-9), Double.toString(1E5), "true",
				"-o", outDir +File.separator+ "izrezek2_"+cutTime+".s2"});
		
		//forward                  1,2
		
		Cli.start(new String[]{"-m", "true", "-i", outDir +File.separator+ "izrezek1_"+cutTime+".s2",
				outDir +File.separator+ "izrezek2_"+cutTime+".s2",
				"-o", outDir +File.separator+ "SestavljenaF"+cutTime+".s2"});
		
		Cli.start(new String[]{"-r", "-i", outDir +File.separator+ "SestavljenaF"+cutTime+".s2",
				"-o", outDir +File.separator+ "IzpisSestavljeneF"+cutTime+".csv"});
		
		//reverse order            2,1
		
		Cli.start(new String[]{"-m", "true", "-i", outDir +File.separator+ "izrezek2_"+cutTime+".s2",
				outDir +File.separator+ "izrezek1_"+cutTime+".s2",
				"-o", outDir +File.separator+ "SestavljenaR"+cutTime+".s2"});
		
		Cli.start(new String[]{"-r", "-i", outDir +File.separator+ "SestavljenaR"+cutTime+".s2",
				"-o", outDir +File.separator+ "IzpisSestavljeneR"+cutTime+".csv"});


		File correct = new File(inDir + File.separator + inCSV);
		File testingF = new File(outDir + File.separator + "IzpisSestavljeneF"+cutTime+".csv");
		File testingR = new File(outDir + File.separator + "IzpisSestavljeneR"+cutTime+".csv");


		try {
			boolean isTwoEqualF = FileUtils.contentEquals(correct, testingF);
			boolean isTwoEqualR = FileUtils.contentEquals(correct, testingR);
			assertTrue("Cutting at time " + cutTime + " ", isTwoEqualF & isTwoEqualR);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testChangeTimeStamps()
	{
		String tem = "zamaknjeniTimestamps";
		Cli.start(new String[]{"-"+Cli.CHANGE_TIME, "10", "-i", inDirName, "-o", outDir +File.separator+ tem+".s2"});
		
		Cli.start(new String[]{"-r", "-i", outDir +File.separator+ tem+".s2",
				"-o", outDir +File.separator+ tem+".csv"});
		
		File correct = new File(inDir + File.separator + inDelayed);
		File testing = new File(outDir +File.separator+ tem+".csv");
		
		
		try {
			boolean isTwoEqual = FileUtils.contentEquals(correct, testing);
			assertTrue("Delaying for 10 ns " , isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGenerator2()
	{
		//1-10 s
		Cli.start(new String[] {"-g", "10", "125", "0.1", "0.1", "1000000", "0.05", "10", "0", "-t", "1", "10",
				"-o", outDir +File.separator+ "generatedRan.s2"});
		
		assertTrue("generating S2 PCARD", new File(outDir +File.separator+ "AndroidgeneratedRan.s2").exists());
		assertTrue("generating S2 PCARD", new File(outDir +File.separator+ "MachinegeneratedRan.s2").exists());
	
	}


	/*
	@Test
	public void SignalTest2()
	{
		final double metrikaR = 1.0530298123870087E8;
		String ime1 = "andrej1-popravljena.s2";
		String ime2 = "andrej2-popravljena.s2";
		String out1 = outDir +File.separator+ ime1;
		String out2 = outDir +File.separator+ ime2;
		Cli.start(new String[]{"-p", "-i", inDirNameSignal1, "-o", out1, "-t", "0","3600"});
		Cli.start(new String[]{"-p", "-i", inDirNameSignal2, "-o", out2, "-t", "0","3600"});

		Runner r = new Runner();
		double R = r.unitRun(outDir,ime1, ime2);


		//assertTrue("je blizu ? : ",R <=1.5*metrikaR);
		//assertTrue("je boljše ? : ",R <=metrikaR);
	}
	 */

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
