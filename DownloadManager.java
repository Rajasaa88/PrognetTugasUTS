import java.awt.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class DownloadManager extends JFrame implements Observer {
    private JTextField addTextField;
    private DownloadsTableModel tableModel;
    private JTable table;
    private JButton pauseButton, resumeButton, cancelButton, clearButton;
    private Download selectedDownload;
    private boolean clearing;

    public DownloadManager() {
        setTitle("Internet Programming - GUI Download Manager");
        setSize(860, 460);
        setMinimumSize(new Dimension(640, 360));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // ===== Panel input URL =====
        JPanel addPanel = new JPanel(new BorderLayout(8, 0));
        addTextField = new JTextField();
        addTextField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addTextField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xCBD5E1), 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        addTextField.addActionListener(e -> actionAdd()); // tekan Enter = Download

        JButton addButton = new JButton("Download");
        stylePrimaryButton(addButton);
        addButton.addActionListener(e -> actionAdd());

        addPanel.add(addTextField, BorderLayout.CENTER);
        addPanel.add(addButton, BorderLayout.EAST);
        addPanel.setBorder(titledPanelBorder("Masukkan URL File"));

        // ===== Tabel =====
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(e -> tableSelectionChanged());
        DownloadsTableModel.styleTable(table); // <-- tampilan tabel profesional

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(0xE2E8F0)));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel downloadsPanel = new JPanel(new BorderLayout());
        downloadsPanel.setBorder(titledPanelBorder("Daftar Unduhan"));
        downloadsPanel.add(scrollPane, BorderLayout.CENTER);

        // ===== Tombol aksi =====
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
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

        for (JButton b : new JButton[]{pauseButton, resumeButton, cancelButton, clearButton}) {
            b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            b.setPreferredSize(new Dimension(115, 34));
            b.setFocusPainted(false);
            buttonsPanel.add(b);
        }

        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    // ===== Helper tampilan =====
    private static Border titledPanelBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                new LineBorder(new Color(0xE2E8F0), 1, true), title);
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 13));
        tb.setTitleColor(DownloadsTableModel.HEADER_BG);
        return BorderFactory.createCompoundBorder(new EmptyBorder(8, 12, 4, 12),
                BorderFactory.createCompoundBorder(tb, new EmptyBorder(6, 8, 8, 8)));
    }

    private static void stylePrimaryButton(JButton b) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(DownloadsTableModel.BAR_ACTIVE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(8, 20, 8, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void main(String[] args) {
        // Nimbus: tampilan lebih modern & warna progress bar tampil benar
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new DownloadManager().setVisible(true));
    }

    private void actionAdd() {
        URL verifiedUrl = verifyUrl(addTextField.getText().trim());
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

    @Override
    public void update(Observable o, Object arg) {
        if (selectedDownload != null && selectedDownload.equals(o)) {
            SwingUtilities.invokeLater(this::updateButtons);
        }
    }
}