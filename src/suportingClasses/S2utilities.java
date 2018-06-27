package suportingClasses;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;

import callBacks.MultiBitBuffer;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;

public class S2utilities {

	public static ArrayList<Float> decodeData(StructDefinition sd, Map<Byte,SensorDefinition> m, byte[] data, PrintStream errPS)
	{
		ArrayList<Float> sensorData = new ArrayList<>();
		//hardcoded conversion
		MultiBitBuffer mbb = new MultiBitBuffer(data);
		int mbbOffset = 0;
		for (char element : sd.elementsInOrder.toCharArray())
		{
			byte cb = (byte) element;
			if (m.get(cb) != null){
				SensorDefinition tempSensor = m.get(cb);
				int entitySize = tempSensor.resolution;
				//OLD CODE int entitySize = s2.getEntityHandles(cb).sensorDefinition.resolution;
				int temp = mbb.getInt(mbbOffset, entitySize);
				mbbOffset += entitySize;

				float k = tempSensor.k;
				float n = tempSensor.n;
				float t = calculateANDround(temp,k,n);
				sensorData.add(t);


			}else{
				errPS.println("Measurement data encountered invalid sensor: " + (int) (cb));
			}
		}
		return sensorData;
	}
	
	/**
	 * affine transformation of data and round 
	 * @param temp - raw data
	 * @param k  - multipliyer
	 * @param n - ad
	 * @return k*temp+n rounded based on k
	 */
	private static float calculateANDround(int temp, float k, float n) {
		if(k == 0)
		{
			System.err.println("There is k = 0 in file");
			return 0;
		}
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


}
