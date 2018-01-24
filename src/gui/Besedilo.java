package gui;

import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.JTextField;
import gui.BaurOkno2.Komponenta;

public class Besedilo extends JTextField implements Komponenta {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private HashSet<Komponenta> kids = new HashSet<Komponenta>();
	
	public Besedilo() {
		super();
	}
	
	public void addKid(Komponenta kid)
	{
		kids.add(kid);
	}
	
	public void resetKids()
	{
		kids = new HashSet<Komponenta>();
	}

	@Override
	public boolean ready() {
		boolean R = true;
		for(Komponenta kid:kids)
		{
			R &= kid.ready();
		}
		R &= (getText().length() > 0);
		return R;
	}

	@Override
	public ArrayList<String> getInfo() {
		ArrayList<String> temp = new ArrayList<String>();
		temp.add(getText());
		return temp;
	}

}
