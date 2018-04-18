package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Console extends JFrame{
	
	private static final long serialVersionUID = -5546758728060230891L;
	
	JTextArea ta;
	
	public Console()
	{
		setTitle("GUI for S2");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(500,300));
		setVisible(true);
		
		
		JPanel jp = new JPanel();
		jp.setLayout(new BorderLayout());
		setContentPane(jp);
		
		ta = new JTextArea();
		JScrollPane sp = new JScrollPane(ta,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setBackground(Color.red);
		jp.add(sp, BorderLayout.NORTH);
		
		JPanel jpb = new JPanel();
		jpb.setLayout(new BorderLayout());
		jpb.setBackground(Color.yellow);
		jp.add(jpb, BorderLayout.SOUTH);
		
		JButton jb = new JButton("Ok");
		jb.setPreferredSize(new Dimension(40,40));
		jpb.add(jb, BorderLayout.CENTER);
		JPanel filer1 = new JPanel();
		filer1.setBackground(Color.blue);
		JPanel filer2 = new JPanel();
		filer1.setBackground(Color.green);
		jpb.add(filer1, BorderLayout.WEST);
		jpb.add(filer2, BorderLayout.EAST);
		
		pack();
	}
	
	public void setText(String t)
	{
		ta.setText(t);
		repaint();
		pack();
	}
	

}
