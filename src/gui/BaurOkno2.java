package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.TextField;
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
import javax.swing.BoxLayout;

public class BaurOkno2 extends JFrame {


	public interface Komponenta
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
	HashMap<String,Komponenta[]> components;
	String[] allowedGroups;


	private Besedilo textField_MainInput;
	private Besedilo textField_SecondaryInput;
	private Besedilo textField_Output;
	private Besedilo textField_Handles;
	//private Besedilo textField_OutputName;
	private Besedilo textField_start;
	private Besedilo textField_end;

	ArrayList<Radijo> skupina;
	ButtonGroup JRadioGRoup;
	Radijo act;

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

		setPreferredSize(new Dimension(780,370));


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

			components = new HashMap<String,Komponenta[]>();
			panel_Options.setLayout(new MigLayout("", "[131px][131px,grow]",
					"[29px,grow][29px,grow][29px,grow][29px,grow][29px,grow][29px,grow]"));

			{
				Gumb btn_MainInput = new Gumb("main input","-i");
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

				components.put("main input",new Komponenta[]{btn_MainInput,textField_MainInput});
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
							textField_SecondaryInput.setText(chooser.getSelectedFile().getPath());
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

				components.put("secondary input",new Komponenta[]{btn_SecondaryInput,textField_SecondaryInput});
			}

