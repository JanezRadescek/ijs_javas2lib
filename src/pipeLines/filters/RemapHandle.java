package pipeLines.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import si.ijs.e6.S2.TimestampDefinition;

/**
 * changes handles. Since it must remain valid S2 values in struct definitions are also remaped accourdingly
 * @author janez
 *
 */
public class RemapHandle extends Pipe {

	private Map<Byte,Byte> remap;
	private ArrayList<Byte> reservedHandles = new ArrayList<Byte>();
	
	private Map<Byte,StructDefinition> oldDefinitions = new HashMap<Byte,StructDefinition>();
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
	
	
	public void setRemap(Map<Byte, Byte> remap)
	{
		this.remap = remap;
	}
	
	public void addRemap(Byte oldValue, Byte newValue)
	{
		remap.put(oldValue, newValue);
	}
	
	//SETTERS
	
	public void setReserved(ArrayList<Byte> rh)
	{
		reservedHandles = rh;
	}
	
	//OVERRIDES
	
	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		if(!remap.containsKey(handle))
		{
			findNewRemap128(handle);
		}
		//TODO NEDELA PROV 1
		return pushDefinition(remap.get(handle), definition);
	}
	

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		
		oldDefinitions.put(handle, definition);
		//TODO en onDefinition vrne false kje bomo to reportal če si jih shranjujemo ? M
		
		String elementsNew = "";
		for(char element:definition.elementsInOrder.toCharArray())
		{
			elementsNew += (char)(byte)remap.getOrDefault(element, (byte) element);
		}
		definition.elementsInOrder = elementsNew;
		//TODO NEDELA PROV 2
		if(!remap.containsKey(handle))
		{
			findNewRemap32(handle);
		}
		return pushDefinition(remap.get(handle), definition);
	}
	
	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		//TODO NEDELA PROV 3
		if(!remap.containsKey(handle))
		{
			findNewRemap32(handle);
		}
		return pushDefinition(remap.get(handle), definition);
	}
	

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		if(!remap.containsKey(handle))
		{
			findNewRemap32(handle);
		}
		return pushStremPacket(remap.get(handle), timestamp, len, data);
	}
	
	
	//GETERS
	
	
	public Map<Byte,Byte> getRemap()
	{
		return this.remap;
	}
	
	
	// PRIVATTE METHODS
	
	private void findNewRemap32(byte handle) {
		
		for(byte i =0; i<32;i++)
		{
			if(!reservedHandles.contains(i))
			{
				reservedHandles.add(i);
				remap.put(handle, i);
				return;
			}
		}
		errors += "We are out of handles. Handle "+handle+" will get default handle 0";
		remap.put(handle, (byte) 0);
		
	}
	
	private void findNewRemap128(byte handle) {
		for(byte i =32; i<128;i++)
		{
			if(!reservedHandles.contains(i))
			{
				reservedHandles.add(i);
				remap.put(handle, i);
				return;
			}
		}
		errors += "We are out of handles. Handle "+handle+" will get default handle 32";
		remap.put(handle, (byte) 32);

		
	}
}
