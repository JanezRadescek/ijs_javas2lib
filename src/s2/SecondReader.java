package s2;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;

import s2.FirtstReader.SpecialMessage;
import s2.FirtstReader.StreamPacket;
import s2.FirtstReader.Version;
import s2.S2.MessageType;
import s2.S2.Nanoseconds;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;

public class SecondReader implements ReadLineCallbackInterface {
	//najvišja verzija, ki jo še znamo brati/pisati
	public final int VERSION = 1;
	public final String VERSIONLONG = "PCARD";
	
	S2 inFile;
	S2 outFile;
	S2.StoreStatus storeS;
	
	//First S2 file datas
	public Version versionFirst;
	public Queue<String> commentFirstQ = new LinkedList<>();
	public Queue<SpecialMessage> specialMessageFirstQ = new LinkedList<SpecialMessage>();
	public Map<String, String> metadataFirstMap = new HashMap<String, String>();
	public Map<Byte, SensorDefinition> sensorDefinitionFirst = new HashMap<Byte, SensorDefinition>();
	public Map<Byte, StructDefinition> structDefinitionFirst = new HashMap<Byte, StructDefinition>(); 
	public Map<Byte, TimestampDefinition> timestampDefinitionFirst = new HashMap<Byte, TimestampDefinition>();
	public Queue<Long> timestampFirstQ = new LinkedList<Long>();
	public Queue<StreamPacket> streamPacketFirstQ = new LinkedList<StreamPacket>();
	
	//Second S2 file datas
	public Map<String, String> metadataSecondMap = new HashMap<String, String>();
	public Map<Byte, TimestampDefinition> timestampDefinitionSecond = new HashMap<Byte, TimestampDefinition>();
	public int unknownStreamPacketCounter = 0;
	public int errorCounter = 0;
	
	//date releated 
	HashSet<String> specialMeta = new HashSet<String>();
	boolean weHaveTime = false;
	long nanoOffStrim1;
	long nanoOffStrim2;
	
	public Set<Byte> usedHandlesFirst = new HashSet<Byte>();
	
	public long lastTime = 0;
	public long lastWrittenTime = 0;
	
	public SecondReader(S2 file2, String outDir, String outName) 
	{
		this.inFile = file2;
		this.outFile = new S2();
		this.storeS = this.outFile.store(new File(outDir), outName);
		
		specialMeta.add("date");
		specialMeta.add("time");
	}
	
	public void onFirstReaderEnd()
	{
		
	}
	
	/**
	 * Called when we have read metadata with time and date.
	 * calculate time diference betwen files and set first date and time for new S2
	 */
	public void parseSpecMeta()
	{
		String dateM1 = metadataFirstMap.get("date");
		String timeM1 = metadataFirstMap.get("time");
		String dateM2 = metadataSecondMap.get("date");
		String timeM2 = metadataSecondMap.get("time");
		
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date1 = null;
		Date date2 = null;
		try {
			date1 = format.parse(dateM1+" "+timeM1);
			date2 = format.parse(dateM2+" "+timeM2);
		} catch (ParseException e) {
			System.out.println(dateM1);
			System.out.println(timeM1);
			System.out.println(dateM2);
			System.out.println(timeM2);
			System.err.println("We coudnt read date/time");
			e.printStackTrace();
		}	
		long razlika = date2.getTime() - date1.getTime(); 
		//če je večja je drugi kasneje
		if(razlika > 0)
		{
			storeS.addMetadata("date", dateM1);
			storeS.addMetadata("time", timeM1);
			nanoOffStrim1 = (long)0;
			nanoOffStrim2 = razlika*1000000;
		}
		else
		{
			storeS.addMetadata("date", dateM2);
			storeS.addMetadata("time", timeM2);
			nanoOffStrim1 = razlika*1000000;
			nanoOffStrim2 = (long)0;
		}
		weHaveTime = true;
		
	}

