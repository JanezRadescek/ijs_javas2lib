
package callBacks;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import s2.S2;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;


/**
 * Callback for writing in human-readable data
 * @author janez
 *
 */
public class OutCSVCallback implements  ReadLineCallbackInterface {

	//S2 file we are reading from
	S2 s2;
	//user
	long a;
	long b;
	PrintStream out;
	boolean dataMapping = true;
	long theHandle;

	long lastTime = 0;
	

	/**
	 * Creates callback for writing human-readable data in CSV
	 * @param s2 - S2 file we are reading from 
	 * @param ab - interval for which we write data
	 * @param handle - number of stream we will write 
	 * (if handle has more than one 1 in binary it may cause incorect output) 
	 */
	public OutCSVCallback(S2 s2, long[] ab, long handle) {
		this.s2 = s2;
		this.a = ab[0];
		this.b = ab[1];
		this.theHandle = handle;
		if ((handle & (--handle)) != 0)
			System.out.println("Handle is not power off 2");
		this.out = System.out;
		
	}
	
	/**
	 * Creates callback for writing human-readable data in CSV
	 * @param s2 - S2 file we are reading from 
	 * @param ab - interval for which we write data
	 * @param handles - handle of stream we will write. Handle = 2^(wanted stream) ,stream = log2(handle)
	 * If we want i-stream than on i+1 position in binary form of handles must be 1.
	 * Example wanted stream = 0, corect handle = 1; wanted stream 4, corect handle = 16(10) = 10000(2)
	 * @param directory - directory of output file
	 * @param name - name of output file
	 */
	public OutCSVCallback(S2 s2, long[] ab, long handle, String directory, String name)
	{
		this(s2, ab, handle);
		try {
			this.out = new CsvStream(new FileOutputStream(directory+"\\"+name));
			System.out.println("writing data into file " + directory+"\\"+name);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onComment(String comment) {
		return true;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		return true;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		return true;
	}

	@Override
	public boolean onEndOfFile() {
		out.close();
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		out.close();
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		//writes "data format"
		if((theHandle & (1<<handle)) != 0)
		{
			char[] zaporedje = definition.elementsInOrder.toCharArray();
			this.out.print("TimeStamp");
			for(char s:zaporedje)
			{
				this.out.print(s+"");
			}
			this.out.println("Handle");
		}
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		if(nanoSecondTimestamp<b)
			return true;
		else
		{
			out.close();
			return false;
		}
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		lastTime = timestamp;
		if((a<= lastTime && lastTime < b) && ((theHandle & (1<<handle)) != 0))
		{
			//converted data
			ArrayList<Float> sensorData = new ArrayList<>();
			
			//hardcoded conversion
			MultiBitBuffer mbb = new MultiBitBuffer(data);
			int mbbOffset = 0;
			
			for (int i = 0; i < s2.getEntityHandles(handle).elementsInOrder.length(); ++i)
			{
				byte cb = (byte) s2.getEntityHandles(handle).elementsInOrder.charAt(i);
				if (s2.getEntityHandles(cb).sensorDefinition != null){
	                int entitySize = s2.getEntityHandles(cb).sensorDefinition.resolution;
	                int temp = mbb.getInt(mbbOffset, entitySize);
	                mbbOffset += entitySize;
	                if(dataMapping){
	                	float k = s2.getEntityHandles(cb).sensorDefinition.k;
	                	float n = s2.getEntityHandles(cb).sensorDefinition.n;
	                	float t = calculateANDround(temp,k,n);
	                	sensorData.add(t);
	                }else{
	                	sensorData.add((float) temp);
	                }
	                
				}else{
					System.out.println("Measurement data encountered invalid sensor: " + (int) (cb));
				}
			}
			//writing
			out.print(timestamp+"");
			for(Float tdata : sensorData){
				out.print(tdata+"");
			}
			out.println(handle);
			
			return true;
		}
		if(lastTime < b)
			return true;
		else
		{
			out.close();
			return false;
		}
			
	}

	/**
	 * affine transformation of data and round 
	 * @param temp - raw data
	 * @param k  - multipliyer
	 * @param n - ad
	 * @return k*temp+n rounded based on k
	 */
	private float calculateANDround(int temp, float k, float n) {
		float r = k*temp + n;
		int dec = 0;
		while(k<1)
		{
			k *= 10;
			dec++;
		}
		int zaokr = (int) Math.pow(10, dec);
		r = (float)Math.round(r * zaokr)/zaokr;
		
		return r;
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		return true;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		return true;
	}

}
