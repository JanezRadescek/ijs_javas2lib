package suportingClasses;

import java.util.Objects;

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
	
	@Override
	public boolean equals(Object obj) {
		
		//TODO override hashFunction
		if(obj == this)
		{
			return true;
		}
		if(obj instanceof SpecialMessage)
		{
			SpecialMessage tem = (SpecialMessage) obj;
			return Objects.equals(this.who,tem.who) && Objects.equals(this.what, tem.what) && Objects.equals(this.message, tem.message);
		}else
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(who,what,message);
	}
}
