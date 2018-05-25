package gui;

import java.util.ArrayList;

import javax.swing.JCheckBox;

import gui.BaurOkno2.Komponenta;

public class Kljukica extends JCheckBox implements Komponenta {

	private static final long serialVersionUID = -957891834912441245L;

	private ArrayList<String> info = new ArrayList<String>();

	public Kljukica(String string) {
		super(string);
	}

	@Override
	public boolean ready() {

		return true;
	}

	@Override
	public ArrayList<String> getInfo() {
		if(isSelected())
		{
			if(!info.contains("-e"))
			{
				info.add("-e");
			}
			return info;
		}
		else
		{
			info.clear();
			return info;
		}
	}

}
