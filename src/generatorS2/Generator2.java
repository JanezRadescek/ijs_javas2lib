package generatorS2;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;

import pipeLines.filters.SaveS2;
import si.ijs.e6.MultiBitBuffer;
import si.ijs.e6.S2;

public class Generator2 {

	float frequency;
	float frequencyChange;
	int change = 0;
	float percentigeMissing;
	long normalDelay;
	float bigDelayChance;
	float bigDelayFactor;
	float curentF;
	long curentTonMashine;
	long curentTonAndroid;
	long previousTonAndroid = 0;
	int curentC;
	byte[] curentD;
	boolean pause;
	boolean disconect = false;
	long[] disconectIntervals;
	long pauseTill = 0;
	Random r;


	/**
	 * device is simulated randomly inside provided borders. Providing seed for random allow us repetions with the same result.
	 * @param directory directory of new file S2 file.
	 * @param errPS	PrintStream for errors.
	 * @param seed seed for random generator.
	 * @param start start in ns. [normal use 10^10 = 10s]
	 * @param end end of measurement in ns. [normal use 60*60*10^9 = 1hour].
	 * @param frequency frequency of EKG device in Hz. [PCARD has around 128].
	 * @param frequencyChange factor of how much can frequency change. [?normal? use 0.1].
	 * @param percentigeMissing Aproximate factor of missing packets. This number is used to calculate number of pauses(machine still save the data but not android).[for ?good? S2 use 0.01 for ?bad? use 0.1].
	 * @param normalDelay aproximate usual delay of packets in ns [?reasonable? value is around (1/ @param frequency /10) * 10^9].
	 * @param bigDelaychance chance for big delay [?reasonable? value is 0.01].
	 * @param bigDelayFactor big delay will be up to @param bigDelayFactor*normal ns [?reasonable? value is 10].
	 * @param numPauses number of pauses that will ocure in file 
	 */
	public Generator2(String directory, PrintStream errPS, long start, long end, long seed, float frequency, float frequencyChange, float percentigeMissing, 
			long normalDelay, float bigDelayChance, float bigDelayFactor, int numPauses) 
	{
		this.frequency = frequency;
		this.curentF = frequency;
		this.frequencyChange = frequencyChange;
		this.percentigeMissing = percentigeMissing;
		this.normalDelay = normalDelay;
		this.bigDelayChance = bigDelayChance;
		this.bigDelayFactor = bigDelayFactor;
		this.disconectIntervals = new long[2*numPauses];

		//*************************************                  VERSION,META,DEFINITIONS

		File f = new File(directory);
		//******************        Simulating saving on machine
		SaveS2 ss2M = new SaveS2(f.getParent() +File.separator+ "Machine" + f.getName(), errPS);
		//******************        Simulating saving on Android
		SaveS2 ss2A = new SaveS2(f.getParent() +File.separator+ "Android" + f.getName(), errPS);


		ss2A.onVersion(1, "PCARD");
		ss2A.onMetadata("date", "2018-01-01");
		ss2A.onMetadata("time", "10:30:10.555");
		ss2A.onMetadata("timezone", "+01:00");

		S2.SensorDefinition sd1 = new S2.SensorDefinition("EKG test");
		sd1.setUnit("mV", 6.2E-3f, -3.19f);
		sd1.setScalar(10, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd1.setSamplingFrequency(frequency);
		S2.SensorDefinition sd2 = new S2.SensorDefinition("counter");
		sd2.setUnit("enota", 1f, 0f);
		sd2.setScalar(10, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd2.setSamplingFrequency(0);

		ss2A.onDefinition((byte) 'e', sd1);
		ss2A.onDefinition((byte) 'c', sd2);//" "
		ss2A.onDefinition((byte)0, new S2.StructDefinition("EKG stream", "eeeeeeeeeeeeeec"));
		ss2A.onDefinition((byte)0, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 1E-6));
		ss2A.onComment("1. comment. Original location after definitions");


		ss2M.onVersion(1, "PCARD");
		ss2M.onMetadata("date", "2018-01-01");
		ss2M.onMetadata("time", "10:30:10.555");
		ss2M.onMetadata("timezone", "+01:00");
		ss2M.onDefinition((byte) 'e', sd1);
		ss2M.onDefinition((byte) 'c', sd2);
		ss2M.onDefinition((byte)0, new S2.StructDefinition("EKG stream", "eeeeeeeeeeeeeec"));
		ss2M.onDefinition((byte)0, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 1E-6));
		ss2M.onComment("1. comment. Original location after definitions");

		//*************************************                 RANDOM

		r = new Random();
		r.setSeed(seed);



		//************************************                  STARTING POINT

		curentTonMashine = start;
		curentC = 0;
		for(int i=0;i<numPauses;i++)
		{
			disconectIntervals[2*i] = r.nextLong()%end;
			disconectIntervals[2*i+1] =  (long) (disconectIntervals[2*i] + 1E9 + r.nextLong()%(60E9));
		}

		//************************************                 FILING STREAMLINES

		while(curentTonMashine < end)
		{
			calculateFrequency();
			curentTonMashine += 1E9/curentF;


			checkDisconect();

			if(disconect)
			{
				// we are inside disconect we just restart counters, all data
				curentC = 0;
			}else
			{
				curentC += 14;
				curentD = makeData();

				//*******************           SAVING ON MACHINE
				ss2M.onStreamPacket((byte) 0, curentTonMashine, curentD.length, curentD);

				//*******************           SAVING ON ANDROID
				checkPause();

				if(pause)
				{

				}else
				{
					float wifi = r.nextFloat();
					if(wifi >= this.percentigeMissing)
					{
						calculateTonAndroid();
						ss2A.onStreamPacket((byte) 0, curentTonAndroid, curentD.length, curentD);
						previousTonAndroid = curentTonAndroid;
					}
				}
			}

		}

		ss2M.onComment("2. comment. Original location after packets before end");
		ss2M.onEndOfFile();

		ss2A.onComment("2. comment. Original location after packets before end");
		ss2A.onEndOfFile();
	}

