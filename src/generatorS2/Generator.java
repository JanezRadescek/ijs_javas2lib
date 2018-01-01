package generatorS2;

import java.io.File;
import java.util.Random;

import s2.S2;

public class Generator {

	public static void main(String[] args) {

		final int N = 100;

		for(int n=0; n<N; n++)
		{
			generateS2(n);
		}

	}

	private static void generateS2(int n) {
		
		String inDir  = "C:\\Users\\janez\\workspace\\S2_rw\\Original";
		String fname = "generated";
		S2 s2 = new S2();
		S2.StoreStatus storeS = s2.store(new File(inDir), fname + n + ".s2");

		double a =  Math.random()*5;
		double ab = Math.random()*10;
		double b = a + ab;
		double korak = ab/(5*ab + Math.random()*10);

		storeS.setVersion(1, "PCARD").addMetadata("date", "2018-01-01").addMetadata("time", "10:30:10.555")
		.addMetadata("zone", "+01:00");
		
		storeS.addTextMessage("Test number " + n);
		
		S2.SensorDefinition sensord = new S2.SensorDefinition("foo");
		storeS.addDefinition((byte) 'e', sensord);
		
		storeS.addDefinition((byte)0, new S2.StructDefinition("Test", "eeeeeee"));
		storeS.addDefinition((byte)0, new S2.TimestampDefinition(S2.AbsoluteId.abs_relative, (byte)3, 0.000001));
		
		
		

	}



}
