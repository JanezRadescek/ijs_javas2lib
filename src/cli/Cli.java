package cli;

import java.lang.Exception;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import org.apache.commons.cli.*;

import filtersOld.FilterProcessSignal;
import generatorS2.Generator2;
import generatorS2.Generator3;
import pipeLines.Connector;
import pipeLines.Pipe;
import pipeLines.conglomerates.SmartMerge;
import pipeLines.filters.ChangeDateTime;
import pipeLines.filters.ChangeTimeStamps;
import pipeLines.filters.FilterComments;
import pipeLines.filters.FilterData;
import pipeLines.filters.FilterHandles;
import pipeLines.filters.FilterSpecial;
import pipeLines.filters.GetInfo;
import pipeLines.filters.LimitNumberLines;
import pipeLines.filters.SaveCSV;
import pipeLines.filters.SaveS2;
import pipeLines.filters.SaveTXT;
import pipeLines.filters.FilterTime;
import si.ijs.e6.S2;

/**
 * parses input String arguments and crates/calls appropriate callbacks and add them to S2 object.
 * At the end it S2.
 * 
 * @author janez
 *
 */
public class Cli {
	
	private static final int CLI_VERSION = 1;

	private static final int good = 0;
	private static final int unknown = 1;
	private static final int fileError = 2;
	private static final int badInputArgs = 3;

	//special it doesn't run filters

	public static final String HELP = "help";
	public static final String VERSION = "version";

	public static final String INPUT = "i";
	public static final String MERGE = "m";
	public static final String FILTER_DATA = "fd";
	
	//TODO make documentation for filternumberlines
	public static final String FILTER_NUMBER_LINES = "fnl";
	
	public static final String FILTER_COMMENTS = "fc";
	public static final String FILTER_SPECIAL = "fs";
	public static final String FILTER_HANDLES = "fh";
	public static final String FILTER_TIME = "ft";
	public static final String CHANGE_TIME = "ct";
	public static final String CHANGE_DATE_TIME = "cdt";
	public static final String PROCESS_SIGNAL = "p";

	public static final String STATISTIKA = "s";
	public static final String OUTPUT = "o";

	public static final String GENERATE_OLD = "g1";
	public static final String GENERATE_RANDOM = "g2";
	public static final String GENERATE_FROM_FILE = "g3";



