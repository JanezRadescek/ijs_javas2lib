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
	int freqCurentIndex = 0;//for faster searching
	long freqOffset = 0; //for repeting
	
	ArrayList<Long> disc = new ArrayList<Long>();
	int discCurentIndex = 0;
	long discOffset = 0;
	
	ArrayList<Long> paus = new ArrayList<Long>();
	ArrayList<Long> dela = new ArrayList<Long>();

	private long curentTonMashine; //trenutni čas
	private int curentC = 0; //trenutni counter
	private float curentF; //trenutna frequenca
	private int cicle = 0;// trenutni cikel podobno kor counter le da se counter lahko resetira, ustavi...

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
		try {	
			BufferedReader brF = new BufferedReader(new FileReader(frequencies));
			String s;
			while((s = brF.readLine()) != null)
			{
				String[] ss = s.split(",");
				freqTime.add(Long.parseLong(ss[0]));
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
				disc.add(Long.parseLong(s));
			}
			brDi.close();
			
			BufferedReader brP = new BufferedReader(new FileReader(pauses));
			while((s = brP.readLine()) != null)
			{
				paus.add(Long.parseLong(s));
			}
			brP.close();
			
			BufferedReader brDe = new BufferedReader(new FileReader(delays));
			while((s = brDe.readLine()) != null)
			{
				dela.add(Long.parseLong(s));
			}
			brDe.close();
			
		} catch (FileNotFoundException e) {
			errPS.println("Canon read from input files");
			return;
		}catch (IOException e)
		{
			errPS.println("Canon read from input files");
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
			curentTonMashine += 1E9/curentF * 14;

			if(checkDisconnect())
			{
				ss2.onComment("Disconect has just happened.");
				// we are inside disconnect we just restart counters. No data is sent.
				curentC = 0;
			}
			else
			{
				curentC += 14;
				
				
				
			}

		}


	}

	

	private void calculateFrequency() {
		int n = freqTime.size();	
		for(int i = freqCurentIndex; i<n; i++)
		{
			if(freqTime.get(i) + freqOffset > curentTonMashine)
			{
				curentF = freq.get(i);
				freqCurentIndex = i;
				return;
			}
		}
		//we must reapet/reset 
		freqOffset += freqTime.get(n-1);
		freqCurentIndex = 0;
		calculateFrequency();
	}

	private boolean checkDisconnect() 
	{
		int n = disc.size()/2;
		for(int i = discCurentIndex; i<n; i++)
		{
			if(disc.get(2*i) + discOffset < curentTonMashine)
			{
				discCurentIndex = i;
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
		//reapet
		discOffset += disc.get(2*n - 1);
		discCurentIndex = 0;
		return checkDisconnect();
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
