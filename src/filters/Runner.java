package filters;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;

import callBacks.MultiBitBuffer;
import filters.FilterGetLines.StreamPacket;
import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;
import si.ijs.e6.S2.SensorDefinition;

public class Runner {
	
	private double startTime = 0*60;
	private double endTime = 6.01*60;
	
	public static void main(String[] args)
	{
		String ime = "andrej1.s2";
		
		Runner r = new Runner();
		
		r.setOldTVP(ime);
		r.setTVP(ime);
		//r.saveTVP(ime);
		
		
	}
	
	

	ArrayList<Double> samplesTime;
	ArrayList<Double> packetsTime;
	ArrayList<Double> packetsCounter;
	ArrayList<Float> samplesVoltage;
	ArrayList<Integer> samplesPeak;
	
	
	public void saveTVP(String ime)
	{
		File dir = new File("C:\\Users\\janez\\workspace\\S2_rw\\S2files");

		samplesTime = new ArrayList<Double>();
		samplesVoltage = new ArrayList<Float>();


		System.out.println("save START");

		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		//TODO popravi filterprocessignal
		FilterProcessSignal f1 = new FilterProcessSignal();
		//FilterSaveS2 f2 = new FilterSaveS2(out);
		FilterSaveCSV f2 = new FilterSaveCSV("C:\\Users\\janez\\workspace\\S2_rw\\UnitTests\\andrej11.csv", true);
		ls.addReadLineCallback(f0);
		f0.addChild(f1);
		f1.addChild(f2);

		System.out.println("save zgleda vredu : " + ls.readAndProcessFile());
		System.out.println(s2.getNotes());
		
	}
	
	
	public void setOldTVP(String ime) {
		//IN
		//C:\Users\janez\workspace\S2_rw\S2files\andrej1.s2  moja datoteka ki jo hočem popraviti
		File dir = new File("C:\\Users\\janez\\workspace\\S2_rw\\S2files");

		samplesTime = new ArrayList<Double>();
		samplesVoltage = new ArrayList<Float>();
		packetsTime = new ArrayList<Double>();
		packetsCounter = new ArrayList<Double>();

		//OUT maybe you need to create unittest dir




		//Dont change below that

		System.out.println("old START");



		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		//TODO popravi filterprocessignal
		//FilterProcessSignal f1 = new FilterProcessSignal();
		FilterGetLines f2 = new FilterGetLines();
		ls.addReadLineCallback(f0);
		f0.addChild(f2);
		//f1.addChild(f2);

		System.out.println("old zgleda vredu : " + ls.readAndProcessFile());
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
				int entitySize = (int) tempSensor.getResolution();
				//OLD CODE int entitySize = s2.getEntityHandles(cb).sensorDefinition.resolution;
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
	
	public void setTVP(String ime) {
		//IN
		//C:\Users\janez\workspace\S2_rw\S2files\andrej1.s2  moja datoteka ki jo hočem popraviti
		File dir = new File("C:\\Users\\janez\\workspace\\S2_rw\\S2files");

		samplesTime = new ArrayList<Double>();
		samplesVoltage = new ArrayList<Float>();
		packetsTime = new ArrayList<Double>();

		//OUT maybe you need to create unittest dir




		//Dont change below that

		System.out.println("runner START");



		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		//TODO popravi filterprocessignal
		FilterProcessSignal f1 = new FilterProcessSignal();
		//FilterSaveS2 f2 = new FilterSaveS2(out);
		FilterGetLines f2 = new FilterGetLines();
		ls.addReadLineCallback(f0);
		f0.addChild(f1);
		f1.addChild(f2);

		System.out.println("runner zgleda vredu : " + ls.readAndProcessFile());
		System.out.println("Notes : " + s2.getNotes());

		Queue<StreamPacket> tpackets = f2.getPacketQ();
		Map<Byte, SensorDefinition> tsensors = f2.getSensorDefinitions();

		int Cbase = 0;
		Float Cprevious = 0f;
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
				int entitySize = (int) tempSensor.getResolution();
				//OLD CODE int entitySize = s2.getEntityHandles(cb).sensorDefinition.resolution;
				int temp = mbb.getInt(mbbOffset, entitySize);
				mbbOffset += entitySize;

				float k = tempSensor.k;
				float n = tempSensor.n;
				float t = k*temp + n;
				sensorData.add(t);
			}

			Float Ctemp = sensorData.get(14) + Cbase;
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
			Cprevious = Ctemp;
			
		}
		
		tpackets = null;
		
		checkPositivefunction();
		
		removeDCoffset();
		peakSearch();
		System.out.println("runner DONE" + "\n");

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
				double tt = samplesTime.get(i)-samplesTime.get(i+1);
				nazaj.add(samplesTime.get(i));
			}
			if(samplesTime.get(i)==samplesTime.get(i+1))
			{
				double tt = samplesTime.get(i)-samplesTime.get(i+1);
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

		double EXPECTED_MAXIMUM = 50;
		
		int size = samplesVoltage.size();
		
		ArrayList<Integer> peak_list = new ArrayList<>();
		for (int i = 2; i < samplesVoltage.size() - 3; i++) {
			if (samplesVoltage.get(i-2) < samplesVoltage.get(i) && samplesVoltage.get(i-1) < samplesVoltage.get(i) && samplesVoltage.get(i) > samplesVoltage.get(i+1) && samplesVoltage.get(i) > samplesVoltage.get(i+2)) {
				if (samplesVoltage.get(i) > EXPECTED_MAXIMUM / 1.5 && samplesVoltage.get(i) < EXPECTED_MAXIMUM * 1.5) {
					peak_list.add(i);
					//                    System.out.println("i, LEFT, left, peak, right, RIGHT =" + i + " " + (double)voltage[i-2] + " " + (double)voltage[i-1] + " " + (double)voltage[i] + " " + (double)voltage[i+1] + " " + (double)voltage[i+2]);
				}
			}
		}

		samplesPeak = peak_list;

	}


	/**
	 * @return the samplesT
	 */
	public double[] getSamplesTimeStamp() {
		double[] R = new double[samplesTime.size()];
		for(int j =0; j<R.length; j++)
			R[j] = samplesTime.get(j);
		System.out.println("# of samplesT" + R.length);
		return R;
	}

	/**
	 * @return the samplesV
	 */
	public float[] getVoltage() {
		float[] R = new float[samplesVoltage.size()];
		for(int j =0; j<R.length; j++)
			R[j] = samplesVoltage.get(j);
		System.out.println("# of samplesV" + R.length);
		return R;
	}

	public int[] getPeaks() {
		int[] R = new int[samplesPeak.size()];
		for(int j =0; j<R.length; j++)
			R[j] = samplesPeak.get(j);
		System.out.println("# of peaks" + R.length);
		return R;
	}
	
	/**
	 * @return the paketsT
	 */
	public double[] getPacketsTimeStamp() {
		double[] R = new double[packetsTime.size()];
		for(int j =0; j<R.length; j++)
			R[j] = packetsTime.get(j);
		System.out.println("# of packets" + R.length);
		return R;
	}
	
	/**
	 * @return the paketsT
	 */
	public double[] getPacketsCounter() {
		double[] R = new double[packetsCounter.size()];
		for(int j =0; j<R.length; j++)
			R[j] = packetsCounter.get(j);
		System.out.println("# of packets" + R.length);
		return R;
	}

}
