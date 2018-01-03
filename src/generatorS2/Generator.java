package generatorS2;

import java.io.File;
import s2.S2;
import s2.S2.Nanoseconds;

public class Generator {

	public static void main(String[] args) {
		
		generateS2();
	}

	private static void generateS2() {
		
		String inDir  = "C:\\Users\\janez\\workspace\\S2_rw\\Original";
		String fname = "generated";
		S2 s2 = new S2();
		S2.StoreStatus storeS = s2.store(new File(inDir), fname+".s2");

		long a =  (long) (5 * Math.pow(10, 9));

		storeS.setVersion(1, "PCARD").addMetadata("date", "2018-01-01").addMetadata("time", "10:30:10.555")
		.addMetadata("zone", "+01:00");
		
		storeS.addTextMessage("Test message");
		
		S2.SensorDefinition sd1 = new S2.SensorDefinition("Testni sensor 1");
		sd1.setUnit("testne sekunde", 2/3, 98);
		sd1.setVector(127, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 8, 16, 2);
		sd1.setSamplingFrequency(1/32);
		storeS.addDefinition((byte) 'e', sd1);
		
		S2.SensorDefinition sd3 = new S2.SensorDefinition("Testni sensor 2");
		sd3.setUnit("testne sekunde", 0, 0);
		sd3.setVector(16, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0, 2, 0);
		sd3.setSamplingFrequency(0);
		storeS.addDefinition((byte) 32, sd3);//" "
		
		S2.SensorDefinition sd2 = new S2.SensorDefinition("Testni sensor 3");
		sd2.setUnit("testne sekunde", 255, 255);
		sd2.setVector(127, S2.ValueType.vt_integer, S2.AbsoluteId.abs_absolute, 0, 1, 0);
		sd2.setSamplingFrequency(255);
		storeS.addDefinition((byte) 126, sd2); //"~"
		
		
		

		
		storeS.addDefinition((byte)0, new S2.StructDefinition("Testni struct 1", "ee"));
		storeS.addDefinition((byte)0, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 0.000001));
		
		storeS.addDefinition((byte)1, new S2.StructDefinition("Testni struct 2", " ~"));
		storeS.addDefinition((byte)1, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)0, 0));
		
		storeS.addDefinition((byte)2, new S2.StructDefinition("Testni struct 3", "e ~e~ ~e")); //8
		storeS.addDefinition((byte)2, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)127, 127));
		
		
		
		storeS.addTimestamp(new Nanoseconds(a));
		
		storeS.addSensorPacket((byte) 0, 0, new byte[]{1,2});
		storeS.addSensorPacket((byte) 1, 0, new byte[]{6,3,3});
		storeS.addSensorPacket((byte) 2, 0, new byte[]{9,8,7,6,5,4,3,2});
		
		storeS.addTimestamp(new Nanoseconds(a+10));
		
		storeS.addSensorPacket((byte) 0, 1000, new byte[]{1,2});
		storeS.addSensorPacket((byte) 1, 10, new byte[]{6,3,3});
		storeS.addSensorPacket((byte) 2, 100, new byte[]{9,8,7,6,5,4,3,2});
		storeS.addSensorPacket((byte) 2, 100, new byte[]{9,8,7,6,5,4,3,2});
		
		storeS.endFile(true);
		
		System.out.println("THE END");
	}



}
