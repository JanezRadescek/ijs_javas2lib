package callBacks;

import java.util.LinkedList;
import java.util.Queue;

import callBacks.FirtstReader.StreamPacket;
import si.ijs.e6.S2;
import si.ijs.e6.S2.ReadLineCallbackInterface;

public abstract class ProcessingCallback extends OutS2Callback implements ReadLineCallbackInterface {

	//highest suported S2 version
	final int VERSION = 1;
	
	Queue<StreamPacket> lastData = new LinkedList<StreamPacket>(); 
	
	public ProcessingCallback(S2 inFile, String directory)
	{
		super(inFile, new long[]{0,Long.MAX_VALUE}, false, Long.MAX_VALUE, (byte) 0b111, directory);
	}
	
	@Override
	public boolean onVersion(int versionInt, String version) {
		if(versionInt > VERSION){
			System.err.println("Version of source S2 is higher than what this can read");}
		
		if(correspond(version))
		{
			storeS.setVersion(versionInt, version);
			return true;
		}
		else
		{
			System.err.println("Read S2 version doesnt correspond with given transformation");
			return false;
		}
		
		
	}

	abstract boolean correspond(String version);
	
	@Override
	public abstract boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data);
	
	
}
