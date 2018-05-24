package filtersOld;

import java.io.File;

import pipeLines.filters.SaveS2;
import pipeLines.filters.FilterTime;
import si.ijs.e6.S2;
import si.ijs.e6.S2.LoadStatus;

/**
 * @author janez
 *For testing purposes only
 */
public class RunnerRAM {

	private final double startTime = 0*60;
	private final double endTime = 3*60*60*60;
	final File dir = new File("C:\\Users\\janez\\workspace\\S2_rw\\S2files");
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String ime = "dolge1.s2";
		
		RunnerRAM r = new RunnerRAM();
		r.save(ime);
	}

	
	private void save(String ime) {
		System.out.println("save START");

		System.out.println(ime);

		S2 s2 = new S2();
		LoadStatus ls = s2.load(dir, ime);

		FilterTime f0 = new FilterTime(startTime,endTime);
		FilterProcessSignal f1 = new FilterProcessSignal();
		SaveS2 f2 = new SaveS2(dir.getAbsolutePath() + File.separator+"kopija"+ime, System.out);
		//FilterSaveCSV f2 = new FilterSaveCSV("C:\\Users\\janez\\workspace\\S2_rw\\UnitTests\\andrej11.csv", true);
		ls.addReadLineCallback(f0);
		f0.addChild(f1);
		f1.addChild(f2);

		System.out.println("save zgleda vredu : " + ls.readAndProcessFile());
		System.out.println(s2.getNotes());
	}

}
