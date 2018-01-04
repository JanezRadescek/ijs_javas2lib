package callBacks;

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

import callBacks.FirtstReader.Comment;
import callBacks.FirtstReader.SpecialMessage;
import callBacks.FirtstReader.StreamPacket;
import callBacks.FirtstReader.TimeData;
import callBacks.FirtstReader.TimeStamp;
import callBacks.FirtstReader.Version;
import s2.S2;
import s2.S2.MessageType;
import s2.S2.Nanoseconds;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StoreStatus;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;


public class SecondReader implements ReadLineCallbackInterface {
	//najvišja verzija, ki jo še znamo brati/pisati
	public final int VERSION = 1;
	public final String VERSIONLONG = "PCARD";
	
	S2 inFile;
	S2 outFile;
	StoreStatus storeS;
	boolean mergeHandles;
	
	//First S2 file datas
	public Version versionFirst;
	public Map<String, String> metadataFirstMap = new HashMap<String, String>();
	public Map<Byte, SensorDefinition> sensorDefinitionFirst = new HashMap<Byte, SensorDefinition>();
	public Map<Byte, StructDefinition> structDefinitionFirst = new HashMap<Byte, StructDefinition>(); 
	public Map<Byte, TimestampDefinition> timestampDefinitionFirst = new HashMap<Byte, TimestampDefinition>();
	public Queue<TimeData> timeDataQ = new LinkedList<TimeData>();
	
	
	//Second S2 file datas
	public Map<String, String> metadataSecondMap = new HashMap<String, String>();
	public Map<Byte, TimestampDefinition> timestampDefinitionSecond = new HashMap<Byte, TimestampDefinition>();
	public Map<Byte, StructDefinition> structDefinitionSecondQ = new HashMap<Byte, StructDefinition>();
	public int unknownStreamPacketCounter = 0;
	public int errorCounter = 0;
	
	//date releated 
	HashSet<String> specialMeta = new HashSet<String>();
	boolean weHaveDate = false;
	long nanoOffStrim1;
	long nanoOffStrim2;
	
	//handles
	public Set<Byte> usedHandles = new HashSet<Byte>();
	public Map<Byte,Byte> HandlesSecondConverter = new HashMap<Byte,Byte>();
	boolean checkStructDefinitions = true;
	
	//time
	public long[] newLastTime = new long[32];
	public long newLastTimestamp = 0;
	boolean TimeStampWriten = true;
	
	public SecondReader(S2 file2, String outDir, String outName, boolean mergeHandles) 
	{
		this.inFile = file2;
		this.outFile = new S2();
		this.storeS = this.outFile.store(new File(outDir), outName);
		this.mergeHandles = mergeHandles;
		
		specialMeta.add("date");
		specialMeta.add("time");
		specialMeta.add("timezone");
		
		System.out.println("writing to file " + outDir +File.separator+ outName);
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
		System.out.println("razlika med zacetnima datuma = " + diff);
		weHaveDate = true;
		
	}

	/**
	 * If first file already contains given handle than it finds new one else return same one
	 * @param handle - convert this
	 * @return - converted handle
	 */
	public byte convertHandle(byte handle)
	{
		
		if(HandlesSecondConverter.keySet().contains(handle))
		{
			return HandlesSecondConverter.get(handle);
		}
		else
		{	
			if(0<=handle && handle < 32)
			{
				for(int i = 0;i<32;i++)
				{
					byte temp = (byte) ((handle+i)%32);
					if(!usedHandles.contains(temp))
					{
						HandlesSecondConverter.put(handle, temp);
						usedHandles.add(temp);
						return temp;
					}
				}
				
				System.err.println("Not enought handles. Information from handle" + handle + "has been overwritten");
				return handle;
			}
			else
			{
				byte abc = Byte.MAX_VALUE + 1 - 32;
				for(int i =0;i<abc+1;i++)
				{
					byte temp = (byte) ((handle+i-32)%abc + 32);
					if(!usedHandles.contains(temp))
					{
						HandlesSecondConverter.put(handle, temp);
						usedHandles.add(temp);
						return temp;
					}
				}
				
				System.err.println("Not enought handles. Information from handle" + handle + "has been overwritten");
				return handle;
			}

		}	
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
		if(!weHaveDate && metadataSecondMap.keySet().containsAll(specialMeta))
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
		if(mergeHandles)
		{	
			for(byte i:sensorDefinitionFirst.keySet())
			{
				sensorDefinitionFirst.get(i);
				if(definition.equalValues(sensorDefinitionFirst.get(i)))
				{
					HandlesSecondConverter.put(handle, i);
					break;
				}
			}
		}
		byte newHandle = convertHandle(handle);
		if(!mergeHandles)
		{
			storeS.addDefinition(newHandle, definition);
		}
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		if(mergeHandles)
		{
			structDefinitionSecondQ.put(handle, definition);
			//TODO premakni na prvi podatkovni paket
			
		}else
		{
			byte newHandle = convertHandle(handle);
			corectDefinition(definition);
			storeS.addDefinition(newHandle, definition);
		}
		return true;
	}
	
