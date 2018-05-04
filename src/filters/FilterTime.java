package filters;

/**
 * Send further only data in given time interval. 
 * @author janez
 *
 */
public class FilterTime extends Filter {
	//CENCEPTUAL THINGIS
	private long start;
	private long end;
	private boolean filterTimeLessData;
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


	/**
	 * [start, end) in nanoseconds
	 * @param start 
	 * @param end
	 */
	public FilterTime(long start, long end)
	{
		this.start = start;
		this.end = end;
		this.filterTimeLessData = false;
		this.type = FILTER;
	}

	/**
	 * @param start in seconds
	 * @param end in seconds
	 * @param filterTimeLessData
	 */
	public FilterTime(long start, long end, boolean filterTimeLessData)
	{
		this(start,end);
		this.filterTimeLessData = filterTimeLessData;
	}

	/**
	 * @param start in seconds
	 * @param end in seconds
	 * @param filterTimeLessData
	 * @param type use one of static constants FILTER,PAUSE,BUSTER
	 */
	public FilterTime(long start, long end, boolean filterTimeLessData, byte type)
	{
		this(start,end);
		this.filterTimeLessData = filterTimeLessData;
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
		this.filterTimeLessData = false;
		this.type = FILTER;
	}

	/**
	 * @param start in seconds
	 * @param end in seconds
	 * @param filterTimeLessData
	 */
	public FilterTime(double start, double end, boolean filterTimeLessData)
	{
		this(start,end);
		this.filterTimeLessData = filterTimeLessData;
	}

	/**
	 * @param start in seconds
	 * @param end in seconds
	 * @param filterTimeLessData
	 * @param type use one of static constants FILTER,PAUSE,BUSTER
	 */
	public FilterTime(double start, double end, boolean filterTimeLessData, byte type)
	{
		this(start,end);
		this.filterTimeLessData = filterTimeLessData;
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
		if(!filterTimeLessData || (filterTimeLessData && (start<=lastRecordedTime && lastRecordedTime<end)))
		{
			return pushComment(comment);
		}

		return true;

	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if(!filterTimeLessData || (filterTimeLessData && (start<=lastRecordedTime && lastRecordedTime<end)))
		{
			return pushSpecilaMessage(who, what, message);
		}
		return true;

	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		lastRecordedTime = nanoSecondTimestamp;
		if(lastRecordedTime<end)
		{
			return pushTimestamp(nanoSecondTimestamp);
		}
		else
		{
			return atWorldsEns();
		}
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		lastRecordedTime = timestamp;

		if(lastRecordedTime<start)
		{
			return true;
		}

		if(start<=lastRecordedTime && lastRecordedTime<end)
		{
			return pushStremPacket(handle, timestamp, len, data);
		}
		else
		{
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

}
