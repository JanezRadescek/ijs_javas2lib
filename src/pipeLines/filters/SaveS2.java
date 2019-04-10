package pipeLines.filters;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import pipeLines.Pipe;
import si.ijs.e6.S2;
import si.ijs.e6.S2.MessageType;
import si.ijs.e6.S2.Nanoseconds;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StoreStatus;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/**
 * Saves data into s2 file. 
 * @author janez
 *
 */
public class SaveS2 extends Pipe {

	private S2 s2;
	StoreStatus storeS;
	private Map<Byte, TimestampDefinition> timestampDefinitions = new HashMap<Byte, TimestampDefinition>();
	private long lastTimestamp;
	private boolean lastTimestampWriten;
	private Map<Byte,Long> lastTime = new HashMap<Byte,Long>();

	/**
	 * @param outDir directory AND name of new S2 file in which we will save
	 * @param errPS {@link PrintStream} for errors
	 */
	public SaveS2(String outDir, PrintStream errPS)
	{
		File temF = new File(outDir);
		if((temF.getParentFile() != null) && !temF.getParentFile().exists())
		{
			errPS.println("Given directory " +temF.getParent() +" does not exist. Creating one");
			temF.getParentFile().mkdirs();
		}
		
		this.errPS = errPS;
		s2 = new S2();
		File f = new File(outDir);
		storeS = s2.store(f);
		storeS.enableDebugOutput(false);
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		storeS.setVersion(versionInt, version);
		
		return pushVersion(versionInt, version);
	}

	@Override
	public boolean onComment(String comment) {
		if (!lastTimestampWriten)
		{
			storeS.addTimestamp(new Nanoseconds(lastTimestamp));
			lastTimestampWriten = true;
			for(byte t:lastTime.keySet())
				lastTime.replace(t, lastTimestamp);
		}
		
		storeS.addTextMessage(comment);
		
		return pushComment(comment);
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		if (!lastTimestampWriten)
		{
			storeS.addTimestamp(new Nanoseconds(lastTimestamp));
			lastTimestampWriten = true;
			for(byte t:lastTime.keySet())
				lastTime.replace(t, lastTimestamp);
		}
		
		storeS.addSpecialTextMessage((byte)who, MessageType.convert((byte)what), message, -1);
		
		return pushSpecilaMessage(who, what, message);
	}

	@Override
	public boolean onMetadata(String key, String value) {
		storeS.addMetadata(key, value);
		
		return pushMetadata(key, value);
	}

	@Override
	public boolean onEndOfFile() {
		if (!lastTimestampWriten)
		{
			storeS.addTimestamp(new Nanoseconds(lastTimestamp));
			lastTimestampWriten = true;
			for(byte t:lastTime.keySet())
				lastTime.replace(t, lastTimestamp);
		}
		
		storeS.endFile(true);
		boolean r = pushEndofFile();
		if(storeS.getNotes().length() > 0)
		{
			errPS.println(storeS.getNotes());
		}
		return r;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		if (!lastTimestampWriten)
		{
			storeS.addTimestamp(new Nanoseconds(lastTimestamp));
			lastTimestampWriten = true;
			for(byte t:lastTime.keySet())
				lastTime.replace(t, lastTimestamp);
		}
		
		storeS.endFile(true);
		boolean r = pushUnmarkedEndofFile();
		if(storeS.getNotes().length() > 0)
		{
			errPS.println(storeS.getNotes());
		}
		return r;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		storeS.addDefinition(handle, definition);
		
		return pushDefinition(handle, definition);
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		lastTime.put(handle, 0L);
		
		storeS.addDefinition(handle, definition);
		
		return pushDefinition(handle, definition);
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		timestampDefinitions.put(handle, definition);

		storeS.addDefinition(handle, definition);
		
		return pushDefinition(handle, definition);
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		lastTimestamp = nanoSecondTimestamp;
		lastTimestampWriten = false;

		
		return pushTimestamp(nanoSecondTimestamp);
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
			//TODO lasttimestamp -> timestamp
			storeS.addTimestamp(new Nanoseconds(timestamp));
			for(byte t:lastTime.keySet())
			{
				lastTime.replace(t, timestamp);
			}
			writeReadyDiff = 0;
		}
		storeS.addSensorPacket(handle, writeReadyDiff, data);
		long reducedPrecisionTimestamp = lastTime.get(handle) + timestampDefinitions.get(handle).getNanoMultiplier() * writeReadyDiff;
		lastTime.replace(handle, reducedPrecisionTimestamp);
		//lastTime.replace(handle, timestamp);
		return pushStreamPacket(handle, timestamp, len, data);
	}
	
	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		if (!lastTimestampWriten)
		{
			storeS.addTimestamp(new Nanoseconds(lastTimestamp));
			lastTimestampWriten = true;
			for(byte t:lastTime.keySet())
				lastTime.replace(t, lastTimestamp);
		}
		
		storeS.writeLine(type, data);
		
		return super.onUnknownLineType(type, len, data);
	}


}
