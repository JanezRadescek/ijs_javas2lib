package pipeLines.filters;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;

import pipeLines.Pipe;

/**
 * Date in meta can only be set backwards not forwards as not to get negative timestamps
 * @author janez
 *
 */
public class ChangeDateTime extends Pipe {

	String dateM1 = null;
	String timeM1 = null;
	String zoneM1 = null;
	String dateM2 = null;
	String timeM2 = null;
	String zoneM2 = null;

	long posibleCorection = 0;
	private long corection = 0;
	/**
	 * If true we already have metadata for this S2
	 */
	boolean preMeta;



	public ChangeDateTime(long corection)
	{
		this.posibleCorection = corection;
		preMeta = false;
	}

	public ChangeDateTime(String date2, String time2, String zone2) {
		dateM2 = date2;
		timeM2 = time2;
		zoneM2 = zone2;
		preMeta = false;
	}

	/**
	 * @param date1 date of this S2 file
	 * @param time1 time of this S2 file
	 * @param zone1 zone of this S2 file
	 * @param date2 date on which we want to set this S2
	 * @param time2 time on which we want to set this S2
	 * @param zone2 zone on which we want to set this S2
	 */
	public ChangeDateTime(String date1, String time1, String zone1, String date2, String time2, String zone2) {
		dateM1 = date1;
		timeM1 = time1;
		zoneM1 = zone1;


		dateM2 = date2;
		timeM2 = time2;
		zoneM2 = zone2;
	
		parseMeta();

		preMeta = true;
	}
	
	public ChangeDateTime(Map<String,String> meta1, Map<String, String> meta2) {
		this(meta1.get("date"), meta1.get("time"), meta1.get("timezone"), meta2.get("date"), meta2.get("time"), meta2.get("timezone"));
	}




	/*
	 * DOES NOT RETURN DESCENDATS RESULTS!
	 * (non-Javadoc)
	 * @see filters.Filter#onMetadata(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean onMetadata(String key, String value) {
		if(!preMeta)
		{
			switch(key)
			{
			case "date" :
				dateM1 = value;
				break;
			case "time" :
				timeM1 = value;
				break;
			case "timezone" :
				zoneM1 = value;
				break;
			default:
				return pushMetadata(key, value);
			}

			if(dateM1 != null && timeM1 == null && zoneM1 !=null)
			{
				parseMeta();

				boolean R = true;
				if(posibleCorection>0)
				{

					R &= pushMetadata("date", dateM1);
					R &= pushMetadata("time", timeM1);
					R &= pushMetadata("timezone", zoneM1);

					corection = 0;
				}else
				{

					R &= pushMetadata("date", dateM2);
					R &= pushMetadata("time", timeM2);
					R &= pushMetadata("timezone", zoneM2);

					corection = posibleCorection;
				}
				return R;


			}
			return true;
		}
		else
		{
			return pushMetadata(key, value);
		}
	}


	/**
	 * Called when we have read metadata with time and date.
	 * calculate time diference betwen files and set first date and time for new S2
	 */
	private void parseMeta()
	{
		ZonedDateTime date1;
		ZonedDateTime date2;
		try
		{
			date1 = ZonedDateTime.parse(dateM1+"T"+timeM1+zoneM1);

		}
		catch(java.time.format.DateTimeParseException e)
		{
			zoneM1 = zoneM1.substring(0, 3) + ":" + zoneM1.substring(3, zoneM1.length());
			date1 = ZonedDateTime.parse(dateM1+"T"+timeM1+zoneM1);
		}


		if(dateM2 !=null)
		{
			try
			{
				date2 = ZonedDateTime.parse(dateM2+"T"+timeM2+zoneM2);
			}
			catch(java.time.format.DateTimeParseException e)
			{
				zoneM2 = zoneM2.substring(0, 3) + ":" + zoneM2.substring(3, zoneM2.length());
				date2 = ZonedDateTime.parse(dateM2+"T"+timeM2+zoneM2);
			}

			Duration difference = Duration.between(date1, date2);
			//če je pozitivno je prvi datum prvi
			posibleCorection = (long) (difference.getSeconds()*1E9) + difference.getNano();
		}else
		{

			date2 = date1.plusNanos(posibleCorection);
			dateM2 = date2.getYear()+"-"+date2.getMonthValue()+"-"+date2.getDayOfMonth();
			timeM2 = date2.getHour()+":"+date2.getMinute()+":"+date2.getSecond()+"."+((int)(date2.getNano()/1E6));
			zoneM2 = date2.getOffset().getId().substring(0, 6);
		}


		if(posibleCorection>0)
		{
			corection = 0;
		}else
		{
			corection = posibleCorection;
		}
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		return super.onTimestamp(nanoSecondTimestamp-corection);
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		return super.onStreamPacket(handle, timestamp-corection, len, data);
	}

	//GETTERS SETTERS
	public long getCorection()
	{
		return corection;
	}
}
