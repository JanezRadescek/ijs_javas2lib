package filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import si.ijs.e6.S2.LoadStatus;

/**
 * makes sure diferent streams have differt handles
 * @author janez
 *
 */
public class SyncHandles extends Sync {
	
	private boolean doIT = true;
	RemapHandle rhS;
	RemapHandle rhP;
	
	public SyncHandles(LoadStatus ls, Filter firsFilter, Filter secondaryInput)
	{
		this.ls = ls;
		this.firstFilter = firsFilter;
		
		ArrayList<Byte> reservedHandles = new ArrayList<Byte>(); 
		rhP = new RemapHandle(new HashMap<Byte,Byte>());
		rhS = new RemapHandle(new HashMap<Byte,Byte>());
		rhP.setReserved(reservedHandles);
		rhS.setReserved(reservedHandles);
		
		
		secondaryInput.addChild(rhS);
		
		
		primaryOutPut = rhP;
		secondaryOutPut = rhS;
	}

}
