package s2;

import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;

public class FirtstReader implements ReadLineCallbackInterface {
	//najvišja verzija, ki jo še znamo brati/pisati
	public int VERSION = 1;
	
	S2 file1;
	SecondReader bob;
	
	public int unknownStreamPacketCounter = 0;
	public int errorCounter = 0;
	

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
	
	public class SpecialMessage
	{
		char who;
		char what;
		String message;
		public SpecialMessage(char who, char what, String message)
		{
			this.who = who;
			this.what = what;
			this.message = message;
		}
	}
	
	public class StreamPacket
	{
		byte handle;
		long timestamp;
		int len;
		byte[] data;
		
		public StreamPacket(byte handle, long timestamp, int len, byte[] data)
		{
			this.handle = handle;
			this.timestamp = timestamp;
			this.len = len;
			this.data = data;
		}
	}
	
	@Override
	public boolean onComment(String comment) {
		bob.commentFirstQ.add(comment);
		return true;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		bob.versionFirst = new Version(versionInt, version);
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		bob.specialMessageFirstQ.add(new SpecialMessage(who, what, message));
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
		bob.timestampFirstQ.add(nanoSecondTimestamp);
		return true;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		bob.streamPacketFirstQ.add(new StreamPacket(handle,timestamp,len,data));
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
