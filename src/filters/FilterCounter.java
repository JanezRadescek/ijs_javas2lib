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
		
		return counter<=maxLines & pushVersion(versionInt, version);
	}

	@Override
	public boolean onMetadata(String key, String value) {
		counter++;
		
		return counter<=maxLines & pushMetadata(key, value);
	}

	@Override
	public boolean onComment(String comment) {
		counter++;
		
		return counter<=maxLines & pushComment(comment);
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		counter++;
		
		return counter<=maxLines & pushSpecilaMessage(who, what, message);
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		counter++;
		
		return counter<=maxLines & pushDefinition(handle, definition);
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		counter++;
		
		return counter<=maxLines & pushDefinition(handle, definition);
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		counter++;
		
		return counter<=maxLines & pushDefinition(handle, definition);
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		counter++;
		
		return counter<=maxLines & pushTimestamp(nanoSecondTimestamp);
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		counter++;
		
		return counter<=maxLines & pushStremPacket(handle, timestamp, len, data);
	}

	@Override
	public boolean onError(int lineNum, String error) {
		counter++;
		
		return counter<=maxLines & pushError(lineNum, error);
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		counter++;
		
		return counter<=maxLines & pushUnknownLineType(type, len, data);
	}


}
