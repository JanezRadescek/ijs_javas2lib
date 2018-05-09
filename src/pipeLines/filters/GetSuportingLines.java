package pipeLines.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

public class GetSuportingLines extends Pipe {
	
	int vsersionInt;
	String version;
	Map<String,String> meta = new HashMap<String,String>();
	ArrayList<String> specialMeta = new ArrayList<String>();
	Set<Byte> usedHandles = new HashSet<Byte>();
	
	
	public GetSuportingLines() {
		specialMeta.add("date");
		specialMeta.add("time");
		specialMeta.add("timezone");
	}
	
	@Override
	public boolean onVersion(int versionInt, String version) {
		this.vsersionInt = versionInt;
		this.version = version;
		return super.onVersion(versionInt, version);
	}
	
	@Override
	public boolean onMetadata(String key, String value) {
		if(specialMeta.contains(key))
		{
			meta.put(key, value);
		}
		return super.onMetadata(key, value);
	}
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		usedHandles.add(handle);
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		usedHandles.add(handle);
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		usedHandles.add(handle);
		return super.onDefinition(handle, definition);
	}

	

	/**
	 * @return the vsersionInt
	 */
	public int getVsersionInt() {
		return vsersionInt;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the meta
	 */
	public Map<String, String> getMeta() {
		return meta;
	}
	
	/**
	 * @return the usedHandles
	 */
	public Set<Byte> getUsedHandles() {
		return usedHandles;
	}
}
