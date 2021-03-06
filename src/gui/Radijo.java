package gui;

import java.util.ArrayList;

import javax.swing.JRadioButton;

import gui.BaurOkno2.Komponenta;

public class Radijo extends JRadioButton implements Komponenta{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ArrayList<String> info = new ArrayList<String>();
	
	public Radijo(String string, String prefix) {
		super(string);
		info.add(prefix);
		setName(prefix);
	}

	
	public Radijo(String string, String prefix, String bool) {
		this(string, prefix);
		info.add(bool);
		
	}


	public ArrayList<String> getInfo()
	{
		return info;
	}


	@Override
	public boolean ready() {
		return isEnabled();
	}
	

}
