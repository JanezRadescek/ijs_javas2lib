package filters;

import java.time.Duration;
import java.time.ZonedDateTime;

public class SyncTime extends Filter {

	String dateM1;
	String timeM1;
	String zoneM1;
	String dateM2;
	String timeM2;
	String zoneM2;

	boolean parse = false;
	long corection = 0;

	public SyncTime()
	{
	}

	public SyncTime(long corection)
	{
		this.corection = corection;
		parse = false;
	}

	public SyncTime(String date, String time, String zone) {
		dateM2 = date;
		timeM2 = time;
		zoneM2 = zone;
		parse = true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		if(key.equals(dateM1))
		{
			dateM1 = value;
		}
		if(key.equals(timeM1))
		{
			timeM1 = value;
		}
		if(key.equals(zoneM1))
		{
			zoneM1 = value;
		}
		if(parse && dateM1 != null && timeM1 == null && zoneM1 !=null)
		{
			parseMeta();

		}else
		{
			if(!parse)
			{
				//TODO pošlji meta data naprej !!!
			}
		}
		
		return true;
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
			date2 = ZonedDateTime.parse(dateM2+"T"+timeM2+zoneM2);
		}
		catch(java.time.format.DateTimeParseException e)
		{
			zoneM1 = zoneM1.substring(0, 3) + ":" + zoneM1.substring(3, zoneM1.length());
			zoneM2 = zoneM2.substring(0, 3) + ":" + zoneM2.substring(3, zoneM2.length());
			date1 = ZonedDateTime.parse(dateM1+"T"+timeM1+zoneM1);
			date2 = ZonedDateTime.parse(dateM2+"T"+timeM2+zoneM2);
		}
		
		Duration difference = Duration.between(date1, date2);
		//če je pozitivno je prvi datum prvi
		corection = (long) (difference.getSeconds()*1E9) + difference.getNano();
		
		
	}
	
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		// TODO Auto-generated method stub
		return super.onTimestamp(nanoSecondTimestamp-corection);
	}
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		// TODO Auto-generated method stub
		return super.onStreamPacket(handle, timestamp-corection, len, data);
	}

}
