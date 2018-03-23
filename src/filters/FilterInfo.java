package filters;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;


/**
 * Creates basic Information about S2 file and print it to PrintStream From constructor.
 * @author janez
 *
 */
public class FilterInfo extends Filter {

	boolean start = false;
	boolean end = false;
	boolean printAfter = false;
	PrintStream out;

	private Map<String, Integer> generalCounter = new HashMap<String, Integer>();
	private Map<Byte, Integer> stremCounter = new HashMap<Byte, Integer>();
	private Map<Character, Integer> sensorCounter = new HashMap<Character, Integer>();
	long startTime;
	long endTime;

	//save meta for later output
	Map<String,String> metaData = new HashMap<String, String>();
	//special message counter based on type
	Map<Character,Integer> special = new HashMap<Character,Integer>();
	//save representation of structdefinition
	Map<Byte,String> structDefinitions = new HashMap<Byte, String>();

	//version
	int versionInt;
	String version;

	public FilterInfo(PrintStream out)
	{
		this(out, true);
	}
	
	public FilterInfo(PrintStream out, boolean printAfter)
	{
		this.out = out;
		this.printAfter = printAfter;
	}


	/**
	 * writes info
	 * @param out PrintStrem on which all info will be written
	 */
	public void izpisi(PrintStream out)
	{
		this.out = out;
		izpisi();
	}



	/**
	 * writes info
	 * If Output PrintStream is null it will use System.out
	 * if called beffore S2 has been read it wont print enything.
	 */
	public void izpisi()
	{
		if(out == null)
		{
			out = System.out;
		}
		if(!end)
		{
			return;
		}
		out.println(versionInt + " " + version);
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

		out.println("Special messeges : ");
		for(char key:special.keySet())
		{
			out.println("	" +  key + " : " + special.get(key));
		}

		for(String key:generalCounter.keySet())
		{
			out.println(key +" : "+ generalCounter.get(key));
		}


		out.println("Number of streams : " + stremCounter.size());

		//stream represantation
		out.println("Stream represantation: ");
		for(byte key:structDefinitions.keySet())
		{
			out.println("	stream " + key +" : " + structDefinitions.get(key));
		}

		//packets
		out.println("Packets per stream: ");
		for(byte key:stremCounter.keySet())
		{
			out.println("	stream " + key + " : " + stremCounter.get(key));
		}
		//samples
		out.println("Data per sensor type: ");
		for(char key:sensorCounter.keySet())
		{
			out.println("	sensor " + (char)key + " : " + sensorCounter.get(key));
		}

		if(generalCounter.containsKey("Unknown Lines"))
			out.println("Unknown Lines : "  + generalCounter.get("Unknown Lines"));
		if(generalCounter.containsKey("Errors"))
			out.println("Errors : "  + generalCounter.get("Errors"));

	}

	@Override
	public boolean onComment(String comment) {
		generalCounter.merge("comment", 1, Integer::sum);

		pushComment(comment);
		return true;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		this.versionInt = versionInt;
		this.version = version;
		pushVersion(versionInt, version);
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		special.merge(what, 1, Integer::sum);

		pushSpecilaMessage(who, what, message);
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		metaData.put(key, value);

		pushMetadata(key, value);
		return true;
	}
	
	
	@Override
	public boolean onEndOfFile() {
		this.end = true;
		if(printAfter)
			izpisi();
		
		pushEndofFile();
		return false;
	}
	
	@Override
	public boolean onUnmarkedEndOfFile() {
		this.end = true;
		if(printAfter)
			izpisi();
		
		pushUnmarkedEndofFile();
		return false;
	}
	

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		generalCounter.merge("Definitions", 1, Integer::sum);

		pushDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		structDefinitions.put(handle, definition.elementsInOrder);
		generalCounter.merge("Definitions", 1, Integer::sum);

		pushDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		generalCounter.merge("Definitions", 1, Integer::sum);

		pushDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		generalCounter.merge("Timestamps", 1, Integer::sum);
		if (!start)
		{
			startTime = nanoSecondTimestamp;
			endTime = nanoSecondTimestamp;
			start = true;
		}
		else{
			endTime = nanoSecondTimestamp;
		}

		pushTimestamp(nanoSecondTimestamp);
		return true;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		if (!start)
		{
			startTime = timestamp;
			endTime = timestamp;
			start = true;
		}
		else{
			endTime = timestamp;
		}

		stremCounter.merge(handle, 1, Integer::sum);
		for(char element:structDefinitions.get(handle).toCharArray())
		{
			sensorCounter.merge(element, 1, Integer::sum);
		}

		pushStremPacket(handle, timestamp, len, data);
		return true;
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		generalCounter.merge("UnknownLineType", 1, Integer::sum);

		pushUnknownLineType(type, len, data);
		return true;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		generalCounter.merge("Error", 1, Integer::sum);

		pushError(lineNum, error);
		return true;
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
		return endTime;
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
	public Map<Byte, String> getStructRepresentations() {
		return structDefinitions;
	}

	/**
	 * @return representation of selected struct
	 */
	public String getStructRepresentation(byte handle) {
		return structDefinitions.get(handle);
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
