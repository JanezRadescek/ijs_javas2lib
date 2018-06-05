package pipeLines;

import java.io.PrintStream;
import java.util.ArrayList;

import si.ijs.e6.S2.ReadLineCallbackInterface;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/**
 * Template class for further subclassing. It send variables it gets in methonds specified in ReadLineCallbackInterface
 * to its children. All data send further down the three must be valid S2 file(Actual implementations must be independent and).
 * @author janez
 *
 */
public class Pipe implements ReadLineCallbackInterface
{
	
	ArrayList<Pipe> children = new ArrayList<Pipe>();
	protected PrintStream errPS;
	
	public Pipe addChild(Pipe f)
	{
		children.add(f);
		return f;
	}
	
	public void setErrPS(PrintStream errPS)
	{
		this.errPS = errPS;
	}
	
//    OVERRIDES	


	@Override
	public boolean onComment(String comment) {
		return pushComment(comment);
	}
	protected boolean pushComment(String comment) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onComment(comment);
		}
		return r;
	}


	@Override
	public boolean onVersion(int versionInt, String version) {
		return pushVersion(versionInt, version);
	}
	protected boolean pushVersion(int versionInt, String version) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onVersion(versionInt, version);
		}
		return r;
	}


	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		
		return pushSpecilaMessage(who, what, message);
	}
	protected boolean pushSpecilaMessage(char who, char what, String message) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onSpecialMessage(who, what, message);
		}
		return r;
	}


	@Override
	public boolean onMetadata(String key, String value) {
		
		return pushMetadata(key, value);
	}
	protected boolean pushMetadata(String key, String value) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onMetadata(key, value);
		}
		return r;
	}


	@Override
	public boolean onEndOfFile() {
		pushEndofFile();
		return false;
	}
	protected void pushEndofFile() {
		for(Pipe c:children)
		{
			c.onEndOfFile();
		}
	}


	@Override
	public boolean onUnmarkedEndOfFile() {
		pushUnmarkedEndofFile();
		return false;
	}
	protected void pushUnmarkedEndofFile() {
		for(Pipe c:children)
		{
			c.onUnmarkedEndOfFile();
		}
	}


	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
	
		return pushDefinition(handle, definition);
	}
	protected boolean pushDefinition(byte handle, SensorDefinition definition) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onDefinition(handle, definition);
		}
		return r;
	}


	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		return pushDefinition(handle, definition);
	}
	protected boolean pushDefinition(byte handle, StructDefinition definition) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onDefinition(handle, definition);
		}
		return r;
	}


	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		return pushDefinition(handle, definition);
	}
	protected boolean pushDefinition(byte handle, TimestampDefinition definition) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onDefinition(handle, definition);
		}
		return r;
	}


	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		
		return pushTimestamp(nanoSecondTimestamp);
	}
	protected boolean pushTimestamp(long nanoSecondTimestamp) {
		boolean r= true;
		for(Pipe c:children)
		{
			r &= c.onTimestamp(nanoSecondTimestamp);
		}
		return r;
	}


	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		
		return pushStreamPacket(handle, timestamp, len, data);
	}
	protected boolean pushStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onStreamPacket(handle, timestamp, len, data);
		}
		return r;
	}


	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		
		return pushUnknownLineType(type, len, data);
	}
	protected boolean pushUnknownLineType(byte type, int len, byte[] data) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onUnknownLineType(type, len, data);
		}
		return r;
	}


	@Override
	public boolean onError(int lineNum, String error) {
		
		return pushError(lineNum, error);
	}
	protected boolean pushError(int lineNum, String error) {
		boolean r = true;
		for(Pipe c:children)
		{
			r &= c.onError(lineNum, error);
		}
		return r;
	}
}
