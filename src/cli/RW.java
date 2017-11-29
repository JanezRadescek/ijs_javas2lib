package cli;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import s2.OutS2Callback;
import s2.OutputCallback;
import s2.S2;
import s2.S2.LoadStatus;
import s2.StatisticsCallback;

/**bere , izrezuje, ... s2 file */
public class RW {

	public static void main(String[] args){
		
		//parsanje vhodnih podatkov haahah
		Options options = new Options();
		
		options.addOption("s", false, "izpisi statistiko na izhod");
		options.addOption("c", false, "izrezi del in izpisi na izhod");
		
		Option time = new Option("t", "zacetni in koncni cas izseka, ki nas zanima -t zacetni koncni");
		time.setArgs(2);
		options.addOption(time);
		
		Option input = new Option("i", "vhodna datoteka, ima 2 argumenta pot in ime datoteke");
		input.setArgs(2);
		options.addOption(input);
		
		Option output = new Option("o", "'lokacija+ime+koncnica' IN koncnica izhodne datoteke");
		output.setArgs(2);
		options.addOption(output);
		
		Option handle = new Option("h",true ,"Handle, ki ga želimo izpisati");
		options.addOption(handle);
		
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("vhodni podatki so napačni ali manjkajoči");
			return;
		}
		
		
		
		//priprava S2
		S2 file;
		S2.LoadStatus ls;
		
		if(cmd.hasOption("i"))
		{
			File inDirectory = new File(cmd.getOptionValues("i")[0]);
			String inFname = cmd.getOptionValues("i")[1];
			file = new S2();
			ls = file.load(inDirectory, inFname);
		
			//dodajanje callbackov glede na vhodne podatke
			
			if(cmd.hasOption("s"))
			{
				String izhodna = cmd.getOptionValues("o")[0];
				outStatistics(file, ls, izhodna);
			}
			
			if(cmd.hasOption("c"))
			{
				long a;
				long b;
				String izhodDir;
				String izhodName;
				if(cmd.hasOption("t"))
				{
					try{
					float aa = Float.parseFloat(cmd.getOptionValues("t")[0])* 1000000000;
					float bb = Float.parseFloat(cmd.getOptionValues("t")[1])* 1000000000;
					a = (long) aa;
					b = (long) bb;
					}catch(NumberFormatException e){
						System.out.println("Argumenta pri t morata biti float");
						return;
					}
					if (a>=b) System.out.println("Zacetni cas mora biti mansi od koncnega");
				}else{
					a = 0;
					b = Long.MAX_VALUE;
				}
				if(cmd.hasOption("o"))
				{
					try{
						izhodDir = cmd.getOptionValues("o")[0];
						izhodName = cmd.getOptionValues("o")[1];
					} catch(Error e)
					{
						System.out.println("ni zadosti argumentov");
						return;
					}
				}else
				{
					izhodDir = null;
					izhodName = null;
				}
				
				String theH = cmd.getOptionValue("h");
				byte theHandle;
				try{
					theHandle = Byte.parseByte(theH);
				}catch(NumberFormatException e){
					System.out.println("argument od h mora biti stevilka");
					return;
				}
				outData(file, ls, a, b, theHandle,izhodDir, izhodName);
			}
		
			
			//preberemo S2 in obdelamo
			System.err.println("using file "+file.getFilePath()+"\n");
			
			boolean everythingOk = (ls.readAllLinesAndProcess());
			
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

	private static void outData(S2 file, LoadStatus ls, long a, long b, byte theHandle, String izhodDir, String izhodName) {
	
		
		if(izhodDir != null && izhodName != null){
			String kon = izhodName.split("\\.")[1];
			if(kon.equals("s2"))
			{
				OutS2Callback callback = new OutS2Callback(file, a, b, theHandle, izhodDir, izhodName);
				ls.addReadLineCallback(callback);
			}else
			{
				OutputCallback callback = new OutputCallback(file, a, b, theHandle, izhodDir, izhodName, kon);
				ls.addReadLineCallback(callback);
			}
			
		}else {
				OutputCallback callback = new OutputCallback(file, a, b, theHandle);
				ls.addReadLineCallback(callback);
		}
		
		//ls.addReadLineCallback(callback);
		
	}

	//izpiše osnovno statistiko
	//                0              1             2                  3
	//args = ["outstatistics","C:\\user\admin\","test.s2", system.out /\ dire  name ]    file= C:\\user out.txt
	private static void outStatistics(S2 file, LoadStatus ls, String izhodna) {
			
		if(izhodna != null){
			String directoryANDname = izhodna;
			StatisticsCallback callback = new StatisticsCallback(file, directoryANDname);
			ls.addReadLineCallback(callback);
		}
		
		else {
			StatisticsCallback callback = new StatisticsCallback(file);
			ls.addReadLineCallback(callback);
		}
		
	}


}
