package filters;

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
public abstract class Filter implements ReadLineCallbackInterface
{
	ArrayList<Filter> children = new ArrayList<Filter>();
	
	public Filter addChild(Filter f)
	{
		children.add(f);
		return this;
	}


	@Override
	public boolean onComment(String comment) {
		pushComment(comment);
		return true;
	}
	protected void pushComment(String comment) {
		for(Filter c:children)
		{
			c.onComment(comment);
		}
	}


	@Override
	public boolean onVersion(int versionInt, String version) {
		pushVersion(versionInt, version);
		return true;
	}
	protected void pushVersion(int versionInt, String version) {
		for(Filter c:children)
		{
			c.onVersion(versionInt, version);
		}
	}


	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		pushSpecilaMessage(who, what, message);
		return true;
	}
	protected void pushSpecilaMessage(char who, char what, String message) {
		for(Filter c:children)
		{
			c.onSpecialMessage(who, what, message);
		}
	}


	@Override
	public boolean onMetadata(String key, String value) {
		pushMetadata(key, value);
		return true;
	}
	protected void pushMetadata(String key, String value) {
		for(Filter c:children)
		{
			c.onMetadata(key, value);
		}
	}


	@Override
	public boolean onEndOfFile() {
		pushEndofFile();
		return false;
	}
	protected void pushEndofFile() {
		for(Filter c:children)
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
		for(Filter c:children)
		{
			c.onUnmarkedEndOfFile();
		}
	}


	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		pushDefinition(handle, definition);
		return true;
	}
	protected void pushDefinition(byte handle, SensorDefinition definition) {
		for(Filter c:children)
		{
			c.onDefinition(handle, definition);
		}
	}


	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		pushDefinition(handle, definition);
		return true;
	}
	protected void pushDefinition(byte handle, StructDefinition definition) {
		for(Filter c:children)
		{
			c.onDefinition(handle, definition);
		}
	}


	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		pushDefinition(handle, definition);
		return true;
	}
	protected void pushDefinition(byte handle, TimestampDefinition definition) {
		for(Filter c:children)
		{
			c.onDefinition(handle, definition);
		}
	}


	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		pushTimestamp(nanoSecondTimestamp);
		return true;
	}
	protected void pushTimestamp(long nanoSecondTimestamp) {
		for(Filter c:children)
		{
			c.onTimestamp(nanoSecondTimestamp);
		}
	}


	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		pushStremPacket(handle, timestamp, len, data);
		return true;
	}
	protected void pushStremPacket(byte handle, long timestamp, int len, byte[] data) {
		for(Filter c:children)
		{
			c.onStreamPacket(handle, timestamp, len, data);
		}
	}


	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		pushUnknownLineType(type, len, data);
		return true;
	}
	protected void pushUnknownLineType(byte type, int len, byte[] data) {
		for(Filter c:children)
		{
			c.onUnknownLineType(type, len, data);
		}
	}


	@Override
	public boolean onError(int lineNum, String error) {
		pushError(lineNum, error);
		return true;
	}
	protected void pushError(int lineNum, String error) {
		for(Filter c:children)
		{
			c.onError(lineNum, error);
		}
	}
}
