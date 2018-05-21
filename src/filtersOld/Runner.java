package filtersOld;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;

import callBacks.MultiBitBuffer;
import e6.ECG.time_sync.Signal;
import pipeLines.filters.GetLines;
import pipeLines.filters.SaveCSV;
import pipeLines.filters.FilterTime;
import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;
import si.ijs.e6.S2.SensorDefinition;
import suportingClasses.StreamPacket;

/**
 * Meant for analizing FilterProcessSignal. can be called from matlab/octave.
 * @author janez
 *
 */
public class Runner {

	private final double startTime = 0*60;
	private final double endTime = 1*60*60;
	private final double EXPECTED_MAXIMUM = 0.2;
	private final double EXPECTED_TIME_DIFF = 8*1E6;

	//C:\Users\janez\workspace\S2_rw\S2files\andrej1.s2  moja datoteka ki jo hočem popraviti
	File dir = new File("C:\\Users\\janez\\workspace\\S2_rw\\S2files");

	ArrayList<Double> samplesTime4;
	ArrayList<Double> packetsTime;
	ArrayList<Double> packetsCounter;
	ArrayList<Float> samplesVoltage4;
	ArrayList<Integer> samplesPeak4;

	ArrayList<Double> samplesTime2;
	ArrayList<Integer> samplesPeak2;
	ArrayList<Float> samplesVoltage2;
	
	Queue<StreamPacket> tpackets;
	Map<Byte, SensorDefinition> tsensors;

	public static void main(String[] args)
	{
		String ime = "dolge1.s2";

		Runner r = new Runner();
		System.out.println("BLALA");
		//r.setOldTVP(ime);
		r.getNewSamples(ime);
		//r.saveTVP(ime);
		//r.setAndrejTVP(ime);

	}



	public double unitRun(String dire, String ime1, String ime2)
	{
		this.dir = new File(dire);
		getOldSamples(ime1);

		samplesPeak2 = samplesPeak4;
		samplesTime2 = samplesTime4;
		samplesVoltage2 = samplesVoltage4;

		getOldSamples(ime2);
		return metrics();
	}




	public void saveTVP(String ime)
	{


		samplesTime4 = new ArrayList<Double>();
		samplesVoltage4 = new ArrayList<Float>();


		System.out.println("save START");

		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		FilterProcessSignal f1 = new FilterProcessSignal();
		//FilterSaveS2 f2 = new FilterSaveS2(out);
		SaveCSV f2 = new SaveCSV("C:\\Users\\janez\\workspace\\S2_rw\\UnitTests\\andrej11.csv", true, System.out);
		f0.addChild(f1);
		f1.addChild(f2);

		System.out.println("save zgleda vredu : " + ls.readLines(f0, true));
		System.out.println(s2.getNotes());

	}

	public void getOldSamples(String ime)
	{
		

		System.out.println("get old samples");
		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		//FilterProcessSignal f1 = new FilterProcessSignal();
		GetLines f2 = new GetLines();

		f0.addChild(f2);
		//f1.addChild(f2);

		ls.readLines(f0, true);
		System.out.println("Notes : " + s2.getNotes());

		tpackets = f2.getPacketQ();
		tsensors = f2.getSensorDefinitions();
		
		setTVP();
	}
	
	public void getNewSamples(String ime)
	{

		System.out.println("get new Samples");
		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		FilterProcessSignal f1 = new FilterProcessSignal();
		GetLines f2 = new GetLines();

		f0.addChild(f1);
		f1.addChild(f2);

		ls.readLines(f0, true);
		System.out.println("Notes : " + s2.getNotes());

		tpackets = f2.getPacketQ();
		tsensors = f2.getSensorDefinitions();
		
		setTVP();
	}
	
