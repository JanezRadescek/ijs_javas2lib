package suportingClasses;

public class SpecialMessage extends Line {

	public char who;
	public char what;
	public String message;
	public SpecialMessage(long timestamp, char who, char what, String message)
	{
		super(timestamp);
		this.who = who;
		this.what = what;
		this.message = message;
	}
	public SpecialMessage(char who, char what, String message)
	{
		this.who = who;
		this.what = what;
		this.message = message;
	}
}
