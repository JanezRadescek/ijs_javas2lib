package filters;

import si.ijs.e6.S2.LoadStatus;

public abstract class Sync extends Filter {
	
	//INTERFACE STUFF
		//secondary
		LoadStatus ls;
		Filter firstFilter;
		Filter secondaryOutPut;
		//primary
		Filter primaryOutPut;
		String Errors = "";
		
		
		@Override
		public Filter addChild(Filter f) {
			primaryOutPut.addChild(f);
			return f;
		}
		
		/**
		 * @return the secondaryOutPut
		 */
		public Filter getSecondaryOutPut() {
			return secondaryOutPut;
		}

}