	private void checkDisconect() {
		int n = disconectIntervals.length/2;
		for(int i = 0;i<n;i++)
		{
			if(disconectIntervals[2*i]<= curentTonMashine & curentTonMashine < disconectIntervals[2*i+1])
			{
				disconect = true;
				return;
			}
		}
		disconect = false;

	}

	private void checkPause()
	{
		if(curentTonMashine < pauseTill)
		{
			//PASS
		}
		else
		{
			float wifi = r.nextFloat();

			float modifier = 20 + 30*r.nextFloat();

			if(wifi < percentigeMissing/modifier)
			{
				pauseTill = curentTonMashine + (long)(modifier*percentigeMissing / frequency * 1E6); 
				pause = true;
			}
			else
			{
				pause = false;
			}
		}
	}

	/**
	 * when device isnt changing its frequeny there is 1% it will start changing its frequency in next cycle. when its changing its frequeny its just make sure its stays within boundaries.
	 */
	private void calculateFrequency()
	{
		if(change != 0)
		{
			if(Math.abs(curentF/frequency + Math.signum(change) * frequencyChange /10 - 1) < frequencyChange)
			{
				curentF += (change/change) * frequencyChange * frequency /10;
				change -= Math.signum(change);
			}
			else
			{
				change = 0;
			}
		}
		else
		{

			float temperature = r.nextFloat();
			if(temperature<1/100)
			{
				change = r.nextInt(20)-10;
			}
		}
	}

	/**
	 * calculates timestamps for android
	 */
	private void calculateTonAndroid()
	{
		if(normalDelay > 0)
		{
			curentTonAndroid = curentTonMashine + (r.nextLong() % normalDelay);
			if(r.nextFloat() < bigDelayChance)
			{
				curentTonAndroid += r.nextLong() % (long) (bigDelayFactor*normalDelay);
			}
			if(curentTonAndroid<=previousTonAndroid)
			{
				curentTonAndroid = previousTonAndroid + normalDelay/100 + 1;
			}
		}else
		{
			curentTonAndroid = curentTonMashine;
		}
	}

	/**
	 * creates constant data compatible with struct and sensor definition
	 * @return
	 */
	private byte[] makeData()
	{
		byte R[] = new byte[19];
		MultiBitBuffer mbb = new MultiBitBuffer(R);
		mbb.setInts(10, 0, 10, 14);
		mbb.setInts(curentC, 140, 10, 1);
		return R;
	}

}
