import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

public class DownloadManager extends JFrame implements Observer {
    private JTextField addTextField;
    private DownloadsTableModel tableModel;
    private JTable table;
    private JButton pauseButton, resumeButton, cancelButton, clearButton;
    private Download selectedDownload;
    private boolean clearing;

    public DownloadManager() {
        setTitle("Internet Programming - GUI Download Manager");
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Panel URL
        JPanel addPanel = new JPanel(new BorderLayout());
        addTextField = new JTextField();
        JButton addButton = new JButton("Download");
        addButton.addActionListener(e -> actionAdd());
        addPanel.add(addTextField, BorderLayout.CENTER);
        addPanel.add(addButton, BorderLayout.EAST);
        addPanel.setBorder(BorderFactory.createTitledBorder("Masukkan URL File"));

        // Panel Tabel Daftar Download
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(e -> tableSelectionChanged());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true);
        table.setDefaultRenderer(JProgressBar.class, renderer);
        table.setRowHeight((int) renderer.getPreferredSize().getHeight());
        
        JPanel downloadsPanel = new JPanel(new BorderLayout());
        downloadsPanel.setBorder(BorderFactory.createTitledBorder("Daftar Unduhan"));
        downloadsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Panel Tombol Kontrol
        JPanel buttonsPanel = new JPanel();
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> actionPause());
        pauseButton.setEnabled(false);
        
        resumeButton = new JButton("Resume");
        resumeButton.addActionListener(e -> actionResume());
        resumeButton.setEnabled(false);
        
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> actionCancel());
        cancelButton.setEnabled(false);
        
        clearButton = new JButton("Clear Selesai");
        clearButton.addActionListener(e -> actionClear());
        clearButton.setEnabled(false);

        buttonsPanel.add(pauseButton);
        buttonsPanel.add(resumeButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(clearButton);

        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void actionAdd() {
        URL verifiedUrl = verifyUrl(addTextField.getText());
        if (verifiedUrl != null) {
            tableModel.addDownload(new Download(verifiedUrl));
            addTextField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Format URL tidak valid!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private URL verifyUrl(String url) {
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) return null;
        try { return URI.create(url).toURL(); } catch (Exception e) { return null; }
    }

    private void tableSelectionChanged() {
        if (selectedDownload != null) selectedDownload.deleteObserver(DownloadManager.this);
        if (!clearing && table.getSelectedRow() > -1) {
            selectedDownload = tableModel.getDownload(table.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateButtons();
        }
    }

    private void actionPause() { selectedDownload.pause(); updateButtons(); }
    private void actionResume() { selectedDownload.resume(); updateButtons(); }
    private void actionCancel() { selectedDownload.cancel(); updateButtons(); }
    private void actionClear() {
        clearing = true;
        tableModel.clearCompleted();
        clearing = false;
        selectedDownload = null;
        updateButtons();
    }

    private void updateButtons() {
        if (selectedDownload != null) {
            int status = selectedDownload.getStatus();
            pauseButton.setEnabled(status == Download.DOWNLOADING);
            resumeButton.setEnabled(status == Download.PAUSED);
            cancelButton.setEnabled(status == Download.DOWNLOADING || status == Download.PAUSED);
            clearButton.setEnabled(status == Download.COMPLETE || status == Download.CANCELLED || status == Download.ERROR);
        } else {
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
            clearButton.setEnabled(false);
        }
    }

    public void update(Observable o, Object arg) {
        if (selectedDownload != null && selectedDownload.equals(o)) {
            SwingUtilities.invokeLater(this::updateButtons);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DownloadManager().setVisible(true));
    }
}

class Download extends Observable implements Runnable {
    private static final int MAX_BUFFER_SIZE = 1024;
    public static final String[] STATUSES = {"Downloading", "Paused", "Complete", "Cancelled", "Error"};
    public static final int DOWNLOADING = 0, PAUSED = 1, COMPLETE = 2, CANCELLED = 3, ERROR = 4;

    private URL url;
    private int size, downloaded, status;

    public Download(URL url) {
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
        download();
    }

    public String getUrl() { return url.toString(); }
    public int getSize() { return size; }
    public float getProgress() { return ((float) downloaded / size) * 100; }
    public int getStatus() { return status; }
    public void pause() { status = PAUSED; stateChanged(); }
    public void resume() { status = DOWNLOADING; stateChanged(); download(); }
    public void cancel() { status = CANCELLED; stateChanged(); }
    private void error() { status = ERROR; stateChanged(); }

    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }

    private String getFileName(URL url) {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
            connection.connect();

            if (connection.getResponseCode() / 100 != 2) { error(); return; }

            int contentLength = connection.getContentLength();
            if (contentLength < 1) { error(); return; }

            if (size == -1) {
                size = contentLength;
                stateChanged();
            }

            file = new RandomAccessFile(getFileName(url), "rw");
            file.seek(downloaded);
            stream = connection.getInputStream();

            while (status == DOWNLOADING) {
                byte[] buffer = new byte[MAX_BUFFER_SIZE];
                int read = stream.read(buffer);
                if (read == -1) break;
                file.write(buffer, 0, read);
                downloaded += read;
                stateChanged();
            }

            if (status == DOWNLOADING) {
                status = COMPLETE;
                stateChanged();
            }
        } catch (Exception e) {
            error();
        } finally {
            if (file != null) { try { file.close(); } catch (Exception e) {} }
            if (stream != null) { try { stream.close(); } catch (Exception e) {} }
        }
    }

    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
}

class DownloadsTableModel extends AbstractTableModel implements Observer {
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