package filters;

import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/**
 * Abstract because its unfinished
 * Counts number of lines and return false after MaxLines lines is read.
 * @author janez
 *
 */
public class FilterCounter extends Filter {

	private int counter;
	private int maxLines;

	public FilterCounter()
	{
		this.counter = 0;
		this.maxLines = 100;
	}
	
	public FilterCounter(int maxLines)
	{
		this.counter = 0;
		this.maxLines = maxLines;
	}

	public void reset(int maxLines)
	{
		this.counter = 0;
		this.maxLines = maxLines;
	}
	
	public int getCounter()
	{
		return counter;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		counter++;
		pushVersion(versionInt, version);
		return counter<=maxLines;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		counter++;
		pushMetadata(key, value);
		return counter<=maxLines;
	}

	@Override
	public boolean onComment(String comment) {
		counter++;
		pushComment(comment);
		return counter<=maxLines;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		counter++;
		pushSpecilaMessage(who, what, message);
		return counter<=maxLines;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		counter++;
		pushDefinition(handle, definition);
		return counter<=maxLines;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		counter++;
		pushDefinition(handle, definition);
		return counter<=maxLines;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		counter++;
		pushDefinition(handle, definition);
		return counter<=maxLines;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		counter++;
		pushTimestamp(nanoSecondTimestamp);
		return counter<=maxLines;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		counter++;
		pushStremPacket(handle, timestamp, len, data);
		return counter<=maxLines;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		counter++;
		pushError(lineNum, error);
		return counter<=maxLines;
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		counter++;
		pushUnknownLineType(type, len, data);
		return counter<=maxLines;
	}


}
