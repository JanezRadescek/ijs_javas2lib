package filters;

/**
 * Imlementation of merge for merge redy input strems 
 * @author janez
 *
 */
public class Merge extends Filter {
	
	//TODO little smarter filter
	public Merge(Filter primaryInput, Filter secondaryInput)
	{
		primaryInput.addChild(this);
		secondaryInput.addChild(this);
	}
	

}
