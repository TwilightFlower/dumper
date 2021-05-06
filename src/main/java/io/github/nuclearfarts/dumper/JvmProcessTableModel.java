package io.github.nuclearfarts.dumper;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class JvmProcessTableModel extends AbstractTableModel {
	private List<JvmProcess> processes = new ArrayList<>(); 
	
	@Override
	public int getRowCount() {
		return processes.size();
	}
	
	@Override
	public int getColumnCount() {
		return 2;
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		JvmProcess proc = processes.get(rowIndex);
		switch(columnIndex) {
		case 0: return proc.target;
		case 1: return proc.pid;
		default: throw new IllegalArgumentException(Integer.toString(columnIndex));
		}
	}
	
	public void setJvms(List<JvmProcess> jvms) {
		processes.clear();
		processes.addAll(jvms);
		fireTableDataChanged();
	}
	
	public JvmProcess getJvm(int i) {
		return processes.get(i);
	}
}
