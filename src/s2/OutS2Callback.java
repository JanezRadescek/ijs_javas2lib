package s2;

import java.io.File;
import s2.S2.MessageType;
import s2.S2.Nanoseconds;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;

/**na željenem intervalu filtriramo vrstice glede na podatke/messege/meta ... in zapišemo v s2**/
public class OutS2Callback implements ReadLineCallbackInterface {
	
	final int C = 0b1;
	final int SM = 0b10;
	final int MD = 0b100;
	final int sensorD = 0b1000;
	final int structD = 0b10000;
	final int timeD = 0b100000;
		
	S2 inFile;
	S2 outFile;
	S2.StoreStatus ss;
	
	long casZacetni;
	long casKoncni;
	long casTrenutni = (long) 0;
	long casTimestamp = (long) 0;
	
	TimestampDefinition lasttTimeDef = null;
	
	
	long handles = Long.MAX_VALUE;
	byte citizens = Byte.MAX_VALUE;
	

	public OutS2Callback(S2 file, long [] ab, long handles, byte dataT, String directory, String name) {
		this.inFile = file;
		this.outFile = new S2();
		this.ss = this.outFile.store(new File(directory), name);
		System.out.println("writing to " + directory + " " + name);
		
		this.casZacetni = ab[0];
		this.casKoncni = ab[1];
		
		this.handles = (handles>0) ? (this.handles & handles) : (this.handles & ~handles);
		this.citizens = (byte) ((dataT>0) ? (this.citizens & dataT) : (this.citizens & ~dataT));
		
	}
	
	private boolean naIntervalu() {
		return ((casZacetni<=casTrenutni) && (casTrenutni <= casKoncni));
	}

	@Override
	public boolean onComment(String comment) {
		if ((C & citizens) != 0)
		{
			ss.addTextMessage(comment);
		}
		return true;
	}


	@Override
	public boolean onVersion(int versionInt, String version) {
		ss.setVersion(versionInt, version);
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if((SM & citizens) != 0)
		{
			ss.addSpecialTextMessage((byte)who, MessageType.convert((byte)what), message, -1);
		}
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		if((MD & citizens) != 0)
		{
			ss.addMetadata(key, value);
		}
		return true;
	}

	@Override
	public boolean onEndOfFile() {
		ss.endFile(true);
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		ss.endFile(true);
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		ss.addDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		ss.addDefinition(handle, definition);  
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		lasttTimeDef = definition;
		ss.addDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		casTrenutni = nanoSecondTimestamp;
		casTimestamp = nanoSecondTimestamp;
		if(naIntervalu())
		{
			ss.addTimestamp(new Nanoseconds(nanoSecondTimestamp));
			return true;
		}
		ss.endFile(true);
		return false;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		casTrenutni = timestamp;
		if(naIntervalu() && ((this.handles & 1<<handle) != 0))
		{
			long relative = casTrenutni - casTimestamp;
			long formatted = (long) (relative * lasttTimeDef.multiplier);
			
			ss.addSensorPacket(handle, formatted, data);
			return true;
		}
		ss.endFile();
		return false;
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
