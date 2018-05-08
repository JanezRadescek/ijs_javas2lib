package pipeLines.filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

public class GetUsedHandles extends Pipe {
	
	Set<Byte> usedHandles = new HashSet<Byte>();
	
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
	 * @return the usedHandles
	 */
	public Set<Byte> getUsedHandles() {
		return usedHandles;
	}
	
	
	

}
