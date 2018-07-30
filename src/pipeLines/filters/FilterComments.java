package pipeLines.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pipeLines.Pipe;

public class FilterComments extends Pipe {

	Pattern pat;
	boolean keep;

	public FilterComments(String regex, boolean keep) {
		this.pat = Pattern.compile(regex);
		this.keep = keep;
	}

	public boolean onComment(String comment) {
		Matcher m = pat.matcher(comment);
		if(!(keep ^ m.matches()))
		{
			return pushComment(comment);	
		}
		else
		{
			return true;
		}
	};

}
