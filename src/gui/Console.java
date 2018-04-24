package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.miginfocom.swing.MigLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Console extends JFrame{
	
	private static final long serialVersionUID = -5546758728060230891L;
	
	JTextArea ta;
	Console trenutno;
	
	public Console()
	{
		trenutno = this;
		setTitle("GUI for S2");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setPreferredSize(new Dimension(500,500));
		setVisible(true);
		
		
		JPanel jp = new JPanel();
		jp.setLayout(new BorderLayout());
		setContentPane(jp);
		
		ta = new JTextArea();
		JScrollPane sp = new JScrollPane(ta,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setBackground(Color.red);
		jp.add(sp, BorderLayout.CENTER);
		
		JPanel jpb = new JPanel();
		jpb.setBackground(Color.yellow);
		jp.add(jpb, BorderLayout.SOUTH);
		jpb.setLayout(new MigLayout("", "[grow][40px][grow]", "[30px]"));
		
		JButton jb = new JButton("Ok");
		jb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				trenutno.dispose();
			}
		});
		jb.setPreferredSize(new Dimension(40,30));
		
		jpb.add(jb, "cell 1 0,growx,aligny top");
		
		//JPanel filer1 = new JPanel();
		//filer1.setBackground(Color.blue);
		//JPanel filer2 = new JPanel();
		//filer2.setBackground(Color.green);
		//jpb.add(filer1, "cell 0 0,alignx left,aligny top");
		//jpb.add(filer2, "cell 2 0,alignx left,aligny top");
		
		pack();
	}
	
	public void setText(String t)
	{
		ta.setText(t);
		repaint();
		pack();
	}
	

}
