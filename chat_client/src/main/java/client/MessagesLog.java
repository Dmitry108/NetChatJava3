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

//    вариант получения произвольного количества последних записей при помощи RandomAccessFile
//    работает медленнее
//    public StringBuffer readrr(int n){
////        StringBuffer sb = new StringBuffer();
////        if (logFile.length() == 0) return sb;
//        int c;
//        lineCount
//        while ((c = in.read()) != -1 || )
//        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
//            //цикл проходится по файлу с конца, пока не прочитает 100 строчек
//            //k - счетчик встретившихся символов новой строки
//            byte c[] = new byte[(int)logFile.length()];
//            for(int i=(int)raf.length()-2, k=0; i>=0; i--){
//                raf.seek(i);
//                if ((c[i]=(byte)raf.read())=='\n') if (++k==n) break;
//            }
//            sb = new StringBuffer(new String(c, "UTF-8"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return sb;
//    }

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