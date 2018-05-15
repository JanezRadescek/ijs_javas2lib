package pipeLines.conglomerates;

import java.io.PrintStream;

import pipeLines.Pipe;
import si.ijs.e6.S2.LoadStatus;

/**
 * Sync classes are intended to sync S2 lines betwen two S2 file. Usualy to be able to merge them later
 * @author janez
 *
 */
public abstract class Sync{

	//"INTERFACE STUFF"
	Pipe primaryInPut;
	Pipe secondaryInPut;
	Pipe primaryOutPut;
	Pipe secondaryOutPut;

	protected PrintStream out;
	
	public void setPrintStream(PrintStream print)
	{
		out = print;
	}
	
	public Sync(Pipe primaryInput, Pipe secondaryInput)
	{
		this.primaryInPut = primaryInput;
		this.secondaryInPut = secondaryInput;
	}

	/**
	 * @return the primaryOutPut
	 */
	public Pipe getPrimaryOutPut() {
		return primaryOutPut;
	}

	/**
	 * @return the secondaryOutPut
	 */
	public Pipe getSecondaryOutPut() {
		return secondaryOutPut;
	}



}
