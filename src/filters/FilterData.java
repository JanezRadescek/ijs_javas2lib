package filters;

public class FilterData extends Filter 
{
	public final int S2_version = 1;
	
	final int C = 0b1;
	final int SM = 0b10;
	
	final int MD = 0b100;
	
	private int data;

	public FilterData(int data)
	{
		this.data = data;
		if((data & MD) != 0)
		{
			data |=MD;
			System.err.println("This version of S2 needs meta data. Parameter data set to " + data);
		}
	}
	
	@Override
	public boolean onComment(String comment) {
		if((data & C) != 0)
		{
			pushComment(comment);
		}
		return true;
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if((data & SM) != 0)
		{
			pushSpecilaMessage(who, what, message);
		}
		return true;
	}
	
	@Override
	public boolean onMetadata(String key, String value) {
		if((data & MD) != 0)
		{
			pushMetadata(key, value);
		}
		return true;
	}
	
}
