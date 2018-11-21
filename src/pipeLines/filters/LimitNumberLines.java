package pipeLines.filters;

import pipeLines.Pipe;

/**
 * Its purpose is mostly for limiting output for debuging purposes.
 * @author janez
 *
 */
public class LimitNumberLines extends Pipe {
	
	int counterStreamPackets = 0;
	int counterSpecial = 0;
	int counterUnknown = 0;
	int maxStreamPackets = 0;
	int maxSpecial = 10;
	int maxUnknown = 10;
	
	public LimitNumberLines(int maxLines)
	{
		this.maxStreamPackets = maxLines;
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		counterSpecial++;
		if(counterSpecial <= maxSpecial)
		{
			return super.onSpecialMessage(who, what, message);
		}
		return true;
		
	}
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		counterStreamPackets++;
		if(counterStreamPackets <= maxStreamPackets)
		{
			return super.onStreamPacket(handle, timestamp, len, data);
		}
		return true;
	}
	
	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		counterUnknown++;
		if(counterUnknown <= maxUnknown)
		{
			return super.onUnknownLineType(type, len, data);
		}
		return true;
		
	}
	

}
