package filters;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import si.ijs.e6.S2;
import si.ijs.e6.S2.MessageType;
import si.ijs.e6.S2.Nanoseconds;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StoreStatus;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

public class FilterSaveS2 extends Filter {
	
	private S2 s2;
	StoreStatus storeS;
	private Map<Byte, TimestampDefinition> timestampDefinitions = new HashMap<Byte, TimestampDefinition>();
	private long lastTimestamp;
	private boolean lastTimestampWriten;
	private Map<Byte,Long> lastTime = new HashMap<Byte,Long>();

	/**
	 * @param directory directory AND name of new S2 file in which we will save
	 */
	public FilterSaveS2(String directory)
	{
		s2 = new S2();
		//TODO zakaj ne moremo S2.store/load dati
		File f = new File(directory);
		storeS = s2.store(f.getParentFile(), f.getName());
	}
	
	
	@Override
	public boolean onVersion(int versionInt, String version) {
		storeS.setVersion(versionInt, version);
		pushVersion(versionInt, version);
		return true;
	}
	
	@Override
	public boolean onComment(String comment) {
		//TODO baje obstaja addComment. trenutno maš addTextmessage
		storeS.addTextMessage(comment);
		pushComment(comment);
		return true;
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		storeS.addSpecialTextMessage((byte)who, MessageType.convert((byte)what), message, -1);
		pushSpecilaMessage(who, what, message);
		return true;
	}
	
	@Override
	public boolean onMetadata(String key, String value) {
		storeS.addMetadata(key, value);
		pushMetadata(key, value);
		return true;
	}
	
	@Override
	public boolean onEndOfFile() {
		storeS.endFile(true);
		pushEndofFile();
		return false;
	}
	
	@Override
	public boolean onUnmarkedEndOfFile() {
		storeS.endFile(true);
		pushUnmarkedEndofFile();
		return false;
	}
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		storeS.addDefinition(handle, definition);
		pushDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		storeS.addDefinition(handle, definition);
		pushDefinition(handle, definition);
		return true;
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		timestampDefinitions.put(handle, definition);
		
		storeS.addDefinition(handle, definition);
		pushDefinition(handle, definition);
		return true;
	}
	
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		lastTimestamp = nanoSecondTimestamp;
		lastTimestampWriten = false;
		
		pushTimestamp(nanoSecondTimestamp);
		return true;
	}
	
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
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
		if(64 - Long.numberOfLeadingZeros(writeReadyDiff) > maxBits)
		{
			storeS.addTimestamp(new Nanoseconds(timestamp));
			writeReadyDiff = 0;
		}
		storeS.addSensorPacket(handle, writeReadyDiff, data);
		lastTime.replace(handle, timestamp);
		
		pushStremPacket(handle, timestamp, len, data);
		return true;
	}
	

}
