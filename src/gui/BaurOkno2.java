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
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import java.awt.GridLayout;
import javax.swing.JLabel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class BaurOkno2 extends JFrame {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtCliArgs;
	JFileChooser chooser = new JFileChooser();

	@Deprecated
	private HashMap<String,String[]> cliArgs = new HashMap<String,String[]>();
	@Deprecated
	private HashMap<String,String[]> dArguments = new HashMap<String,String[]>();
	HashMap<String,JComponent[]> components;
	String[] allowedGroups;

	private JTextField textField_MainInput;
	private JTextField textField_SecondaryInput;
	private JTextField textField_OutputDire;
	private JTextField txtHandles;
	private JTextField textField_OutputName;
	private JTextField textField_start;
	private JTextField textField_end;

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
		chooser.setAcceptAllFileFilterUsed(false);

		//default falues
		{
			dArguments.put("time",new String[]{"0 " + (60*60)});
			dArguments.put("handles", new String[]{Long.toString(Long.MAX_VALUE)});
			dArguments.put("handles", new String[]{Long.toString(Long.MAX_VALUE)});
		}



		//MenuBar
		{
			JMenuBar menuBar = new JMenuBar();
			setJMenuBar(menuBar);

			JMenu mnNewMenu = new JMenu("file");
			menuBar.add(mnNewMenu);

			JMenuItem mntmSave = new JMenuItem("save");
			mnNewMenu.add(mntmSave);

			JMenu mnHelp = new JMenu("Help");
			menuBar.add(mnHelp);

			JMenuItem mntmDocumentation = new JMenuItem("Documentation");
			mnHelp.add(mntmDocumentation);
		}
		//Panel
		{
			contentPane = new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPane.setBackground(Color.red);
			setContentPane(contentPane);
			contentPane.setLayout(new BorderLayout(0, 0));
		}



		//arguments + go
		{
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
					ArrayList<String> temp1 = new ArrayList<String>();
					if(allowedGroups != null)
					{
						for(String group:allowedGroups)
						{
							cliArgs.get(group);
						}
					}
				}
			});
			splitPane_South.setRightComponent(btnNewButton);
		}


		//Options
		{
			JPanel panel_Options = new JPanel();
			panel_Options.setBackground(Color.CYAN);
			contentPane.add(panel_Options, BorderLayout.CENTER);

			components = new HashMap<String,JComponent[]>();
			panel_Options.setLayout(new MigLayout("", "[131px][131px,grow]", "[29px][29px][29px][29px][29px,grow][29px][29px]"));

			{
				JButton btn_MainInput = new JButton("Main input");
				btn_MainInput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
						FileNameExtensionFilter filter = new FileNameExtensionFilter(
						        "S2 file format", "s2");
						chooser.setFileFilter(filter);

						if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
							textField_MainInput.setText(chooser.getSelectedFile().getPath());
						}
					}
				});
				btn_MainInput.setEnabled(false);
				panel_Options.add(btn_MainInput, "cell 0 0,grow");
				//components.add(btn_MainInput);

				textField_MainInput = new JTextField();
				textField_MainInput.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == 10)
						{
							upDate();
						}
					}
				});
				textField_MainInput.setEnabled(false);
				panel_Options.add(textField_MainInput, "cell 1 0,grow");
				textField_MainInput.setColumns(10);

				components.put("in1",new JComponent[]{btn_MainInput,textField_MainInput});
			}
			{
				JButton btn_SecondaryInput = new JButton("Secondary input");
				btn_SecondaryInput.setEnabled(false);
				panel_Options.add(btn_SecondaryInput, "cell 0 1,grow");
				//components.add(btn_SecondaryInput);

				textField_SecondaryInput = new JTextField();
				textField_SecondaryInput.setEnabled(false);
				panel_Options.add(textField_SecondaryInput, "cell 1 1,grow");
				textField_SecondaryInput.setColumns(10);

				components.put("in2",new JComponent[]{btn_SecondaryInput,textField_SecondaryInput});
			}

			{
				JButton btn_Output = new JButton("Output directory");
				btn_Output.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						
						if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
							textField_OutputDire.setText(chooser.getSelectedFile().getPath());
						} else {
							System.out.println("No Selection ");
						}
					}
				});
				btn_Output.setEnabled(false);
				panel_Options.add(btn_Output, "cell 0 2,grow");
				//components.add(btn_Output);

				textField_OutputDire = new JTextField();
				textField_OutputDire.setEnabled(false);
				panel_Options.add(textField_OutputDire, "cell 1 2,grow");
				textField_OutputDire.setColumns(10);

				//name

				JButton btn_Name = new JButton("Output file name");
				btn_Name.setEnabled(false);
				panel_Options.add(btn_Name, "cell 0 3,grow");
				//components.add(btn_Time);

				textField_OutputName = new JTextField();
				textField_OutputName.setEnabled(false);
				panel_Options.add(textField_OutputName, "cell 1 3,grow");
				textField_OutputName.setColumns(10);

				components.put("out",new JComponent[]{btn_Output,textField_OutputDire,btn_Name,textField_OutputName});
			}

			{
				JButton btn_Time = new JButton("Time interval");
				btn_Time.setEnabled(false);
				panel_Options.add(btn_Time, "cell 0 4,grow");

				JSplitPane splitPane = new JSplitPane();
				splitPane.setResizeWeight(0.5);
				panel_Options.add(splitPane, "cell 1 4,grow");

				textField_start = new JTextField();
				textField_start.setEnabled(false);
				splitPane.setLeftComponent(textField_start);
				textField_start.setColumns(10);

				textField_end = new JTextField();
				textField_end.setEnabled(false);
				splitPane.setRightComponent(textField_end);
				textField_end.setColumns(10);

				components.put("time",new JComponent[]{btn_Time,textField_start,textField_end});
			}

			{
				JButton btn_Handles = new JButton("Handles");
				btn_Handles.setEnabled(false);
				panel_Options.add(btn_Handles, "cell 0 5,grow");
				//components.add(btn_Handles);

				JPanel panel_1 = new JPanel();
				panel_Options.add(panel_1, "cell 1 5,grow");
				panel_1.setLayout(new BorderLayout(0, 0));

				JCheckBox chckbxAll = new JCheckBox("All");
				chckbxAll.setEnabled(false);
				chckbxAll.setSelected(true);
				panel_1.add(chckbxAll, BorderLayout.WEST);
				//components.add(chckbxAll);

				txtHandles = new JTextField();
				txtHandles.setEnabled(false);
				txtHandles.setText("handles");
				panel_1.add(txtHandles, BorderLayout.CENTER);
				txtHandles.setColumns(10);

				components.put("handles",new JComponent[]{btn_Handles,chckbxAll,txtHandles});
			}

			{
				JButton btn_DataTypes = new JButton("Data types");
				btn_DataTypes.setEnabled(false);
				panel_Options.add(btn_DataTypes, "cell 0 6,grow");
				//components.add(btn_DataTypes);

				JPanel panel = new JPanel();
				panel_Options.add(panel, "cell 1 6,grow");
				panel.setLayout(new BorderLayout(0, 0));

				JCheckBox chckbx_C = new JCheckBox("Comments");
				chckbx_C.setEnabled(false);
				chckbx_C.setSelected(true);
				panel.add(chckbx_C, BorderLayout.NORTH);
				//components.add(chckbx_C);

				JCheckBox chckbx_SM = new JCheckBox("Special messeges");
				chckbx_SM.setEnabled(false);
				chckbx_SM.setSelected(true);
				panel.add(chckbx_SM, BorderLayout.CENTER);
				//components.add(chckbx_SM);

				JCheckBox chckbx_MD = new JCheckBox("Meta data");
				chckbx_MD.setEnabled(false);
				chckbx_MD.setSelected(true);
				panel.add(chckbx_MD, BorderLayout.SOUTH);
				components.put("data",new JComponent[]{btn_DataTypes,chckbx_C,chckbx_SM,chckbx_MD});
			}
		}

		//Radio Panel
		{
			JPanel panel_radioButtons = new JPanel();
			panel_radioButtons.setBackground(Color.GREEN);
			panel_radioButtons.setForeground(Color.BLACK);
			contentPane.add(panel_radioButtons, BorderLayout.WEST);

			ButtonGroup JRadioGRoup = new ButtonGroup();
			panel_radioButtons.setLayout(new MigLayout("", "[71px]", "[51px][51px][51px][51px]"));

			JRadioButton rdbtn1 = new JRadioButton("Statistics");
			rdbtn1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					allowedGroups=new String[]{"in1","out"};
					enableButtons();
				}
			});
			panel_radioButtons.add(rdbtn1, "cell 0 0,grow");
			JRadioGRoup.add(rdbtn1);

			JRadioButton rdbtn2 = new JRadioButton("CSV");
			rdbtn2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					allowedGroups = new String[]{"in1","out","time","handles"};
					enableButtons();
				}
			});
			panel_radioButtons.add(rdbtn2, "cell 0 1,grow");
			JRadioGRoup.add(rdbtn2); 

			JRadioButton rdbtn3 = new JRadioButton("cut S2");
			rdbtn3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					allowedGroups = new String[]{"in1","out","time","handles","data"};
					enableButtons();
				}
			});
			panel_radioButtons.add(rdbtn3, "cell 0 2,grow");
			JRadioGRoup.add(rdbtn3); 

			JRadioButton rdbtn4 = new JRadioButton("Merge S2 - New handles");
			rdbtn4.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					allowedGroups = new String[]{"in1","in2","out"};
					enableButtons();
				}
			});
			JRadioGRoup.add(rdbtn4);
			panel_radioButtons.add(rdbtn4, "cell 0 3,grow");

			JRadioButton rdbtn5 = new JRadioButton("Merge S2 - Same handles");
			rdbtn5.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					allowedGroups = new String[]{"in1","in2","out"};
					enableButtons();
				}
			});
			JRadioGRoup.add(rdbtn5);
			panel_radioButtons.add(rdbtn5, "cell 0 4,grow");
		}

	}
	
	private void upDate() {
		// TODO Auto-generated method stub
		
	}

	private void enableButtons() {
		for(String skupina:components.keySet())
		{
			for(JComponent comp:components.get(skupina))
			{
				comp.setEnabled(false);
			}
		}
		for(String skupina:allowedGroups)
		{
			for(JComponent comp:components.get(skupina))
			{
				comp.setEnabled(true);
			}
		}

	}
}
