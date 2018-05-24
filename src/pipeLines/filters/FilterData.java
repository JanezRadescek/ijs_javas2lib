package pipeLines.filters;

import java.io.PrintStream;

import pipeLines.Pipe;

public class FilterData extends Pipe 
{
	public final int S2_version = 1;
	
	final int C = 0b1;
	final int SM = 0b10;
	
	final int MD = 0b100;
	
	private int data;

	public FilterData(int data, PrintStream errPS)
	{
		this.errPS = errPS;
		this.data = data;
		if((data & MD) == 0)
		{
			data |=MD;
			this.errPS.println("Filtering Data. This version of S2 needs meta data. Parameter data set to " + data);
		}
	}
	
	@Override
	public boolean onComment(String comment) {
		if((data & C) != 0)
		{
			return pushComment(comment);
		}
		return true;
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if((data & SM) != 0)
		{
			return pushSpecilaMessage(who, what, message);
		}
		return true;
	}
	
	@Override
	public boolean onMetadata(String key, String value) {
		if((data & MD) != 0)
		{
			return pushMetadata(key, value);
		}
		return true;
	}
	
}