	public static void main(String[] args)
	{
		int code;
		try 
		{
			code = start(args);
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
			code = unknown;
		}	
		System.exit(code);
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

	public static int start(String[] args)
	{
		return Cli.start(args, System.out, System.err);
	}

	public static int start(String[] args, PrintStream outPS)
	{
		return Cli.start(args, outPS, outPS);
	}



	public static int start(String[] args, PrintStream outPS, PrintStream errPS)
	{


		//********************************             CLI                ********************************

		Options options = new Options();


		options.addOption(HELP, false, "Prints Help. Other flags will be ignored.");
		options.addOption(VERSION, false, "Prints version. Help has priority over this. Other flags will be ignored.");


		Option input = new Option(INPUT, "General input. First argument is Directory and name of input file. "
				+ "Secondary argument is optional and is secondary input used when needed."
				+ "\nArguments:\n"
				+ "-file path"
				+ "-[file path]");
		input.setArgs(2);
		input.setOptionalArg(true);
		options.addOption(input);


		options.addOption(MERGE, true, "Combines two S2 files in one S2 file. Needs flag '-i' with two inputs. Combining with other filters has undefined behavior!  Has mandatory argument."
				+ " If true streams with same handles will be merged,"
				+ " else streams from second file will get new one where needed.\nArguments:\n"
				+ "-Boolean mergingHandles");


		Option dataTypes = new Option(FILTER_DATA,true, "Filters by data type. Argument must be a number in binary form: "+
				"@@@@1=keeps comments, @@@1@=keeps Special, @@1@@=keeps meta, @1@@@=keeps data streams, 1@@@@=keeps unknown lines."
				+ "\nArguments:\n"
				+ "Byte data [Byte]");
		options.addOption(dataTypes);
		
		options.addOption(Cli.FILTER_NUMBER_LINES, true, "Max 10 special, max 10 Unknown, max number of lines specified in argument");


		options.addOption(Cli.FILTER_COMMENTS, true, "Filters comments based on regex provided in argument. Comments not matching regex will be removed.\nArguments:\n"
				+ "-String regex");


		Option special = new Option(Cli.FILTER_SPECIAL, "Filters special messages. Who and What must be equal to their respective values in SP to get through."
				+ " Massage must suit regex provided in argument to get through.\nArguments:\n"
				+ "-Who [char]"
				+ "-What [char]"
				+ "-regex for message [String]");
		special.setArgs(3);
		options.addOption(special);


		Option handle = new Option(FILTER_HANDLES,true ,"Filters handles. Argument represent wanted handles. " +
				"To include handle #i, put 1 in position i+1 (from right to left) in the argument, to exclude it, put 0." +
				"\nArguments:\n"
				+ "-handles written in binary notation [Long]. Example: If we want to keep only handles 0 and 4 we pass '10001'");
		options.addOption(handle);


		Option time = new Option(FILTER_TIME, "Filters time. Data on interval [End, start) will be keep, the rest will be deleted. If third optional argument is true we approximate "
				+ "comments and special messages with last previous time and therefore delete them if outside interval."
				+ "\nArguments:\n"
				+ "-start in s [double]"
				+ "-end in s [double]"
				+ "-[approximate [bool]]");
		time.setArgs(3);
		time.setOptionalArg(true);
		options.addOption(time);


		options.addOption(CHANGE_TIME, true, "Add time in argument to all timestamps. "
				+ "If added time is negative and its absolute value bigger than value of first time stamp, "
				+ "added time will be set to -first time, resulting in new first time stamp being 0.\nArguments:\n"
				+ "-Long delay[ns]");


		options.addOption(CHANGE_DATE_TIME, true, "Change date in meta into new one. Timestamps are relative to date in meta"
				+ " and therefore are changed so the absolute timestamps of data does not change."
				+ "\nArguments:\n"
				+ "-date with time and zone in ISO format. Example: \"2018-01-01T10:30:10.554+0100\"");


		options.addOption(PROCESS_SIGNAL,false, "Process signal.");


		Option statistika = new Option(STATISTIKA,true, "Output statistics of input S2 file. Has optional argument directory and name of file for outputting statistics. "
				+ "If argument is missing it will print statistics to outPS (default is System.out). DO NOT confuse this argument with flag -o output.\nArguments:\n"
				+ "-String directoryAndName");
		statistika.setArgs(1);
		statistika.setOptionalArg(true);;
		options.addOption(statistika);


		Option output = new Option(OUTPUT, true, "General output for result of other flags. If Argument is valid Directory and name with extension, it will output into specifed file."
				+ " If argument equals 'xyz' where 'xyz' is file extension only, it will print result to the outPUT stream (Default is System.out)."
				+ "Type of output will be based on extension of the name. Possible extensions are 'csv', 's2' and 'txt'."
				+ "\nArguments:\n"
				+ "dirNameExt [String]. Example: .\\myFile\\Result.s2 or txt");
		options.addOption(output);


		Option generate2 = new Option(GENERATE_RANDOM, "Generates semirandom S2 PCARD based on arguments. Needs option/flag filter time -ft and output -o.\nArguments:\n" 
				+ "-seed for random [long] \n"
				+ "-frequency in Hz [float] (around 128 for PCARD)\n"
				+ "-frequency change [0..1]\n"
				+ "-percentage missing [0..1]\n"
				+ "-normal delay in s [double]\n"
				+ "-big delay chance [0..1] \n"		// when pause occurs machine will be saving packets normaly but android will get in transmission (packets are not only delayed, they are missing)
				+ "-big delay in s [double] \n"
				+ "-number of disconnects (disconnects are scattered randomly across whole S2 file. "
				+ 		"When disconnect occurs machine stops recoding and resets counters. Consequently android doesn't get any packets \n"
				+ "-index of stuck bit. LITTLE_ENDIAN. Negative value will not change any index. \n"
				+ "-value of stuck bit");
		generate2.setArgs(10);
		options.addOption(generate2);

		Option generate3 = new Option(GENERATE_FROM_FILE,"Generates S2 PCARD based on 'numbers' in files from arguments.\nArguments:\n"
				+ "-input directory of Frequencies file \n"
				+ "-input directory of Disconnects file \n"
				+ "-input directory of Pauses file \n"
				+ "-input directory of Delays file \n");
		generate3.setArgs(4);
		options.addOption(generate3);


		//************************************         APACHE CLI                      *******************************



		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		HelpFormatter formatter = new HelpFormatter();
		String header = "Do something with S2 file";
		String footer = "END of help";
		try {
			cmd = parser.parse(options, args);
		} catch (UnrecognizedOptionException e) {
			errPS.println("Unrecognized argument: "+e.getOption());


			PrintWriter pw = new PrintWriter(errPS);
			formatter.printUsage(pw, 80, args[0], options);
			pw.flush();
			return badInputArgs;
		} catch (ParseException e) {

			errPS.println("Exception caught while parsing input arguments. " + e.getLocalizedMessage());
			formatter.printHelp("Cli",header,options,footer);

			return unknown;
		} 
		
		if(cmd.hasOption(HELP))
		{
			formatter.printHelp("Cli",header,options,footer);
			
			return good;
		}
		
		if(cmd.hasOption(VERSION))
		{
			outPS.println(CLI_VERSION);
		}



		//********************************           PARSANJE   ARGUMENTOV                 ********************************


		//brez vhodne ne moremo delati nekaterih
		if(cmd.hasOption(INPUT))
		{
			S2 file1;
			S2.LoadStatus loadS1;
			S2 file2 = null;
			S2.LoadStatus loadS2 = null;
			File inDirectory1;
			try
			{
				inDirectory1 = new File(cmd.getOptionValues(INPUT)[0]);
				if(!inDirectory1.exists())
				{
					errPS.println("Input file does not exist.");
					return badInputArgs;
				}
			}catch(Exception e)
			{
				errPS.println("Option i need directory and name of input S2 file. TERMINATE");
				return badInputArgs;
			}
			file1 = new S2();
			loadS1 = file1.load(inDirectory1);


			//******************************************************************
			//******************************************************************
			ArrayList<Pipe> pipeLine = new ArrayList<Pipe>();
			//******************************************************************
			//******************************************************************



			if(cmd.hasOption(MERGE))
			{
				try
				{
					File inDirectory2 = new File(cmd.getOptionValues(INPUT)[1]);
					if(!inDirectory2.exists())
					{
						errPS.println("Input file does not exist.");
						return badInputArgs;
					}
					boolean newHandles = Boolean.parseBoolean(cmd.getOptionValue(MERGE));
					file2 = new S2();
					loadS2 = file2.load(inDirectory2.getParentFile(), inDirectory2.getName());
					Pipe pipeP = new Pipe(); 
					Pipe pipeS = new Pipe();
					SmartMerge sm = new SmartMerge(loadS2, pipeS, pipeP, pipeS, inDirectory1, inDirectory2, false, newHandles, errPS);

					Connector con = new Connector();
					con.addStart(pipeP);
					sm.getPrimaryOutPut().addChild(con.getEnd());

					pipeLine.add(con);
				}
				catch(Exception r)
				{
					errPS.println("Option "+Cli.INPUT+" needs directory and name of second input file for "+Cli.MERGE +". TERMINATE");
					return badInputArgs;
				}
			}


			if(cmd.hasOption(FILTER_DATA))
			{
				try
				{
					pipeLine.add(new FilterData(Byte.parseByte(cmd.getOptionValue(FILTER_DATA),2),errPS));
				}catch(NumberFormatException e){
					errPS.println("argument of "+FILTER_DATA+" must be a number in binary format. TERMINATE");
					return badInputArgs;
				}
			}
			
			if(cmd.hasOption(FILTER_NUMBER_LINES))
			{
				try
				{
				int maxLines = Integer.parseInt(cmd.getOptionValue(Cli.FILTER_NUMBER_LINES));
				pipeLine.add(new LimitNumberLines(maxLines));
				}catch(NumberFormatException e){
					errPS.println("argument of "+FILTER_NUMBER_LINES+" must be an integer. TERMINATE");
					return badInputArgs;
				}
			}


			if(cmd.hasOption(FILTER_COMMENTS))
			{
				pipeLine.add(new FilterComments(cmd.getOptionValue(Cli.FILTER_COMMENTS), true));
			}


			if(cmd.hasOption(FILTER_SPECIAL))
			{
				char who = cmd.getOptionValues(FILTER_SPECIAL)[0].charAt(0);
				char what = cmd.getOptionValues(FILTER_SPECIAL)[1].charAt(0);
				String regex = cmd.getOptionValues(FILTER_SPECIAL)[2];
				pipeLine.add(new FilterSpecial(who, what, regex, true));
			}


			if(cmd.hasOption(FILTER_HANDLES))
			{
				try{
					pipeLine.add(new FilterHandles(Long.parseLong(cmd.getOptionValue(FILTER_HANDLES),2)));
				}catch(NumberFormatException e){
					errPS.println("argument of "+FILTER_HANDLES+" must be a number in binary format. TERMINATE");
					return badInputArgs;
				}
			}

			//FILTER TIME !!!
			if(cmd.hasOption(FILTER_TIME))
			{
				boolean approximate = true;
				try{
					long a = (long)(Double.parseDouble(cmd.getOptionValues(FILTER_TIME)[0])* 1E9);
					long b = (long)(Double.parseDouble(cmd.getOptionValues(FILTER_TIME)[1])* 1E9);
					if(cmd.getOptionValues(FILTER_TIME).length == 3)
					{
						approximate = Boolean.parseBoolean(cmd.getOptionValues(FILTER_TIME)[2]);
					}
					if (a>b)
					{
						errPS.println("Starting time must be lower than ending. TERMINATE");
						return badInputArgs;
					}
					pipeLine.add(new FilterTime(a, b, approximate));
				}catch(NumberFormatException e){
					errPS.println("Arguments at" +FILTER_TIME+ "must be float float boolean");
					return badInputArgs;
				}

			}

			if(cmd.hasOption(CHANGE_TIME))
			{
				long delay = Long.parseLong(cmd.getOptionValue(CHANGE_TIME));
				ChangeTimeStamps cts = new ChangeTimeStamps(delay, errPS);

				pipeLine.add(cts);
			}

			if(cmd.hasOption(CHANGE_DATE_TIME))
			{
				String iso = cmd.getOptionValue(CHANGE_DATE_TIME);
				ChangeDateTime cdt = new ChangeDateTime(iso.split("T")[0],iso.split("T")[1].split("\\+")[0],"+" + iso.split("T")[1].split("\\+")[1],errPS );
				pipeLine.add(cdt);
			}

			if(cmd.hasOption(PROCESS_SIGNAL))
			{
				FilterProcessSignal filterP = new FilterProcessSignal();
				pipeLine.add(filterP);
			}

			if(cmd.hasOption(STATISTIKA))
			{
				GetInfo filter;
				String outStat = cmd.getOptionValue(STATISTIKA);
				if(outStat !=null)
				{
					filter = new GetInfo(outStat, errPS);
				}
				else
				{
					filter = new GetInfo(outPS, errPS);
				}
				pipeLine.add(filter);
			}

			if(cmd.hasOption(OUTPUT))
			{
				Pipe filterSave;
				String outDir;

				try
				{
					outDir = cmd.getOptionValue(OUTPUT);
				} catch(Exception e)
				{
					errPS.println("Option "+OUTPUT+" needs file directory and name with extension. Example './File/name.txt'. TERMINATE");
					return badInputArgs;
				}

				File tepF = new File(outDir);
				String name = tepF.getName();
				String[] parts = name.split("\\.");
				String extension = parts[parts.length - 1];

				if(parts.length == 1)
				{
					switch(extension)
					{
					case "txt": filterSave = new SaveTXT(outPS, errPS);break;
					case "csv": filterSave = new SaveCSV(outPS, errPS);break;
					case "s2":  errPS.println("s2 cant be printed to PrintStream");return badInputArgs;
					default: errPS.println("Wrong extension of output file name");return badInputArgs;
					}
				}else
				{		
					if((tepF.getParentFile() != null) && !tepF.getParentFile().exists())
					{
						errPS.println("Given directory " +tepF.getParent() +" does not exist. Creating one");
						tepF.getParentFile().mkdirs();
					}

					switch(extension)
					{
					case "txt": filterSave = new SaveTXT(outDir, errPS);break;
					case "csv": filterSave = new SaveCSV(outDir, errPS);break;
					case "s2": {
						SaveS2 saveFilter = new SaveS2(outDir, errPS);
						filterSave = saveFilter;
						if (cmd.hasOption(INPUT))
							saveFilter.setSourceFilePath(cmd.getOptionValues(INPUT)[0]);
						break;
					}
					default: errPS.println("Wrong extension of output file name");return badInputArgs;
					}
				}

				pipeLine.add(filterSave);
			}


			//***************************************          COMBINING           **************************
			//***************************************          EXECUTING           **************************
			if(pipeLine.size() > 0)
			{
				for(int i = 1;i<pipeLine.size();i++)
				{
					pipeLine.get(i-1).addChild(pipeLine.get(i));
				}
				loadS1.readLines(pipeLine.get(0), false);
				
				if(file1.getNotes().length() > 0)
				{
					errPS.println("S2 notes are : ");
					errPS.print(file1.getNotes());
				}
				loadS1.closeFile();
				if(loadS2 != null) loadS2.closeFile();
			}else
			{
				errPS.println("No flags were detected");
				return unknown;
			}



		}
		else
		{
			//TODO generate could replace input file in Cli ?

			if(cmd.hasOption(GENERATE_RANDOM) & cmd.hasOption(OUTPUT) & cmd.hasOption(FILTER_TIME))
			{

				String outDir = cmd.getOptionValue(OUTPUT);
				File tepF = new File(outDir);
				//				if(tepF.getParentFile() == null)
				//				{
				//					errPS.println("Output needs directory and name. Not just name.");
				//					return badInputArgs;
				//				}
				if(tepF.getName().split("\\.").length != 2)
				{
					errPS.println("Name in given directory mush have extension .s2");
					return badInputArgs;
				}				

				long a = (long)(Double.parseDouble(cmd.getOptionValues(FILTER_TIME)[0])* 1E9);
				long b = (long)(Double.parseDouble(cmd.getOptionValues(FILTER_TIME)[1])* 1E9);

				String[] tem = cmd.getOptionValues(GENERATE_RANDOM);
				long seed = Long.parseLong(tem[0]);
				float frequency = Float.parseFloat(tem[1]);
				float frequencyChange = Float.parseFloat(tem[2]);
				float percentageMissing = Float.parseFloat(tem[3]);
				long normalDelay = (long) (Double.parseDouble(tem[4])*1E9);
				if(normalDelay < 0)
				{
					errPS.println("Normal delay must be bigger or equal to 0.");
					return badInputArgs;
				}
				float bigDelayChance = Float.parseFloat(tem[5]);
				Long bigDelay = (long) (Double.parseDouble(tem[6])*1E9);
				int numDisconects = Integer.parseInt(tem[7]);
				int indexSB = Integer.parseInt(tem[8]);
				int valueSB = Integer.parseInt(tem[9]);

				@SuppressWarnings("unused")
				Generator2 g = new Generator2(outDir, errPS, a, b, seed, frequency, frequencyChange, percentageMissing, normalDelay, bigDelayChance, bigDelay, numDisconects, indexSB, valueSB);
			}else
			{
				if(cmd.hasOption(GENERATE_RANDOM))
				{
					errPS.println("Option "+GENERATE_RANDOM+"-generate s2 PCARD needs option o-out and option t-time. TERMINATE");
					return badInputArgs;
				}
			}


			if(cmd.hasOption(GENERATE_FROM_FILE) & cmd.hasOption(OUTPUT) & cmd.hasOption(FILTER_TIME))
			{
				//output
				String outDir = cmd.getOptionValue(OUTPUT);
				File tepF = new File(outDir);
				if(tepF.getParentFile() == null)
				{
					errPS.println("Output needs directory and name. Not just name.");
					return badInputArgs;
				}
				if(tepF.getName().split("\\.").length != 2)
				{
					errPS.println("Name in given directory mush have extension .s2");
					return badInputArgs;
				}

				//

				long start = (long)(Double.parseDouble(cmd.getOptionValues(FILTER_TIME)[0])* 1E9);
				long end = (long)(Double.parseDouble(cmd.getOptionValues(FILTER_TIME)[1])* 1E9);

				String[] tem = cmd.getOptionValues(GENERATE_FROM_FILE);
				if(! new File(tem[0]).exists())
				{
					errPS.println("File from first argument of option "+GENERATE_FROM_FILE+" doesnt exist.");
					return badInputArgs;
				}
				if(! new File(tem[1]).exists())
				{
					errPS.println("File from second argument of option "+GENERATE_FROM_FILE+" doesnt exist.");
					return badInputArgs;
				}
				if(! new File(tem[2]).exists())
				{
					errPS.println("File from third argument of option "+GENERATE_FROM_FILE+" doesnt exist.");
					return badInputArgs;
				}
				if(! new File(tem[3]).exists())
				{
					errPS.println("File from fourth argument of option "+GENERATE_FROM_FILE+" doesnt exist.");
					return badInputArgs;
				}


				@SuppressWarnings("unused")
				Generator3 g = new Generator3(outDir, errPS, start, end, tem[0], tem[1], tem[2], tem[3]);


			}

		}
		errPS.println("CLI finished");
		return good;
	}
}
