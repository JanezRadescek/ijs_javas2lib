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
	//TODO rename package filters to pipeline and this to prototype
	ArrayList<Filter> children = new ArrayList<Filter>();
	@Deprecated
	ArrayList<Filter> ancestors = new ArrayList<Filter>();
	public String errors = "";
	
	public Filter addChild(Filter f)
	{
		children.add(f);
		f.addAncestor(this);
		return f;
	}
	
	@Deprecated
	public void addAncestor(Filter f)
	{
		ancestors.add(f);
	}


	@Override
	public boolean onComment(String comment) {
		return pushComment(comment);
	}
	protected boolean pushComment(String comment) {
		boolean r = true;
		for(Filter c:children)
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
		for(Filter c:children)
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
		for(Filter c:children)
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
		for(Filter c:children)
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
	
		return pushDefinition(handle, definition);
	}
	protected boolean pushDefinition(byte handle, SensorDefinition definition) {
		boolean r = true;
		for(Filter c:children)
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
		for(Filter c:children)
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
		for(Filter c:children)
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
		for(Filter c:children)
		{
			r &= c.onTimestamp(nanoSecondTimestamp);
		}
		return r;
	}


	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		
		return pushStremPacket(handle, timestamp, len, data);
	}
	protected boolean pushStremPacket(byte handle, long timestamp, int len, byte[] data) {
		boolean r = true;
		for(Filter c:children)
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
		for(Filter c:children)
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
		for(Filter c:children)
		{
			r &= c.onError(lineNum, error);
		}
		return r;
	}
}
