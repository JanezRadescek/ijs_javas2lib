package callBacks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import s2.S2;
import s2.S2.MessageType;
import s2.S2.Nanoseconds;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StoreStatus;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;


/**
 * Callback for writing part of source S2 file to new S2 file
 * @author janez
 *
 */
public class OutS2Callback implements ReadLineCallbackInterface {
	
	final int VERSION = 1;
	
	final int C = 0b1;
	final int SM = 0b10;
	final int MD = 0b100;
	//final int sensorD = 0b1000;
	//final int structD = 0b10000;
	//final int timeD = 0b100000;
		
	S2 inFile;
	S2 outFile;
	StoreStatus storeS;
	
	long a;
	long b;
	boolean nonEssTiming;
	//for calculating offset in packetstreams
	Map<Byte,Long> lastTime = new HashMap<Byte,Long>();
	long lastTimestamp = 0;
	boolean lastTimestampWriten = true;
	//for data without time, last time we got from anywhere.
	long apriximateTime = 0;
	

	public Map<Byte, TimestampDefinition> timestampDefinitions = new HashMap<Byte, TimestampDefinition>();
	
	long handles = Long.MAX_VALUE;
	byte wantedData = 0b111;
	
	/**
	 * Creates callback for writing part of source S2 file to new S2 file
	 * @param inFile - source S2 file
	 * @param ab - interval of time [a,b)
	 * @param handles - handles of streams we want to keep/let go.
	 * @param dataT - data types we want to keep/let go.
	 * @param directory - directory of new S2 file
	 * @param name - name of new S2 file
	 */
	public OutS2Callback(S2 inFile, long [] ab, boolean nonEss, long handles, byte dataT, String directory, String name) {
		this.inFile = inFile;
		this.outFile = new S2();
		this.storeS = this.outFile.store(new File(directory), name);
		System.out.println("writing to " + directory + File.separator + name);
		
		this.a = ab[0];
		this.b = ab[1];
		this.nonEssTiming = nonEss; 
		
		//algebraic additive inverse is not equal to binary additive inverse
		//na mestih v binarnem zapisu kjer so enke nam predstavljajo "izbrane"
		//this.handles = (handles>=0) ? (this.handles & handles) : (this.handles & handles--);
		//this.wantedData = (byte) ((dataT>=0) ? (this.wantedData & dataT) : (this.wantedData & dataT--));
		this.handles &= handles;
		this.wantedData &= dataT;
		if((this.wantedData&0b100) == 0)
		{
			this.wantedData |= 0b100;
			System.err.println("S2 file curently needs metadata. -d set to number " + this.wantedData);
		}
		
	}
	
	
	/**
	 * @param timestamp 
	 * @return timeStart<=timeCurent<=timeEnd
	 */
	private boolean naIntervalu(long timestamp) {
		return ((a<=timestamp) && (timestamp < b));
	}

	
	@Override
	public boolean onComment(String comment) {
		//C = 1, zastavica za komentarje je na prvem mestu v citizens
		if ((!nonEssTiming || a<=apriximateTime) && (wantedData & C) != 0)
		{
			storeS.addTextMessage(comment);
		}
		return true;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		if(versionInt > VERSION){
			System.err.println("Version of source S2 is higher than what this can read");}
		storeS.setVersion(versionInt, version);
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if((!nonEssTiming || a<=apriximateTime) && (wantedData & SM) != 0)
		{
			storeS.addSpecialTextMessage((byte)who, MessageType.convert((byte)what), message, -1);
		}
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		if((wantedData & MD) != 0)
		{
			storeS.addMetadata(key, value);
		}
		return true;
	}

	@Override
	public boolean onEndOfFile() {
		storeS.endFile(true);
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		storeS.endFile(true);
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		storeS.addDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		storeS.addDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		timestampDefinitions.put(handle, definition);
		storeS.addDefinition(handle, definition);
		lastTime.put(handle, (long) 0);
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		if(nanoSecondTimestamp<b)
		{
			lastTimestamp = nanoSecondTimestamp;
			lastTimestampWriten = false;
			apriximateTime = nanoSecondTimestamp;
			return true;
		}else
		{
			storeS.endFile(true);
			return false;
		}
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		apriximateTime = timestamp;
		if(naIntervalu(timestamp) && ((this.handles & 1<<handle) != 0))
		{
			if (!lastTimestampWriten)
			{
				storeS.addTimestamp(new Nanoseconds(lastTimestamp));
				lastTimestampWriten = true;
				for(byte t:lastTime.keySet())
					lastTime.replace(t, lastTimestamp);
			}
			
			int maxBits = timestampDefinitions.get(handle).byteSize * 8;
			long diff = timestamp - lastTime.get(handle);
			long writeReadyDiff = timestampDefinitions.get(handle).toImplementationFormat(new Nanoseconds(diff));
			//time stampe iz prvotne ohranjamo.
			if(64 - Long.numberOfLeadingZeros(writeReadyDiff) > maxBits)
			{
				storeS.addTimestamp(new Nanoseconds(timestamp));
				writeReadyDiff = 0;
			}
			storeS.addSensorPacket(handle, writeReadyDiff, data);
			lastTime.replace(handle, timestamp);
			return true;
		}
		if(timestamp<b)
		{
			return true;
		}
		else
		{
			storeS.endFile(true);
			return false;
		}
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		return true;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		return true;
	}

}
