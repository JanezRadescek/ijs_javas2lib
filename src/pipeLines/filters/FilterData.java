package pipeLines.filters;

import java.io.PrintStream;

import pipeLines.Pipe;

public class FilterData extends Pipe 
{
	public final int S2_version = 1;

	final int  C = 0b1;
	final int SM = 0b10;
	final int MD = 0b100;
	final int SP = 0b1000;
	final int UL = 0b10000;

	private int dataTypes;
	
	private boolean deleted = false;//if we delete datapacket with time we lose time for lines without time
	private long lastTime = 0;

	/**
	 * @param dataTypes which lines should stayed 
	 * @param errPS
	 */
	public FilterData(int dataTypes, PrintStream errPS)
	{
		this.errPS = errPS;
		this.dataTypes = dataTypes;

	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		if((dataTypes & MD) == 0 && version.equals("PCARD"))
		{
			dataTypes |=MD;
			this.errPS.println("Filtering Data. PCARD version of S2 needs meta data. Parameter data set to " + dataTypes);
		}
		return super.onVersion(versionInt, version);
	}

	@Override
	public boolean onComment(String comment) {
		if((dataTypes & C) != 0)
		{
			if(deleted)
			{
				pushTimestamp(lastTime);
				deleted = false;
			}
			return pushComment(comment);
		}
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if((dataTypes & SM) != 0)
		{
			if(deleted)
			{
				pushTimestamp(lastTime);
				deleted = false;
			}
			return pushSpecilaMessage(who, what, message);
		}
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		if((dataTypes & MD) != 0)
		{
			return pushMetadata(key, value);
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
		if((dataTypes & SP) != 0)
		{
			deleted = false;
			return super.onStreamPacket(handle, timestamp, len, data);
		}
		else
		{
			deleted = true;
			lastTime = timestamp;
			return true;
		}
	}
	
	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		if(deleted)
		{
			pushTimestamp(lastTime);
			deleted = false;
		}
		
		if((dataTypes & UL) != 0)
		{
			return super.onUnknownLineType(type, len, data);
		}
		
		return true;
	}

}
