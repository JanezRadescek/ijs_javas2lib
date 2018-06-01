package pipeLines.filters;

import java.io.PrintStream;

import pipeLines.Pipe;

public class FilterData extends Pipe 
{
	public final int S2_version = 1;

	final int C = 0b1;
	final int SM = 0b10;
	final int MD = 0b100;
	final int SP = 0b1000;

	private int dataTypes;

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
			return pushComment(comment);
		}
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if((dataTypes & SM) != 0)
		{
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
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		if((dataTypes & SP) != 0)
		{
			return super.onStreamPacket(handle, timestamp, len, data);
		}
		else
		{
			return true;
		}
	}

}
