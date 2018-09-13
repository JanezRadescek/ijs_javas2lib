package generatorS2;

import java.io.PrintStream;
import java.util.Random;

import cli.Cli;
import pipeLines.filters.SaveS2;
import si.ijs.e6.MultiBitBuffer;
import si.ijs.e6.S2;

import static java.lang.Math.floor;

public class Generator2 {

	double frequency;
	double frequencyChange;
	int frequencyRamp = 0;
    double frequencyIncrement = 0;
	float percentageMissing;
	long normalDelay;
	float bigDelayChance;
	long bigDelay;
	double currentF;
    double targetF;
	long currentTonMachine;
	long currentTonAndroid;
	long previousTonAndroid = 0;
	int currentC;
	byte[] currentD;
	boolean pause;
	boolean disconnect = false;
	long[] disconnectIntervals;
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
	 * @param frequencyChange factor of how much can frequency frequencyRamp. [?normal? use 0.1].
	 * @param percentageMissing Aproximate factor of missing packets. This number is used to calculate number of pauses(machine still save the data but not android).[for ?good? S2 use 0.01 for ?bad? use 0.1].
	 * @param normalDelay maximal delay of packets in ns [?reasonable? value is around (1/ @param frequency /10) * 10^9].
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
			errPS.println("PercentageMissing should be 0 or bigger.");
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
			errPS.print("numDisconnects should be 0 or bigger.");
			return;
		}
		
		this.frequency = frequency;
		this.currentF = frequency;
		this.frequencyChange = frequencyChange;
		this.percentageMissing = percentageMissing;
		this.normalDelay = normalDelay;
		this.bigDelayChance = bigDelayChance;
		this.bigDelay = bigDelay;
		this.disconnectIntervals = new long[2*numDisconects];

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

		currentTonMachine = start;
		currentC = 0;
		for(int i=0;i<numDisconects;i++)
		{
            // start of the disconnect: random positive long lower than 'end'
			disconnectIntervals[2*i] = (r.nextLong() & Long.MAX_VALUE) % end;
            // end of the disconnect: start + 1 second + random 0.000..59.000 seconds
			disconnectIntervals[2*i+1] =  (long) (disconnectIntervals[2*i] + 1E9 + r.nextInt(59000)*1000000L);
		}

		//************************************                 FILING STREAMLINES

        long firstAndroidTime = -1;
		while(currentTonMachine < end)
		{
			if (frequencyChange > 0) {
				if (calculateFrequency())
				    ss2A.onComment("Frequency will change to "+targetF);
                if (frequencyRamp % 1000 == 1)
                    ss2A.onComment("Frequency is "+currentF);
				currentTonMachine += 14e9 / currentF;
			} else {
				currentTonMachine =  start + (long) (cycle * 14e9/frequency);
			}
			
            // check if disconnect is 'scheduled' and if it is, get the time of reconnect
			long timeAfterDisconnect = checkDisconnect();

			if (disconnect) {
				ss2A.onComment("Disconnect has just happened.");
				// we are inside disconnect: we just restart counters. No data is sent.
				currentC = 0;
                currentTonMachine = timeAfterDisconnect;
                // advance 'cycle' to the time just prior to reconnect
                cycle = (int)floor((timeAfterDisconnect - start)*frequency/14*1e-9);
            } else {
				currentC += 14;
				//*******************           SAVING ON MACHINE
				//ss2M.onStreamPacket((byte) 0, currentTonMachine, currentD.length, currentD);

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
		//System.err.printf("0 .. %g, %d .. %.8g; %.8g\n", start * 1e-9, cycle, currentTonMachine*1e-9, (cycle-1)*14e9/(currentTonMachine-start));
//        System.err.printf("0 .. %g, %d .. %.8g; %.8g\n", start * 1e-9, cycle, currentTonAndroid*1e-9, (cycle-1)*14e9/(currentTonAndroid-firstAndroidTime));

		//ss2M.onComment("2. comment. Original location after packets before end");
		//ss2M.onEndOfFile();

		ss2A.onComment("2. comment. Original location after packets before end");
		ss2A.onEndOfFile();
	}

	private long checkDisconnect() {
		int n = disconnectIntervals.length/2;
		for(int i = 0;i<n;i++)
		{
			if ((disconnectIntervals[2*i] <= currentTonMachine) && (currentTonMachine < disconnectIntervals[2*i+1]))
			{
				disconnect = true;
				return disconnectIntervals[2*i+1];
			}
		}
		disconnect = false;
        return -1;
	}

	private void bigMissing()
	{
		if(currentTonMachine < bigMissingTill)
		{
			//PASS
		}
		else
		{
			float wifi = r.nextFloat();

			float modifier = 20 + 30*r.nextFloat();

			if(wifi < percentageMissing /modifier)
			{
				bigMissingTill = currentTonMachine + (long)(modifier* percentageMissing / frequency * 1E6);
				pause = true;
			}
			else
			{
				pause = false;
			}
		}
	}

	/**
	 * When device isn't changing its frequency there is 1% chance it will start changing its frequency in next cycle.
     * When its changing its frequency its just make sure its stays within boundaries.
     * return true when new target frequency is calculated
	 */
	private boolean calculateFrequency() {
		if (frequencyRamp > 0) {
            //currentF += (targetF - currentF) / frequencyRamp;
            currentF +=frequencyIncrement;
            frequencyRamp--;
        } else {
            // chance for changing the frequency
			if (r.nextFloat() < 0.01) {
                // set a new target frequency (minimum of 0.1 Hz) and set up a slow ramp up to that frequency
                targetF = Math.max((frequency + r.nextGaussian() * frequencyChange * frequency), 0.1);
                double freqDiff = Math.abs(targetF - currentF);
                int rampLengthMin = 1+(int)(freqDiff * 100.0f);
                frequencyRamp = rampLengthMin + r.nextInt(rampLengthMin*9);
                frequencyIncrement = (targetF - currentF) / frequencyRamp;
                return true;
			}
		}
		return false;
	}

	/**
	 * calculates timestamps for android
	 */
	private void calculateTonAndroid() {
		if(normalDelay > 0) {
			currentTonAndroid = currentTonMachine + Math.abs(r.nextLong() % normalDelay);
			if(currentTonAndroid <=previousTonAndroid) {
				currentTonAndroid = previousTonAndroid + normalDelay/20 + 1;
			} else if (bigDelay != 0) {
				if(r.nextFloat() < bigDelayChance)
				{
					currentTonAndroid += Math.abs(r.nextLong() % bigDelay);
				}
			}
		} else {
			currentTonAndroid = currentTonMachine;
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
		mbb.setInts(currentC, 140, 10, 1);
		return R;
	}

}
