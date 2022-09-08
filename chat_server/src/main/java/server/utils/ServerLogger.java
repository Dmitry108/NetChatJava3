package server.utils;

import java.io.IOException;
import java.util.Date;
import java.util.logging.*;

public class ServerLogger {
    private static final Logger log;
    static {
        log = Logger.getLogger("ServerLogger");
        log.setLevel(Level.ALL);
        try {
            Handler handler = new FileHandler("chat_server/src/main/resources/server_logs.log");
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("%s\t%s\t%s\t%s%n",
                            record.getLevel(), new Date(record.getMillis()),
                            record.getMessage(), record.getSourceClassName());
                }
            });
            log.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void severe(String message) {
        log.severe(message);
    }

    public static void finer(String message) {
        log.finer(message);
    }

    public static void warning(String message) {
        log.warning(message);
    }
}
