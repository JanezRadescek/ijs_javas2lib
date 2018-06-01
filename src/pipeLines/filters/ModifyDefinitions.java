package pipeLines.filters;

import java.io.PrintStream;
import java.util.Map;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

public class ModifyDefinitions extends Pipe {
	
	Map modifications;
	
	public ModifyDefinitions(Map modifications, PrintStream errPS) {
		this.modifications = modifications;
		this.errPS = errPS;
	}
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		return super.onDefinition(handle, (SensorDefinition) modifications.getOrDefault(handle, definition));
	}
	
	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		return super.onDefinition(handle, (StructDefinition) modifications.getOrDefault(handle, definition));
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		return super.onDefinition(handle, (TimestampDefinition) modifications.getOrDefault(handle, definition));
	}

}
