package cli;

import java.lang.Exception;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import org.apache.commons.cli.*;

import callBacks.FirtstReader;
import callBacks.SecondReader;
import filters.FilterData;
import filters.FilterHandles;
import filters.FilterInfo;
import filters.FilterProcessSignal;
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

	public static void main(String[] args)
	{
		start(args);
		System.exit(0);
	}

	public static String GuiCliLink(String[] args)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		PrintStream ps = null;
		
		try {
			ps = new PrintStream(baos, true, "utf-8");
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return e.getMessage();
		}
		start(args, ps, ps);
		String sOut = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		
		ps.close();
		
		return sOut;
	}
	
	public static void start(String[] args)
	{
		Cli.start(args, System.out, System.err);
	}

	public static void start(String[] args, PrintStream outPS, PrintStream errPS)
	{
		final String STATISTIKA = "s";
		final String CUT = "c";
		final String READ = "r";
		final String MEARGE = "m";
		final String HELP = "help";

		final String PROCESS_SIGNAL = "p";

		final String TIME = "t";
		final String INPUT = "i";
		final String OUTPUT = "o";
		final String HANDLES = "h";
		final String DATA = "d";

		//parsanje vhodnih podatkov
		Options options = new Options();

		options.addOption(STATISTIKA, false, "statistics. Output statistics. ");
		options.addOption(CUT, false, "cut. cut/filter S2");
		options.addOption(READ, false, "read. izrezi del in izpisi na izhod v CSV in human readable form");
		options.addOption(MEARGE, true, "mearge. Combines two S2 files in one S2 file. Has mendatory argument."
				+ " If true streams with same hendels will be merged,"
				+ " else strems from second file will get new one where needed");
		options.addOption(HELP, false, "Help");
		options.addOption(PROCESS_SIGNAL,false, "Proces signal. If argument is true it will process"
				+ " as if the frequency of sensor is constant. Simple processsing. Otherwise it will split into intervals");

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
			errPS.println("Unrecognized argument: "+e.getOption());


			PrintWriter pw = new PrintWriter(errPS);
			formatter.printUsage(pw, 80, args[0], options);
			pw.flush();
			return;
		} catch (ParseException e) {

			formatter.printHelp("Cli",header,options,footer);
			errPS.println("Exception caught while parting input arguments:");
			e.printStackTrace(errPS);

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
				outPS.println("Option i need directory and name of input S2 file. TERMINATE");
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
					outPS.println("Option i needs directory and name of second input file. TERMINATE");
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
					outPS.println("Arguments at" +TIME+ "must be float float boolean");
					return;
				}
				if (ab[0]>ab[1])
				{
					outPS.println("Starting time must be lower than ending. TERMINATE");
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
					outPS.println("Option "+OUTPUT+" needs file directory and name. TERMINATE");
					return;
				}
			}
			//handle
			if(cmd.hasOption(HANDLES))
			{
				try{
					handles = Long.parseLong(cmd.getOptionValue(HANDLES),2);
				}catch(NumberFormatException e){
					outPS.println("argument of "+HANDLES+" must be a number in binary format. TERMINATE");
					return;
				}
			}
			//"data types"
			if(cmd.hasOption(DATA))
			{
				try{
					dataT = Byte.parseByte(cmd.getOptionValue(DATA),2);
				}catch(NumberFormatException e){
					outPS.println("argument of "+DATA+" must be a number in binary format. TERMINATE");
					return;
				}
			}




			if(cmd.hasOption(MEARGE) && inDirectory2!=null && outDir!=null)
			{
				file2 = new S2();
				loadS2 = file2.load(inDirectory2.getParentFile(), inDirectory2.getName());
				boolean mergeHandles = Boolean.parseBoolean(cmd.getOptionValue(MEARGE));

				SecondReader bob = new SecondReader(file2, outDir, mergeHandles);
				FirtstReader gre = new FirtstReader(file1, bob);
				loadS1.addReadLineCallback(gre);
				loadS2.addReadLineCallback(bob);

				loadS1.readAndProcessFile();
				loadS2.readAndProcessFile();

			}else if (cmd.hasOption(MEARGE))
			{
				errPS.println("If we want to use -m(concat 2 S2 files)"+
						"we need option -i with second argument (second S2 file) and -o(output S2 file). TERMINATE");
				return;
			}

			if(cmd.hasOption(STATISTIKA))
			{
				FilterInfo filter;
				if(outDir !=null)
				{
					try {
						filter = new FilterInfo(new PrintStream(new File(outDir)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						outPS.println("File couldnt be made");
						return;
					}
				}
				else
				{
					filter = new FilterInfo(outPS);
				}
				//loadS1.addReadLineCallback(filter);
				loadS1.readLines(filter, false);
			}

			if(cmd.hasOption(CUT))
			{
				if(outDir != null)
				{

					//new way
					FilterTime filterT = new FilterTime(ab[0], ab[1], nonEss);
					FilterData filterD = new FilterData(dataT);
					FilterHandles filterH = new FilterHandles(handles);
					FilterSaveS2 filterS = new FilterSaveS2(outDir);

					loadS1.addReadLineCallback(filterT);
					filterT.addChild(filterD);
					filterD.addChild(filterH);
					filterH.addChild(filterS);
					loadS1.readLines(filterT, false);



				}else
				{
					errPS.println("Option c-cut need option o-out(directory and name of output file). TERMINATE");
				}
			}

			if(cmd.hasOption(READ))
			{
				if(outDir != null)
				{
					//new way with filters

					FilterSaveCSV filter = new FilterSaveCSV(outDir, dataMapping);
					loadS1.readLines(filter, false);

				}else
				{
					errPS.println("Option r-read need option o-out(directory and name of output file). TERMINATE");
				}
			}



			if(cmd.hasOption(PROCESS_SIGNAL))
			{
				FilterTime filterT = new FilterTime(ab[0], ab[1]);
				FilterProcessSignal filterP = new FilterProcessSignal();
				filterT.addChild(filterP);
				filterP.addChild(new FilterSaveS2(outDir));

				loadS1.readLines(filterT, false);
			}

			outPS.println("The End of CLI" + "\n");
			

		}
		else
		{
			outPS.println("Input is mandatory. TERMINATE");
			formatter.printHelp("Cli",header,options,footer);
		}
	}
}
