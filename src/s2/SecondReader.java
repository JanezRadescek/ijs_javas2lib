package s2;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
	
	//public long lastTime = 0;
	public long newLastTime = 0;
	
	public SecondReader(S2 file2, String outDir, String outName) 
	{
		this.inFile = file2;
		this.outFile = new S2();
		this.storeS = this.outFile.store(new File(outDir), outName);
		
		specialMeta.add("date");
		specialMeta.add("time");
		specialMeta.add("timezone");
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
		String zoneM1 = metadataFirstMap.get("timezone");
		String dateM2 = metadataSecondMap.get("date");
		String timeM2 = metadataSecondMap.get("time");
		String zoneM2 = metadataSecondMap.get("timezone");
		
		ZonedDateTime date1;
		ZonedDateTime date2;
		try
		{
			date1 = ZonedDateTime.parse(dateM1+"T"+timeM1+zoneM1);
			date2 = ZonedDateTime.parse(dateM2+"T"+timeM2+zoneM2);
		}
		catch(java.time.format.DateTimeParseException e)
		{
			zoneM1 = zoneM1.substring(0, 3) + ":" + zoneM1.substring(3, zoneM1.length());
			zoneM2 = zoneM2.substring(0, 3) + ":" + zoneM2.substring(3, zoneM2.length());
			date1 = ZonedDateTime.parse(dateM1+"T"+timeM1+zoneM1);
			date2 = ZonedDateTime.parse(dateM2+"T"+timeM2+zoneM2);
		}
		/*
		finally
		{
			if(date1 == null || date2 == null)
			{
				System.err.println("Cant read date/time/zone : " + dateM1 + " " + timeM1 + " " + zoneM1);
				System.err.println("Cant read date/time/zone : " + dateM2 + " " + timeM2 + " " + zoneM2);
			}
		}*/
		
		Duration difference = Duration.between(date1, date2);
		//če ni negatina je prvi datum prvi
		if(!difference.isNegative())
		{
			storeS.addMetadata("date", dateM1);
			storeS.addMetadata("time", timeM1);
			storeS.addMetadata("timezone", zoneM1);
			nanoOffStrim1 = (long)0;
			nanoOffStrim2 = (long) (difference.getSeconds()*Math.pow(10,9) + difference.getNano());
		}
		else
		{
			storeS.addMetadata("date", dateM2);
			storeS.addMetadata("time", timeM2);
			storeS.addMetadata("timezone", zoneM2);
			nanoOffStrim1 = (long) (difference.getSeconds()*Math.pow(10,9) + difference.getNano());
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
		if(unknownStreamPacketCounter > 0 || errorCounter > 0)
		{
			System.err.println("Second S2 file contained ");
			System.err.println(unknownStreamPacketCounter + " unknownStreamPackets" );
			System.err.println(errorCounter + " errors");
		}
		
		storeS.endFile();
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		if(unknownStreamPacketCounter > 0 || errorCounter > 0)
		{
			System.err.println("Second S2 file contained ");
			System.err.println(unknownStreamPacketCounter + " unknownStreamPackets" );
			System.err.println(errorCounter + " errors");
		}
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
		//TODO dodeli razlicne handle novim
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
		/*
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
		
		*/
		return true;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		//TODO popravi prevec timestampov
		
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
			int maxBits = timestampDefinitionFirst.get(temp.handle).byteSize*8;
			long writeReadyTime;
			long newDiff = temp.timestamp+nanoOffStrim1 - newLastTime;
			//long formatted = (long) (timestampDefinitionFirst.get(temp.handle).toImplementationFormat(
			//		new Nanoseconds(temp.timestamp+nanoOffStrim1 - newAbsoluteTime)) );
					// / timestampDefinitionFirst.get(temp.handle).multiplier);
			int newDiffBits = 64 - Long.numberOfLeadingZeros(newDiff);
			if(newDiffBits > maxBits)
			{
				storeS.addTimestamp(new Nanoseconds(temp.timestamp+nanoOffStrim1));
				newDiff = (long) 0;
			}
			
			writeReadyTime = timestampDefinitionFirst.get(temp.handle).toImplementationFormat(
					new Nanoseconds(newDiff));
			storeS.addSensorPacket(temp.handle, writeReadyTime, temp.data);
			newLastTime = temp.timestamp+nanoOffStrim1;
		}
		
		//TODO popravi racunaje casa in kdaj je treba nov timestamp;
		int maxBits = timestampDefinitionFirst.get(handle).byteSize*8;
		long writeReadyTime;
		long newDiff = timestamp+nanoOffStrim2 - newLastTime;
		//long formatted = (long) (timestampDefinitionSecond.get(handle).toImplementationFormat(
		//		new Nanoseconds(timestamp+nanoOffStrim2 - newAbsoluteTime)) / timestampDefinitionSecond.get(handle).multiplier);
		int newDeffBits = 64 - Long.numberOfLeadingZeros(newDiff);
		if(newDeffBits > maxBits)
		{
			storeS.addTimestamp(new Nanoseconds(timestamp+nanoOffStrim2));
			newDiff = (long) 0;
		}
		
		writeReadyTime = timestampDefinitionFirst.get(handle).toImplementationFormat(
				new Nanoseconds(newDiff));
		storeS.addSensorPacket(handle, writeReadyTime, data);
		newLastTime = timestamp + nanoOffStrim2;
		
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
