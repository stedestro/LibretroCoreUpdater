import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.omg.CORBA.VersionSpecHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.Window.Type;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Toolkit;

public class MainForm {

	/**
	 * Settings class for the application
	 *
	 */
	private static class LCUSettings {		
		private String filename;
		private String coresDirectory;		

		public LCUSettings(String filename) throws InvalidFileFormatException, IOException {
			// TODO Auto-generated constructor stub
			this.filename = filename;
			File f = new File(filename);
			if (!f.exists())
				createSettings(filename);
			Ini ini = new Ini(f);
			coresDirectory = ini.get("core", "cores_directory");
		}

		public static void createSettings(String filename) throws IOException {
			File f = new File(filename);
			FileWriter fw = new FileWriter(f, false);
			fw.write("[core]\n");
			fw.write("cores_directory = :null:\n");
			fw.close();
		}

		public void saveSettings() throws IOException {
			File f = new File(this.filename);
			FileWriter fw = new FileWriter(f, false);
			fw.write("[core]\n");
			fw.write("cores_directory = " + coresDirectory + "\n");
			fw.close();
		}

		public String getCoresDirectory() {
			return coresDirectory;
		}

		public void setCoresDirectory(String directory) {
			coresDirectory = directory;
		}
	}

	private static class CoresTableModel extends DefaultTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public CoresTableModel(Object[] headers) {
			// TODO Auto-generated constructor stub
			super(headers, 0);
		}

		public void addCoreRow(String row) {
			addRow(row.split(":"));
		}

		public void addTest(String row) {
			addRow(new Object[] { row, "" });
		}

