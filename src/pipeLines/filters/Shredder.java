package pipeLines.filters;

import pipeLines.Pipe;

public class Shredder extends Pipe {
	
	@Override
	public Pipe addChild(Pipe f) {
		return f;
	}

}
