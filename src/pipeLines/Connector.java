package pipeLines;

/**
 * @author janez
 *Wraps pipeline into one pipe. 
 *Add starting pipe in pipeline with addStart. Add Pipe end from getEnd to last pipe in pipeline.
 */
public class Connector extends Pipe {

	private Pipe end = new Pipe();

	public Pipe addStart(Pipe f) {
		return super.addChild(f);
	}

	@Override
	public Pipe addChild(Pipe f) {
		end.addChild(f);
		return f;
	}

	public Pipe getEnd()
	{
		return end;
	}

}
