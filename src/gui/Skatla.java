package gui;

import java.util.ArrayList;

import javax.swing.JCheckBox;

import gui.BaurOkno2.Komponenta;

public class Skatla extends JCheckBox implements Komponenta{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ArrayList<String> info = new ArrayList<String>();

	public Skatla(String text) {
		super(text);
	}
	
	@Override
	public boolean ready() {
		return isEnabled();
	}

	public void SetInfo(String Tinfo)
	{
		ArrayList<String> temp = new ArrayList<String>();
		temp.add(Tinfo);
		this.info = temp;
	}
	
	@Override
	public ArrayList<String> getInfo() {
		return info;
	}

}
