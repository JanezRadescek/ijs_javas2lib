package pipeLines.conglomerates;

import pipeLines.Pipe;
import si.ijs.e6.S2.LoadStatus;

/**
 * Sync classes are intended to sync S2 lines betwen two S2 file. Usualy to be able to merge them later
 * @author janez
 *
 */
public abstract class Sync extends Pipe {
	
	//"INTERFACE STUFF"
		//secondary
		LoadStatus ls;
		Pipe firstFilter;
		Pipe secondaryOutPut;
		//primary
		Pipe primaryOutPut = this;
		
		
		@Override
		public Pipe addChild(Pipe f) {
			primaryOutPut.addChild(f);
			return f;
		}
		
		/**
		 * @return the secondaryOutPut
		 */
		public Pipe getSecondaryOutPut() {
			return secondaryOutPut;
		}

}
