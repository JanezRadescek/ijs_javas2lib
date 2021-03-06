package pipeLines.filters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pipeLines.Pipe;
import si.ijs.e6.S2.SensorDefinition;
import si.ijs.e6.S2.StructDefinition;
import suportingClasses.S2utilities;

/**
 * Filter which saves as CSV. Since CSV is very restrictive only timestamps, handles and actual datas will be saved.
 * @author janez
 *
 */
public class SaveCSV extends Pipe{

	PrintStream outCSV;//for csv out
	boolean close = false;//do we close outCSV ? yes if we made it

	String[] CSVline; //one line of data
	boolean body = false;
	

	private int maxColumns;

	private Map<Byte,SensorDefinition> sensorDefinitions = new HashMap<Byte,SensorDefinition>();
	private Map<Byte,StructDefinition> structDefinitions = new HashMap<Byte,StructDefinition>();


	/**
	 * Filter which saves as CSV. Since CSV is very restrictive only timestamps, handles and actual datas will be saved.
	 * @param outDir string representing file directory AND name
	 * @param errPS PrintStream on which we write any errors or something like that.
	 */
	public SaveCSV(String outDir, PrintStream errPS)
	{
		File temF = new File(outDir);
		if((temF.getParentFile() != null) && !temF.getParentFile().exists())
		{
			errPS.println("Given directory " +temF.getParent() +" does not exist. Creating one");
			temF.getParentFile().mkdirs();
		}
		
		this.errPS = errPS;
		try {
			this.outCSV = new PrintStream(new FileOutputStream(outDir));
			close = true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Filter which saves as CSV. Since CSV is very restrictive only timestamps, handles and actual datas will be saved.
	 * @param csv PrintStream on which we write CSV
	 * @param print PrintStream on which we write any errors or something like that.
	 */
	public SaveCSV(PrintStream csv, PrintStream errPS)
	{
		this.errPS = errPS;
		this.outCSV = csv;
	}


	private void printLine() {
		for(int i=0;i<2+maxColumns-1;i++)
		{
			outCSV.print(CSVline[i] + ",");
		}
		outCSV.println(CSVline[maxColumns+2-1]);

	}


	@Override
	public boolean onEndOfFile() {
		if(!body)
		{
			body = true;
			CSVline = new String[2 + maxColumns];
			CSVline[0] = "TimeStamp";
			CSVline[1] = "Handle";
			for(int c = 2; c<maxColumns+2;c++)
			{
				CSVline[c] = "data" + (c-1);
			}
			printLine();
		}
		
		if(close) outCSV.close();
		
		return pushEndofFile();
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		if(!body)
		{
			body = true;
			CSVline = new String[2 + maxColumns];
			CSVline[0] = "TimeStamp";
			CSVline[1] = "Handle";
			for(int c = 2; c<maxColumns+2;c++)
			{
				CSVline[c] = "data" + (c-1);
			}
			printLine();
		}
		
		if(close) outCSV.close();
		
		return pushUnmarkedEndofFile();
	}


	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		sensorDefinitions.put(handle, definition);

		return pushDefinition(handle, definition);
	}


	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		//TODO should we clone definition ? perhaps we will change that down the line.
		structDefinitions.put(handle, definition);
		int temp = definition.elementsInOrder.length();
		if(temp>maxColumns)
		{
			maxColumns = temp;
		}

		return pushDefinition(handle, definition);
	}


	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		//when we get first packet we write head of csv first
		if(!body)
		{
			body = true;
			CSVline = new String[2 + maxColumns];
			CSVline[0] = "TimeStamp";
			CSVline[1] = "Handle";
			for(int c = 2; c<maxColumns+2;c++)
			{
				CSVline[c] = "data" + (c-1);
			}
			printLine();
		}

		ArrayList<Float> sensorData = S2utilities.decodeData(structDefinitions.get(handle), sensorDefinitions, data, errPS);
		
		//writing
		CSVline = new String[2 + maxColumns];
		CSVline[0] = timestamp+"";
		CSVline[1] = handle+"";
		for(int i = 0;i<maxColumns;i++)
		{
			if(i<sensorData.size())
				CSVline[2+i] = sensorData.get(i)+"";
			else
				CSVline[2+i] = "";
		}
		printLine();

		//push

		return pushStreamPacket(handle, timestamp, len, data);
	}
}
