package pipeLines.enzymes;

import pipeLines.Pipe;
import si.ijs.e6.S2.StructDefinition;

public class LockHandles extends Lock {

	byte theHandle;

	/**
	 * Pipe key will be applied only on streampackets with given handle
	 * @param key
	 * @param handle
	 */
	public LockHandles(Pipe key, byte handle) {
		super(key);
		theHandle = handle;
	}

	//TODO very dangerous class to use
	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		if(handle == theHandle)
		{
			return pipeKey.onDefinition(handle, definition);
		}else
		{
			return super.onDefinition(handle, definition);
		}
	}



	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		if(handle == theHandle)
		{
			return pipeKey.onStreamPacket(handle, timestamp, len, data);
		}else
		{
			return pushStreamPacket(handle, timestamp, len, data);
		}
	}

}
