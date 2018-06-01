package pipeLines.filters;

import pipeLines.Pipe;
import si.ijs.e6.S2.LoadStatus;

/**
 * Class used to read secondary file up to last ready time
 * @author janez
 *
 */
public class Sprevodnik extends Pipe {
	
	LoadStatus lsS;
	Pipe firstPipeS;
	private Pipe secondayOutPut;
	private FilterTime ft;
	private LimitEnds le;
	
	
	/**
	 * @param lsS load status of secondary file
	 * @param firstPipeS first callback of first file
	 * @param secondaryInPut direct ancestor of second file
	 */
	public Sprevodnik(LoadStatus lsS, Pipe firstPipeS, Pipe secondaryInPut)
	{
		this.lsS = lsS;
		this.firstPipeS = firstPipeS;
		
		ft = new FilterTime(0, 0, false, FilterTime.PAUSE);
		le = new LimitEnds();
		
		secondaryInPut.addChild(ft);
		ft.addChild(le);
		secondayOutPut = le;
			
	}
	
	@Override
	public boolean onUnmarkedEndOfFile() {
		ft.setTimeInterval(0, Long.MAX_VALUE);
		lsS.readLines(firstPipeS, true);
		return super.onUnmarkedEndOfFile();
	}
	
	@Override
	public boolean onEndOfFile() {
		ft.setTimeInterval(0, Long.MAX_VALUE);
		lsS.readLines(firstPipeS, true);
		return super.onEndOfFile();
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