	/**
	 * Takes elementsInOrder from definition and replace them with new one.
	 * @param definition
	 */
	private void corectDefinition(StructDefinition definition)
	{
		int le = definition.elementsInOrder.length();
		char[] elements = definition.elementsInOrder.toCharArray();
		for(int i =0;i<le;i++)
		{
			byte temp = convertHandle((byte) elements[i]);
			elements[i] = (char) temp;
		}
		definition.elementsInOrder = new String(elements);
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		if(mergeHandles)
		{
			if(timestampDefinitionFirst.containsKey(handle))
			{
				if(!definition.equalValues(timestampDefinitionFirst.get(handle)))
				{
					System.err.println("input files have diferent TimestampDefinition on handle " + handle 
							+ ". Program terminated, output file corupted");
					return false;
				}
				else
				{
					HandlesSecondConverter.put(handle, handle);
				}
			}else
			{
				storeS.addDefinition(handle, definition);
			}
		}
		byte newHandle = convertHandle(handle);
		if(!mergeHandles)
		{
			storeS.addDefinition(newHandle, definition);
		}
		timestampDefinitionSecond.put(newHandle, definition);
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		addOldTimeData(nanoSecondTimestamp);
		
		newLastTimestamp = nanoSecondTimestamp+nanoOffStrim2;
		Arrays.fill(newLastTime, newLastTimestamp);
		storeS.addTimestamp(new Nanoseconds(newLastTimestamp));
		return true;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		if(!weHaveDate)
		{
			System.err.println("First StreamPacket before metadata with date and time.");
			return false;
		}
		if(checkStructDefinitions)
		{
			//TODO neki ne dela popravi
			checkStructDefinitions = false;
			
			for(byte key:structDefinitionSecondQ.keySet())
			{
				StructDefinition tempSD = structDefinitionSecondQ.get(key);
				corectDefinition(tempSD);
				if(structDefinitionFirst.containsKey(key))
				{
					if(!tempSD.equalValues(structDefinitionFirst.get(key)))
					{
						System.err.println("input files have diferent StructDefinition on handle " + key 
								+ ". Program terminated, output file corupted");
						System.out.println(tempSD.elementsInOrder + " " + tempSD.name);
						System.out.println(structDefinitionFirst.get(key).elementsInOrder + " " + structDefinitionFirst.get(key).name);
						return false;
					}else
					{
						HandlesSecondConverter.put(key, key);
					}
				}else
				{
					storeS.addDefinition(key, tempSD);
				}
			}
			
		}
		
		//if we have packets from first file before curent time we safe them first.
		addOldTimeData(timestamp);
		
		//for everything we use new handle
		byte newHandle = HandlesSecondConverter.get(handle);
		
		
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
	
	private void addOldTimeData(long timestamp)
	{
		while(!timeDataQ.isEmpty() && 
				(timeDataQ.peek().timestamp+nanoOffStrim1 <= timestamp+nanoOffStrim2))
		{
			TimeData temp = timeDataQ.poll();
			
			if(temp instanceof Comment)
			{
				Comment tempC = (Comment) temp;
				storeS.addTextMessage(tempC.comment);
			}
			else if(temp instanceof SpecialMessage)
			{
				SpecialMessage tempMSG = (SpecialMessage) temp;
				storeS.addSpecialTextMessage((byte)tempMSG.who,MessageType.convert((byte)tempMSG.what),tempMSG.message,-1);
			}
			else if(temp instanceof TimeStamp)
			{
				long tempTime = temp.timestamp + nanoOffStrim1;
				if(tempTime > newLastTimestamp)
				{
					newLastTimestamp = tempTime;
					Arrays.fill(newLastTime, newLastTimestamp);
					storeS.addTimestamp(new Nanoseconds(newLastTimestamp));
				}
			}
			else if(temp instanceof StreamPacket)
			{
				StreamPacket tempPacket = (StreamPacket) temp;
				//int maxBits = timestampDefinitionFirst.get(temp.handle).byteSize*8;
				long newDiff = tempPacket.timestamp+nanoOffStrim1 - newLastTime[tempPacket.handle];

				long writeReadyDiff = timestampDefinitionFirst.get(tempPacket.handle).toImplementationFormat(new Nanoseconds(newDiff));
				
				storeS.addSensorPacket(tempPacket.handle, writeReadyDiff, tempPacket.data);
				
				newLastTime[tempPacket.handle] += writeReadyDiff* timestampDefinitionFirst.get(tempPacket.handle).getNanoMultiplier();
				//newLastTime[temp.handle] = temp.timestamp+nanoOffStrim1;
			}
			else
			{
				System.err.println("First file saved something wrong in timeDataQ");
			}
			
		}
	}

}
