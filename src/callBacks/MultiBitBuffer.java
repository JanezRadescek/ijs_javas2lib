package callBacks;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MultiBitBuffer {
	ByteBuffer bb;
	public MultiBitBuffer(byte[] data) {
		bb = ByteBuffer.wrap(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);
	}

	public short getShort(int offset, int bits) {
		return (short)getInt(offset, bits);
	}

	// get the number of bits from the buffer, starting at offset bits (0-based count)
	public int getInt(int offset, int bits) {
		int byteOffset = offset >> 3;
		int bitOffset = offset & 7;
		int countBits = 0;

		int ret = 0;
		while (countBits < bits) {
			// readyBits: bits that can be from the byte at the current offset
			int readyBits = 8 - bitOffset;
			// copy all remaining bits in the byte?
			if (bits - countBits >= readyBits) {
				int getBits = 0;
				try
				{
					getBits = (bb.get(byteOffset) & 0xFF) >>> bitOffset;
				}catch(Exception e)
				{
					e.printStackTrace();
				}
				ret += (getBits << countBits);
				countBits += readyBits;
				byteOffset++;
				bitOffset = 0;
			} else {
				readyBits = (bits - countBits);
				int getBits = (((bb.get(byteOffset) & 0xFF) >>> bitOffset) & ((1 << readyBits) - 1));
				ret += (getBits << countBits);
				countBits += readyBits;
				break;
			}
		}
		return ret;
	}

	public void setInts(int val, int offset, int bits, int num) {
		int byteOffset = offset >> 3;
				int bitOffset = offset & 7;

				for (int n = 0; n < num; ++n) {
					int bitsPut = 0;
					int putVal = val;
					bb.put(byteOffset, (byte)((bb.get(byteOffset) & ((1 << bitOffset)-1)) + (putVal << bitOffset)));
					bitsPut += (8-bitOffset);
					++byteOffset;
					putVal = putVal >> (8 - bitOffset);

					int remainingBits = bits - bitsPut;
					while (remainingBits > 0) {
						bitOffset = 0;
						bb.put(byteOffset, (byte)((bb.get(byteOffset) & (0xFF << remainingBits)) + putVal));
						bitsPut += 8;
						remainingBits = bits - bitsPut;
						putVal = putVal >> 8;
						if (remainingBits < 0) {
							bitOffset = 8 + remainingBits;
						} else
							++byteOffset;
					}
				}
	}

	public ByteBuffer getBytes()
	{
		return this.bb;
	}
}
