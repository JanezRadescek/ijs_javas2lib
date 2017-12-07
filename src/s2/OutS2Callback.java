package s2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
	S2.StoreStatus storeS;
	
	long casZacetni;
	long casKoncni;
	long timePrevious = (long) 0;
	long timeCurent = (long) 0;
	
	TimestampDefinition lastTimeDef = null;
	public Map<Byte, TimestampDefinition> timestampDefinitions = new HashMap<Byte, TimestampDefinition>();
	
	long handles = Long.MAX_VALUE;
	byte wantedData = Byte.MAX_VALUE;
	

	public OutS2Callback(S2 file, long [] ab, long handles, byte dataT, String directory, String name) {
		this.inFile = file;
		this.outFile = new S2();
		this.storeS = this.outFile.store(new File(directory), name);
		System.out.println("writing to " + directory + " " + name);
		
		this.casZacetni = ab[0];
		this.casKoncni = ab[1];
		
		//če je handles//dataT večji od 0 pomeni da želimo točno tiste handle//podatke
		//če je handles//dataT manjši od 0 pomeni da tistih nočemo
		//na mestih v binarnem zapisu kjer so enke nam predstavljajo "izbrane"
		this.handles = (handles>0) ? (this.handles & handles) : (this.handles & ~handles);
		this.wantedData = (byte) ((dataT>0) ? (this.wantedData & dataT) : (this.wantedData & ~dataT));
		
	}
	
	/**
	 * @return true natanko tedaj, ko je zadnji prebrani čas večji od začetnega in manjši od končnega časa.
	 */
	private boolean naIntervalu() {
		return ((casZacetni<=timeCurent) && (timeCurent <= casKoncni));
	}

	/**
	 * če želimo komentarje jih zapiše v S2
	 * @return true
	 */
	@Override
	public boolean onComment(String comment) {
		//C = 1, zastavica za komentarje je na prvem mestu v citizens
		if ((wantedData & C) != 0)
		{
			storeS.addTextMessage(comment);
		}
		return true;
	}


	@Override
	public boolean onVersion(int versionInt, String version) {
		if(versionInt > 1){
			System.err.println("Version of S2 is higher than what this can read");}
		storeS.setVersion(versionInt, version);
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if((wantedData & SM) != 0)
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
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		timeCurent = nanoSecondTimestamp;
		if(naIntervalu())
		{
			storeS.addTimestamp(new Nanoseconds(nanoSecondTimestamp));
			timePrevious = nanoSecondTimestamp;
			return true;
		}
		storeS.endFile(true);
		return false;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		timeCurent = timestamp;
		if(naIntervalu() && ((this.handles & 1<<handle) != 0))
		{
			long relative = timestamp - timePrevious;
			long formatted = (long) (timestampDefinitions.get(handle).toImplementationFormat(
					new Nanoseconds(relative)) );
			// *,/ timestampDefinitions.get(handle).multiplier
			
			storeS.addSensorPacket(handle, formatted, data);
			timePrevious = timestamp;
			return true;
		}
		storeS.endFile();
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
