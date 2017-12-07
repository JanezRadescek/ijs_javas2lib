package cli;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class donkey {

	public static void main(String[] args) {
		/*
		System.out.println(Integer.MAX_VALUE);
		System.out.println(2 & 1<<1);
		System.out.println("logic " + (false&false));
		*/
		String string = "2017-09-08 11:40:49.270";
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = null;
		try {
			date = format.parse(string);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		System.out.println(date.getTime());
		System.out.println(date);
		System.out.println("");
		
		
		Map<String, Integer> meta = new HashMap<String, Integer>();
		meta.put("ha", 123);
		meta.put("haha", 321);
		meta.put("bo", 5670);
		HashSet<String> iskani = new HashSet<String>();
		iskani.add("ha");
		iskani.add("haa");
		System.out.println(meta.keySet().containsAll(iskani));
		
	}

}
