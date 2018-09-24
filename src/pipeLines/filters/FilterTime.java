package pipeLines.filters;

import pipeLines.Pipe;

/**
 * Send further only data in given time interval. 
 * @author janez
 *
 */
public class FilterTime extends Pipe {
	//CENCEPTUAL THINGIS
	private long start;
	private long end;
	private boolean approximate;
	// 
	private byte type;

	//IMPEMENTATION THINGIS
	/**
	 * It doent return false outside interval
	 */
	public static final byte FILTER = 0;
	/**
	 * It returns false outside interval
	 */
	public static final byte PAUSE = 1;
	/**
	 * It returns false outside interval. It also pushEndOfFile
	 */
	public static final byte BUSTER = 2;

	private long lastRecordedTime = 0;

	boolean overWasSend = false;

	private boolean deleted = false;//in case we have deleted timestamps related data we want to keep timestams in some cases.

	/**
	 * [start, end) in nanoseconds
	 * @param start 
	 * @param end
	 */
	public FilterTime(long start, long end)
	{
		this.start = start;
		this.end = end;
		this.approximate = false;
		this.type = FILTER;
	}

	/**
	 * [start, end) in nanoseconds
	 * @param start 
	 * @param end
	 * @param approximate
	 */
	public FilterTime(long start, long end, boolean approximate)
	{
		this(start,end);
		this.approximate = approximate;
	}

	/**
	 * [start, end) in nanoseconds
	 * @param start
	 * @param end
	 * @param approximate
	 * @param type use one of static constants FILTER,PAUSE,BUSTER
	 */
	public FilterTime(long start, long end, boolean approximate, byte type)
	{
		this(start,end);
		this.approximate = approximate;
		this.type = type;
	}

	/**
	 * [start, end) in seconds
	 * @param start 
	 * @param end
	 */
	public FilterTime(double start, double end)
	{
		this.start = (long) (start*1E9);
		this.end = (long)(end*1E9);
		this.approximate = false;
		this.type = FILTER;
	}

	/**
	 * @param start in seconds
	 * @param end in seconds
	 * @param approximate
	 */
	public FilterTime(double start, double end, boolean approximate)
	{
		this(start,end);
		this.approximate = approximate;
	}

	/**
	 * @param start in seconds
	 * @param end in seconds
	 * @param approximate
	 * @param type use one of static constants FILTER,PAUSE,BUSTER
	 */
	public FilterTime(double start, double end, boolean approximate, byte type)
	{
		this(start,end);
		this.approximate = approximate;
		this.type = type;
	}

	/**
	 * Dont use this method after endoffile was called(dont use if it was run with BUSTER). May result with double endoffile in S2
	 * @param start
	 * @param end
	 */
	public void setTimeInterval(long start, long end)
	{
		this.start = start;
		this.end = end;
	}

	/**
	 * @param type use one of static constants FILTER,PAUSE,BUSTER
	 */
	public void setType(byte type)
	{
		this.type = type;
	}



	@Override
	public boolean onComment(String comment) {
		if(!approximate || (approximate && (start<=lastRecordedTime && lastRecordedTime<end)))
		{
			if(deleted)
			{
				deleted = false;
				pushTimestamp(lastRecordedTime);
			}
			return pushComment(comment);
		}

		return true;

	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if(!approximate || (approximate && (start<=lastRecordedTime && lastRecordedTime<end)))
		{
			if(deleted)
			{
				deleted = false;
				pushTimestamp(lastRecordedTime);
			}
			return pushSpecilaMessage(who, what, message);
		}
		return true;

	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		lastRecordedTime = nanoSecondTimestamp;
		if(lastRecordedTime<end)
		{
			deleted = false;
			return pushTimestamp(nanoSecondTimestamp);
		}
		else
		{
			deleted = true;
			return atWorldsEns();
		}
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		lastRecordedTime = timestamp;

		if(lastRecordedTime<start)
		{
			deleted = true;
			return true;
		}

		if(start<=lastRecordedTime && lastRecordedTime<end)
		{
			deleted = false;
			return pushStreamPacket(handle, timestamp, len, data);
		}
		else
		{
			deleted = true;
			return atWorldsEns();
		}


	}
	
	
	private boolean atWorldsEns()
	{
		if(type == BUSTER)
		{
			if(!overWasSend)
			{
				pushEndofFile();
				overWasSend = true;
			}
			return false;
		}
		if(type == PAUSE)
		{
			return false;
		}
		if(type == FILTER)
		{
			return true;
		}
		
		//ČE JE KEJ DRUCGA IN PRIDE DO SEM SE OBNAŠA KOT FILTER
		
		return true;
	}
	
	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		if(deleted)
		{
			pushTimestamp(lastRecordedTime);
			deleted = false;
		}
		return super.onUnknownLineType(type, len, data);
	}

}
