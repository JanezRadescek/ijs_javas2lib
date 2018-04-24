package filters;

import java.io.File;

import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;

public class FilterMerge extends Filter {

	//stuff for second S2 file
	private LoadStatus ls;
	private Filter firstFilter;
	private Filter middleFilter;
	//filters for regulating second S2
	FilterTime ft;
	FilterCounter fc;
	FilterGetVersion fv;
	Pipe lastFilter;

	//stuff we need to know for some time


	public FilterMerge(LoadStatus ls) 
	{
		this(ls, new Pipe());
	}
	
	/**
	 * @param ls load status of second file
	 * @param firstFilter 
	 */
	public FilterMerge(LoadStatus ls, Filter firstFilter) 
	{
		this(ls, firstFilter,firstFilter);
	}
	
	/**
	 * @param ls load status of second file
	 * @param firstFilter
	 * @param middleFilter last filter of second file
	 */
	public FilterMerge(LoadStatus ls,Filter firstFilter, Filter middleFilter) 
	{
		this.ls = ls;
		this.firstFilter = firstFilter;
		this.middleFilter = middleFilter;

		ft = new FilterTime(0, 0);
		fc = new FilterCounter();
		fv = new FilterGetVersion();
		lastFilter = new Pipe();
		
		middleFilter.addChild(ft);
		ft.addChild(fc);
		fc.addChild(fv);
		fv.addAncestor(lastFilter);
		


	}

	@Override
	public Filter addChild(Filter f) {
		lastFilter.addChild(f);
		return super.addChild(f);
	}


	@Override
	public boolean onVersion(int versionInt, String version) {

		ls.readLines(firstFilter, true);

		if(versionInt == fv.versionInt && version.equals(fv.version))
		{
			return true;
		}
		else
		{
			return false;
		}
		
	}




}
