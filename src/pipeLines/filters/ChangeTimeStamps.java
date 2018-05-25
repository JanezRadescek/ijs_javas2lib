package pipeLines.filters;

import java.io.PrintStream;

import pipeLines.Pipe;

public class ChangeTimeStamps extends Pipe {
	
	long delay = 0;
	long delayChange = 0;
	boolean first = true;
	
	/**
	 * adds delay to all timestamps
	 * @param delay delay in ns
	 * @param errPS PrintStream for errors
	 */
	public ChangeTimeStamps(long delay, PrintStream errPS)
	{
		this.errPS = errPS;
		this.delay = delay;
	}
	
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		long timenew = nanoSecondTimestamp + delay;
		if(timenew >= 0)
		{
			first = false;
			return super.onTimestamp(timenew);
		}
		else
		{
			if(first)
			{
				errPS.println("Delay is negative and too big. Timestamps would be negative. Delay changed to "+ -nanoSecondTimestamp+ ".");
			}else
			{
				errPS.println("Delay is negative and too big. Timestamps would be negative. Delay changed to "+ -nanoSecondTimestamp+ "."
						+ "Timestams that have been processed before have original delay instead of new one");
				errPS.println();
			}
			first = false;
			delayChange = -(nanoSecondTimestamp + delay);
			delay += delayChange;
			
			return super.onTimestamp(0);
		}
	}
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		long timenew = timestamp + delay;
		if(timenew >= 0)
		{
			return super.onStreamPacket(handle, timenew, len, data);
		}
		else
		{
			errPS.println("Delay is negative and too big. Timestamps would be negative. Delay changed to "+ -timestamp+ ".");
			delayChange = -(timestamp + delay);
			delay += delayChange;
			
			return super.onStreamPacket(handle, 0, len, data);
		}
		
	}

}
