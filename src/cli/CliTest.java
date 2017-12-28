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
	String koren;

	String inDir = "C:\\Users\\janez\\workspace\\S2_rw\\IO\\Original";
	String outDir = "C:\\Users\\janez\\workspace\\S2_rw\\UnitTests";


	public CliTest(String inFile)
	{
		this.inFile = inFile;
		this.koren = inFile.split("\\.")[0]; 
	}

	@Parameterized.Parameters
	public static String[] data() {
		return new String[]{"test1.s2", "test2.s2", "test3.s2"};

	}


	@Test
	public void outS2Test()
	{
		//-r -i C:\Users\janez\workspace\S2_rw\IO test1.s2 -h 1 -t 16 19 -o C:\Users\janez\workspace\S2_rw\IO izhod1.csv

		Cli.main(new String[]{"-r", "-i", inDir, inFile, "-o", outDir, koren + "IzpisOriginala.csv"});

		//-c -i C:\Users\janez\workspace\S2_rw\IO test2.s2 -o C:\Users\janez\workspace\S2_rw\IO izrezek.s2
		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-o", outDir, koren + "KopijaOriginala.s2"});

		Cli.main(new String[]{"-r", "-i", inDir, koren + "KopijaOriginala.s2",
				"-o", outDir, koren + "IzpisKopije.csv"});
		
		
		File file1 = new File(outDir + "\\" + koren + "IzpisOriginala.csv");
		File file2 = new File(outDir + "\\" + koren + "IzpisKopije.csv");
		boolean isTwoEqual = false;
		try {
			isTwoEqual = FileUtils.contentEquals(file1, file2);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertEquals(isTwoEqual, true);
		
	}

	@Test
	public void buildTest()
	{
		
		Cli.main(new String[]{"-r", "-i", inDir, inFile, "-o", outDir, koren + "IzpisOriginala.csv"});

		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-t", "0", "100",
				"-o", outDir, koren + "izrezek1.s2"});

		Cli.main(new String[]{"-c", "-i", inDir, inFile, "-t", "100", "2000",
				"-o", outDir, koren + "izrezek2.s2"});

		Cli.main(new String[]{"-b", "true", "-i", inDir, koren + "izrezek1.s2",
				"-v", inDir, koren + "izrezek2.s2",
				"-o", outDir, koren + "Sestavljena.s2"});

		Cli.main(new String[]{"-r", "-i", inDir, koren + "Sestavljena.s2",
				"-o", outDir, koren + "IzpisSestavljene.csv"});

		
		File file1 = new File(outDir + "\\" + koren + "IzpisOriginala.csv");
		File file2 = new File(outDir + "\\" + koren + "IzpisSestavljene.csv");
		File file3 = new File(inDir + "\\" + "izhodSESTA.csv");
		
		try {
			boolean isTwoEqual = FileUtils.contentEquals(file1, file2);
			boolean notEqual = FileUtils.contentEquals(file1, file3);
			
			assertEquals(isTwoEqual, true);
			assertEquals(notEqual, false);
		} catch (IOException e) {
			e.printStackTrace();
		}


	}


}
