package cli;

import java.lang.Exception;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.commons.cli.*;

import callBacks.FirtstReader;
import callBacks.FixTime;
import callBacks.OutCSVCallback;
import callBacks.OutS2Callback;
import callBacks.SecondReader;
import callBacks.StatisticsCallback;
import filters.Filter;
import filters.FilterData;
import filters.FilterGetLines;
import filters.FilterHandles;
import filters.FilterInfo;
import filters.FilterSaveCSV;
import filters.FilterSaveS2;
import filters.FilterTime;
import si.ijs.e6.S2;

/**
 * parses input String arguments and crates/calls appropriate callbacks and add them to S2 object.
 * At the end it S2.
 * 
 * @author janez
 *
 */
public class Cli {

	public static void main(String[] args){

		final String PASS = "pass";
		final String STATISTIKA = "s";
		final String CUT = "c";
		final String READ = "r";
		final String MEARGE = "m";
		final String HELP = "help";

		final String PROCESS_SIGNAL = "p";

		final HashSet<String> PROCESS_FIRST_S2 = new HashSet<String>(
				Arrays.asList(new String[]{STATISTIKA, CUT, READ, MEARGE, HELP, PROCESS_SIGNAL}));
		final HashSet<String> PROCESS_SECOND_S2 = new HashSet<String>(
				Arrays.asList(new String[]{MEARGE}));

		final String TIME = "t";
		final String INPUT = "i";
		final String OUTPUT = "o";
		final String HANDLES = "h";
		final String DATA = "d";

		//curent task
		String ctask = PASS;

		//parsanje vhodnih podatkov
		Options options = new Options();

		options.addOption(STATISTIKA, false, "statistics. Output statistics. ");
		options.addOption(CUT, false, "cut. cut/filter S2");
		options.addOption(READ, false, "read. izrezi del in izpisi na izhod v CSV in human readable form");
		options.addOption(MEARGE, true, "mearge. Combines two S2 files in one S2 file. Has mendatory argument."
				+ " If true streams with same hendels will be merged,"
				+ " else strems from second file will get new one where needed");
		options.addOption(HELP, false, "Help");
		options.addOption(PROCESS_SIGNAL,true, "Proces signal. If argument is true it will process"
				+ " as if the frequency of sensor is constant. Otherwise ");

		Option time = new Option(TIME, "time. zacetni in koncni cas izseka, ki nas zanima. 3 argument if we aproximate "
				+ "datas without own time with last previous time"
				+ "-t start end nonEssential. Defaul -t 0 Long.MAX_VALUE true");
		time.setArgs(3);
		time.setOptionalArg(true);
		options.addOption(time);

		Option input1 = new Option(INPUT, "input. Directory of input file. "
				+ "Optional has also directory of second directory.");
		input1.setArgs(2);
		input1.setOptionalArg(true);

		options.addOption(input1);

		Option output = new Option(OUTPUT, true, "output. Directory and name of output file");
		options.addOption(output);

		Option handle = new Option(HANDLES,true ,"handles. Handles, we want to use.Deafault all. " +
				"Argument represent wanted handles. " +
				"If we want handle with num. i there has to be 1 on i+1 position from right to left in argument,0 atherwise" +
				"If we want to keep only handles with 0 and 4 we pass '10001'" );
		options.addOption(handle);

		Option dataTypes = new Option(DATA,true, "datatype. data types we want to keep. Deafault all" +
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
		if(cmd.hasOption(INPUT))
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
			String outDir = null;
			long handles = Long.MAX_VALUE;
			byte dataT = Byte.MAX_VALUE;
			boolean simpleProcessing = true;
			boolean dataMapping = true;

			//second input S2 file directory and name
			if(cmd.hasOption(MEARGE))
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
			if(cmd.hasOption(TIME))
			{
				try{
					double aa = Double.parseDouble(cmd.getOptionValues("t")[0])* 1E9;
					double bb = Double.parseDouble(cmd.getOptionValues("t")[1])* 1E9;
					if(cmd.hasOption(CUT))
						nonEss = Boolean.parseBoolean(cmd.getOptionValues("t")[2]);
					ab[0] = (long)aa;
					ab[1] = (long)bb;
				}catch(NumberFormatException e){
					System.out.println("Arguments at" +TIME+ "must be float float boolean");
					return;
				}
				if (ab[0]>ab[1])
				{
					System.out.println("Starting time must be lower than ending. TERMINATE");
					return;
				}
			}
			//output direcotry and name 
			if(cmd.hasOption(OUTPUT))
			{
				try{
					outDir = cmd.getOptionValue(OUTPUT);
					//izhodName = cmd.getOptionValues("o")[1];
				} catch(Exception e)
				{
					System.out.println("Option "+OUTPUT+" needs file directory and name. TERMINATE");
					return;
				}
			}
			//handle
			if(cmd.hasOption(HANDLES))
			{
				try{
					handles = Long.parseLong(cmd.getOptionValue(HANDLES),2);
				}catch(NumberFormatException e){
					System.out.println("argument of "+HANDLES+" must be a number in binary format. TERMINATE");
					return;
				}
			}
			//"data types"
			if(cmd.hasOption(DATA))
			{
				try{
					dataT = Byte.parseByte(cmd.getOptionValue(DATA),2);
				}catch(NumberFormatException e){
					System.out.println("argument of "+DATA+" must be a number in binary format. TERMINATE");
					return;
				}
			}


			//dodajanje callBackov
			//TODO completly remove old callbacks



			if(cmd.hasOption(MEARGE) && inDirectory2!=null && outDir!=null)
			{
				ctask = MEARGE;
				file2 = new S2();
				loadS2 = file2.load(inDirectory2.getParentFile(), inDirectory2.getName());
				boolean mergeHandles = Boolean.parseBoolean(cmd.getOptionValue(MEARGE));

				SecondReader bob = new SecondReader(file2, outDir, mergeHandles);
				FirtstReader gre = new FirtstReader(file1, bob);
				loadS1.addReadLineCallback(gre);
				loadS2.addReadLineCallback(bob);
				//merge(file1, loadS1, file2, loadS2, izhodDir, mergeHandles);

			}else if (cmd.hasOption(MEARGE))
			{
				System.err.println("If we want to use -m(concat 2 S2 files)"+
						"we need option -i with second argument (second S2 file) and -o(output S2 file). TERMINATE");
				return;
			}

			if(cmd.hasOption(STATISTIKA))
			{
				//TODO callback je star zbriši ga ko bo testruner deloval
				ctask = STATISTIKA;
				StatisticsCallback callback;
				FilterInfo filter;
				if(outDir !=null)
				{
					callback = new StatisticsCallback(file1, outDir);
					try {
						filter = new FilterInfo(new PrintStream(new File(outDir)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						System.out.println("File couldnt be made");
						return;
					}
				}
				else
				{
					filter = new FilterInfo(System.out);
					callback = new StatisticsCallback(file1);
				}
				loadS1.addReadLineCallback(filter);
			
			}

			if(cmd.hasOption(CUT))
			{
				ctask = CUT;
				if(outDir != null)
				{
					if(true)
					{
						//old way with callbacks

						OutS2Callback callback = new OutS2Callback(file1, ab, nonEss, handles, dataT, outDir);
						loadS1.addReadLineCallback(callback);

					}else
					{
						//new way
						FilterGetLines getlines = new FilterGetLines();
						//
						FilterTime filterT = new FilterTime(ab[0], ab[1], nonEss);
						FilterData filterD = new FilterData(dataT);
						FilterHandles filterH = new FilterHandles(handles);
						FilterSaveS2 filterS = new FilterSaveS2(outDir);

						loadS1.addReadLineCallback(filterT);
						filterT.addChild(filterD);
						filterD.addChild(filterH);
						filterH.addChild(filterS);
						//
						filterS.addChild(getlines);
						
					}

				}else
				{
					System.err.println("Option c-cut need option o-out(directory and name of output file). TERMINATE");
				}
			}

			if(cmd.hasOption(READ))
			{
				ctask = READ;
				if(outDir != null)
				{


					if(true)
					{
						// old way with callback
						OutCSVCallback callback = new OutCSVCallback(file1, ab, handles, outDir);
						loadS1.addReadLineCallback(callback);
					}else
					{

						//new way with filters

						FilterSaveCSV filter = new FilterSaveCSV(outDir, dataMapping);
						loadS1.addReadLineCallback(filter);
					}

				}else
				{
					System.err.println("Option r-read need option o-out(directory and name of output file). TERMINATE");
				}
			}



			if(cmd.hasOption(PROCESS_SIGNAL))
			{
				ctask = PROCESS_SIGNAL;

				simpleProcessing = Boolean.parseBoolean(cmd.getOptionValue(PROCESS_SIGNAL));
				FixTime callback = new FixTime(file1, ab, nonEss, handles, dataT, outDir, simpleProcessing);
				loadS1.addReadLineCallback(callback);
			}


			boolean everythingOk = true;

			if(PROCESS_FIRST_S2.contains(ctask))
			{
				//preberemo prvi S2 in obdelamo
				System.out.println("using file "+file1.getFilePath());
				everythingOk &= loadS1.readAndProcessFile();
				System.out.println(file1.getNotes());
				//samo opcija b potrebuje drugi file
			}
			if (PROCESS_SECOND_S2.contains(ctask))
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

	public static void start(String[] args)
	{
		Cli.start(args, System.out);
	}

	public static void start(String[] args, PrintStream out)
	{

	}
}
