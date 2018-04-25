package filters;

import java.util.ArrayList;

import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/**
 * Data bounded to handles is send further only if its handle is in Array of handles from constructor
 * @author janez
 *
 */
public class FilterHandles extends Filter {

	private ArrayList<Byte> handles = new ArrayList<Byte>();

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
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		if(handles.contains(handle))
		{
			return pushStremPacket(handle, timestamp, len, data);
		}
		return true;
	}

}
