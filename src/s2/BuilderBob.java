package s2;

import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;

public class BuilderBob implements ReadLineCallbackInterface {

	public BuilderBob(S2 file2, String izhodDir, String izhodName) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onComment(String comment) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onVersion(int versionInt, String version) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onMetadata(String key, String value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onEndOfFile() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUnmarkedEndOfFile() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onError(int lineNum, String error) {
		// TODO Auto-generated method stub
		return false;
	}

}
