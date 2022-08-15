package client;

import java.io.*;

public class MessagesLog {
    private File logFile;
    private Writer out;
    private Reader in;

    public MessagesLog(String login){
        logFile = new File(String.format("chat_client%ssrc%smain%sresources%slog_%s.txt", File.separator,
                File.separator, File.separator, File.separator, login));
        try {
            out = new BufferedWriter(new FileWriter(logFile, true));
            in = new BufferedReader(new FileReader(logFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StringBuffer read(){
        int n;
        StringBuffer log = new StringBuffer();
        try {
            while ((n = in.read()) != -1) {
                log.append((char)n);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return log;
    }

    public StringBuffer read(int n) {
        StringBuffer log = read();
        int point = log.length() - 1;
        int countOfNewLines = 0;
        while (countOfNewLines <= n && point != -1) {
            point = log.lastIndexOf("\n", point - 1);
            countOfNewLines++;
        }
        if (point != -1) {
            log.delete(0, point);
        }
        return log;
    }

    public void write(String message) {
        try {
            out.write(String.format("%s%n", message));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}