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
	S2.StoreStatus storeS;
	
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
		this.storeS = this.outFile.store(new File(directory), name);
		System.out.println("writing to " + directory + " " + name);
		
		this.casZacetni = ab[0];
		this.casKoncni = ab[1];
		
		//če je handles//dataT večji od 0 pomeni da želimo točno tiste handle//podatke
		//če je handles//dataT manjši od 0 pomeni da tistih nočemo
		//na mestih v binarnem zapisu kjer so enke nam predstavljajo "izbrane"
		this.handles = (handles>0) ? (this.handles & handles) : (this.handles & ~handles);
		this.citizens = (byte) ((dataT>0) ? (this.citizens & dataT) : (this.citizens & ~dataT));
		
	}
	
	/**
	 * @return true natanko tedaj, ko je zadnji prebrani čas večji od začetnega in manjši od končnega časa.
	 */
	private boolean naIntervalu() {
		return ((casZacetni<=casTrenutni) && (casTrenutni <= casKoncni));
	}

	/**
	 * če želimo komentarje jih zapiše v S2
	 * @return true
	 */
	@Override
	public boolean onComment(String comment) {
		//C = 1, zastavica za komentarje je na prvem mestu v citizens
		if ((citizens & C) != 0)
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
		if((SM & citizens) != 0)
		{
			storeS.addSpecialTextMessage((byte)who, MessageType.convert((byte)what), message, -1);
		}
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		if((MD & citizens) != 0)
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
		lasttTimeDef = definition;
		storeS.addDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		casTrenutni = nanoSecondTimestamp;
		casTimestamp = nanoSecondTimestamp;
		if(naIntervalu())
		{
			storeS.addTimestamp(new Nanoseconds(nanoSecondTimestamp));
			return true;
		}
		storeS.endFile(true);
		return false;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		casTrenutni = timestamp;
		if(naIntervalu() && ((this.handles & 1<<handle) != 0))
		{
			long relative = casTrenutni - casTimestamp;
			long formatted = (long) (relative * lasttTimeDef.multiplier);
			
			storeS.addSensorPacket(handle, formatted, data);
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
