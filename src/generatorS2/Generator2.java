package generatorS2;

import java.io.PrintStream;
import java.util.Random;

import cli.Cli;
import pipeLines.filters.SaveS2;
import si.ijs.e6.MultiBitBuffer;
import si.ijs.e6.S2;

import static java.lang.Math.floor;

public class Generator2 {

	float frequency;
	float frequencyChange;
	int change = 0;
	float percentageMissing;
	long normalDelay;
	float bigDelayChance;
	long bigDelay;
	float curentF;
	long currentTonMashine;
	long currentTonAndroid;
	long previousTonAndroid = 0;
	int curentC;
	byte[] currentD;
	boolean pause;
	boolean disconnect = false;
	long[] disconectIntervals;
	long bigMissingTill = 0;
	Random r;
	int cycle = 0; //counter for cycle in which we are if there were no disconects


	/**
	 * device is simulated randomly inside provided borders. Providing seed for random allow us repetions with the same result.
	 * @param outDir directory of new file S2 file.
	 * @param errPS	PrintStream for errors.
	 * @param seed seed for random generator.
	 * @param start start in ns. [normal use 10^10 = 10s]
	 * @param end end of measurement in ns. [normal use 60*60*10^9 = 1hour].
	 * @param frequency frequency of EKG device in Hz. [PCARD has around 128].
	 * @param frequencyChange factor of how much can frequency change. [?normal? use 0.1].
	 * @param percentageMissing Aproximate factor of missing packets. This number is used to calculate number of pauses(machine still save the data but not android).[for ?good? S2 use 0.01 for ?bad? use 0.1].
	 * @param normalDelay aproximate usual delay of packets in ns [?reasonable? value is around (1/ @param frequency /10) * 10^9].
	 * @param bigDelayChance chance for big delay, meaning machine works on. Android doesnt get any packets till the end of big delay.
	 * 				After that it gets them all in burst. They come in same order they would if not delayed [?reasonable? value is 0.01].
	 * @param bigDelay big delay in ns will be added to delay that would come from @param normalDelay [reasonable value is 10*normalDelay].
	 * @param numDisconects number of disconects that will ocure in file. Machine stops recording packages for some random time. consequently android also doesnt get them.
	 */
	public Generator2(String outDir, PrintStream errPS, long start, long end, long seed, float frequency, float frequencyChange, float percentageMissing,
			long normalDelay, float bigDelayChance, long bigDelay, int numDisconects) 
	{
		if(frequency <= 0)
		{
			errPS.println("Frequency should be bigger than 0.");
			return;
		}
		if(frequencyChange < 0)
		{
			errPS.println("frequencyChange should be 0 or bigger.");
			return;
		}
		if(percentageMissing < 0)
		{
			errPS.println("PercantigeMissing should be 0 or bigger.");
			return;
		}
		if(normalDelay < 0)
		{
			errPS.println("NormalDelay should be 0 or bigger.");
			return;
		}
		if(bigDelayChance < 0)
		{
			errPS.println("BigDelayChance should be 0 or bigger.");
			return;
		}
		if(bigDelay < 0)
		{
			errPS.println("BigDelay should be 0 or bigger.");
			return;
		}
		if(numDisconects < 0)
		{
			errPS.print("numDisconects should be 0 or bigger.");
			return;
		}
		
		this.frequency = frequency;
		this.curentF = frequency;
		this.frequencyChange = frequencyChange;
		this.percentageMissing = percentageMissing;
		this.normalDelay = normalDelay;
		this.bigDelayChance = bigDelayChance;
		this.bigDelay = bigDelay;
		this.disconectIntervals = new long[2*numDisconects];

		//*************************************                  VERSION,META,DEFINITIONS

		//File f = new File(outDir);
		//******************        Simulating saving on machine
		//SaveS2 ss2M = new SaveS2(f.getParent() +File.separator+ "Machine" + f.getName(), errPS);
		//******************        Simulating saving on Android
		SaveS2 ss2A = new SaveS2(outDir, errPS);
		

		S2.SensorDefinition sd1 = new S2.SensorDefinition("EKG test");
		sd1.setUnit("mV", 6.2E-3f, -3.19f);
		sd1.setScalar(10, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd1.setSamplingFrequency(frequency);
		S2.SensorDefinition sd2 = new S2.SensorDefinition("counter");
		sd2.setUnit("enota", 1f, 0f);
		sd2.setScalar(10, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd2.setSamplingFrequency(0);

		
		ss2A.onVersion(1, "PCARD");
		ss2A.onMetadata("date", "2018-01-01");
		ss2A.onMetadata("time", "10:30:10.555");
		ss2A.onMetadata("timezone", "+01:00");
		ss2A.onDefinition((byte) 'e', sd1);
		ss2A.onDefinition((byte) 'c', sd2);//" "
		ss2A.onDefinition((byte)0, new S2.StructDefinition("EKG stream", "eeeeeeeeeeeeeec"));
		ss2A.onDefinition((byte)0, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 1E-6));
		ss2A.onComment("1. comment. Original location after definitions");
		ss2A.onComment("Command line for generating this file using Cli was '-"+Cli.GENERATE_RANDOM+" "+seed+" "+frequency+" "+frequencyChange+" "
				+percentageMissing+" "+normalDelay/1E9+" "+bigDelayChance+" "+bigDelay/1E9+" "+numDisconects+" -"+Cli.FILTER_TIME+" "+start/1E9+" "+end/1E9
				+" -"+Cli.OUTPUT+" "+outDir+"'.");

//		ss2M.onVersion(1, "PCARD");
//		ss2M.onMetadata("date", "2018-01-01");
//		ss2M.onMetadata("time", "10:30:10.555");
//		ss2M.onMetadata("timezone", "+01:00");
//		ss2M.onDefinition((byte) 'e', sd1);
//		ss2M.onDefinition((byte) 'c', sd2);
//		ss2M.onDefinition((byte)0, new S2.StructDefinition("EKG stream", "eeeeeeeeeeeeeec"));
//		ss2M.onDefinition((byte)0, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 1E-6));
//		ss2M.onComment("1. comment. Original location after definitions");
//		ss2M.onComment("Command line for generating this file using Cli was '-"+Cli.GENERATE+" "+seed+" "+frequency+" "+frequencyChange+" "
//				+percentageMissing+" "+normalDelay/1E9+" "+bigDelayChance+" "+bigDelay/1E9+" "+numDisconects+" -"+Cli.FILTER_TIME+" "+start/1E9+" "+end/1E9
//				+" -"+Cli.OUTPUT+" "+outDir+"'.");
		
		//*************************************                 RANDOM

		r = new Random();
		r.setSeed(seed);


		//************************************                  STARTING POINT

		currentTonMashine = start;
		curentC = 0;
		for(int i=0;i<numDisconects;i++)
		{
            // start of the disconnect: random positive long lower than 'end'
			disconectIntervals[2*i] = (r.nextLong() & Long.MAX_VALUE) % end;
            // end of the disconnect: start + 1 second + random 0.000..59.000 seconds
			disconectIntervals[2*i+1] =  (long) (disconectIntervals[2*i] + 1E9 + r.nextInt(59000)*1000000L);
		}

		//************************************                 FILING STREAMLINES

        long firstAndroidTime = -1;
		while(currentTonMashine < end)
		{
			if (frequencyChange > 0) {
				calculateFrequency();
				currentTonMashine += 1E9/curentF * 14;
			} else {
				currentTonMashine =  start + (long) (cycle * 14e9/frequency);
			}
			
            // check if disconnect is 'scheduled' and if it is, get the time of reconnect
			long timeAfterDisconnect = checkDisconnect();

			if (disconnect) {
				ss2A.onComment("Disconnect has just happened.");
				// we are inside disconnect: we just restart counters. No data is sent.
				curentC = 0;
                currentTonMashine = timeAfterDisconnect;
                // advance 'cycle' to the time just prior to reconnect
                cycle = (int)floor((timeAfterDisconnect - start)*frequency/14*1e-9);
            } else {
				curentC += 14;
				//*******************           SAVING ON MACHINE
				//ss2M.onStreamPacket((byte) 0, currentTonMashine, currentD.length, currentD);

				//*******************           SAVING ON ANDROID
				bigMissing();

				if(pause)
				{
					// unlike disconnect, machine still saves packages but android doesn't. chance for pause is calculated based on chanceForMissing
				}else
				{
					float wifi = r.nextFloat();
					if(wifi >= this.percentageMissing) //random losses
					{
						calculateTonAndroid(); //calculates normal or possibly big delay
                        if (firstAndroidTime == -1)
                            firstAndroidTime = currentTonAndroid;

						currentD = makeData();
						ss2A.onStreamPacket((byte) 0, currentTonAndroid, currentD.length, currentD);
						previousTonAndroid = currentTonAndroid;
					}
				}
			}
			cycle++;
		}
		System.err.printf("0 .. %g, %d .. %.8g; %.8g\n", start * 1e-9, cycle, currentTonMashine*1e-9, (cycle-1)*14e9/(currentTonMashine-start));
        System.err.printf("0 .. %g, %d .. %.8g; %.8g\n", start * 1e-9, cycle, currentTonAndroid*1e-9, (cycle-1)*14e9/(currentTonAndroid-firstAndroidTime));

		//ss2M.onComment("2. comment. Original location after packets before end");
		//ss2M.onEndOfFile();

		ss2A.onComment("2. comment. Original location after packets before end");
		ss2A.onEndOfFile();
	}

	private long checkDisconnect() {
		int n = disconectIntervals.length/2;
		for(int i = 0;i<n;i++)
		{
			if ((disconectIntervals[2*i] <= currentTonMashine) && (currentTonMashine < disconectIntervals[2*i+1]))
			{
				disconnect = true;
				return disconectIntervals[2*i+1];
			}
		}
		disconnect = false;
        return -1;
	}

	private void bigMissing()
	{
		if(currentTonMashine < bigMissingTill)
		{
			//PASS
		}
		else
		{
			float wifi = r.nextFloat();

			float modifier = 20 + 30*r.nextFloat();

			if(wifi < percentageMissing /modifier)
			{
				bigMissingTill = currentTonMashine + (long)(modifier* percentageMissing / frequency * 1E6);
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
	private void calculateTonAndroid() {
		if(normalDelay > 0) {
			currentTonAndroid = currentTonMashine + Math.abs(r.nextLong() % normalDelay);
			if(currentTonAndroid <=previousTonAndroid) {
				currentTonAndroid = previousTonAndroid + normalDelay/20 + 1;
			} else if (bigDelay != 0) {
				if(r.nextFloat() < bigDelayChance)
				{
					currentTonAndroid += Math.abs(r.nextLong() % bigDelay);
				}
			}
		} else {
			currentTonAndroid = currentTonMashine;
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
