package s2;

import java.io.File;
import java.util.HashMap;

import s2.S2.MessageType;
import s2.S2.Nanoseconds;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;

public class OutS2Callback implements ReadLineCallbackInterface {
	
	S2 inFile;
	S2 outFile;
	S2.StoreStatus ss;
	
	long casZacetni;
	long casKoncni;
	Long casTrenutni = (long) 0;
	
	TimestampDefinition lasttTimeDef = null;
	Long casTimestamp = (long) 0;
	
	HashMap<String, Boolean> filter = new HashMap<String, Boolean>();
	HashMap<Byte, Boolean> handles = new HashMap<Byte, Boolean>();
	

	public OutS2Callback(S2 file, long a, long b, byte theHandle, String directory, String name) {
		this.inFile = file;
		this.outFile = new S2();
		this.ss = this.outFile.store(new File(directory), name);
		System.out.println("writing to " + directory + " " + name);
		
		this.casZacetni = a;
		this.casKoncni = b;
		
		
		//zapolnemo za testiranje
		this.filter.put("c", true);
		filter.put("sm", true);
		filter.put("md", true);
		filter.put("sensorD", true);
		filter.put("structD", true);
		filter.put("timeD", true);
		
		handles.put((byte)0, true);
	}
	
	private boolean naIntervalu() {
		return ((casZacetni<=casTrenutni) && (casTrenutni <= casKoncni));
	}

	@Override
	public boolean onComment(String comment) {
		if (filter.get("c"))
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
		if(filter.get("sm"))
		{
			ss.addSpecialTextMessage((byte)who, MessageType.convert((byte)what), message, -1);
		}
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		if(filter.get("md"))
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
		//če je začetna pokvarjena jo pustimo pokvarjeno ??
		ss.endFile(true);
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		boolean gandalf = (filter.get("sensorD") && handles.containsKey(handle));
		if(gandalf)
		{
			ss.addDefinition(handle, definition);
		}
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		/*if(filter.get("structD") && handles.containsKey(handle))
		{
			ss.addDefinition(handle, definition);
		}*/     
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		if(filter.get("timeD") && handles.containsKey(handle))
		{
			lasttTimeDef = definition;
			ss.addDefinition(handle, definition);
		}
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		//se lahko zgodi da nočmo timestampa ???
		casTrenutni = nanoSecondTimestamp;
		casTimestamp = nanoSecondTimestamp;
		if(naIntervalu())
		{
			ss.addTimestamp(new Nanoseconds(nanoSecondTimestamp));
			return true;
		}
		return false;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		//se lahko zgodi da nočmo podatkov ??
		casTrenutni = timestamp;
		if(naIntervalu())
		{
			long relative = casTrenutni - casTimestamp;
			long formatted = (long) (relative * lasttTimeDef.multiplier);
			
			ss.addSensorPacket(handle, formatted, data);
			return true;
		}
		return false;
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		if(naIntervalu())
		{
			// pass ???
			
			return true;
		}
		return false;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		// TODO Auto-generated method stub
		return false;
	}

}
