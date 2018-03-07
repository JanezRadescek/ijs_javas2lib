package callBacks;

import java.io.File;
import java.util.ArrayList;

import e6.ECG.time_sync.Signal;
import si.ijs.e6.MeasurementData.Metadata;
import si.ijs.e6.S2;

/**
 * WARNING only works for s2 PCARD with one stream
 * 
 * @author janez
 *
 */
public class FixTime extends OutS2Callback {

	double[] fixedTimestamps;
	int counter = 0;
	long timeOff;
	
	public FixTime(S2 inFile, long[] ab, boolean nonEss, long handles, byte dataT, String directory,
			boolean simpleProcessing) {
		
		super(inFile, ab, nonEss, handles, dataT, directory);
		
		Signal signal = new Signal();
		
		File in = new File(inFile.getFilePath());
		signal.readS2File(in.getParent(), in.getName(), ab[0], ab[1], 0);
		if(simpleProcessing)
		{
			signal.processSignalSimple();
		}else
		{
			signal.processSignal();
		}
		
		fixedTimestamps = signal.getNewTimeStamp();
		timeOff = getTimeOffset(signal.getMetadata());		
	}
	
	private long getTimeOffset(ArrayList<Metadata> md) {
		String sTimeStr = md.get(4).getValue();

		double hours = Double.parseDouble(sTimeStr.substring(0,2));
		double minutes = Double.parseDouble(sTimeStr.substring(3,5));
		double seconds = Double.parseDouble(sTimeStr.substring(6,8));
		double microSec = Double.parseDouble(sTimeStr.substring(9,12));
		long sTime = (long) (3600 * 1E9 * (hours + minutes/60 + seconds/3600 + microSec/3600000));

		return sTime;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		long temp = (long) fixedTimestamps[counter] - timeOff;
		counter++;
		return super.onStreamPacket(handle, temp, len, data);
	
	}
	
}
