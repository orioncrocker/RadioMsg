/*
 * This is the error reporting section based on Google's ACRA documentation.
 * It allows the sending of an email, under user's control and review, to the 
 * author of the application with stack trace and Logcat information for 
 * debugging purposes.
 */

package com.RadioMSG;

//error reporting
import org.acra.*;
import org.acra.annotation.*;
import android.app.Application;
import android.preference.PreferenceManager;

//import com.squareup.leakcanary.LeakCanary;

// Error reporting: un-comment the following
@ReportsCrashes(formKey="",
mailTo = "vk2eta@gmail.com",
customReportContent = { ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT },                
mode = ReportingInteractionMode.TOAST,
forceCloseDialogAfterToast = true, // optional, default false
resToastText = R.string.txt_crash_toast_text)


    public class RadioMSGDebug extends Application {
    

	@Override
    public void onCreate() {
        // Initialization of ACRA error reporting
        super.onCreate();

        /* only when debugging Memory leaks
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
        // Normal app init code...
        */

         // Init config
        RadioMSG.mysp = PreferenceManager.getDefaultSharedPreferences(this);
        //Only for attended apps (disable if unattended)
        boolean unattended = config.getPreferenceB("UNATTENDEDRELAY", false); //UNATTENDEDRELAY
        if (!unattended) {
            ACRA.init(this);
        }
    }
}