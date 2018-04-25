package filters;

import java.util.HashMap;
import java.util.Map;

import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/**
 * changes handles. Since it must remain valid S2 values in struct definitions are also remaped accourdingly
 * @author janez
 *
 */
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
		
		return pushDefinition(remap.getOrDefault(handle, handle), definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		String elementsNew = "";
		for(char element:definition.elementsInOrder.toCharArray())
		{
			elementsNew += (char)(byte)remap.getOrDefault(element, (byte) element);
		}
		definition.elementsInOrder = elementsNew;
		
		return pushDefinition(remap.getOrDefault(handle, handle), definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		
		return pushDefinition(remap.getOrDefault(handle, handle), definition);
	}
	

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		
		return pushStremPacket(remap.getOrDefault(handle, handle), timestamp, len, data);
	}
	
}
