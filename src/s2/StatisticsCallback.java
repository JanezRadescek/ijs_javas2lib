package s2;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;


/**osnutek callbacka ki prešteje število vseh podatkov*/
public class StatisticsCallback implements ReadLineCallbackInterface{

	S2 s2;
	PrintStream out;
	
	int counters[] = new int[3];
	String countersNames[] = {"Comments", "Definitions", "Timestamps"};
	

	//shranjuje število podatkovnih paketov glede na handle
	Map<Byte,Integer[]> packetCounters= new HashMap<Byte,Integer[]>();
	
	long startTime;
	long endTime;
	boolean start = false;
	//shrani meta podatke
	Map<String,String> metaData = new HashMap<String, String>();
	//shranjuje število posebnih mesegev glede na tip
	Map<Character,Integer> special = new HashMap<Character,Integer>();
	
	public StatisticsCallback(S2 file)
	{
		s2 = file;
		this.out = System.out;
	}
	
	public StatisticsCallback(S2 file, String directoryANDname)
	{
		s2 = file;
		try {
			this.out = new PrintStream(new FileOutputStream(directoryANDname, true));
			System.out.println("writing statistics into " + directoryANDname);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
	}
	
	
	public boolean onComment(String comment)
	{
		
		counters[0]++;
		
		return true;
	}

	public boolean onVersion(int versionInt, String extendedVersion)
    {
    	return true;
    }

	public boolean onSpecialMessage(char who, char what, String message)
	{
		if (special.containsKey(what))
		{
			special.put(what, special.get(what) + 1);
		}
		else
		{
			special.put(what, 1);
		}
		
		return true;
	}

	public boolean onMetadata(String key, String value)
	{
		metaData.put(key, value);
		
		return true;
	}

	public boolean onEndOfFile()
	{
		out.println("End of file");
		for(int podatek = 0;podatek<counters.length;podatek++){
			out.println(countersNames[podatek] + " : " + counters[podatek]);
		}
		
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile()
	{
		out.println("Unmarked End of file");
		//time
		float trajanje = ((float)Math.round(((endTime - startTime) / 1000000)))/1000;
		float st = ((float)(startTime/1000000))/1000;
		float et = ((float)(endTime/1000000))/1000;
		out.println("Start Time at : " + st + "s");
		out.println("End time at : " + et + "s");
		out.println("Total time : " + trajanje + "s");
		//metadata
		out.println("metaData : ");
		String[] potrebni = {"time", "date", "timezone"};
		for(String key:potrebni)
		{
			out.println("	" + key + " : " + metaData.get(key));
		}
		
		
		out.println("Special messeges");
		for(char key:special.keySet())
		{
			out.println("	" +  key + " : " + special.get(key));
		}
		
		for(int podatek = 0;podatek<counters.length;podatek++){
			out.println(countersNames[podatek] + " : " + counters[podatek]);
		}
		
		
		out.println("Number of streams : " + packetCounters.size());
		
		//packets
		out.println("Packets per stream: ");
		for(Byte key:packetCounters.keySet())
		{
			out.println("	" + key + " : " + packetCounters.get(key)[0]);
		}
		
		out.println("Samples per stream: ");
		for(Byte key:packetCounters.keySet())
		{
			out.println("	" + key + " : " + packetCounters.get(key)[1]);
		}
		
		return false;
	}

	public boolean onDefinition(byte handle, SensorDefinition definition)
	{
    	counters[1]++;
		return true;
	}

	public boolean onDefinition(byte handle, StructDefinition definition)
	{
		counters[1]++;
		return true;
	}

	public boolean onDefinition(byte handle, TimestampDefinition definition)
	{
		counters[1]++;
		return true;
	}

	public boolean onTimestamp(long nanoSecondTimestamp) {
		counters[2]++;
		if (!start)
		{
			startTime = nanoSecondTimestamp;
			start = true;
		}
		else{
			endTime = nanoSecondTimestamp;
		}
		return true;
	}

	public boolean onStreamPacket(byte handle, long timestamp, int len, byte data[]) {
		
		endTime = timestamp;
		int dataCounter = 0;
		
		for (int i = 0; i < s2.getEntityHandles(handle).elementsInOrder.length(); ++i) {
            byte cb = (byte) s2.getEntityHandles(handle).elementsInOrder.charAt(i);
            if (cb == 'e'){dataCounter++;}
		}
		
		if (packetCounters.containsKey(handle))
		{
			packetCounters.put(handle,new Integer[] {packetCounters.get(handle)[0]+1,packetCounters.get(handle)[1]+ dataCounter});
		}
		else
		{
			packetCounters.put(handle, new Integer[] {1, dataCounter});
		}
		
		return true;
	}

	public boolean onUnknownLineType(byte type, int len, byte data[])
	{
		return true;
	}

	public boolean onError(int lineNum,  String error)
	{
		return true;
	}
	
	
}
