package pipeLines.filters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

public class SaveTXT extends Pipe {

	PrintStream txt;
	boolean close = false;//do we close this stream?
	
	int counterC = 0;
	int counterSM = 0;
	int counterM = 0;
	int counterT = 0;
	int counterP = 0;
	
	String format = "%.2fs %23s";	
	
	long lastTime = 0;

	public SaveTXT(PrintStream txt, PrintStream errPS) {
		this.txt = txt;
		this.errPS = errPS;
	}
	
	public SaveTXT(String outDir, PrintStream errPS) {
		File temF = new File(outDir);
		if(!temF.getParentFile().exists())
		{
			errPS.println("Given directory " +temF.getParent() +" does not exist. Creating one");
			temF.getParentFile().mkdirs();
		}
		
		try {
			this.txt = new PrintStream(new FileOutputStream(new File(outDir)));
			close = true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.errPS = errPS;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		printOnTXT("Version : ", versionInt + "-" + version);
		return super.onVersion(versionInt, version);
	}
	
	@Override
	public boolean onComment(String comment) 
	{
		printOnTXT("Comment : ", comment);
		return super.onComment(comment);
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		printOnTXT("Special message : ", "who=" + who + ", what=" + what + ", message="+ message);
		counterSM++;
		return super.onSpecialMessage(who, what, message);
	}
	
	@Override
	public boolean onMetadata(String key, String value) {
		printOnTXT("Metadata : ", "key=" + key + ", value="+value);
		counterM++;
		return super.onMetadata(key, value);
	}
	
	@Override
	public boolean onEndOfFile() {
		txt.println("EndOfFile");
		if(close) txt.close();
		return super.onEndOfFile();
	}
	
	@Override
	public boolean onUnmarkedEndOfFile() {
		txt.println("UnmarkedEndOfFile");
		if(close) txt.close();
		return super.onUnmarkedEndOfFile();
	}
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		printOnTXT("Sensor definition : ", "handle=" + handle + ", name=" + definition.name + ", unit="+definition.unit + ", resolution="+ definition.resolution
				+", scalarBitPadding=" + definition.scalarBitPadding+ ", valueType=" + definition.valueType + ", absoluteId="+ definition.absoluteId
				+", vectorSize=" + definition.vectorSize + ", vectorBitPadding=" + definition.vectorBitPadding + ", samplingFrequency=" + definition.samplingFrequency
				+", k="+ definition.k + ", n=" + definition.n);
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		printOnTXT("Struct definition : ", "handle=" + handle + ", name=" + definition.name + ", elementsInOrder=\""+ definition.elementsInOrder+"\"");
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		printOnTXT("Timestamp definition : ", "handle=" + handle + ", absoluteId=" + definition.absoluteId + ", byteSize=" + definition.byteSize + ", multiplier=" + definition.multiplier);
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		lastTime = nanoSecondTimestamp;
		printOnTXT("Timestamp : ", "time=" + nanoSecondTimestamp);
		counterT++;
		return super.onTimestamp(nanoSecondTimestamp);
	}
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		lastTime = timestamp;
		printOnTXT("Stream Packet : ", "handle=" + handle + ", timestamp="+ timestamp + ", bytes="+ Arrays.toString(data));
		return super.onStreamPacket(handle, timestamp, len, data);
	}
	
	@Override
	public boolean onError(int lineNum, String error) {
		printOnTXT("Error : ", error);
		return super.onError(lineNum, error);
	}
	
	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		printOnTXT("Unknown line type : ", "type=" + type +", len="+ len + ", bytes=" + Arrays.toString(data));
		return super.onUnknownLineType(type, len, data);
	}
	
	private void printOnTXT(String a, String b)
	{
		txt.printf(Locale.US, format, lastTime/1e9, a);
		txt.println(b);
	}
}
