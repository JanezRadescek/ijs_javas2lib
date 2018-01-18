package gui;

import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import gui.BaurOkno2.komponenta;

public class Gumb extends JButton implements komponenta {



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Gumb() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Gumb(Action arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public Gumb(Icon arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public Gumb(String arg0, Icon arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public Gumb(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public boolean ready() {
		
		return isEnabled();
	}

	@Override
	public ArrayList<String> getInfo() {
		return new ArrayList<String>();
	}

}
