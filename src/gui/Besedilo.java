package gui;

import java.util.ArrayList;

import javax.swing.JTextField;
import gui.BaurOkno2.komponenta;

public class Besedilo extends JTextField implements komponenta {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Besedilo() {
		super();
	}

	@Override
	public boolean ready() {
		return (getText().length() > 0);
	}

	@Override
	public ArrayList<String> getInfo() {
		ArrayList<String> temp = new ArrayList<String>();
		temp.add(getText());
		return temp;
	}

}
