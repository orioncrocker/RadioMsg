package com.RadioMSG;

import android.content.Context;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manages a background logcat process that writes the app's own logs to a file.
 * Uses logcat's native -f flag for efficient file output.
 */
public class LogcatLogger {

    private static Process sLogcatProcess;

    public static synchronized void start(Context context) {
        if (sLogcatProcess != null) return; // already running

        File outDir = context.getExternalFilesDir(null);
        if (outDir == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US);
        String filename = "radiomsg_log_" + sdf.format(new Date()) + ".txt";
        File logFile = new File(outDir, filename);

        try {
            int pid = android.os.Process.myPid();
            sLogcatProcess = Runtime.getRuntime().exec(new String[]{
                    "logcat", "--pid=" + pid, "-f", logFile.getAbsolutePath()
            });
        } catch (Exception e) {
            sLogcatProcess = null;
        }
    }

    public static synchronized void stop() {
        if (sLogcatProcess != null) {
            sLogcatProcess.destroy();
            sLogcatProcess = null;
        }
    }

    public static synchronized boolean isRunning() {
        return sLogcatProcess != null;
    }
}
