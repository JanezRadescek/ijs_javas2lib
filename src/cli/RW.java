package cli;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.cli.*;

import s2.SecondReader;
import s2.StatisticsCallback;
import s2.FirtstReader;
import s2.OutCSVCallback;
import s2.OutS2Callback;
import s2.S2;
import s2.S2.LoadStatus;

/**bere , izrezuje, ... s2 file */
public class RW {

	public static void main(String[] args){
		
		//parsanje vhodnih podatkov haahah
		Options options = new Options();
		
		options.addOption("s", false, "statistics. izpisi statistiko na izhod");
		options.addOption("c", false, "cut. izrezi del in izpisi na izhod v S2");
		options.addOption("r", false, "read. izrezi del in izpisi na izhod v CSV in human readable form");
		options.addOption("b", false, "build. sestavi 2 datoteki skupaj");
		
		Option time = new Option("t", "time. zacetni in koncni cas izseka, ki nas zanima -t zacetni koncni");
		time.setArgs(2);
		options.addOption(time);
		
		Option input1 = new Option("i", "input. Glavna vhodna datoteka, ima 2 argumenta pot in ime datoteke");
		input1.setArgs(2);
		options.addOption(input1);
		
		Option input2 = new Option("v", "vhod. Dodatna datoteka za sestavljanje");
		input2.setArgs(2);
		options.addOption(input2);
		
		Option output = new Option("o", "output. izhodna datoteka, ima 2 argumenta pot in ime datoteke");
		output.setArgs(2);
		options.addOption(output);
		
		Option handle = new Option("h",true ,"handles. Handli, ki jih želimo izpisati.Privzeto vsi. " +
				"Če želimo i-ti handle mora biti na i+1 mestu argumenta v dvojiškem zapisu števka 1." +
				"Če je argument pozitiven bodo handli z 1 ohranjeni, če je negativen bodo odstranjeni." +
				"long flags");
		//handle.setArgs(2);
		options.addOption(handle);
		
		Option dataTypes = new Option("d",true, "datatype. Tipi vrstic, ki jih želimo izpustiti.Privzeto vsi. 1števka=comment, 2števka=Special, 3števka=meta");
		options.addOption(dataTypes);
		
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (UnrecognizedOptionException e) {
            System.err.println("Unrecognized argument: "+e.getOption());

            HelpFormatter formatter = new HelpFormatter();
            PrintWriter pw = new PrintWriter(System.err);
            formatter.printUsage(pw, 80, args[0], options);
            pw.flush();
            return;
        } catch (ParseException e) {
            {
                System.err.println("Exception caught while parting input arguments:");
                e.printStackTrace(System.err);
            }
			return;
		}
		
		
		
		//priprava S2
		S2 file1;
		S2.LoadStatus loadS1;
		S2 file2 = null;
		S2.LoadStatus loadS2 = null;
		
		//brez vhodne ne moremo delati
		if(cmd.hasOption("i"))
		{
			File inDirectory1;
			String inFname1;
			File inDirectory2 = null;
			String inFname2 = null;
			try
			{
				inDirectory1 = new File(cmd.getOptionValues("i")[0]);
				inFname1 = cmd.getOptionValues("i")[1];
			}catch(Error e)
			{
				System.out.println("vhodni argument potrebuje pot in ime");
				return;
			}
			file1 = new S2();
			loadS1 = file1.load(inDirectory1, inFname1);
		
			//default values			
			long[] ab = new long[]{0,Long.MAX_VALUE};
			String izhodDir = null;
			String izhodName = null;
			long handles = Long.MAX_VALUE;
			byte dataT = Byte.MAX_VALUE;
			
			//second input S2 file directory and name
			if(cmd.hasOption("v"))
			{
				try
				{
					inDirectory2 = new File(cmd.getOptionValues("v")[0]);
					inFname2 = cmd.getOptionValues("v")[1];
				}
				catch(Error r)
				{
					System.out.println("v potrebuje pot in ime");
					return;
				}
			}
			// time interval
			if(cmd.hasOption("t"))
			{
				try{
				float aa = Float.parseFloat(cmd.getOptionValues("t")[0])* 1000000000;
				float bb = Float.parseFloat(cmd.getOptionValues("t")[1])* 1000000000;
				ab[0] = (long) aa;
				ab[1] = (long) bb;
				}catch(NumberFormatException e){
					System.out.println("Argumenta pri t morata biti float");
					return;
				}
				if (ab[0]>=ab[1])
				{
					System.out.println("Zacetni cas mora biti mansi od koncnega");
					return;
				}
			}
			//output direcotry and name 
			if(cmd.hasOption("o"))
			{
				try{
					izhodDir = cmd.getOptionValues("o")[0];
					izhodName = cmd.getOptionValues("o")[1];
				} catch(Error e)
				{
					System.out.println("o potrebuje pot in ime");
					return;
				}
			}
			//handle
			if(cmd.hasOption("h"))
			{
				try{
					handles = Long.parseLong(cmd.getOptionValue("h"));
				}catch(NumberFormatException e){
					System.out.println("argument od h mora biti stevilka");
					return;
				}
			}
			//"data types"
			if(cmd.hasOption("d"))
			{
				try{
					dataT = Byte.parseByte(cmd.getOptionValue("h"));
				}catch(NumberFormatException e){
					System.out.println("argument od d mora biti stevilka");
					return;
				}
			}
			
			//dodajanje callBackov
			if(cmd.hasOption("b") && cmd.hasOption("v") && cmd.hasOption("o"))
			{
				file2 = new S2();
				loadS2 = file2.load(inDirectory2, inFname2);
				
				build(file1, loadS1, file2, loadS2, izhodDir, izhodName);
			}else if (cmd.hasOption("b") && (cmd.hasOption("v") || cmd.hasOption("o")))
			{
				System.err.println("If we want to use -b(concat 2 S2 file)"+
						"we need option -v (second S2 file) and -o(output S2 file)");
				return;
			}
			if(cmd.hasOption("s"))
			{
				outStatistics(file1, loadS1, izhodDir, izhodName);
			}
			if(cmd.hasOption("c"))
			{
				if(izhodDir != null && izhodName != null)
				{
					outS2(file1, loadS1, ab, handles, dataT, izhodDir, izhodName);
				}else
				{
					System.err.println("Option c-cut need option o-out(directory and name of output file)");
				}
			}
			if(cmd.hasOption("r"))
			{
				outData(file1, loadS1, ab, handles, izhodDir, izhodName);
			}
			
			

			
			//preberemo prvi S2 in obdelamo
			System.err.println("using file "+file1.getFilePath()+"\n");
			boolean everythingOk = (loadS1.readAndProcessFile());
			
			//samo opcija b potrebuje drugi file
			if (file2 != null && loadS2 != null)
			{
				System.err.println("using file "+file2.getFilePath()+"\n");
				everythingOk &= loadS2.readAndProcessFile();
			}
			
	        if (everythingOk){
	        	System.out.println("Končano");
	        } else {
	        	System.out.println("Napaka, najbrž napačna vhodna datoteka");
	        }
		}
		else
		{
			System.out.println("ni vhodnih podatkov");
		}
	
	}

