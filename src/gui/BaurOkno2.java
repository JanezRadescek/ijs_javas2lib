package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.SpringLayout;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;
import net.miginfocom.swing.MigLayout;
import java.awt.FlowLayout;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Font;
import javax.swing.JTextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class BaurOkno2 extends JFrame {

	private JPanel contentPane;
	private JTextField Cli;
	private JTextField textField;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					BaurOkno2 frame = new BaurOkno2();
					frame.setVisible(true);
					frame.pack();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public BaurOkno2() {
		//Frame
		setTitle("GUI for S2");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//setBounds(100, 100, screenSize.width/2, screenSize.height/2);
		
		//Menu
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnNewMenu = new JMenu("file");
		menuBar.add(mnNewMenu);
		
		JMenuItem mntmSave = new JMenuItem("save");
		mnNewMenu.add(mntmSave);
		
		//Panel
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setBackground(Color.red);
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		textField = new JTextField();
		contentPane.add(textField, BorderLayout.NORTH);
		
		JPanel radioPanel = new JPanel();
		contentPane.add(radioPanel, BorderLayout.WEST);
		GridBagLayout gbl_radioPanel = new GridBagLayout();
		gbl_radioPanel.columnWidths = new int[]{109, 109, 0};
		gbl_radioPanel.rowHeights = new int[]{79, 23, 0};
		gbl_radioPanel.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_radioPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		radioPanel.setLayout(gbl_radioPanel);
		
		ButtonGroup JRadioGRoup = new ButtonGroup();
		
		JRadioButton rdbtn1 = new JRadioButton("Statistics");
		GridBagConstraints gbc_rdbtn1 = new GridBagConstraints();
		gbc_rdbtn1.anchor = GridBagConstraints.WEST;
		gbc_rdbtn1.insets = new Insets(0, 0, 0, 5);
		gbc_rdbtn1.gridx = 0;
		gbc_rdbtn1.gridy = 0;
		radioPanel.add(rdbtn1, gbc_rdbtn1);
		JRadioGRoup.add(rdbtn1);
		
		JRadioButton rdbtn2 = new JRadioButton("CSV");
		GridBagConstraints gbc_rdbtn2 = new GridBagConstraints();
		gbc_rdbtn2.anchor = GridBagConstraints.WEST;
		gbc_rdbtn2.gridx = 0;
		gbc_rdbtn2.gridy = 1;
		radioPanel.add(rdbtn2, gbc_rdbtn2);
		JRadioGRoup.add(rdbtn2); 
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(1);
		contentPane.add(splitPane, BorderLayout.SOUTH);
		
		Cli = new JTextField();
		splitPane.setLeftComponent(Cli);
		Cli.setText("haha12345");
		
		JButton btnNewButton = new JButton("GO");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		splitPane.setRightComponent(btnNewButton);

		Cli.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				
				textField.setText("" + e.getKeyCode());
				
				if(e.getKeyCode() == 10)
				{
					textField.setText("Kappa");
				}
			}
		});
	}
}
