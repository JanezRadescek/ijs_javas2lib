package pipeLines.filters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;
import suportingClasses.S2utilities;


/**
 * Creates basic Information about S2 file and print it to PrintStream From constructor.
 * @author janez
 *
 */
public class GetInfo extends Pipe {

	PrintStream printer;
	
	boolean close = false;//if we made printstrem we close it

	boolean start = false;
	boolean end = false;
	boolean printAfter = false;

	private Map<String, Integer> generalCounter = new HashMap<String, Integer>();
	private Map<Byte, Integer> stremCounter = new HashMap<Byte, Integer>();
	private Map<Character, Integer> sensorCounter = new HashMap<Character, Integer>();
	long startTime = 0;


	//for counting lost packets/samples and calculating frequeny   PCARD only
	long lastPacketCounter = 0;
	long lostPackets = 0;
	long lostSamples = 0;
	long lastTime = 0;
	ArrayList<Long> dif = new ArrayList<Long>();  

	//save meta for later output
	Map<String,String> metaData = new HashMap<String, String>();
	//special message counter based on type
	Map<Character,Integer> special = new HashMap<Character,Integer>();
	//save representation of structdefinition
	Map<Byte,StructDefinition> structDefinitions = new HashMap<Byte, StructDefinition>();
	Map<Byte,SensorDefinition> sensorDefinitions = new HashMap<Byte, SensorDefinition>();
	ArrayList<String> annotationsM= new ArrayList<String>();
	ArrayList<Long> annotationsT= new ArrayList<Long>();

	//version
	int versionInt;
	String version;

	/**
	 * @param print PrintStream on which we will write data
	 * @param errPS errPS
	 * 
	 */
	public GetInfo(PrintStream print, PrintStream errPS)
	{
		this(print, true, errPS);
	}

	/**
	 * @param directory
	 * @param errPS
	 */
	public GetInfo(String directory, PrintStream errPS)
	{
		this.errPS = errPS;
		try {
			this.printer = new PrintStream(new FileOutputStream(new File(directory)));
		} catch (FileNotFoundException e) {
			e.printStackTrace(errPS);
		}
		this.printAfter = true;
		this.close = true;
		
	}

	public GetInfo(PrintStream print, boolean printAfter, PrintStream errPS)
	{
		this.errPS = errPS;
		this.printer = print;
		this.printAfter = printAfter;
	}


	/**
	 * writes info
	 * @param print PrintStrem on which all info will be written
	 */
	public void izpisi(PrintStream print)
	{
		this.printer = print;
		izpisi();
	}



	/**
	 * writes info
	 * If Output PrintStream is null it will use System.out
	 * if called beffore S2 has been read it wont print enything.
	 */
	public void izpisi()
	{
		if(printer == null)
		{
			printer = System.out;
		}
		if(!end)
		{
			return;
		}
		printer.println(versionInt + " " + version);
		float trajanje = ((float)((lastTime - startTime) / 1000000))/1000;
		float st = ((float)(startTime/1000000))/1000;
		float et = ((float)(lastTime/1000000))/1000;
		printer.println("Start Time at : " + st + "s");
		printer.println("End time at : " + et + "s");
		printer.println("Total time : " + trajanje + "s");
		//metadata
		printer.println("metaData : ");
		//TODO print all meta not just this one
		/*
		String[] potrebni = {"time", "date", "timezone"};
		for(String key:potrebni)
		{
			errPS.println("	" + key + " : " + metaData.get(key));
		}*/

		for(String key:metaData.keySet())
		{
			printer.println("	" + key + " : " + metaData.get(key));
		}

		printer.println("Special messeges : ");
		for(char key:special.keySet())
		{
			printer.println("	" +  key + " : " + special.get(key));
		}

		for(String key:generalCounter.keySet())
		{
			printer.println(key +" : "+ generalCounter.get(key));
		}


		printer.println("Number of streams : " + stremCounter.size());

		//stream represantation
		printer.println("Stream represantation: ");
		for(byte key:structDefinitions.keySet())
		{
			printer.println("	stream " + key +" : " + structDefinitions.get(key).elementsInOrder);
		}

		//packets
		printer.println("Packets per stream: ");
		for(byte key:stremCounter.keySet())
		{
			printer.println("	stream " + key + " : " + stremCounter.get(key));
		}
		//samples
		printer.println("Data per sensor type: ");
		for(char key:sensorCounter.keySet())
		{
			printer.println("	sensor " + (char)key + " : " + sensorCounter.get(key));
		}

		if(generalCounter.containsKey("Unknown Lines"))
			printer.println("Unknown Lines : "  + generalCounter.get("Unknown Lines"));
		if(generalCounter.containsKey("Errors"))
			printer.println("Errors : "  + generalCounter.get("Errors"));

		if(version.equals("PCARD"))
		{
			printer.println();
			printer.println("PCARD specific statistics:");
			printer.println(" Min lost packets : " + lostPackets);
			
			if(dif.size() > 0)
			{
				long sum = 0;
				for(long f : dif)
				{
					sum += f;
				}
				if(sum > 0)
				{
					printer.println(" Mean frequency of " + dif.size() + " sequential packets : " + dif.size()*14E9/sum);
				}
				else
				{
					errPS.println("their are packets with 0 time diffference.");
				}
			}


			if(annotationsM.size() > 0)
			{
				printer.println(" Annotations : ");
				for(int i = 0; i<annotationsM.size(); i++)
				{
					printer.println("  Time : " + annotationsT.get(i) + " message : " + annotationsM.get(i));
				}
			}
		}

		if(close) this.printer.close();
	}

