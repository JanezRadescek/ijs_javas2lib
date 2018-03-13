package filters;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import callBacks.MultiBitBuffer;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;

public class FilterSaveCSV extends Filter{

	PrintStream out;


	String[] CSVline;
	boolean body = false;

	private int maxColumns;

	private Map<Byte,StructDefinition> definitionsStruct = new HashMap<Byte,StructDefinition>();
	private Map<Byte,SensorDefinition> definitionsSensor = new HashMap<Byte,SensorDefinition>();


	private boolean dataMapping;


	/**
	 * Filter which saves as CSV. Since CSV is very restrictive only timestamps, handles and actual datas will be saved.
	 * @param directory string representing file directory AND name
	 * @param dataMapping boolean value. if true packets will be translated acording to sensor definitions.
	 */
	public FilterSaveCSV(String directory, boolean dataMapping)
	{
		this.dataMapping = dataMapping;
		try {
			this.out = new PrintStream(new FileOutputStream(directory));
			System.out.println("writing data into file " + directory);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void printLine() {
		for(int i=0;i<2+maxColumns-1;i++)
		{
			out.print(CSVline[i] + ",");
		}
		out.println(CSVline[maxColumns+2-1]);

	}


	@Override
	public boolean onEndOfFile() {
		if(!body)
		{
			body = true;
			CSVline = new String[2 + maxColumns];
			CSVline[0] = "TimeStamp";
			CSVline[1] = "Handle";
			for(int c = 2; c<maxColumns+2;c++)
			{
				CSVline[c] = "data" + (c-1);
			}
			printLine();
		}

		out.close();
		pushEndofFile();
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		if(!body)
		{
			body = true;
			CSVline = new String[2 + maxColumns];
			CSVline[0] = "TimeStamp";
			CSVline[1] = "Handle";
			for(int c = 2; c<maxColumns+2;c++)
			{
				CSVline[c] = "data" + (c-1);
			}
			printLine();
		}

		out.close();
		pushUnmarkedEndofFile();
		return false;
	}


	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		definitionsSensor.put(handle, definition);
		pushDefinition(handle, definition);
		return true;
	}


	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		//TODO should we clone definition ? perhaps we will change that down the line.
		definitionsStruct.put(handle, definition);
		int temp = definition.elementsInOrder.length();
		if(temp>maxColumns)
		{
			maxColumns = temp;
		}

		pushDefinition(handle, definition);
		return true;
	}


	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		if(!body)
		{
			body = true;
			CSVline = new String[2 + maxColumns];
			CSVline[0] = "TimeStamp";
			CSVline[1] = "Handle";
			for(int c = 2; c<maxColumns+2;c++)
			{
				CSVline[c] = "data" + (c-1);
			}
			printLine();
		}

		ArrayList<Float> sensorData = new ArrayList<>();

		//hardcoded conversion
		MultiBitBuffer mbb = new MultiBitBuffer(data);
		int mbbOffset = 0;

		for (char element : definitionsStruct.get(handle).elementsInOrder.toCharArray())
		{
			byte cb = (byte) element;
			if (definitionsSensor.get(cb) != null){
				SensorDefinition tempSensor = definitionsSensor.get(cb);
				int entitySize = tempSensor.resolution;
				//OLD CODE int entitySize = s2.getEntityHandles(cb).sensorDefinition.resolution;
				int temp = mbb.getInt(mbbOffset, entitySize);
				mbbOffset += entitySize;
				if(dataMapping){
					float k = tempSensor.k;
					float n = tempSensor.n;
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
		CSVline = new String[2 + maxColumns];
		CSVline[0] = timestamp+"";
		CSVline[1] = handle+"";
		for(int i = 0;i<maxColumns;i++)
		{
			if(i<sensorData.size())
				CSVline[2+i] = sensorData.get(i)+"";
			else
				CSVline[2+i] = "";
		}
		printLine();


		pushStremPacket(handle, timestamp, len, data);
		return true;
	}


	/**
	 * affine transformation of data and round 
	 * @param temp - raw data
	 * @param k  - multipliyer
	 * @param n - ad
	 * @return k*temp+n rounded based on k
	 */
	private float calculateANDround(int temp, float k, float n) {
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
