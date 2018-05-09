package pipeLines.filters;

import pipeLines.Pipe;
import si.ijs.e6.S2.LoadStatus;

public class Sprevodnik extends Pipe {
	
	LoadStatus lsS;
	Pipe firstPipeS;
	private Pipe secondayOutPut;
	private FilterTime ft;
	
	
	public Sprevodnik(LoadStatus lsS, Pipe firstPipeS, Pipe secondaryInPut)
	{
		this.lsS = lsS;
		this.firstPipeS = firstPipeS;
		
		ft = new FilterTime(0, 0, false, FilterTime.PAUSE);
		
		secondaryInPut.addChild(ft);
		secondayOutPut = ft;
		
		
	}
	
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		ft.setTimeInterval(0, nanoSecondTimestamp);
		lsS.readLines(firstPipeS, true);
		return super.onTimestamp(nanoSecondTimestamp);
	}
	
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		ft.setTimeInterval(0, timestamp);
		lsS.readLines(firstPipeS, true);
		return super.onStreamPacket(handle, timestamp, len, data);
	}
	
	
	public Pipe getSecondaryOutPut()
	{
		return secondayOutPut;
	}

}
