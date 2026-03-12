package com.RadioMSG;

import android.Manifest;
import android.os.Handler;
import android.os.Looper;
//import android.support.v4.content.ContextCompat;

public class uiTools {


    static public void update() {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Code here will run in UI thread
                //ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == granted;
            }
        });
    }
}
