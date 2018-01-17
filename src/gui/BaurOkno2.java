package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;
import net.miginfocom.swing.MigLayout;

public class BaurOkno2 extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtCliArgs;
	JFileChooser chooser = new JFileChooser();
	
	private HashMap<String,ArrayList<String>> cliArgs = new HashMap<String,ArrayList<String>>();
	private JTextField textField_DMainInput;
	private JTextField textField_DSecondaryInput;
	private JTextField textField_DOutput;
	private JTextField txtlongmaxvalue;
	private JTextField txtall;
	private JTextField txtall_1;

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
		//chooser
		chooser.setCurrentDirectory(new File("."));
		
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
		
		JPanel panel_radioButtons = new JPanel();
		panel_radioButtons.setBackground(Color.GREEN);
		panel_radioButtons.setForeground(Color.BLACK);
		contentPane.add(panel_radioButtons, BorderLayout.WEST);
		
		ButtonGroup JRadioGRoup = new ButtonGroup();
		panel_radioButtons.setLayout(new MigLayout("", "[71px]", "[51px][51px][51px][51px]"));
		
		JRadioButton rdbtn1 = new JRadioButton("Statistics");
		panel_radioButtons.add(rdbtn1, "cell 0 0,grow");
		JRadioGRoup.add(rdbtn1);
		
		JRadioButton rdbtn2 = new JRadioButton("CSV");
		panel_radioButtons.add(rdbtn2, "cell 0 1,grow");
		JRadioGRoup.add(rdbtn2); 
		
		JRadioButton rdbtn3 = new JRadioButton("cut S2");
		panel_radioButtons.add(rdbtn3, "cell 0 2,grow");
		JRadioGRoup.add(rdbtn3); 
		
		JRadioButton rdbtn4 = new JRadioButton("Merge S2");
		JRadioGRoup.add(rdbtn4);
		panel_radioButtons.add(rdbtn4, "cell 0 3,grow");
		
		JSplitPane splitPane_South = new JSplitPane();
		splitPane_South.setResizeWeight(1);
		contentPane.add(splitPane_South, BorderLayout.SOUTH);
		
		txtCliArgs = new JTextField();
		txtCliArgs.setEditable(false);
		splitPane_South.setLeftComponent(txtCliArgs);
		txtCliArgs.setText("Cli Args");
		
		JButton btnNewButton = new JButton("GO");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		splitPane_South.setRightComponent(btnNewButton);
		
		JPanel panel_Options = new JPanel();
		panel_Options.setBackground(Color.CYAN);
		contentPane.add(panel_Options, BorderLayout.CENTER);
		panel_Options.setLayout(new MigLayout("", "[176px][176px]", "[34px][34px][34px][34px][34px][34px]"));
		
		JButton btn_MainInput = new JButton("Main input");
		panel_Options.add(btn_MainInput, "cell 0 0,grow");
		
		textField_DMainInput = new JTextField();
		panel_Options.add(textField_DMainInput, "cell 1 0,grow");
		textField_DMainInput.setColumns(10);
		
		JButton btn_SecondaryInput = new JButton("Secondary input");
		panel_Options.add(btn_SecondaryInput, "cell 0 1,grow");
		
		textField_DSecondaryInput = new JTextField();
		panel_Options.add(textField_DSecondaryInput, "cell 1 1,grow");
		textField_DSecondaryInput.setColumns(10);
		
		JButton btnNewButton_1 = new JButton("Output");
		panel_Options.add(btnNewButton_1, "cell 0 2,grow");
		
		textField_DOutput = new JTextField();
		panel_Options.add(textField_DOutput, "cell 1 2,grow");
		textField_DOutput.setColumns(10);
		
		JButton btn_Time = new JButton("Time interval");
		panel_Options.add(btn_Time, "cell 0 3,grow");
		
		txtlongmaxvalue = new JTextField();
		txtlongmaxvalue.setText("0,Long.MAXVALUE");
		panel_Options.add(txtlongmaxvalue, "cell 1 3,grow");
		txtlongmaxvalue.setColumns(10);
		
		JButton btnNewButton_2 = new JButton("Handle");
		panel_Options.add(btnNewButton_2, "cell 0 4,grow");
		
		JButton btn_DataTypes = new JButton("Data types");
		btn_DataTypes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		
		txtall = new JTextField();
		txtall.setText("\"All\" , 32 in binary");
		panel_Options.add(txtall, "cell 1 4,grow");
		txtall.setColumns(10);
		panel_Options.add(btn_DataTypes, "cell 0 5,grow");
		
		txtall_1 = new JTextField();
		txtall_1.setText("\"All\" 1111111111");
		panel_Options.add(txtall_1, "cell 1 5,grow");
		txtall_1.setColumns(10);
		
	}
}