			{
				JPanel panel_Output = new JPanel();
				panel_Output.setLayout(new BorderLayout(0, 0));
				panel_Options.add(panel_Output, "cell 0 2,grow");


				Gumb btn_Output = new Gumb("Out directory","-o");
				Gumb btn_Name = new Gumb("Out name");
				Besedilo txt_out = new Besedilo();
				Besedilo txt_name = new Besedilo();
				
				textField_Output = new Besedilo();

				btn_Output.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						chooser.resetChoosableFileFilters();
						if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
							textField_Output.addKid(txt_out);
							txt_out.setText(chooser.getSelectedFile().getPath());
							textField_Output.setText(txt_out.getText() + File.separator + txt_name.getText());
						} 
					}
				});
				btn_Output.setEnabled(false);
				panel_Output.add(btn_Output, BorderLayout.WEST);
				//panel_Options.add(btn_Output, "cell 0 2,grow");

				//components.add(btn_Output);

				btn_Name.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String name = JOptionPane.showInputDialog(panel_Options, "Name of OutPut file. "
								+ "Apropriate extension will be automaticly added.", "Name dialog", JOptionPane.PLAIN_MESSAGE);
						if(name != null)
						{
							textField_Output.addKid(txt_name);
							String prefix = null;
							for(Radijo temp:skupina)
							{
								if(temp.isSelected())
								{
									prefix = temp.getName();
									break;
								}
							}
							switch(prefix)
							{
							case "-s":{
								name = name+".txt";
							}break;
							case "-r":{
								name = name+".csv";
							}break;
							case "-c":{
								name = name+".s2";
							}break;
							case "-m":{
								name = name+".s2";
							}break;
							}

							txt_name.setText(name);
							textField_Output.setText(txt_out.getText() + File.separator + txt_name.getText());
						}

					}
				});
				btn_Name.setEnabled(false);
				panel_Output.add(btn_Name, BorderLayout.EAST);

				textField_Output.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == 10)
						{
							textField_Output.resetKids();
							evaluate();
						}
					}
				});
				textField_Output.setEnabled(false);
				panel_Options.add(textField_Output, "cell 1 2,grow");
				textField_Output.setColumns(10);

				components.put("out",new Komponenta[]{btn_Output, btn_Name, textField_Output});
			}

			{
				Gumb btn_Time = new Gumb("Time interval","-t");
				btn_Time.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						JTextField txt_start = new JTextField();
						JTextField txt_end = new JTextField();
						Skatla timeless = new Skatla("Keep timeless data outside interval");
						Object[] msg = {"Start [s]: ",txt_start ,"End [s]: ",txt_end,timeless};

						JOptionPane.showConfirmDialog(panel_Options, msg,
								"Time dialog", JOptionPane.PLAIN_MESSAGE);

						textField_start.setText(txt_start.getText());
						textField_end.setText(txt_end.getText());

					}
				});
				btn_Time.setEnabled(false);
				panel_Options.add(btn_Time, "cell 0 3,grow");

				JPanel panel_Time = new JPanel();
				panel_Time.setLayout(new BoxLayout(panel_Time, BoxLayout.X_AXIS));
				panel_Options.add(panel_Time, "cell 1 3,grow");


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
				panel_Time.add(textField_start, BorderLayout.WEST);

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
				panel_Time.add(textField_end, BorderLayout.CENTER);

				textField_end.setColumns(10);

				Skatla timeAproximate = new Skatla("keep timeless data");
				timeAproximate.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						timeAproximate.SetInfo(Boolean.toString(timeAproximate.isSelected()));
					}
				});
				timeAproximate.setEnabled(false);
				panel_Time.add(timeAproximate, BorderLayout.EAST);

				components.put("time",new Komponenta[]{btn_Time,textField_start,textField_end, timeAproximate});
			}

			{
				Gumb btn_Handles = new Gumb("Handles","-h");
				btn_Handles.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						//String s = JOptionPane.showInputDialog("Handles separated with coma.");
						String s = (String) JOptionPane.showInputDialog(panel_Options, "Handles separated with coma", "Handles dialog", JOptionPane.PLAIN_MESSAGE, null, null, null);
						if(s!= null)
						{
							try
							{
								int R = 0;
								String[] sep = s.split(",");
								for(String temp:sep)
								{
									R |= 1<<Integer.parseInt(temp);
								}
								textField_Handles.setText(Integer.toBinaryString(R));
							}
							catch(Error e)
							{

							}
						}
					}
				});
				btn_Handles.setEnabled(false);
				panel_Options.add(btn_Handles, "cell 0 4,grow");
				//components.add(btn_Handles);

				JPanel panel_1 = new JPanel();
				panel_Options.add(panel_1, "cell 1 4,grow");
				panel_1.setLayout(new BorderLayout(0, 0));

				Skatla chckbxAll = new Skatla("All");
				chckbxAll.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if(chckbxAll.isSelected())
						{
							textField_Handles.setText("11111111111111111111111111111111");
						}
						else
						{
							textField_Handles.setText("");
						}
					}
				});
				chckbxAll.setEnabled(false);
				chckbxAll.setSelected(true);
				panel_1.add(chckbxAll, BorderLayout.WEST);
				//components.add(chckbxAll);

				textField_Handles = new Besedilo();
				textField_Handles.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						chckbxAll.setSelected(false);

						if(e.getKeyCode() == 10)
						{
							evaluate();
						}
					}
				});
				textField_Handles.setEnabled(false);
				textField_Handles.setText("11111111111111111111111111111111");
				panel_1.add(textField_Handles, BorderLayout.CENTER);
				textField_Handles.setColumns(10);

				components.put("handles",new Komponenta[]{btn_Handles,chckbxAll,textField_Handles});
			}

			{
				Gumb btn_DataTypes = new Gumb("Data types","-d");
				btn_DataTypes.setEnabled(false);
				panel_Options.add(btn_DataTypes, "cell 0 5,grow");
				//components.add(btn_DataTypes);

				JPanel panel = new JPanel();
				panel_Options.add(panel, "cell 1 5,grow");
				panel.setLayout(new BorderLayout(0, 0));

				Skatla chckbx_C = new Skatla("Comments");
				chckbx_C.setEnabled(false);
				chckbx_C.setSelected(true);
				panel.add(chckbx_C, BorderLayout.NORTH);
				//components.add(chckbx_C);

				Skatla chckbx_SM = new Skatla("Special messeges");
				chckbx_SM.setEnabled(false);
				chckbx_SM.setSelected(true);
				panel.add(chckbx_SM, BorderLayout.CENTER);
				//components.add(chckbx_SM);

				Skatla chckbx_MD = new Skatla("Meta data");
				chckbx_MD.setEnabled(false);
				chckbx_MD.setSelected(true);
				panel.add(chckbx_MD, BorderLayout.SOUTH);

				btn_DataTypes.addKid(chckbx_C);
				btn_DataTypes.addKid(chckbx_SM);
				btn_DataTypes.addKid(chckbx_MD);

				components.put("data",new Komponenta[]{btn_DataTypes,chckbx_C,chckbx_SM,chckbx_MD});
			}
		}

		//Radio Panel
		{
			JPanel panel_radioButtons = new JPanel();
			panel_radioButtons.setBackground(Color.GREEN);
			panel_radioButtons.setForeground(Color.BLACK);
			contentPane.add(panel_radioButtons, BorderLayout.WEST);

			JRadioGRoup = new ButtonGroup();
			skupina = new ArrayList<Radijo>();
			panel_radioButtons.setLayout(new MigLayout("", "[71px]", "[51px][51px][51px][51px]"));

			Radijo rdbtn1 = new Radijo("Statistics","-s");
			rdbtn1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					act = rdbtn1;
					allowedGroups=new String[]{"main input","out"};
					enableButtons();
				}
			});
			panel_radioButtons.add(rdbtn1, "cell 0 0,grow");
			JRadioGRoup.add(rdbtn1);
			skupina.add(rdbtn1);

			Radijo rdbtn2 = new Radijo("CSV","-r");
			rdbtn2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					act = rdbtn2;
					allowedGroups = new String[]{"main input","out","time","handles"};
					enableButtons();
				}
			});
			panel_radioButtons.add(rdbtn2, "cell 0 1,grow");
			JRadioGRoup.add(rdbtn2);
			skupina.add(rdbtn2);

			Radijo rdbtn3 = new Radijo("cut S2","-c");
			rdbtn3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					act = rdbtn3;
					allowedGroups = new String[]{"main input","out","time","handles","data"};
					enableButtons();
				}
			});
			panel_radioButtons.add(rdbtn3, "cell 0 2,grow");
			JRadioGRoup.add(rdbtn3); 
			skupina.add(rdbtn3);

			Radijo rdbtn4 = new Radijo("Merge S2 - New handles","-m", "false");
			rdbtn4.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					act = rdbtn4;
					allowedGroups = new String[]{"main input","secondary input","out"};
					enableButtons();
				}
			});
			JRadioGRoup.add(rdbtn4);
			skupina.add(rdbtn4);
			panel_radioButtons.add(rdbtn4, "cell 0 3,grow");

			Radijo rdbtn5 = new Radijo("Merge S2 - Same handles","-m", "true");
			rdbtn5.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					act = rdbtn5;
					allowedGroups = new String[]{"main input","secondary input","out"};
					enableButtons();
				}
			});
			JRadioGRoup.add(rdbtn5);
			skupina.add(rdbtn5);
			panel_radioButtons.add(rdbtn5, "cell 0 4,grow");
		}

	}


	private boolean evaluate() {

		ArrayList<String> seznam = new ArrayList<String>();

		if(act==null)
		{
			JOptionPane.showMessageDialog(this, "Select action");
			return false;
		}else
		{
			seznam.addAll(act.getInfo());
		}

		for(String grup:allowedGroups)
		{
			for(Komponenta haha:components.get(grup))
			{
				if(!haha.ready())
				{
					JOptionPane.showMessageDialog(this, "Select " + grup);
					return false;
				}else
				{
					seznam.addAll(haha.getInfo());
				}
			}
		}
		cliArgs = seznam.toArray(new String[0]);
		String temp = "";
		for(int i=0;i<seznam.size()-1;i++)
		{
			temp += seznam.get(i) + " ";
		}
		temp += seznam.get(seznam.size()-1);
		txtCliArgs.setText(temp);
		return true;

	}

	private void enableButtons() {
		for(String skupina:components.keySet())
		{
			for(Komponenta comp:components.get(skupina))
			{
				comp.setEnabled(false);
			}
		}
		for(String skupina:allowedGroups)
		{
			for(Komponenta comp:components.get(skupina))
			{
				comp.setEnabled(true);
			}
		}
	}
}
