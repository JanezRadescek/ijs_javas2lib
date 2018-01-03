package callBacks;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import s2.S2;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;


/**
 * Callback for basic information about S2 file
 * */
public class StatisticsCallback implements ReadLineCallbackInterface{

	S2 s2;
	PrintStream out;
	
	int counters[] = new int[5];
	String countersNames[] = {"Comments", "Definitions", "Timestamps", "Unknown", "Error"};
	

	//shranjuje število podatkovnih paketov glede na handle
	Map<Byte,Integer[]> packetCounters= new HashMap<Byte,Integer[]>();
	
	//start time and end time relative to time and date writen in meta 
	long startTime;
	long endTime;
	//helps us with finding start time
	boolean start = false;
	
	//save meta for later output
	Map<String,String> metaData = new HashMap<String, String>();
	//special message counter based on type
	Map<Character,Integer> special = new HashMap<Character,Integer>();
	//save representation of structdefinition
	Map<Byte,String> structDefinitions = new HashMap<Byte, String>();
	
	//version
	int ver;
	String extendedVer;
	
	/**
	 * Create callback which will write basic information on STDOUT
	 * @param s2 - S2 file from which we read
	 */
	public StatisticsCallback(S2 s2)
	{
		this.s2 = s2;
		this.out = System.out;
	}
	
	/**
	 * Create callback which will write basic information in txt file
	 * @param s2 - S2 file from which we read
	 * @param directoryANDname - the system-dependent filename
	 */
	public StatisticsCallback(S2 s2, String directoryANDname)
	{
		this.s2 = s2;
		try {
			this.out = new PrintStream(new FileOutputStream(directoryANDname));
			System.out.println("writing statistics into " + directoryANDname);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
	}
	
	
	private void izpisi()
	{
		out.println("End of file");
		out.println(ver + " " + extendedVer);
		float trajanje = ((float)((endTime - startTime) / 1000000))/1000;
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
		
		//stream represantation
		out.println("Stream represantation: ");
		for(byte key:structDefinitions.keySet())
		{
			out.println("	stream " + key +" : " + structDefinitions.get(key));
		}
		
		//packets
		out.println("Packets per stream: ");
		for(Byte key:packetCounters.keySet())
		{
			out.println("	stream " + key + " : " + packetCounters.get(key)[0]);
		}
		//samples
		out.println("Samples per stream: ");
		for(Byte key:packetCounters.keySet())
		{
			out.println("	stream " + key + " : " + packetCounters.get(key)[1]);
		}
		
		if(counters[3]>0 || counters[4]>0)
		{
			out.println("Unknown Lines : "  + counters[3]);
			out.println("Errors : " + counters[4]);
		}
		
		out.close();
	}
	
	
	public boolean onComment(String comment)
	{
		counters[0]++;
		return true;
	}

	public boolean onVersion(int versionInt, String extendedVersion)
    {
		ver = versionInt;
		this.extendedVer = extendedVersion;
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
		izpisi();
		
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile()
	{
		izpisi();
		
		return false;
	}

	public boolean onDefinition(byte handle, SensorDefinition definition)
	{
    	counters[1]++;
		return true;
	}

	public boolean onDefinition(byte handle, StructDefinition definition)
	{
		structDefinitions.put(handle, definition.elementsInOrder);
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
			endTime = nanoSecondTimestamp;
			start = true;
		}
		else{
			endTime = nanoSecondTimestamp;
		}
		return true;
	}

	public boolean onStreamPacket(byte handle, long timestamp, int len, byte data[]) {
		
		if (!start)
		{
			startTime = timestamp;
			endTime = timestamp;
			start = true;
		}
		else{
			endTime = timestamp;
		}
		int dataCounter = 0;
		
		//deprecated
		/*
		for (int i = 0; i < s2.getEntityHandles(handle).elementsInOrder.length(); ++i) {
            byte cb = (byte) s2.getEntityHandles(handle).elementsInOrder.charAt(i);
            //TODO spremeni stetje
            if (cb == 'e'){dataCounter++;}
		}*/
		
		dataCounter = s2.getEntityHandles(handle).elementsInOrder.length();
		
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
		counters[3]++;
		return true;
	}

	public boolean onError(int lineNum,  String error)
	{
		counters[4]++;
		return true;
	}
}
