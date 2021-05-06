package io.github.nuclearfarts.dumper;

import java.awt.Container;
import java.awt.Dimension;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Gui {
	private final ExecutorService workerThread = Executors.newSingleThreadExecutor();
	
	private final JFrame frame = new JFrame("Class Dumper");
	private final JvmProcessTableModel tableModel = new JvmProcessTableModel();
	private final JTable jvmTable = new JTable(tableModel);
	private final JTextField dumpLocationField = new JTextField();
	private final JTextField classToDumpField = new JTextField();
	private final JButton dumpButton = new JButton("Dump");
	private final JButton reloadButton = new JButton("Reload List");
	
	public Gui() {
		tableModel.setJvms(JvmProcess.getRunningJvms());
		dumpButton.addActionListener(a -> {
			String dumpLoc = dumpLocationField.getText();
			String dumpClass = classToDumpField.getText();
			JvmProcess attached = tableModel.getJvm(jvmTable.getSelectedRow());
			if(attached != null) {
				workerThread.submit(() -> {
					AgentConnection conn = AgentConnection.get(attached);
					conn.askDump(dumpClass, Paths.get(dumpLoc).toAbsolutePath().toString());
					try {
						conn.awaitDump();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				});
			}
		});
		reloadButton.addActionListener(a -> {
			workerThread.submit(() -> {
				List<JvmProcess> jvms = JvmProcess.getRunningJvms();
				SwingUtilities.invokeLater(() -> {
					tableModel.setJvms(jvms);
				});
			});
		});
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
		makeLessRidiculous(classToDumpField);
		makeLessRidiculous(dumpLocationField);
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));
		panel1.add(new JLabel("Dump Location"));
		panel1.add(dumpLocationField);
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.LINE_AXIS));
		panel2.add(new JLabel("Class To Dump"));
		panel2.add(classToDumpField);
		JPanel panel3 = new JPanel();
		panel3.setLayout(new BoxLayout(panel3, BoxLayout.LINE_AXIS));
		panel3.add(reloadButton);
		panel3.add(dumpButton);
		Container c = frame.getContentPane();
		c.add(jvmTable);
		c.add(panel1);
		c.add(panel2);
		c.add(panel3);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 300);
		frame.setVisible(true);
	}
	
	private void makeLessRidiculous(JTextField field) {
		field.setMaximumSize(new Dimension(field.getMaximumSize().width, field.getPreferredSize().height));
	}
}