		public boolean isCellEditable(int row, int column) {
			return false;// This causes all cells to be not editable
		}
	}

	private static double retroVersion = 0.1;
	private static final String SEP = System.getProperties().getProperty("file.separator");
	private LCUSettings settings;
	ArrayList<LibretroCore.LibCore> serverCores;
	ArrayList<LibretroCore.LibCore> localCores;
	

	private JFrame frmRetrocoreUpdater;
	private JTextField textFieldCoresDir;
	private JTextField textFieldCoreName;
	private JTable coresTable;
	private CoresTableModel coresTabModel;
	private JTextField textFieldCoreStatus;
	private JTextField textFieldCoreCrc;
	private JTextField textFieldCoreFilesize;
	private JTextField textFieldCoreAvailable;
	private JButton btnUpdateCore;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainForm window = new MainForm();
					window.frmRetrocoreUpdater.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * 
	 * @throws IOException
	 * @throws InvalidFileFormatException
	 */
	public MainForm() throws InvalidFileFormatException, IOException {
		initialize();

		Config.getGlobal().setEscape(false);

		settings = new LCUSettings("./settings.ini");
		if (settings.getCoresDirectory().equals(":null:")) {
			textFieldCoresDir.setText(" Double-click here to set cores directory");
		} else {
			textFieldCoresDir.setText(settings.getCoresDirectory());
		}
		serverCores = new ArrayList<>();
		localCores = new ArrayList<>();		
		
		frmRetrocoreUpdater.setTitle("Libretro Core Updater " + Double.toString(retroVersion));
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmRetrocoreUpdater = new JFrame();
		frmRetrocoreUpdater.setIconImage(Toolkit.getDefaultToolkit().getImage(MainForm.class.getResource("/ressources/ralogo2.png")));
		frmRetrocoreUpdater.setTitle("Retrocore Updater 0.1");
		frmRetrocoreUpdater.setBounds(100, 100, 640, 532);
		frmRetrocoreUpdater.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmRetrocoreUpdater.getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		frmRetrocoreUpdater.getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(new GridLayout(0, 2, 0, 0));

		JPanel panel_3 = new JPanel();
		panel_3.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					try {
						setCoresDirectory();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		panel.add(panel_3);

		JLabel lblCoreDirectory = new JLabel(" Core directory : ");
		panel_3.add(lblCoreDirectory);

		textFieldCoresDir = new JTextField();
		textFieldCoresDir.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					try {
						setCoresDirectory();
						if (JOptionPane.showConfirmDialog(null, "Scan directory?") == JOptionPane.YES_OPTION) {
							scanCoresDirectory();
							if (JOptionPane.showConfirmDialog(null, "Check for update?") == JOptionPane.YES_OPTION) {
								checkForUpdate(true);
							}
						}
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		textFieldCoresDir.setEditable(false);
		panel.add(textFieldCoresDir);
		textFieldCoresDir.setColumns(10);

		JButton btnScanDirectory = new JButton("Scan directory");
		panel.add(btnScanDirectory);

		JButton btnCheckForUpdates = new JButton("Check for updates");
		btnCheckForUpdates.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					checkForUpdate(true);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		panel.add(btnCheckForUpdates);
		btnScanDirectory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(settings.coresDirectory.equals(":null:")) {
					JOptionPane.showMessageDialog(null, "Set the cores directory first");
				} else {
					try {
						scanCoresDirectory();
						
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});

		JPanel panelCore = new JPanel();
		frmRetrocoreUpdater.getContentPane().add(panelCore, BorderLayout.EAST);
		panelCore.setPreferredSize(new Dimension(348, 100));
		panelCore.setLayout(new GridLayout(0, 2, 0, 0));

		JLabel lblCoreName = new JLabel(" Core Name");
		panelCore.add(lblCoreName);

		textFieldCoreName = new JTextField();
		panelCore.add(textFieldCoreName);
		textFieldCoreName.setColumns(10);
		textFieldCoreName.setEditable(false);

		JLabel lblStatus = new JLabel(" Status");
		panelCore.add(lblStatus);

		textFieldCoreStatus = new JTextField();
		panelCore.add(textFieldCoreStatus);
		textFieldCoreStatus.setColumns(10);
		textFieldCoreStatus.setEditable(false);

		JLabel lblCrc = new JLabel(" Crc32");
		panelCore.add(lblCrc);

		textFieldCoreCrc = new JTextField();
		panelCore.add(textFieldCoreCrc);
		textFieldCoreCrc.setColumns(10);
		textFieldCoreCrc.setEditable(false);

		JLabel labelFileSize = new JLabel(" FileSize");
		panelCore.add(labelFileSize);

		textFieldCoreFilesize = new JTextField();
		textFieldCoreFilesize.setColumns(10);
		panelCore.add(textFieldCoreFilesize);
		textFieldCoreFilesize.setEditable(false);

		JLabel labelAvailable = new JLabel(" Available");
		panelCore.add(labelAvailable);

		textFieldCoreAvailable = new JTextField();
		textFieldCoreAvailable.setColumns(10);
		panelCore.add(textFieldCoreAvailable);
		textFieldCoreAvailable.setEditable(false);

		JLabel label_3 = new JLabel("");
		panelCore.add(label_3);

		btnUpdateCore = new JButton("Update Core");
		btnUpdateCore.setEnabled(false);
		btnUpdateCore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// coresTabModel.addTest(settings.getCoresDirectory());
				try {
					downloadCore();
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		panelCore.add(btnUpdateCore);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		frmRetrocoreUpdater.getContentPane().add(scrollPane, BorderLayout.CENTER);

		String[] headers = new String[] { "Name", "Status" };
		coresTabModel = new CoresTableModel(headers);
		coresTable = new JTable(coresTabModel){
		    @Override
		       public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		           Component component = super.prepareRenderer(renderer, row, column);
		           int rendererWidth = component.getPreferredSize().width;
		           TableColumn tableColumn = getColumnModel().getColumn(column);
		           tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
		           return component;
		        }
		    };
		coresTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (coresTable.getRowCount() > 0) {
					int row = coresTable.getSelectedRow();
					showCore((String) coresTabModel.getValueAt(row, 0), (String) coresTabModel.getValueAt(row, 1));
				}
			}
		});
		scrollPane.setViewportView(coresTable);
		coresTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}

	/**
	 * Set the libretro cores directory
	 * 
	 * @throws IOException
	 */
	private void setCoresDirectory() throws IOException {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		JPanel panel = new JPanel();
		if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
			textFieldCoresDir.setText(chooser.getSelectedFile().toString());
			settings.setCoresDirectory(chooser.getSelectedFile().toString());
			settings.saveSettings();			
		}
	}

	/**
	 * Scan the local cores and saves them as s json file
	 * @throws IOException
	 */
	private void scanCoresDirectory() throws IOException {
		String res = LibretroCore.scanLocalCores(settings.coresDirectory);
		FileWriter fw = new FileWriter(new File("./cores.json"));
		fw.write(res);
		fw.close();
		JOptionPane.showMessageDialog(null, "Cores directory scanned");
	}

	/**
	 * Read the local cores from json file NOT USED :::: DELETE
	 * @throws IOException
	 */
	private void loadLocalCores() throws IOException {
		ArrayList<LibretroCore.LibCore> coreList = LibretroCore.loadLocalCores("./cores.json");
		for (LibretroCore.LibCore core : coreList)
			coresTabModel.addCoreRow(core.name + ":" + core.crc32);
	}

	/**
	 * Download index-extended from server
	 * @throws MalformedURLException
	 */
	private void downloadIndexExtended() throws MalformedURLException {
		if (LibretroCore.downloadIndexExtended()) {
			JOptionPane.showMessageDialog(null, "index-extended downloaded");
		} else {
			JOptionPane.showMessageDialog(null, "Error downloading index-extended");
		}
	}

	/**
	 * Compare local and server cores, set up cores table
	 * @param downloadExtended
	 * @throws IOException
	 */
	private void checkForUpdate(boolean downloadExtended) throws IOException {
		if (Files.exists(Paths.get("./cores.json"))) {
			coresTabModel.setRowCount(0);
			ArrayList<String[]> tableStrings = new ArrayList<>();
			localCores = LibretroCore.loadLocalCores("./cores.json");
			if (downloadExtended)
				downloadIndexExtended();
			serverCores = LibretroCore.loadServerCores(true);
			for (LibretroCore.LibCore score : serverCores) {
				String status = "2Not installed";
				for (LibretroCore.LibCore lcore : localCores) {
					if (score.name.equals(lcore.name)) {
						if (score.crc32.equals(lcore.crc32))
							status = "0Updated";
						else
							status = "1Outdated";
						break;
					}
				}
				tableStrings.add(new String[] { score.name, status });
			}
			Collections.sort(tableStrings, new Comparator<String[]>() {
				@Override
				public int compare(String[] o1, String[] o2) {
					int statusComp = o1[1].compareTo(o2[1]);
					if (statusComp != 0) {
						return statusComp;
					} else {
						return o1[0].compareTo(o2[0]);
					}
				}
			});
			for (String[] core : tableStrings)
				coresTabModel.addRow(new String[] { core[0], core[1].substring(1) });
			colorTable();
		} else {
			JOptionPane.showMessageDialog(null, "Scan directory first");
		}
	}

	private void colorTable() {

	}

	/**
	 * Return a specific core from server cores list
	 * @param corename
	 * @return
	 */
	private LibretroCore.LibCore getServerCore(String corename) {
		for (int i = 0; i < serverCores.size(); i++) {
			if (serverCores.get(i).name.equals(corename))
				return serverCores.get(i);
		}
		return new LibretroCore.LibCore();
	}

	/**
	 * Show core information on right panel
	 * @param coreName
	 * @param status
	 */
	private void showCore(String coreName, String status) {
		boolean foundCore = false;
		for (LibretroCore.LibCore core : localCores) {
			if (core.name.equals(coreName)) {
				textFieldCoreName.setText(core.name);
				textFieldCoreStatus.setText(status);
				textFieldCoreCrc.setText(core.crc32);
				textFieldCoreFilesize.setText(core.size);
				if (status.equals("Updated")) {
					textFieldCoreAvailable.setText("Already up-to-date");
					btnUpdateCore.setEnabled(false);
				} else if (status.equals("Outdated")) {
					LibretroCore.LibCore score = getServerCore(coreName);
					textFieldCoreAvailable.setText(score.crc32 + " / " + score.date);
					btnUpdateCore.setEnabled(true);
				}
				foundCore = true;
				break;
			}
		}
		if (!foundCore) {
			// core not installed
			textFieldCoreName.setText(coreName);
			textFieldCoreStatus.setText(status);
			textFieldCoreCrc.setText("Not available");
			textFieldCoreFilesize.setText("Not available");
			LibretroCore.LibCore score = getServerCore(coreName);
			textFieldCoreAvailable.setText(score.crc32 + " / " + score.date);
			btnUpdateCore.setEnabled(true);
		}
	}

	/**
	 * Download and install a core
	 * @throws IOException
	 */
	private void downloadCore() throws IOException {
		String sep = System.getProperties().getProperty("file.separator");
		if (LibretroCore.downloadCore(textFieldCoreName.getText(), settings.coresDirectory + SEP + textFieldCoreName.getText() + ".zip")) {
			String name = textFieldCoreName.getText();
			String filename = settings.coresDirectory + sep + name;
			unZipSingleFile(settings.coresDirectory + sep + name + ".zip", settings.coresDirectory);
			File f = new File(settings.coresDirectory + sep + name + ".zip");
			f.delete();
			LibretroCore.LibCore newCore = LibretroCore.scanCore(filename, name);
			boolean foundCore = false;
			for (LibretroCore.LibCore core : localCores) {
				if (core.name.equals(newCore.name)) {
					core.crc32 = newCore.crc32;
					core.size = newCore.size;
					foundCore = true;
					break;
				}
			}
			if(!foundCore) {
				LibretroCore.LibCore core = new LibretroCore.LibCore();
				core.name = newCore.name;
				core.crc32 = newCore.crc32;
				core.size = newCore.size;
				localCores.add(core);
			}
			saveLocalCores();
			checkForUpdate(false);
			JOptionPane.showMessageDialog(null, "Core " + name +  " updated");
			coresTable.requestFocus();
			coresTable.changeSelection(0, 0, false, false);
		} else {
			JOptionPane.showMessageDialog(null, "Error downloading " + textFieldCoreName.getText());
		}
	}

	/**
	 * Save local cores list to json file
	 * @throws IOException
	 */
	private void saveLocalCores() throws IOException {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting().serializeNulls();
		Gson gson = builder.create();
		String res = gson.toJson(localCores);
		FileWriter fw = new FileWriter(new File("./cores.json"));
		fw.write(res);
		fw.close();
	}

	/**
	 * Unzip only the first file of a zip file
	 * @param file
	 * @param dest
	 * @throws IOException
	 */
	private void unZipSingleFile(String file, String dest) throws IOException {
		byte[] buffer = new byte[1024];
		ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
		ZipEntry entry = zis.getNextEntry();
		String filename = entry.getName();
		File f = new File(dest + SEP + filename);
		FileOutputStream fos = new FileOutputStream(f);
		int len = 0;
		while ((len = zis.read(buffer)) > 0) {
			fos.write(buffer, 0, len);
		}
		fos.close();
		zis.closeEntry();
		zis.close();
	}
}
