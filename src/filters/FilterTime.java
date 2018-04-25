package filters;

/**
 * Send further only data in given time interval. 
 * @author janez
 *
 */
public class FilterTime extends Filter {

	private long start;
	private long end;
	private boolean filterTimeLessData;

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
	 * [start, end) in seconds
	 * @param start 
	 * @param end
	 */
	public FilterTime(double start, double end)
	{
		this.start = (long) (start*1E9);
		this.end = (long)(end*1E9);
		this.filterTimeLessData = false;
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

	public void setTimeInterval(long start, long end)
	{
		this.start = start;
		this.end = end;
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
			if(!overWasSend)
			{
				pushEndofFile();
				overWasSend = true;
			}
			return true;
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
			if(!overWasSend)
			{
				pushEndofFile();
				overWasSend = true;
			}
			return true;
		}


	}

}
