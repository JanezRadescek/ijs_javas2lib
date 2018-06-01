package pipeLines.conglomerates;

import java.io.PrintStream;
import java.util.ArrayList;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/**
 * Imlementation of merge for merge redy input strems. This means it doesnt changes data/lines. It only discard duplicated lines which are not allowed to be duplicated (version,definitions)
 * @author janez
 *
 */
public class Merge extends Pipe{

	int versionInt = -1;
	String version = "";

	ArrayList<String> writtenMeta = new ArrayList<String>();
	ArrayList<Byte> writtenSensor = new ArrayList<Byte>();
	ArrayList<Byte> writtenStruct = new ArrayList<Byte>();
	ArrayList<Byte> writtenTime = new ArrayList<Byte>();
	boolean FirstEnd = true;
	boolean endWritten = false;


	public Merge(Pipe primaryInput, Pipe secondaryInput, PrintStream errPS)
	{
		this.errPS = errPS;

		primaryInput.addChild(this);
		secondaryInput.addChild(this);
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		if(this.versionInt == -1)
		{
			this.versionInt = versionInt;
			this.version = version;
			return super.onVersion(versionInt, version);
		}else
		{
			if(this.version.equals(version))
			{
				return true;
			}
			else
			{
				errPS.println("Merging files do not have same versions. Primary version is : "+this.version+" Secondary version is : "+version);
				return false;
			}
		}
	}


	@Override
	public boolean onMetadata(String key, String value) {
		if(writtenMeta.contains(key))
		{
			return true;
		}else
		{
			writtenMeta.add(key);
			return super.onMetadata(key, value);
		}

	}

	@Override
	public boolean onEndOfFile() {
		if(FirstEnd)
		{
			FirstEnd = false;
			return true;
		}
		else
		{
			if(!endWritten)
			{
				endWritten = true;
				return super.onEndOfFile();
			}else
			{
				errPS.println("We got too many file end in merge");
				return false;
			}
		}
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		if(FirstEnd)
		{
			FirstEnd = false;
			return true;
		}
		else
		{
			if(!endWritten)
			{
				endWritten = true;
				return super.onUnmarkedEndOfFile();
			}else
			{
				errPS.println("We got too many file end in merge");
				return false;
			}
		}
	}


	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		if(writtenSensor.contains(handle))
		{
			return true;
		}else
		{
			writtenSensor.add(handle);
			return super.onDefinition(handle, definition);
		}
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		if(writtenStruct.contains(handle))
		{
			return true;
		}else
		{
			writtenStruct.add(handle);
			return super.onDefinition(handle, definition);
		}
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		if(writtenTime.contains(handle))
		{
			return true;
		}else
		{
			writtenTime.add(handle);
			return super.onDefinition(handle, definition);
		}
	}


	//FOR debuging purposes only
	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {

		return super.onTimestamp(nanoSecondTimestamp);
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {

		return super.onStreamPacket(handle, timestamp, len, data);
	}

}
