package pipeLines.filters;

import java.util.ArrayList;

import pipeLines.Pipe;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/**
 * Data bounded to handles is send further only if its handle is in Array of handles from constructor.
 * Deprecated. Use {@link LockHandles} with {@link Shredder}. 
 * @author janez
 *
 */

public class FilterHandles extends Pipe {

	private ArrayList<Byte> handles = new ArrayList<Byte>();

	private long lastTime = 0;//in case we have deleted timestamps related data we want to keep timestams in some cases.
	private boolean deleted = false;//in case we have deleted timestamps related data we want to keep timestams in some cases.
	
	/**
	 * @param handles byte array of handles and its data we want to send forward
	 */
	public FilterHandles(ArrayList<Byte> handles)
	{
		this.handles = handles;
	}

	public FilterHandles(long handles)
	{
		ArrayList<Byte> tHandles = new ArrayList<Byte>();
		for(byte i=0; i<32; i++)
		{
			if((handles & 1<<i) != 0)
				tHandles.add(i);
		}
		this.handles = tHandles;
	}
	
	
	@Override
	public boolean onComment(String comment) {
		if(deleted)
		{
			pushTimestamp(lastTime);
			deleted = false;
		}
		return super.onComment(comment);
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if(deleted)
		{
			pushTimestamp(lastTime);
			deleted = false;
		}
		return super.onSpecialMessage(who, what, message);
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		if(handles.contains(handle))
		{
			return pushDefinition(handle, definition);
		}
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		if(handles.contains(handle))
		{
			return pushDefinition(handle, definition);
		}
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		deleted = false;
		return super.onTimestamp(nanoSecondTimestamp);
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		if(handles.contains(handle))
		{
			deleted = false;
			return pushStreamPacket(handle, timestamp, len, data);
		}
		deleted = true;
		lastTime = timestamp;
		return true;
	}

}
