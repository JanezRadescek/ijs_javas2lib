package pipeLines.conglomerates;

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
	
	//TODO little smarter filter
	public Merge(Pipe primaryInput, Pipe secondaryInput)
	{
		primaryInput.addChild(this);
		secondaryInput.addChild(this);
	}
	
	@Override
	public boolean onVersion(int versionInt, String version) {
		if(versionInt == -1)
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
				errors += "Merging files do not have same versions. Version 1 = "+this.version+" Version 2 = "+version;
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
}
