package suportingClasses;

public class StreamPacket extends Line {

	public byte handle;
	public int len;
	public byte[] data;
	
	public StreamPacket(byte handle, long timestamp, int len, byte[] data)
	{
		super(timestamp);
		this.handle = handle;
		this.len = len;
		this.data = data;
	}
}
