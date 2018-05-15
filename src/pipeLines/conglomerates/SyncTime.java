package pipeLines.conglomerates;

import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import pipeLines.Pipe;
import pipeLines.filters.ChangeDateTime;
import pipeLines.filters.GetSuportingLines;
import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;

/**
 * 
 * add merge as a child only to primary pipeline and provide start and end of secondary pipeline
 * @author janez
 *
 */
public class SyncTime extends Sync { 
	//IMPLEMENTATIONS STUFF

	//filters for regulating secondary S2
	ChangeDateTime cdtS;
	//PRIMARY PIPELINES
	ChangeDateTime cdtP;




	/**
	 * @param secondaryInput last filter of secondary file
	 * @param primaraS2 File of primary S2 file
	 * @param secondaryS2 File of secondary S2 file
	 */
	public SyncTime(Pipe primaryInput, Pipe secondaryInput,  File primaryS2, File secondaryS2, PrintStream print) 
	{
		super(primaryInput, secondaryInput);
		out = print;
		
		S2 temS21 = new S2();
		S2 temS22 = new S2();
		LoadStatus ls1 = temS21.load(primaryS2.getParentFile(), primaryS2.getName());
		LoadStatus ls2 = temS22.load(secondaryS2.getParentFile(), secondaryS2.getName());

		GetSuportingLines gsl1 = new GetSuportingLines();
		GetSuportingLines gsl2 = new GetSuportingLines();

		ls1.readLines(gsl1 , true);
		ls2.readLines(gsl2 , true);

		Map<String, String> meta1 = gsl1.getMeta();
		Map<String, String> meta2 = gsl2.getMeta();


		buildSyncTime(meta1, meta2);

	}

	/**
	 * @param primaryInput last filter of primary file
	 * @param secondaryInput last filter of secondary file
	 * @param meta1 metadata from primary S2 file
	 * @param meta2 metadata from secondary S2 file
	 */
	public SyncTime(Pipe primaryInput, Pipe secondaryInput,  Map<String, String> meta1, Map<String, String> meta2, PrintStream print) 
	{
		super(primaryInput, secondaryInput);
		out = print;
		buildSyncTime(meta1, meta2);
	}

	private void buildSyncTime(Map<String, String> meta1, Map<String, String> meta2)
	{
		//PRIMARY PIPES 
		cdtP = new ChangeDateTime(meta1, meta2, this.out);
		primaryInPut.addChild(cdtP);
		primaryOutPut = cdtP;

		//SECONDARY PIPES
		cdtS = new ChangeDateTime(meta2, meta1, this.out);
		secondaryInPut.addChild(cdtS);
		secondaryOutPut = cdtS;
	}
}
