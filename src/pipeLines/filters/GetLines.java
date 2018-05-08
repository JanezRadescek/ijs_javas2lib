package pipeLines.filters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;
import suportingClasses.Comment;
import suportingClasses.SpecialMessage;
import suportingClasses.StreamPacket;
import suportingClasses.Line;
import suportingClasses.TimeStamp;
import suportingClasses.Version;

/**
 * It saves ALL variables/lines it gets in methonds specified in ReadLineCallbackInterface.
 * Variables/lines can than be accessed for further use via getters. 
 * @author janez
 *
 */
public class GetLines extends Pipe {
	
	private Version version;
	private Map<String, String> metadata = new HashMap<String, String>();
	private Map<Byte, SensorDefinition> sensorDefinitions = new HashMap<Byte, SensorDefinition>();
	private Map<Byte, StructDefinition> structDefinitions = new HashMap<Byte, StructDefinition>(); 
	private Map<Byte, TimestampDefinition> timestampDefinitions = new HashMap<Byte, TimestampDefinition>();
	private Queue<Line> timeDataQ = new LinkedList<Line>();
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
	public Queue<Line> getTimeDataQ() {
		return timeDataQ;
	}

	/**
	 * @return the packetQ
	 */
	public Queue<StreamPacket> getPacketQ() {
		return packetQ;
	}

	//OVERRIDES
	
	@Override
	public boolean onVersion(int versionInt, String version) {
		this.version = new Version(versionInt, version);

		return pushVersion(versionInt, version);
	}
	
	@Override
	public boolean onComment(String comment) {
		timeDataQ.add(new Comment(lastExplicitTimestamp, comment));

		
		return pushComment(comment);
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		timeDataQ.add(new SpecialMessage(lastExplicitTimestamp, who, what, message));
		
		
		return pushSpecilaMessage(who, what, message);
	}
	
	@Override
	public boolean onMetadata(String key, String value) {
		metadata.put(key, value);
		
		
		return pushMetadata(key, value);
	}
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		sensorDefinitions.put(handle, definition);
		
		
		return pushDefinition(handle, definition);
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		structDefinitions.put(handle, definition);
		
		
		return pushDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		timestampDefinitions.put(handle, definition);
		
		
		return pushDefinition(handle, definition);
	}
	
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		lastExplicitTimestamp = nanoSecondTimestamp;
		timeDataQ.add(new TimeStamp(nanoSecondTimestamp));
		
		
		return pushTimestamp(nanoSecondTimestamp);
	}
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		lastExplicitTimestamp = timestamp;
		packetQ.add(new StreamPacket(handle,timestamp,len,data));

		
		return pushStremPacket(handle, timestamp, len, data);
	}
	
}



