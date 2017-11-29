package s2;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;

public class OutputCallback implements  ReadLineCallbackInterface {

	S2 s2;
	long a;
	long b;
	PrintStream out;
	boolean dataMapping = true;
	byte theHandle;

	//kako nastaviti lasttime? lahko dobimo prvi paket brez časa??
	long lastTime = 0;
	

	public OutputCallback(S2 s2, long a, long b, byte theHandle) {
		this.s2 = s2;
		this.a = a;
		this.b = b;
		this.theHandle = theHandle;
		this.out = System.out;
		
	}
	
	public OutputCallback(S2 s2, long a, long b, byte theHandle, String directory, String name, String extension)
	{
		this.s2 = s2;
		this.a = a;
		this.b = b;
		this.theHandle = theHandle;
		
		if (extension.equals("csv"))
		{
			try {
				this.out = new CsvStream(new FileOutputStream(directory+"\\"+name));
				System.out.println("writing data into file " + directory+"\\"+name);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (extension.equals("txt"))
		{
			try {
				this.out = new PrintStream(new FileOutputStream(directory+name));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		char[] zaporedje = definition.elementsInOrder.toCharArray();
		
		this.out.print("TimeStamp");
		for(char s:zaporedje)
		{
			this.out.print(s+"");
		}
		this.out.println("Handle");
		
		
		
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		lastTime = nanoSecondTimestamp;
		return true;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		lastTime = timestamp;
		
		if((a<= lastTime && lastTime <= b) && (this.theHandle == handle))
		{
			ArrayList<Float> sensorData = new ArrayList<>();
			
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
			
			out.print(timestamp+"");
			for(Float tdata : sensorData){
				out.print(tdata+"");
			}
			out.println(handle);
			
			return true;
		}
		return false;
	}

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
		if(a<= lastTime && lastTime <= b)
		{
			// TODO Auto-generated method stub
			return true;
		}
		else
			return false;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		if(a<= lastTime && lastTime <= b)
		{
			return true;
		}
		else
			return false;
	}

}
