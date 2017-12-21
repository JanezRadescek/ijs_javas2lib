package donkey;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class donkey {

	public static void main(String[] args) {
		/*
		System.out.println(Integer.MAX_VALUE);
		System.out.println(2 & 1<<1);
		System.out.println("logic " + (false&false));
		*/
		String slo = "2017-09-08T10:40:48.271+01:00";
		String mur = "2017-09-08T10:40:47.270+01:00";
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		
		
		System.out.println(Float.MAX_VALUE);
		
		ZonedDateTime time1 = ZonedDateTime.parse(slo);
		
		DateTimeFormatter formater = DateTimeFormatter.ISO_DATE_TIME;
		ZonedDateTime time2 = ZonedDateTime.parse(mur);
		
		//ZonedDateTime time2 = ZonedDateTime.parse(murica, formater );
		Duration diff = Duration.between(time1, time2);
		System.out.println(diff.getSeconds());
		System.out.println(diff.getNano());
		System.out.println(diff.isNegative());
		
	}

}
