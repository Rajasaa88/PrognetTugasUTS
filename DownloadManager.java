import java.awt.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

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

        JPanel addPanel = new JPanel(new BorderLayout());
        addTextField = new JTextField();
        JButton addButton = new JButton("Download");
        addButton.addActionListener(e -> actionAdd());
        addPanel.add(addTextField, BorderLayout.CENTER);
        addPanel.add(addButton, BorderLayout.EAST);
        addPanel.setBorder(BorderFactory.createTitledBorder("Masukkan URL File"));

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DownloadManager().setVisible(true));
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
}