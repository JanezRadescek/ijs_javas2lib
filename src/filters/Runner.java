package filters;

import java.io.File;

import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;

public class Runner {

	public static void main(String[] args) {
		
		//C:\Users\janez\workspace\S2_rw\UnitTests\test2.s2  moja datoteka ki jo hočem popraviti
		File dir = new File("UnitTests");
		String inName = "test2.s2";
		
		
		//if you want add subdirectory
		
		String out = "test2Fixed.s2";

		
		//Dont change below that
		
		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, inName);
		
		FilterProcessSignal f1 = new FilterProcessSignal();
		FilterSaveS2 f2 = new FilterSaveS2(out);
		
		ls.addReadLineCallback(f1);
		f1.addChild(f2);
		
		
		System.out.print("zgleda vredu : " + ls.readAndProcessFile());
		System.out.print(s2.getNotes());
		
	}

}
