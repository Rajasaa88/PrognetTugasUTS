import java.io.*;
import java.net.*;
import java.util.*;

public class Download extends Observable implements Runnable {
    private static final int MAX_BUFFER_SIZE = 8192;
    private static final long NOTIFY_INTERVAL_MS = 100; // batasi frekuensi update UI

    public static final String[] STATUSES = {"Downloading", "Paused", "Complete", "Cancelled", "Error"};
    public static final int DOWNLOADING = 0, PAUSED = 1, COMPLETE = 2, CANCELLED = 3, ERROR = 4;

    private final URL url;
    private volatile int size, downloaded, status;

    public Download(URL url) {
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
        download();
    }

    public String getUrl() { return url.toString(); }
    public int getSize() { return size; }

    public float getProgress() {
        if (size <= 0) return 0f;
        return ((float) downloaded / size) * 100;
    }

    public int getStatus() { return status; }
    public void pause() { status = PAUSED; stateChanged(); }
    public void resume() { status = DOWNLOADING; stateChanged(); download(); }
    public void cancel() { status = CANCELLED; stateChanged(); }
    private void error() { status = ERROR; stateChanged(); }

    private void download() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    private String getFileName(URL url) {
        String path = url.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.isEmpty() ? "download.bin" : name;
    }

    @Override
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
            connection.connect();

            int code = connection.getResponseCode();
            if (code / 100 != 2) { error(); return; }

            // Jika server mengabaikan Range (balas 200), mulai ulang dari awal
            if (code == HttpURLConnection.HTTP_OK) downloaded = 0;

            int contentLength = connection.getContentLength();
            if (contentLength < 1) { error(); return; }

            if (size == -1) {
                size = contentLength;
                stateChanged();
            }

            file = new RandomAccessFile(getFileName(url), "rw");
            file.seek(downloaded);
            stream = connection.getInputStream();

            byte[] buffer = new byte[MAX_BUFFER_SIZE];
            long lastNotify = 0;
            while (status == DOWNLOADING) {
                int read = stream.read(buffer);
                if (read == -1) break;
                file.write(buffer, 0, read);
                downloaded += read;

                long now = System.currentTimeMillis();
                if (now - lastNotify >= NOTIFY_INTERVAL_MS) {
                    lastNotify = now;
                    stateChanged();
                }
            }

            if (status == DOWNLOADING) {
                status = COMPLETE;
            }
            stateChanged(); // update terakhir (progress & status final)
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