	@Override
	public boolean onComment(String comment) {
		generalCounter.merge("comment", 1, Integer::sum);


		return pushComment(comment);
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		this.versionInt = versionInt;
		this.version = version;

		return pushVersion(versionInt, version);
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		special.merge(what, 1, Integer::sum);
		if(what == 'a')
		{
			annotationsM.add(message);
			annotationsT.add(lastTime);
		}

		return pushSpecilaMessage(who, what, message);
	}

	@Override
	public boolean onMetadata(String key, String value) {
		metaData.put(key, value);


		return pushMetadata(key, value);
	}


	@Override
	public boolean onEndOfFile() {
		this.end = true;
		if(printAfter)
		{
			izpisi();
		}
		return pushEndofFile();
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		this.end = true;
		if(printAfter)
		{
			izpisi();
		}
		return pushUnmarkedEndofFile();
	}


	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		sensorDefinitions.put(handle, definition);
		generalCounter.merge("Definitions", 1, Integer::sum);

		return pushDefinition(handle, definition);
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		structDefinitions.put(handle, definition);
		generalCounter.merge("Definitions", 1, Integer::sum);

		return pushDefinition(handle, definition);
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		generalCounter.merge("Definitions", 1, Integer::sum);

		return pushDefinition(handle, definition);
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		generalCounter.merge("Timestamps", 1, Integer::sum);
		if (!start)
		{
			startTime = nanoSecondTimestamp;

			start = true;
		}
		lastTime = nanoSecondTimestamp;
		return pushTimestamp(nanoSecondTimestamp);
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {		

		if (!start)
		{
			startTime = timestamp;
			start = true;
		}

		stremCounter.merge(handle, 1, Integer::sum);
		for(char element:structDefinitions.get(handle).elementsInOrder.toCharArray())
		{
			sensorCounter.merge(element, 1, Integer::sum);
		}

		//PCARD specific statistics

		if(version.equals("PCARD"))
		{
			ArrayList<Float> convertedData = S2utilities.decodeData(structDefinitions.get(handle), sensorDefinitions, data, errPS);
			if(convertedData.size() == 15)
			{
				long curentCounter = (long)convertedData.get(14).floatValue();
				for(int i =0; i<14;i++)
				{
					//counting missing samples
					if(convertedData.get(i) == 0)
					{
						lostSamples++;
					}
				}
				if((curentCounter == lastPacketCounter + 14) || (curentCounter == lastPacketCounter + 14 - 1024))
				{
					dif.add(timestamp - lastTime);
				}else
				{
					lostPackets++;
				}
				lastPacketCounter = curentCounter;
			}else
			{
				errPS.println("This file does not meet PCARD specifications. Only "+ convertedData.size() + " samples. There should be 14+1.");
			}

		}
		lastTime = timestamp;

		return pushStreamPacket(handle, timestamp, len, data);
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		generalCounter.merge("UnknownLineType", 1, Integer::sum);

		return pushUnknownLineType(type, len, data);
	}

	@Override
	public boolean onError(int lineNum, String error) {
		generalCounter.merge("Error", 1, Integer::sum);

		return pushError(lineNum, error);
	}




	/**
	 * @return map of counters for various things like comments, errors...
	 */
	public Map<String, Integer> getGeneralCounter() {
		return generalCounter;
	}

	/**
	 * @return map of the stremCounter
	 */
	public Map<Byte, Integer> getStremCounter() {
		return stremCounter;
	}

	/**
	 * @return the counter of the selected strem
	 */
	public Integer getStremCounter(byte handle) {
		return stremCounter.get(handle);
	}


	/**
	 * @return map of the sensorCounters
	 */
	public Map<Character, Integer> getSensorCounter() {
		return sensorCounter;
	}

	/**
	 * @return counter of the selected sensor
	 */
	public int getSensorCounter(byte handle) {
		return sensorCounter.get((char)handle);
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return lastTime;
	}


	/**
	 * @return map of the metaData
	 */
	public Map<String, String> getMetaData() {
		return metaData;
	}

	/**
	 * @return map of special messages
	 */
	public Map<Character, Integer> getSpecial() {
		return special;
	}

	/**
	 * @return map of struct representations
	 */
	public Map<Byte, StructDefinition> getStructRepresentations() {
		return structDefinitions;
	}

	/**
	 * @return representation of selected struct
	 */
	public String getStructRepresentation(byte handle) {
		return structDefinitions.get(handle).elementsInOrder;
	}




	/**
	 * @return the integer version
	 */
	public int getVersionInt() {
		return versionInt;
	}

	/**
	 * @return the string version
	 */
	public String getVersion() {
		return version;
	}





}