	@Override
	public boolean onComment(String comment) {
		storeS.addTextMessage(comment);
		return true;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		//handling higer/different versions
		if(versionInt > VERSION || versionFirst.intVersion > VERSION
				|| !version.equals(VERSIONLONG) || !versionFirst.version.equals(VERSIONLONG))
		{
			System.err.println("One of the read S2 file is not version " 
					+ VERSION + " " + versionFirst.version);
			return false;
		}
		else
			storeS.setVersion(versionInt, version);
		
		//storing timeindepend data of first file
		for(String c : commentFirstQ)
			storeS.addTextMessage(c);
		for(SpecialMessage sm : specialMessageFirstQ)
			storeS.addSpecialTextMessage((byte)sm.who,MessageType.convert((byte)sm.what),sm.message,-1);
		for(String key : metadataFirstMap.keySet())
		{
			if(!specialMeta.contains(key))
				storeS.addMetadata(key+" 1", metadataFirstMap.get(key));
		}
			
		for(Byte handle : sensorDefinitionFirst.keySet())
			storeS.addDefinition(handle, sensorDefinitionFirst.get(handle));
		for(Byte handle:structDefinitionFirst.keySet())
			storeS.addDefinition(handle, structDefinitionFirst.get(handle));
		for(Byte handle:timestampDefinitionFirst.keySet())
			storeS.addDefinition(handle, timestampDefinitionFirst.get(handle));
		
		
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		storeS.addSpecialTextMessage((byte)who,MessageType.convert((byte)what),message,-1);
		
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		metadataSecondMap.put(key, value);
		if(!specialMeta.contains(key))
			storeS.addMetadata(key+" 2", value);
		//date and time can only be one
		if(!weHaveTime && metadataSecondMap.keySet().containsAll(specialMeta))
			parseSpecMeta();
		return true;
	}

	@Override
	public boolean onEndOfFile() {
		System.err.println("Second S2 file contained " + unknownStreamPacketCounter + " unknownStreamPackets" );
		System.err.println("Second S2 file contained " + errorCounter + " errors");
		storeS.endFile();
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		System.err.println("Second S2 file contained " + unknownStreamPacketCounter + " unknownStreamPackets" );
		System.err.println("Second S2 file contained " + errorCounter + " errors");
		storeS.endFile();
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		storeS.addDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		storeS.addDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		storeS.addDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		//TODO delete all this. we will create new timestamps when needed.
		if(!weHaveTime)
		{
			System.err.println("First TimeStamp before metadata with date and time.");
			return false;
		}
		//if there are Packets
		while(!streamPacketFirstQ.isEmpty() && 
				(streamPacketFirstQ.peek().timestamp+nanoOffStrim1 < nanoSecondTimestamp+nanoOffStrim2))
		{
			StreamPacket temp = streamPacketFirstQ.poll();
			long maxBits = timestampDefinitionFirst.get(temp.handle).byteSize*8;
			long writeReadyTime;
			long formated = (long) (timestampDefinitionFirst.get(temp.handle).toImplementationFormat(
					new Nanoseconds(temp.timestamp+nanoOffStrim1 - lastWrittenTime)) / timestampDefinitionFirst.get(temp.handle).multiplier);
			if(64 - Long.numberOfLeadingZeros(formated) <= maxBits){
				writeReadyTime = formated;}
			else
			{
				lastTime = temp.timestamp+nanoOffStrim1;
				lastWrittenTime = lastTime;
				storeS.addTimestamp(new Nanoseconds(lastTime));
				writeReadyTime = (long) 0;
			}
			storeS.addSensorPacket(temp.handle, writeReadyTime, temp.data);
		}
		
		
		
		lastTime = nanoSecondTimestamp+nanoOffStrim2;
		lastWrittenTime = lastTime;
		storeS.addTimestamp(new Nanoseconds(lastTime));
		return true;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		//TODO check if you need boat lastTime and lastWriten Time hint:NO!!
		if(!weHaveTime)
		{
			System.err.println("First StreamPacket before metadata with date and time.");
			return false;
		}
		//if we have packets from first file before curent time we safe them first.
		while(!streamPacketFirstQ.isEmpty() && 
				(streamPacketFirstQ.peek().timestamp+nanoOffStrim1 < timestamp+nanoOffStrim2))
		{
			StreamPacket temp = streamPacketFirstQ.poll();
			long maxBits = timestampDefinitionFirst.get(temp.handle).byteSize*8;
			long writeReadyTime;
			long formatted = (long) (timestampDefinitionFirst.get(temp.handle).toImplementationFormat(
					new Nanoseconds(temp.timestamp+nanoOffStrim1 - lastWrittenTime)) / timestampDefinitionFirst.get(temp.handle).multiplier);
			if(64 - Long.numberOfLeadingZeros(formatted) <= maxBits){
				writeReadyTime = formatted;}
			else
			{
				lastTime = temp.timestamp+nanoOffStrim1;
				lastWrittenTime = lastTime;
				storeS.addTimestamp(new Nanoseconds(lastTime));
				writeReadyTime = (long) 0;
			}
				
			storeS.addSensorPacket(temp.handle, writeReadyTime, temp.data);
		}
		
		lastTime = timestamp;
		
		long formatted = (long) (timestampDefinitionSecond.get(handle).toImplementationFormat(
				new Nanoseconds(timestamp+nanoOffStrim1 - lastWrittenTime)) / timestampDefinitionSecond.get(handle).multiplier);
		
		storeS.addSensorPacket(handle, formatted, data);
		
		return true;
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		unknownStreamPacketCounter++;
		return true;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		errorCounter++;
		return true;
	}

}
