package gui;

import javax.swing.JTextField;
import javax.swing.text.Document;

import gui.BaurOkno2.komponenta;

public class Besedilo extends JTextField implements komponenta {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Besedilo() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Besedilo(Document arg0, String arg1, int arg2) {
		super(arg0, arg1, arg2);
		// TODO Auto-generated constructor stub
	}

	public Besedilo(int arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public Besedilo(String arg0, int arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public Besedilo(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean ready() {
		// TODO Auto-generated method stub
		return (getText() != null);
	}

}
