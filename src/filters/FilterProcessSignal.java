package filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import filters.FilterGetLines.StreamPacket;
import si.ijs.e6.MultiBitBuffer;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;

public class FilterProcessSignal extends Filter {

	private static long nanosInHour = ((long)1E9) * 60L * 60L;

	private long intervalsLength;

	private long intervalStart = 0;

	private Map<Byte,StructDefinition> mapStruct= new HashMap<Byte,StructDefinition>();
	private Map<Byte,SensorDefinition> mapSensor= new HashMap<Byte,SensorDefinition>();

	private Map<Integer,StreamPacket> mapPacket = new HashMap<Integer,StreamPacket>();
	private int numOverFlovs = 0;
	private int lastC = -1;


	public FilterProcessSignal(long intervalsLength) 
	{
		this.intervalsLength = intervalsLength;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		if(!version.equals("PCARD"))
			System.err.print("We are processing S2 file which is not PCARD");
		pushVersion(versionInt, version);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		mapSensor.put(handle, definition);

		pushDefinition(handle, definition);
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		mapStruct.put(handle, definition);

		pushDefinition(handle, definition);
		return true;
	}



	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		//TODO preveri če je razmik vredu, preveri nov pcardtimesync
		if(timestamp < intervalStart + intervalsLength)
		{
			int c = convertPacket(handle, data);
			if(c>=0)
			{
				c += numOverFlovs*1024;
				if(c<lastC)
				{
					c += 1024;
					numOverFlovs++;
				}


				lastC = c;
				mapPacket.put(c, new StreamPacket(handle,timestamp,len,data));
			}
		}

		calculateInterval();
		return true;
	}




	private int convertPacket(byte handle, byte[] data) {
		MultiBitBuffer mbb = new MultiBitBuffer(data);

		int mbbOffset = 0;
		// sample counter is still half-way hardcoded into stream reading

		for (int i = 0; i < mapStruct.get(handle).elementsInOrder.length(); ++i) 
		{
			byte cb = (byte) mapStruct.get(handle).elementsInOrder.charAt(i);

			if ((cb == 'c') && (mapSensor.get(cb) != null)) 
			{
				int entitySize = mapSensor.get(cb).resolution;
				int temp = mbb.getInt(mbbOffset, entitySize);
				mbbOffset += entitySize;

				return (int) (temp * mapSensor.get(cb).k + mapSensor.get(cb).n);

			} 
		}

		return -1;

	}

	private void calculateInterval()
	{
		/*
		calc

		pushInterval
		 */
	}

}
