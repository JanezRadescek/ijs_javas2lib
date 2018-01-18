package gui;

import java.util.ArrayList;

import javax.swing.JCheckBox;

import gui.BaurOkno2.komponenta;

public class Skatla extends JCheckBox implements komponenta{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Skatla(String text) {
		super(text);
	}
	
	@Override
	public boolean ready() {
		return isEnabled();
	}

	@Override
	public ArrayList<String> getInfo() {
		return new ArrayList<String>();
	}

}
