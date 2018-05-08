package pipeLines.filters;

import java.time.Duration;
import java.time.ZonedDateTime;

import pipeLines.Pipe;

/**
 * Date in meta can only be set backwards not forwards as not to get negative timestamps
 * if parseMeta is called without needed data we will get errors.
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
	boolean pushMeta = true;
	boolean parse = false;

	public ChangeDateTime()
	{
	}
	
	public ChangeDateTime(long corection)
	{
		this.posibleCorection = corection;
		parse = true;
	}

	public ChangeDateTime(String date, String time, String zone, boolean pushMeta) {
		dateM2 = date;
		timeM2 = time;
		zoneM2 = zone;
		this.pushMeta = pushMeta;
		parse = true;
	}

	
	
	
	/*
	 * DOES NOT RETURN DESCENDATS RESULTS!
	 * (non-Javadoc)
	 * @see filters.Filter#onMetadata(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean onMetadata(String key, String value) {

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
			pushMetadata(key, value);
			return true;
		}

		if(parse && dateM1 != null && timeM1 == null && zoneM1 !=null)
		{
			parseMeta();
		}
		return true;
	}


	/**
	 * Called when we have read metadata with time and date.
	 * calculate time diference betwen files and set first date and time for new S2
	 */
	public void parseMeta()
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
			if(pushMeta)
			{
				pushMetadata("date", dateM1);
				pushMetadata("time", timeM1);
				pushMetadata("timezone", zoneM1);
			}
			corection = 0;
		}else
		{
			if(pushMeta)
			{
				pushMetadata("date", dateM2);
				pushMetadata("time", timeM2);
				pushMetadata("timezone", zoneM2);
			}
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
	
	
	// NEW METHODS
	
	
	public void pushMeta()
	{
		pushMetadata("date", dateM1);
		pushMetadata("time", timeM1);
		pushMetadata("timezone", zoneM1);
	}
	
	
	//GETTERS SETTERS
	
	
	public long getPosibleCorection()
	{
		return posibleCorection;
	}
	
	
	/**
	 * @return 0 if we will corent this meta or posiblecorection if posiblecorection is >0 and thefore we didnt chage meta here.
	 */
	public long getNeededCorection()
	{
		return Math.max(0, posibleCorection);
	}
	
	public void setPosibleCorection(long posibleCorection)
	{
		this.posibleCorection = posibleCorection;
	}
	
	public void setMetaM2(String date, String time, String zone)
	{
		dateM2 = date;
		timeM2 = time;
		zoneM2 = zone;
	}
	
	public void setPushMeta(boolean pushMeta)
	{
		this.pushMeta = pushMeta;
	}

}
