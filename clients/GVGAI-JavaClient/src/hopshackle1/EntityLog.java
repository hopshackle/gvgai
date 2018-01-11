package hopshackle1;

import java.io.*;
import java.util.logging.Logger;

public class EntityLog {

    public static String logDir = "C:\\GVGAI\\SimulationLogs";
    public static String newline = System.getProperty("line.separator");
    protected static Logger errorLogger = Logger.getLogger("hopshackle.simulation");
    protected File logFile;
    protected boolean logFileOpen;
    protected FileWriter logWriter;

    public EntityLog(String logFileName) {
        logFile = new File(logDir + File.separator + logFileName + ".txt");
        logFileOpen = false;
    }

    public void log(String message) {
        if (!logFileOpen) {
            try {
                logWriter = new FileWriter(logFile, true);
                logFileOpen = true;
            } catch (IOException e) {
                e.printStackTrace();
                errorLogger.severe("Failed to open logWriter" + e.toString());
                return;
            }
        }

        try {
            logWriter.write(message+newline);
        } catch (IOException e) {
            errorLogger.severe(e.toString());
            e.printStackTrace();
        }
    }

    public void rename(String newName) {
        close();
        logFile.renameTo(new File(logDir + File.separator + newName + ".txt"));
    }

    public void flush() {
        if (logFileOpen) {
            try {
                logWriter.flush();
            } catch (Exception e) {
                errorLogger.severe(e.toString());
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (logFileOpen)
            try {
                logWriter.close();
                logFileOpen = false;
            } catch (Exception e) {
                errorLogger.severe(e.toString());
                e.printStackTrace();
            }
    }
}
