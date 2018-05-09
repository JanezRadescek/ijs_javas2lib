package pipeLines.conglomerates;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import pipeLines.Pipe;
import pipeLines.filters.GetSuportingLines;
import pipeLines.filters.RemapHandle;
import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;

/**
 * makes sure diferent streams have differt handles
 * @author janez
 *
 */
public class SyncHandles extends Sync {

	RemapHandle rhS;

	/**
	 * @param secondaryInput last filter before Sync
	 * @param primaryS2 File of primary S2 file
	 * @param secondaryS2 File of secondary S2 file
	 */
	public SyncHandles(Pipe primaryInput, Pipe secondaryInput, File primaryS2, File secondaryS2)
	{	
		super(primaryInput,secondaryInput);
		S2 temS21 = new S2();
		S2 temS22 = new S2();
		LoadStatus ls1 = temS21.load(primaryS2.getParentFile(), primaryS2.getName());
		LoadStatus ls2 = temS22.load(secondaryS2.getParentFile(), secondaryS2.getName());

		GetSuportingLines gsl1 = new GetSuportingLines();
		GetSuportingLines gsl2 = new GetSuportingLines();

		ls1.readLines(gsl1 , false);
		ls2.readLines(gsl2 , false);

		Set<Byte> handles1 = gsl1.getUsedHandles();
		Set<Byte> handles2 = gsl2.getUsedHandles();

		buildSyncHandles(handles1, handles2);
	}
	
	
	
	
	/**
	 * @param secondaryInput last filter before Sync
	 * @param handles1 handles used in primary S2
	 * @param handles2 handles used in secondary S2
	 */
	public SyncHandles(Pipe primaryInput, Pipe secondaryInput, Set<Byte> handles1, Set<Byte> handles2)
	{
		super(primaryInput, secondaryInput);
		buildSyncHandles(handles1, handles2);
	}
	
	private void buildSyncHandles(Set<Byte> handles1, Set<Byte> handles2)
	{
		Map<Byte,Byte> remapForSecondary = new HashMap<Byte,Byte>();
		for(byte i:handles2)
		{
			if(handles1.contains(i))
			{
				int start = 0;
				int end = 32;
				if(i>=32)
				{
					start = 32;
					end = 128;
				}
				byte t = 0;
				for(int j=start;j<end;j++)
				{
					if(!handles1.contains((byte)j) && !remapForSecondary.containsValue((byte)j));
					{
						t = (byte) j;
						break;
					}
				}
				remapForSecondary.put(i, t);
				
			}else
			{
				remapForSecondary.put(i, i);
			}

		}

		primaryOutPut = primaryInPut;
		
		rhS = new RemapHandle(remapForSecondary);
		secondaryInPut.addChild(rhS);
		secondaryOutPut = rhS;
	}

}
