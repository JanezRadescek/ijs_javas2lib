package filters;

import suportingClasses.Version;

public class FilterGetVersion extends Filter {
	
	public int versionInt;
	public String version;
	
	@Override
	public boolean onVersion(int versionInt, String version) {
		this.versionInt = versionInt;
		this.version = version;
		return super.onVersion(versionInt, version);
	}

}
