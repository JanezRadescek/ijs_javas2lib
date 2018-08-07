package generatorS2;

import java.io.PrintStream;

import cli.Cli;
import pipeLines.filters.SaveS2;
import si.ijs.e6.S2;

/**
 * @author janez
 * unlike previous generators purpose of this one is to read file with pattern and generate S2 file based on this pattern.
 */
public class Generator3 {
	
	/**
	 * @param outDir directory of new file S2 file.
	 * @param errPS PrintStream for errors.
	 * @param frequencies directory of file with frequencies.
	 * @param disconects directory of file with disconects.
	 * @param pauses directory of file with pauses.
	 * @param delays directory of file with delays.
	 */
	public Generator3(String outDir, PrintStream errPS, String frequencies, String disconects, String pauses, String delays)
	{
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
		
		
		
		
		
		
		
	}
	

}
