package s2;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
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
	
	public Set<Byte> usedHandles = new HashSet<Byte>();
	public Map<Byte,Byte> HandlesSecondConverter = new HashMap<Byte,Byte>();
	
	//time
	public long[] newLastTime = new long[32];
	public long newLastTimestamp = 0;
	boolean TimeStampWriten = true;
	
	public SecondReader(S2 file2, String outDir, String outName) 
	{
		this.inFile = file2;
		this.outFile = new S2();
		this.storeS = this.outFile.store(new File(outDir), outName);
		
		specialMeta.add("date");
		specialMeta.add("time");
		specialMeta.add("timezone");
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
		Long diff = (long) (difference.getSeconds()*Math.pow(10,9) + difference.getNano());
		if(!difference.isNegative())
		{
			storeS.addMetadata("date", dateM1);
			storeS.addMetadata("time", timeM1);
			storeS.addMetadata("timezone", zoneM1);
			nanoOffStrim1 = (long)0;
			nanoOffStrim2 = diff;
		}
		else
		{
			storeS.addMetadata("date", dateM2);
			storeS.addMetadata("time", timeM2);
			storeS.addMetadata("timezone", zoneM2);
			nanoOffStrim1 = diff;
			nanoOffStrim2 = (long)0;
		}
		System.out.println(diff);
		weHaveTime = true;
		
	}

	public byte convertHandle(byte handle,byte reqDepth)
	{
		if(usedHandles.contains(handle))
		{
			if(0<=handle && handle < 32)
			{
				if(reqDepth >= 32)
				{
					System.err.println("Not enought handles. Information from handle" + handle + "has been overwritten");
					return handle;
				}
				return convertHandle((byte)((handle+1)%32),(byte)(reqDepth+1));
			}
			else
			{
				byte abc = Byte.MAX_VALUE + 1 - 32;
				if(reqDepth>= abc)
				{
					System.err.println("Not enought handles. Information from handle" + handle + "has been overwritten");
					return handle;
				}
				return convertHandle((byte)((handle+1-32)%abc+32),(byte)(reqDepth+1));
			}
		}
		else
			return handle;
		
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
		
		storeS.endFile(true);
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
		storeS.endFile(true);
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		//TODO teh morda ni treba konvertat
		byte newHandle = HandlesSecondConverter.get(handle);
		storeS.addDefinition(newHandle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		byte newHandle;
		if(HandlesSecondConverter.containsKey(handle))
		{
			newHandle = HandlesSecondConverter.get(handle);
		}
		else
		{
			newHandle = convertHandle(handle,(byte)0);
			usedHandles.add(newHandle);
			HandlesSecondConverter.put(handle, newHandle);
		}
		
		corectDefinition(definition);
		
		storeS.addDefinition(newHandle, definition);
		return true;
	}
	
	private void corectDefinition(StructDefinition old)
	{
		int le = old.elementsInOrder.length();
		char[] elements = old.elementsInOrder.toCharArray();
		for(int i =0;i<le;i++)
		{
			byte temp = convertHandle((byte) elements[i], (byte)0);
			usedHandles.add(temp);
			elements[i] = (char) temp;
		}
		old.elementsInOrder = elements.toString();
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		byte newHandle;
		if(HandlesSecondConverter.containsKey(handle))
		{
			newHandle = HandlesSecondConverter.get(handle);
		}
		else
		{
			newHandle = convertHandle(handle,(byte)0);
			usedHandles.add(newHandle);
			HandlesSecondConverter.put(handle, newHandle);
		}
		storeS.addDefinition(newHandle, definition);
		timestampDefinitionSecond.put(newHandle, definition);
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		//TODO dokoncaj spodnje v skladu z OUTS2 zapisovanjem časa
		addOldPackets(nanoSecondTimestamp);
		
		newLastTimestamp = nanoSecondTimestamp+nanoOffStrim2;
		Arrays.fill(newLastTime, newLastTimestamp);
		storeS.addTimestamp(new Nanoseconds(newLastTimestamp));
		return true;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		//TODO popravi cas skladno z OutS2
		if(!weHaveTime)
		{
			System.err.println("First StreamPacket before metadata with date and time.");
			return false;
		}
		//if we have packets from first file before curent time we safe them first.
		addOldPackets(timestamp);
		
		//for time releate ting we use new handle
		byte newHandle = HandlesSecondConverter.get(handle);
		
		//int maxBits = timestampDefinitionFirst.get(handle).byteSize*8;
		long newDiff = timestamp+nanoOffStrim2 - newLastTime[newHandle];
		TimestampDefinition td = timestampDefinitionSecond.get(newHandle);
		long writeReadyDiff = td.toImplementationFormat(
				new Nanoseconds(newDiff));
		
		storeS.addSensorPacket(newHandle, writeReadyDiff, data);
		newLastTime[newHandle] += writeReadyDiff * td.getNanoMultiplier();
		//newLastTime[newHandle] = timestamp + nanoOffStrim2;
		
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
	
	private void addOldPackets(long timestamp)
	{
		while(!streamPacketFirstQ.isEmpty() && 
				(streamPacketFirstQ.peek().timestamp+nanoOffStrim1 < timestamp+nanoOffStrim2))
		{
			StreamPacket temp = streamPacketFirstQ.poll();
			
			while(!timestampFirstQ.isEmpty() && (timestampFirstQ.peek() <= temp.timestamp))
			{
				long tempTime = timestampFirstQ.poll() + nanoOffStrim1;
				if(tempTime > newLastTimestamp)
				{
					newLastTimestamp = tempTime;
					Arrays.fill(newLastTime, newLastTimestamp);
					storeS.addTimestamp(new Nanoseconds(newLastTimestamp));
				}
			}
			
			//int maxBits = timestampDefinitionFirst.get(temp.handle).byteSize*8;
			long newDiff = temp.timestamp+nanoOffStrim1 - newLastTime[temp.handle];

			long writeReadyDiff = timestampDefinitionFirst.get(temp.handle).toImplementationFormat(new Nanoseconds(newDiff));
			
			storeS.addSensorPacket(temp.handle, writeReadyDiff, temp.data);
			
			newLastTime[temp.handle] += writeReadyDiff* timestampDefinitionFirst.get(temp.handle).getNanoMultiplier();
			//newLastTime[temp.handle] = temp.timestamp+nanoOffStrim1;
			
			int waitt = 0;
		}
	}

}
