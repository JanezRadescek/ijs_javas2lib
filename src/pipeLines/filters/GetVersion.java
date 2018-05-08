package pipeLines.filters;

import pipeLines.Pipe;

public class GetVersion extends Pipe {
	
	public int versionInt;
	public String version;
	
	@Override
	public boolean onVersion(int versionInt, String version) {
		this.versionInt = versionInt;
		this.version = version;
		return super.onVersion(versionInt, version);
	}

}
