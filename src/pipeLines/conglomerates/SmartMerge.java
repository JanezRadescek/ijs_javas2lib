package pipeLines.conglomerates;

import java.io.File;

import pipeLines.Pipe;
import pipeLines.filters.GetSuportingLines;
import pipeLines.filters.Sprevodnik;
import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;

public class SmartMerge extends Sync{

	/**
	 * @param lsS LoadStatus of secondary S2 file 
	 * @param firstPipeS First Pipe of secondary S2 file
	 * @param primaryInPut 
	 * @param secondaryInPut
	 * @param primaryS2 File of primary S2
	 * @param secondaryS2 File of secondary S2
	 * @param makeNewHandles If true it will create new handles for secondary file where would otherwise overlap.
	 */
	public SmartMerge(LoadStatus lsS, Pipe firstPipeS, Pipe primaryInPut, Pipe secondaryInPut, File primaryS2, File secondaryS2, boolean makeNewHandles)
	{
		super(primaryInPut, secondaryInPut);

		//get data needed to merge
		S2 temS21 = new S2();
		S2 temS22 = new S2();
		LoadStatus ls1 = temS21.load(primaryS2.getParentFile(), primaryS2.getName());
		LoadStatus ls2 = temS22.load(secondaryS2.getParentFile(), secondaryS2.getName());

		GetSuportingLines gsl1 = new GetSuportingLines();
		GetSuportingLines gsl2 = new GetSuportingLines();

		ls1.readLines(gsl1 , false);
		ls2.readLines(gsl2 , false);
		
		temS21 = null;
		temS22 = null;
		ls1 = null;
		ls2 = null;
		
		if(!gsl1.getVersion().equals(gsl2.getVersion()))
		{
			error +=  "Versions are not the same. Primary file has version " + gsl1.getVersion() + ". Seconary file has version " +gsl2.getVersion();
		}

		SyncTime syncT = new SyncTime(primaryInPut, secondaryInPut, gsl1.getMeta(), gsl2.getMeta());
		Sprevodnik sp;
		
		if(makeNewHandles)
		{
			SyncHandles syncH = new SyncHandles(syncT.primaryOutPut, syncT.secondaryOutPut, gsl1.getUsedHandles(), gsl2.getUsedHandles());
			sp = new Sprevodnik(lsS, firstPipeS, syncH.secondaryOutPut);
			syncH.primaryOutPut.addChild(sp);
		}
		else
		{
			sp = new Sprevodnik(lsS, firstPipeS, secondaryInPut);
			primaryInPut.addChild(sp);
		}
		
		Merge m = new Merge(sp, sp.getSecondaryOutPut());
		
		primaryOutPut = m;

		//TODO to nekam vstavi
		/*
		  if(versionInt == fvS.versionInt && version.equals(fvS.version))
		{
			return pushVersion(versionInt, version);
		}
		else
		{
			errors += "Versions of S2 files arent equal.\n";
			return false;
		}
		 */
	}

}
