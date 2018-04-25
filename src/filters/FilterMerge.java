package filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import si.ijs.e6.S2.LoadStatus;

/**
 * 
 * add merge as a child only to primary pipeline and provide start and end of secondary pipeline
 * @author janez
 *
 */
public class FilterMerge extends Filter {

	//TODO unfinished class 
	
	//stuff for second S2 file
	private LoadStatus ls;
	private Filter firstFilter;
	//COMON
	Pipe exitFilter;
	//filters for regulating second S2
	ChangeDateTime cdtS;
	FilterTime ftS;
	FilterGetVersion fvS;
	//PRIMARY PIPELINES
	ChangeDateTime cdtP;

	//stuff we need to know for some time
	long lastTimeP = 0;


	//CONSTANTS
	ArrayList<String> specialMeta = new ArrayList<String>();
	Map<String,String> metaP = new HashMap<String,String>();

	/**
	 * Stuff we need to do on construction
	 */
	private FilterMerge()
	{
		specialMeta.add("date");
		specialMeta.add("time");
		specialMeta.add("timezone");
	}

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
		this();
		this.ls = ls;
		this.firstFilter = firstFilter;

		//COMON EXIT
		exitFilter = new Pipe();
		
		//SECONDARY PIPES
		cdtS = new ChangeDateTime();
		ftS = new FilterTime(0, lastTimeP);
		fvS = new FilterGetVersion();

		middleFilter.addChild(cdtS);
		cdtS.addChild(ftS);
		ftS.addChild(fvS);
		fvS.addChild(exitFilter);

		//PRIMARY PIPES AFTER MERGE
		cdtP = new ChangeDateTime();

		children.add(cdtP);
		cdtP.addChild(exitFilter);



	}

	@Override
	public Filter addChild(Filter f) {
		exitFilter.addChild(f);
		return f;
	}


	@Override
	public boolean onVersion(int versionInt, String version) {

		ls.readLines(firstFilter, true);

		if(versionInt == fvS.versionInt && version.equals(fvS.version))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean onMetadata(String key, String value) {
		if(!specialMeta.contains(key))
		{
			return super.onMetadata(key, value);
		}
		else
		{
			metaP.put(key, value);

			if(metaP.size() == 3)
			{
				cdtS.setMetaM2(metaP.get("date"), metaP.get("time"), metaP.get("timezone"));
				cdtS.parseMeta();
				if(cdtS.getNeededCorection() != 0)
				{
					cdtP.setPosibleCorection(-cdtS.getNeededCorection());
					cdtP.parseMeta();
					cdtP.pushNewMeta();
				}
				else
				{
					//we changed secondary meta therefore this one stays the same
					cdtS.pushNewMeta();
				}
			}
			return true;
		}
	}
	
	
	



}
