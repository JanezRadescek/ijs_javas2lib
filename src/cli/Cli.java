package cli;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.cli.*;

import callBacks.FirtstReader;
import callBacks.OutCSVCallback;
import callBacks.OutS2Callback;
import callBacks.SecondReader;
import callBacks.StatisticsCallback;
import s2.S2;

/**bere , izrezuje, ... s2 file */
public class Cli {

	public static void main(String[] args){

		//parsanje vhodnih podatkov haahah
		Options options = new Options();

		options.addOption("s", false, "statistics. Output statistics. ");
		options.addOption("c", false, "cut. cut/filter S2");
		options.addOption("r", false, "read. izrezi del in izpisi na izhod v CSV in human readable form");
		options.addOption("m", true, "mearge. Combines two S2 files in one S2 file. Has mendatory argument."
				+ " If true streams with same hendels will be merged,"
				+ " else strems from second file will get new one where needed");
		options.addOption("help", false, "Help");

		Option time = new Option("t", "time. zacetni in koncni cas izseka, ki nas zanima. 3 argument if we aproximate "
				+ "datas without own time with last previous time"
				+ "-t start end nonEssential. Defaul -t 0 Long.MAX_VALUE true");
		time.setArgs(3);
		time.setOptionalArg(true);
		options.addOption(time);

		Option input1 = new Option("i", "input. Directory of input file. "
				+ "Optional has also directory of second directory.");
		input1.setArgs(2);
		input1.setOptionalArg(true);

		options.addOption(input1);
		/*
		Option input2 = new Option("v", "vhod. Dodatna datoteka za sestavljanje");
		input2.setArgs(2);
		options.addOption(input2);*/

		Option output = new Option("o", true, "output. Directory and name of output file");
		options.addOption(output);

		Option handle = new Option("h",true ,"handles. Handles, we want to use.Deafault all. " +
				"Argument represent wanted handles. " +
				"If we want handle with num. i there has to be 1 on i+1 position from right to left in argument,0 atherwise" +
				"If we want to keep only handles with 0 and 4 we pass '10001'" );
		//handle.setArgs(2);
		options.addOption(handle);

		Option dataTypes = new Option("d",true, "datatype. data types we want to keep. Deafault all" +
				"Argument must be a number in binary form"+
				".*1=keeps comments, .*1.=keeps Special, .*1..=keeps meta");
		options.addOption(dataTypes);

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		HelpFormatter formatter = new HelpFormatter();
		String header = "Do something with S2 file";
		String footer = "footer";
		try {
			cmd = parser.parse(options, args);
		} catch (UnrecognizedOptionException e) {
			System.err.println("Unrecognized argument: "+e.getOption());


			PrintWriter pw = new PrintWriter(System.err);
			formatter.printUsage(pw, 80, args[0], options);
			pw.flush();
			return;
		} catch (ParseException e) {

			formatter.printHelp("Cli",header,options,footer);
			System.err.println("Exception caught while parting input arguments:");
			e.printStackTrace(System.err);

			return;
		}
		if(cmd.hasOption("help"))
		{
			formatter.printHelp("Cli",header,options,footer);
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
			//String inFname1;
			File inDirectory2 = null;
			//String inFname2 = null;
			try
			{
				inDirectory1 = new File(cmd.getOptionValues("i")[0]);
				//inFname1 = cmd.getOptionValues("i")[1];
			}catch(Exception e)
			{
				System.out.println("Option i need directory and name of input S2 file. TERMINATE");
				return;
			}
			file1 = new S2();
			loadS1 = file1.load(inDirectory1.getParentFile(), inDirectory1.getName());

			//default values			
			long[] ab = new long[]{0,Long.MAX_VALUE};
			boolean nonEss = true;
			String izhodDir = null;
			//String izhodName = null;
			long handles = Long.MAX_VALUE;
			byte dataT = Byte.MAX_VALUE;

			//second input S2 file directory and name
			if(cmd.hasOption("m"))
			{
				try
				{
					inDirectory2 = new File(cmd.getOptionValues("i")[1]);
					//inFname2 = cmd.getOptionValues("v")[1];
				}
				catch(Exception r)
				{
					System.out.println("Option i needs directory and name of second input file. TERMINATE");
					return;
				}
			}
			// time interval
			if(cmd.hasOption("t"))
			{
				try{
					double aa = Double.parseDouble(cmd.getOptionValues("t")[0])* 1E9;
					double bb = Double.parseDouble(cmd.getOptionValues("t")[1])* 1E9;
					if(cmd.hasOption("c"))
						nonEss = Boolean.parseBoolean(cmd.getOptionValues("t")[2]);
					ab[0] = (long)aa;
					ab[1] = (long)bb;
				}catch(NumberFormatException e){
					System.out.println("Arguments at t must be float float boolean");
					return;
				}
				if (ab[0]>ab[1])
				{
					System.out.println("Starting time must be lower than ending. TERMINATE");
					return;
				}
			}
			//output direcotry and name 
			if(cmd.hasOption("o"))
			{
				try{
					izhodDir = cmd.getOptionValue("o");
					//izhodName = cmd.getOptionValues("o")[1];
				} catch(Exception e)
				{
					System.out.println("Option o needs file directory and name. TERMINATE");
					return;
				}
			}
			//handle
			if(cmd.hasOption("h"))
			{
				try{
					handles = Long.parseLong(cmd.getOptionValue("h"),2);
				}catch(NumberFormatException e){
					System.out.println("argument of h must be a number in binary format. TERMINATE");
					return;
				}
			}
			//"data types"
			if(cmd.hasOption("d"))
			{
				try{
					dataT = Byte.parseByte(cmd.getOptionValue("d"),2);
				}catch(NumberFormatException e){
					System.out.println("argument of d must be a number in binary format. TERMINATE");
					return;
				}
			}
			//dodajanje callBackov
			if(cmd.hasOption("m") && inDirectory2!=null && izhodDir!=null)
			{
				file2 = new S2();
				loadS2 = file2.load(inDirectory2.getParentFile(), inDirectory2.getName());
				boolean mergeHandles = Boolean.parseBoolean(cmd.getOptionValue("m"));

				SecondReader bob = new SecondReader(file2, izhodDir, mergeHandles);
				FirtstReader gre = new FirtstReader(file1, bob);
				loadS1.addReadLineCallback(gre);
				loadS2.addReadLineCallback(bob);
				//merge(file1, loadS1, file2, loadS2, izhodDir, mergeHandles);

			}else if (cmd.hasOption("m"))
			{
				System.err.println("If we want to use -m(concat 2 S2 files)"+
						"we need option -i with second argument (second S2 file) and -o(output S2 file). TERMINATE");
				return;
			}

			if(cmd.hasOption("s"))
			{
				StatisticsCallback callback;
				if(izhodDir !=null)
					callback = new StatisticsCallback(file1, izhodDir);
				else
					callback = new StatisticsCallback(file1);
				loadS1.addReadLineCallback(callback);
				//outStatistics(file1, loadS1, izhodDir);
			}

			if(cmd.hasOption("c"))
			{
				if(izhodDir != null)
				{
					OutS2Callback callback = new OutS2Callback(file1, ab, nonEss, handles, dataT, izhodDir);
					loadS1.addReadLineCallback(callback);
					//outS2(file1, loadS1, ab, nonEss, handles, dataT, izhodDir);
				}else
				{
					System.err.println("Option c-cut need option o-out(directory and name of output file). TERMINATE");
				}
			}

			if(cmd.hasOption("r"))
			{
				if(izhodDir != null)
				{
					OutCSVCallback callback = new OutCSVCallback(file1, ab, handles, izhodDir);
					loadS1.addReadLineCallback(callback);
					//outData(file1, loadS1, ab, handles, izhodDir);
				}else
				{
					OutCSVCallback callback = new OutCSVCallback(file1, ab, handles, izhodDir);
					loadS1.addReadLineCallback(callback);
				}


			}

			//preberemo prvi S2 in obdelamo
			System.out.println("using file "+file1.getFilePath());
			boolean everythingOk = loadS1.readAndProcessFile();
			System.out.println(file1.getNotes());
			//samo opcija b potrebuje drugi file
			if (file2 != null && loadS2 != null)
			{
				System.out.println("using file "+file2.getFilePath());
				everythingOk &= loadS2.readAndProcessFile();
				System.out.println(file2.getNotes());
			}

			if (everythingOk){
				System.out.println("THE END" + "\n");
			} else {
				System.err.println("Error in procesing S2 file");
			}

		}
		else
		{
			System.out.println("Input is mandatory. TERMINATE");
			formatter.printHelp("Cli",header,options,footer);
		}

	}

	public Exception start(String[] args)
	{
		return null;
	}
}
