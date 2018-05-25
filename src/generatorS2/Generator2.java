package generatorS2;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;

import pipeLines.filters.SaveS2;
import si.ijs.e6.S2;

public class Generator2 {

	float frequency;
	float frequencyChange;
	int change = 0;
	float percentigeMissing;
	float delayFactor;
	float curentF;
	long curentTonMashine;
	long curentTonAndroid;
	int curentC;
	byte[] curentD;
	boolean pause;
	long pauseTill = 0;
	Random r;


	/**
	 * @param directory directory of new file S2 file
	 * @param errPS	PrintStream for errors
	 * @param seed seed for random generator
	 * @param length length of measurement in ns
	 * @param frequency frequency of EKG device
	 * @param frequencyChange percentige of how much can frequency change
	 * @param percentigeMissing Aproximate percentige of missing packets
	 * @param delayFactor Aproximate everage of delay
	 */
	public Generator2(String directory, PrintStream errPS, long seed, long length, float frequency, float frequencyChange, float percentigeMissing, float delayFactor) 
	{
		this.frequency = frequency;
		this.curentF = frequency;
		this.frequencyChange = frequencyChange;
		this.percentigeMissing = percentigeMissing;
		this.delayFactor = delayFactor;


		//*************************************                  VERSION,META,DEFINITIONS

		SaveS2 ss2 = new SaveS2(directory, errPS);

		ss2.onVersion(1, "PCARD");

		ss2.onMetadata("date", "2018-01-01");
		ss2.onMetadata("time", "10:30:10.555");
		ss2.onMetadata("timezone", "+01:00");


		S2.SensorDefinition sd1 = new S2.SensorDefinition("EKG test");
		sd1.setUnit("mV", 6.2E-3f, -3.19f);
		sd1.setScalar(10, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd1.setSamplingFrequency(frequency);
		ss2.onDefinition((byte) 'e', sd1);

		S2.SensorDefinition sd2 = new S2.SensorDefinition("counter");
		sd2.setUnit("enota", 1f, 0f);
		sd2.setScalar(10, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd2.setSamplingFrequency(0);
		ss2.onDefinition((byte) 'c', sd2);//" "


		ss2.onDefinition((byte)0, new S2.StructDefinition("EKG stream", "eeeeeeeeeeeeeec"));
		ss2.onDefinition((byte)0, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 1E-6));



		//*************************************                 RANDOM

		r = new Random();
		r.setSeed(seed);


		//************************************                  STARTING POINT

		long temL = r.nextLong();
		curentTonMashine = modul(temL,(long) 1E10);


		//************************************                 FILING STREAMLINES

		while(curentTonMashine < length)
		{
			float wifi = r.nextFloat();

			calculateFrequency();
			curentTonMashine += curentF;
			curentC += 14;
			curentD = makeData();

			checkPause();

			if(!pause)
			{
				if(wifi >= percentigeMissing)
				{
					ss2.onStreamPacket((byte) 0, toWriteReady(curentTonMashine), curentD.length, curentD);
				}
			}else
			{
				//zaenkrat nič
			}

		}

	}


	private static long modul(long a, long b)
	{
		long c = a/b;
		return a - c*b;
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

	private void calculateFrequency()
	{
		if(change != 0)
		{
			if(Math.abs(curentF + (change/change) * frequencyChange /10 - frequency) < frequencyChange)
			{
				curentF += (change/change) * frequencyChange /10;
			}
			else
			{
				change = 0;
			}
		}
		else
		{
			float mac = r.nextFloat();
			if(mac<1/100)
			{
				change = r.nextInt(20)-10;
			}
		}
	}
	
	private void calculateTonAndroid()
	{
		//TODO todo
	}

	private long toWriteReady(long a)
	{
		return 0;
	}

	private byte[] makeData()
	{
		//TODO todo
		return null;

	}

}
