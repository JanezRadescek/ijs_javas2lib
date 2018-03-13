package filters;

import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

public class FilterHandles extends Filter {

	private long handles;

	public FilterHandles(long handles)
	{
		this.handles = handles;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		if((handles & 1<<handle) != 0)
		{
			pushDefinition(handle, definition);
		}
		return true;
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		if((handles & 1<<handle) != 0)
		{
			pushDefinition(handle, definition);
		}
		return true;
	}
	
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		if((handles & 1<<handle) != 0)
		{
			pushStremPacket(handle, timestamp, len, data);
		}
		return true;
	}

}
