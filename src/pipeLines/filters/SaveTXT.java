package pipeLines.filters;

import java.io.PrintStream;
import java.util.Arrays;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

public class SaveTXT extends Pipe {

	PrintStream txt;
	
	int counterC = 0;
	int counterSM = 0;
	int counterM = 0;
	int counterT = 0;
	int counterP = 0;

	public SaveTXT(PrintStream txt, PrintStream errPS) {
		this.txt = txt;
		this.errPS = errPS;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		txt.println("Version :  " + versionInt + "-" + version);
		return super.onVersion(versionInt, version);
	}
	
	@Override
	public boolean onComment(String comment) {

		txt.println("Comment num. "+counterC + " :  " + comment);
		counterC++;
		return super.onComment(comment);
	}
	
	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		txt.println("Special message num. "+counterSM + " :  who=" + who + ", what=" + what + ", message="+ message);
		counterSM++;
		return super.onSpecialMessage(who, what, message);
	}
	
	@Override
	public boolean onMetadata(String key, String value) {
		txt.println("Metadata num. "+ counterM +" :  key=" + key + ", value="+value);
		counterM++;
		return super.onMetadata(key, value);
	}
	
	@Override
	public boolean onEndOfFile() {
		txt.println("EndOfFile");
		return super.onEndOfFile();
	}
	
	@Override
	public boolean onUnmarkedEndOfFile() {
		txt.println("UnmarkedEndOfFile");
		return super.onUnmarkedEndOfFile();
	}
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		txt.println("Sensor definition :  handle=" + handle + ", name=" + definition.name + ", unit="+definition.unit + ", resolution="+ definition.resolution
				+", scalarBitPadding=" + definition.scalarBitPadding+ ", valueType=" + definition.valueType + ", absoluteId="+ definition.absoluteId
				+", vectorSize=" + definition.vectorSize + ", vectorBitPadding=" + definition.vectorBitPadding + ", samplingFrequency=" + definition.samplingFrequency
				+", k="+ definition.k + ", n=" + definition.n);
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		txt.println("Struct definition :  handle=" + handle + ", name=" + definition.name + ", elementsInOrder=\""+ definition.elementsInOrder+"\"");
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		txt.println("Timestamp definition :  handle=" + handle + ", absoluteId=" + definition.absoluteId + ", byteSize=" + definition.byteSize + ", multiplier=" + definition.multiplier);
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		txt.println("Timestam num. "+ counterT+" :  time=" + nanoSecondTimestamp);
		counterT++;
		return super.onTimestamp(nanoSecondTimestamp);
	}
	
	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		txt.println("Stream Packet num. " + counterP + " :  handle=" + handle + ", timestamp="+ timestamp + ", data="+ Arrays.toString(data));
		return super.onStreamPacket(handle, timestamp, len, data);
	}
	
	@Override
	public boolean onError(int lineNum, String error) {
		txt.println("Error in line "+ lineNum + " :  "+ error);
		return super.onError(lineNum, error);
	}
	
	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		txt.println("Unknown line type :  type=" + type +", len="+ len + ", data=" + Arrays.toString(data));
		return super.onUnknownLineType(type, len, data);
	}
}
