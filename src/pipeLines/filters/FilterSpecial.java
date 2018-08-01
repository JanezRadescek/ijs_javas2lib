package pipeLines.filters;

import java.util.regex.Pattern;

import pipeLines.Pipe;

public class FilterSpecial extends Pipe {

	//public static char WHO_TRUE = (char) 5;
	//public static char WHAT_TRUE = (char) 5;
	//public static String MESSEGE_TRUE = "";

	char who;
	char what;

	String regex;
	Pattern pat;
	boolean keep;

	/**
	 * 
	 * @param who device type
	 * @param what messege type
	 * @param regex regular explesion for messege
	 * @param keep if keep is true it will keep data maching previous 3 requirements and delete if not. if keep is false it will do reverse.
	 */
	public FilterSpecial(char who, char what, String regex, boolean keep) {
		this.who = who;
		this.what = what;
		this.regex = regex;
		pat = Pattern.compile(regex);
		this.keep = keep;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		boolean one = false;
		if(this.who == who)
		{
			one = true;
		}
		boolean two = false;
		if(this.what == what)
		{
			two = true;
		}
		boolean three = false;
		if(this.regex.equals(""))
		{
			three = true;
		}else
		{
			three = pat.matcher(message).matches();
		}


		if(!(keep ^ (one & two & three)))
		{
			return super.onSpecialMessage(who, what, message);
		}else
		{
			return true;
		}
	}

}
