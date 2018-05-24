package generatorS2;

import java.io.File;

import si.ijs.e6.S2;
import si.ijs.e6.S2.Nanoseconds;

/**
 * @author janez
 *For generating special S2 for testing purposes
 */
public class Generator {

	public static void main(String[] args) {
		
		generateS2();
	}

	private static void generateS2() {
		
		String outDir  = "."+File.separator+"Generated";
		String fname = "generated";
		S2 s2 = new S2();
		File targetDirectory = new File(outDir);
		// if it doesn't exis, create it now
		targetDirectory.mkdir();
		S2.StoreStatus storeS = s2.store(targetDirectory, fname+".s2");

		long sensorTime = 5L;

		storeS.setVersion(1, "PCARD")
                .addMetadata("date", "2018-01-01")
                .addMetadata("time", "10:30:10.555")
		        .addMetadata("timezone", "+01:00")
                .addTextMessage("Test message");
		
		S2.SensorDefinition sd1 = new S2.SensorDefinition("Testni sensor 1");
		sd1.setUnit("testne sekunde", 1, 0);
		//sd1.setVector(12, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 8, 3, 2);
		sd1.setScalar(8, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd1.setSamplingFrequency(1/32);
		storeS.addDefinition((byte) 'e', sd1);
		
		S2.SensorDefinition sd2 = new S2.SensorDefinition("Testni sensor 2");
		sd2.setUnit("testne sekunde", (float)2/3, 5);
		//sd2.setVector(16, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0, 2, 0);
		sd2.setScalar(16, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd2.setSamplingFrequency(0);
		storeS.addDefinition((byte) 32, sd2);//" "
		
		S2.SensorDefinition sd3 = new S2.SensorDefinition("Testni sensor 3");
		sd3.setUnit("testne sekunde", (float)1/255, 255);
		//sd3.setVector(127, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0, 1, 0);
		sd3.setScalar(64, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0);
		sd3.setSamplingFrequency(255);
		storeS.addDefinition((byte) 126, sd3); //"~"

		
		storeS.addDefinition((byte)0, new S2.StructDefinition("Testni struct 1", "ee")); //2*byte
		storeS.addDefinition((byte)0, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 1E-9));
		
		storeS.addDefinition((byte)1, new S2.StructDefinition("Testni struct 2", " ~")); //10*byte
		storeS.addDefinition((byte)1, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 1E-9));
		
		storeS.addDefinition((byte)2, new S2.StructDefinition("Testni struct 3", "e ~")); //11*byte
		storeS.addDefinition((byte)2, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 1E-9));
		
		
		storeS.addTimestamp(new Nanoseconds(sensorTime));
		
		storeS.addSensorPacket((byte) 0, 0, new byte[]{1,2});
		storeS.addSensorPacket((byte) 1, 0, new byte[]{6,3,6,3,6,3,6,3,6,3});
		storeS.addSensorPacket((byte) 2, 0, new byte[]{9,8,7,6,5,4,3,2,1,0,-1});
		
		storeS.addTimestamp(new Nanoseconds(sensorTime+10));
		
		storeS.addSensorPacket((byte) 0, 1, new byte[]{1,2});
		storeS.addSensorPacket((byte) 1, 1, new byte[]{6,3,6,3,6,3,6,3,6,3});
		storeS.addSensorPacket((byte) 2, 1, new byte[]{9,8,7,6,5,4,3,2,1,0,-1});
		storeS.addSensorPacket((byte) 0, 1, new byte[]{1,2});
		storeS.addSensorPacket((byte) 2, 3, new byte[]{9,8,7,6,5,4,3,2,1,0,-1});
		
		
		storeS.endFile(true);
		
		System.out.println("THE END");
	}
}
