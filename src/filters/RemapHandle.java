package filters;

import java.util.HashMap;
import java.util.Map;

import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

public class RemapHandle extends Filter {

	private Map<Byte,Byte> remap;

	
	/**
	 * @param remap keys are old handles, values are new handles
	 */
	public RemapHandle(Map<Byte, Byte> remap) {
		this.remap = remap;
	}
	
	public RemapHandle(byte handleOld, byte handleNew) {
		this.remap = new HashMap<Byte,Byte>();
		remap.put(handleOld, handleNew);
	}
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		pushDefinition(remap.getOrDefault(handle, handle), definition);
		return true;
	}
	
	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		String elementsNew = "";
		for(char element:definition.elementsInOrder.toCharArray())
		{
			elementsNew += (char)(byte)remap.getOrDefault(handle, handle);
		}
		definition.elementsInOrder = elementsNew;
		pushDefinition(remap.getOrDefault(handle, handle), definition);
		return true;
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		pushDefinition(remap.getOrDefault(handle, handle), definition);
		return true;
	}
	

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		pushStremPacket(remap.getOrDefault(handle, handle), timestamp, len, data);
		return true;
	}
	
}
