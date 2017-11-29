package s2;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class CsvStream extends PrintStream {

	public CsvStream(OutputStream out) {
		super(out);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void print(String a)
	{
		a += ",";
		try {
			this.out.write(a.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
	
	@Override
	public void println(String a)
	{
		a += "\n";
		try {
			this.out.write(a.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