	public void setTVP()
	{
		samplesTime4 = new ArrayList<Double>();
		samplesVoltage4 = new ArrayList<Float>();
		packetsTime = new ArrayList<Double>();
		packetsCounter = new ArrayList<Double>();
		
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
				samplesTime4.add(previousT);
				samplesVoltage4.add(sensorData.get(i));
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

		samplesTime4 = new ArrayList<Double>();
		samplesVoltage4 = new ArrayList<Float>();
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
			samplesTime4.add(tempT[i]);
		}
		float[] tempV = s.getVoltage();
		for(int i =0;i<tempV.length;i++)
		{
			samplesVoltage4.add(tempV[i]);
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
			samplesPeak4.add(tempP[i]);
		}	
	}


	/**
	 * Check if i+1 timestamp is bigger than i
	 */
	private void checkPositivefunction() {
		ArrayList<Double> nazaj = new ArrayList<Double>();
		ArrayList<Integer> konstanta = new ArrayList<Integer>();
		for(int i =0;i<samplesTime4.size()-1;i++)
		{
			if(samplesTime4.get(i)>samplesTime4.get(i+1))
			{
				nazaj.add(samplesTime4.get(i));
			}
			if(samplesTime4.get(i)==samplesTime4.get(i+1))
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
			System.out.println("vzorcev " + samplesTime4.size());
			System.out.println("skokov/vzorcev = " + ((float)nazaj.size()/samplesTime4.size()));
			System.out.println("1/14 " + ((float)1/14));

		}else
			System.out.println("vsi timestampi so naprej");

	}


	/**
	 * Removes DC offset from the signal voltage.
	 */
	private void removeDCoffset() {

		double sum = 0;
		for (double v : samplesVoltage4) sum += v;
		float average = (float) (sum / samplesVoltage4.size());

		for (int i = 0; i < samplesVoltage4.size(); i++)
			samplesVoltage4.set(i, samplesVoltage4.get(i)-average);
	}



	/**
	 * Searches local peaks.
	 */
	private void peakSearch() {

		ArrayList<Integer> peak_list = new ArrayList<Integer>();
		for (int i = 2; i < samplesVoltage4.size() - 3; i++) {
			if (samplesVoltage4.get(i-2) < samplesVoltage4.get(i-1) && samplesVoltage4.get(i-1) < samplesVoltage4.get(i)
					&& samplesVoltage4.get(i) > samplesVoltage4.get(i+1) && samplesVoltage4.get(i+1) > samplesVoltage4.get(i+2)) 
			{
				double dif = samplesTime4.get(i+1) - samplesTime4.get(i-1);
				if (samplesVoltage4.get(i) > EXPECTED_MAXIMUM / 1.5 && samplesVoltage4.get(i) < EXPECTED_MAXIMUM * 1.5
						&& dif < EXPECTED_TIME_DIFF*2*1.5) 
				{
					peak_list.add(i);
					//                    System.out.println("i, LEFT, left, peak, right, RIGHT =" + i + " " + (double)voltage[i-2] + " " + (double)voltage[i-1] + " " + (double)voltage[i] + " " + (double)voltage[i+1] + " " + (double)voltage[i+2]);
				}
			}
		}


		samplesPeak4 = peak_list;

	}



	private double metrics()
	{
		double secondBonus = 4e6; 
		int p2 = 0;
		int p4 = 0;
		Double vsota = 0d;
		while(p2<samplesPeak2.size() && p4 <samplesPeak4.size())
		{
			int i4 = samplesPeak4.get(p4);
			int i2 = samplesPeak2.get(p2);

			double aa = samplesTime4.get(i4)-secondBonus - samplesTime2.get(i2);
			if(aa < 2* 1E7)
			{
				double alpha41 = samplesVoltage4.get(i4-1);
				double alpha42 = samplesVoltage4.get(i4);
				double alpha43 = samplesVoltage4.get(i4+1);
				double p44 = (alpha41 -alpha43)/(alpha41 - 2*alpha42 +alpha43)/2;
				double t4 = samplesTime4.get(i4-1) + (samplesTime4.get(i4) - samplesTime4.get(i4-1)) * (1 + p44);

				double alpha21 = samplesVoltage2.get(i2-1);
				double alpha22 = samplesVoltage2.get(i2);
				double alpha23 = samplesVoltage2.get(i2+1);
				double p22 = (alpha21 -alpha23)/(alpha21 - 2*alpha22 +alpha23)/2;
				double t2 = samplesTime2.get(i2-1) + (samplesTime2.get(i2) - samplesTime2.get(i2-1)) * (1 + p22);

				Double razlika = Math.abs(t4-t2);

				if(razlika > 2*Math.abs(aa))
				{
					//System.err.println("razlika med dvema vrhoma po interpolaciji je veliko slabša kot pred");
					razlika = 0d;
				}
				
				if(razlika.equals(Double.NaN))
				{
					System.out.println("ralika med dvema vrhovoma je NaN");
				}
				vsota += razlika;

				p2++;
				p4++;
			}
			else
			{
				if(aa>0)
				{
					p2++;
				}
				else
				{
					p4++;
				}
			}
		}


		return vsota;
	}


	/**
	 * @return the samplesT
	 */
	public double[] getSamplesTimeStamp() {
		double[] R = new double[samplesTime4.size()];
		for(int j =0; j<R.length; j++)
			R[j] = samplesTime4.get(j);
		return R;
	}

	/**
	 * @return the samplesV
	 */
	public float[] getVoltage() {
		float[] R = new float[samplesVoltage4.size()];
		for(int j =0; j<R.length; j++)
			R[j] = samplesVoltage4.get(j);
		return R;
	}

	public int[] getPeaks() {
		int[] R = new int[samplesPeak4.size()];
		for(int j =0; j<R.length; j++)
			R[j] = samplesPeak4.get(j);
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
