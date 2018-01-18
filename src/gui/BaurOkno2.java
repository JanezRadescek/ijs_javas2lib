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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import cli.Cli;
import net.miginfocom.swing.MigLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class BaurOkno2 extends JFrame {


	public interface komponenta
	{
		boolean ready();
		void setEnabled(boolean b);
		ArrayList<String> getInfo();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtCliArgs;
	JFileChooser chooser = new JFileChooser();


	private String[] cliArgs;
	@Deprecated
	private HashMap<String,String[]> dArguments = new HashMap<String,String[]>();
	HashMap<String,komponenta[]> components;
	String[] allowedGroups;

	//TODO premakni podatke iz teksta v "gumbe"
	
	private Besedilo textField_MainInput;
	private Besedilo textField_SecondaryInput;
	private Besedilo textField_OutputDire;
	private Besedilo txtHandles;
	private Besedilo textField_OutputName;
	private Besedilo textField_start;
	private Besedilo textField_end;
	ButtonGroup JRadioGRoup;

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
					if(evaluate())
					{
						Cli.main(cliArgs);
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

			components = new HashMap<String,komponenta[]>();
			panel_Options.setLayout(new MigLayout("", "[131px][131px,grow]", "[29px][29px][29px][29px][29px,grow][29px][29px]"));

			{
				Gumb btn_MainInput = new Gumb("Main input");
				btn_MainInput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
						FileNameExtensionFilter filter = new FileNameExtensionFilter(
								"S2 file format", "s2");
						chooser.setFileFilter(filter);

						if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
							textField_MainInput.setText(chooser.getSelectedFile().getPath());
							evaluate();
						}
					}
				});
				btn_MainInput.setEnabled(false);
				panel_Options.add(btn_MainInput, "cell 0 0,grow");
				//components.add(btn_MainInput);

				textField_MainInput = new Besedilo();
				textField_MainInput.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == 10)
						{
							evaluate();
						}
					}
				});
				textField_MainInput.setEnabled(false);
				panel_Options.add(textField_MainInput, "cell 1 0,grow");
				textField_MainInput.setColumns(10);

				components.put("in1",new komponenta[]{btn_MainInput,textField_MainInput});
			}
			{
				Gumb btn_SecondaryInput = new Gumb("Secondary input");
				btn_SecondaryInput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
						FileNameExtensionFilter filter = new FileNameExtensionFilter(
								"S2 file format", "s2");
						chooser.setFileFilter(filter);

						if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
							textField_MainInput.setText(chooser.getSelectedFile().getPath());
							evaluate();
						}
					}
				});
				btn_SecondaryInput.setEnabled(false);
				panel_Options.add(btn_SecondaryInput, "cell 0 1,grow");
				//components.add(btn_SecondaryInput);

				textField_SecondaryInput = new Besedilo();
				textField_SecondaryInput.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == 10)
						{
							evaluate();
						}
					}
				});
				textField_SecondaryInput.setEnabled(false);
				panel_Options.add(textField_SecondaryInput, "cell 1 1,grow");
				textField_SecondaryInput.setColumns(10);

				components.put("in2",new komponenta[]{btn_SecondaryInput,textField_SecondaryInput});
			}

			{
				Gumb btn_Output = new Gumb("Output directory");
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

				textField_OutputDire = new Besedilo();
				textField_OutputDire.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == 10)
						{
							evaluate();
						}
					}
				});
				textField_OutputDire.setEnabled(false);
				panel_Options.add(textField_OutputDire, "cell 1 2,grow");
				textField_OutputDire.setColumns(10);

				//name

				Gumb btn_Name = new Gumb("Output file name");
				btn_Name.setEnabled(false);
				panel_Options.add(btn_Name, "cell 0 3,grow");
				//components.add(btn_Time);

				textField_OutputName = new Besedilo();
				textField_OutputName.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == 10)
						{
							evaluate();
						}
					}
				});
				textField_OutputName.setEnabled(false);
				panel_Options.add(textField_OutputName, "cell 1 3,grow");
				textField_OutputName.setColumns(10);

				components.put("out",new komponenta[]{btn_Output,textField_OutputDire,btn_Name,textField_OutputName});
			}

			{
				Gumb btn_Time = new Gumb("Time interval");
				btn_Time.setEnabled(false);
				panel_Options.add(btn_Time, "cell 0 4,grow");

				JSplitPane splitPane = new JSplitPane();
				splitPane.setResizeWeight(0.5);
				panel_Options.add(splitPane, "cell 1 4,grow");

				textField_start = new Besedilo();
				textField_start.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == 10)
						{
							evaluate();
						}
					}
				});
				textField_start.setEnabled(false);
				splitPane.setLeftComponent(textField_start);
				textField_start.setColumns(10);

				textField_end = new Besedilo();
				textField_end.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == 10)
						{
							evaluate();
						}
					}
				});
				textField_end.setEnabled(false);
				splitPane.setRightComponent(textField_end);
				textField_end.setColumns(10);

				components.put("time",new komponenta[]{btn_Time,textField_start,textField_end});
			}

			{
				Gumb btn_Handles = new Gumb("Handles");
				btn_Handles.setEnabled(false);
				panel_Options.add(btn_Handles, "cell 0 5,grow");
				//components.add(btn_Handles);

				JPanel panel_1 = new JPanel();
				panel_Options.add(panel_1, "cell 1 5,grow");
				panel_1.setLayout(new BorderLayout(0, 0));

				Skatla chckbxAll = new Skatla("All");
				chckbxAll.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						txtHandles.setText(null);
					}
				});
				chckbxAll.setEnabled(false);
				chckbxAll.setSelected(true);
				panel_1.add(chckbxAll, BorderLayout.WEST);
				//components.add(chckbxAll);

				txtHandles = new Besedilo();
				txtHandles.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						chckbxAll.setSelected(false);

						if(e.getKeyCode() == 10)
						{
							evaluate();
						}
					}
				});
				txtHandles.setEnabled(false);
				txtHandles.setText("handles");
				panel_1.add(txtHandles, BorderLayout.CENTER);
				txtHandles.setColumns(10);

				components.put("handles",new komponenta[]{btn_Handles,chckbxAll,txtHandles});
			}

			{
				Gumb btn_DataTypes = new Gumb("Data types");
				btn_DataTypes.setEnabled(false);
				panel_Options.add(btn_DataTypes, "cell 0 6,grow");
				//components.add(btn_DataTypes);

				JPanel panel = new JPanel();
				panel_Options.add(panel, "cell 1 6,grow");
				panel.setLayout(new BorderLayout(0, 0));

				Skatla chckbx_C = new Skatla("Comments");
				chckbx_C.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						evaluate();
					}
				});
				chckbx_C.setEnabled(false);
				chckbx_C.setSelected(true);
				panel.add(chckbx_C, BorderLayout.NORTH);
				//components.add(chckbx_C);

				Skatla chckbx_SM = new Skatla("Special messeges");
				chckbx_SM.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						evaluate();
					}
				});
				chckbx_SM.setEnabled(false);
				chckbx_SM.setSelected(true);
				panel.add(chckbx_SM, BorderLayout.CENTER);
				//components.add(chckbx_SM);

				Skatla chckbx_MD = new Skatla("Meta data");
				chckbx_MD.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						evaluate();
					}
				});
				chckbx_MD.setEnabled(false);
				chckbx_MD.setSelected(true);
				panel.add(chckbx_MD, BorderLayout.SOUTH);
				components.put("data",new komponenta[]{btn_DataTypes,chckbx_C,chckbx_SM,chckbx_MD});
			}
		}

		//Radio Panel
		{
			JPanel panel_radioButtons = new JPanel();
			panel_radioButtons.setBackground(Color.GREEN);
			panel_radioButtons.setForeground(Color.BLACK);
			contentPane.add(panel_radioButtons, BorderLayout.WEST);

			JRadioGRoup = new ButtonGroup();
			panel_radioButtons.setLayout(new MigLayout("", "[71px]", "[51px][51px][51px][51px]"));

			Radijo rdbtn1 = new Radijo("Statistics","-s");
			rdbtn1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					allowedGroups=new String[]{"in1","out"};
					enableButtons();
				}
			});
			panel_radioButtons.add(rdbtn1, "cell 0 0,grow");
			JRadioGRoup.add(rdbtn1);

			Radijo rdbtn2 = new Radijo("CSV","-r");
			rdbtn2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					allowedGroups = new String[]{"in1","out","time","handles"};
					enableButtons();
				}
			});
			panel_radioButtons.add(rdbtn2, "cell 0 1,grow");
			JRadioGRoup.add(rdbtn2); 

			Radijo rdbtn3 = new Radijo("cut S2","-c");
			rdbtn3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					allowedGroups = new String[]{"in1","out","time","handles","data"};
					enableButtons();
				}
			});
			panel_radioButtons.add(rdbtn3, "cell 0 2,grow");
			JRadioGRoup.add(rdbtn3); 

			Radijo rdbtn4 = new Radijo("Merge S2 - New handles","-m", "false");
			rdbtn4.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					allowedGroups = new String[]{"in1","in2","out"};
					enableButtons();
				}
			});
			JRadioGRoup.add(rdbtn4);
			panel_radioButtons.add(rdbtn4, "cell 0 3,grow");

			Radijo rdbtn5 = new Radijo("Merge S2 - Same handles","-m", "true");
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


	private boolean evaluate() {
		
		ArrayList<String> seznam = new ArrayList<String>();
		
		if(JRadioGRoup.getSelection()==null)
		{
			JOptionPane.showMessageDialog(this, "Select action");
			return false;
		}else
		{
			Radijo temp = (Radijo)JRadioGRoup.getSelection();
			seznam.addAll(temp.getInfo());
		}

		for(String grup:allowedGroups)
		{
			for(komponenta haha:components.get(grup))
				if(!haha.ready())
				{
					JOptionPane.showMessageDialog(this, "Select " + grup);
					return false;
				}else
				{
					seznam.addAll(haha.getInfo());
				}
		}
		cliArgs = (String[]) seznam.toArray();
		return true;

	}

	private void enableButtons() {
		for(String skupina:components.keySet())
		{
			for(komponenta comp:components.get(skupina))
			{
				comp.setEnabled(false);
			}
		}
		for(String skupina:allowedGroups)
		{
			for(komponenta comp:components.get(skupina))
			{
				comp.setEnabled(true);
			}
		}

	}
}