package callBacks;

import si.ijs.e6.S2;
import si.ijs.e6.S2.ReadLineCallbackInterface;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/*
import s2.S2;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;
*/

public class FirtstReader implements ReadLineCallbackInterface {
	//najvišja verzija, ki jo še znamo brati/pisati
	public int VERSION = 1;
	
	S2 file1;
	SecondReader bob;
	
	public int unknownStreamPacketCounter = 0;
	public int errorCounter = 0;
	
	long lastTime = 0;
	

	public FirtstReader(S2 file1, SecondReader bob) {
		this.file1 = file1;
		this.bob = bob;
	}
	
	//classes we neeed for writting into arrays
	
	public class Version
	{
		public int intVersion;
		public String version;
		
		public Version(int intVersion, String version)
		{
			this.intVersion = intVersion;
			this.version = version;
		}
	}
	
	public class TimeData
	{
		long timestamp;
		public TimeData(long timestamp)
		{
			this.timestamp = timestamp;
		}
	}
	
	public class Comment extends TimeData
	{
		String comment;
		public Comment(long timestamp, String comment) {
			super(timestamp);
			this.comment = comment;
		}
		
	}
	
	public class SpecialMessage extends TimeData
	{
		char who;
		char what;
		String message;
		public SpecialMessage(long timestamp, char who, char what, String message)
		{
			super(timestamp);
			this.who = who;
			this.what = what;
			this.message = message;
		}
	}
	
	public class TimeStamp extends TimeData
	{

		public TimeStamp(long timestamp) {
			super(timestamp);
		}
		
	}
	
	public class StreamPacket extends TimeData
	{
		byte handle;
		int len;
		byte[] data;
		
		public StreamPacket(byte handle, long timestamp, int len, byte[] data)
		{
			super(timestamp);
			this.handle = handle;
			this.len = len;
			this.data = data;
		}
	}
	
	@Override
	public boolean onComment(String comment) {
		bob.timeDataQ.add(new Comment(lastTime,comment));
		return true;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		bob.versionFirst = new Version(versionInt, version);
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		bob.timeDataQ.add(new SpecialMessage(lastTime, who, what, message));
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		bob.metadataFirstMap.put(key, value);
		return true;
	}

	@Override
	public boolean onEndOfFile() {
		if(unknownStreamPacketCounter > 0 || errorCounter > 0)
		{
			System.err.println("First S2 file contained ");
			System.err.println(unknownStreamPacketCounter + " unknownStreamPackets" );
			System.err.println(errorCounter + " errors");
		}
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		if(unknownStreamPacketCounter > 0 || errorCounter > 0)
		{
			System.err.println("First S2 file contained ");
			System.err.println(unknownStreamPacketCounter + " unknownStreamPackets" );
			System.err.println(errorCounter + " errors");
		}
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		bob.sensorDefinitionFirst.put(handle, definition);
		bob.usedHandles.add(handle);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		bob.structDefinitionFirst.put(handle, definition);
		bob.usedHandles.add(handle);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		bob.timestampDefinitionFirst.put(handle, definition);
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		bob.timeDataQ.add(new TimeStamp(nanoSecondTimestamp));
		lastTime = nanoSecondTimestamp;
		return true;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		bob.timeDataQ.add(new StreamPacket(handle,timestamp,len,data));
		lastTime = timestamp;
		return true;
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		this.unknownStreamPacketCounter++;
		return true;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		this.errorCounter++;
		return true;
	}
	
	

}
