package gui;

import java.util.ArrayList;

import javax.swing.JButton;

import gui.BaurOkno2.komponenta;

public class Gumb extends JButton implements komponenta {



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private ArrayList<String> info = new ArrayList<String>();
	private ArrayList<Skatla> kids = new ArrayList<Skatla>();
	

	public Gumb(String arg0) {
		super(arg0);
	}

	public Gumb(String string, String prefix) {
		this(string);
		info.add(prefix);
		
	}

	public void addKid(Skatla s)
	{
		kids.add(s);
	}
	
	public boolean ready() {
		
		return isEnabled();
	}

	@Override
	public ArrayList<String> getInfo() {
		if(kids.isEmpty())
			return info;
		else
		{
			ArrayList<String> temp = (ArrayList<String>) info.clone();
			int R =0;
			for(int i=0;i<kids.size();i++)
			{
				if(kids.get(i).isSelected())
					R |=  1<<i;
			}
			temp.add(Integer.toBinaryString(R));
			return temp;
		}
	}

}
