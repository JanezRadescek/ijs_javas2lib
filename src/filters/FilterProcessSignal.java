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

	private final long defaultLength = ((long)1E9) * 60L * 3L;  //3 min
	//expected number of packets in block
	private final double vseh = 125 * defaultLength/(1E9);
	//for calculating new Slopes
	private final double weight = 0.7;
	
	//If less than that we merge previous and curent block
	private final double premalo=0.1 * vseh;

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


	//written variabls
	private double writtenMark = 0;
	private double writtenSlope = 0;
	private double writtenIntercept = 0;
	private boolean curentBad = true;
	//previous variabls
	private boolean previousTooBig = true;
	private double previousMark = 0;
	private int previousMerges = 0;
	//curent variabls
	private double curentMark = 0;
	private long curentBlockStartTime = 0;


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
				//if previous is to small we merge it with curent
				if(previousBlockC.size() <= premalo)
				{
					previousTooBig = false;

					previousBlockC.addAll(curentBlockC);
					previousBlockP.addAll(curentBlockP);
					curentBlockC = new ArrayList<Integer>();
					curentBlockP = new ArrayList<StreamPacket>();
					previousMerges++;
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
							previousMerges++;
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

		//Vseh je samo ocena zato se lahko zgodi da dobimo več paketov kot je ocenjeno
		previousMark = n/((1+previousMerges)*vseh);
		
		double tempMark1 = writtenMark;
		double tempMark2 = previousMark;
		tempMark1 *= (1-weight);
		tempMark2 *= weight;
		
		double norm = tempMark1 + tempMark2; 
		tempMark1 /= norm;
		tempMark2 /= norm;


		double[] timePrevious = new double[n];
		double[] counterPrevious = new double[n];
		for(int i=0; i<n; i++)
		{
			timePrevious[i] = previousBlockP.get(i).timestamp;
			counterPrevious[i] = previousBlockC.get(i);
		}
		//TODO popravt linearregresion v long verzijo.
		LinearRegression linePrevious = new LinearRegression(timePrevious, counterPrevious, noInterations);


		int m = curentBlockC.size();
		double[] timeCurent = new double[m];
		double[] counterCurent = new double[m];
		for(int i=0; i<m; i++)
		{
			timeCurent[i] = curentBlockP.get(i).timestamp;
			counterCurent[i] = curentBlockC.get(i);
		}
		//TODO popravt linearregresion v long verzijo.
		LinearRegression lineCurent = new LinearRegression(timeCurent, counterCurent, noInterations);
		curentMark = m/vseh;

		double tempMark3 = writtenMark;
		double tempMark4 = previousMark;
		tempMark3 *= (1-weight);
		tempMark4 *= weight;
		
		norm = tempMark3 + tempMark4; 
		tempMark3 /= norm;
		tempMark4 /= norm;
		
		
		//Calculate new intercept and slope to make whole thing continious
		/*we assume already written intervals are written as good as they can be.
		 *Therefore we calculate left part based on them except if we know
		 * that the best we could is still bad
		 *Right part is calculated based on which one is good.if both are good/bad we make average 
		 */
		double x1,x2,y1,y2;
		x1 = timePrevious[0];
		x2 = timePrevious[n-1];


		y1 = tempMark1 * (writtenSlope * x1 + writtenIntercept) 
				+ tempMark2 * (linePrevious.slope() * x1 + linePrevious.intercept());
		

		y2 = tempMark3 * (linePrevious.slope() * x2 + linePrevious.intercept()) 
				+ tempMark4 * (lineCurent.slope() * x2 + lineCurent.intercept());

		//OLD code to realize how stupy old one is
		/*
		if((previousBad & curentBad) | (!previousBad & !curentBad))
		{
			y2 = (     (linePrevious.slope() * x2 + linePrevious.intercept()) + 
					(lineCurent.slope() * x2 + lineCurent.intercept())     )/2;
		}
		else
		{
			if(previousBad)
			{
				//previous bad curent good
				y2 = lineCurent.slope() * x2 + lineCurent.intercept();
			}
			else
			{
				//previous good curent bad
				y2 = linePrevious.slope() * x2 + linePrevious.intercept();
			}

		}*/

		//Old way
		/*
		if(writtenBad)
		{
			//int index = line.sequenceNumber();
			int index = 0;
			intercept = previousBlockC.get(index) - slope * previousBlockP.get(index).timestamp;
		}else
		{
			intercept = writtenC - slope * writtenT;
		}
		 */


		double slope = (y2 - y1) / (x2 - x1);
		double intercept = y2 - slope * x2;

		for(int i=0; i<n; i++)
		{
			int tempC = previousBlockC.get(i);
			StreamPacket tempP = previousBlockP.get(i);

			long timestampNew = (long) ((tempC - intercept)/slope);
			pushStremPacket(tempP.handle, timestampNew, tempP.len, tempP.data);

		}
		writtenSlope = slope;
		writtenIntercept = intercept;


		previousBlockC = curentBlockC;
		previousBlockP = curentBlockP;
		curentBlockC = new ArrayList<Integer>();
		curentBlockP = new ArrayList<StreamPacket>();

		writtenMark = previousMark;
		previousTooBig = false;
		previousMerges = 0;
	}

}
