package pipeLines.enzymes;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/**
 * @author janez
 * abstract class for lock classes. Lock and key like in enzymes
 * its purpose it to split pipe. Consequently you can apply one pipe on only one part of it. The other part will flow through it without modification. At the end of it merges it back into one stream again.
 * By default everything goes true key. Its up to subclasses to define which lines shouldnt go true key. Be carefull when to use pushXXXXX and when super.onXXXXXX
 */
public abstract class Lock extends Pipe {
	
	Pipe pipeKey;
	
	public Lock(Pipe key)
	{
		this.pipeKey = key;
	}
	
	@Override
	public Pipe addChild(Pipe f) {
		pipeKey.addChild(f);
		return super.addChild(f);
	}
	
	
	
	@Override
	public boolean onVersion(int versionInt, String version) {
		return pipeKey.onVersion(versionInt, version);
	}
	
	@Override
	public boolean onComment(String comment) {
		return pipeKey.onComment(comment);
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		return pipeKey.onSpecialMessage(who, what, message);
	}
	
	@Override
	public boolean onMetadata(String key, String value) {
		return pipeKey.onMetadata(key, value);
	}
	
	@Override
	public boolean onEndOfFile() {
		return pipeKey.onEndOfFile();
	}
	
	@Override
	public boolean onUnmarkedEndOfFile() {
		return pipeKey.onUnmarkedEndOfFile();
	}
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		return pipeKey.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		return pipeKey.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		return pipeKey.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		return pipeKey.onTimestamp(nanoSecondTimestamp);
	}
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		return pipeKey.onStreamPacket(handle, timestamp, len, data);
	}
	
	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		return pipeKey.onUnknownLineType(type, len, data);
	}
	
	@Override
	public boolean onError(int lineNum, String error) {
		return pipeKey.onError(lineNum, error);
	}
	
	
	
	
	

}
