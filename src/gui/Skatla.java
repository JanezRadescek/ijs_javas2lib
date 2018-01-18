package gui;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;

import gui.BaurOkno2.komponenta;

public class Skatla extends JCheckBox implements komponenta{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean ready() {
		return isEnabled();
	}

	public Skatla() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Skatla(Action a) {
		super(a);
		// TODO Auto-generated constructor stub
	}

	public Skatla(Icon icon, boolean selected) {
		super(icon, selected);
		// TODO Auto-generated constructor stub
	}

	public Skatla(Icon icon) {
		super(icon);
		// TODO Auto-generated constructor stub
	}

	public Skatla(String text, boolean selected) {
		super(text, selected);
		// TODO Auto-generated constructor stub
	}

	public Skatla(String text, Icon icon, boolean selected) {
		super(text, icon, selected);
		// TODO Auto-generated constructor stub
	}

	public Skatla(String text, Icon icon) {
		super(text, icon);
		// TODO Auto-generated constructor stub
	}

	public Skatla(String text) {
		super(text);
		// TODO Auto-generated constructor stub
	}

}
