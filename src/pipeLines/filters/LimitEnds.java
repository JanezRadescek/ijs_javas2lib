package pipeLines.filters;

import pipeLines.Pipe;

/**
 * Primarly used in sprevodnik because of end repetions cause caos
 * @author janez
 *
 */
public class LimitEnds extends Pipe {
	
	boolean firstEnd = true;
	
	
	
	@Override
	public boolean onEndOfFile() {
		if(firstEnd)
		{
			firstEnd = false;
			return super.onEndOfFile();
		}
		else
		{
			return true;
		}
		
	}
	
	@Override
	public boolean onUnmarkedEndOfFile() {
		if(firstEnd)
		{
			firstEnd = false;
			return super.onUnmarkedEndOfFile();
		}
		else
		{
			return true;
		}
		
	}

}
