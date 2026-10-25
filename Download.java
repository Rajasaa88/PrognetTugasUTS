import java.io.*;
import java.net.*;
import java.util.*;

public class Download extends Observable implements Runnable {
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