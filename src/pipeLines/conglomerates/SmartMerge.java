package pipeLines.conglomerates;

import java.io.File;

import pipeLines.Pipe;
import pipeLines.filters.GetSuportingLines;
import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;

public class SmartMerge extends Sync {
	
	LoadStatus lsS;
	Pipe firstPipeS;
	
	public SmartMerge(LoadStatus lsS, Pipe firsPipeS, Pipe primaryInput, Pipe secondaryInput, File primaryS2, File secondaryS2)
	{
		super(primaryInput, secondaryInput);
		
		
		this.lsS = lsS;
		this.firstPipeS = firsPipeS;
		
		//get data needed to merge
		S2 temS21 = new S2();
		S2 temS22 = new S2();
		LoadStatus ls1 = temS21.load(primaryS2.getParentFile(), primaryS2.getName());
		LoadStatus ls2 = temS22.load(secondaryS2.getParentFile(), secondaryS2.getName());

		GetSuportingLines guh1 = new GetSuportingLines();
		GetSuportingLines guh2 = new GetSuportingLines();

		ls1.readLines(guh1 , false);
		ls2.readLines(guh2 , false);
		
		
		
		
		
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
