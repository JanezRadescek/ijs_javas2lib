package cli;

import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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

	static String inDir  = "." + File.separator + "UnitInput";
	static String inFile = "generated.s2";
	static String inG3TXT = "generated3.txt";
	static String inCSV = "generated.csv";
	static String inINFO = "generatedINFO.txt";
	static String inTXT = "generatedTXT.txt";
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
	}
	
	@Before
	public void beforeTest()
	{
		File t = new File(outDir);
		if(!t.exists()) t.mkdir();
	}

	@Test
	public void testInitialization() {
		File directory = new File(outDir);
		assertTrue(directory.exists());
	}


	//***********************            TEST CLI FUNCTIONALITIS                    *******************************

	
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
		if(inFile.equals("generated.s2"))
		{
			cutMiddle(6L,17L);
		}
	}

	private void cutMiddle(long head, long tail) {
		Cli.start(new String[]{"-"+Cli.INPUT, inDirName, "-"+Cli.FILTER_TIME, "0", Double.toString(head*1E-9), "true",
				"-"+Cli.OUTPUT, outDir +File.separator+ "head"+head+".s2"});
		//t is false
		Cli.start(new String[]{"-"+Cli.INPUT, inDirName, "-"+Cli.FILTER_TIME, Double.toString(tail*1E-9), "1", "false",
				"-"+Cli.OUTPUT, outDir +File.separator+ "tail"+tail+".s2"});
		//Order of head/tail is deliberatly reversed.
		Cli.start(new String[]{"-"+Cli.MEARGE, "true", "-"+Cli.INPUT, outDir +File.separator+ "tail"+tail+".s2",
				outDir +File.separator+ "head"+head+".s2",
				"-"+Cli.OUTPUT, outDir +File.separator+ "cutedMiddle.csv"});

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
		Cli.start(new String[]{"-"+Cli.INPUT, inDirName, "-"+Cli.FILTER_TIME, "0", Double.toString(cutTime*1E-9), "true",
				"-"+Cli.OUTPUT, outDir +File.separator+ "izrezek1_"+cutTime+".s2"});

		Cli.start(new String[]{"-"+Cli.INPUT, inDirName, "-"+Cli.FILTER_TIME, Double.toString(cutTime*1E-9), Double.toString(1E5), "true",
				"-"+Cli.OUTPUT, outDir +File.separator+ "izrezek2_"+cutTime+".s2"});


		//reverse order            2,1

		Cli.start(new String[]{"-"+Cli.MEARGE, "true", "-"+Cli.INPUT, outDir +File.separator+ "izrezek2_"+cutTime+".s2",
				outDir +File.separator+ "izrezek1_"+cutTime+".s2",
				"-"+Cli.OUTPUT, outDir +File.separator+ "IzpisSestavljeneR"+cutTime+".csv"});

		File correct = new File(inDir + File.separator + inCSV);
		File testingR = new File(outDir + File.separator + "IzpisSestavljeneR"+cutTime+".csv");

		try {
			boolean isTwoEqualR = FileUtils.contentEquals(correct, testingR);
			assertTrue("Cutting at time " + cutTime + " ", isTwoEqualR);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void filterDataTest()
	{
		Cli.start(new String[] {"-"+Cli.INPUT, inDirName, "-"+Cli.FILTER_DATA, "1111", "-"+Cli.OUTPUT, outDir +File.separator+ "FilteredData.txt" });
		
		File corect = new File(inDir + File.separator + inTXT);
		File testing = new File(outDir + File.separator + "FilteredData.txt");
		try {
			boolean isTwoEqual = FileUtils.contentEquals(corect, testing);
			assertTrue("Testing filtering data ", isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void filterCommentsTest()
	{
		Cli.start(new String[] {"-"+Cli.INPUT, inDirName, "-"+Cli.FILTER_COMMENTS, "Te.*", "-"+Cli.STATISTIKA, outDir +File.separator+ "FilteredComments1.txt" });
		Cli.start(new String[] {"-"+Cli.INPUT, inDirName, "-"+Cli.FILTER_COMMENTS, "TeS.*", "-"+Cli.STATISTIKA, outDir +File.separator+ "FilteredComments2.txt" });
		
		File correct = new File(inDir + File.separator + inINFO);
		File testing1 = new File(outDir + File.separator + "FilteredComments1.txt");

		try 
		{
			boolean isTwoEqual = FileUtils.contentEquals(correct, testing1);
			assertTrue("filter comments",isTwoEqual);		
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		BufferedReader bf;
		try {
			bf = new BufferedReader(new FileReader(outDir + File.separator + "FilteredComments2.txt"));
			String s;
			while((s=bf.readLine()) != null)
			{
				if(s.contains("Comment"))
				{
					assertTrue(false);
					break;
				}
			}
			bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void filterSpecialTest()
	{
		//TODO this
	}
	
	
	@Test
	public void filterHandlesTest()
	{
		Cli.start(new String[] {"-"+Cli.INPUT, inDirName, "-"+Cli.FILTER_HANDLES, "001", "-"+Cli.STATISTIKA, outDir +File.separator+ "Filtered.txt" });
	
		BufferedReader bf;
		try {
			bf = new BufferedReader(new FileReader(outDir + File.separator + "Filtered.txt"));
			String s;
			while((s=bf.readLine()) != null)
			{
				if(s.contains("Number of streams"))
				{
					String[] sss = s.split("\\s+");
					boolean isE = sss[4].equals(""+1);
					assertTrue("we deleted all but one stream",isE);
					break;
				}
			}
			bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void filterTimeTest()
	{
		//testeed in singleton
	}
	
	
	@Test
	public void testChangeTimeStamps()
	{
		String tem = "zamaknjeniTimestamps";
		Cli.start(new String[]{"-"+Cli.CHANGE_TIME, "10", "-"+Cli.INPUT, inDirName, "-"+Cli.OUTPUT, outDir +File.separator+ tem+".csv"});


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
	public void testChangeDateTime()
	{
		String ime = "changedDateTime.txt";
		Cli.start(new String[] {"-"+Cli.INPUT, inDirName, "-"+Cli.CHANGE_DATE_TIME, "2018-01-01T10:30:10.554+0100", "-"+Cli.OUTPUT, outDir +File.separator+ ime});
		
		BufferedReader bf;
		try {
			bf = new BufferedReader(new FileReader(outDir + File.separator + ime));
			while(!bf.readLine().contains("Timestamp : time=1000015"))
			{

			}
			assertTrue("Testing changing dateTime",bf.readLine().contains("Stream Packet : handle=0, timestamp=1000016, bytes=[1, 2]"));
			bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	@Test
	public void SignalTest2()
	{
		//TODO 
		//final double metrikaR = 1.0530298123870087E8;
		String ime1 = "andrej1-popravljena.s2";
		//String ime2 = "andrej2-popravljena.s2";
		String out1 = outDir +File.separator+ ime1;
		//String out2 = outDir +File.separator+ ime2;
		Cli.start(new String[]{"-"+Cli.PROCESS_SIGNAL, "-"+Cli.INPUT, inDirName, "-"+Cli.OUTPUT, out1});
		assertTrue("Testing processing ",new File(out1).exists());
		
		//Cli.start(new String[]{"-p", "-i", inDirNameSignal2, "-o", out2, "-t", "0","3600"});

		//assertTrue("je blizu ? : ",R <=1.5*metrikaR);
		//assertTrue("je boljše ? : ",R <=metrikaR);
	}
	

	@Test
	public void StatisticsTest()
	{
		Cli.start(new String[]{"-"+Cli.STATISTIKA, outDir+File.separator+ "StatistikaOriginala.txt" ,"-"+Cli.INPUT, inDirName});

		File correct = new File(inDir + File.separator + inINFO);
		File testing = new File(outDir + File.separator + "StatistikaOriginala.txt");

		try {
			boolean isTwoEqual = FileUtils.contentEquals(correct, testing);
			assertTrue("testiranje statistike",isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Test
	public void OutTXTTest()
	{
		Cli.start(new String[]{"-"+Cli.INPUT, inDirName, "-"+Cli.OUTPUT, outDir + File.separator + "IzpisOriginala.txt"});

		File correct = new File(inDir + File.separator + inTXT);
		File testing = new File(outDir + File.separator + "IzpisOriginala.txt");

		try 
		{
			boolean isTwoEqual = FileUtils.contentEquals(correct, testing);
			assertTrue("testiranje izpisa TXT",isTwoEqual);		
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	
	@Test
	public void OutCSVTest()
	{
		{
			//copy
			Cli.start(new String[]{"-"+Cli.INPUT, inDirName, "-"+Cli.OUTPUT, outDir + File.separator + "IzpisOriginala.csv"});
			File corect = new File(inDir + File.separator + inCSV);
			File testing = new File(outDir + File.separator + "IzpisOriginala.csv");
			boolean isTwoEqual = false;
			try {
				isTwoEqual = FileUtils.contentEquals(corect, testing);
				assertTrue("without timeFilter",isTwoEqual);
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}

	
	@Test
	public void outS2Test()
	{
		checkCopy();

		if(inFile.equals("generated.s2"))
		{
			//samo za generated
			assertEquals("Testing singletons time 17",1+1, checkSingleton(17L));
			assertEquals("Testing singletons time 16",1+3, checkSingleton(16L));
			assertEquals("Testing singletons time 15",1+0, checkSingleton(15L));
		}
		else
		{
			//smiselno samo za neko pravo datoteko
			long time = 8855834815L;
			assertEquals("Testing singletons time "+time,1+1, checkSingleton(time));
		}

		deleteUnimportant(false,"111");
		//deleteUnimportant(true,"100");


	}

	private void checkCopy() {

		//do copy of original
		Cli.start(new String[]{"-"+Cli.INPUT, inDirName, "-"+Cli.OUTPUT, outDir +File.separator+ "KopijaOriginala.s2"});

		//read copy
		Cli.start(new String[]{"-"+Cli.INPUT, outDir +File.separator+ "KopijaOriginala.s2",
				"-"+Cli.OUTPUT, outDir +File.separator+ "IzpisKopije.csv"});

		//we cant compare S2 files directly
		//we could also compare txt output but...
		//compare copy.csv with corect.csv
		File corect = new File(inDir + File.separator + inCSV);
		File testing = new File(outDir + File.separator + "IzpisKopije.csv");
		try {
			boolean isTwoEqual = FileUtils.contentEquals(corect, testing);
			assertTrue("Testing copy ", isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public int checkSingleton(long cutTime)
	{
		
		String nameCSV = "Singleton" + cutTime +".csv";
		Cli.start(new String[]{"-"+Cli.INPUT, inDirName, "-"+Cli.OUTPUT, outDir +File.separator+ nameCSV,
				"-"+Cli.FILTER_TIME, Double.toString(cutTime*1E-9),Double.toString((cutTime+1)*1E-9), "true"});

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
		Cli.start(new String[]{"-"+Cli.INPUT, inDirName, "-"+Cli.OUTPUT, outDir +File.separator+  name+".s2",
				"-"+Cli.FILTER_DATA, d, "-"+Cli.FILTER_TIME, "0", "1", "true"});
		Cli.start(new String[]{"-"+Cli.STATISTIKA, outDir + File.separator + name+".txt", "-"+Cli.INPUT, outDir +File.separator+ name+".s2"});

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
	public void testGenerator2()
	{
		//1-10 s
		Cli.start(new String[] {"-"+Cli.GENERATE_RANDOM, "10", "125", "0.1", "0.1", "1000000", "0.05", "10", "0", "-"+Cli.FILTER_TIME, "1", "10",
				"-"+Cli.OUTPUT, outDir +File.separator+ "generatedRan.s2"});

		assertTrue("generating random S2 PCARD", new File(outDir +File.separator+ "generatedRan.s2").exists());

	}
	
	
	//@Test
	public void testGenerator3()
	{
		String s = "inputFile";
		Cli.start(new String[] {"-"+Cli.GENERATE_FROM_FILE, inDir +File.separator+ s+"0.csv", inDir +File.separator+ s+"1.csv", inDir +File.separator+ s+"2.csv",
				inDir +File.separator+ s+"3.csv", "-"+Cli.FILTER_TIME, "1", "10",
				"-"+Cli.OUTPUT, outDir +File.separator+ "generated3.s2"});

		Cli.start(new String[]{"-"+Cli.INPUT, outDir +File.separator+ "generated3.s2" ,"-"+Cli.OUTPUT, outDir +File.separator+ "generated3.txt"});

		File correct = new File(inDir + File.separator + inG3TXT);
		File testing = new File(outDir + File.separator + "generated3.txt");

		try {
			boolean isTwoEqual = FileUtils.contentEquals(correct, testing);
			assertTrue("generating S2 from files",isTwoEqual);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	@After
	public void afterTest()
	{
		try {
			delete(new File(outDir));
		} catch (IOException e) {
			assertTrue("we coudnt delete",false);
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void clean()
	{
		assertTrue(true);
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