	/**
	 * Merges 2 S2 files into 1 S2 file
	 * @param file1 - first S2 file
	 * @param loadS1 - load status of first file
	 * @param file2 - second S2 file
	 * @param loadS2 - load status of second file
	 * @param izhodDir - directory of new S2 file
	 * @param izhodName - name of new S2 file
	 */
	private static void build(S2 file1, LoadStatus loadS1, S2 file2, LoadStatus loadS2, String izhodDir,
			String izhodName) {
		//hermione prebere prvo datoteke in prebrane podatke zapiše v boba, slednji bo prebral še drugo datoteko in 
		//podatke združil v nov S2
		SecondReader bob = new SecondReader(file2, izhodDir, izhodName);
		FirtstReader gre = new FirtstReader(file1, bob);
		
		loadS1.addReadLineCallback(gre);
		loadS2.addReadLineCallback(bob);
		
	}

	/**
	 * Writes data in CSV file in human readable form
	 * @param file - input S2 file
	 * @param ls - load status of S2
	 * @param ab - pair of numbers represanting time interval
	 * @param izhodDir - directory of CSV file
	 * @param izhodName - name of CSV
	 * @param handles - handle we want to write(we can only write one handle)
	 */
	private static void outData(S2 file, LoadStatus ls, long []ab, long handles, String izhodDir, String izhodName) {
		if(izhodDir != null && izhodName != null){
			/*
			String kon = izhodName.split("\\.")[1];
			if(kon.equals("s2"))
			{
				OutS2Callback callback = new OutS2Callback(file, ab, handles, dataT, izhodDir, izhodName);
				ls.addReadLineCallback(callback);
			}else
			{
				OutCSVCallback callback = new OutCSVCallback(file, ab, handles, izhodDir, izhodName);
				ls.addReadLineCallback(callback);
			}*/
			OutCSVCallback callback = new OutCSVCallback(file, ab, handles, izhodDir, izhodName);
			ls.addReadLineCallback(callback);
			
		}else 
		{
			OutCSVCallback callback = new OutCSVCallback(file, ab, handles);
			ls.addReadLineCallback(callback);
		}	
	}
	
	private static void outS2(S2 file, LoadStatus ls, long []ab, long handles, byte dataT, String directory, String name)
	{
		OutS2Callback callback = new OutS2Callback(file, ab, handles, dataT, directory, name);
		ls.addReadLineCallback(callback);
	}

	
	private static void outStatistics(S2 file, LoadStatus ls, String outDirectory, String outName) {
		StatisticsCallback callback;
		if(outDirectory != null){
			String directoryANDname = outDirectory + "\\" + outName;
			callback = new StatisticsCallback(file, directoryANDname);
		}else 
		{
			callback = new StatisticsCallback(file);
		}
		ls.addReadLineCallback(callback);
	}


}
