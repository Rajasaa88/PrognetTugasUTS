import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

public class DownloadsTableModel extends AbstractTableModel implements Observer {
    private static final String[] columnNames = {"URL", "Size (MB)", "Progress", "Status"};
    private static final Class<?>[] columnClasses = {String.class, String.class, JProgressBar.class, String.class};
    private final ArrayList<Download> downloadList = new ArrayList<>();

    // ====== Palet warna ======
    static final Color HEADER_BG   = new Color(0x2C3E50);
    static final Color HEADER_FG   = Color.WHITE;
    static final Color ROW_EVEN    = Color.WHITE;
    static final Color ROW_ODD     = new Color(0xF5F7FA);
    static final Color ROW_SELECT  = new Color(0xD6E6FB);
    static final Color TEXT_MAIN   = new Color(0x2D3748);
    static final Color GRID_LINE   = new Color(0xE2E8F0);
    static final Color BAR_ACTIVE  = new Color(0x3B82F6);
    static final Color BAR_DONE    = new Color(0x22C55E);

    /**
     * Terapkan tampilan modern ke JTable. Panggil sekali setelah
     * table.setModel(downloadsTableModel).
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(GRID_LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT_MAIN);
        table.setSelectionBackground(ROW_SELECT);
        table.setSelectionForeground(TEXT_MAIN);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
        header.setDefaultRenderer(new HeaderRenderer());

        // Renderer per kolom
        table.setDefaultRenderer(String.class, new CellRenderer(SwingConstants.LEFT));
        table.setDefaultRenderer(JProgressBar.class, new ProgressRenderer(0, 100));

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(1).setCellRenderer(new CellRenderer(SwingConstants.RIGHT)); // Size
        cm.getColumn(3).setCellRenderer(new StatusRenderer());                   // Status

        // Lebar kolom
        int[] widths = {380, 90, 190, 110};
        for (int i = 0; i < widths.length; i++) {
            cm.getColumn(i).setPreferredWidth(widths[i]);
        }
        cm.getColumn(1).setMaxWidth(120);
        cm.getColumn(3).setMaxWidth(140);

        // Background area kosong di bawah baris
        if (table.getParent() instanceof JViewport) {
            ((JViewport) table.getParent()).setBackground(ROW_EVEN);
        }
    }

    // ====== Model ======
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

    @Override public int getColumnCount() { return columnNames.length; }
    @Override public String getColumnName(int col) { return columnNames[col]; }
    @Override public Class<?> getColumnClass(int col) { return columnClasses[col]; }
    @Override public int getRowCount() { return downloadList.size(); }

    @Override
    public Object getValueAt(int row, int col) {
        Download download = downloadList.get(row);
        switch (col) {
            case 0: return download.getUrl();
            case 1:
                int size = download.getSize();
                return (size == -1) ? "-" : String.format("%.2f", (float) size / 1048576);
            case 2: return Float.valueOf(download.getProgress());
            case 3: return Download.STATUSES[download.getStatus()];
        }
        return "";
    }

    @Override
    public void update(Observable o, Object arg) {
        // Download berjalan di thread lain, jadi update tabel harus lewat EDT
        SwingUtilities.invokeLater(() -> {
            int index = downloadList.indexOf(o);
            if (index >= 0) fireTableRowsUpdated(index, index);
        });
    }
}

/** Renderer progress bar: tampil persen, warna hijau saat selesai. */
class ProgressRenderer extends JProgressBar implements TableCellRenderer {
    public ProgressRenderer(int min, int max) {
        super(min, max);
        setStringPainted(true);
        setBorderPainted(false);
        setOpaque(true);
        setFont(new Font("Segoe UI", Font.BOLD, 11));
        setBorder(new EmptyBorder(8, 10, 8, 10));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        float p = (value instanceof Number) ? ((Number) value).floatValue() : 0f;
        p = Math.max(0f, Math.min(100f, p));
        setValue((int) p);
        setString(String.format("%.0f%%", p));
        setForeground(p >= 100f ? DownloadsTableModel.BAR_DONE : DownloadsTableModel.BAR_ACTIVE);
        setBackground(isSelected ? DownloadsTableModel.ROW_SELECT
                : (row % 2 == 0 ? DownloadsTableModel.ROW_EVEN : DownloadsTableModel.ROW_ODD));
        return this;
    }
}

/** Renderer teks biasa: padding, zebra striping, perataan. */
class CellRenderer extends DefaultTableCellRenderer {
    private final int align;

    CellRenderer(int align) {
        this.align = align;
        setBorder(new EmptyBorder(0, 12, 0, 12));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        setHorizontalAlignment(align);
        setBorder(new EmptyBorder(0, 12, 0, 12));
        setForeground(DownloadsTableModel.TEXT_MAIN);
        setBackground(isSelected ? DownloadsTableModel.ROW_SELECT
                : (row % 2 == 0 ? DownloadsTableModel.ROW_EVEN : DownloadsTableModel.ROW_ODD));
        return this;
    }
}

/** Renderer status: teks tebal berwarna sesuai kondisi. */
class StatusRenderer extends CellRenderer {
    StatusRenderer() { super(SwingConstants.CENTER); }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String s = String.valueOf(value).toLowerCase();
        Color c;
        if (s.contains("complete"))        c = new Color(0x16A34A); // hijau
        else if (s.contains("error"))      c = new Color(0xDC2626); // merah
        else if (s.contains("cancel"))     c = new Color(0x6B7280); // abu-abu
        else if (s.contains("pause"))      c = new Color(0xD97706); // oranye
        else                               c = new Color(0x2563EB); // biru (downloading)
        setForeground(c);
        setFont(getFont().deriveFont(Font.BOLD));
        return this;
    }
}

/** Renderer header tabel. */
class HeaderRenderer extends DefaultTableCellRenderer {
    HeaderRenderer() {
        setOpaque(true);
        setBackground(DownloadsTableModel.HEADER_BG);
        setForeground(DownloadsTableModel.HEADER_FG);
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setBorder(new EmptyBorder(0, 12, 0, 12));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, false, false, row, column);
        setBackground(DownloadsTableModel.HEADER_BG);
        setForeground(DownloadsTableModel.HEADER_FG);
        setHorizontalAlignment(column == 1 ? SwingConstants.RIGHT
                : (column == 3 ? SwingConstants.CENTER : SwingConstants.LEFT));
        return this;
    }
}