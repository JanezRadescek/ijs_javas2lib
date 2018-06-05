package pipeLines.filters;

import java.util.HashSet;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;
import suportingClasses.SpecialMessage;

/**
 * @author janez
 * delites duplicates off version, definitions, comments and special messeges.
 */
public class Unique extends Pipe {

	boolean ver = false;
	HashSet<String> comm = new HashSet<String>();
	HashSet<SpecialMessage> special = new HashSet<SpecialMessage>();
	HashSet<String> met = new HashSet<String>();
	boolean firstEnd;
	HashSet<Byte> def = new HashSet<Byte>();

	@Override
	public boolean onVersion(int versionInt, String version) {
		if(!ver)
		{
			ver = true;
			return super.onVersion(versionInt, version);
		}else
		{
			return true;
		}
	}

	@Override
	public boolean onComment(String comment) {
		if(!comm.contains(comment))
		{
			comm.add(comment);
			return super.onComment(comment);
		}else
		{
			return true;
		}

	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		SpecialMessage tem = new SpecialMessage(who, what, message);
		if(!special.contains(tem))
		{
			special.add(tem);
			return super.onSpecialMessage(who, what, message);
		}
		else
		{
			return true;
		}	
	}

	@Override
	public boolean onMetadata(String key, String value) {
		if(!met.contains(key))
		{
			met.add(key);
			return super.onMetadata(key, value);
		}else
		{
			return true;
		}
	}

	@Override
	public boolean onEndOfFile() {
		if(firstEnd)
		{
			firstEnd = false;
			return super.onEndOfFile();
		}
		else
		{
			return true;
		}
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		if(firstEnd)
		{
			firstEnd = false;
			return super.onUnmarkedEndOfFile();
		}
		else
		{
			return true;
		}
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		if(!def.contains(handle))
		{
			def.add(handle);
			return super.onDefinition(handle, definition);
		}else
		{
			return true;
		}
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		if(!def.contains(handle))
		{
			def.add(handle);
			return super.onDefinition(handle, definition);
		}else
		{
			return true;
		}
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		if(!def.contains(handle))
		{
			def.add(handle);
			return super.onDefinition(handle, definition);
		}else
		{
			return true;
		}
	}





}
