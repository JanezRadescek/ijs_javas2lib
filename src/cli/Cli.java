package cli;

import java.lang.Exception;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import org.apache.commons.cli.*;

import callBacks.FirtstReader;
import callBacks.SecondReader;
import filtersOld.FilterProcessSignal;
import generatorS2.Generator2;
import pipeLines.Connector;
import pipeLines.Pipe;
import pipeLines.conglomerates.SmartMerge;
import pipeLines.filters.ChangeTimeStamps;
import pipeLines.filters.FilterData;
import pipeLines.filters.FilterHandles;
import pipeLines.filters.GetInfo;
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

	private static final int good = 0;
	private static final int unknown = 1;
	private static final int fileError = 2;
	private static final int badInputArgs = 3;


	public static final String STATISTIKA = "s";
	//public static final String READ = "r";
	public static final String MEARGE = "m";
	public static final String HELP = "help";
	public static final String CHANGE_TIME = "ct";
	public static final String PROCESS_SIGNAL = "p";
	public static final String GENERATE = "g";


	public static final String TIME = "t";
	public static final String INPUT = "i";
	public static final String OUTPUT = "o";
	public static final String HANDLES = "h";
	public static final String DATA = "d";



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


		Option statistika = new Option(STATISTIKA,true, "Output statistics of input S2 file. Has optional argument directory and name of file for outputing statistics. "
				+ "If argument is mising it will print statistics to outPS (default is System.out). DO NOT confuse this argument with flag -o output.");
		statistika.setArgs(1);
		statistika.setOptionalArg(true);;
		options.addOption(statistika);

		//options.addOption(READ, false, "read. izrezi del in izpisi na izhod v CSV in human readable form");
		options.addOption(MEARGE, true, "mearge. Needs two inputs, and optional output. Combining with other filters has undefined behavior! Combines two S2 files in one S2 file. Has mendatory argument."
				+ " If true streams with same hendels will be merged,"
				+ " else strems from second file will get new one where needed");
		options.addOption(HELP, false, "Prints Help. Other flags will be ignored.");
		options.addOption(CHANGE_TIME, true, "add time in argument to all timestamps. "
				+ "If added time is negative and its absolute value bigger than value of firtst time stamp, "
				+ "added time will be set to -first time, resulting in new first time being 0.");
		options.addOption(PROCESS_SIGNAL,false, "Proces signal. If argument is true it will process"
				+ " as if the frequency of sensor is constant. Simple processsing. Otherwise it will split into intervals");

		Option generate = new Option(GENERATE, "Generates S2 PCARD based on arguments. Needs option time and output. Arguments: \n" 
				+ "seed \n"
				+ "frequency \n"
				+ "frequencyChange \n"
				+ "percentigeMissing \n"
				+ "normalDelay \n"
				+ "bigDelayChance \n"
				+ "bigDelayFactor \n"
				+ "#pauses");
		generate.setArgs(8);
		options.addOption(generate);

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

		Option output = new Option(OUTPUT, true, "General output for result of other flags. If Argument is valid Directory and name with extension it will output into specifed file."
				+ " If argument equals 'xyz' where 'xyz' is file extension it will print result to the outPUT stream (Default is System.out)."
				+ "Type of output will be based on extension of the name. Possible extensions are 'csv', 's2' and 'txt' ");
		options.addOption(output);

		Option handle = new Option(HANDLES,true ,"handles. Handles, we want to use." +
				"Argument represent wanted handles. " +
				"If we want handle with num. i there has to be 1 on i+1 position from right to left in argument. If we dont want it there have to be 0." +
				"If we want to keep only handles with 0 and 4 we pass '10001'" );
		options.addOption(handle);

		Option dataTypes = new Option(DATA,true, "datatype. data types we want to keep. " +
				"Argument must be a number in binary form"+
				".*1=keeps comments, .*1.=keeps Special, .*1..=keeps meta");
		options.addOption(dataTypes);



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

			formatter.printHelp("Cli",header,options,footer);
			errPS.println("Exception caught while parting input arguments:");
			e.printStackTrace(errPS);

			return unknown;
		}
		if(cmd.hasOption(HELP))
		{
			formatter.printHelp("Cli",header,options,footer);
			return good;
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
				//inFname1 = cmd.getOptionValues("i")[1];
			}catch(Exception e)
			{
				errPS.println("Option i need directory and name of input S2 file. TERMINATE");
				return badInputArgs;
			}
			file1 = new S2();
			loadS1 = file1.load(inDirectory1.getParentFile(), inDirectory1.getName());


			//******************************************************************
			//******************************************************************
			ArrayList<Pipe> pipeLine = new ArrayList<Pipe>();
			//******************************************************************
			//******************************************************************



			if(cmd.hasOption(MEARGE))
			{
				try
				{
					File inDirectory2 = new File(cmd.getOptionValues(INPUT)[1]);
					file2 = new S2();
					loadS2 = file2.load(inDirectory2.getParentFile(), inDirectory2.getName());
					Pipe pipeP = new Pipe(); 
					Pipe pipeS = new Pipe();
					SmartMerge sm = new SmartMerge(loadS2, pipeS, pipeP, pipeS, inDirectory1, inDirectory2, false, false, errPS);

					Connector con = new Connector();
					con.addStart(pipeP);
					sm.getPrimaryOutPut().addChild(con.getEnd());
					
					pipeLine.add(con);
				}
				catch(Exception r)
				{
					errPS.println("Option i needs directory and name of second input file. TERMINATE");
					return badInputArgs;
				}
			}



			if(cmd.hasOption(DATA))
			{
				try
				{
					pipeLine.add(new FilterData(Byte.parseByte(cmd.getOptionValue(DATA),2),errPS));
				}catch(NumberFormatException e){
					errPS.println("argument of "+DATA+" must be a number in binary format. TERMINATE");
					return badInputArgs;
				}
			}


			if(cmd.hasOption(HANDLES))
			{
				try{
					pipeLine.add(new FilterHandles(Long.parseLong(cmd.getOptionValue(HANDLES),2)));
				}catch(NumberFormatException e){
					errPS.println("argument of "+HANDLES+" must be a number in binary format. TERMINATE");
					return badInputArgs;
				}
			}

			//FILTER TIME !!!
			if(cmd.hasOption(TIME))
			{
				boolean nonEss = true;
				try{
					long a = (long)(Double.parseDouble(cmd.getOptionValues(TIME)[0])* 1E9);
					long b = (long)(Double.parseDouble(cmd.getOptionValues(TIME)[1])* 1E9);
					if(cmd.getOptionValues(TIME).length == 3)
					{
						nonEss = Boolean.parseBoolean(cmd.getOptionValues(TIME)[2]);
					}
					if (a>b)
					{
						errPS.println("Starting time must be lower than ending. TERMINATE");
						return badInputArgs;
					}
					pipeLine.add(new FilterTime(a, b, nonEss));
				}catch(NumberFormatException e){
					errPS.println("Arguments at" +TIME+ "must be float float boolean");
					return badInputArgs;
				}

			}

			if(cmd.hasOption(CHANGE_TIME))
			{
				long delay = Long.parseLong(cmd.getOptionValue(CHANGE_TIME));
				ChangeTimeStamps cts = new ChangeTimeStamps(delay, errPS);

				pipeLine.add(cts);
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
					try {
						filter = new GetInfo(new PrintStream(new File(outStat)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						outPS.println("File couldnt be made");
						return fileError;
					}
				}
				else
				{
					filter = new GetInfo(outPS);
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
					if(!tepF.getParentFile().exists())
					{
						errPS.println("Given directory " +tepF.getParent() +" does not exist. Creating one");
						tepF.getParentFile().mkdirs();
					}
					
					switch(extension)
					{
					case "txt": try {
						filterSave = new SaveTXT(new PrintStream(new File(outDir)), errPS);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						return badInputArgs;
					}break;
					case "csv": filterSave = new SaveCSV(outDir, errPS);break;
					case "s2":  filterSave = new SaveS2(outDir, errPS);break;
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
			}else
			{
				errPS.println("No flags were detected");
				return unknown;
			}

			if(file1.getNotes().length() > 0)
			{
				errPS.print(file1.getNotes());
			}

		}
		else
		{
			if(cmd.hasOption(GENERATE) & cmd.hasOption(OUTPUT) & cmd.hasOption(TIME))
			{
				String outDir = cmd.getOptionValue(OUTPUT);
				File tepF = new File(outDir);
				if(!tepF.getParentFile().exists())
				{
					errPS.println("Given directory " +tepF.getParent() +" does not exist. Creating one");
					tepF.getParentFile().mkdirs();
				}
				long a = (long)(Double.parseDouble(cmd.getOptionValues(TIME)[0])* 1E9);
				long b = (long)(Double.parseDouble(cmd.getOptionValues(TIME)[1])* 1E9);
				
				String[] tem = cmd.getOptionValues(GENERATE);
				long seed = Long.parseLong(tem[0]);
				float frequency = Float.parseFloat(tem[1]);
				float frequencyChange = Float.parseFloat(tem[2]);
				float percentigeMissing = Float.parseFloat(tem[3]);
				long normalDelay = Long.parseLong(tem[4]);
				float bigDelayChance = Float.parseFloat(tem[5]);
				float bigDelayFactor = Float.parseFloat(tem[6]);
				int numPauses = Integer.parseInt(tem[7]);

				@SuppressWarnings("unused")
				Generator2 g = new Generator2(outDir, errPS, a, b, seed, frequency, frequencyChange, percentigeMissing, normalDelay, bigDelayChance, bigDelayFactor, numPauses);
			}else
			{
				if(cmd.hasOption(GENERATE))
				{
					errPS.println("Option "+GENERATE+"-generate s2 PCARD needs option o-out and option t-time. TERMINATE");
					return badInputArgs;
				}
			}

		}
		errPS.println("CLI finished");
		return good;
	}
}
