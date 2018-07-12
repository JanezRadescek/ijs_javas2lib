package pipeLines.filters;

import java.io.PrintStream;
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
	 * If true we already have new metadata and old for this S2
	 */
	boolean preMeta;



	public ChangeDateTime(long corection, PrintStream errPS)
	{
		this.errPS = errPS;
		this.posibleCorection = corection;
		preMeta = false;
	}

	public ChangeDateTime(String date2, String time2, String zone2, PrintStream errPS) {
		this.errPS = errPS;

		dateM2 = date2;
		timeM2 = time2;
		zoneM2 = zone2;
		preMeta = false;
	}

	/**
	 * this is used if we somehow got meta in some previous run.
	 * @param date1 date of this S2 file
	 * @param time1 time of this S2 file
	 * @param zone1 zone of this S2 file
	 * @param date2 date on which we want to set this S2
	 * @param time2 time on which we want to set this S2
	 * @param zone2 zone on which we want to set this S2
	 */
	public ChangeDateTime(String date1, String time1, String zone1, String date2, String time2, String zone2, PrintStream errPS) {
		this(date2,time2,zone2, errPS);

		dateM1 = date1;
		timeM1 = time1;
		zoneM1 = zone1;

		try
		{
			parseMeta();
		}
		catch(Exception e)
		{
			errPS.println(e.getMessage());
		}

		preMeta = true;
	}

	/**
	 * @param meta1 time meta data of this S2 file
	 * @param meta2 time meta data of some other S2 file
	 * @param errPS printstrem for errors
	 */
	public ChangeDateTime(Map<String,String> meta1, Map<String, String> meta2, PrintStream errPS) {
		this(meta1.get("date"), meta1.get("time"), meta1.get("timezone"), meta2.get("date"), meta2.get("time"), meta2.get("timezone"), errPS);
	}


	@Override
	public boolean onVersion(int versionInt, String version) {
		if(!version.equals("PCARD"))
		{
			errPS.println("Pipe ChangeDateTime has undefined behavior on S2 files that are not PCARD. \nVersion of this S2 file is " +version);
		}
		return super.onVersion(versionInt, version);
	}


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

			if(dateM1 != null && timeM1 != null && zoneM1 !=null)
			{
				try
				{
					parseMeta();
				}
				catch(Exception e)
				{
					errPS.println(e.getMessage());
					return false;
				}

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
			//we have new meta and corection. just do it
			switch(key)
			{
			case "date" :
				return pushMetadata(key, dateM2);
			case "time" :
				return pushMetadata(key, timeM2);
			case "timezone" :
				return pushMetadata(key, zoneM2);
			default:
				return pushMetadata(key, value);
			}
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
			errPS.println("Date and Time can NOT be changed into future since we may get negative timestamps. Corection set to 0.");
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
