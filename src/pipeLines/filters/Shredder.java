package pipeLines.filters;

import pipeLines.Pipe;
import si.ijs.e6.S2.StructDefinition;

public class Shredder extends Pipe {
	
	@Override
	public Pipe addChild(Pipe f) {
		return f;
	}

}
