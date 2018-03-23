package filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import e6.ECG.time_sync.LinearRegression;
import filters.FilterGetLines.StreamPacket;
import si.ijs.e6.MultiBitBuffer;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;

/**
 * Linear regresion is performed locally and then timestamps of packets are changed appropriatly. 
 * Due to time changes old timestamp are removed and added new one where needed.
 * @author janez
 *
 */
public class FilterProcessSignal extends Filter {

	private final long defaultLength = ((long)1E9) * 60L * 3L;
	private final int premalo = 20;
	
	//TODO calibrate this for better results
	private final long intervalLength;
	private final int noInterations;
	

	//needed to get caunters 
	private Map<Byte,StructDefinition> mapStruct= new HashMap<Byte,StructDefinition>();
	private Map<Byte,SensorDefinition> mapSensor= new HashMap<Byte,SensorDefinition>();

	//previous is wating to see what will happen with curent
	private ArrayList<StreamPacket> previousBlockP = new ArrayList<StreamPacket>();
	private ArrayList<Integer> previousBlockC = new ArrayList<Integer>();
	//curent is getting filled
	private ArrayList<StreamPacket> curentBlockP = new ArrayList<StreamPacket>();
	private ArrayList<Integer> curentBlockC = new ArrayList<Integer>();

	//decinding how to make intervals
	private long curentBlockStartTime = 0;
	private boolean previousTooBig = true;
	private boolean previousBad = true;
	
	//decing how to calculte intercept
	private boolean writtenBad = true;
	private int writtenC = -1;
	private long writtenT = -1;

	//for calculating caunters
	private int numOverFlovs = 0;
	private int lastC = -1;


	public FilterProcessSignal() 
	{
		this.intervalLength = defaultLength;
		this.noInterations = 5;
	}
	
	public FilterProcessSignal(long intervalsLength, int noIterations) 
	{
		this.intervalLength = intervalsLength;
		this.noInterations = noIterations;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		if(!version.equals("PCARD"))
			System.err.print("We are processing S2 file which is not PCARD");
		pushVersion(versionInt, version);
		return true;
	}


	@Override
	public boolean onEndOfFile() {
		previousBlockC.addAll(curentBlockC);
		previousBlockP.addAll(curentBlockP);
		procesOldInterval();

		pushEndofFile();
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		previousBlockC.addAll(curentBlockC);
		previousBlockP.addAll(curentBlockP);
		procesOldInterval();

		pushUnmarkedEndofFile();
		return false;
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
		
		//we get the counter
		int c = convertPacket(handle, data);
		//če je popravi števec
		if(c>=0)
		{
			c += numOverFlovs*1024;
			if(c<lastC)
			{
				c += 1024;
				numOverFlovs++;
			}
			lastC = c;

			//TODO bi bilo boljše če bi gledali število paketov namesto pretečen čas ?
			//TODO kombinacija ?? tko kot zdej + če je dovolj paketov kr nared
			//če smo prečkali meje drugega intervala
			//curentBlockP.get(0).timestamp
			if(timestamp > curentBlockStartTime + intervalLength)
			{
				//if previous is to small we add 
				if(previousBlockC.size() <= premalo)
				{
					if(curentBlockC.size() <= premalo)
					{
						previousTooBig = false;
						previousBad = true;
					}else
					{
						previousTooBig = false;
						previousBad = false;
					}

					previousBlockC.addAll(curentBlockC);
					previousBlockP.addAll(curentBlockP);
					curentBlockC = new ArrayList<Integer>();
					curentBlockP = new ArrayList<StreamPacket>();
					curentBlockStartTime = timestamp;
				}
				else
				{
					if(previousTooBig)
					{
						procesOldInterval();
						curentBlockStartTime = timestamp;
					}
					else
					{
						if(curentBlockC.size() > premalo)
						{
							//tole bi se moral dogajat najpogosteje vse ostalo so samo robni primeri.
							procesOldInterval();
							curentBlockStartTime = timestamp;
						}
						else
						{
							previousBlockC.addAll(curentBlockC);
							previousBlockP.addAll(curentBlockP);
							curentBlockC = new ArrayList<Integer>();
							curentBlockP = new ArrayList<StreamPacket>();
							curentBlockStartTime = timestamp;
							previousTooBig = true;
							previousBad = false;
						}
					}
				}

			}
			//shrani trenutni paket
			curentBlockC.add(c);
			curentBlockP.add(new StreamPacket(handle,timestamp,len,data));

		}
		else //DOBIL smo paket z pokvarjenim/brez števca zato se pretvarjamo da ga je zgobil wifi :D. 
		{
			//TODO lahko bi mu dal prejšn števec + 14 ? pa čeprov morda ni ?
		}

		return true;
	}


	/**
	 * @param handle
	 * @param data
	 * @return counter written in data
	 */
	private int convertPacket(byte handle, byte[] data) {
		MultiBitBuffer mbb = new MultiBitBuffer(data);

		int mbbOffset = 0;
		// sample counter is still half-way hardcoded into stream reading

		for (int i = 0; i < mapStruct.get(handle).elementsInOrder.length(); ++i) 
		{
			byte cb = (byte) mapStruct.get(handle).elementsInOrder.charAt(i);
			int entitySize = mapSensor.get(cb).getResolution();
			int temp = mbb.getInt(mbbOffset, entitySize);
			mbbOffset += entitySize;
			
			if ((cb == 'c') && (mapSensor.get(cb) != null)) 
			{
				

				return (int) (temp * mapSensor.get(cb).k + mapSensor.get(cb).n);

			} 
		}

		return -1;
	}



	/**
	 * Linear regresion is performed on previous block, timestamps corrected acourding to results and passed further.
	 */
	private void procesOldInterval()
	{
		int n = previousBlockC.size();
		double[] time = new double[n];
		double[] counter = new double[n];
		for(int i=0; i<n; i++)
		{
			time[i] = previousBlockP.get(i).timestamp;
			counter[i] = previousBlockC.get(i);
		}

		//TODO popravt linearregresion v long verzijo.
		LinearRegression line = new LinearRegression(time, counter, noInterations);

		double slope = line.slope();
		double intercept;
		
		if(writtenBad)
		{
			//int index = line.sequenceNumber();
			int index = 0;
			intercept = previousBlockC.get(index) - slope * previousBlockP.get(index).timestamp;
		}else
		{
			intercept = writtenC - slope * writtenT;
		}
		
		for(int i=0; i<n; i++)
		{
			int tempC = previousBlockC.get(i);
			StreamPacket tempP = previousBlockP.get(i);
			
			long timestampNew = (long) ((tempC - intercept)/slope);
			pushStremPacket(tempP.handle, timestampNew, tempP.len, tempP.data);
		}
		

		previousBlockC = curentBlockC;
		previousBlockP = curentBlockP;
		curentBlockC = new ArrayList<Integer>();
		curentBlockP = new ArrayList<StreamPacket>();

		writtenBad = previousBad;
		previousTooBig = false;
		previousBad = false;
	}

}
