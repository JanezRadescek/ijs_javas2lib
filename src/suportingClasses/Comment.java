package suportingClasses;

public class Comment extends Line {

	public String comment;
	public Comment(long timestamp, String comment) {
		super(timestamp);
		this.comment = comment;
	}
	public Comment(String comment) {
		this.comment = comment;
	}
}
