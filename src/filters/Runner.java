package filters;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;

import callBacks.MultiBitBuffer;
import e6.ECG.time_sync.Signal;
import filters.FilterGetLines.StreamPacket;
import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;
import si.ijs.e6.S2.SensorDefinition;

/**
 * Meant for analizing FilterProcessSignal. can be called from matlab/octave.
 * @author janez
 *
 */
public class Runner {

	private final double startTime = 0*60;
	private final double endTime = 0.5*60*60;
	private final double EXPECTED_MAXIMUM = 0.2;
	private final double EXPECTED_TIME_DIFF = 8*1E6;

	//C:\Users\janez\workspace\S2_rw\S2files\andrej1.s2  moja datoteka ki jo hočem popraviti
	File dir = new File("C:\\Users\\janez\\workspace\\S2_rw\\S2files");

	ArrayList<Double> samplesTime;
	ArrayList<Double> packetsTime;
	ArrayList<Double> packetsCounter;
	ArrayList<Float> samplesVoltage;
	ArrayList<Integer> samplesPeak;

	ArrayList<Double> samplesTime1;
	ArrayList<Integer> samplesPeak1;
	ArrayList<Float> samplesVoltage1;

	public static void main(String[] args)
	{
		String ime = "dolge1.s2";

		Runner r = new Runner();
		System.out.println("BLALA");
		//r.setOldTVP(ime);
		r.setNewTVP(ime);
		//r.saveTVP(ime);
		//r.setAndrejTVP(ime);

	}



	public double unitRun(String dire, String ime1, String ime2)
	{
		this.dir = new File(dire);
		setOldTVP(ime1);

		samplesPeak1 = samplesPeak;
		samplesTime1 = samplesTime;
		samplesVoltage1 = samplesVoltage;

		setOldTVP(ime2);
		return metrics();
	}




	public void saveTVP(String ime)
	{


		samplesTime = new ArrayList<Double>();
		samplesVoltage = new ArrayList<Float>();


		System.out.println("save START");

		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		FilterProcessSignal f1 = new FilterProcessSignal();
		//FilterSaveS2 f2 = new FilterSaveS2(out);
		FilterSaveCSV f2 = new FilterSaveCSV("C:\\Users\\janez\\workspace\\S2_rw\\UnitTests\\andrej11.csv", true);
		f0.addChild(f1);
		f1.addChild(f2);

		System.out.println("save zgleda vredu : " + ls.readLines(f0, false));
		System.out.println(s2.getNotes());

	}


	public void setOldTVP(String ime) {
		//IN
		//C:\Users\janez\workspace\S2_rw\S2files\andrej1.s2  moja datoteka ki jo hočem popraviti


		samplesTime = new ArrayList<Double>();
		samplesVoltage = new ArrayList<Float>();
		packetsTime = new ArrayList<Double>();
		packetsCounter = new ArrayList<Double>();

		System.out.println("old START");
		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		//FilterProcessSignal f1 = new FilterProcessSignal();
		FilterGetLines f2 = new FilterGetLines();

		f0.addChild(f2);
		//f1.addChild(f2);

		System.out.println("old zgleda vredu : " + ls.readLines(f0, false));
		System.out.println("Notes : " + s2.getNotes());

		Queue<StreamPacket> tpackets = f2.getPacketQ();
		Map<Byte, SensorDefinition> tsensors = f2.getSensorDefinitions();

		int Cbase = 0;
		double Cprevious = 0f;
		double previousT = 0;

		for(StreamPacket pack:tpackets)
		{
			ArrayList<Float> sensorData = new ArrayList<>();
			MultiBitBuffer mbb = new MultiBitBuffer(pack.data);
			int mbbOffset = 0;
			for (int i = 0; i<15; i++)
			{

				byte cb = (byte) 'e';

				if(i==14)
					cb = 'c';

				SensorDefinition tempSensor = tsensors.get(cb);
				int entitySize = tempSensor.getResolution();
				int temp = mbb.getInt(mbbOffset, entitySize);
				mbbOffset += entitySize;

				float k = tempSensor.k;
				float n = tempSensor.n;
				float t = k*temp + n;
				sensorData.add(t);
			}

			double Ctemp = sensorData.get(14) + Cbase;
			if(Cprevious> Ctemp)
			{
				Ctemp += 1024;
				Cbase += 1024;
			}

			double timeDiff = pack.timestamp - previousT;
			double cDiff = Ctemp - Cprevious;

			double k = cDiff/timeDiff;
			double n = Ctemp - k * pack.timestamp;

			for (int i = 0; i<14; i++)
			{
				previousT = (Ctemp - 13 + i - n) /k;
				samplesTime.add(previousT);
				samplesVoltage.add(sensorData.get(i));
			}
			packetsTime.add((double) pack.timestamp);
			packetsCounter.add(Ctemp);
			Cprevious = Ctemp;

		}

		tpackets = null;

		checkPositivefunction();

		removeDCoffset();
		peakSearch();
		System.out.println("old DONE" + "\n");


	}

