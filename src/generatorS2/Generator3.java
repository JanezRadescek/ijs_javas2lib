package generatorS2;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import cli.Cli;
import pipeLines.filters.SaveS2;
import si.ijs.e6.MultiBitBuffer;
import si.ijs.e6.S2;

/**
 * @author janez
 * unlike previous generators purpose of this one is to read files with patterns and generate S2 file based on this patterns.
 */
public class Generator3 {

	ArrayList<Long> freqTime = new ArrayList<Long>();
	ArrayList<Float> freq = new ArrayList<Float>();
	private int freqCurentIndex = 0;//for faster searching
	long freqOffset = 0; //for repeting

	ArrayList<Long> disc = new ArrayList<Long>();
	private int discCurentIndex = 0;//for faster seaching 
	private long discOffset = 0; //for repeting

	ArrayList<Long> paus = new ArrayList<Long>();
	private int pauseCurentIndex = 0;//for faster seaching 
	long pauseOffset = 0; //for repeting

	ArrayList<Long> dela = new ArrayList<Long>();
	private int delaCurentIndex = 0;//

	private long curentTonMashine; //trenutni čas
	private int curentC = 0; //trenutni counter
	private float curentF; //trenutna frequenca
	private float previousF = 0; //target frequency
	private long previousT = 0;
	//private int cicle = 0;// trenutni cikel podobno kor counter le da se counter lahko resetira, ustavi...

	PrintStream errPS;//to get it ouside construct
	

