import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

public class DownloadsTableModel extends AbstractTableModel implements Observer {
    private static final String[] columnNames = {"URL", "Size (MB)", "Progress", "Status"};
    private static final Class[] columnClasses = {String.class, String.class, JProgressBar.class, String.class};
    private ArrayList<Download> downloadList = new ArrayList<>();

    public void addDownload(Download download) {
        download.addObserver(this);
        downloadList.add(download);
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
    }

    public Download getDownload(int row) { return downloadList.get(row); }

    public void clearCompleted() {
        for (int i = downloadList.size() - 1; i >= 0; i--) {
            int status = downloadList.get(i).getStatus();
            if (status == Download.COMPLETE || status == Download.CANCELLED || status == Download.ERROR) {
                downloadList.remove(i);
                fireTableRowsDeleted(i, i);
            }
        }
    }

    public int getColumnCount() { return columnNames.length; }
    public String getColumnName(int col) { return columnNames[col]; }
    public Class getColumnClass(int col) { return columnClasses[col]; }
    public int getRowCount() { return downloadList.size(); }

    public Object getValueAt(int row, int col) {
        Download download = downloadList.get(row);
        switch (col) {
            case 0: return download.getUrl();
            case 1: 
                int size = download.getSize();
                return (size == -1) ? "" : String.format("%.2f", (float) size / 1048576);
            case 2: return new Float(download.getProgress());
            case 3: return Download.STATUSES[download.getStatus()];
        }
        return "";
    }

    public void update(Observable o, Object arg) {
        int index = downloadList.indexOf(o);
        fireTableRowsUpdated(index, index);
    }
}

class ProgressRenderer extends JProgressBar implements TableCellRenderer {
    public ProgressRenderer(int min, int max) { super(min, max); }
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setValue((int) ((Float) value).floatValue());
        return this;
    }
}