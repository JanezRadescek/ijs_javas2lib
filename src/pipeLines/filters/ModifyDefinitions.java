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
		if(modifications.containsKey(handle) && modifications.get(handle) instanceof SensorDefinition)
		{
			SensorDefinition sd = (SensorDefinition) modifications.get(handle);
			return super.onDefinition(handle, sd);
		}
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		if(modifications.containsKey(handle) && modifications.get(handle) instanceof StructDefinition)
		{
			StructDefinition sd = (StructDefinition) modifications.get(handle);
			return super.onDefinition(handle, sd);
		}
		return super.onDefinition(handle, definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		if(modifications.containsKey(handle) && modifications.get(handle) instanceof TimestampDefinition)
		{
			TimestampDefinition sd = (TimestampDefinition) modifications.get(handle);
			return super.onDefinition(handle, sd);
		}
		return super.onDefinition(handle, definition);
	}

}
