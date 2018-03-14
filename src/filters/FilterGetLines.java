package filters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

public class FilterGetLines extends Filter {
	
	private Version version;
	private Map<String, String> metadata = new HashMap<String, String>();
	private Map<Byte, SensorDefinition> sensorDefinitions = new HashMap<Byte, SensorDefinition>();
	private Map<Byte, StructDefinition> structDefinitions = new HashMap<Byte, StructDefinition>(); 
	private Map<Byte, TimestampDefinition> timestampDefinitions = new HashMap<Byte, TimestampDefinition>();
	private Queue<TimeData> timeDataQ = new LinkedList<TimeData>();
	private Queue<StreamPacket> packetQ = new LinkedList<StreamPacket>();
	
	long lastExplicitTimestamp = 0;
	
	/**
	 * @return the version
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * @return the metadata
	 */
	public Map<String, String> getMetadata() {
		return metadata;
	}

	/**
	 * @return the sensorDefinitions
	 */
	public Map<Byte, SensorDefinition> getSensorDefinitions() {
		return sensorDefinitions;
	}

	/**
	 * @return the structDefinitions
	 */
	public Map<Byte, StructDefinition> getStructDefinitions() {
		return structDefinitions;
	}

	/**
	 * @return the timestampDefinitions
	 */
	public Map<Byte, TimestampDefinition> getTimestampDefinitions() {
		return timestampDefinitions;
	}

	/**
	 * In timeDataQ are stored comments, special Messages and timestamps
	 * (comments and special messages are given last read timestamp)
	 * @return the timeDataQ
	 */
	public Queue<TimeData> getTimeDataQ() {
		return timeDataQ;
	}

	/**
	 * @return the packetQ
	 */
	public Queue<StreamPacket> getPacketQ() {
		return packetQ;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		this.version = new Version(versionInt, version);

		pushVersion(versionInt, version);
		return true;
	}
	
	@Override
	public boolean onComment(String comment) {
		timeDataQ.add(new Comment(lastExplicitTimestamp, comment));

		pushComment(comment);
		return true;
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		timeDataQ.add(new SpecialMessage(lastExplicitTimestamp, who, what, message));
		
		pushSpecilaMessage(who, what, message);
		return true;
	}
	
	@Override
	public boolean onMetadata(String key, String value) {
		metadata.put(key, value);
		
		pushMetadata(key, value);
		return true;
	}
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		sensorDefinitions.put(handle, definition);
		
		pushDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		structDefinitions.put(handle, definition);
		
		pushDefinition(handle, definition);
		return true;
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		timestampDefinitions.put(handle, definition);
		
		pushDefinition(handle, definition);
		return true;
	}
	
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		lastExplicitTimestamp = nanoSecondTimestamp;
		timeDataQ.add(new TimeStamp(nanoSecondTimestamp));
		
		pushTimestamp(nanoSecondTimestamp);
		return true;
	}
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		lastExplicitTimestamp = timestamp;
		packetQ.add(new StreamPacket(handle,timestamp,len,data));

		pushStremPacket(handle, timestamp, len, data);
		return true;
	}
	
	
	

	//CLASSES WE NEED TOO SAVE THINGS
	
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
	
}