	/**
	 * @param outDir directory of new file S2 file.
	 * @param errPS PrintStream for errors.
	 * @param start start in ns. [normal use 10^10 = 10s]
	 * @param end end of measurement in ns. [normal use 60*60*10^9 = 1hour].
	 * @param frequencies directory of file with frequencies.
	 * @param disconects directory of file with disconects.
	 * @param pauses directory of file with pauses.
	 * @param delays directory of file with delays.
	 */
	public Generator3(String outDir, PrintStream errPS, long start, long end, String frequencies, String disconects, String pauses, String delays)
	{
		this.errPS = errPS;
		
		try {	
			BufferedReader brF = new BufferedReader(new FileReader(frequencies));
			String s;
			while((s = brF.readLine()) != null)
			{
				String[] ss = s.split(",");
				freqTime.add(Long.parseLong(ss[0].trim()));
				freq.add(Float.parseFloat(ss[1]));
			}
			brF.close();
			if(freq.size() == 0)
			{
				errPS.println("Bad frequency file");
			}

			BufferedReader brDi = new BufferedReader(new FileReader(disconects));
			while((s = brDi.readLine()) != null)
			{
				String[] ss = s.split(",");
				disc.add(Long.parseLong(ss[0].trim()));
				disc.add(Long.parseLong(ss[1].trim()));
			}
			brDi.close();

			BufferedReader brP = new BufferedReader(new FileReader(pauses));
			while((s = brP.readLine()) != null)
			{
				String[] ss = s.split(",");
				paus.add(Long.parseLong(ss[0].trim()));
				paus.add(Long.parseLong(ss[1].trim()));
			}
			brP.close();

			BufferedReader brDe = new BufferedReader(new FileReader(delays));
			while((s = brDe.readLine()) != null)
			{
				dela.add(Long.parseLong(s));
			}
			brDe.close();

		} catch (FileNotFoundException e) {
			errPS.println("Can not read from input files.");
			return;
		}catch (IOException e)
		{
			errPS.println("Can not read from input files");
			return;
		}
		catch(NumberFormatException e)
		{
			errPS.println("Make sure the data in files are in correct pattern.");
			return;
		}




		SaveS2 ss2 = new SaveS2(outDir, errPS);

		S2.SensorDefinition sd1 = new S2.SensorDefinition("EKG test");
		sd1.setUnit("mV", 6.2E-3f, -3.19f);
		sd1.setScalar(10, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd1.setSamplingFrequency(0);
		S2.SensorDefinition sd2 = new S2.SensorDefinition("counter");
		sd2.setUnit("enota", 1f, 0f);
		sd2.setScalar(10, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd2.setSamplingFrequency(0);


		ss2.onVersion(1, "PCARD");
		ss2.onMetadata("date", "2018-01-01");
		ss2.onMetadata("time", "10:30:10.555");
		ss2.onMetadata("timezone", "+01:00");
		ss2.onDefinition((byte) 'e', sd1);
		ss2.onDefinition((byte) 'c', sd2);//" "
		ss2.onDefinition((byte)0, new S2.StructDefinition("EKG stream", "eeeeeeeeeeeeeec"));
		ss2.onDefinition((byte)0, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 1E-6));
		ss2.onComment("1. comment. Original location after definitions");
		ss2.onComment("Command line for generating this file using Cli was '-"+Cli.GENERATE_FROM_FILE+" "+frequencies+" "+disconects+" "
				+pauses+" "+delays+" -"+Cli.OUTPUT+" "+outDir+"'.");




		curentTonMashine = start;

		while(curentTonMashine < end)
		{
			calculateFrequency();
			if(curentF > 0)
			{
				curentTonMashine += 14E9/curentF;
			}
			else 
			{
				errPS.println("Curent frequency is " + curentF + ". Frequency should be positive. Generating is ended");
				ss2.onEndOfFile();
				return;
			}

			

			if(checkDisconnect())
			{
				ss2.onComment("Disconect has just happened.");
				// we are inside disconnect. we just restart counters. No data is sent.
				curentC = 0;
			}
			else
			{
				curentC += 14;
				if(checkPause())
				{
					// we are inside pause. we dont restart counters. No data is sent.
					//calculateDelay(); //TODO do we increase delayIndex ? that way delay and pauses which are logicly independent actualy became independent.
					//delaCurentIndex++;//chiper wersion
				}
				else
				{
					long currentTonAndroid = curentTonMashine + calculateDelay();
					byte[] currentD = makeData();
					ss2.onStreamPacket((byte) 0, currentTonAndroid, currentD.length, currentD);
				}
			}
		}

		ss2.onComment("last comment. Original location after packets before end");
		ss2.onEndOfFile();

	}


	private void calculateFrequency() {
		int n = freqTime.size();	
		for(int i = freqCurentIndex; i<n; i++)
		{
			long targetT = freqTime.get(i) + freqOffset;
			if(curentTonMashine < targetT)
			{
				float targetF = freq.get(i);
				
				//curentF = freq.get(i);
				if(targetF <= 0)
				{
					curentF = 0;
					errPS.println("frequency shouldnt be <= 0.");
					return;
				}
				if(previousF <= 0)
				{
					//since we dont have frequency at the start we will just make it constant till first frequency.
					curentF = targetF;
					return;
				}
				freqCurentIndex = i;
				
				double kk = (targetF - previousF)/(targetT- previousT);
				double nn = previousF - kk * previousT;
				
				curentF = (float) (kk * curentTonMashine + nn);
				
				return;
			}
			previousF = freq.get(i);
			previousT = targetT;
		}
		//we must reapet/reset 
		freqOffset += freqTime.get(n-1);
		freqCurentIndex = 0;
		calculateFrequency();
	}


	private boolean checkDisconnect() 
	{
		int n = disc.size()/2;
		if(n == 0)
		{
			return false;
		}
		for(int i = discCurentIndex; i<n; i++)
		{
			disc.get(2*i);
			if(disc.get(2*i) + discOffset < curentTonMashine)
			{
				discCurentIndex = i;
				disc.get(2*i + 1);
				if(curentTonMashine < disc.get(2*i + 1) + discOffset)
				{
					return true;
				}	
			}
			else
			{
				return false;
			}
		}
		//repeat
		discOffset += disc.get(2*n - 1);
		discCurentIndex = 0;
		return checkDisconnect();
	}


	private boolean checkPause() {
		int n = paus.size()/2;
		if(n == 0)
		{
			return false;
		}
		for(int i = pauseCurentIndex; i<n; i++)
		{
			if(paus.get(2*i) + pauseOffset < curentTonMashine)
			{
				pauseCurentIndex = i;
				if(curentTonMashine < paus.get(2*i + 1) + pauseOffset)
				{
					return true;
				}	
			}
			else
			{
				return false;
			}
		}
		//reapet
		pauseOffset += paus.get(2*n - 1);
		pauseCurentIndex = 0;
		return checkPause();
	}


	private long calculateDelay() {
		int n = dela.size();
		if(n>0)
		{
			long delay = dela.get(delaCurentIndex % n);
			delaCurentIndex++;
			return delay;
		}
		else
		{
			//empty file?
			
			return 0;
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
		mbb.setInts(curentC % 1024, 140, 10, 1);//TODO check if corrrect modulo
		return R;
	}

}
