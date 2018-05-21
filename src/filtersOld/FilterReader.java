package filtersOld;

import java.io.File;

import pipeLines.Pipe;
import pipeLines.filters.FilterTime;
import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;

public class FilterReader extends Pipe {
	
	public FilterReader()
	{
		FilterTime ft = new FilterTime(0, 0);
	}
	
	public void read(long maxTime)
	{
		
	}

}
