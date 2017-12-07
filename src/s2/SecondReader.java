package s2;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
	
	public Version versionFirst;
	public Queue<String> commentFirstQ = new LinkedList<>();
	public Queue<SpecialMessage> specialMessageFirstQ = new LinkedList<SpecialMessage>();
	public Map<String, String> metadataFirstMap = new HashMap<String, String>();
	public Map<Byte, SensorDefinition> sensorDefinitionFirst = new HashMap<Byte, SensorDefinition>();
	public Map<Byte, StructDefinition> structDefinitionFirst = new HashMap<Byte, StructDefinition>(); 
	public Map<Byte, TimestampDefinition> timestampDefinitionFirst = new HashMap<Byte, TimestampDefinition>();
	public Queue<Long> timestampFirstQ = new LinkedList<Long>();
	public Queue<StreamPacket> streamPacketFirstQ = new LinkedList<StreamPacket>();
	
	public Map<String, String> metadataSecondMap = new HashMap<String, String>();
	
	Date dateFirst;
	Date dateSecond;
	Calendar calenderFirst;
	SimpleDateFormat simpleFirst;
	
	public Set<Byte> usedHandlesFirst = new HashSet<Byte>();
	
	public long lastTime = 0;
	
	public SecondReader(S2 file2, String outDir, String outName) 
	{
		this.inFile = file2;
		this.outFile = new S2();
		this.storeS = this.outFile.store(new File(outDir), outName);
	}
	
	public void onFirstReaderEnd()
	{
		
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
		//for(String key : metadataFirstMap.keySet())
		//	storeS.addMetadata(key, metadataFirstMap.get(key));
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
		return true;
	}

	@Override
	public boolean onEndOfFile() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		// TODO Auto-generated method stub
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
		return false;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		
		
		/*
		while(!timestampFirstQ.isEmpty() && (timestampFirstQ.peek() < nanoSecondTimestamp))
		{
			lastTime = timestampFirstQ.poll();
			storeS.addTimestamp(new Nanoseconds(lastTime));
			
			while(!streamPacketFirstQ.isEmpty() && (streamPacketFirstQ.peek().timestamp < nanoSecondTimestamp
					&& streamPacketFirstQ.peek().timestamp < timestampFirstQ.peek())      )
			{
				StreamPacket temp = streamPacketFirstQ.poll();
				long writeReadyTime = (long) ((temp.timestamp - lastTime) * timestampDefinitionFirst.get(temp.handle).multiplier);
				storeS.addSensorPacket(temp.handle, writeReadyTime, temp.data);
			}
			
			
			
		}*/
		
		while(!streamPacketFirstQ.isEmpty() && streamPacketFirstQ.peek().timestamp < nanoSecondTimestamp)
		{
			StreamPacket temp = streamPacketFirstQ.poll();
			long maxBits = timestampDefinitionFirst.get(temp.handle).byteSize*8;
			long writeReadyTime;
			long formated = (long) (timestampDefinitionFirst.get(temp.handle).toImplementationFormat(
					new Nanoseconds(temp.timestamp - lastTime)) / timestampDefinitionFirst.get(temp.handle).multiplier);
			if(64 - Long.numberOfLeadingZeros(formated) <= maxBits){
				writeReadyTime = formated;}
			else
			{
				lastTime = temp.timestamp;
				storeS.addTimestamp(new Nanoseconds(lastTime));
				writeReadyTime = (long) 0;
			}
				
			
			
			storeS.addSensorPacket(temp.handle, writeReadyTime, temp.data);
		}
		
		
		
		lastTime = nanoSecondTimestamp;
		storeS.addTimestamp(new Nanoseconds(nanoSecondTimestamp));
		return false;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		// TODO Auto-generated method stub
		return false;
	}

}