	public void setNewTVP(String ime) {

		samplesTime = new ArrayList<Double>();
		samplesVoltage = new ArrayList<Float>();
		packetsTime = new ArrayList<Double>();
		packetsCounter = new ArrayList<Double>();

		System.out.println("runner START");
		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		FilterProcessSignal f1 = new FilterProcessSignal();
		FilterGetLines f2 = new FilterGetLines();

		f0.addChild(f1);
		f1.addChild(f2);

		System.out.println("runner zgleda vredu : " + ls.readLines(f0, false));
		System.out.println("Notes : " + s2.getNotes());

		Queue<StreamPacket> tpackets = f2.getPacketQ();
		Map<Byte, SensorDefinition> tsensors = f2.getSensorDefinitions();

		int Cbase = 0;
		double Cprevious = 0f;
		double previousT = 0;

		for(StreamPacket pack:tpackets)
		{
			ArrayList<Float> sensorData = new ArrayList<>();
			MultiBitBuffer mbb = new MultiBitBuffer(pack.data);
			int mbbOffset = 0;
			for (int i = 0; i<15; i++)
			{

				byte cb = (byte) 'e';

				if(i==14)
					cb = 'c';

				SensorDefinition tempSensor = tsensors.get(cb);
				int entitySize = tempSensor.getResolution();
				int temp = mbb.getInt(mbbOffset, entitySize);
				mbbOffset += entitySize;

				float k = tempSensor.k;
				float n = tempSensor.n;
				float t = k*temp + n;
				sensorData.add(t);
			}

			double Ctemp = sensorData.get(14) + Cbase;
			if(Cprevious> Ctemp)
			{
				Ctemp += 1024;
				Cbase += 1024;
			}

			double timeDiff = pack.timestamp - previousT;
			double cDiff = Ctemp - Cprevious;

			double k = cDiff/timeDiff;
			double n = Ctemp - k * pack.timestamp;

			for (int i = 0; i<14; i++)
			{
				previousT = (Ctemp - 13 + i - n) /k;
				samplesTime.add(previousT);
				samplesVoltage.add(sensorData.get(i));
			}
			packetsTime.add((double) pack.timestamp);
			packetsCounter.add(Ctemp);
			Cprevious = Ctemp;

		}

		tpackets = null;

		checkPositivefunction();

		removeDCoffset();
		peakSearch();
		System.out.println("runner DONE" + "\n");

	}

	public void setAndrejTVP(String ime) {

		samplesTime = new ArrayList<Double>();
		samplesVoltage = new ArrayList<Float>();
		packetsTime = new ArrayList<Double>();
		packetsCounter = new ArrayList<Double>();

		System.out.println("runner START");



		System.out.println(ime);

		Signal s = new Signal();
		s.setIntervalLength(3);
		s.readS2File(dir.getAbsolutePath(), ime, 0, (long) (endTime*1e9), 0);
		s.processSignal();

		double[] tempT = s.getSamplesTimeStamp();
		for(int i =0;i<tempT.length;i++)
		{
			samplesTime.add(tempT[i]);
		}
		float[] tempV = s.getVoltage();
		for(int i =0;i<tempV.length;i++)
		{
			samplesVoltage.add(tempV[i]);
		}
		double[] tempTP = s.getNewTimeStamp();
		for(int i =0;i<tempTP.length;i++)
		{
			packetsTime.add(tempTP[i]);
		}
		double[] tempC = s.getCounter();
		for(int i =0;i<tempC.length;i++)
		{
			packetsCounter.add(tempC[i]);
		}
		int[] tempP = s.getPeaks();
		for(int i =0;i<tempP.length;i++)
		{
			samplesPeak.add(tempP[i]);
		}	
	}


	/**
	 * Check if i+1 timestamp is bigger than i
	 */
	private void checkPositivefunction() {
		ArrayList<Double> nazaj = new ArrayList<Double>();
		ArrayList<Integer> konstanta = new ArrayList<Integer>();
		for(int i =0;i<samplesTime.size()-1;i++)
		{
			if(samplesTime.get(i)>samplesTime.get(i+1))
			{
				nazaj.add(samplesTime.get(i));
			}
			if(samplesTime.get(i)==samplesTime.get(i+1))
			{
				konstanta.add(i);
			}
		}

		if(nazaj.size()>0 || konstanta.size()>0)
		{
			System.out.println("skokov nazaj " + nazaj.size());
			if(nazaj.size()<15)
			{
				System.out.println("skoki " + nazaj);
			}
			System.out.println("konstant " + konstanta.size());
			if(konstanta.size()<15)
			{
				System.out.println("konstanta " + konstanta);
			}
			System.out.println("vzorcev " + samplesTime.size());
			System.out.println("skokov/vzorcev = " + ((float)nazaj.size()/samplesTime.size()));
			System.out.println("1/14 " + ((float)1/14));

		}else
			System.out.println("vsi timestampi so naprej");

	}


