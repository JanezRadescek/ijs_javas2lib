package cli;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import s2.OutS2Callback;
import s2.SecondReader;
import s2.FirtstReader;
import s2.OutCSVCallback;
import s2.S2;
import s2.S2.LoadStatus;
import s2.StatisticsCallback;

/**bere , izrezuje, ... s2 file */
public class RW {

	public static void main(String[] args){
		
		//parsanje vhodnih podatkov haahah
		Options options = new Options();
		
		options.addOption("s", false, "statistics. izpisi statistiko na izhod");
		options.addOption("c", false, "cut. izrezi del in izpisi na izhod");
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
				"Če želimo i-ti handle mora biti na i+1 mestu argumenta števka 1." +
				"Če je drugi argument 0 bodo handli z 1 odstranjeni." +
				" long flags");
		handle.setOptionalArg(true);
		options.addOption(handle);
		
		Option dataTypes = new Option("d",true, "datatype. Tipi vrstic, ki jih želimo izpustiti.Privzeto vsi. 1števka=comment, 2števka=Special, 3števka=meta");
		options.addOption(dataTypes);
		
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("vhodni podatki so napačni ali manjkajoči");
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
		
			//priprava vhodnih podatkov
			
			long[] ab = new long[]{0,Long.MAX_VALUE};
			String izhodDir = null;
			String izhodName = null;
			long handles = Long.MAX_VALUE;
			byte dataT = Byte.MAX_VALUE;
			
			
			if(cmd.hasOption("v"))
			{
				try
				{
					inDirectory2 = new File(cmd.getOptionValues("i")[0]);
					inFname2 = cmd.getOptionValues("i")[1];
				}
				catch(Error r)
				{
					System.out.println("v potrebuje pot in ime");
					return;
				}
			}
			
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
			
			if(cmd.hasOption("h"))
			{
				try{
					handles = Long.parseLong(cmd.getOptionValue("h"));
				}catch(NumberFormatException e){
					System.out.println("argument od h mora biti stevilka");
					return;
				}
				try
				{
					String s = cmd.getOptionValues("h")[2];
					handles *= -1;
					
				}catch(ArrayIndexOutOfBoundsException e)
				{
					
				}
			}
			
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
			
			if(cmd.hasOption("b") && cmd.hasOption("v"))
			{
				file2 = new S2();
				loadS2 = file2.load(inDirectory2, inFname2);
				
				build(file1, loadS1, file2, loadS2, izhodDir, izhodName);
			}else if (cmd.hasOption("b") || cmd.hasOption("v"))
			{
				System.out.println("če izberemo b ali v moramo podati tudi drugega");
				return;
			}
			
			if(cmd.hasOption("s"))
			{
				outStatistics(file1, loadS1, izhodDir, izhodName);
			}
			
			if(cmd.hasOption("c"))
			{
				outData(file1, loadS1, ab,izhodDir, izhodName, handles, dataT);
			}
			
			
		
			
			
			System.err.println("using file "+file1.getFilePath()+"\n");
			//preberemo prvi S2 in obdelamo
			boolean everythingOk = (loadS1.readAllLinesAndProcess());
			
			//samo opcija b potrebuje drugi file
			if (file2 != null && loadS2 != null)
			{
				System.err.println("using file "+file2.getFilePath()+"\n");
				everythingOk &= loadS2.readAllLinesAndProcess();
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

	private static void build(S2 file1, LoadStatus loadS1, S2 file2, LoadStatus loadS2, String izhodDir,
			String izhodName) {
		//hermione prebere prvo datoteke in prebrane podatke zapiše v boba, slednji bo prebral še drugo datoteko in 
		//podatke združil v nov S2
		SecondReader bob = new SecondReader(file2, izhodDir, izhodName);
		FirtstReader gre = new FirtstReader(file1, bob);
		
		loadS1.addReadLineCallback(gre);
		loadS2.addReadLineCallback(bob);
		
	}

	private static void outData(S2 file, LoadStatus ls, long []ab, String izhodDir, String izhodName, long handles, byte dataT) {
	
		
		if(izhodDir != null && izhodName != null){
			String kon = izhodName.split("\\.")[1];
			if(kon.equals("s2"))
			{
				OutS2Callback callback = new OutS2Callback(file, ab, handles, dataT, izhodDir, izhodName);
				ls.addReadLineCallback(callback);
			}else
			{
				OutCSVCallback callback = new OutCSVCallback(file, ab, handles, izhodDir, izhodName, kon);
				ls.addReadLineCallback(callback);
			}
			
		}else {
				OutCSVCallback callback = new OutCSVCallback(file, ab, handles);
				ls.addReadLineCallback(callback);
		}
		
		//ls.addReadLineCallback(callback);
		
	}

	//izpiše osnovno statistiko
	//                0              1             2                  3
	//args = ["outstatistics","C:\\user\admin\","test.s2", system.out /\ dire  name ]    file= C:\\user out.txt
	private static void outStatistics(S2 file, LoadStatus ls, String outDirectory, String outName) {
			
		if(outDirectory != null){
			String directoryANDname = outDirectory + "\\" + outName;
			StatisticsCallback callback = new StatisticsCallback(file, directoryANDname);
			ls.addReadLineCallback(callback);
		}
		
		else {
			StatisticsCallback callback = new StatisticsCallback(file);
			ls.addReadLineCallback(callback);
		}
		
	}


}