	/**
	 * Removes DC offset from the signal voltage.
	 */
	private void removeDCoffset() {

		double sum = 0;
		for (double v : samplesVoltage) sum += v;
		float average = (float) (sum / samplesVoltage.size());

		for (int i = 0; i < samplesVoltage.size(); i++)
			samplesVoltage.set(i, samplesVoltage.get(i)-average);
	}



	/**
	 * Searches local peaks.
	 */
	private void peakSearch() {

		ArrayList<Integer> peak_list = new ArrayList<Integer>();
		for (int i = 2; i < samplesVoltage.size() - 3; i++) {
			if (samplesVoltage.get(i-2) < samplesVoltage.get(i-1) && samplesVoltage.get(i-1) < samplesVoltage.get(i)
					&& samplesVoltage.get(i) > samplesVoltage.get(i+1) && samplesVoltage.get(i+1) > samplesVoltage.get(i+2)) 
			{
				double dif = samplesTime.get(i+1) - samplesTime.get(i-1);
				if (samplesVoltage.get(i) > EXPECTED_MAXIMUM / 1.5 && samplesVoltage.get(i) < EXPECTED_MAXIMUM * 1.5
						&& dif < EXPECTED_TIME_DIFF*2*1.5) 
				{
					peak_list.add(i);
					//                    System.out.println("i, LEFT, left, peak, right, RIGHT =" + i + " " + (double)voltage[i-2] + " " + (double)voltage[i-1] + " " + (double)voltage[i] + " " + (double)voltage[i+1] + " " + (double)voltage[i+2]);
				}
			}
		}


		samplesPeak = peak_list;

	}



	private double metrics()
	{
		int p2 = 0;
		int p4 = 0;
		double vsota = 0;
		while(p2<=samplesPeak1.size() && p4 <= samplesPeak.size())
		{
			double aa = samplesTime.get(samplesPeak.get(p4)) - samplesTime1.get(samplesPeak1.get(p2));
			if(Math.abs(aa) < 2* 1E7)
			{
				double alpha41 = samplesVoltage.get(p4-1);
				double alpha42 = samplesVoltage.get(p4);
				double alpha43 = samplesVoltage.get(p4+1);
				double p44 = 1/2 * (alpha41 -alpha43)/(alpha41 - 2*alpha42 +alpha43);
				double t4 = samplesTime.get(samplesPeak.get(p4)-1) + (samplesTime.get(samplesPeak.get(p4)) - samplesTime.get(samplesPeak.get(p4)-1)) * (1 + p44);
				
				double alpha21 = samplesVoltage.get(p4-1);
				double alpha22 = samplesVoltage.get(p4);
				double alpha23 = samplesVoltage.get(p4+1);
				double p22 = 1/2 * (alpha21 -alpha23)/(alpha21 - 2*alpha22 +alpha23);
				double t2 = samplesTime1.get(samplesPeak1.get(p2)-1) + (samplesTime1.get(samplesPeak1.get(p2)) - samplesTime1.get(samplesPeak1.get(p2)-1)) * (1 + p22);
				
				vsota += Math.abs(t4-t2);
				
				p2++;
				p4++;
			}
			else
			{
				if(aa>0)
					p2++;
				else
					p4++;
			}
		}


		return vsota;
	}


	/**
	 * @return the samplesT
	 */
	public double[] getSamplesTimeStamp() {
		double[] R = new double[samplesTime.size()];
		for(int j =0; j<R.length; j++)
			R[j] = samplesTime.get(j);
		return R;
	}

	/**
	 * @return the samplesV
	 */
	public float[] getVoltage() {
		float[] R = new float[samplesVoltage.size()];
		for(int j =0; j<R.length; j++)
			R[j] = samplesVoltage.get(j);
		return R;
	}

	public int[] getPeaks() {
		int[] R = new int[samplesPeak.size()];
		for(int j =0; j<R.length; j++)
			R[j] = samplesPeak.get(j);
		return R;
	}

	/**
	 * @return the paketsT
	 */
	public double[] getPacketsTimeStamp() {
		double[] R = new double[packetsTime.size()];
		for(int j =0; j<R.length; j++)
			R[j] = packetsTime.get(j);
		return R;
	}

	/**
	 * @return the paketsT
	 */
	public double[] getPacketsCounter() {
		double[] R = new double[packetsCounter.size()];
		for(int j =0; j<R.length; j++)
			R[j] = packetsCounter.get(j);
		return R;
	}

}
