/*
 * RadioMSG.java
 *
 * Copyright (C) 2014 John Douyere (VK2ETA)
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.RadioMSG;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

//Support library for runtime permissions and notifications
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
//import android.support.v7.widget.LinearLayoutCompat;
import androidx.appcompat.widget.LinearLayoutCompat;

//import android.support.v4.app.ActivityCompat;
import androidx.core.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
import androidx.core.content.ContextCompat;
//import android.support.v4.app.NotificationCompat;
import androidx.core.app.NotificationCompat;
//import android.support.v4.app.TaskStackBuilder;
import androidx.core.app.TaskStackBuilder;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.util.MailSSLSocketFactory;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;



public class RadioMSG extends AppCompatActivity {

    public static Context myContext;

    public static RadioMSG myInstance = null;

    private static boolean havePassedAllPermissionsTest = false;

    private Menu mOptionsMenu = null;

    public static boolean RXParamsChanged = false;

    public static SharedPreferences mysp = null;

    // Are we in an individual form display screen?
    private static boolean inFormDisplay = false;
    //Have we just deleted that message displayed in the popup
    private static boolean hasDeletedMessage = false;

    //Are we in a Voide dialog popup dialog?
    private static boolean voiceDialogOpened = false;
    private AlertDialog mExitDialog;

    private static String savedSmsMessage = "";

    //private static String welcomeString = "\n Welcome to RadioMSG "
    //        + RMsgProcessor.version
    //        + "\n\n This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  \n]\nSwipe across the screen to navigate to the other screens and use the device Menu button to get access to the settings and additional functions.\n\n";
    //private static boolean hasDisplayedWelcome = false;

    // Horizontal Fling detection despite scrollview
    private GestureDetector mGesture;
    // screen transitions / animations
    private static final int NORMAL = 0;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int TOP = 3;
    private static final int BOTTOM = 4;

    // Views values
    private final static int POPUPVIEW = -1;
    private final static int TERMVIEW = 1;
    private final static int MODEMVIEWnoWF = 2;
    public final static int MODEMVIEWwithWF = 3;
    public final static int SMSVIEW = 4;
    private final static int SMSQUICKBUTTONVIEW = 5;
    private final static int SMSQUICKLISTVIEW = 6;
    private final static int MMSVIEW = 7;
    //private final static int SELCALLVIEW = 8;
    private final static int ABOUTVIEW = 9;
    //private final static int LOGSVIEW = 10;
    private final static int COMMANDSVIEW = 11;

    //Number of swipe-able screens in preferences and current index
    private static int[] swipeableScreens;
    private static int swipableScreensCount = 0;
    private static int swipableScreensIndex = 0; //Start on messages screen

    static ProgressBar SignalQuality = null;
    static ProgressBar CpuLoad = null;
    static ProgressBar AudioLevel = null;
    static int lastTxCount = 0;

    public static int currentview = 0;
    private int smsViewOption = 1; //To display extra buttons and selections on the SMS view
    public static int speakerOption = 0; //To enable audio from receiver to speaker routing - designed for monitoring receiver sound on speaker-less transceivers
    public static long speakerOffDelay = 0L; //To close the speaker audio after X seconds to hear the ack beeps or RSID

    // Toast text display (declared here to allow for quick replacement rather
    // than queuing)
    private static Toast myToast;
    private static Toast longToast;
    //To have a double back key press out of the data entry forms
    private static int backKeyPressCount = 0;
    private static long lastBackKeyTime = 0;

    // Forms display and forward
    private static String mFileName = new String();


    public static boolean trackPOI = false;
    public static int closestPoi = -1;

    // Layout Views
    private static TextView myTermTV;
    private static ScrollView myTermSC;
    private static TextView myModemTV;
    private static ScrollView myModemSC;
    private static waterfallView myWFView;

    private static TextView modemModeView;

    private static View pwLayout;

    // Array adapter for the message list
    public static RMsgDisplayList msgDisplayList;
    public static boolean updatingMsgListAdapter = false;
    ListView msgListView;
    ListView smsQuickListView;
    ListView manageView;

    private static int firstVisibleSmsItem = -1;

    public static double waterfallDynamic = 1.0;


    // Generic button variable. Just for callback initialisation
    private Button myButton;

    private CheckBox checkbox = null;
    static final String[] opModes = new String[]{"HF-Poor", "HF-Good", "HF-Fast", "UHF-Poor", "UHF-Good", "UHF-Fast"};
    static final String[] defaultModes = new String[]{"OLIVIA_8_500", "OLIVIA_8_1000", "OLIVIA_8_2000", "MFSK32", "MFSK64", "MFSK128"};
    //Keep a reference to the spinner so that we can change its colour
    static private Spinner toDropdown = null;
    static public boolean sendAliasAndDetails = false;
    //To field spinner selection
    public static String selectedTo = "*";
    public static String selectedToAlias = ""; //Aliases for sms or email destinations
    public static String selectedVia = "";
    public static String selectedViaPassword = ""; //For relays with password required
    public static String selectedViaIotPassword = ""; //For relays with password required for IOT commands
    public static String[] toArray; //Stores array of To stations/email addresses/Cellular numbers
    public static String[] toAliasArray; //Stores array of To stations' aliases if any in the same order as toArray
    public static String[] viaArray; //Stores array of relay (via) stations
    public static String[] viaPasswordArray; //Stores array of VIA stations' passwords, if any, in the same order as viaArray
    public static String[] viaIotPasswordArray; //Stores array of VIA stations' passwords for IOT command, if any, in the same order as viaArray
    public static int selectedTxMinute = -1; //"-1" = Tx as soon as possible, 0 to 4 is TX on that minute only (For scanning servers)
    public static String[] minuteArray = {"ASAP","0","1","2","3","4"}; //Stores array of minute to TX in
    public static String[] channelArray = {"Custom Channel", "CH 1 - 600Hz","CH 2 - 900 Hz","CH 3 - 1200Hz","CH 4 - 1500Hz", "CH 5 - 1800Hz","CH 6 - 2100Hz", "CH 7 - 2400Hz"};

    //Resend request radio buttons values
    private static String howManyToResend = "1";
    private static String whatToResend = "";
    private static Boolean forceRelayingQTC = false;

    //Selcall screen persistent variables
    static String lastToSelcall = "";

    //GPS Stuff
    public static LocationManager locationManager;
    public gpsListener locationListener = new gpsListener();
    public static Location currentLocation = null;
    public static boolean GPSisON = false;
    public static boolean GPSTimeAcquired = false;
    public static long deviceToRealTimeCorrection = 0L; //The number of seconds this device's clock is late compared to reference time (server or GPS)
    public static String refTimeSource = "";
    public static boolean newReferenceTimeReceived = false;


    public static String TerminalBuffer = "";
    public static StringBuffer ModemBuffer = new StringBuffer(11000); //1000 char more than max kept, but expandable anyway
    //public static StringBuffer latestModemBuffer = new StringBuffer(100); //From lasted precessed audio samples
    //public static boolean runningScreenUpdates = false;

    //Lock for updating the receive display buffer (Concurrent updates with the Modem thread)
    public static final Object modemBufferlock = new Object();

    // Member object for processing of Rx and Tx
    // Can be stopped (i.e no RX) to save battery and allow Android to reclaim
    // resources if not visible to the user
    public static boolean ProcessorON = false;
    private static boolean modemPaused = false;

    //For providing a progress on the number of smsview and for each message the % complete
    public static String txMessageCount = "";
    public static String progressCount = "";

    // Tx Buffer
    private static String bufferToTx;
    //Images sent in digital (Base64 encoded) or analog (MFSK image mode)
    static boolean digitalImages = false;
    static String imageMode = "";
    static String analogSpeedColour = "";
    public static Bitmap originalImageBitmap = null;
    public static Bitmap tempImageBitmap = null;
    public static int attachedPictureTxSPP = 2;
    public static boolean pictureIsColour = true;
    public static int targetWidth = 0;
    public static int targetHeight = 0;

    //Scanning (for future use)
    public static boolean scanningEnabled = true;
    public static long restartScanAtEpoch = 0L;

    // Notifications
    public static Notification myNotification = null;

    // Need handler for callbacks to the UI thread
    public static final Handler mHandler = new Handler();

    // Contact email picker
    //Not used private static final int CONTACT_PICKER_RESULT = 10101;
    // Share form action
    private static final int SHARE_MESSAGE_RESULT = 10202;
    // Share form action
    //not used private static final int EDIT_CSV_RESULT = 10303;
    // Camera/Gallery picture request
    private static final int PICTURE_REQUEST_CODE = 10404;

    //Temp picture attachment
    private static final String tempAttachPictureFname = "_imgCameraAttachment.pic";

    // Bluetooth handsfree / headset
    public static boolean deviceJustConnected = false;
    public static boolean toBluetooth = false;
    public static AudioManager mAudioManager;
    public static BluetoothAdapter mBluetoothAdapter = null;
    public static final int STREAM_BLUETOOTH_SCO = 6;

    //Bluetooth Audio devices
    public static BroadcastReceiver bluetoothReceiver = null;

    //Listener for incoming SMSs
    public static BroadcastReceiver smsReceiver = null;
    private static boolean listeningForSMSs = false;
    //Flags for immediate relaying of Emails and SMSs
    public static boolean WantRelayEmailsImmediat = true;
    public static boolean WantRelaySMSsImmediat = true;

    //Start epoch of the application for auto restarts
    private static long appStartTime = System.currentTimeMillis();

    // To monitor the incoming calls and disconnect Bluetooth so that we don't
    // send the phone call audio to the radio
    public static TelephonyManager mTelephonyManager = null;

    // Listener for changes in preferences
    public static OnSharedPreferenceChangeListener splistener;

    //USB serial interface
    private static final String ACTION_USB_PERMISSION = "com.RadioMSG.USB_PERMISSION";
    public static UsbSerialPort usbSerialPort = null;
    public enum UsbPermission { Unknown, Requested, Granted, Denied };
    public static UsbPermission usbPermission = UsbPermission.Unknown;
    public static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    //Lock for updating the receive display buffer (Concurrent updates with the Modem thread)
    public static final Object lockUSB = new Object();

    // Create runnable for updating the waterfall display
    public static final Runnable updatewaterfall = new Runnable() {
        public void run() {
            if (myWFView != null) {
                myWFView.postInvalidate();
            }
        }
    };


    // Runnable for updating the modem mode in the modem screens
    public static final Runnable updatemodemmode = new Runnable() {
        public void run() {
            if ((modemModeView != null)
                    && ((currentview == MODEMVIEWnoWF) || (currentview == MODEMVIEWwithWF))) {
                int mIndex = Modem.getModeIndexFullList(RMsgProcessor.RxModem);
                modemModeView.setText(Modem.modemCapListString[mIndex]);
            }
        }
    };

    /*
    public void updatetitle() {
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                //int mIndex = Modem.getModeIndexFullList(RMsgProcessor.RxModem);
                //SAMSUNG & WIKO appcompat BUG Workaround
                try {
                    //Only if buttons shown in the action bar ((AppCompatActivity) myContext).supportInvalidateOptionsMenu();
                    if (ProcessorON) {
                        if (RMsgProcessor.TXActive) {
                            //		    myWindow.setTitleColor(Color.YELLOW);
                            //((AppCompatActivity) myContext).getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#FFFF00\">"
                            getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#FFFF00\">"
                                    + RMsgProcessor.status
                                    + RadioMSG.progressCount
                                    + "</font>")));
                            //Change the Cancel Message Icon to mean cancel current transmission
                            if (mOptionsMenu != null) {
                                mOptionsMenu.findItem(R.id.cancelmsg).setIcon(R.drawable.cancelmsgtx);
                            }
                        } else {
                            //		    myWindow.setTitleColor(Color.CYAN);
                            getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#33D6FF\">"
                                    + RMsgProcessor.status
                                    + RadioMSG.progressCount
                                    + "</font>")));
                            //Change the Cancel Message Icon to mean cancel current transmission
                            if (mOptionsMenu != null) {
                                mOptionsMenu.findItem(R.id.cancelmsg).setIcon(R.drawable.cancelmsgnotx);
                            }
                        }
                    } else {
                        getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#FFFFFF\">"
                                + "Modem OFF" + "</font>")));
                    }
                } catch (Throwable x) {
                    loggingclass.writelog("\nDebug Information: Samsung/Wiko appcompat compatibility issue workaround activated!", null, true);
                }
            }
        }));

    }
*/

    // Create runnable for changing the main window's title
    public static final Runnable updatetitle = new Runnable() {
        public void run() {
            //int mIndex = Modem.getModeIndexFullList(RMsgProcessor.RxModem);
            //SAMSUNG & WIKO appcompat BUG Workaround
            try {
                //Only if buttons shown in the action bar ((AppCompatActivity) myContext).supportInvalidateOptionsMenu();
                if (ProcessorON) {
                    if (RMsgProcessor.TXActive) {
                        //		    myWindow.setTitleColor(Color.YELLOW);
                        //((AppCompatActivity) myContext).getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#FFFF00\">"
                        myInstance.getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#FFFF00\">"
                                + RMsgProcessor.status
                                + RadioMSG.progressCount
                                + "</font>")));
                        //Change the Cancel Message Icon to mean cancel current transmission
                        if (myInstance.mOptionsMenu != null) {
                            myInstance.mOptionsMenu.findItem(R.id.cancelmsg).setIcon(R.drawable.cancelmsgtx);
                        }
                    } else {
                        //		    myWindow.setTitleColor(Color.CYAN);
                        myInstance.getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#33D6FF\">"
                                + RMsgProcessor.status
                                + RadioMSG.progressCount
                                + "</font>")));
                        //Change the Cancel Message Icon to mean cancel current transmission
                        if (myInstance.mOptionsMenu != null) {
                            myInstance.mOptionsMenu.findItem(R.id.cancelmsg).setIcon(R.drawable.cancelmsgnotx);
                        }
                    }
                } else {
                    myInstance.getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#FFFFFF\">"
                            + "Modem OFF" + "</font>")));
                }
            } catch (Throwable x) {
                loggingclass.writelog("\nDebug Information: Samsung/Wiko appcompat compatibility issue workaround activated!", null, true);
            }
        }
    };


    // Runnable for updating the signal quality bar in Modem Window
    public static final Runnable updatesignalquality = new Runnable() {
        public void run() {
            if ((SignalQuality != null)
                    && ((currentview == MODEMVIEWnoWF) || (currentview == MODEMVIEWwithWF))) {
                SignalQuality.setProgress((int) Modem.metric);
                SignalQuality.setSecondaryProgress((int) Modem.squelch);
            }

            if (AudioLevel != null) {
                double rawAudioLevel = Modem.getAudioLevel();
                if (rawAudioLevel != 0.0f) {
                    double audiodB = Math.log(rawAudioLevel) + 10.0f;
                    double maxvalue = RadioMSG.mysp.getFloat("WFMAXVALUE", (float) 11.0);
                    AudioLevel.setProgress((int) ((audiodB - 30) * 400 / maxvalue));
                }
            }
        }
    };


    // Runnable for updating the CPU load bar in Modem Window
    public static final Runnable updatecpuload = new Runnable() {
        public void run() {
            if ((CpuLoad != null)
                    && ((currentview == MODEMVIEWnoWF) || (currentview == MODEMVIEWwithWF))) {
                CpuLoad.setProgress((int) RMsgProcessor.cpuload);
            }
        }
    };


    // Create runnable for posting to terminal window
    public static final Runnable addtoterminal = new Runnable() {
        public void run() {
            // Always drain TermWindow to prevent unbounded growth
            // even when the terminal view is not visible
            TerminalBuffer += RMsgProcessor.TermWindow;
            RMsgProcessor.TermWindow = "";
            if (TerminalBuffer.length() > 10000)
                TerminalBuffer = TerminalBuffer.substring(2000);
            if (myTermTV != null) {
                myTermTV.setText(TerminalBuffer);
                // Then scroll to the bottom
                if (myTermSC != null) {
                    myTermSC.post(new Runnable() {
                        public void run() {
                            myTermSC.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        }
    };

    //Check if a view is visible. Used here to detect when the user scrolls back in the
    //  Modem screen. In which case we do not autoscroll to the bottom and we allow
    //  selection and copy of the text in the modem screen
    public static boolean isVisibleInView(View parentView, final View view, boolean bottom) {
        if (view == null) {
            return false;
        }
        if (!view.isShown()) {
            return false;
        }
        //final Rect actualPosition = new Rect();
        Rect actualPosition = new Rect();
        view.getGlobalVisibleRect(actualPosition);
        int[] location = new int[2];
        parentView.getLocationOnScreen(location);
        //int scX = parentView.getWidth();
        //int scY = parentView.getHeight();
        int scX = parentView.getWidth();
        int scY = parentView.getHeight();
        scX += location[0];
        scY += location[1];
        //
        Display display = myInstance.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Rect scrollViewOnScreen = new Rect(location[0], location[1], scX, scY);
        //return actualPosition.intersect(scrollViewOnScreen);
        if (bottom) {
            int visiblePart = scY - actualPosition.bottom;
            return visiblePart > -3;
        } else {
            int visiblePart = location[1] - actualPosition.bottom;
            return visiblePart < 3;
        }
    }

    static boolean mustScrollDown = true;
    static long lastUpdateTime = 0L;
    //Create runnable for posting to modem window
    public static final Runnable updateModemScreen = new Runnable() {
        public void run() {
            if (currentview == MODEMVIEWnoWF || currentview == MODEMVIEWwithWF) {
                //test
                // if (Modem.lastCharacterTime - lastUpdateTime > 100) {
                if (System.currentTimeMillis() > lastUpdateTime + 100) {
                    lastUpdateTime = System.currentTimeMillis();
                    // myTV.setText(RMsgProcessor.TermWindow);
                    if (myModemTV != null) {
                        boolean allowSelectOnBackScroll = config.getPreferenceB("ALLOWSELECTONBACKSCROLL", false);
                        synchronized (modemBufferlock) {
                            // Then scroll to the bottom ONLY IF we have not scrolled backwards
                            ScrollView myModemSC = myInstance.findViewById(R.id.modemscrollview);
                            if (myModemSC != null) {
                                if (allowSelectOnBackScroll) {
                                    TextView topOfScrollView = myInstance.findViewById(R.id.topofscrollview);
                                    TextView endOfScrollView = myInstance.findViewById(R.id.endofscrollview);
                                    int selectionSize = myModemTV.getSelectionEnd() - myModemTV.getSelectionStart();
                                    //if (isVisibleInView(myModemSC, endOfScrollView) && myModemTV.getSelectionStart() != -1) {
                                    if ((isVisibleInView(myModemSC, endOfScrollView, true)
                                            //    && isVisibleInView(myModemSC, topOfScrollView, false)
                                            && selectionSize == 0)
                                            || mustScrollDown) {
                                        //Reset flag
                                        mustScrollDown = false;
                                        //Make un-selectable to avoid flickering unless we are still filling up the screen (not scrolling yet)
                                        if (isVisibleInView(myModemSC, topOfScrollView, false)) {
                                            if (myModemTV != null && !myModemTV.isTextSelectable()) {
                                                try {
                                                    myModemTV.setTextIsSelectable(true);
                                                } catch (ClassCastException e) {
                                                    //Nothing
                                                }
                                            }
                                        } else {
                                            if (myModemTV != null && myModemTV.isTextSelectable()) {
                                                myModemTV.setTextIsSelectable(false);
                                            }
                                        }
                                        //myModemTV.setText(ModemBuffer);
                                        myModemTV.setText(ModemBuffer, TextView.BufferType.SPANNABLE);
                                        myModemSC.fullScroll(View.FOCUS_DOWN);
                                        //Trim down buffer if overgrown
                                        if (ModemBuffer.length() > 8000) {
                                            ModemBuffer.delete(0, 2000);
                                        }
                                    } else {
                                        //Make selectable as we are not scrolling
                                        if (myModemTV != null && !myModemTV.isTextSelectable()) {
                                            try {
                                                myModemTV.setTextIsSelectable(true);
                                            } catch (ClassCastException e) {
                                                //Nothing
                                            }
                                        }
                                    }
                                } else {
                                    //Old method, always update
                                    myModemTV.setText(ModemBuffer, TextView.BufferType.SPANNABLE);
                                    myModemSC.fullScroll(View.FOCUS_DOWN);
                                    //Trim down buffer if overgrown
                                    if (ModemBuffer.length() > 8000) {
                                        ModemBuffer.delete(0, 2000);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                synchronized (modemBufferlock) {
                    if (ModemBuffer.length() > 8000) {
                        ModemBuffer.delete(0, 2000);
                    }
                }
            }
        }
    };



    // Runnable for updating the image being received
    public static final Runnable updateMfskPicture = new Runnable() {
        public void run() {
            //Update displayed bitmap
            if (pwLayout != null) {
                ImageView imageView = (ImageView) RadioMSG.pwLayout.findViewById(R.id.imageView1);
                if (imageView != null) {
                    imageView.setImageBitmap(Modem.picBitmap);
                }
            }
        }
    };


    // Phone state listener to disable Bluetooth when receiving a call
    // This is to prevent the phone call's audio to be sent to the radio,
    // as well as stopping the TXing from this app until the phone call is finished.
    // This action is only active when we have enabled Bluetooth in the application.
    private PhoneStateListener mPhoneStateListener = null; // used on API < 31
    private Object mCallStateCallback = null; // holds CallStateCallback on API 31+

    private void handleCallStateChange(int state) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            // If using the bluetooth interface for the digital link,
            // disable it otherwise the phone call will use it.
            if (RadioMSG.toBluetooth) {
                RadioMSG.toBluetooth = false;
                RadioMSG.mAudioManager.setMode(AudioManager.MODE_NORMAL);
                // Froyo bug
                RadioMSG.mAudioManager.stopBluetoothSco();
                if (RadioMSG.mBluetoothAdapter != null) {
                    if (RadioMSG.mBluetoothAdapter.isEnabled()) {
                        RadioMSG.mBluetoothAdapter.disable();
                    }
                }
                RadioMSG.mAudioManager.setBluetoothScoOn(false);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.S)
    private class CallStateCallback extends TelephonyCallback
            implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            handleCallStateChange(state);
        }
    }


    // Update the list view IF we are on that view AND NOT in a popup
    // Used when receiving new smsview as they arrive IF we are on that screen
    public static final Runnable updateList = new Runnable() {
        public void run() {
            //Add entry into listview
            if (!trackPOI && currentview == SMSVIEW && !inFormDisplay) {
                myInstance.displaySms(BOTTOM);
            }
        }
    };


    //Time based actions, runs every second
    public void regularActions() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (currentview == SMSVIEW) {
                    try {
                        TextView txtCurrentTime = (TextView) findViewById(R.id.minsec);
                        if (txtCurrentTime != null) {
                            long nowInMilli = System.currentTimeMillis();
                            Time mytime = new Time();
                            //Not using GPS time? Make sure the variables are reset
                            //if (!config.getPreferenceB("USEGPSTIME", false)
                            //        && refTimeSource.equals("")) {
                            //    deviceToRealTimeCorrection = 0L;
                            //    GPSTimeAcquired = false;
                            //}
                            mytime.set(nowInMilli + (deviceToRealTimeCorrection * 1000)); //now +/- GPS correction if any
                            String MinutesStr = "00" + mytime.minute;
                            MinutesStr = MinutesStr.substring(MinutesStr.length() - 2, MinutesStr.length());
                            String SecondsStr = "00" + mytime.second;
                            SecondsStr = SecondsStr.substring(SecondsStr.length() - 2, SecondsStr.length());
                            txtCurrentTime.setText(MinutesStr + ":" + SecondsStr);
                            if (refTimeSource.equals("")) {
                                txtCurrentTime.setTextColor(Color.WHITE);
                            } else if (refTimeSource.equals("GPS")) {
                                txtCurrentTime.setTextColor(Color.GREEN);
                            } else {
                                //Must be after a Time Sync to a server
                                txtCurrentTime.setTextColor(Color.RED);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                int txCount = RMsgTxList.getLength();
                //Now update progress info in Title if we are TXing
                if (RMsgProcessor.TXActive && !Modem.modemIsTuning) {
                    int percent = Modem.getTxProgressPercent();
                    if (txCount > 0) {
                        progressCount = ": " + Integer.toString(percent) + "% - (" + txCount + ")";
                    } else {
                        progressCount = ": " + Integer.toString(percent) + "%";
                    }
                    //RadioMSG.mHandler.post(RadioMSG.updatetitle);
                } else {
                    if (txCount > 0) {
                        progressCount = "  -  (" + txCount + ")";
                    } else {
                        progressCount = "";
                    }
                    lastTxCount = txCount;
                }
                RadioMSG.mHandler.post(RadioMSG.updatetitle);
                //Check if we have a timeout on reception of a picture
                RMsgProcessor.checkPictureReceptionTimeout();
                //Check if we have to stop processing modem data
                if (config.getPreferenceB("IDLEMODEMOFF", false)) {
                    Modem.checkListeningTimeout();
                }
                //Check if we need to send data
                if (!RMsgProcessor.TXActive) {
                    Modem.checkNeedToTransmit();
                }
                //Now check if we have a regular restart request for a server

                //Check if we need to auto restart the app on a regular basis
                int restartEveryXHours = config.getPreferenceI("RESTARTEVERY", 0);
                boolean unattended = config.getPreferenceB("UNATTENDEDRELAY", false);
                if (unattended && restartEveryXHours > 0
                        && (appStartTime + (restartEveryXHours * 60000) < System.currentTimeMillis())
                        && Modem.modemIsIdle()) {
                    doRestart(RadioMSG.this);
                }
                //close the speaker audio after x seconds after receiving message to allow for ack beeps or RSID to be heard
                if (speakerOption == 2 && speakerOffDelay != 0L && speakerOffDelay < System.currentTimeMillis()) {
                    Modem.setSpeakerOff();
                }
            }
        });
    }


    class regularActionsRunner implements Runnable {
        // @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    //Update time display in Terminal window
                    regularActions();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                }
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (RadioMSG.inFormDisplay) { //In "popup" screen displaying individual smsview
/*	    if (currentview != FORMSVIEW && currentview != TEMPLATESVIEW && currentview != SMSQUICKLISTVIEW ) {
        displaySms(BOTTOM, currentview);
	    } else {
            if (++backKeyPressCount < 2) {
                lastBackKeyTime = System.currentTimeMillis();
            } else { //Was the second key press "in-time"?
                if (System.currentTimeMillis() > lastBackKeyTime + 1200) {
                    //Not in time, reset the count to zero. Need two new Back key presses.
                    Toast.makeText(this, "Press the Back Button twice in a row to return to the list", Toast.LENGTH_SHORT).show();
                    backKeyPressCount = 1;
                    //Count this back press as one
                    lastBackKeyTime = System.currentTimeMillis();
                } else {
                    //In time, therefore return
                    displaySms(BOTTOM);
                    backKeyPressCount = 0;
                }
            }
            if (currentview == SMSVIEW) {
                //TO-DO: Reset to last position
            }
*/
            displaySms(BOTTOM);
        } else {
            Toast.makeText(this, "Please Use the Menu then the Exit option to close RadioMSG", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = super.dispatchTouchEvent(ev);
        handled = mGesture.onTouchEvent(ev);
        return handled;
    }

    //Detects swipe movements on screens
    private SimpleOnGestureListener mOnGesture = new GestureDetector.SimpleOnGestureListener() {
        private float xDistance, yDistance, lastX, lastY;

        @Override
        public boolean onDown(MotionEvent e) {
            xDistance = yDistance = 0f;
            lastX = e.getX();
            lastY = e.getY();
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            final float curX = e2.getX();
            final float curY = e2.getY();
            xDistance += (curX - lastX);
            yDistance += (curY - lastY);
            lastX = curX;
            lastY = curY;
            if (!RadioMSG.inFormDisplay) {
                if (Math.abs(xDistance) > Math.abs(yDistance) && Math.abs(velocityX) > 1500) {
                    // toastText("fling......");
                    //Not on About screen as we have to press on one of the two buttons at the bottom
                    if (currentview != ABOUTVIEW) {
                        navigateScreens((int) xDistance);
                    }
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            return false;
        }
    };


    // Swipe (fling) handling to move from screen to screen
    private void navigateScreens(int flingDirection) {
        int displayDir;
        // Navigate between screens by gesture (on top of menu button acces)

        //swipableScreens[swipableScreensCount]
        if (flingDirection > 0) { // swipe/fling right
            displayDir = RIGHT;
            if (--swipableScreensIndex < 0)
                swipableScreensIndex = swipableScreensCount - 1;
        } else {
            displayDir = LEFT;
            if (++swipableScreensIndex >= swipableScreensCount)
                swipableScreensIndex = 0;
        }

        switch (swipeableScreens[swipableScreensIndex]) {

            case TERMVIEW:
                //Do nothing
                //displayTerminal(RIGHT);
                break;

            case MODEMVIEWnoWF:
                displayModem(displayDir, false);
                break;

            case MODEMVIEWwithWF:
                displayModem(displayDir, true);
                break;

            case SMSVIEW:
                displaySms(displayDir);
                break;

            case MMSVIEW:
                displayMms(displayDir);
                break;

            case SMSQUICKBUTTONVIEW:
                displaySmsQuickButtons(displayDir);
                break;

            case SMSQUICKLISTVIEW:
                displaySmsQuickList(displayDir);
                break;

            //case SELCALLVIEW:
            //    displayCcir493(displayDir);
            //    break;

            case ABOUTVIEW:
                displayAbout();
                break;

            default:
                displaySms(displayDir); // Just in case

        }

    }


    public static boolean readContactsPermit = false;
    public static boolean sendSmsPermit = false;
    public static boolean receiveSmsPermit = false;
    public static boolean receiveMmsPermit = false;
    public static boolean readSmsPermit = false;
    public static boolean fineLocationPermit = false;
    public static boolean writeExtStoragePermit = false;
    public static boolean recordAudioPermit = false;
    public static boolean readPhoneStatePermit = false;

    private final int REQUEST_PERMISSIONS = 15556;
    public final String[] permissionList = {
            //Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
//            Manifest.permission.RECEIVE_MMS,
//            Manifest.permission.READ_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
    };

    //Request permission from the user
    private void requestAllCriticalPermissions() {
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(RadioMSG.this);
        myAlertDialog.setMessage("You are about to be presented with a series of permission requests." +
                        "\nAll permissions are essential to make this app work as intended." +
                        "\nIf you do not allow any of the permissions 2. to 5. the app cannot run and will exit." +
                        "\n\nExplanations for the critical permissions: " +
//                "\n1. Read Contacts allows the app to access your contacts email addresses when creating a new email (optional)" +
                        "\n1. Send SMS: if enabled in the settings, allows the relay of messages to the cellular network, sending SMS messages to a phone number." +
                        "\n2. Receive SMS: as above, if enabled, for receiving SMS messages from the cellular network." +
                        "\n3. Access Fine Location: The GPS position and data is essential for position reporting." +
                        "\n4. Write External Storage: access to the app's directory on the SD card (internal or external) for storing messages" +
                        "\n5. Record Audio: audio input for the modem" +
                        "\n6. Read Phone State: to disconnect the Bluetooth interface when receiving a phone call. Telecommunication regulations generally " +
                        "forbid connecting a person to the telephone network or to do so without a warning." +
                        "\n\nIf you have previously denied some critical permissions, you will have to go back to the Device's Settings/Apps and re-allow the missing permission."
        );
        myAlertDialog.setCancelable(false);
        myAlertDialog.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ActivityCompat.requestPermissions(myInstance, permissionList, REQUEST_PERMISSIONS);
            }
        });
        myAlertDialog.show();
    }

    private boolean allPermissionsOk() {
        final int granted = PackageManager.PERMISSION_GRANTED;

        //ContextCompat.checkSelfPermission(myContext, Manifest.permission.READ_CONTACTS) == granted &&
        return ContextCompat.checkSelfPermission(myContext, Manifest.permission.SEND_SMS) == granted &&
                ContextCompat.checkSelfPermission(myContext, Manifest.permission.RECEIVE_SMS) == granted &&
//                ContextCompat.checkSelfPermission(myContext, Manifest.permission.RECEIVE_MMS) == granted &&
//                ContextCompat.checkSelfPermission(myContext, Manifest.permission.READ_SMS) == granted &&
                ContextCompat.checkSelfPermission(myContext, Manifest.permission.ACCESS_FINE_LOCATION) == granted &&
                ContextCompat.checkSelfPermission(myContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == granted &&
                ContextCompat.checkSelfPermission(myContext, Manifest.permission.RECORD_AUDIO) == granted &&
                ContextCompat.checkSelfPermission(myContext, Manifest.permission.READ_PHONE_STATE) == granted;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int granted = PackageManager.PERMISSION_GRANTED;
        for (int i = 0; i < grantResults.length; i++) {
            /* if (permissions[i].equals(Manifest.permission.READ_CONTACTS)) {
                readContactsPermit = grantResults[i] == granted;
            } else */
            if (permissions[i].equals(Manifest.permission.SEND_SMS)) {
                sendSmsPermit = grantResults[i] == granted;
            } else if (permissions[i].equals(Manifest.permission.RECEIVE_SMS)) {
                receiveSmsPermit = grantResults[i] == granted;
//            } else if (permissions[i].equals(Manifest.permission.RECEIVE_MMS)) {
//                receiveMmsPermit = grantResults[i] == granted;
//            } else if (permissions[i].equals(Manifest.permission.READ_SMS)) {
//                readSmsPermit = grantResults[i] == granted;
            } else if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                fineLocationPermit = grantResults[i] == granted;
            } else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                writeExtStoragePermit = grantResults[i] == granted;
            } else if (permissions[i].equals(Manifest.permission.RECORD_AUDIO)) {
                recordAudioPermit = grantResults[i] == granted;
            } else if (permissions[i].equals(Manifest.permission.READ_PHONE_STATE)) {
                readPhoneStatePermit = grantResults[i] == granted;
            } else {
                //Nothing so far
            }
        }
        //Re-do overall check
        havePassedAllPermissionsTest = allPermissionsOk();
        if (havePassedAllPermissionsTest &&
                requestCode == REQUEST_PERMISSIONS) { //Only if requested at OnCreate time
            performOnCreate();
            performOnStart();
        } else {
            //Close that activity and return to previous screen
            finish();
            //Kill the process
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }


    //Could be executed only when all necessary permissions are allowed
    private void performOnCreate() {

        // Init config
        mysp = PreferenceManager.getDefaultSharedPreferences(this);

        // Start file logging if enabled
        if (mysp.getBoolean("LOGTOFILE", false)) {
            LogcatLogger.start(this);
        }

        //Initialize location manager for gps fixes
        locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);

        //Set the Activity's Theme
        int myTheme = config.getPreferenceI("APPTHEME", 0);
        switch (myTheme) {
            case 1:
                setTheme(R.style.radiomsgStandardDark);
                break;
            case 2:
                setTheme(R.style.radiomsgStandardLight);
                break;
            case 3:
                setTheme(R.style.radiomsgSmallScreen);
                break;
            case 0:
            default:
                setTheme(R.style.radiomsgStandard);
                break;
        }

        //Prepare list of "swipe-able" screens from preferences
        String[] swipeableScreensPrefs = {"SMSVIEW", "MODEMVIEWnoWF",
                "MMSVIEW", "SMSQUICKLISTVIEW", "SMSQUICKBUTTONVIEW"};
        int[] swipeableScreensPrefsValue = {SMSVIEW, MODEMVIEWnoWF,
                MMSVIEW, SMSQUICKLISTVIEW, SMSQUICKBUTTONVIEW};
        swipeableScreens = new int[swipeableScreensPrefsValue.length];
        swipableScreensCount = 0;
        for (int i = 0; i < swipeableScreensPrefsValue.length; i++) {
            if (config.getPreferenceB(swipeableScreensPrefs[i], true)) {
                swipeableScreens[swipableScreensCount++] = swipeableScreensPrefsValue[i];
            }
        }

        // Call the folder handling method
        RMsgProcessor.handlefolderstructure();

        //Oppo issue. Restore all settings at start
        if (config.getPreferenceB("RESTORESETTINGATSTART")) {
            config.doLoadSharedPreferencesFromFile("SettingsBackup.bin");
        }

        //Check that op-modes preferences have default values
        SharedPreferences.Editor editor = RadioMSG.mysp.edit();
        int index = 0;
        for (String thisOpMode : opModes) {
            if (config.getPreferenceS(thisOpMode, "Blank").equals("Blank")) {
                editor.putString(thisOpMode, defaultModes[index]);
                editor.commit();
            }
            index++;
        }

        // Get new gesture detector for flings over scrollviews
        mGesture = new GestureDetector(this, mOnGesture);

        // Initialize Toasts for use in the Toast display routines below
        myToast = Toast.makeText(RadioMSG.this, "", Toast.LENGTH_SHORT);
        longToast = Toast.makeText(RadioMSG.this, "", Toast.LENGTH_LONG);

        //Bluetooth Audio (receive/transmit via a Bluetooth headset or hansfree device instead of )
        mAudioManager = (AudioManager) myContext.getSystemService(Context.AUDIO_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Monitor the connection of Bluetooth between devices and USB connections
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String dataString = intent.getDataString();
                String mpackage = intent.getPackage();
                String type = intent.getType();
                int extraSCO = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);

                if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String wantedDevice = config.getPreferenceS("BLUETOOTHDEVICENAME", "noname");
                    //Added check on Bluetooth device name as Samsung A51 (Android 10) would crash there
                    if (deviceName != null
                            && toBluetooth
                            && deviceName.equals(wantedDevice)
                            && mBluetoothAdapter != null
                            && config.getPreferenceB("BLUETOOTHAUTOCONNECT", false)) {
                        toBluetooth = false;
                        mAudioManager.stopBluetoothSco();
                        mAudioManager.setMode(AudioManager.MODE_NORMAL);
                        mAudioManager.setBluetoothScoOn(false);
                        topToastText("Bluetooth Device Disconnected. Audio Now Via Speaker/Headphones");
                    }
                } else if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String wantedDevice = config.getPreferenceS("BLUETOOTHDEVICENAME", "noname");
                    //Added check on Bluetooth device name as Samsung A51 (Android 10) would crash there
                    if (deviceName != null && deviceName.equals(wantedDevice)
                            && mBluetoothAdapter != null
                            && config.getPreferenceB("BLUETOOTHAUTOCONNECT", false)) {
                        deviceJustConnected = true;
                        final Runnable mRunnable = new Runnable() {
                            public void run() {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                }
                                mAudioManager.startBluetoothSco();
                                mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                                mAudioManager.setBluetoothScoOn(true);
                                toBluetooth = true;
                                topToastText("Started BT SCO");
                            }
                        };
                        mRunnable.run();
                    }
                } else if (extraSCO == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    //BluetoothDevice device = intent
                    //        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //String deviceName = device.getName();
                    if (!deviceJustConnected) { //We started the app with the BT device already connected
                        mAudioManager.startBluetoothSco();
                        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                        mAudioManager.setBluetoothScoOn(true);
                        toBluetooth = true;
                        deviceJustConnected = false;
                        topToastText("Re-connected Audio Via Bluetooth Device");
                    } else {
                        topToastText("Connected Audio Via Bluetooth Device");
                    }
                } else if (extraSCO == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
                    //topToastText("Bluetooth SCO Connecting");
                } else if (extraSCO == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    //topToastText("Bluetooth SCO Disconnected");
                } else if (ACTION_USB_PERMISSION.equals(action) || INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    synchronized (lockUSB) {
                        //debug Modem.appendToModemBuffer("Authorisation requested");
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if(device != null){

                                boolean pttViaRTS = config.getPreferenceB("RTSASPTT", false);
                                boolean pttViaDTR = config.getPreferenceB("DTRASPTT", false);
                                boolean pttViaCAT = config.getPreferenceB("CATASPTT", false);
                                if (pttViaRTS | pttViaDTR | pttViaCAT) {
                                    //if required, initialised the USB Serial port
                                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                                            ? UsbPermission.Granted : UsbPermission.Denied;
                                    //debug Modem.appendToModemBuffer("Trying to connect");
                                    connectUsbDevice();
                                }
                            }
                        }
                        else {
                            //debug Modem.appendToModemBuffer("Permission denied for device " + device);
                        }
                    }
                } else {
                    topToastText("Other Actions");
                }
            }

        };

        //Bluetooth File transfers and USB device connections on OTG (Receiving listener)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        //if (android.os.Build.VERSION.SDK_INT >= 14) {
        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(INTENT_ACTION_GRANT_USB);
        // } else {
        // test filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED);
        //}
        // Not called filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            this.registerReceiver(bluetoothReceiver, filter);
        }


        //Listener for mew emails if I relay emails and requested to listen for
        //  new email (and maybe send them immediately over radio)
        if (config.getPreferenceB("LISTENFOREMAILS", false)) {
            Thread myThread = new Thread() {
                @Override
                public void run() {
                    Boolean keepInLoop = true;
                    while (keepInLoop) {
                        //Normally one shot
                        keepInLoop = false;
                        //Request emails from server
                        IMAPFolder folder = null;
                        Store store = null;
                        try {
                            Properties props = System.getProperties();
                            String imapProtocol = config.getPreferenceS("IMAPPROTOCOL", "SSL/TLS");
                            if (imapProtocol.equals("SSL/TLS")) {
                                props.setProperty("mail.store.protocol", "imaps");
                                props.setProperty("mail.imaps.socketFactory.port",
                                        config.getPreferenceS("RELAYEMAILIMAPPORT", "993"));
                                props.setProperty("mail.imaps.port",
                                        config.getPreferenceS("RELAYEMAILIMAPPORT", "993"));
                                //props.setProperty("mail.imap.starttls.enable", "true");
                                props.setProperty("mail.imap.ssl.enable", "true");
                            } else if (imapProtocol.equals("STARTTLS")) {
                                props.setProperty("mail.store.protocol", "imaps");
                                props.setProperty("mail.imaps.socketFactory.port",
                                        config.getPreferenceS("RELAYEMAILIMAPPORT", "993"));
                                props.setProperty("mail.imaps.port",
                                        config.getPreferenceS("RELAYEMAILIMAPPORT", "993"));
                                props.setProperty("mail.imap.starttls.enable", "true");
                                //props.setProperty("mail.imap.ssl.enable", "true");
                            } else {
                                props.setProperty("mail.store.protocol", "imap");
                                props.setProperty("mail.imap.port",
                                        config.getPreferenceS("RELAYEMAILIMAPPORT", "993"));
                            }
                            MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
                            socketFactory.setTrustAllHosts(true);
                            props.put("mail.imaps.ssl.socketFactory", socketFactory);
                            //conflict with default instance, create a new one each time
                            //Session session = Session.getDefaultInstance(props, null);
                            javax.mail.Session session = javax.mail.Session.getInstance(props, null); //Conflicts with local Session.java, must be explicit
                            if (imapProtocol.equals("NONE")) {
                                store = session.getStore("imap");
                            } else {
                                store = session.getStore("imaps");
                            }
                            String imapHost = config.getPreferenceS("RELAYEMAILIMAPSERVER");
                            String userName = config.getPreferenceS("RELAYUSERNAME");
                            String emailPassword = config.getPreferenceS("RELAYEMAILPASSWORD");
                            store.connect(imapHost, userName, emailPassword);
                            folder = (IMAPFolder) store.getFolder("inbox");
                            if (!folder.isOpen())
                                folder.open(Folder.READ_WRITE);

                            // Add messageCountListener to listen for new messages
                            folder.addMessageCountListener(new MessageCountAdapter() {
                                public void messagesAdded(MessageCountEvent ev) {
                                    Message[] messages = ev.getMessages();
                                    //Iterate through all the messages received
                                    for (int i = messages.length; i > 0; i--) {
                                        String senderAddress = "";
                                        String smsString = "";
                                        Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                        try {
                                            Message msg = messages[i - 1];
                                            //From email address
                                            senderAddress = msg.getFrom()[0].toString();
                                            //Remove name and only keep email address proper
                                            String[] emailAdresses = senderAddress.split("[ <>]+");
                                            for (String fromPart : emailAdresses) {
                                                if (fromPart.indexOf("@") > 0) {
                                                    senderAddress = fromPart;
                                                    break;
                                                }
                                            }
                                            //Save message time for making filenames later on
                                            Date msgDateTime = msg.getReceivedDate();
                                            c1.setTime(msgDateTime);

                                            smsString = RMsgProcessor.getBodyTextFromMessage(msg);
                                            if (smsString.startsWith("\n")) {
                                                smsString = smsString.substring(1);
                                            }
                                            if (smsString.endsWith("\r\n\r\n")) {
                                                smsString = smsString.substring(0, smsString.length() - 4);
                                            }
                                            if (smsString.length() > 155) {
                                                smsString = smsString.subSequence(0, 155 - 1) + " ...>";
                                            }
                                            String smsSubject = msg.getSubject();
                                            //Not a reply to a previous radio message, add the subject line
                                            if (!smsSubject.contains("Radio Message from ")
                                                    && !smsSubject.contains("Reply from ")
                                                    && !smsSubject.trim().equals("")) {
                                                smsString = smsSubject + "\n" + smsString;
                                            }
                                        } catch (Exception e) {
                                            continue; //Skip processing that message
                                        }
                                        //Toast.makeText(OtpActivity.this, "senderNum: " + senderNo + " :\n message: " + message, Toast.LENGTH_LONG).show();
                                        //Toast.makeText(myContext, senderNo + ": " + smsBody, Toast.LENGTH_SHORT).show();
                                        String emailFilterTo[] = passEmailFilter(senderAddress);
                                        //Iterate through all the linked "to" stations as found in the SMS filter
                                        for (String toString : emailFilterTo) {
                                            //Blank string means nobody to send to
                                            if (toString != null && !toString.equals("")) {
                                                // Create message from cellular SMS
                                                RMsgObject radioEmailMessage = new RMsgObject();
                                                radioEmailMessage.to = toString;
                                                radioEmailMessage.relay = RMsgProcessor.getCall();
                                                radioEmailMessage.sms = smsString;
                                                radioEmailMessage.via = ""; //No relay here
                                                radioEmailMessage.receiveDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                                radioEmailMessage.receiveDate.setTime(c1.getTime());
                                                radioEmailMessage.from = RadioMSG.msgDisplayList.getAliasFromOrigin(senderAddress, toString);
                                                //Need to forward it over Radio too?
                                                if (config.getPreferenceB("RELAYRECEIVEDEMAILS", false) && WantRelayEmailsImmediat) {
                                                    //Create message and add in outbox list
                                                    RMsgTxList.addMessageToList(radioEmailMessage);
                                                }
                                            /* We do not save the messages received from the internet in the incoming list as we (re)fetch them at each request
                                            FileWriter out = null;
                                            //Crete a file name for this received cellular SMS
                                            Calendar c1 = Calendar.getInstance(TimeZone.getDefault());
                                            radioEmailMessage.fileName = String.format(Locale.US, "%04d", c1.get(Calendar.YEAR)) + "-" +
                                                    String.format(Locale.US, "%02d", c1.get(Calendar.MONTH) + 1) + "-" +
                                                    String.format(Locale.US, "%02d", c1.get(Calendar.DAY_OF_MONTH)) + "_" +
                                                    String.format(Locale.US, "%02d%02d%02d", c1.get(Calendar.HOUR_OF_DAY),
                                                            c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND)) + ".txt";
                                            //Save message in file
                                            String resultString = radioEmailMessage.formatForRx(false); //Do NOT use via Password for CRC
                                            String inboxFolderPath = RMsgProcessor.HomePath +
                                                    RMsgProcessor.Dirprefix + RMsgProcessor.DirInbox + RMsgProcessor.Separator;
                                            try {

                                                File msgReceivedFile = new File(inboxFolderPath + radioEmailMessage.fileName);
                                                if (msgReceivedFile.exists()) {
                                                    msgReceivedFile.delete();
                                                }
                                                out = new FileWriter(msgReceivedFile, true);
                                                out.write(resultString);
                                                out.close();
                                            } catch (Exception e) {
                                                loggingclass.writelog("Exception Error in 'OnReceive Email Message' " + e.getMessage(), null, true);
                                            }
                                            //Add to listadapter
                                            //final RMsgObject finalMessage = radioEmailMessage;
                                            final RMsgObject finalMessage = RMsgObject.extractMsgObjectFromFile(RMsgProcessor.DirInbox, radioEmailMessage.fileName, false); //Text part only
                                            RadioMSG.myInstance.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    msgDisplayList.addNewItem(finalMessage, false); //Is NOT my own message
                                                }
                                            });
                                            //update the list if we are on that screen
                                            RadioMSG.mHandler.post(RadioMSG.updateList);
                                            RMsgProcessor.PostToTerminal("\nSaved File: " + radioEmailMessage.fileName);
                                            RMsgUtil.addEntryToLog("Received Email " + radioEmailMessage.fileName);
                                            //Perform notification on speaker/lights/vibrate of new message
                                            RadioMSG.middleToastText("Email Received: " + radioEmailMessage.fileName);
                                            //Sleep 1 second to ensure all SMSes have a different file name (1 second resolution)
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                            }
                                            */
                                            }
                                        }
                                    }
                                }
                            });
                            // Check mail once in "freq" MILLIseconds
                            int freq = config.getPreferenceI("CHECKEMAILSEVERY", 600) * 1000; //converted to milliseconds
                            //Minimum every 10 seconds
                            freq = freq < 10000 ? 10000 : freq;
                            boolean supportsIdle = false;
                            RadioMSG.middleToastText("Listening to incoming emails");
                            try {
                                if (folder instanceof IMAPFolder) {
                                    IMAPFolder f = (IMAPFolder) folder;
                                    f.idle();
                                    supportsIdle = true;
                                }
                            } catch (FolderClosedException fex) {
                                throw fex;
                            } catch (MessagingException mex) {
                                supportsIdle = false;
                            }
                            for (; ; ) {
                                if (supportsIdle && folder instanceof IMAPFolder) {
                                    IMAPFolder f = (IMAPFolder) folder;
                                    f.idle();
                                    //System.out.println("IDLE done");
                                } else {
                                    Thread.sleep(freq); // sleep for freq milliseconds
                                    // This is to force the IMAP server to send us
                                    // EXISTS notifications.
                                    folder.getMessageCount();
                                }
                            }
                        } catch (Exception e) {
                            Exception e1 = e; //for debug
                            RadioMSG.middleToastText(e.toString());
                            //Save in log for debugging
                            RMsgUtil.addEntryToLog("Error relaying message as Email: \n" + e.toString());
                            //We have lost the connection with the server, restart the NewMailMonitor processing
                            keepInLoop = true;
                            if (e.toString().contains("UnknownHostException")
                                    || e.toString().contains("AuthenticationFailedException")
                            ) {
                                try {
                                    Thread.sleep(20000);//Wait 20 seconds before next try
                                } catch (InterruptedException e3) {
                                    //Nothing
                                }
                            }
                            //System.out.println("Restarting NewMailMonitor loop");
                        } finally {
                            try {
                                if (folder != null && folder.isOpen()) {
                                    folder.close(true);
                                }
                                if (store != null) {
                                    store.close();
                                }
                            } catch (Exception e) {
                                //do nothing
                            }
                        }
                    }
                }
            };
            myThread.start();
        }


        //Broadcast receiver for receiving and Forwarding Cellular SMSes over Radio
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Debug
                //Modem.appendToModemBuffer("\n<<<<Broadcast Receiver Activated>>>>\n");
                if (config.getPreferenceB("LISTENFORSMS", false) &&
                        intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                    final Bundle intentExtras = intent.getExtras();
                    if (intentExtras != null) {
                        //Start a new thread as it may be long running and by default the onReceive runs on the UI thread
                        final Object[] smses = (Object[]) intentExtras.get("pdus");
                        if (smses == null || smses.length == 0) return;
                        //Debug
                        //Modem.appendToModemBuffer("\n<<<<Received " + smses.length + " SMS(s)>>>>\n");
                        Thread myThread = new Thread() {
                            @Override
                            public void run() {
                                for (int i = 0; i < smses.length; ++i) {
                                    SmsMessage mySMS;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        String format = intentExtras.getString("format");
                                        mySMS = SmsMessage.createFromPdu((byte[]) smses[i], format);
                                    } else {
                                        mySMS = SmsMessage.createFromPdu((byte[]) smses[i]);
                                    }
                                    String senderNo = mySMS.getDisplayOriginatingAddress();
                                    //Convert to international number format E.g: +61499888777 for mobile number in AU
                                    senderNo = RMsgProcessor.convertNumberToE164(senderNo);
                                    //Get body of message
                                    String smsBody = mySMS.getDisplayMessageBody();
                                    //Toast.makeText(OtpActivity.this, "senderNum: " + senderNo + " :\n message: " + message, Toast.LENGTH_LONG).show();
                                    //Toast.makeText(myContext, senderNo + ": " + smsBody, Toast.LENGTH_SHORT).show();
                                    String smsFilterTo[] = passSMSFilter(senderNo);
                                    //Iterate through all the linked "to" stations as found in the SMS filter
                                    for (String toString : smsFilterTo) {
                                        //Blank string means nobody to send to
                                        if (toString != null && !toString.equals("")) {
                                            //Debug
                                            //Modem.appendToModemBuffer("\n<<<<Filtered in one SMS to " + toString + " >>>>\n");
                                            FileWriter out = null;
                                            // Create message from cellular SMS
                                            RMsgObject radioSmsMessage = new RMsgObject();
                                            radioSmsMessage.to = toString;
                                            radioSmsMessage.relay = RMsgProcessor.getCall();
                                            radioSmsMessage.sms = smsBody;
                                            radioSmsMessage.via = ""; //No relay here
                                            //Display as valid messages (which they are)
                                            radioSmsMessage.crcValid = true;
                                            radioSmsMessage.crcValidWithRelayPW = true;
                                            radioSmsMessage.crcValidWithIotPW = true;
                                            //radioSmsMessage.from = senderNo;
                                            //Use alias if present
                                            radioSmsMessage.from = RadioMSG.msgDisplayList.getAliasFromOrigin(senderNo, toString);
                                            if (radioSmsMessage.from.contains("=")) {
                                                //We found an alias, add the origin number
                                                radioSmsMessage.from = radioSmsMessage.from + senderNo;
                                            }
                                            //Create a file name for this received cellular SMS
                                            //Calendar c1 = Calendar.getInstance(TimeZone.getDefault());
                                            Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                            radioSmsMessage.fileName = String.format(Locale.US, "%04d", c1.get(Calendar.YEAR)) + "-" +
                                                    String.format(Locale.US, "%02d", c1.get(Calendar.MONTH) + 1) + "-" +
                                                    String.format(Locale.US, "%02d", c1.get(Calendar.DAY_OF_MONTH)) + "_" +
                                                    String.format(Locale.US, "%02d%02d%02d", c1.get(Calendar.HOUR_OF_DAY),
                                                            c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND)) + ".txt";
                                            //Save message in file
                                            boolean isCCIRMode = Modem.isCCIR476();
                                            //Need to store the received date as date, not offset (is it present?...hmmmm)
                                            // String resultString = radioSmsMessage.formatForTx(isCCIRMode); //Conditional change to lowercase
                                            String resultString = radioSmsMessage.formatForStorage(isCCIRMode); //Conditional change to lowercase
                                            String inboxFolderPath = RMsgProcessor.HomePath +
                                                    RMsgProcessor.Dirprefix + RMsgProcessor.DirInbox + RMsgProcessor.Separator;
                                            try {

                                                File msgReceivedFile = new File(inboxFolderPath + radioSmsMessage.fileName);
                                                if (msgReceivedFile.exists()) {
                                                    msgReceivedFile.delete();
                                                }
                                                out = new FileWriter(msgReceivedFile, true);
                                                out.write(resultString);
                                                out.close();
                                            } catch (Exception e) {
                                                loggingclass.writelog("Exception Error in 'OnReceive Cellular SMS' " + e.getMessage(), null, true);
                                            }
                                            //Add to listadapter
                                            //final RMsgObject finalMessage = radioSmsMessage;
                                            final RMsgObject finalMessage = RMsgObject.extractMsgObjectFromFile(RMsgProcessor.DirInbox, radioSmsMessage.fileName, false); //Text part only
                                            RadioMSG.myInstance.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    msgDisplayList.addNewItem(finalMessage, false); //Is NOT my own message
                                                    //VK2ETA debug test fixing changing filename of entry
                                                    //RadioMSG.msgDisplayList.notifyDataSetChanged();
                                                    //update the list if we are on that screen
                                                    RadioMSG.mHandler.post(RadioMSG.updateList);
                                                }
                                            });
                                            //Moved in the UI thread, just above
                                            //RadioMSG.mHandler.post(RadioMSG.updateList);
                                            //Advise reception of message
                                            //RMsgProcessor.PostToTerminal("\nFile integrity check: Checksum OK");
                                            //RMsgProcessor.PostToTerminal("\nSaved File: " + radioSmsMessage.fileName);
                                            RMsgUtil.addEntryToLog("Received Cellular SMS " + radioSmsMessage.fileName);
                                            //Perform notification on speaker/lights/vibrate of new message
                                            RadioMSG.middleToastText("Message Received: " + radioSmsMessage.fileName);


                                            //Need to forward it over Radio too?
                                            if (config.getPreferenceB("RELAYRECEIVEDSMS", false) && WantRelaySMSsImmediat) {
                                                //Create message and add in outbox list
                                                //RMsgObject txSmsMessage = new RMsgObject();
                                                //txSmsMessage = radioSmsMessage;
                                                //txSmsMessage.crcValid = true; //VK2ETA debug, just to change the object
                                                radioSmsMessage.from = msgDisplayList.getAliasFromOrigin(senderNo, toString);
                                                RMsgTxList.addMessageToList(radioSmsMessage);
                                            }
                                            //Sleep 1 second to ensure all SMSes have a different file name (1 second resolution)
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                            }
                                        }
                                    }
                                }
                            }
                        };
                        myThread.start();
                    }
                }
            }

        };


        //Register for receiving SMSes if requested
        if (config.getPreferenceB("LISTENFORSMS", false)) {
            if (ContextCompat.checkSelfPermission(RadioMSG.myContext, Manifest.permission.RECEIVE_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                filter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
                //Raise priority to maximum as some system apps (E.g. Google Messenger) can abort the broadcast
                filter.setPriority(2147483647); //Max integer value
                //Debug
                //Modem.appendToModemBuffer("\n<<<<smsReceiver registered>>>>\n");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(smsReceiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    registerReceiver(smsReceiver, filter);
                }
                listeningForSMSs = true;
            } else {
                RadioMSG.topToastText("Permission to Receive SMS not granted. Go to the device \"Settings/Apps\" and " +
                        "allow \"Receive SMS\" for the RadioMsg app");
                //Debug
                //Modem.appendToModemBuffer("\n<<<<smsReception not permitted>>>>\n");
            }
        } else {
            //Debug
            //Modem.appendToModemBuffer("\n<<<<SMS monitoring not requested>>>>\n");
        }

        //init NMEA listener for GPS time (to negate the device clock drift when not in mobile reception area)
        //This is important for detecting duplicate messages as they rely on a 10 seconds window for validation
        locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(myInstance,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < 24) {
                locationManager.addNmeaListener(new GpsStatus.NmeaListener() {
                    public void onNmeaReceived(long timestamp, String nmea) {
                        if (config.getPreferenceB("USEGPSTIME", false) &&
                                (refTimeSource.equals("") || refTimeSource.equals("GPS"))) {
                            String[] NmeaArray = nmea.split(",");
                            if (NmeaArray.length > 1 &&
                                    (NmeaArray[0].equals("$GPGGA") || NmeaArray[0].equals("$GNRMC"))) {
                                //debug
                                //Processor.APRSwindow += "\n NMEA is :"+nmea;
                                //AndPskmail.mHandler.post(AndPskmail.addtoAPRS);
                                // Some devices do not include decimal seconds
                                //if (NmeaArray[1].indexOf(".") > 4) {
                                if (NmeaArray[1].length() > 5) { //6 or more characters
                                    //String GpsTime = NmeaArray[1].substring(0,NmeaArray[1].indexOf("."));
                                    String GpsTime = NmeaArray[1].substring(0, 6);
                                    GPSTimeAcquired = true; //Mark that we have acquired time (for the clock colour display and autobeacon time)
                                    GpsTime = "000000" + GpsTime;
                                    //							Processor.APRSwindow += " GpsTime:" + GpsTime + "\n";
                                    GpsTime = GpsTime.substring(GpsTime.length() - 4, GpsTime.length());
                                    int GpsMin = Integer.parseInt(GpsTime.substring(0, 2));
                                    int GpsSec = Integer.parseInt(GpsTime.substring(2, 4));
                                    //Apply leap seconds correction: GPS is 16 seconds faster than UTC as of June 2013.
                                    //Some devices do not apply this automatically (depends on the internal GPS engine)
                                    int leapseconds = config.getPreferenceI("LEAPSECONDS", 0);
                                    GpsSec -= leapseconds;
                                    if (GpsSec < 0) {
                                        GpsSec += 60;
                                        GpsMin--;
                                        if (GpsMin < 0) {
                                            GpsMin += 60;
                                        }
                                    }
                                    //In case of (unexpected) negative leap seconds values
                                    if (GpsSec > 60) {
                                        GpsSec -= 60;
                                        GpsMin++;
                                        if (GpsMin > 60) {
                                            GpsMin -= 60;
                                        }
                                    }
                                    //Compare to current device time and date and calculate the offset to be applied at display
                                    long nowInMilli = System.currentTimeMillis();
                                    Time mytime = new Time();
                                    mytime.set(nowInMilli); //initialized to now
                                    int DeviceTime = mytime.second + (mytime.minute * 60);
                                    //Correction (in seconds)
                                    deviceToRealTimeCorrection = (GpsSec + (GpsMin * 60)) - DeviceTime;
                                    refTimeSource = "GPS";
                                    //Debug
                                    //							Processor.APRSwindow += " Device Time is :" + mytime.minute + ":" + mytime.second + "\n";
                                    //							Processor.APRSwindow += " GPS Time is :" + GpsMin + ":" + GpsSec + "\n";
                                    //							Processor.APRSwindow += " Correction is :" + DeviceToGPSTimeCorrection + "\n";
                                }
                            }
                            //					AndPskmail.mHandler.post(AndPskmail.addtoAPRS);
                            //loggingclass.writelog("Timestamp is :" +timestamp+"   nmea is :"+nmea,
                            //					  null, true);
                        }
                    }
                });

            } else {
                locationManager.addNmeaListener(new OnNmeaMessageListener() {
                    public void onNmeaMessage(String nmea, long timestamp) {
                        if (config.getPreferenceB("USEGPSTIME", false) &&
                                (refTimeSource.equals("") || refTimeSource.equals("GPS"))) {
                            String[] NmeaArray = nmea.split(",");
                            if (NmeaArray.length > 1 &&
                                    (NmeaArray[0].equals("$GPGGA") || NmeaArray[0].equals("$GNRMC"))) {
                                //debug
                                //Processor.APRSwindow += "\n NMEA is :"+nmea;
                                //RadioMsg.mHandler.post(AndPskmail.addtoAPRS);
                                // Some devices do not include decimal seconds
                                //if (NmeaArray[1].indexOf(".") > 4) {
                                if (NmeaArray[1].length() > 5) { //6 or more characters
                                    //String GpsTime = NmeaArray[1].substring(0,NmeaArray[1].indexOf("."));
                                    String GpsTime = NmeaArray[1].substring(0, 6);
                                    GPSTimeAcquired = true; //Mark that we have acquired time (for the clock colour display and autobeacon time)
                                    GpsTime = "000000" + GpsTime;
                                    //							Processor.APRSwindow += " GpsTime:" + GpsTime + "\n";
                                    GpsTime = GpsTime.substring(GpsTime.length() - 4, GpsTime.length());
                                    int GpsMin = Integer.parseInt(GpsTime.substring(0, 2));
                                    int GpsSec = Integer.parseInt(GpsTime.substring(2, 4));
                                    //Apply leap seconds correction: GPS is 16 seconds faster than UTC as of June 2013.
                                    //Some devices do not apply this automatically (depends on the internal GPS engine)
                                    int leapseconds = config.getPreferenceI("LEAPSECONDS", 0);
                                    GpsSec -= leapseconds;
                                    if (GpsSec < 0) {
                                        GpsSec += 60;
                                        GpsMin--;
                                        if (GpsMin < 0) {
                                            GpsMin += 60;
                                        }
                                    }
                                    //In case of (unexpected) negative leap seconds values
                                    if (GpsSec > 60) {
                                        GpsSec -= 60;
                                        GpsMin++;
                                        if (GpsMin > 60) {
                                            GpsMin -= 60;
                                        }
                                    }
                                    //Compare to current device time and date and calculate the offset to be applied at display
                                    long nowInMilli = System.currentTimeMillis();
                                    Time mytime = new Time();
                                    mytime.set(nowInMilli); //initialized to now
                                    int DeviceTime = mytime.second + (mytime.minute * 60);
                                    //Correction (in seconds)
                                    deviceToRealTimeCorrection = (GpsSec + (GpsMin * 60)) - DeviceTime;
                                    refTimeSource = "GPS";
                                    //Debug
                                    //							Processor.APRSwindow += " Device Time is :" + mytime.minute + ":" + mytime.second + "\n";
                                    //							Processor.APRSwindow += " GPS Time is :" + GpsMin + ":" + GpsSec + "\n";
                                    //							Processor.APRSwindow += " Correction is :" + DeviceToGPSTimeCorrection + "\n";
                                }
                            }
                            //					RadioMSG.mHandler.post(AndPskmail.addtoAPRS);
                            //loggingclass.writelog("Timestamp is :" +timestamp+"   nmea is :"+nmea,
                            //					  null, true);
                        }
                    }
                });

            }

            /* Old APIs
            locationManager.addNmeaListener(new GpsStatus.NmeaListener() {
                public void onNmeaReceived(long timestamp, String nmea) {
                    if (config.getPreferenceB("USEGPSTIME", true)) {
                        String[] NmeaArray = nmea.split(",");
                        if (NmeaArray[0].equals("$GPGGA")
                                //Only if we haven't time synchronised with a server
                                && (refTimeSource.equals("") || refTimeSource.equals("GPS"))) {
                            //debug
                            //Processor.APRSwindow += "\n NMEA is :"+nmea;
                            //AndPskmail.mHandler.post(AndPskmail.addtoAPRS);
                            // Some devices do not include decimal seconds
                            //if (NmeaArray[1].indexOf(".") > 4) {
                            if (NmeaArray[1].length() > 5) { //6 or more characters
                                //String GpsTime = NmeaArray[1].substring(0,NmeaArray[1].indexOf("."));
                                String GpsTime = NmeaArray[1].substring(0, 6);
                                GPSTimeAcquired = true; //Mark that we have acquired time (for the clock colour display and autobeacon time)
                                GpsTime = "000000" + GpsTime;
                                //							Processor.APRSwindow += " GpsTime:" + GpsTime + "\n";
                                GpsTime = GpsTime.substring(GpsTime.length() - 4, GpsTime.length());
                                int GpsMin = Integer.parseInt(GpsTime.substring(0, 2));
                                int GpsSec = Integer.parseInt(GpsTime.substring(2, 4));
                                //Apply leap seconds correction: GPS is 16 seconds faster than UTC as of June 2013.
                                //Some devices do not apply this automatically (depends on the internal GPS engine)
                                int leapseconds = config.getPreferenceI("LEAPSECONDS", 0);
                                GpsSec -= leapseconds;
                                if (GpsSec < 0) {
                                    GpsSec += 60;
                                    GpsMin--;
                                    if (GpsMin < 0) {
                                        GpsMin += 60;
                                    }
                                }
                                //In case of (unexpected) negative leap seconds values
                                if (GpsSec > 60) {
                                    GpsSec -= 60;
                                    GpsMin++;
                                    if (GpsMin > 60) {
                                        GpsMin -= 60;
                                    }
                                }
                                //Compare to current device time and date and calculate the offset to be applied at display
                                long nowInMilli = System.currentTimeMillis();
                                long timeTarget = nowInMilli;
                                Time mytime = new Time();
                                mytime.set(timeTarget); //initialized to now
                                int DeviceTime = mytime.second + (mytime.minute * 60);
                                deviceToRealTimeCorrection = (GpsSec + (GpsMin * 60)) - DeviceTime;
                                refTimeSource = "GPS";
                                //Debug
                                //							Processor.APRSwindow += " Device Time is :" + mytime.minute + ":" + mytime.second + "\n";
                                //							Processor.APRSwindow += " GPS Time is :" + GpsMin + ":" + GpsSec + "\n";
                                //							Processor.APRSwindow += " Correction is :" + deviceToRealTimeCorrection + "\n";
                            }
                        }
                        //					AndPskmail.mHandler.post(AndPskmail.addtoAPRS);
                        //loggingclass.writelog("Timestamp is :" +timestamp+"   nmea is :"+nmea,
                        //					  null, true);
                    }
                }
            });
            */
        }

        // Register for phone state monitoring
        mTelephonyManager = (TelephonyManager) myContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mCallStateCallback = new CallStateCallback();
            mTelephonyManager.registerTelephonyCallback(
                    myContext.getMainExecutor(), (TelephonyCallback) mCallStateCallback);
        } else {
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    handleCallStateChange(state);
                }
            };
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        // Launch task for time display (Time is GPS time aligned if requested)
        Runnable displayTimeRunnable = new regularActionsRunner();
        Thread displayTimeThread = new Thread(displayTimeRunnable);
        displayTimeThread.start();

        //Update the list of available modems
        Modem.updateModemCapabilityList();

        // Get last mode (if not set, returns -1)
        RMsgProcessor.TxModem = RMsgProcessor.RxModem = Modem.getModeFromSpinner();
        //If we do not have a last mode, this is the first time in the app (use safest default: MT63-1000-Short)
        if (RMsgProcessor.RxModem == -1) RMsgProcessor.RxModem = Modem.getMode("MT63_1000_ST");

        // Get the RSID flags from stored preferences
        Modem.rxRsidOn = config.getPreferenceB("RXRSID", true);
        Modem.txRsidOn = config.getPreferenceB("TXRSID", true);

        //Check if we need to restore the previous session's TO and VIA dropdowns
        if (config.getPreferenceB("RESTORETOANDVIA", false)) {
            restoreToAndVia();
        }

        //Build the list of inbox and/or outbox messages to display
        buildDisplayList();

        // We start with the SMS screen
        if (config.getPreferenceS("HASAGREED", "NO").equals("YES")) {
            displaySms(NORMAL);
        } else {
            displayAbout();
        }

    }


    private void buildDisplayList() {
        //Collect all stored messages for display
        //Prevent updates by GPS while (re-)building the list
        updatingMsgListAdapter = true;
        // Initialize the message array adapter and load Received Messages
        int whichFolders = config.getPreferenceI("WHICHFOLDERS", 3);
        msgDisplayList = new RMsgDisplayList(this, R.layout.smslistview,
                RMsgObject.loadFileListFromFolders(whichFolders, RMsgObject.SORTBYNAME));
        updatingMsgListAdapter = false;
    }


    //Tries to find a match for a given combination of phone number and time
    //Alows wildcards "*" for any incoming numbers and "0" for time-persistent filters
    // Returns "" if no match
    //Examples:
    // *,*,0 (the default, matches any incoming cellular number at any time and sends SMSs to ALL Radio recipients
    // *,joe,0  send any incoming SMS to "joe" only, any time
    // 0412345678,joe,0 sends SMSes from 0412345678 to joe, any time
    // Automatically generated entries look like:
    // 0412345678,joe,1524632518000
    // This entry links the sender of a radio message with the nominated cellular number it was
    //   relayed to, counting time from the epoch as shown after the last comma.
    // Receipts of incoming cellular SMSs which result in a match will update the time of the last exchange to keep the link alive (unless
    //   the link is time persistent, i.e. "0" time.
    private String[] passSMSFilter(String senderNo) {
        String myResult[] = new String[20]; //Max 20 links = max 20 messages to send on receipt of this SMS
        String[] filterList = config.getPreferenceS("SMSLISTENINGFILTER", "*,*,0").split("\\|");
        int toCount = 0;
        //Changed to days intead of hours
        // int maxHoursSinceLastComm = config.getPreferenceI("MAXHOURSSINCELASTCOMM", 24);
        int maxDaysSinceLastComm = config.getPreferenceI("DAYSTOKEEPLINK", 90);
        Long nowTime = System.currentTimeMillis();
        for (int i = 0; i < filterList.length; i++) {
            //Match on time, then on incoming phone number
            String[] thisFilter = filterList[i].split(",");
            //Only properly formed filters
            if (thisFilter.length == 3) {
                long lastCommTime;
                try {
                    //Added left trim as some filters would have a space before the Linux Epoch number
                    lastCommTime = Long.parseLong(RMsgMisc.ltrim(thisFilter[2].trim()));
                } catch (Exception e) {
                    lastCommTime = 1; //Any small number non zero
                }
                String phoneFilter = thisFilter[0].trim();
                if ((lastCommTime == 0 || (lastCommTime + maxDaysSinceLastComm * 3600000L * 24L) > nowTime)
                        && (phoneFilter.equals("*") || senderNo.endsWith(RMsgMisc.lTrimZeros(phoneFilter)))) {
                    //Do we need to update that last communication time
                    if (lastCommTime > 1) {
                        //We had a real time stamp in here, not a zero or a mis-typed number
                        filterList[i] = phoneFilter + "," + thisFilter[1].trim() + "," + nowTime.toString();
                    }
                    //Add the destination callsign for this number and time combination
                    myResult[toCount++] = thisFilter[1].trim();
                    //Make sure we don't max out
                    if (toCount >= myResult.length) {
                        break;
                    }
                } else if (lastCommTime > 1 && (lastCommTime + maxDaysSinceLastComm * 3600000L * 24L) <= nowTime) {
                    //Remove obsolete filters with valid time stamps
                    filterList[i] = "";
                }
            }
        }
        //Rebuild the new filter list with the updated link times, minus the time obsolete filters
        String newSmsFilter = "";
        for (int j = 0; j < filterList.length; j++) {
            if (!filterList[j].equals("")) { //Skip blanked ones
                newSmsFilter = newSmsFilter + filterList[j] + "|";
            }
        }
        SharedPreferences.Editor editor = RadioMSG.mysp.edit();
        editor.putString("SMSLISTENINGFILTER", newSmsFilter.replace("||", "|"));
        editor.commit();
        //Return resulting matches in an array
        return myResult;
    }


    //Tries to find a match for a given combination of email address and time
    //Alows wildcards "*" for any incoming email address and "0" for time-persistent filters
    // Returns "" if no match
    //Examples:
    // *,*,0 (the default, matches any incoming email address at any time and sends email to ALL Radio recipients
    // *,joe,0  send any incoming email to "joe" only, any time
    // myaddress@myprovider.com.au,joe,0 sends SMSes from 0412345678 to joe, any time
    // Automatically generated entries look like:
    // myaddress@myprovider.com.au,joe,1524632518000
    // This entry links the sender of a radio message with the nominated email address it was
    //   relayed to, counting time from the epoch as shown after the last comma.
    // Receipts of incoming emails which result in a match will update the time of the last exchange to keep the link alive (unless
    //   the link is time persistent, i.e. "0" time.
    public static String[] passEmailFilter(String emailAddress) {
        String myResult[] = new String[20]; //Max 20 links = max 20 messages to send on receipt of this email
        String[] filterList = config.getPreferenceS("EMAILLISTENINGFILTER", "*,*,0").split("\\|");
        int toCount = 0;
        //Changed to days intyead of hours
        // int maxHoursSinceLastComm = config.getPreferenceI("MAXHOURSSINCELASTCOMM", 24);
        int maxDaysSinceLastComm = config.getPreferenceI("DAYSTOKEEPLINK", 90);
        Long nowTime = System.currentTimeMillis();
        for (int i = 0; i < filterList.length; i++) {
            //Match on time, then on incoming phone number
            String[] thisFilter = filterList[i].split(",");
            //Only properly formed filters
            if (thisFilter.length == 3) {
                long lastCommTime;
                try {
                    lastCommTime = Long.parseLong(thisFilter[2].trim());
                } catch (Exception e) {
                    lastCommTime = 1; //Any small number non zero
                }
                String emailFilter = thisFilter[0].trim();
                if ((lastCommTime == 0 || (lastCommTime + maxDaysSinceLastComm * 3600000L * 24L) > nowTime)
                        && (emailFilter.equals("*") || emailFilter.toLowerCase(Locale.US).equals(emailAddress.toLowerCase(Locale.US)))) {
                    //Do we need to update that last communication time
                    if (lastCommTime > 1) {
                        //We had a real time stamp in here, not a zero or a mis-typed number
                        filterList[i] = emailFilter + "," + thisFilter[1].trim() + "," + nowTime.toString();
                    }
                    //Add the destination callsign for this number and time combination
                    myResult[toCount++] = thisFilter[1].trim();
                    //Make sure we don't max out
                    if (toCount >= myResult.length) {
                        break;
                    }
                } else if (lastCommTime > 1 && (lastCommTime + maxDaysSinceLastComm * 3600000L * 24L) <= nowTime) {
                    //Remove obsolete filters with valid time stamps
                    filterList[i] = "";
                }
            }
        }
        //Rebuild the new filter list with the updated link times, minus the time obsolete filters
        String newEmailFilter = "";
        for (int j = 0; j < filterList.length; j++) {
            if (!filterList[j].equals("")) { //Skip blanked ones
                newEmailFilter = newEmailFilter + filterList[j] + "|";
            }
        }
        SharedPreferences.Editor editor = RadioMSG.mysp.edit();
        editor.putString("EMAILLISTENINGFILTER", newEmailFilter.replace("||", "|"));
        editor.commit();
        //Return resulting matches in an array
        return myResult;
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Avoid app restart if already running and pressing on the app icon again
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                //Log.w(LOG_TAG, "Main Activity is not the root.  Finishing Main Activity instead of launching.");
                finish();
                return;
            }
        }

        //Debug only: Menu hack to force showing the overflow button (aka menu button) on the
        //   action bar EVEN IF there is a hardware button
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            // presumably, not relevant
        }


        //Enable progress bar in action bar
        //supportRequestWindowFeature(Window.FEATURE_PROGRESS);
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);


        // Get a static copy of the base Context
        myContext = RadioMSG.this;

        // Get a static copy of the activity instance
        this.myInstance = this;

        //Request all permissions up-front and be done with it.
        //If the app can't perform properly with what is requested then
        // abort rather than have a crippled app running
        //Dangerous permissions groups that need to ne asked for:
        //Contacts: for when creating a new mail if we want to get the email address of a contact. Optional.
        //Location: for GPS to send position and to get accurage time for scanning servers. Essential.
        //Microphone: to get the audio input for the modems. Essential.
        //Phone: to disconnect the Bluetooth audio if a phone call comes in. Otherwise we
        //   send the phone call over the radio. Not allowed in Amateur radio or only with severe restrictions. Essential.
        //Storage: to read and write to the SD card. Essential, otherwise why use the app. There is Tivar for Rx only applications.
        //First check if the app already has the permissions

        havePassedAllPermissionsTest = allPermissionsOk();
        if (havePassedAllPermissionsTest) {
            performOnCreate();
        } else {
            requestAllCriticalPermissions();
        }

    }


    //Could be executed when all necessary permissions are allowed
    @TargetApi(Build.VERSION_CODES.O)
    private void performOnStart() {
        //If we are called from the notifications we need to re-update the status of the icons
        if (mOptionsMenu != null) {
            if (trackPOI) {
                mOptionsMenu.findItem(R.id.track).setIcon(R.drawable.trackon);
            } else {
                mOptionsMenu.findItem(R.id.track).setIcon(R.drawable.trackoff);
            }
        }
        // Store preference reference for later (config.java)
        mysp = PreferenceManager.getDefaultSharedPreferences(this);
        // Refresh defaults since we could be coming back
        // from the preference activity

        // Re-initilize modem when NOT busy to use the latest parameters
        if (!Modem.receivingMsg && RXParamsChanged) {
            // Reset flag then stop and restart modem
            RXParamsChanged = false;
            // Cycle modem service off then on
            if (ProcessorON) {
                if (Modem.modemState == Modem.RXMODEMRUNNING) {
                    Modem.stopRxModem();
                    stopService(new Intent(RadioMSG.this, RMsgProcessor.class));
                    ProcessorON = false;
                    // Force garbage collection to prevent Out Of Memory errors
                    // on small RAM devices
                    //System.gc();
                }
            }
            // Wait for modem to stop and then restart
            while (Modem.modemState != Modem.RXMODEMIDLE) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            // Force garbage collection to prevent Out Of Memory errors on small
            // RAM devices
            //System.gc();

            // Get current mode index (returns first position if not in list
            // anymore in case we changed to custom mode list on the current
            // mode)
            RMsgProcessor.RxModem = RMsgProcessor.TxModem = Modem.
                    customModeListInt[Modem.getModeIndex(RMsgProcessor.RxModem)];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(RadioMSG.this, RMsgProcessor.class));
            } else {
                startService(new Intent(RadioMSG.this, RMsgProcessor.class));
            }
            ProcessorON = true;

            // Finally, if we were on the modem screen AND we come back to it,
            // then redisplay in case we changed the waterfall frequency
            if (currentview == MODEMVIEWwithWF) {
                displayModem(NORMAL, true);
            }
        } else { // start if not ON yet AND we haven't paused the modem manually
            if (!ProcessorON && !modemPaused) {
                String NOTIFICATION_CHANNEL_ID = "com.RadioMsg";
                String channelName = "Background Modem";
                NotificationChannel chan = null;
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
                NotificationCompat.Builder mBuilder;
                String chanId = "";
                //New code for support of Android version 8+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
                    chan.setLightColor(Color.BLUE);
                    chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (manager != null) {
                        manager.createNotificationChannel(chan);
                    }
                    chanId = chan.getId();
                }
                mBuilder = new NotificationCompat.Builder(this, chanId)
                        .setSmallIcon(R.drawable.notificationicon)
                        .setContentTitle("Modem ON")
                        .setContentText("Microphone/Bluetooth in use by App")
                        .setOngoing(true);
                // Creates an explicit intent for an Activity in your app
                Intent notificationIntent = new Intent(this, RadioMSG.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                //Google: The stack builder object will contain an artificial back stack for the started Activity.
                // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(RadioMSG.class);
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(notificationIntent);
                TaskStackBuilder nstackBuilder = TaskStackBuilder.create(myContext);
                nstackBuilder.addParentStack(RadioMSG.class);
                nstackBuilder.addNextIntent(notificationIntent);
                PendingIntent pIntent = nstackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(pIntent);
                // VK2ETA notification is done in RMsgProcessor service now
                myNotification = mBuilder.build();
                // Force garbage collection to prevent Out Of Memory errors on
                // small RAM devices
                //System.gc();
                //Start service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(RadioMSG.this, RMsgProcessor.class));
            } else {
                startService(new Intent(RadioMSG.this, RMsgProcessor.class));
            }
                ProcessorON = true;
            }
        }
        RadioMSG.mHandler.post(RadioMSG.updatetitle);


        // If we elected to keep the gps receiver "warm", (re)start GPS listening now
        startRegularGpsUpdates();

    }

    /**
     * Called when the activity is (re)started (to foreground)
     **/
    @Override
    public void onStart() {
        super.onStart();
        //Conditional to having passed the permission tests
        if (havePassedAllPermissionsTest) {
            performOnStart();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {

        super.onResume();
        //USB device init if required
        boolean pttViaRTS = config.getPreferenceB("RTSASPTT", false);
        boolean pttViaDTR = config.getPreferenceB("DTRASPTT", false);
        boolean pttViaCAT = config.getPreferenceB("CATASPTT", false);
        if (pttViaRTS | pttViaDTR | pttViaCAT) {
            //if required, initialised the USB Serial port
            connectUsbDevice();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mExitDialog != null && mExitDialog.isShowing()) {
            mExitDialog.dismiss();
        }
        //Release activity instance
        //myInstance = null;
    }


    //Connect to USB Serial device if present
    public void connectUsbDevice() {

        synchronized (lockUSB) {
            // Find all available drivers from attached devices.
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
            if (availableDrivers.isEmpty()) {
                //middleToastText("PTT via Serial Port Requested, But No Supported Device Found");
                middleToastText(RadioMSG.myContext.getString(R.string.txt_PttViaSerialPortRequestedButNDF)); //"PTT via Serial Port Requested, But No Supported Device Found");
                return;
            }

            //Open a connection to the first available driver.
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            //Ensure we reset the Serial Port so it can be opened again below
            usbSerialPort = null;

            if (connection == null && usbPermission == UsbPermission.Unknown && !manager.hasPermission(driver.getDevice())) {
                middleToastText("Intent to Request Permission");
                usbPermission = UsbPermission.Requested;
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(myContext, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);
                return;
            }
            if (connection == null) {
                if (!manager.hasPermission(driver.getDevice()))
                    //middleToastText("USB Permission Denied");
                    middleToastText("In RadioMSG.java, Permission not granted. Ask Again");
                usbPermission = UsbPermission.Requested;
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(myContext, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);
                //middleToastText("In RadioMSG.java, Permission not granted. Disconnect / Reconnect");
                return;
            }

            if (usbSerialPort == null || !usbSerialPort.isOpen()) {
                usbSerialPort = driver.getPorts().get(0); // Most devices have just one usbSerialPort (usbSerialPort 0)
                try {
                    usbSerialPort.open(connection);
                    usbSerialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_2, UsbSerialPort.PARITY_NONE);
                    //usbSerialPort.setRTS(<default here>);
                    //Same for DTR
                    middleToastText("USB Serial Initialised.");
                } catch (IOException e) {
                    connection.close();
                    middleToastText("Error at USB serial init: " + e);
                }
            } else {
                // Port already open; close the connection we just opened to avoid leaking it
                connection.close();
            }
        }
    }


    /*
     * Only necessary with popup windows
     *
     * @Override public void onConfigurationChanged(Configuration newConfig) {
     * super.onConfigurationChanged(newConfig);
     *
     * if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) { if
     * (RadioMSG.pw != null) { Display display = ((WindowManager)
     * getSystemService(WINDOW_SERVICE)).getDefaultDisplay(); int winWidth =
     * display.getWidth(); int winHeight = display.getHeight();
     * pw.update(winWidth, winHeight); } } if (newConfig.orientation ==
     * Configuration.ORIENTATION_LANDSCAPE) { if (RadioMSG.pw != null) { Display
     * display = ((WindowManager)
     * getSystemService(WINDOW_SERVICE)).getDefaultDisplay(); int winWidth =
     * display.getWidth(); int winHeight = display.getHeight();
     * pw.update(winWidth, winHeight); } }
     *
     * }
     */

    /*
     * Parked code for monitoring the user-controlled enabling of the Bluetooth
     * interface
     *
     * @Override public void onActivityResult(int requestCode, int resultCode,
     * Intent data) { switch (requestCode) { case REQUEST_CONNECT_DEVICE: //
     * When DeviceListActivity returns with a device to connect if (resultCode
     * == Activity.RESULT_OK) { // Get the device MAC address String address =
     * data.getExtras() .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS); //
     * Get the BLuetoothDevice object BluetoothDevice device =
     * mBluetoothAdapter.getRemoteDevice(address); // Attempt to connect to the
     * device mChatService.connect(device); } break;
     *
     * case REQUEST_ENABLE_BT: // When the request to enable Bluetooth returns
     * if (resultCode == Activity.RESULT_OK) { // Bluetooth is now enabled, so
     * set up an audio channel if (Double.valueOf(android.os.Build.VERSION.SDK)
     * >= 8) { mAudioManager.startBluetoothSco(); } //Android 2.1. Needs
     * testinband.apk to be launched before hand
     * mAudioManager.setMode(AudioManager.MODE_IN_CALL);
     * mAudioManager.setBluetoothScoOn(true); //
     * mAudioManager.setSpeakerphoneOn(false); toBluetooth = true; } else { //
     * User did not enable Bluetooth or an error occured toBluetooth = false;
     * Toast.makeText(this, "Bluetooth has not been enabled",
     * Toast.LENGTH_LONG).show(); } } }
     */

//From www.stackoverflow.com

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @TargetApi(19)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKatOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKatOrLater && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    @TargetApi(9)


//Calculate final scaling required to achieve a picture at most X pixels in any axis
    private static double getPictureScale(int photoW, int photoH, int targetLargestDimension) {
        double targetScale = 1.0;
        if (photoW > photoH) {
            targetScale = photoW / targetLargestDimension;
            targetWidth = targetLargestDimension;
            targetHeight = (int) (photoH / targetScale + 0.5);
        } else {
            targetScale = (double) photoH / (double) targetLargestDimension;
            targetHeight = targetLargestDimension;
            targetWidth = (int) (photoW / targetScale + 0.5);
        }
        if (targetScale < 1) {
            targetWidth = photoW;
            targetHeight = photoH;
            return 1.0;
        } else {
            return targetScale;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        final int myrc = requestCode;

        //Share message action
        if (requestCode == SHARE_MESSAGE_RESULT) {
            if (resultCode == RESULT_OK) {
            }
        }

        //Camera/Gallery/File picture request
        if (requestCode == PICTURE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                final boolean isCamera;
                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    if (action == null) {
                        isCamera = false;
                    } else {
                        isCamera = action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                    }
                }
                Uri selectedImageUri;
                if (isCamera) {
                    selectedImageUri = outputFileUri;
                } else {
                    selectedImageUri = data == null ? null : data.getData();
                }

                try {

                    //Scale down image if likely to be too large (max dimension size in User preference)
                    String imageFullFilePath = RadioMSG.getPath(RadioMSG.myContext, selectedImageUri);
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    //Just get the size without the picture data
                    bmOptions.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(imageFullFilePath, bmOptions);
                    int photoW = bmOptions.outWidth;
                    int photoH = bmOptions.outHeight;
                    //Adjust if required (powers of 2 only for scale factor)
                    int targetLargestDimension = config.getPreferenceI("LASTPICTURESIZE", 640); //largest of the two dimensions only
                    double scaleFactor = 1;
                    double finalScale = getPictureScale(photoW, photoH, targetLargestDimension);
                    //Approach final scale by a factor of 2 to avoid pixilation, then scales it to desired dimensions
                    scaleFactor = finalScale / 2; //Or by 4 (see below)
                    if (scaleFactor < 1.0) scaleFactor = 1.0;
                    //Now real decode with sub-sampling only
                    bmOptions.inJustDecodeBounds = false;
                    //Debug, leve at 1 for now, check the effect of 2, 4 etc on final image quality
                    bmOptions.inSampleSize = 1; //(int) scaleFactor;
                    bmOptions.inPurgeable = true;
                    originalImageBitmap = BitmapFactory.decodeFile(imageFullFilePath, bmOptions);
                    //Extract file name by itself
                    //String imageFileName = imageFullFilePath;
                    //if (imageFullFilePath.indexOf("/") != -1) {
                    //    imageFileName = imageFullFilePath.substring(imageFullFilePath.lastIndexOf("/") + 1);
                    //}
                    //Now do the final scaling with filtering
                    //tempImageBitmap = Bitmap.createScaledBitmap(originalImageBitmap, targetWidth, targetHeight, true);
                } catch (Exception e) {
                    loggingclass.writelog("Failed to read and encode picture file" + e.getMessage(), null, true);
                }
                //Update the fields in the UI thread
                //final String myImageFile = "file://" + RadioMSG.getPath(RadioMSG.myContext, selectedImageUri);
                runOnUiThread(new Runnable() {
                    public void run() {
                        /*
                        ImageView myImage = (ImageView) findViewById(R.id.mmsimage);
                        myImage.setImageBitmap(tempImageBitmap);
                        //Update the size and time needed to Tx
                        updateImageModeTime();
                        */
                        //Start with colour picture
                        pictureIsColour = true;
                        //Recover previous resolution value
                        int maxDim = config.getPreferenceI("LASTPICTURESIZE", 640);
                        updateMaxPicDimensionAndColour(maxDim, pictureIsColour);
                        //Enable/disable the send button
                        //Disable send button if no picture selected or HF-Clubs op-mode
                        boolean isHfClubs = (config.getPreferenceS("LASTMODEUSED", "HF-Poor")).equals("HF-Clubs");
                        myButton = (Button) findViewById(R.id.button_send);
                        setTextSize(myButton);
                        myButton.setEnabled(false);
                        if (tempImageBitmap != null && !isHfClubs) {
                            myButton.setEnabled(true);
                        }
                    }
                });
            }
        }
    }

    /* Not used. Keep for auto-forwarding to email.
    if (requestCode == CONTACT_PICKER_RESULT) {
		    if (resultCode == RESULT_OK && data != null) {
			Cursor cursor = null;
			String email = "";
			try {
			    Uri result = data.getData();
			    // get the contact id from the Uri
			    String id = result.getLastPathSegment();

			    // query for everything email
			    cursor = getContentResolver().query(Email.CONTENT_URI,
				    null, Email.CONTACT_ID + "=?", new String[] { id },
				    null);

			    int emailIdx = cursor.getColumnIndex(Email.DATA);

			    // let's just get the first email
			    if (cursor.moveToFirst()) {
				email = cursor.getString(emailIdx);
			    }
			} catch (Exception ex) {
			    loggingclass.writelog("Failed to get email data" + ex.getMessage(), null, true);
			} finally {
			    if (cursor != null) {
				cursor.close();
			    }
			    EditText emailEntry = (EditText) findViewById(R.id.emailaddress);
			    if (emailEntry != null)
				emailEntry.setText(email);
			    if (email.length() == 0) {
				Toast.makeText(this, "No email found for contact.", Toast.LENGTH_SHORT).show();
			    }

			}
		    }
		}
     */


    //Use external apps to select an image file (Camera, file system or Gallery)
//Based on code from www.stackoverflow.com (thank you David Manpearl and Austyn Mahoney)
    private Uri outputFileUri;

    private void openImageIntent() {

// Determine Uri of camera image to save.
        final File root = new File(RMsgProcessor.HomePath + RMsgProcessor.Dirprefix
                + RMsgProcessor.DirTemp + RMsgProcessor.Separator);
        final File sdImageMainDirectory = new File(root, tempAttachPictureFname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

//Camera
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

// Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

// Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        startActivityForResult(chooserIntent, PICTURE_REQUEST_CODE);
    }


    //Opens a popup dialog window for the VOICE ON/OFF feature
// as in Rx Radio sound to Speaker, and Internal Mic sound to Radio for tx
    public void openVoiceDialog() {
        if (!voiceDialogOpened) {
            //Custom alert for Voice on/off
            voiceDialogOpened = true;
            AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(
                    RadioMSG.this);
            LayoutInflater inflater = getLayoutInflater();
            final View myDialoglayout = inflater.inflate(R.layout.voicealertdialog, null);
            myAlertDialog.setView(myDialoglayout);

            myAlertDialog.setMessage("Press Talk and use Microphone to talk through the radio. Return to listening with LISTEN button");
            myAlertDialog.setCancelable(false);
            myAlertDialog.setTitle("VOICE MODE");
            myAlertDialog.setPositiveButton("Return to RMsgUtil Mode", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Modem.stopVoiceListening();
                    Modem.stopSendingVoice();
                    dialog.cancel();
                    voiceDialogOpened = false;
                }
            });

            //Initialize the Talk button
            myButton = (Button) myDialoglayout.findViewById(R.id.button_talk);
            setTextSize(myButton);
            myButton.setBackgroundColor(Color.LTGRAY);
            myButton.setTextColor(Color.BLACK);
            myButton.setEnabled(true);
            myButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!RMsgProcessor.TXActive) {
                        Button thisButton = (Button) v.getRootView().findViewById(R.id.button_talk);
                        setTextSize(myButton);
                        thisButton.setBackgroundColor(Color.RED);
                        thisButton.setEnabled(false);
                        //Now sets the listen button to enabled
                        thisButton = (Button) v.getRootView().findViewById(R.id.button_listen);
                        setTextSize(myButton);
                        thisButton.setBackgroundColor(Color.LTGRAY);
                        thisButton.setEnabled(true);
                        Modem.stopVoiceListening();
                        Modem.startSendingVoice();
                    }
                }
            });

            //Initialize the Listen button
            myButton = (Button) myDialoglayout.findViewById(R.id.button_listen);
            setTextSize(myButton);
            myButton.setBackgroundColor(Color.GREEN);
            myButton.setTextColor(Color.BLACK);
            myButton.setEnabled(false);
            myButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Button thisButton = (Button) v.getRootView().findViewById(R.id.button_listen);
                    setTextSize(myButton);
                    thisButton.setBackgroundColor(Color.GREEN);
                    thisButton.setEnabled(false);
                    //Now sets the talk button to enabled
                    thisButton = (Button) v.getRootView().findViewById(R.id.button_talk);
                    setTextSize(myButton);
                    thisButton.setBackgroundColor(Color.LTGRAY);
                    thisButton.setEnabled(true);
                    Modem.stopSendingVoice();
                    Modem.startVoiceListening();
                }
            });

            myAlertDialog.show();
            //First make sure we are not sending voice (should not)
            Modem.stopSendingVoice();
            //Assumes we want to listen to radio audio when we enter this dialog
            Modem.startVoiceListening();
        }
    }


    static TextView notifCount;
    static int mNotifCount = 5;

    // Option Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        //Save for updates outside the menu manipulation
        mOptionsMenu = menu;
        if (trackPOI) {
            mOptionsMenu.findItem(R.id.track).setIcon(R.drawable.trackon);
            mOptionsMenu.findItem(R.id.track).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            mOptionsMenu.findItem(R.id.track).setIcon(R.drawable.trackoff);
            mOptionsMenu.findItem(R.id.track).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        //Toggle the extra options if on SMS view
        if (currentview == SMSVIEW && config.getPreferenceB("EXPERTMODE")) {
            mOptionsMenu.findItem(R.id.cycledisplay).setVisible(true);
        } else {
            mOptionsMenu.findItem(R.id.cycledisplay).setVisible(false);
        }
        //Display/hide speaker button is not selected in preferences
        if ((config.getPreferenceI("SPEAKERVOLUME", -99) < -10)) {
            //Hide speaker icon
            myInstance.mOptionsMenu.findItem(R.id.cyclespeaker).setVisible(false).setEnabled(false);
        } else {
            switch (speakerOption) {
                case 0:
                    myInstance.mOptionsMenu.findItem(R.id.cyclespeaker).setIcon(R.drawable.speakermuted);
                    break;
                case 1:
                    myInstance.mOptionsMenu.findItem(R.id.cyclespeaker).setIcon(R.drawable.speakeron);
                    break;
                case 2:
                    myInstance.mOptionsMenu.findItem(R.id.cyclespeaker).setIcon(R.drawable.speakerautomute);
                    break;
            }
        }
        return super.onCreateOptionsMenu(menu);
        }


    /*
    // Customize the Option Menu at run time
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        mOptionsMenu = menu;
        menu.clear();
        getMenuInflater().inflate(R.menu.menu, menu);

        if (trackPOI) {
            mOptionsMenu.findItem(R.id.track).setIcon(R.drawable.trackon);
            mOptionsMenu.findItem(R.id.track).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            mOptionsMenu.findItem(R.id.track).setIcon(R.drawable.trackoff);
            mOptionsMenu.findItem(R.id.track).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        //Toggle the extra options if on SMS view
        if (currentview == SMSVIEW && config.getPreferenceB("EXPERTMODE")) {
            mOptionsMenu.findItem(R.id.cycledisplay).setVisible(true);
        } else {
            mOptionsMenu.findItem(R.id.cycledisplay).setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }
    */

    // Option Screen handler
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.cycledisplay:
                if (currentview == SMSVIEW) {
                    if (++smsViewOption > 2) smsViewOption = 0;
                    displaySms(NORMAL);
                }
                break;

            case R.id.cyclespeaker:
                //Hidden in pre-Android 9 devices
                if (++speakerOption > 2) speakerOption = 0;
                switch(speakerOption) {
                    case 0:
                        Modem.setSpeakerOff();
                        item.setIcon(R.drawable.speakermuted);
                        break;
                    case 1:
                        Modem.setSpeakerOn();
                        item.setIcon(R.drawable.speakeron);
                        break;
                    case 2:
                        if (Modem.receivingMsg) {
                            Modem.setSpeakerOn();//open strait away
                        } else {
                            Modem.setSpeakerOff();//close for now
                        }
                        item.setIcon(R.drawable.speakerautomute);
                        break;
                }
                break;
            case R.id.track:
                trackPOI = !trackPOI;
                if (trackPOI) {
                    item.setIcon(R.drawable.trackon);
                    middleToastText("Tracking POIs. Active when moving");
                } else {
                    item.setIcon(R.drawable.trackoff);
                    middleToastText("POI Tracking OFF");
                }
                //Re-draw menu
                this.invalidateOptionsMenu();
                startRegularGpsUpdates();
                if (currentview == SMSVIEW) {
                    displaySms(BOTTOM);
                }
                break;

            case R.id.cancelmsg:
                if (RMsgProcessor.TXActive) {
                    if (Modem.modemIsTuning && Modem.tune) {
                        //Stop tuning
                        Modem.tune = false;
                    } else {
                        //The same as the Stop Tx button in the Modem Screen
                        if (Modem.stopTX != true) {
                            middleToastText("Transmission Cancelled");
                            RadioMSG.mHandler.post(RadioMSG.updatetitle);
                        }
                        Modem.stopTX = true;
                    }
                } else {
                    //Un-queue the last message in the FIFO (not the first one to TX)
                    if (RMsgTxList.getLatest() != null) {
                        middleToastText("Message Cancelled");
                        RadioMSG.mHandler.post(RadioMSG.updatetitle);
                    }
                }
                break;

            case R.id.prefs:
                Intent OptionsActivity = new Intent(RadioMSG.this,
                        myPreferences.class);
                startActivity(OptionsActivity);
                break;

            case R.id.BTon:
                // mBluetoothHeadset.startVoiceRecognition();
                if (mBluetoothAdapter != null) {
                    // Code that works (Manual connect to BT device)
                    if (android.os.Build.VERSION.SDK_INT >= 8) {
                        mAudioManager.startBluetoothSco();
                    }
                    mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                    mAudioManager.setBluetoothScoOn(true);
                    toBluetooth = true;
                }
                break;

            case R.id.BToff:
                toBluetooth = false;
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
                mAudioManager.setBluetoothScoOn(false);
                if (android.os.Build.VERSION.SDK_INT >= 8) {
                    mAudioManager.stopBluetoothSco();
                }
                break;

            //case R.id.voice:
            //    openVoiceDialog();
            //    break;

            case R.id.Silent:
                SharedPreferences.Editor editor = RadioMSG.mysp.edit();
                editor.putString("ALARM", "0");
                editor.commit();
                break;

            case R.id.VibrateOnly:
                SharedPreferences.Editor editor2 = RadioMSG.mysp.edit();
                editor2.putString("ALARM", "1");
                editor2.commit();
                break;

            case R.id.SoundVibrate:
                SharedPreferences.Editor editor3 = RadioMSG.mysp.edit();
                editor3.putString("ALARM", "3");
                editor3.commit();
                break;

            case R.id.SoundOnly:
                SharedPreferences.Editor editor4 = RadioMSG.mysp.edit();
                editor4.putString("ALARM", "2");
                editor4.commit();
                break;

            case R.id.savePreferences:
                config.saveSharedPreferencesToFile("SettingsBackup.bin", false);
                break;

            case R.id.restorePreferences:
                config.loadSharedPreferencesFromFile("SettingsBackup.bin");
                break;

            case R.id.defaultPreferences:
                config.restoreSettingsToDefault();
                break;

            case R.id.manageInbox:
                if (!trackPOI) {
                    manageMsgs(RMsgObject.INBOXONLY);
                } else {
                    middleToastText("Not while in Tracking Mode.\n Stop Tracking first");
                }
                break;

            case R.id.manageSent:
                if (!trackPOI) {
                    manageMsgs(RMsgObject.SENTONLY);
                } else {
                    middleToastText("Not while in Tracking Mode.\n Stop Tracking first");
                }
                break;

            case R.id.exit:
                AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(
                        RadioMSG.this);
                myAlertDialog.setMessage("Are you sure you want to Exit?");
                myAlertDialog.setCancelable(false);
                myAlertDialog.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                exitApplication();
                            }
                        });
                myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                mExitDialog = myAlertDialog.show();
                break;
            case R.id.About:
                displayAbout();
                break;
            case R.id.MessagesScreen:
                displaySms(NORMAL);
                break;
            case R.id.ModemScreen:
                displayModem(NORMAL, false); // no waterfall to start with
                break;
            case R.id.MMSScreen:
                displayMms(NORMAL);
                break;
            case R.id.QuickSMSScreen:
                displaySmsQuickList(NORMAL);
                break;
            case R.id.QuickSMSButtonsScreen:
                displaySmsQuickButtons(NORMAL);
                break;
            //case R.id.Ccir493Screen:
            //    displayCcir493(NORMAL);
            //    break;
            case R.id.commands:
                displayCommandsDialog();
                /*if (selectedTo == "*") {
                    middleToastText("CAN'T Request Time from \"ALL\"\n\nSelect a single TO destination above");
                } else if (RMsgProcessor.matchMyCallWith(selectedTo, false)) {
                    middleToastText("CAN'T Request Time from \"YOURSELF\"\n\nSelect another TO destination above");
                } else {
                    RMsgTxList.addMessageToList(selectedTo, "", "*tim?", //Via is always null, does not make sense to relay time info
                            false, null, 0,
                            null);
                }*/
                break;
            case R.id.record:
                Modem.startRecording();
                break;
        }
        return true;
    }

    private void exitApplication() {

        // Toast.makeText(this, "GoodBye", Toast.LENGTH_SHORT).show();
        // Stop the Modem and Listening Service
        if (ProcessorON) {
            stopService(new Intent(RadioMSG.this,
                    RMsgProcessor.class));
            ProcessorON = false;
        }
        // Stop the GPS if running
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        //Reset forced diversions of audio input / outputs
        Modem.resetSoundToNormal();
        //Unregisters receivers
        if (bluetoothReceiver != null)
            RadioMSG.myInstance.unregisterReceiver(bluetoothReceiver);
        // Stop listening for phone state
        if (mTelephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (mCallStateCallback != null)
                    mTelephonyManager.unregisterTelephonyCallback((TelephonyCallback) mCallStateCallback);
            } else {
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        }
        // Stop listening for received SMSes
        if (listeningForSMSs && smsReceiver != null)
            RadioMSG.myInstance.unregisterReceiver(smsReceiver);
        //If we selected to "Restore preferences at start - Oppo bug workaround", we must
        //   save the current preferences as they contain the latest SMS and email filters
        if (config.getPreferenceB("RESTORESETTINGATSTART", false)) {
            config.saveSharedPreferencesToFile("SettingsBackup.bin", true);
        }
        // Close that activity and return to previous screen
        finish();
        // Close that activity and return to previous screen
        finish();
        // Kill the process
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    public void requestQuickGpsUpdate() {
        //Now request fast periodic updates
        //The cancellation of these requests is done on first receipt of a location fix
        runOnUiThread(new Runnable() {
            @SuppressWarnings({"MissingPermission"})
            public void run() {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 500, // 1/2 a sec in milisecs
                        0, // meters
                        RadioMSG.myInstance.locationListener);
            }
        });
    }


    //Request regular updates of GPS positions
    @SuppressWarnings({"MissingPermission"})
    public void startRegularGpsUpdates() {

        //Start regular updates
        if (trackPOI || config.getPreferenceB("KEEPGPSWARM", false)) {
            int gpsInterval = 0;
            if (trackPOI) {
                gpsInterval = config.getPreferenceI("GPSTRACKINGINTERVAL", 2); //Every 2 seconds
            } else {
                gpsInterval = config.getPreferenceI("GPSINTERVAL", 60); //Every minute
            }
            //return to normal position update to keep the gps receiver "warm"
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, gpsInterval * 1000, // milisecs
                    0, // meters
                    locationListener);
        } else {
            locationManager.removeUpdates(locationListener);
        }
    }


    // Handles callbacks from GPS service
    private class gpsListener implements LocationListener {
        public void onLocationChanged(Location location) {
            //Save latest location regardless
            currentLocation = location;
            //TO-DO: Add test for obsolete requests, like more than 2 minutes old (pointless/misleading sending otherwise)
            if (RMsgTxList.getLength() > 0) {
                //Update all messages with location
                RMsgTxList.updateLocation(location);
            }
            //Request normal regular updates
            startRegularGpsUpdates();
            //Update list if tracking POIs
            if (trackPOI) {
                if (myInstance != null) { //Prevents crash on app restart
                    runOnUiThread(new Runnable() {
                        public void run() {
                            int closest = 0;
                            //Perform tracking update if we are NOT currently re-building the message list
                            //And we are doing more than 1 Km/H
                            if (!updatingMsgListAdapter && currentLocation.getSpeed() > (1 / 3.6)) {
                                //Launch update in background
                                final Runnable mRunnable = new Runnable() {
                                    public void run() {
                                        msgDisplayList.updateClosestPois();
                                    }
                                };
                                mRunnable.run();
                                //Update with latest value if present
                                if (closestPoi != -1) {
                                    msgListView.setSelection(closestPoi);
                                }
                            }
                        }
                    });
                } else {
                    middleToastText("Null myinstance in gpslistener");
                }
            }
        }

        public void onStatusChanged(String s, int i, Bundle b) {
        }

        public void onProviderDisabled(String s) {
            // toastText("GPS turned off - Please turn ON");
            GPSisON = false;
        }

        public void onProviderEnabled(String s) {
            // toastText("GPS turned ON");
            GPSisON = true;
        }
    }


    // Simple text transparent popups (Top of screen)
    public static void topToastText(String message) {
        try {
            myToast.setText(message);
            myToast.setGravity(Gravity.TOP, 0, 100);
            myToast.show();
        } catch (Exception ex) {
            loggingclass.writelog("Toast RMsgUtil error: " + ex.getMessage(), null, true);
        }
    }


    // Simple text transparent popups TOWARDS MIDDLE OF SCREEN
    public static void middleToastText(String message) {
        try {
            myToast.setText(message);
            myToast.setGravity(Gravity.CENTER, 0, 0);
            myToast.show();
        } catch (Exception ex) {
            loggingclass.writelog("Toast RMsgUtil error: " + ex.getMessage(), null, true);
        }
    }


    // Simple text transparent popups TOWARDS MIDDLE OF SCREEN, LONG duration
    public static void longMiddleToastText(String message) {
        try {
            longToast.setText(message);
            longToast.setGravity(Gravity.CENTER, 0, 0);
            longToast.show();
        } catch (Exception ex) {
            loggingclass.writelog("Toast RMsgUtil error: " + ex.getMessage(), null, true);
        }
    }


    // Simple text transparent popups (Bottom of screen)
    public static void bottomToastText(String message) {
        try {
            myToast.setText(message);
            myToast.setGravity(Gravity.BOTTOM, 0, 100);
            myToast.show();
        } catch (Exception ex) {
            loggingclass.writelog("Toast RMsgUtil error: " + ex.getMessage(), null, true);
        }
    }


    public static void screenAnimation(ViewGroup panel, int screenAnimation) {

        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(100);
        set.addAnimation(animation);

        switch (screenAnimation) {

            case NORMAL:
                return;
            // break;

            case RIGHT:
                animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                        -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f);
                break;
            case LEFT:
                animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                        1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f);
                break;

            case TOP:
                animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, -1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f);
                break;

            case BOTTOM:
                animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f);
                break;

        }

        animation.setDuration(200);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(
                set, 0.25f);
        if (panel != null) {
            panel.setLayoutAnimation(controller);
        }
    }


/*
    // Display the Terminal layout and associate it's buttons
    private void displayTerminal(int screenMovement) {

        //Ensure we reset the swipe action
        RadioMSG.inFormDisplay = false;
        //
        // TO-DO: reformat as per other screens
        //
        // Change layout and remember which one we are on
        currentview = TERMVIEW;
        setContentView(R.layout.terminal);
        screenAnimation((ViewGroup) findViewById(R.id.termscreen),
                screenMovement);
        myTermTV = (TextView) findViewById(R.id.terminalview);
        myTermTV.setHorizontallyScrolling(false);
        myTermTV.setTextSize(16);
        myWindow = getWindow();
        //VK2ETA Delay title update until after modem init (capability list is NULL)
        //RadioMSG.mHandler.post(RadioMSG.updatetitle);

        //VK2ETA Debug test
//	setSupportProgressBarIndeterminateVisibility(true);
//	setProgressBarIndeterminateVisibility(true);
//	setSupportProgressBarVisibility(true);
//	setSupportProgressBarIndeterminate(false);
//	setSupportProgress(95);

        // If blank (on start), display version
        if (TerminalBuffer.length() == 0 && !hasDisplayedWelcome) {
            TerminalBuffer = welcomeString;
        } else {
            if (TerminalBuffer.equals(welcomeString)) {
                TerminalBuffer = "";
            }
        }
        hasDisplayedWelcome = true;
        // Reset terminal display in case it was blanked out by a new oncreate
        // call
        myTermTV.setText(TerminalBuffer);
        myTermSC = (ScrollView) findViewById(R.id.terminalscrollview);
        // update with whatever we have already accumulated then scroll
        RadioMSG.mHandler.post(RadioMSG.addtoterminal);
        // Advise which screen we are in
        //middleToastText("Terminal Screen");

        //Initialize the Send Text button (commands in connected mode)
        myButton = (Button) findViewById(R.id.button_sendtext);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String intext = view.getText().toString();

                //No exceptions thrown here???
                //		try {
                if (!Modem.receivingMsg) {
                    //RMsgProcessor.TX_Text += (intext + "\n");
                    // RMsgProcessor.q.send_txrsid_command("ON");
                    //Modem.txData("", "", intext + "\n", 0, 0, false, "");
                } else {
                    bottomToastText("Not while in the middle of receiving a message");
                }
                //		}
                //		catch (Exception ex) {
                //		    loggingclass.writelog("Error reading back CSV file from spreadsheet editor" + ex.getMessage(), null, true);
                //		}
            }
        });
    }




    private void fileNameDialog(String filePath, String templateFileName,
                                String dataBuffer) {
        final String savedFilePath = filePath;
        final String savedDataBuffer = dataBuffer;

        AlertDialog.Builder myAlertBuilder = new AlertDialog.Builder(this);
        LayoutInflater myInflater = LayoutInflater.from(this);
        final View fileNameDialogView = myInflater.inflate(
                R.layout.filenamedialog, null);
        myAlertBuilder.setView(fileNameDialogView).setCancelable(true)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Hide the keyboard since we handle it manually
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(fileNameDialogView.getWindowToken(), 0);
                        dialog.cancel();
                    }
                })
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TextView view = (TextView) fileNameDialogView.findViewById(R.id.edit_text_input);
                        String newFileName = view.getText().toString().trim();
                        if (newFileName.length() == 0) {
                            // Hide the keyboard since we handle it
                            // manually
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(fileNameDialogView.getWindowToken(), 0);
                            dialog.cancel();
                            middleToastText("File Name can't be blank");
                        } else {
                            // Hide the keyboard since we handle it
                            // manually
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(fileNameDialogView.getWindowToken(), 0);
                            // Do something with the file name
                            RMsgUtil.saveMessageAsFile(savedFilePath, newFileName, savedDataBuffer);
                            returnFromFormView();
                        }
                    }
                });

        AlertDialog myFileNameAlert = myAlertBuilder.create();
        EditText view = (EditText) fileNameDialogView.findViewById(R.id.edit_text_input);
        view.setText(templateFileName);
        myFileNameAlert.setTitle("Enter File Name");
        myFileNameAlert.show();

    }


    //Background task to format for displaying only. Result is updated webview.
    class backgroundFormatForDisplay extends AsyncTask<Void, Integer, String> {
        String workingDir = "";
        String fileToDisplay = "";

        // ProgressDialog progress;

        public backgroundFormatForDisplay(String mDir) {
            workingDir = mDir;
            // progress = new ProgressDialog(RadioMSG.myContext);
        }

        public backgroundFormatForDisplay(String mDir, String fileToDisp) {
            workingDir = mDir;
            fileToDisplay = fileToDisp;
            // progress = new ProgressDialog(RadioMSG.myContext);
        }

        @Override
        protected void onPreExecute() {
            // progress.setTitle("Formatting...");
            // progress.setMessage("Wait while Formatting...");
            // progress.show();
        }

        @Override
        protected String doInBackground(Void... arg0) {

//Not yet            mDisplayForm = RMsgUtil.formatForDisplay(workingDir, fileToDisplay, "");
            return "";
        }

        protected void onProgressUpdate(Integer... a) {
        }

        protected void onPostExecute(String result) {
            // progress.dismiss();
            mWebView.loadDataWithBaseURL("", mDisplayForm, "text/html", "UTF-8", "");
            //Special processing for html forms received over radio
            //Replace copy to drafts with "Install Form"
            if (currentview == SMSVIEW && RMsgUtil.formName.startsWith("html_form.")) {
                // Change Copy to Drafts button
                myButton = (Button) findViewById(R.id.button_cpdrafts);
                setTextSize(myButton);
                myButton.setText("Inst. Form");
                myButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        //Prompt to confirm that we will overwrite forms in the DisplayForms and EntryForms folders
                        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(RadioMSG.this);
                        myAlertDialog.setMessage("This will install the received form in the DisplayForms and EntryForms folders, OVERWRITTING any existing forms. Proceed?");
                        myAlertDialog.setCancelable(false);
                        myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //Copy the extracted form into both folders
                                String filePath = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix
                                        + RMsgProcessor.DirEntryForms + RMsgProcessor.Separator;
                                RMsgUtil.saveDataStringAsFile(filePath, mFileName, mDisplayForm);
                                filePath = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix
                                        + RMsgProcessor.DirDisplayForms + RMsgProcessor.Separator;
                                RMsgUtil.saveDataStringAsFile(filePath, mFileName, mDisplayForm);
                            }
                        });
                        myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                        myAlertDialog.show();
                    }
                });

            }
            // Debug
            // RMsgProcessor.PostToTerminal("\nForm:\n" + myDisplayForm +"\n");
        }
    }

*/

/*
    //Background task to format for editing from a new form (new message). Result is updated webview.
    class backgroundNewFormDisplay extends AsyncTask<Void, Integer, String> {

        // ProgressDialog progress;

        public backgroundNewFormDisplay() {
            // progress = new ProgressDialog(RadioMSG.myContext);
        }

        @Override
        protected void onPreExecute() {
            // progress.setTitle("Formatting...");
            // progress.setMessage("Wait while Formatting...");
            // progress.show();
        }

        @Override
        protected String doInBackground(Void... arg0) {
            mDisplayForm = "";
            try {
                // Store the form name for later (when we post the form for
                // example)
                RMsgUtil.formName = mFileName;
                // Read the form into a string
                File f = new File(RMsgProcessor.HomePath + RMsgProcessor.Dirprefix
                        + RMsgProcessor.DirEntryForms + RMsgProcessor.Separator
                        + mFileName);
                FileInputStream fileIS = new FileInputStream(f);
                BufferedReader buf = new BufferedReader(new InputStreamReader(fileIS));
                String readString = new String();
                while ((readString = buf.readLine()) != null) {
                    mDisplayForm += readString + "\n";
                }
            } catch (FileNotFoundException e) {
                loggingclass.writelog("File Not Found error in 'backgroundNewFormDisplay' " + e.getMessage(), null, true);
            } catch (IOException e) {
                loggingclass.writelog("IO Exception error in 'backgroundNewFormDisplay' " + e.getMessage(), null, true);
            }
            // Inject the JavaScript into the form to get the data when the user
            // selects the Submit button
            if (mDisplayForm.contains("</FORM>")) {
                mDisplayForm = mDisplayForm.replaceFirst("</FORM>", "</FORM>"
                        + jsSendNewFormDataInject);
            } else if (mDisplayForm.contains("</form>")) {
                mDisplayForm = mDisplayForm.replaceFirst("</form>", "</form>"
                        + jsSendNewFormDataInject);
            }
            return "";
        }

        protected void onProgressUpdate(Integer... a) {
        }

        protected void onPostExecute(String result) {
            // progress.dismiss();
            String filePath = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix
                    + RMsgProcessor.DirImages + RMsgProcessor.Separator;
            mWebView.loadDataWithBaseURL("", mDisplayForm, "text/html",
                    "UTF-8", null);
        }
    }


    //Background task to format for editing. Result is updated webview.
    class backgroundFormatForEditing extends AsyncTask<Void, Integer, String> {

        String workingDir = "";
        String injectJS = "";

        // ProgressDialog progress;

        public backgroundFormatForEditing(String mDir, String mInjectString) {
            workingDir = mDir;
            injectJS = mInjectString;
            // progress = new ProgressDialog(RadioMSG.myContext);
        }

        @Override
        protected void onPreExecute() {
            // progress.setTitle("Formatting...");
            // progress.setMessage("Wait while Formatting...");
            // progress.show();
        }

        @Override
        protected String doInBackground(Void... arg0) {
            // Format for display and sharing
            // mDisplayForm = RMsgUtil.formatForEditing(RMsgProcessor.DirDrafts,
            // mFileName);
            mDisplayForm = RMsgUtil.formatForEditing(workingDir, mFileName);
            // Inject the JavaScript into the form to get the data when the user
            // selects the Submit button
            if (mDisplayForm.contains("</FORM>")) {
                mDisplayForm = mDisplayForm.replaceFirst("</FORM>", "</FORM>" + injectJS);
            } else if (mDisplayForm.contains("</form>")) {
                mDisplayForm = mDisplayForm.replaceFirst("</form>", "</form>" + injectJS);
            }
            return "";
        }

        protected void onProgressUpdate(Integer... a) {
        }

        protected void onPostExecute(String result) {
            // progress.dismiss();
            mWebView.loadDataWithBaseURL("", mDisplayForm, "text/html",
                    "UTF-8", "");
            // Debug
            // RMsgProcessor.PostToTerminal("\nForm:\n" + mDisplayForm +"\n");
        }
    }

*/

    // Popup Windows Dismiss listener
    OnDismissListener myDismissListerner = new OnDismissListener() {
        public void onDismiss() {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Re-display the lists in case we deleted or moved an item
                    displaySms(BOTTOM);
                    // Update the title if we changed the mode (in Outbox view)
                    RadioMSG.mHandler.post(RadioMSG.updatetitle);
                }
            });
        }
    };


    //Return from a manually managed "pop-up" screen
    public void returnFromFormView() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (hasDeletedMessage) {
                    buildDisplayList();
                }
                inFormDisplay = false;
                // Return to lists display
                displaySms(BOTTOM);
                // Update the title if we changed the mode (in Outbox view)
                RadioMSG.mHandler.post(RadioMSG.updatetitle);
            }
        });
    }


    //Custom spinner that does not trigger on layout display
    private class modeSpinnerListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        boolean userSelect = false;

        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (userSelect) {
                if (!RMsgProcessor.TXActive) {
                    String opMode = (String) parent.getItemAtPosition(position);
                    //Translate to selected modem for this op-mode
                    String thisMode = config.getPreferenceS(opMode, "MFSK32");
                    //Set modem
                    RMsgProcessor.RxModem = RMsgProcessor.TxModem = Modem.getMode(thisMode);
                    ;
                    Modem.changemode(RMsgProcessor.RxModem); // to make the changes effective
                    RadioMSG.mHandler.post(RadioMSG.updatetitle);
                    //Save it for next time
                    saveLastModeUsed(opMode);
                    //Disable the send buttion is opMode is HF-Clubs and we are on the MMS screen
                    if (currentview == MMSVIEW) {
                        //Button mButton = (Button) findViewById(R.id.button_send);
                        //setTextSize(myButton);
                        //if (mButton != null) {
                        //    mButton.setEnabled(false);
                        //}
                        //RE-display updated screen
                        displayMms(NORMAL);
                    }
                } else {
                    middleToastText("Not while transmitting");
                    //Reset to previous op-mode
                    String thisSpinnerMode = config.getPreferenceS("LASTMODEUSED", "HF-Poor");
                    for (int j = 0; j < opModes.length; j++) {
                        if (opModes[j].equals(thisSpinnerMode)) {
                            parent.setSelection(j);
                        }
                    }
                }
            }
            userSelect = false;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub
        }
    }

    //Custom spinner that does not trigger on layout display AND detects long press on items
    private class toSpinnerListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        boolean userSelect = false;

        /* Replaced by check box now
        //Spinner long press listener
        final Handler actionHandler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (sendAliasAndDetails) {
                    Toast.makeText(RadioMSG.this, "Messages will be with alias ONLY.", Toast.LENGTH_LONG).show();
                    sendAliasAndDetails = false;
                    if (toDropdown != null) {
                        //Set the text color of the Spinner's selected view (not a drop down list view)
                        //toDropdown.setSelection(0, true);
                        View v2 = toDropdown.getSelectedView();
                        ((TextView) v2).setTextColor(getResources().getColor(android.R.color.white));
                        //Update the data to be sent
                        if (selectedToAlias.length() > 0) {
                            //We have an alias for this entry, remove anything after the "=" to signify it is an alias with no details
                            selectedTo = selectedTo.replaceFirst("=.+$", "=");
                        }
                    }
                } else {
                    Toast.makeText(RadioMSG.this, "Messages will be with alias AND full details.", Toast.LENGTH_LONG).show();
                    sendAliasAndDetails = true;
                    if (toDropdown != null) {
                        //Set the text color of the Spinner's selected view (not a drop down list view)
                        //toDropdown.setSelection(0, true);
                        View v2 = toDropdown.getSelectedView();
                        ((TextView) v2).setTextColor(Color.RED);
                        //Update the data to be sent
                        if (selectedToAlias.length() > 0) {
                            //We have an alias for this entry, send alias=fullDetails
                            selectedTo = selectedToAlias;
                        }
                    }
                }
            }
        };

        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                actionHandler.postDelayed(runnable, 1000);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                actionHandler.removeCallbacks(runnable);
            }
            userSelect = true;
            return false;
        }
        */

        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (userSelect) {
                String to = (String) parent.getItemAtPosition(position);
                if (sendAliasAndDetails) {
                    ((TextView) view).setTextColor(Color.RED);
                } else {
                    ((TextView) view).setTextColor(getResources().getColor(android.R.color.white));
                }
                if (to.equals("To_ALL")) {
                    selectedTo = "*";
                    selectedToAlias = ""; //None for ALL
                } else {
                    //Save matching destination
                    selectedTo = to;
                    selectedToAlias = toAliasArray[position];
                    if (sendAliasAndDetails) {
                        //Send full details if they exist
                        if (selectedToAlias.length() > 0) {
                            //We have an alias for this entry, send the full details "alias=destination"
                            selectedTo = selectedToAlias;
                        }
                    } else {
                        if (selectedToAlias.length() > 0) {
                            //We have an alias for this entry, append an "=" to signify it is an alias
                            selectedTo = to + "=";
                        }
                    }
                }
                //Save is requested to restora at next launch
                if (config.getPreferenceB("RESTORETOANDVIA", false)) {
                    SharedPreferences.Editor editor = RadioMSG.mysp.edit();
                    editor.putString("SAVEDTO", selectedTo);
                    // Commit the edits!
                    editor.commit();
                }
            } else {
                View v2 = toDropdown.getSelectedView();
                if (v2 != null && !sendAliasAndDetails) {
                    ((TextView) v2).setTextColor(getResources().getColor(android.R.color.white));
                } else if (v2 != null) {
                    ((TextView) v2).setTextColor(Color.RED);
                }
            }
            userSelect = false;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub
        }
    }


    //Custom spinner that does not trigger on layout display
    private class viaSpinnerListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        boolean userSelect = false;

        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (userSelect) {
                String via = (String) parent.getItemAtPosition(position);
                if (via.equals("--DIRECT--")) {
                    selectedVia = "";
                    selectedViaPassword = ""; //None for Direct
                    selectedViaIotPassword = ""; //None for Direct
                } else {
                    //if (currentview == SMSQUICKLISTVIEW || currentview == MMSVIEW || currentview == SMSVIEW) {
                    selectedVia = via.substring(4); //Remove the "via " prefix
                    selectedViaPassword = viaPasswordArray[position];
                    selectedViaIotPassword = viaIotPasswordArray[position];
                    //}
                }
                //Save is requested to restora at next launch
                if (config.getPreferenceB("RESTORETOANDVIA", false)) {
                    SharedPreferences.Editor editor = RadioMSG.mysp.edit();
                    editor.putString("SAVEDVIA", selectedVia);
                    // Commit the edits!
                    editor.commit();
                }

            }
            userSelect = false;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub
        }
    }


    //Custom spinner that does not trigger on layout display
    private class minuteSpinnerListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        boolean userSelect = false;

        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (userSelect) {
                String minute = (String) parent.getItemAtPosition(position);
                if (minute.equals("ASAP")) {
                    selectedTxMinute = -1;
                } else {
                    try {
                        selectedTxMinute = Integer.parseInt(minute);
                    } catch (NumberFormatException e) {
                        //Should not happen by definition
                        selectedTxMinute = 0;
                    }
                }
            }
            userSelect = false;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub
        }
    }

    //Restart modem at new Audio frequency
    private void setAndSaveModemAudioFreq(int audioFreq) {
        //Restart Modem at that frequency
        // Re-initilize modem if we changed the audio frequency
        if (audioFreq != config.getPreferenceI("AFREQUENCY", 1500)) {
            //Save the new audio frequency
            SharedPreferences.Editor editor = RadioMSG.mysp.edit();
            editor.putString("AFREQUENCY", "" + audioFreq);
            // Commit the edits!
            editor.commit();
            // Cycle modem service off then on
            if (ProcessorON) {
                if (Modem.modemState == Modem.RXMODEMRUNNING) {
                    Modem.stopRxModem();
                    stopService(new Intent(RadioMSG.this, RMsgProcessor.class));
                    ProcessorON = false;
                    // Force garbage collection to prevent Out Of Memory errors
                    // on small RAM devices
                    //System.gc();
                }
            }
            // Wait for modem to stop and then restart
            while (Modem.modemState != Modem.RXMODEMIDLE) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(RadioMSG.this, RMsgProcessor.class));
            } else {
                startService(new Intent(RadioMSG.this, RMsgProcessor.class));
            }
            ProcessorON = true;
        }
    }


    //Custom spinner
    private class channelDropdownListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

        public boolean onTouch(View v, MotionEvent event) {



            return false;
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            String channelStr = (String) parent.getItemAtPosition(position);
            int channel;
            try {
                channel = Integer.parseInt(channelStr.substring(3, 4));
            } catch (NumberFormatException e) {
                //Non numeric, therefore must be Custom Channel (value = 0)
                channel = 0;
            }
            //Change Audio frequency and store in preferences
            switch (channel) {
                case 1:
                    setAndSaveModemAudioFreq(600);
                    break;
                case 2:
                    setAndSaveModemAudioFreq(900);
                    break;
                case 3:
                    setAndSaveModemAudioFreq(1200);
                    break;
                case 4:
                    setAndSaveModemAudioFreq(1500);
                    break;
                case 5:
                    setAndSaveModemAudioFreq(1800);
                    break;
                case 6:
                    setAndSaveModemAudioFreq(2100);
                    break;
                case 7:
                    setAndSaveModemAudioFreq(2400);
                    break;
                default:
                    //Do nothing for other frequencies
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub
        }
    }



    //Restore of TO and VIA values and associated aliases and passwords at startup
    private void restoreToAndVia() {

        //Restore To information
        selectedTo = config.getPreferenceS("SAVEDTO", "*");
        //Find alias for this TO value, if present
        String[] toArrayOriginal = ("To_ALL," + config.getPreferenceS("TOLIST", "")).split(",");
        String[] toAliasArrayOriginal = new String[toArrayOriginal.length];
        int validEntries = 0;
        Pattern toPattern = Pattern.compile("^\\s*([0-9a-zA-Z/\\-_@.+]+)\\s*(=?)\\s*(\\S*)\\s*$");
        for (int i = 0; i < toArrayOriginal.length; i++) {
            Matcher msc = toPattern.matcher(toArrayOriginal[i]);
            if (msc.find()) {
                String callSign = "";
                if (msc.group(1) != null) callSign = msc.group(1);
                String separator = "";
                if (msc.group(2) != null) separator = msc.group(2);
                String accessPassword = "";
                if (msc.group(3) != null) accessPassword = msc.group(3);
                if (!callSign.equals("")) {
                    validEntries++;
                    toArrayOriginal[i] = callSign;
                    if (!separator.equals("") && !accessPassword.equals("")) {
                        toAliasArrayOriginal[i] = accessPassword;
                    } else {
                        toAliasArrayOriginal[i] = ""; //As it is copied later on
                    }
                } else {
                    toArrayOriginal[i] = ""; //Blank it out
                    toAliasArrayOriginal[i] = "";
                }
            } else {
                //Malformed to destination, blank it out too
                toArrayOriginal[i] = "";
                toAliasArrayOriginal[i] = "";
            }
        }
        //Blank first password as it corresponds to the "ALL" destination
        toAliasArrayOriginal[0] = "";
        //Copy only non null strings to final array
        toArray = new String[validEntries];
        toAliasArray = new String[validEntries];
        int j = 0;
        for (int i = 0; i < toArrayOriginal.length; i++) {
            if (toArrayOriginal[i].length() > 0) {
                toArray[j] = toArrayOriginal[i];
                if (toAliasArrayOriginal[i].length() > 0) {
                    toAliasArray[j] = toArrayOriginal[i] + "=" + toAliasArrayOriginal[i];
                } else {
                    toAliasArray[j] = ""; //Must be initialized (non null)
                }
                j++;
            }
        }
        //Find matching alias if any
        int toSelect = 0; //default
        for (j = 0; j < toArray.length; j++) {
            if (toArray[j].equals(selectedTo.replaceFirst("=.*\\s*$", ""))) {
                toSelect = j;
            }
        }
        selectedToAlias = toAliasArray[toSelect];

        //Restore Via information now
        selectedVia = config.getPreferenceS("SAVEDVIA", "");
        //Find password for this callsign, if present
        //Fill-in spinner for Via relays, with passwords now
        //Format is Eg: vk2eta-1:pass1, vk2eta-2
        String[] viaArrayOriginal = ("--DIRECT--," + config.getPreferenceS("RELAYLIST", "")).split(",");
        String[] viaPasswordArrayOriginal = new String[viaArrayOriginal.length];
        String[] viaIotPasswordArrayOriginal = new String[viaArrayOriginal.length];
        int viaValidEntries = 0;
        //Add option for IOT password like in: vk2eta-9:relayPW:IOTpw
        Pattern viaPattern = Pattern.compile("^\\s*([0-9a-zA-Z/\\-_.+]+)\\s*(:?)\\s*([^\\s:]*)\\s*(:?)\\s*([^\\s:]*)\\s*$"); //"^\\s*([0-9a-zA-Z/\\-_.+]+)\\s*(:?)\\s*(\\S*)\\s*$"
        for (int i = 0; i < viaArrayOriginal.length; i++) {
            Matcher msc = viaPattern.matcher(viaArrayOriginal[i]);
            if (msc.find()) {
                String callSign = "";
                if (msc.group(1) != null) callSign = msc.group(1);
                String firstSeparator = "";
                if (msc.group(2) != null) firstSeparator = msc.group(2);
                String accessPassword = "";
                if (msc.group(3) != null) accessPassword = msc.group(3);
                String secondSeparator = "";
                if (msc.group(4) != null) secondSeparator = msc.group(2);
                String IOTaccessPassword = "";
                if (msc.group(5) != null) IOTaccessPassword = msc.group(5);
                if (!callSign.equals("")) {
                    viaValidEntries++;
                    viaArrayOriginal[i] = callSign;
                    if (!firstSeparator.equals("") && !accessPassword.equals("")) {
                        viaPasswordArrayOriginal[i] = accessPassword;
                    } else {
                        viaPasswordArrayOriginal[i] = ""; //As it is copied later on
                    }
                    if (!secondSeparator.equals("") && !IOTaccessPassword.equals("")) {
                        viaIotPasswordArrayOriginal[i] = IOTaccessPassword;
                    } else {
                        viaIotPasswordArrayOriginal[i] = ""; //As it is copied later on
                    }
                } else {
                    viaArrayOriginal[i] = ""; //Blank it out
                    viaPasswordArrayOriginal[i] = "";
                }
            } else {
                //Malformed via destination, blank it out too
                viaArrayOriginal[i] = "";
                viaPasswordArrayOriginal[i] = "";
                viaIotPasswordArrayOriginal[i] = "";
            }
        }
        //Blank first password as it corresponds to the "--DIRECT--" entry (no relay)
        viaPasswordArrayOriginal[0] = "";
        //Copy only non null strings to final array
        viaArray = new String[viaValidEntries];
        viaPasswordArray = new String[viaValidEntries];
        viaIotPasswordArray = new String[viaValidEntries];
        j = 0;
        for (int i = 0; i < viaArrayOriginal.length; i++) {
            if (viaArrayOriginal[i].length() > 0) {
                if (i > 0) {
                    //After the "--DIRECT--" entry
                    viaArray[j] = "via " + viaArrayOriginal[i];
                    viaPasswordArray[j] = viaPasswordArrayOriginal[i];
                    viaIotPasswordArray[j++] = viaIotPasswordArrayOriginal[i];
                } else {
                    viaArray[j] = viaArrayOriginal[i];
                    viaPasswordArray[j] = ""; //Initialise to empty string, not null element
                    viaIotPasswordArray[j++] = "";
                }
            }
        }
        selectedViaPassword = "";
        selectedViaIotPassword = "";
        //Re-select last selection
        for (j = 0; j < viaArray.length; j++) {
            if (viaArray[j].equals("via " + selectedVia)) {
                selectedViaPassword = viaPasswordArray[j];
                selectedViaIotPassword = viaIotPasswordArray[j];
            }
        }
    }


    //Common screen setup section
    private void setupDisplay(int thisView) {

        //Save current view if we need to display automatically
        if (thisView > POPUPVIEW) {
            currentview = thisView;
            //We are not in a "popup" display screen
            RadioMSG.inFormDisplay = false;
        }

        //Hide/show extra commands menu item
        this.invalidateOptionsMenu();

        //Fill-in spinner for mode
        Spinner modeDropdown = (Spinner) findViewById(R.id.modes_spinner);
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, opModes);
        modeDropdown.setAdapter(modeAdapter);
        String thisSpinnerMode = config.getPreferenceS("LASTMODEUSED", "HF-Poor");
        for (int j = 0; j < opModes.length; j++) {
            if (opModes[j].equals(thisSpinnerMode)) {
                modeDropdown.setSelection(j);
            }
        }
        modeSpinnerListener mslistener = new modeSpinnerListener();
        modeDropdown.setOnTouchListener(mslistener);
        modeDropdown.setOnItemSelectedListener(mslistener);

        //Fill-in spinner for To address
        String[] toArrayOriginal = ("To_ALL," + config.getPreferenceS("TOLIST", "")).split(",");
        String[] toAliasArrayOriginal = new String[toArrayOriginal.length];
        int validEntries = 0;
        Pattern toPattern = Pattern.compile("^\\s*([0-9a-zA-Z/\\-_@.+]+)\\s*(=?)\\s*(\\S*)\\s*$");
        for (int i = 0; i < toArrayOriginal.length; i++) {
            Matcher msc = toPattern.matcher(toArrayOriginal[i]);
            if (msc.find()) {
                String callSign = "";
                if (msc.group(1) != null) callSign = msc.group(1);
                String separator = "";
                if (msc.group(2) != null) separator = msc.group(2);
                String accessPassword = "";
                if (msc.group(3) != null) accessPassword = msc.group(3);
                if (!callSign.equals("")) {
                    validEntries++;
                    toArrayOriginal[i] = callSign;
                    if (!separator.equals("") && !accessPassword.equals("")) {
                        toAliasArrayOriginal[i] = accessPassword;
                    } else {
                        toAliasArrayOriginal[i] = ""; //As it is copied later on
                    }
                } else {
                    toArrayOriginal[i] = ""; //Blank it out
                    toAliasArrayOriginal[i] = "";
                }
            } else {
                //Malformed to destination, blank it out too
                toArrayOriginal[i] = "";
                toAliasArrayOriginal[i] = "";
            }
        }
        //Blank first password as it corresponds to the "ALL" destination
        toAliasArrayOriginal[0] = "";
        //Copy only non null strings to final array
        toArray = new String[validEntries];
        toAliasArray = new String[validEntries];
        int j = 0;
        for (int i = 0; i < toArrayOriginal.length; i++) {
            if (toArrayOriginal[i].length() > 0) {
                toArray[j] = toArrayOriginal[i];
                if (toAliasArrayOriginal[i].length() > 0) {
                    toAliasArray[j] = toArrayOriginal[i] + "=" + toAliasArrayOriginal[i];
                } else {
                    toAliasArray[j] = ""; //Must be initialized (non null)
                }
                j++;
            }
        }
        toDropdown = (Spinner) findViewById(R.id.to_spinner);
        ArrayAdapter<String> toAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, toArray);
        toDropdown.setAdapter(toAdapter);
        //Re-select last selection
        int toSelect = 0; //default
        for (j = 0; j < toArray.length; j++) {
            if (toArray[j].equals(selectedTo.replaceFirst("=.*\\s*$", ""))) {
                toSelect = j;
            }
        }
        toDropdown.setSelection(toSelect);
        toSpinnerListener toListener = new toSpinnerListener();
        toDropdown.setOnTouchListener(toListener);
        toDropdown.setOnItemSelectedListener(toListener);

        // Initialize the Send Alias and Details check box
        checkbox = (CheckBox) findViewById(R.id.fullalias);
        checkbox.setChecked(sendAliasAndDetails);
        checkbox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    // Store preference
                    sendAliasAndDetails = true;
                    if (toDropdown != null) {
                        //Set the text color of the Spinner's selected view (not a drop down list view)
                        //toDropdown.setSelection(0, true);
                        View v2 = toDropdown.getSelectedView();
                        ((TextView) v2).setTextColor(Color.RED);
                        //Update the data to be sent
                        if (selectedToAlias.length() > 0) {
                            //We have an alias for this entry, send alias=fullDetails
                            selectedTo = selectedToAlias;
                        }
                    }
                } else {
                    // Store preference
                    sendAliasAndDetails = false;
                    if (toDropdown != null) {
                        //Set the text color of the Spinner's selected view (not a drop down list view)
                        //toDropdown.setSelection(0, true);
                        View v2 = toDropdown.getSelectedView();
                        ((TextView) v2).setTextColor(getResources().getColor(android.R.color.white));
                        //Update the data to be sent
                        if (selectedToAlias.length() > 0) {
                            //We have an alias for this entry, remove anything after the "=" to signify it is an alias with no details
                            selectedTo = selectedTo.replaceFirst("=.+$", "=");
                        }
                    }
                }
            }
        });

        //Fill-in spinner for Via relays, with password for relaying and password for IOT access
        //valid formats are Eg: vk2eta-1:relayPW:IotPW, vk2eta-2:relayPW, vk2eta-3::IotPW, vk2eta-4
        String[] viaArrayOriginal = ("--DIRECT--," + config.getPreferenceS("RELAYLIST", "")).split(",");
        String[] viaPasswordArrayOriginal = new String[viaArrayOriginal.length];
        String[] viaIotPasswordArrayOriginal = new String[viaArrayOriginal.length];
        viaArrayOriginal[0] = "--DIRECT--";
        viaPasswordArrayOriginal[0] = "";
        viaIotPasswordArrayOriginal[0] = "";
        int viaValidEntries = 1;
        Pattern viaPattern = Pattern.compile("^\\s*([0-9a-zA-Z/\\-_.+]+)\\s*(:?)\\s*([^\\s:]*)\\s*(:?)\\s*([^\\s:]*)\\s*$"); //"^\\s*([0-9a-zA-Z/\\-_.+]+)\\s*(:?)\\s*(\\S*)\\s*$");
        for (int i = 1; i < viaArrayOriginal.length; i++) {
            Matcher msc = viaPattern.matcher(viaArrayOriginal[i]);
            if (msc.find()) {
                String callSign = "";
                if (msc.group(1) != null) callSign = msc.group(1);
                String separator = "";
                if (msc.group(2) != null) separator = msc.group(2);
                String accessPassword = "";
                if (msc.group(3) != null) accessPassword = msc.group(3);
                String IOTaccessPassword = "";
                if (msc.group(5) != null) IOTaccessPassword = msc.group(5);
                if (!callSign.equals("")) {
                    viaValidEntries++;
                    viaArrayOriginal[i] = callSign;
                    if (!accessPassword.equals("") || !IOTaccessPassword.equals("") ) {
                        viaPasswordArrayOriginal[i] = accessPassword;
                        viaIotPasswordArrayOriginal[i] = IOTaccessPassword;
                    } else {
                        viaPasswordArrayOriginal[i] = ""; //As it is copied later on
                        viaIotPasswordArrayOriginal[i] = ""; //As it is copied later on
                    }
                } else {
                    viaArrayOriginal[i] = ""; //Blank it out
                    viaPasswordArrayOriginal[i] = "";
                    viaIotPasswordArrayOriginal[i] = "";
                }
            } else {
                //Malformed Via destination, blank it out too
                viaArrayOriginal[i] = "";
                viaPasswordArrayOriginal[i] = "";
                viaIotPasswordArrayOriginal[i] = "";
            }
        }
        //Blank first password as it corresponds to the "--DIRECT--" entry (no relay)
        viaPasswordArrayOriginal[0] = "";
        viaIotPasswordArrayOriginal[0] = "";
        //Copy only non null strings to final array
        viaArray = new String[viaValidEntries];
        viaPasswordArray = new String[viaValidEntries];
        viaIotPasswordArray  = new String[viaValidEntries];
        j = 0;
        for (int i = 0; i < viaArrayOriginal.length; i++) {
            if (viaArrayOriginal[i].length() > 0) {
                if (i > 0) {
                    //After the "--DIRECT--" entry
                    viaArray[j] = "via " + viaArrayOriginal[i];
                    viaPasswordArray[j] = viaPasswordArrayOriginal[i];
                    viaIotPasswordArray[j++] = viaIotPasswordArrayOriginal[i];
                } else {
                    viaArray[j] = viaArrayOriginal[i];
                    viaPasswordArray[j] = ""; //Initialise to empty string, not null element
                    viaIotPasswordArray[j++] = "";
                }
            }
        }
        Spinner viaDropdown = (Spinner) findViewById(R.id.via_spinner);
        if (config.getPreferenceB("EXPERTMODE")) {
            ArrayAdapter<String> viaAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, viaArray);
            viaDropdown.setAdapter(viaAdapter);
            //Re-select last selection
            for (j = 0; j < viaArray.length; j++) {
                if (viaArray[j].equals("via " + selectedVia)) {
                    viaDropdown.setSelection(j);
                }
            }
            viaSpinnerListener viaListener = new viaSpinnerListener();
            viaDropdown.setOnTouchListener(viaListener);
            viaDropdown.setOnItemSelectedListener(viaListener);
        } else {
            viaDropdown.setVisibility(View.GONE);
            //Also remove the Alias and Details checkbox as it is irrelevant then
            checkbox = (CheckBox) findViewById(R.id.fullalias);
            checkbox.setVisibility(View.GONE);
        }
        //Minute dropdown for scanning servers
        Spinner minuteDropdown = (Spinner) findViewById(R.id.minute_spinner);
        if (minuteDropdown != null) {
            if (config.getPreferenceB("EXPERTMODE")) {
                ArrayAdapter<String> minuteAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, minuteArray);
                minuteDropdown.setAdapter(minuteAdapter);
                //Re-select last selection
                if (selectedTxMinute == -1) {
                    minuteDropdown.setSelection(0);
                } else {
                    minuteDropdown.setSelection(selectedTxMinute + 1);
                }
                minuteSpinnerListener minuteListener = new minuteSpinnerListener();
                minuteDropdown.setOnTouchListener(minuteListener);
                minuteDropdown.setOnItemSelectedListener(minuteListener);
            } else {
                minuteDropdown.setVisibility(View.GONE);
            }
        }
        //Channel dropdown for ELWHA type audio channels selection
        Spinner channelDropdown = (Spinner) findViewById(R.id.channel_spinner);
        if (channelDropdown != null) {
            ArrayAdapter<String> channelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, channelArray);
            channelDropdown.setAdapter(channelAdapter);
            //Re-select last selection
            channelDropdown.setSelection(getChannelFromAudioFreq());
            channelDropdownListener channelDropdownListener = new channelDropdownListener();
            //channelDropdown.setOnTouchListener(channelDropdownListener);
            channelDropdown.setOnItemSelectedListener(channelDropdownListener);
        }

        //AudioSignal Strength indicator
        AudioLevel = (ProgressBar) findViewById(R.id.audio_level);
    }



    private int getChannelFromAudioFreq() {
        int selectedChannel = 0;

        int audioFreq = config.getPreferenceI("AFREQUENCY", 1500); //Defaults to 1500Hz (== middle channel number 4)
        switch (audioFreq) {
            case 600:
                selectedChannel = 1;
                break;
            case 900:
                selectedChannel = 2;
                break;
            case 1200:
                selectedChannel = 3;
                break;
            case 1500:
                selectedChannel = 4;
                break;
            case 1800:
                selectedChannel = 5;
                break;
            case 2100:
                selectedChannel = 6;
                break;
            case 2400:
                selectedChannel = 7;
                break;
            default:
                selectedChannel = 0;
        }
        return selectedChannel;
    }

    //Provides a list of message for selection and subsequent action (delete, export, archive)
    private void manageMsgs(int whichFolders) {
        // Prevent swipping out of the View
        inFormDisplay = true;
        //Save to final variable
        final int myWhichFolders = whichFolders;
        //Show the form and associated buttons
        setContentView(R.layout.smsmanage);
//debug set to inbox only for now
        final ArrayList<String> manageList = RMsgObject.loadManageListFromFolder(myWhichFolders, RMsgObject.SORTBYNAME);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                //android.R.layout.simple_list_item_multiple_choice,
                R.layout.smsmanagelistview, manageList);
        //Setup listview
        manageView = (ListView) findViewById(R.id.msglist);
        manageView.setAdapter(adapter);
        manageView.setDividerHeight(8);
        manageView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        manageView.performHapticFeedback(MODE_APPEND);

        //Initialize the Archive button
        myButton = (Button) findViewById(R.id.button_archive);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String selected = "";
                int cntChoice = manageView.getCount();
                SparseBooleanArray sparseBooleanArray = manageView.getCheckedItemPositions();
                int processed = 0;
                for (int i = 0; i < cntChoice; i++) {
                    if (sparseBooleanArray.get(i)) {
                        selected = manageView.getItemAtPosition(i).toString();
                        int fileNamePos = selected.lastIndexOf(", On ");
                        if (fileNamePos != -1 && Character.isDigit(selected.charAt(fileNamePos + 5))) {
                            processed++;
                            String mFileName = selected.substring(fileNamePos + 5) + ".txt";
                            String thisFolder = (myWhichFolders == RMsgObject.INBOXONLY ?
                                    RMsgProcessor.DirInbox : RMsgProcessor.DirSent);
                            RMsgUtil.copyAnyFile(thisFolder, mFileName, RMsgProcessor.DirArchive, false);
                            RMsgUtil.deleteFile(thisFolder, mFileName, false);
                            mFileName = selected.substring(fileNamePos + 5) + ".png";
                            RMsgUtil.copyAnyFile(RMsgProcessor.DirImages, mFileName, RMsgProcessor.DirArchive, false);
                            RMsgUtil.deleteFile(RMsgProcessor.DirImages, mFileName, false);
                            //mFileName = selected.substring(fileNamePos + 3) + ".wav";
                            //RMsgUtil.copyAnyFile(RMsgProcessor.DirVoice, mFileName, RMsgProcessor.DirArchive, false);
                        }
                    }
                }
                RadioMSG.topToastText(processed + " Messages Moved to \"Archive\" Folder");
                //re-display updated list
                manageMsgs(myWhichFolders);
            }
        });

        //Initialize the Return button
        myButton = (Button) findViewById(R.id.button_return);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                returnFromFormView();
            }
        });
        //Rebuilt the list of messages as we may have deleted some messages
        buildDisplayList();

        //Initialize the Export button
        myButton = (Button) findViewById(R.id.button_export);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String selected = "";
                int cntChoice = manageView.getCount();
                SparseBooleanArray sparseBooleanArray = manageView.getCheckedItemPositions();
                List<RMsgObject> msgList = new ArrayList<RMsgObject>();
                int processed = 0;
                for (int i = 0; i < cntChoice; i++) {
                    if (sparseBooleanArray.get(i)) {
                        selected = manageView.getItemAtPosition(i).toString();
                        int fileNamePos = selected.lastIndexOf(", On ");
                        if (fileNamePos != -1 && Character.isDigit(selected.charAt(fileNamePos + 5))) {
                            String mFileName = selected.substring(fileNamePos + 5) + ".txt";
                            RMsgObject mMessage = new RMsgObject();
                            String thisFolder = (myWhichFolders == RMsgObject.INBOXONLY ?
                                    RMsgProcessor.DirInbox : RMsgProcessor.DirSent);
                            mMessage = RMsgObject.extractMsgObjectFromFile(thisFolder, mFileName, false);
                            if (mMessage.position != null) {
                                msgList.add(mMessage);
                                processed++;
                            }
                        }
                    }
                }
                if (processed == 0) {
                    return;
                }
                Calendar c1 = Calendar.getInstance(TimeZone.getDefault());
                String fileName = String.format(Locale.US, "%04d", c1.get(Calendar.YEAR)) + "-" +
                        String.format(Locale.US, "%02d", c1.get(Calendar.MONTH) + 1) + "-" +
                        String.format(Locale.US, "%02d", c1.get(Calendar.DAY_OF_MONTH)) + "_" +
                        String.format(Locale.US, "%02d%02d%02d", c1.get(Calendar.HOUR_OF_DAY),
                                c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND)) + ".gpx";
                GPXWriter.writeGpxFile(fileName, "RadioMsgTrack", msgList);
                RadioMSG.topToastText(processed + " Messages Exported to GPX track file " + fileName);
            }
        });

        //Initialize the Delete button
        myButton = (Button) findViewById(R.id.button_delete);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(RadioMSG.myContext);
                myAlertDialog.setMessage("Are you sure you want to Delete These Messages?");
                myAlertDialog.setCancelable(false);
                myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String selected = "";
                        int cntChoice = manageView.getCount();
                        SparseBooleanArray sparseBooleanArray = manageView.getCheckedItemPositions();
                        int processed = 0;
                        for (int i = 0; i < cntChoice; i++) {
                            if (sparseBooleanArray.get(i)) {
                                selected = manageView.getItemAtPosition(i).toString();
                                //Date format = ", On 2022-03-06 at 07:27:40"
                                int fileNamePos = selected.lastIndexOf(", On ");
                                if (fileNamePos != -1 && Character.isDigit(selected.charAt(fileNamePos + 5))) {
                                    processed++;
                                    String mFileName = selected.substring(fileNamePos + 5) + ".txt";
                                    String thisFolder = (myWhichFolders == RMsgObject.INBOXONLY ?
                                            RMsgProcessor.DirInbox : RMsgProcessor.DirSent);
                                    RMsgUtil.deleteFile(thisFolder, mFileName, false);
                                    mFileName = selected.substring(fileNamePos + 5) + ".png";
                                    RMsgUtil.deleteFile(RMsgProcessor.DirImages, mFileName, false);
                                    //mFileName = selected.substring(fileNamePos + 3) + ".wav";
                                    //RMsgUtil.deleteFile(RMsgProcessor.DirVoice, mFileName, false);
                                }
                            }
                        }
                        RadioMSG.topToastText(processed + " Messages Deleted");
                        //re-display updated list
                        manageMsgs(myWhichFolders);
                    }
                });
                myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                myAlertDialog.show();
            }
        });

        //Initialize the Select ALL button
        myButton = (Button) findViewById(R.id.button_selectall);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < manageList.size(); i++) {
                    manageView.setItemChecked(i, true);
                }
            }
        });

        //Initialize the Select NONE button
        myButton = (Button) findViewById(R.id.button_selectnone);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < manageList.size(); i++) {
                    manageView.setItemChecked(i, false);
                }
            }
        });

        //Initialize the Invert Selection button
        myButton = (Button) findViewById(R.id.button_invertselection);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                SparseBooleanArray sparseBooleanArray = manageView.getCheckedItemPositions();
                for (int i = 0; i < manageList.size(); i++) {
                    if (sparseBooleanArray.get(i)) {
                        manageView.setItemChecked(i, false);
                    } else {
                        manageView.setItemChecked(i, true);
                    }
                }
            }
        });

        //Initialize the Select GPS Positions button
        myButton = (Button) findViewById(R.id.button_selectgps);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String mMsgString;
                for (int i = 0; i < manageList.size(); i++) {
                    mMsgString = manageView.getItemAtPosition(i).toString();
                    if (mMsgString.contains("Position: ")) {
                        manageView.setItemChecked(i, true);
                    }
                }
            }
        });

    }


    //Display the relay (or Pskmail server) command options
    private void displayCommandsDialog() {

        //Save previous view
        //int previousView = currentview;
        // Prevent swipping out of the View
        inFormDisplay = true;
        //Show the form and associated buttons
        setContentView(R.layout.servercontrolview);
        //Init common spinners and GPS stuff
        setupDisplay(POPUPVIEW);
        //Monitor the radio buttons and change the visibility of the data fields below
        RadioGroup actionRadioGroup = (RadioGroup) findViewById(R.id.actionRadioGroup);
        actionRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                try {
                    switch (checkedId) {
                        case R.id.timesync:
                        case R.id.scanon:
                        case R.id.mute:
                        case R.id.unmute:
                            //No parameters needed
                            View item = findViewById(R.id.forsomanytext);
                            item.setEnabled(false);
                            item = findViewById(R.id.rbminutes);
                            item.setEnabled(false);
                            item = findViewById(R.id.rbhours);
                            item.setEnabled(false);
                            item = findViewById(R.id.forcallsign);
                            item.setEnabled(false);
                            item = findViewById(R.id.foraddress);
                            item.setEnabled(false);
                            break;
                        case R.id.scanoff:
                            item = findViewById(R.id.forsomanytext);
                            item.setEnabled(true);
                            item = findViewById(R.id.rbminutes);
                            item.setEnabled(true);
                            item = findViewById(R.id.rbhours);
                            item.setEnabled(true);
                            item = findViewById(R.id.forcallsign);
                            item.setEnabled(false);
                            item = findViewById(R.id.foraddress);
                            item.setEnabled(false);
                            break;
                        case R.id.unlink:
                            item = findViewById(R.id.forsomanytext);
                            item.setEnabled(false);
                            item = findViewById(R.id.rbminutes);
                            item.setEnabled(false);
                            item = findViewById(R.id.rbhours);
                            item.setEnabled(false);
                            item = findViewById(R.id.forcallsign);
                            item.setEnabled(true);
                            item = findViewById(R.id.foraddress);
                            item.setEnabled(true);
                            break;
                        default:
                            //Do nothing
                    }
                } catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        //No parameters needed on itinial selection (time sync)
        View item = findViewById(R.id.forsomanytext);
        item.setEnabled(false);
        item = findViewById(R.id.rbminutes);
        item.setEnabled(false);
        item = findViewById(R.id.rbhours);
        item.setEnabled(false);
        item = findViewById(R.id.forcallsign);
        item.setEnabled(false);
        item = findViewById(R.id.foraddress);
        item.setEnabled(false);
        //Preset the callsign field with our callsign
        EditText etxt = findViewById(R.id.forcallsign);
        etxt.setText(config.getPreferenceS("CALL").trim());
        //Preset the time field at 5 (minutes)
        etxt = findViewById(R.id.forsomanytext);
        etxt.setText("5");
        //Initialize the Cancel button
        myButton = (Button) findViewById(R.id.button_cancel);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                displaySms(BOTTOM);
            }
        });

        //Initialize the Send button
        myButton = (Button) findViewById(R.id.button_sendcommand);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                boolean stayHere = false; //Assume all data usable
                RadioGroup actionRadioGroup = findViewById(R.id.actionRadioGroup);
                int checkedId = actionRadioGroup.getCheckedRadioButtonId();
                TextView forsomany = findViewById(R.id.forsomanytext);
                int amount;
                try {
                    amount = Integer.parseInt(forsomany.getText().toString());
                } catch (NumberFormatException e) {
                    amount = 0;
                }
                RadioGroup unitsGroup = findViewById(R.id.unitsGroup);
                int checkedUnit = unitsGroup.getCheckedRadioButtonId();
                String suffix = checkedUnit == R.id.rbminutes ? "m" : "h";
                TextView forcallsign = findViewById(R.id.forcallsign);
                String client = forcallsign.getText().toString();
                TextView foraddress = findViewById(R.id.foraddress);
                String address = foraddress.getText().toString();
                if (selectedTo.equals("*") && selectedVia.equals("")) {
                    middleToastText("Must select a relay/server to send the command to (use To or Via above");
                } else {
                    switch (checkedId) {
                        case R.id.timesync:
                            //Send time request to remote station
                            RMsgTxList.addMessageToList(selectedTo, selectedVia, "*tim?",
                                    false, null, 0, null);
                            break;
                        case R.id.scanoff:
                            if (amount == 0 || (amount > 600 && suffix.equals("m")) || (amount > 24 && suffix.equals("h"))) {
                                middleToastText("Time between 1 minute and 24 hours");
                                stayHere = true;
                            } else {
                                //Send scan off command (we do not allow for infinite scan off for the moment)
                                RMsgTxList.addMessageToList(selectedTo, selectedVia, "*cmd s off " + amount + " " + suffix,
                                        false, null, 0L, null);
                            }
                            break;
                        case R.id.scanon:
                            RMsgTxList.addMessageToList(selectedTo, selectedVia, "*cmd s on",
                                    false, null, 0L, null);
                            break;
                        case R.id.mute:
                            RMsgTxList.addMessageToList(selectedTo, selectedVia, "*cmd mute",
                                    false, null, 0L, null);
                            break;
                        case R.id.unmute:
                            RMsgTxList.addMessageToList(selectedTo, selectedVia, "*cmd unmute",
                                    false, null, 0L, null);
                            break;
                        case R.id.unlink:
                            if (address.trim().length() == 0) {
                                middleToastText("Must specify an email address OR a cellular number OR an Alias");
                                stayHere = true;
                            } else if (client.trim().length() == 0) {
                                middleToastText("Must specify a Callsign");
                                stayHere = true;
                            } else {
                                RMsgTxList.addMessageToList(selectedTo, selectedVia, "*cmd unlink " + client.trim() +
                                        " " + address.trim(), false, null, 0L, null);
                            }
                            break;
                        default:
                            //Do nothing
                    }
                    if (!stayHere) displaySms(BOTTOM);
                }
            }
        });
    }


    //Set the button text size based on user's preferences
    private void setTextSize(Button thisButton) {
        int textSize = config.getPreferenceI("BUTTONTEXTSIZE", 12);
        if (textSize < 7) textSize = 7;
        if (textSize > 20) textSize = 20;
        thisButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
    }


    // Display one of the message list layout and associate it's buttons
    private void displaySms(int screenAnimation) {

        setContentView(R.layout.smsview);
        screenAnimation((ViewGroup) findViewById(R.id.smsmailscreen),
                screenAnimation);
        //Set screen always ON if selected
        if (config.getPreferenceB("KEEPSCREENON", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        //Init common spinners and GPS stuff
        setupDisplay(SMSVIEW);
        float msgListViewLayoutWeight = 0.85f;
        View v;
        if (smsViewOption == 0) {
            v = findViewById(R.id.spinnerlayout2);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = findViewById(R.id.FrameLayout3);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = findViewById(R.id.FrameLayout5);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            msgListViewLayoutWeight += 0.1f;
        } else if (smsViewOption == 1) {
            v = findViewById(R.id.spinnerlayout2);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
            }
            v = findViewById(R.id.FrameLayout3);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = findViewById(R.id.FrameLayout5);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
        } else if (smsViewOption == 2) {
            v = findViewById(R.id.spinnerlayout2);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
            }
            v = findViewById(R.id.FrameLayout3);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
            }
            v = findViewById(R.id.FrameLayout5);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
            }
            msgListViewLayoutWeight -= 0.1f;
        }

        //ELWHA display options (Channel and Audio Input Level indicator
        int usageMode = config.getPreferenceI("USAGEMODE", 0);
        if (usageMode == 1) {
            v = findViewById(R.id.channelandlevellayout);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
                msgListViewLayoutWeight -= 0.1f;
            }
        } else {
            //Remove the ELWHA specific fields
            v = findViewById(R.id.channelandlevellayout);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
        }

        // Load Received Messages
        msgListView = findViewById(R.id.msglist);
        //Adjust the layout weight depending on above displayed items
        ((LinearLayout.LayoutParams) msgListView.getLayoutParams()).weight = msgListViewLayoutWeight;
        msgListView.setAdapter(msgDisplayList);
        msgListView.setDividerHeight(8);
        msgListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        //Are we returning from a popup?
        if (firstVisibleSmsItem >= 0) {
            msgListView.setSelection(firstVisibleSmsItem);
        } else {
            msgListView.setSelection(msgDisplayList.getsize() - 1);//end
        }
        firstVisibleSmsItem = -1; //Consumed
        msgListView.performHapticFeedback(MODE_APPEND);

        //Restore the data entry field at the bottom
        EditText myView = (EditText) findViewById(R.id.edit_text_out);
        myView.setText(savedSmsMessage);
        myView.setSelection(myView.getText().length());
        //Add a textwatcher to save the text as it is being typed. Used for periodic beacons
        myView.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable arg0) {
            }

            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
            }

            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                savedSmsMessage = arg0.toString();
            }

        });


        //Do we have Single Click on RMsgUtil List enabled?
        if (config.getPreferenceB("SHORTPRESSONLIST", false)) {
            msgListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    // When short clicked, display the message if more than an SMS
                    RMsgDisplayItem mDisplayItem = (RMsgDisplayItem) parent.getItemAtPosition(position);
                    //Save first visible item to come back to when we dismiss the popup
                    firstVisibleSmsItem = msgListView.getFirstVisiblePosition();
                    //Display message info
                    mFileName = mDisplayItem.mMessage.fileName;
                    String msgFolder = mDisplayItem.myOwn ? RMsgProcessor.DirSent : RMsgProcessor.DirInbox;
                    final RMsgObject mMessage = RMsgObject.extractMsgObjectFromFile(msgFolder, mFileName, true);
                    if (mMessage.picture != null) {
                        //We have a picture and maybe a position and smsview too
                        // Prevent swipping out of the WebView
                        inFormDisplay = true;
                        //Show the form and associated buttons
                        setContentView(R.layout.smsshortpresspopup);

                        // Initialize the return button
                        myButton = (Button) findViewById(R.id.button_return);
                        setTextSize(myButton);
                        myButton.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                returnFromFormView();
                            }
                        });
                        //Build picture filename
                        //String pictureFn = mFileName.replace(".txt", ".png");
                        //String filePath = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix
                        //        + RMsgProcessor.DirImages + RMsgProcessor.Separator;
                        ImageView picView = (ImageView) findViewById(R.id.mmsimage);
                        picView.setImageBitmap(mMessage.picture);
                        TextView myText = (TextView) findViewById(R.id.fromtext);
                        myText.setText("From: " + mMessage.from + " @ " + mFileName.replace(".txt", ""));
                        myText = (TextView) findViewById(R.id.smstext);
                        if (!mMessage.sms.equals("")) {
                            myText.setText(mMessage.sms);
                        } else {
                            myText.setVisibility(View.GONE);
                        }
                        myText = (TextView) findViewById(R.id.positiontext);
                        if (mMessage.msgHasPosition && mMessage.position != null) {
                            //myText.setText(mMessage.getLatLongString());
                            String formattedString = "Pos: " + mMessage.getLatLongString() + "\n" +
                                    ((int) (mMessage.position.getSpeed() * 3.6));
                            if (mMessage.positionAge > 0) {
                                formattedString += " Km/h,  " +
                                        mMessage.positionAge + " second(s) late";
                            } else {
                                formattedString += " Km/h";
                            }
                            myText.setText(formattedString);
                        } else {
                            myText.setVisibility(View.GONE);
                        }
                        myText = (TextView) findViewById(R.id.picturetext);
                        if (!mMessage.pictureString.equals("")) {
                            myText.setText("Pic: " + mMessage.pictureString);
                        } else {
                            myText.setVisibility(View.GONE);
                        }
                        // Where is it? button
                        myButton = (Button) findViewById(R.id.button_whereisit);
                        setTextSize(myButton);
                        if (mMessage.msgHasPosition && mMessage.position != null &&
                                isPackageInstalled("com.github.ruleant.getback_gps")) {
                            //We have a position info
                            myButton.setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    String geoFormat = RMsgObject.getGeoFormat(mMessage);
                                    Uri uri = Uri.parse(geoFormat);
                                    Intent myIntent = new Intent(Intent.ACTION_VIEW, uri);
                                    myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    myIntent.setClassName("com.github.ruleant.getback_gps",
                                            "com.github.ruleant.getback_gps.MainActivity");
                                    myContext.startActivity(myIntent);
                                }
                            });
                            myButton.setEnabled(true);
                        } else {
                            myButton.setEnabled(false);
                        }
                        //Manage the position button
                        myButton = (Button) findViewById(R.id.button_gpsposition);
                        setTextSize(myButton);
                        if (mMessage.msgHasPosition && mMessage.position != null) {
                            myButton.setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    String geoFormat = RMsgObject.getGeoFormat(mMessage);
                                    Uri uri = Uri.parse(geoFormat);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    myContext.startActivity(intent);
                                }
                            });
                        } else {
                            myButton.setEnabled(false);
                        }
                        // Share/Send over internet button
                        myButton = (Button) findViewById(R.id.button_share);
                        setTextSize(myButton);
                        myButton.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                Intent shareIntent = RMsgObject.shareInfoIntent(mMessage);
                                startActivityForResult(Intent.createChooser(shareIntent, "Send Message..."), SHARE_MESSAGE_RESULT);
                            }
                        });
                    } else if (mMessage.msgHasPosition && mMessage.position != null &&
                            isPackageInstalled("com.github.ruleant.getback_gps")) {
                        //We have a position info, display the Where is app if installed
                        String geoFormat = RMsgObject.getGeoFormat(mMessage);
                        Uri uri = Uri.parse(geoFormat);
                        Intent myIntent = new Intent(Intent.ACTION_VIEW, uri);
                        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        myIntent.setClassName("com.github.ruleant.getback_gps",
                                "com.github.ruleant.getback_gps.MainActivity");
                        myContext.startActivity(myIntent);
                    } else {
                        middleToastText("Nothing More to display");
                    }
                    return;
                }
            });
        }

        //Now for Longpress
        msgListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                //On long press, offer other options (Delete, Share, Forward over radio...etc)
                //Prevent swipping out of the View
                inFormDisplay = true;
                //Reset delete flag
                hasDeletedMessage = false;
                //Save first visible item to come back to when we dismiss the popup
                firstVisibleSmsItem = msgListView.getFirstVisiblePosition();
                //String msgText = (String) parent.getItemAtPosition(position);
                RMsgDisplayItem mDisplayItem = (RMsgDisplayItem) parent.getItemAtPosition(position);
                //int fileNamePos = mMessage.formatForList().lastIndexOf(" @ ");
                //if (fileNamePos != -1) {
                //    mFileName = msgText.substring(fileNamePos + 3) + ".txt";
                //}
                mFileName = mDisplayItem.mMessage.fileName;
                //Get the message including binary data
                final String msgFolder = mDisplayItem.myOwn ? RMsgProcessor.DirSent : RMsgProcessor.DirInbox;
                final RMsgObject mMessage = RMsgObject.extractMsgObjectFromFile(msgFolder, mFileName, true);
                //Show the form and associated buttons
                setContentView(R.layout.smslongpresspopup);
                //Change background based if checksum is not valid
                if (!mMessage.crcValid && !mDisplayItem.myOwn) {
                    ScrollView smsPopupScreen = (ScrollView) findViewById(R.id.smspopupscreen);
                    smsPopupScreen.setBackgroundColor(getResources().getColor(R.color.DARKRED));
                }
                //set spinners for mode and to, but don't record that view as current
                setupDisplay(POPUPVIEW);
                //Set the from, smsview , image and position textviews
                //Hide blank ones
                TextView myText = (TextView) findViewById(R.id.fromtext);
                String addrString = "From: " + mMessage.from + "\nTo: " +
                        (mMessage.to.equals("*") ? "All" : mMessage.to);
                if (!mMessage.via.equals("")) {
                    addrString += "\nVia: " + mMessage.via;
                }
                if (!mMessage.relay.equals("")) {
                    addrString += "\nRelay by: " + mMessage.relay;
                }
                addrString += " @ " + mFileName.replace(".txt", "");
                myText.setText(addrString);
                myText = (TextView) findViewById(R.id.smstext);
                if (!mMessage.sms.equals("")) {
                    myText.setText("Msg: " + RMsgMisc.unescape(mMessage.sms));
                } else {
                    myText.setVisibility(View.GONE);
                }
                myText = (TextView) findViewById(R.id.positiontext);
                if (mMessage.msgHasPosition && mMessage.position != null) {
                    //myText.setText("Pos: " + mMessage.getLatLongString());
                    String formattedString = "Pos: " + mMessage.getLatLongString() + "\n" +
                            ((int) (mMessage.position.getSpeed() * 3.6));
                    if (mMessage.positionAge > 0) {
                        formattedString += " Km/h,  " +
                                mMessage.positionAge + " second(s) late";
                    } else {
                        formattedString += " Km/h";
                    }
                    myText.setText(formattedString);
                } else {
                    myText.setVisibility(View.GONE);
                }
                myText = (TextView) findViewById(R.id.picturetext);
                if (!mMessage.pictureString.equals("")) {
                    myText.setText("Pic: " + mMessage.pictureString);
                } else {
                    myText.setVisibility(View.GONE);
                }
                //Do we have a picture
                if (mMessage.picture != null) {
                    ImageView picView = (ImageView) findViewById(R.id.mmsimage);
                    picView.setImageBitmap(mMessage.picture);
                }

                // Return button
                myButton = (Button) findViewById(R.id.button_return);
                setTextSize(myButton);
                myButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        returnFromFormView();
                    }
                });

                // Where is it? button
                myButton = (Button) findViewById(R.id.button_whereisit);
                setTextSize(myButton);
                if (mMessage.msgHasPosition && mMessage.position != null &&
                        isPackageInstalled("com.github.ruleant.getback_gps")) {
                    //We have a position info
                    myButton.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            String geoFormat = RMsgObject.getGeoFormat(mMessage);
                            Uri uri = Uri.parse(geoFormat);
                            Intent myIntent = new Intent(Intent.ACTION_VIEW, uri);
                            myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            myIntent.setClassName("com.github.ruleant.getback_gps",
                                    "com.github.ruleant.getback_gps.MainActivity");
                            myContext.startActivity(myIntent);
                        }
                    });
                    myButton.setEnabled(true);
                } else {
                    myButton.setEnabled(false);
                }

                // Map View button
                myButton = (Button) findViewById(R.id.button_mapview);
                setTextSize(myButton);
                if (mMessage.msgHasPosition && mMessage.position != null) {
                    //We have a position info
                    myButton.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            String geoFormat = RMsgObject.getGeoFormat(mMessage);
                            Uri uri = Uri.parse(geoFormat);
                            Intent myIntent = new Intent(Intent.ACTION_VIEW, uri);
                            myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            myContext.startActivity(myIntent);
                        }
                    });
                    myButton.setEnabled(true);
                } else {
                    myButton.setEnabled(false);
                }

                // Delete button
                myButton = (Button) findViewById(R.id.button_delete);
                setTextSize(myButton);
                myButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(RadioMSG.myContext);
                        myAlertDialog.setMessage("Are you sure you want to Delete This Message?");
                        myAlertDialog.setCancelable(false);
                        myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                RMsgUtil.deleteFile(msgFolder, mFileName, true);// Advise deletion
                                if (mMessage.picture != null) {
                                    String pictureFileName = mFileName.replace(".txt", ".png");
                                    RMsgUtil.deleteFile(RMsgProcessor.DirImages, pictureFileName, false);// Advise deletion
                                }
                                //Reset delete flag
                                hasDeletedMessage = true;
                                returnFromFormView();
                            }
                        });
                        myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                        myAlertDialog.show();
                    }
                });

                // Share/Send over internet button
                myButton = (Button) findViewById(R.id.button_share);
                setTextSize(myButton);
                myButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        Intent shareIntent = RMsgObject.shareInfoIntent(mMessage);
                        startActivityForResult(Intent.createChooser(shareIntent, "Send Message..."), SHARE_MESSAGE_RESULT);
                    }
                });

                // Re-send button
                myButton = (Button) findViewById(R.id.button_radioresend);
                setTextSize(myButton);
                if (!mDisplayItem.myOwn) {
                    myButton.setEnabled(false);
                }
                myButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (RMsgProcessor.matchMyCallWith(mMessage.from, false)) {
                            //From me, therefore send as is.
                            RMsgObject.resendMessage(mMessage, msgFolder);
                        } else {
                            //Not mine, warn to use forward instead
                            RadioMSG.middleToastText("Not sent by You, use FORWARD button instead");
                        }
                    }
                });

                // Forward button
                myButton = (Button) findViewById(R.id.button_forward);
                setTextSize(myButton);
                //Always allow forward if we sent to the wrong target
                // if (RMsgProcessor.matchMyCallWith(mMessage.from, false)) {
                //    myButton.setEnabled(false);
                //}
                myButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        RMsgObject.forwardMessage(mMessage, msgFolder);
                    }
                });

                // Position request button
                myButton = (Button) findViewById(R.id.button_requestposition);
                setTextSize(myButton);
                myButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {

                        if (RMsgProcessor.matchMyCallWith(mMessage.from, false)) {
                            middleToastText("CAN'T Request Positions from \"Yourself\"");
                        } else {
                            RMsgTxList.addMessageToList(mMessage.from, RadioMSG.selectedVia, "*pos?",
                                    false, null, 0,
                                    null);
                        }
                    }
                });

                // Reply button
                myButton = (Button) findViewById(R.id.button_reply);
                setTextSize(myButton);
                if (mDisplayItem.myOwn) {
                    myButton.setEnabled(false);
                } else {
                    myButton.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            //RMsgObject.forwardMessage(mMessage);
                            TextView myView = (TextView) findViewById(R.id.edit_text_out);
                            String intext = myView.getText().toString();
                            if (!intext.equals("")) {
                                //RMsgTxList.addMessageToList (mMessage.from, //RadioMSG.selectedVia, intext,
                                //Also strip alias of details if this is the mode we operate in
                                String toStr = mMessage.from;
                                if (!sendAliasAndDetails) {
                                    if (toStr.matches(".+=.+")) { //"Alias=Details" format
                                        toStr = toStr.substring(0, toStr.indexOf("=") + 1);
                                    }
                                }
                                //Always reply via the relay if any was used to receive that message
                                RMsgTxList.addMessageToList(toStr, mMessage.relay, intext,
                                        false, null, 0,
                                        null);
                                ((EditText) myView).getText().clear();
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            }
                        }
                    });
                }

                return true; //consumed the event here
            }
        });

        //Initialize the Inquire button
        myButton = (Button) findViewById(R.id.button_inquire);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                if (selectedTo.equals("*")) {
                    middleToastText("CAN'T Inquire to ALL\n\nSelect TO destination above");
                } else {
                    RMsgTxList.addMessageToList(RadioMSG.selectedTo, RadioMSG.selectedVia, "*snr?",
                            false, null, 0,
                            null);
                }
            }
        });

        //Initialize the Time Sync button
        myButton = (Button) findViewById(R.id.button_timesync);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                if ((selectedTo.equals("*") && selectedVia.equals(""))
                        || (!selectedTo.equals("*") && !selectedVia.equals(""))) {
                    middleToastText("CAN'T Request Re-Send from \"ALL\"\n\nSelect a single TO destination above OR a VIA relay");
                } else if (RMsgProcessor.matchMyCallWith(selectedTo, false) || RMsgProcessor.matchMyCallWith(selectedVia, false)) {
                    middleToastText("CAN'T Request Re-Send from \"YOURSELF\"\n\nSelect another TO destination above");
                } else {
                    RMsgTxList.addMessageToList(RadioMSG.selectedTo, RadioMSG.selectedVia, "*tim?",
                            false, null, 0,
                            null);
                }

                //Test
                //doRestart(RadioMSG.this);
            }
        });

        //Initialize the Scan off 5 minutes button
        myButton = (Button) findViewById(R.id.button_scanoff5m);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (selectedTo.equals("*") && selectedVia.equals("")) {
                    middleToastText("CAN'T Request Re-Send from \"ALL\"\n\nSelect a single TO destination above OR a VIA relay");
                } else if (RMsgProcessor.matchMyCallWith(selectedTo, false) || RMsgProcessor.matchMyCallWith(selectedVia, false)) {
                    middleToastText("CAN'T Request Re-Send from \"YOURSELF\"\n\nSelect another TO destination above");
                } else {
                    RMsgTxList.addMessageToList(RadioMSG.selectedTo, RadioMSG.selectedVia, "*cmd s off 5 m",
                            false, null, 0,
                            null);
                }
            }
        });

        //Initialize the Scan off 15 minutes button
        myButton = (Button) findViewById(R.id.button_scanoff15m);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (selectedTo.equals("*") && selectedVia.equals("")) {
                    middleToastText("CAN'T Request Re-Send from \"ALL\"\n\nSelect a single TO destination above OR a VIA relay");
                } else if (RMsgProcessor.matchMyCallWith(selectedTo, false) || RMsgProcessor.matchMyCallWith(selectedVia, false)) {
                    middleToastText("CAN'T Request Re-Send from \"YOURSELF\"\n\nSelect another TO destination above");
                } else {
                    RMsgTxList.addMessageToList(RadioMSG.selectedTo, RadioMSG.selectedVia, "*cmd s off 15 m",
                            false, null, 0,
                            null);
                }
            }
        });

        //Initialize the Send SMS button
        myButton = (Button) findViewById(R.id.button_sms);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView myView = (TextView) findViewById(R.id.edit_text_out);
                String intext = myView.getText().toString();
                if (!intext.equals("")) {
                    RMsgTxList.addMessageToList(RadioMSG.selectedTo, RadioMSG.selectedVia, intext,
                            false, null, 0,
                            null);
                    ((EditText) myView).getText().clear();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });

        //Initialize the Request Re-Send SMS button ("Message for Me?")
        myButton = (Button) findViewById(R.id.button_requestresend);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView myView = (TextView) findViewById(R.id.edit_text_out);
                String intext = myView.getText().toString();
                if (selectedTo.equals("*") && selectedVia.equals("")) {
                    middleToastText("CAN'T Request Re-Send from \"ALL\"\n\nSelect a single TO destination above OR a VIA relay");
                } else if (RMsgProcessor.matchMyCallWith(selectedTo, false) || RMsgProcessor.matchMyCallWith(selectedVia, false)) {
                    middleToastText("CAN'T Request Re-Send from \"YOURSELF\"\n\nSelect another TO destination above");
                } else {
                    ((EditText) myView).getText().clear();
                    //Remove TO is there is via data as we never relay *qtc? messages
                    // RMsgTxList.addMessageToList(selectedTo, selectedVia, "*qtc?" + " " + intext,
                    String toStr = selectedVia.equals("") ? selectedTo : "*";
                    RMsgTxList.addMessageToList(toStr, selectedVia, "*qtc?" + " " + intext,
                            false, null, 0,
                            null);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });
        //Help Popup dialog when long click
        myButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
/*                middleToastText("By default request the last message." + "" +
                        "\nAlternatively In the text field, enter a number and an optional letter:\n" +
                        "\nX M for messages received in the last X minutes" +
                        "\nX H for the last X hours, X D for the last X days" +
                        "\nX P for the last X position messages" +
                        "\nL for messages since last re-send request"
                );
*/
                if (selectedTo.equals("*") && selectedVia.equals("")) {
                    middleToastText("CAN'T Request Re-Send from \"ALL\"\n\nSelect a single TO destination above OR a VIA relay");
                } else if (RMsgProcessor.matchMyCallWith(selectedTo, false) || RMsgProcessor.matchMyCallWith(selectedVia, false)) {
                    middleToastText("CAN'T Request Re-Send from \"YOURSELF\"\n\nSelect another TO destination above");
                } else {
                    //Open dialog popup to get the details of the request
                    AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(RadioMSG.myContext);
                    // only valid for api 21 and above, inflate manually
                    // myAlertDialog.setView(R.layout.resendpopupdialog);
                    LayoutInflater inflater = (LayoutInflater) RadioMSG.myContext
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    //View dialogLayout = inflater.inflate(R.layout.resendpopupdialog, (ViewGroup) findViewById(R.id.resenddialog));
                    View dialogLayout = inflater.inflate(R.layout.resendpopupdialog, null);
                    myAlertDialog.setView(dialogLayout);
                    myAlertDialog.setCancelable(false);
                    myAlertDialog.setPositiveButton("Request", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String resendString = "";
                            resendString = " " + (forceRelayingQTC ? "r" : "") + howManyToResend + (whatToResend.length() > 0 ? " " + whatToResend : "");
                            dialog.dismiss();
                            //Remove To if we use a via data as we never relay *qtc? messages
                            // RMsgTxList.addMessageToList("*", selectedVia, "*qtc?" + resendString, false, null, 0, null);
                            String toStr = (selectedVia.equals("") || forceRelayingQTC )? selectedTo : "*";
                            RMsgTxList.addMessageToList(toStr, selectedVia, "*qtc?" + resendString, false, null, 0, null);
                        }
                    });
                    myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
                    //Monitor changes in the "howMany" buttons
                    RadioGroup howManyGroup = (RadioGroup) dialogLayout.findViewById(R.id.howmany);
                    //set default to "1"
                    howManyGroup.check(R.id.howmany_L);
                    howManyToResend = "l"; //Lower case L = email/sms since last request
                    // Add the Listener to the RadioGroup
                    howManyGroup.setOnCheckedChangeListener(
                            new RadioGroup.OnCheckedChangeListener() {
                                // Check which radio button has been clicked
                                @Override
                                public void onCheckedChanged(RadioGroup group,
                                                             int checkedId) {
                                    switch (checkedId) {
                                        case R.id.howmany_1:
                                            howManyToResend = "1";
                                            break;
                                        case R.id.howmany_2:
                                            howManyToResend = "2";
                                            break;
                                        case R.id.howmany_3:
                                            howManyToResend = "3";
                                            break;
                                        case R.id.howmany_5:
                                            howManyToResend = "5";
                                            break;
                                        case R.id.howmany_7:
                                            howManyToResend = "7";
                                            break;
                                        case R.id.howmany_10:
                                            howManyToResend = "10";
                                            break;
                                        case R.id.howmany_20:
                                            howManyToResend = "20";
                                            break;
                                        case R.id.howmany_L:
                                            //Since last qtc request
                                            howManyToResend = "l";
                                            break;
                                    }
                                }
                            });
                    //Monitor changes in the "howmany" buttons
                    RadioGroup whatGroup = (RadioGroup) dialogLayout.findViewById(R.id.what);
                    //set default to "1"
                    whatGroup.check(R.id.what_3);
                    whatToResend = "e";
                    // Add the Listener to the RadioGroup
                    whatGroup.setOnCheckedChangeListener(
                            new RadioGroup.OnCheckedChangeListener() {
                                // Check which radio button has been clicked
                                @Override
                                public void onCheckedChanged(RadioGroup group,
                                                             int checkedId) {
                                    switch (checkedId) {
                                        case R.id.what_1:
                                            //All messages
                                            whatToResend = "";
                                            break;
                                        case R.id.what_2:
                                            //Positions only
                                            whatToResend = "p";
                                            break;
                                        case R.id.what_3:
                                            //Emails, short version
                                            whatToResend = "e";
                                            break;
                                        case R.id.what_4:
                                            //Emails, long version
                                            whatToResend = "f";
                                            break;
                                        case R.id.what_5:
                                            //in the last X minute(s)
                                            whatToResend = "m";
                                            break;
                                        case R.id.what_6:
                                            //in the last X hour(s)
                                            whatToResend = "h";
                                            break;
                                        case R.id.what_7:
                                            //in the last X day(s)
                                            whatToResend = "d";
                                            break;
                                        case R.id.what_8:
                                            //in the last X day(s)
                                            whatToResend = "w";
                                            break;
                                    }
                                }
                            });
                    // Initialize the Force Relaying check box
                    checkbox = (CheckBox) dialogLayout.findViewById(R.id.forcerelaying);
                    checkbox.setChecked(false);
                    checkbox.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            if (((CheckBox) v).isChecked()) {
                                if (!selectedVia.equals("")) {
                                    forceRelayingQTC = true;
                                } else {
                                    ((CheckBox) v).setChecked(false);
                                }
                            } else {
                                forceRelayingQTC = false;
                            }
                        }
                    });
                    myAlertDialog.show();
                }
                return true;
            }
        });

        // Initialize the send Position button
        myButton = (Button) findViewById(R.id.button_position);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //Request a location update. The sending of the position is done inside the location listener
                //See if a text message needs to be sent at the same time
                TextView myView = (TextView) findViewById(R.id.edit_text_out);
                String smsMessage = myView.getText().toString();
                //Clear text now
                ((EditText) myView).getText().clear();
                RMsgUtil.sendPosition(smsMessage);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                bottomToastText("Getting GPS Fix, then sending position");
            }
        });

        // Position request button
        myButton = (Button) findViewById(R.id.button_requestposition);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (selectedTo == "*") {
                    middleToastText("CAN'T Request Positions from \"ALL\"\n\nSelect a single TO destination above");
                } else if (RMsgProcessor.matchMyCallWith(selectedTo, false)) {
                    middleToastText("CAN'T Request Positions from \"YOURSELF\"\n\nSelect another TO destination above");
                } else {
                    RMsgTxList.addMessageToList(selectedTo, selectedVia, "*pos?",
                            false, null, 0,
                            null);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });

        /* Not used at present until custom dictionaries are available
        //Initialize the voice SMS button
        myButton = (Button) findViewById(R.id.button_smsvoice);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final SpeechRecognizer speechrecon = SpeechRecognizer.createSpeechRecognizer(RadioMSG.myContext);
                final Intent recognizerIntent;
                speechrecon.setRecognitionListener(new speechlistener());
                recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-AU");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, RadioMSG.myInstance.getPackageName());
                int websearch = config.getPreferenceI("WEBSEARCH", 0);
                if (websearch != 0) {
                    recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                } else {
                    recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                }
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
                int timing = config.getPreferenceI("SIMLM", -1);
                if (timing >= 0) {
                    recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, timing * 1000);
                }
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                //recognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
                speechrecon.startListening(recognizerIntent);
                // Send a message using content of the edit text widget
                TextView myView = (TextView) findViewById(R.id.edit_text_out);
                String intext = myView.getText().toString();
                if (!intext.equals("")) {
                    RMsgTxList.addMessageToList (RadioMSG.selectedTo, "", intext,
                            false, null, 0,
                            null);
                    ((EditText) myView).getText().clear();
                }
            }
        });
        */
    }


    /* Parked code for when offline voice recognition is reliable enough or custom dictionaries can be used
    //Speech recognition listener
    class speechlistener implements RecognitionListener {
        public void onReadyForSpeech(Bundle params)
        {
            //Log.d(TAG, "onReadyForSpeech");
        }
        public void onBeginningOfSpeech()
        {
            //Log.d(TAG, "onBeginningOfSpeech");
        }
        public void onRmsChanged(float rmsdB)
        {
            //Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            //Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech()
        {
            //Log.d(TAG, "onEndofSpeech");
        }
        public void onError(int error)
        {
            //Log.d(TAG,  "error " +  error);
            //mText.setText("error " + error);
            String message;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "Audio recording error";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "Client side error";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "Insufficient permissions";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "Network error";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "Network timeout";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "No match";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RecognitionService busy";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "error from server";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "No speech input";
                    break;
                default:
                    message = "Didn't understand, please try again.";
                    break;
            }
            RadioMSG.topToastText(message);
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            //Log.d(TAG, "onResults " + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                //Log.d(TAG, "result " + data.get(i));
                str += data.get(i) + " / " ;
            }

            if (data.size() > 0) {
                str = (String) data.get(0);//first one only
            }

            EditText mText = (EditText) findViewById(R.id.edit_text_out);
            if (mText != null) {
                mText.setText(str);
            }
        }
        public void onPartialResults(Bundle partialResults)
        {
            String str = new String();
            //Log.d(TAG, "onResults " + results);
            ArrayList data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                //Log.d(TAG, "result " + data.get(i));
                str += data.get(i);
            }

            if (data.size() > 0) {
                str = (String) data.get(0);//first one only
                EditText mText = (EditText) findViewById(R.id.edit_text_out);
                if (mText != null) {
                    mText.setText(str);
                }
            }

            //Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            //Log.d(TAG, "onEvent " + eventType);
        }

    }
*/


    //Check if a specific app is installed on this device
    private boolean isPackageInstalled(String packagename) {
        try {
            PackageManager pm = myContext.getPackageManager();
            pm.getPackageInfo(packagename, pm.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    //Custom Adapter to change the text and colour of the items in the textview
    public class quickSmsArrayAdapter<T> extends ArrayAdapter<T> {

        public quickSmsArrayAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            TextView tv = (TextView) view.findViewById(android.R.id.text1);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
            //Get separator and padding size (for "fat finger" syndrome);
            int linesPadding = config.getPreferenceI("QUICKSMSPADDING", 7);
            linesPadding = (linesPadding < 1 ? 1 : (linesPadding > 100 ? 100 : linesPadding));
            final float scale = getResources().getDisplayMetrics().density;
            linesPadding = (int) (linesPadding * scale + 0.5f);
            tv.setPadding(0, linesPadding, 0, linesPadding);
            return view;
        }
    }


    //QuickSms action when single clicking or long clicking on list item
    public void quickSmsClickAction(AdapterView<?> parent, View view, final int position, long id) {
        String msgText = (String) parent.getItemAtPosition(position);
        // Send a message using content of the edit text widget
        TextView myView = (TextView) findViewById(R.id.edit_text_out);
        String lastSmsMessage = myView.getText().toString();
        //Clear text now
        ((EditText) myView).getText().clear();
        if (lastSmsMessage.trim().length() != 0) {
            msgText += " (" + lastSmsMessage + ")";
        }
        //Anything to send?
        if (!msgText.equals("")) {
            //Send position as well?
            CheckBox myCheckbox = (CheckBox) findViewById(R.id.sendgpspos);
            if (((CheckBox) myCheckbox).isChecked()) {
                //Request a location update. The sending of the position is done inside the location listener
                long positionRequestTime = System.currentTimeMillis();
                RMsgTxList.addMessageToList(RadioMSG.selectedTo, RadioMSG.selectedVia, msgText,
                        true, null, positionRequestTime,
                        null);
                //Queue for GPS read and send
                //Now request periodic updates at one second interval
                //The cancellation of these requests is done on first receipt of a location fix
                requestQuickGpsUpdate();
                bottomToastText("Getting GPS Fix, then sending position");
            } else { //No GPS position, send immediately
                RMsgTxList.addMessageToList(RadioMSG.selectedTo, RadioMSG.selectedVia, msgText,
                        false, null, 0, null);
            }
        }
    }


    // Display the Quick SMS layout and associate it's buttons
    private void displaySmsQuickList(int screenAnimation) {

        //Show the form and associated buttons
        setContentView(R.layout.quicksmsview);
        screenAnimation((ViewGroup) findViewById(R.id.quicksmsmailscreen),
                screenAnimation);

        //Init common spinners and GPS stuff
        setupDisplay(SMSQUICKLISTVIEW);

        //set Send Position to default of true if requested
        if (config.getPreferenceB("SETSENDPOSFORQUICKSMS", true)) {
            checkbox = (CheckBox) findViewById(R.id.sendgpspos);
            checkbox.setChecked(true);
        }

        // Load Quick Messages list
        ArrayAdapter<String> listAdapter = new quickSmsArrayAdapter(myContext, R.layout.quicksmslistview);
        String myQuickSmsText = "";
        int i = 1;
        while (!myQuickSmsText.equals("<End>")) {
            String quickSmsIndex = "QUICKSMS" + i++;
            myQuickSmsText = config.getPreferenceS(quickSmsIndex, "<End>");
            if (!myQuickSmsText.equals("<End>") && !(myQuickSmsText.trim().length() == 0)) {
                listAdapter.add(myQuickSmsText);
            }
        }
        smsQuickListView = (ListView) findViewById(R.id.smslist);
        smsQuickListView.setAdapter(listAdapter);
        //Get separator and padding size (for "fat finger" syndrome);
        int linesPadding = config.getPreferenceI("QUICKSMSPADDING", 7);
        linesPadding = (linesPadding < 1 ? 1 : (linesPadding > 100 ? 100 : linesPadding));
        final float scale = getResources().getDisplayMetrics().density;
        linesPadding = (int) (linesPadding * scale + 0.5f);
        smsQuickListView.setDividerHeight(linesPadding);
        //Scroll to first item
        smsQuickListView.setSelection(0);
        // Set listener for item selection
        smsQuickListView.performHapticFeedback(MODE_APPEND);
        if (config.getPreferenceB("SHORTPRESSONQUICKSMS", false)) {
            smsQuickListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    quickSmsClickAction(parent, view, position, id);
                }
            });
        } else {
            smsQuickListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    quickSmsClickAction(parent, view, position, id);
                    return true;
                }
            });
        }
    }


    // Display the Quick SMS layout and associate it's buttons
    private void displaySmsQuickButtons(int screenAnimation) {

        //Show the form and associated buttons
        setContentView(R.layout.quicksmsbuttonview);
        screenAnimation((ViewGroup) findViewById(R.id.quicksmsmailscreen),
                screenAnimation);

        //Init common spinners and GPS stuff
        setupDisplay(SMSQUICKBUTTONVIEW);

        //set Send Position to default of true if requested
        if (config.getPreferenceB("SETSENDPOSFORQUICKSMS", true)) {
            checkbox = (CheckBox) findViewById(R.id.sendgpspos);
            checkbox.setChecked(true);
        }

        // Load Quick Messages buttons
        //Only 2 wide by 3 high for now

        ViewGroup vertLinearLayout = (ViewGroup) findViewById(R.id.ButtonsLayout);
        setTextSize(myButton);
        String myQuickSmsText = "";
        LinearLayout horizLinearLayout = null;
        int buttonCount = 1;
        int columnCount = 0;
        while (!myQuickSmsText.equals("<End>") && buttonCount < 7) { //2 x 3 for now //later 3 x 4 or 4 x 3 max buttons
            String quickSmsIndex = "QUICKSMS" + buttonCount;
            myQuickSmsText = config.getPreferenceS(quickSmsIndex, "<End>");
            if (!myQuickSmsText.equals("<End>") && !(myQuickSmsText.trim().length() == 0)) {
                if (columnCount == 0) {
                    //Add a horizontal linear layout in the vertical one
                    horizLinearLayout = new LinearLayout(this);
                    horizLinearLayout.setLayoutParams(
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT, 0.3333f));
                    //Set layout as horizontal
                    horizLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    horizLinearLayout.setWeightSum(1.0f);
                    vertLinearLayout.addView(horizLinearLayout);
                }
                if (++columnCount > 1) {
                    columnCount = 0;
                }
                //Add the button in the new horizontal linear layout
                myButton = new Button(this);
                myButton.setText(myQuickSmsText);
                myButton.setLayoutParams(
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT, 0.5f));

                myButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        //Vibrate to indicate press
                        if (Build.VERSION.SDK_INT >= 26) {
                            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(100,
                                    VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(100);
                        }
                        String msgText = ((Button) v).getText().toString();
                        //Send position as well?
                        CheckBox myCheckbox = (CheckBox) findViewById(R.id.sendgpspos);
                        if (((CheckBox) myCheckbox).isChecked()) {
                            //Request a location update. The sending of the position is done inside the location listener
                            long positionRequestTime = System.currentTimeMillis();
                            RMsgTxList.addMessageToList(RadioMSG.selectedTo, RadioMSG.selectedVia, msgText,
                                    true, null, positionRequestTime,
                                    null);
                            //Queue for GPS read and send
                            //Now request periodic updates at one second interval
                            //The cancellation of these requests is done on first receipt of a location fix
                            requestQuickGpsUpdate();
                            bottomToastText("Getting GPS Fix, then sending position");
                        } else { //No GPS position, send immediately
                            RMsgTxList.addMessageToList(RadioMSG.selectedTo, RadioMSG.selectedVia, msgText,
                                    false, null, 0, null);
                        }


                    }
                });
                horizLinearLayout.addView(myButton);
            }
            //Next quick sms entry
            buttonCount++;
        }
    }


    // Display the MMS layout and associate it's buttons
    private void displayMms(int screenAnimation) {

        //Show the form and associated buttons
        setContentView(R.layout.mmsview);
        screenAnimation((ViewGroup) findViewById(R.id.mmsmailscreen),
                screenAnimation);

        //Init common spinners and GPS stuff
        setupDisplay(MMSVIEW);

        //Pre-sets reasonable parameters depending on selected opmodes
        String currentOpMode = config.getPreferenceS("LASTMODEUSED", "HF-Poor");
        boolean isHfClubs = (currentOpMode.equals("HF-Clubs"));
        //Disable send button if no picture selected or HF-Clubs op-mode
        myButton = (Button) findViewById(R.id.button_send);
        setTextSize(myButton);
        myButton.setEnabled(false);

        //Check which mode has been manually selected
        String mImageMode = Modem.modemCapListString[RMsgProcessor.imageTxModemIndex];
        if (mImageMode.equals("MFSK16")) {
            //pictureIsColour = true;
            attachedPictureTxSPP = 8;
            updateMaxPicDimensionAndColour(160, pictureIsColour);//Low
        } else if (currentOpMode.equals("HF-Good")) {
            RMsgProcessor.imageTxModemIndex = Modem.minImageModeIndex + 2; //MFSK64 by default
            //pictureIsColour = true;
            attachedPictureTxSPP = 2;
            updateMaxPicDimensionAndColour(380, pictureIsColour);//Med-High
        } else if (currentOpMode.equals("HF-Fast")) {
            RMsgProcessor.imageTxModemIndex = Modem.maxImageModeIndex; //MFSK128 by default
            //pictureIsColour = true;
            attachedPictureTxSPP = 1;
            updateMaxPicDimensionAndColour(640, pictureIsColour);//High
        } else if (currentOpMode.equals("UHF-Poor")) {
            RMsgProcessor.imageTxModemIndex = Modem.minImageModeIndex + 1; //MFSK32 by default
            //pictureIsColour = true;
            attachedPictureTxSPP = 4;
            updateMaxPicDimensionAndColour(250, pictureIsColour);//Med-Low
        } else if (currentOpMode.equals("UHF-Good")) {
            RMsgProcessor.imageTxModemIndex = Modem.minImageModeIndex + 2; //MFSK64 by default
            //pictureIsColour = true;
            attachedPictureTxSPP = 2;
            updateMaxPicDimensionAndColour(380, pictureIsColour);//Med-High
        } else if (currentOpMode.equals("UHF-Fast")) {
            RMsgProcessor.imageTxModemIndex = Modem.maxImageModeIndex; //MFSK128 by default
            //pictureIsColour = true;
            attachedPictureTxSPP = 1;
            updateMaxPicDimensionAndColour(640, pictureIsColour);//High
        } else {
            //Assumes HF poor and/or MFSK32
            RMsgProcessor.imageTxModemIndex = Modem.minImageModeIndex + 1; //MFSK32 by default
            //pictureIsColour = true;
            attachedPictureTxSPP = 4;
            updateMaxPicDimensionAndColour(250, pictureIsColour);//Med-Low
        }

        //If we had a previous picture, re-display it
        if (tempImageBitmap != null) {
            ImageView myImage = (ImageView) findViewById(R.id.mmsimage);
            myImage.setImageBitmap(tempImageBitmap);
            if (!isHfClubs) {
                myButton = (Button) findViewById(R.id.button_send);
                setTextSize(myButton);
                myButton.setEnabled(true);
            }
        }

        //set Send Position to default of false
        checkbox = (CheckBox) findViewById(R.id.sendgpspos);
        checkbox.setChecked(false);

        //Get the current simple/expert modes for the app
        boolean expertMode = config.getPreferenceB("EXPERTMODE", false);

        //Initialise the New Picture button
        myButton = (Button) findViewById(R.id.button_newpicture);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //Minimize current memory footpring to prevent crash on new picture processing
                //tempImageBitmap = null;
                originalImageBitmap = null;
                // Force garbage collection to prevent Out Of Memory
                // errors on small RAM devices
                //System.gc();
                openImageIntent();
            }
        });

        //Initialise the Low Resolution button
        myButton = (Button) findViewById(R.id.button_lowres);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                updateMaxPicDimensionAndColour(160, pictureIsColour);
            }
        });

        //Initialise the Low Resolution button
        myButton = (Button) findViewById(R.id.button_mediumlowres);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                updateMaxPicDimensionAndColour(250, pictureIsColour);
            }
        });

        //Initialise the Medium Resolution button
        myButton = (Button) findViewById(R.id.button_mediumhighres);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                updateMaxPicDimensionAndColour(380, pictureIsColour);
            }
        });

        //Initialise the High Resolution button
        myButton = (Button) findViewById(R.id.button_highres);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                updateMaxPicDimensionAndColour(640, pictureIsColour);
            }
        });

        // Initialize the send button
        myButton = (Button) findViewById(R.id.button_send);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (tempImageBitmap != null) {
                    //See if a text message needs to be sent at the same time
                    TextView myView = (TextView) findViewById(R.id.edit_text_out);
                    String lastSmsMessage = myView.getText().toString();
                    //Clear text now
                    ((EditText) myView).getText().clear();
                    //Send position as well?
                    CheckBox myCheckbox = (CheckBox) findViewById(R.id.sendgpspos);
                    //Scrambling mode
                    int scramblingMode = 0;
                    if (((CheckBox) findViewById(R.id.scrambled)).isChecked()) {
                        scramblingMode = 1; //For now
                    }
                    if (((CheckBox) myCheckbox).isChecked()) {
                        //Clear Send Position checkbox
                        myCheckbox.setChecked(false);
                        //Request a location update. The sending of the position is done inside the location listener
                        long positionRequestTime = System.currentTimeMillis();
                        RMsgTxList.addMessageToList(selectedTo, RadioMSG.selectedVia, lastSmsMessage,
                                tempImageBitmap, attachedPictureTxSPP, pictureIsColour, RMsgProcessor.imageTxModemIndex,
                                true, null, positionRequestTime, scramblingMode);
                        //Queue for GPS read and send. Request periodic updates at one second interval
                        //The cancellation of these requests is done on first receipt of a location fix
                        requestQuickGpsUpdate();
                        bottomToastText("Getting GPS Fix, then sending the picture");
                    } else {
                        RMsgTxList.addMessageToList(selectedTo, RadioMSG.selectedVia, lastSmsMessage,
                                tempImageBitmap, attachedPictureTxSPP, pictureIsColour, RMsgProcessor.imageTxModemIndex,
                                false, null, 0, scramblingMode);
                    }
                }
            }
        });

        // JD Initialize the IMAGE MODE button
        myButton = (Button) findViewById(R.id.button_imagemode);
        setTextSize(myButton);
        if (!expertMode) {
            myButton.setEnabled(false);
        }
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    int timeToTxImages = 0;
                    //Anytime now as we queue the messages asynchronously
                    //if (ProcessorON
                    //          && !RMsgProcessor.TXActive && Modem.modemState == Modem.RXMODEMRUNNING) {
                    if (++RMsgProcessor.imageTxModemIndex > Modem.maxImageModeIndex)
                        RMsgProcessor.imageTxModemIndex = Modem.minImageModeIndex;
                    updateImageModeTime();
                    //  }
                } catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

        // JD Initialize the COLOUR/B&W button
        myButton = (Button) findViewById(R.id.button_colour);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (pictureIsColour) {
                        pictureIsColour = false;
                    } else {
                        pictureIsColour = true;
                    }
                    updateMaxPicDimensionAndColour(-1, pictureIsColour);
                    updateImageModeTime();
                } catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });


        // JD Initialize the SPEED button
        myButton = (Button) findViewById(R.id.button_speed);
        setTextSize(myButton);
        if (!expertMode) {
            myButton.setEnabled(false);
        }
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {

                    //Test value of slower SPP in narrow channels (MFSK16/32)
                    //if (attachedPictureTxSPP >= 32) {
                    //    attachedPictureTxSPP = 16;
                    //} else if (attachedPictureTxSPP >= 16) {
                    //    attachedPictureTxSPP = 8;
                    if (attachedPictureTxSPP >= 8) {
                        //} else if (attachedPictureTxSPP >= 8) {
                        attachedPictureTxSPP = 4;
                    } else if (attachedPictureTxSPP == 4) {
                        attachedPictureTxSPP = 2;
                    } else if (attachedPictureTxSPP == 2) {
                        attachedPictureTxSPP = 1;
                    } else { //Must be "1"
                        //Test value of slower SPP in narrow channels (MFSK16/32)
                        attachedPictureTxSPP = 8;
                        //attachedPictureTxSPP = 32;
                    }
                    updateImageModeTime();
                } catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

        updateImageModeTime();

    }


    private void updateMaxPicDimensionAndColour(int maxDimension, boolean isColour) {
        int maxDim = maxDimension;
        if (maxDimension > 0) {
            SharedPreferences.Editor editor = RadioMSG.mysp.edit();
            editor.putString("LASTPICTURESIZE", "" + maxDimension);
            // Commit the edits!
            editor.commit();
        } else {
            //Recover previous values
            maxDim = config.getPreferenceI("LASTPICTURESIZE", 640);
        }
        if (originalImageBitmap != null) {
            float finalScale = (float) getPictureScale(originalImageBitmap.getWidth(), originalImageBitmap.getHeight(), maxDim);
            //Re-display the image bitmap at the new scale
            //tempImageBitmap = Bitmap.createScaledBitmap(originalImageBitmap, targetWidth, targetHeight, true);

            //BitmapFactory.Options options=new BitmapFactory.Options();
            //InputStream is = getContentResolver().openInputStream(currImageURI);
            //bm = BitmapFactory.decodeStream(is,null,options);
            //int Height = bm.getHeight();
            //int Width = bm.getWidth();
            //int newHeight = 300;
            //int newWidth = 300;
            //float scaleWidth = ((float) newWidth) / Width;
            //float scaleHeight = ((float) newHeight) / Height;
            Matrix matrix = new Matrix();
            matrix.postScale((float) 1.0 / finalScale, (float) 1.0 / finalScale);
            tempImageBitmap = Bitmap.createBitmap(originalImageBitmap, 0, 0, originalImageBitmap.getWidth(), originalImageBitmap.getHeight(), matrix, true);
            //BitmapDrawable bmd = new BitmapDrawable(resizedBitmap);

            //Coour to B&W?
            if (!isColour) {
                //Bitmap grayscaleBitmap = Bitmap.createBitmap(
                //        tempImageBitmap.getWidth(), tempImageBitmap.getHeight(),
                //        Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(tempImageBitmap);
                Paint p = new Paint();
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
                p.setColorFilter(filter);
                c.drawBitmap(tempImageBitmap, 0, 0, p);
            }
            //Update the fields in the UI thread
            ImageView myImage = (ImageView) findViewById(R.id.mmsimage);
            myImage.setImageBitmap(tempImageBitmap);
            //Update the size and time needed to Tx
            updateImageModeTime();
            //Enable/disable the send button
            //Disable send button if no picture selected or HF-Clubs op-mode
            boolean isHfClubs = (config.getPreferenceS("LASTMODEUSED", "HF-Poor")).equals("HF-Clubs");
            myButton = (Button) findViewById(R.id.button_send);
            setTextSize(myButton);
            myButton.setEnabled(false);
            if (tempImageBitmap != null && !isHfClubs) {
                myButton.setEnabled(true);
            }
        }
    }


    //Updates the image mode and time required to send the image in the mmsview.xml.xml layout
    public void updateImageModeTime() {
        //Initialise display of mode and time required
        TextView myImageModeView = (TextView) findViewById(R.id.imagemodetext);
        //Calculate the time required to Tx the image
        double timeToTxImages = 0;
        if (tempImageBitmap != null) {
            timeToTxImages = tempImageBitmap.getWidth() *
                    tempImageBitmap.getHeight() *
                    attachedPictureTxSPP * 0.000125 *
                    (pictureIsColour ? 3 : 1) + 3 + 1;
        }
        imageMode = Modem.modemCapListString[RMsgProcessor.imageTxModemIndex];
        //Update display
        analogSpeedColour = (pictureIsColour ? "Color" : "Grey") + ", X" +
                Integer.toString(Modem.SPPtoSpeed[attachedPictureTxSPP]);
        myImageModeView.setText(imageMode);
        TextView totalEstimateView = (TextView) findViewById(R.id.totalestimatetext);
        //format in minutes, seconds
        //totalEstimateView.setText(analogSpeedColour + ", " + Integer.toString((int) timeToTxImages) + " secs");
        totalEstimateView.setText(analogSpeedColour + ", " + RMsgMisc.secToTime((int) timeToTxImages));
    }


    //Save last mode used for next app start
    public static void saveLastModeUsed(String currentMode) {
        SharedPreferences.Editor editor = RadioMSG.mysp.edit();
        editor.putString("LASTMODEUSED", currentMode);
        // Commit the edits!
        editor.commit();
    }


    private void setVolume(int sliderValue) {
        AudioManager audioManager = (AudioManager) RadioMSG.myContext.getSystemService(Context.AUDIO_SERVICE);
        try {
            int maxVolume;
            int stream = RadioMSG.toBluetooth ? STREAM_BLUETOOTH_SCO : AudioManager.STREAM_MUSIC;
            maxVolume = audioManager.getStreamMaxVolume(stream);
            maxVolume = maxVolume * sliderValue / 100;
            audioManager.setStreamVolume(stream, maxVolume, 0);
            //Save the new value in preferences
            SharedPreferences.Editor editor = RadioMSG.mysp.edit();
            editor.putString("MEDIAVOLUME", Integer.toString(sliderValue));
            // Commit the edits!
            editor.commit();

        } catch (Exception e) {
            RadioMSG.middleToastText("Error Adjusting Volume");
        }

    }


    // Display the Modem layout and associate it's buttons
    private void displayModem(int screenAnimation, boolean withWaterfall) {

        //Ensure we reset the swipe action
        RadioMSG.inFormDisplay = false;

        if (withWaterfall) {
            currentview = MODEMVIEWwithWF;
            setContentView(R.layout.modemwithwf);
            screenAnimation((ViewGroup) findViewById(R.id.modemwwfscreen),
                    screenAnimation);
            // Get the waterfall view object for the runnable
            myWFView = (waterfallView) findViewById(R.id.WFbox);
        } else {
            currentview = MODEMVIEWnoWF;
            setContentView(R.layout.modemwithoutwf);
            screenAnimation((ViewGroup) findViewById(R.id.modemnwfscreen),
                    screenAnimation);
            myWFView = null;
        }

        //Re-draw menu
        this.invalidateOptionsMenu();

        myModemTV = (TextView) findViewById(R.id.modemview);
        myModemTV.setHorizontallyScrolling(false);
        myModemTV.setTextSize(16);

        //Allow select/copy
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            //test copy/paste myModemTV.setTextIsSelectable(true);
            myModemTV.setTextIsSelectable(false);
        }
        //Get waterfall gain
        try {
            waterfallDynamic = Double.parseDouble(RadioMSG.mysp.getString("WATERFALLGAIN", "0.1"));
        } catch (NumberFormatException e) {
            middleToastText("Wrong value in Waterfall gain, using default");
            waterfallDynamic = 0.1;
        }

        // initialise Mode display
        modemModeView = (TextView) findViewById(R.id.mode);
        RadioMSG.mHandler.post(RadioMSG.updatemodemmode);

        // initialise CPU load bar display
        CpuLoad = (ProgressBar) findViewById(R.id.cpu_load);

        // initialise squelch and signal quality dislay
        SignalQuality = (ProgressBar) findViewById(R.id.signal_quality);

        //Reset modem display in case it was blanked out by a new oncreate call
        //myModemTV.setText(ModemBuffer);
        myModemSC = (ScrollView) findViewById(R.id.modemscrollview);
        myModemTV.setText(ModemBuffer, TextView.BufferType.SPANNABLE);
        //Make sure we go to the bottom to enable auto-scroll
        myModemSC.fullScroll(View.FOCUS_DOWN);
        //Set flag to force scrolldown on modem updates
        mustScrollDown = true;
        // update with whatever we have already accumulated then scroll
        //RadioMSG.mHandler.post(RadioMSG.setModemScreen);
        RadioMSG.mHandler.post(RadioMSG.updateModemScreen);

        // Advise user of which screen we are in
        //middleToastText("Modem Screen");

        if (withWaterfall) { // initialise two extra buttons

            // JD Initialize the Waterfall Sensitivity UP button
            myButton = (Button) findViewById(R.id.button_wfsensup);
            setTextSize(myButton);
            myButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    try {
                        if (myWFView != null) {
                            myWFView.maxvalue /= 1.25;
                            //VK2ETA Test higher sensitivity for Thor Micro modes
                            if (myWFView.maxvalue < 0.00001)
                                myWFView.maxvalue = 0.00001;
                            // store value into preferences
                            SharedPreferences.Editor editor = RadioMSG.mysp
                                    .edit();
                            editor.putFloat("WFMAXVALUE",
                                    (float) myWFView.maxvalue);
                            // Commit the edits!
                            editor.commit();
                        }
                    }
                    // JD fix this catch action
                    catch (Exception ex) {
                        loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                    }
                }
            });

            // JD Initialize the Waterfall Sensitivity DOWN button
            myButton = (Button) findViewById(R.id.button_wfsensdown);
            setTextSize(myButton);
            myButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    try {
                        if (myWFView != null) {
                            myWFView.maxvalue *= 1.25;
                            if (myWFView.maxvalue > 400.0)
                                myWFView.maxvalue = 400.0;
                            // store value into preferences
                            SharedPreferences.Editor editor = RadioMSG.mysp
                                    .edit();
                            editor.putFloat("WFMAXVALUE",
                                    (float) myWFView.maxvalue);
                            // Commit the edits!
                            editor.commit();
                        }
                    }
                    // JD fix this catch action
                    catch (Exception ex) {
                        loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                    }
                }
            });

        }

        // Initialize the RxRSID check box
        checkbox = (CheckBox) findViewById(R.id.rxrsid);
        Modem.rxRsidOn = config.getPreferenceB("RXRSID", true);
        checkbox.setChecked(Modem.rxRsidOn);
        checkbox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    // Store preference
                    config.setPreferenceB("RXRSID", true);
                    Modem.rxRsidOn = true;
                } else {
                    // Store preference
                    config.setPreferenceB("RXRSID", false);
                    Modem.rxRsidOn = false;
                }
            }
        });

        // Initialize the TxRSID check box
        checkbox = (CheckBox) findViewById(R.id.txrsid);
        Modem.txRsidOn = config.getPreferenceB("TXRSID", true);
        checkbox.setChecked(Modem.txRsidOn);
        checkbox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    // Store preference
                    config.setPreferenceB("TXRSID", true);
                    Modem.txRsidOn = true;
                } else {
                    // Store preference
                    config.setPreferenceB("TXRSID", false);
                    Modem.txRsidOn = false;
                }
            }
        });

        //Initialize the volume slider bar
        SeekBar volControl = (SeekBar)findViewById(R.id.volumeSlider);
        int mediaVolume = config.getPreferenceI("MEDIAVOLUME", 100);
        volControl.setMax(100);
        volControl.setProgress(mediaVolume);
        volControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                setVolume(arg1);
            }
        });

        // JD Initialize the MODEM RX ON/OFF button
        myButton = (Button) findViewById(R.id.button_modemONOFF);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (ProcessorON) {
                        if (Modem.modemState == Modem.RXMODEMRUNNING
                                //&& !Modem.receivingMsg
                                && !RMsgProcessor.TXActive) {
                            modemPaused = true;
                            Modem.stopRxModem();
                            stopService(new Intent(RadioMSG.this,
                                    RMsgProcessor.class));
                            // Force garbage collection to prevent Out Of Memory
                            // errors on small RAM devices
                            //System.gc();
                            ProcessorON = false;
                            // Set modem text as selectable
                            TextView myTempModemTV = (TextView) findViewById(R.id.modemview);
                            if (myTempModemTV != null) {
                                if (android.os.Build.VERSION.SDK_INT >= 11) {
                                    myTempModemTV.setTextIsSelectable(true);
                                }
                            }
                        } else {
                            bottomToastText("Transmitting, Modem cannot be stopped now!");
                        }
                    } else {
                        if (Modem.modemState == Modem.RXMODEMIDLE) {
                            modemPaused = false;
                            // Force garbage collection to prevent Out Of Memory
                            // errors on small RAM devices
                            //System.gc();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(new Intent(RadioMSG.this, RMsgProcessor.class));
                            } else {
                                startService(new Intent(RadioMSG.this, RMsgProcessor.class));
                            }
                            ProcessorON = true;
                            // Set modem text as NOT selectable (Only for Android 3.0 and UP)
                            TextView myTempModemTV = (TextView) findViewById(R.id.modemview);
                            if (myTempModemTV != null) {
                                if (android.os.Build.VERSION.SDK_INT >= 11) {
                                    myTempModemTV.setTextIsSelectable(false);
                                }
                            }
                            //Force a scroll to the bottom of the screen on next update
                            mustScrollDown = true;
                        }
                    }
                    RadioMSG.mHandler.post(RadioMSG.updatetitle);
                } catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

        // JD Initialize the MODE UP button
        myButton = (Button) findViewById(R.id.button_modeUP);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (!Modem.receivingMsg && ProcessorON
                            && !RMsgProcessor.TXActive
                            && Modem.modemState == Modem.RXMODEMRUNNING) {
                        RMsgProcessor.TxModem = RMsgProcessor.RxModem = Modem
                                .getModeUpDown(RMsgProcessor.RxModem, +1);
                        Modem.changemode(RMsgProcessor.RxModem); // to make the changes effective
                        RadioMSG.mHandler.post(RadioMSG.updatemodemmode);
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

        // JD Initialize the MODE DOWN button
        myButton = (Button) findViewById(R.id.button_modeDOWN);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (!Modem.receivingMsg && ProcessorON
                            && !RMsgProcessor.TXActive
                            && Modem.modemState == Modem.RXMODEMRUNNING) {
                        RMsgProcessor.TxModem = RMsgProcessor.RxModem = Modem
                                .getModeUpDown(RMsgProcessor.RxModem, -1);
                        Modem.changemode(RMsgProcessor.RxModem); // to make the changes effective
                        RadioMSG.mHandler.post(RadioMSG.updatemodemmode);
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

        // JD Initialize the Squelch UP button
        myButton = (Button) findViewById(R.id.button_squelchUP);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    Modem.AddtoSquelch(5.0);
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

        // JD Initialize the Squelch DOWN button
        myButton = (Button) findViewById(R.id.button_squelchDOWN);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    Modem.AddtoSquelch(-5.0);
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

        // JD Initialize the TUNE button
        myButton = (Button) findViewById(R.id.button_tune);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    int tuneLength = config.getPreferenceI("TUNEDURATION", 0);

                    if (tuneLength <= 0) {
                        Modem.tune = !Modem.tune;
                        if (Modem.tune) {
                            if (!RMsgProcessor.TXActive && !Modem.receivingMsg
                                    && Modem.modemState == Modem.RXMODEMRUNNING) {
                                Modem.sendToneSequence(0, "", false, false);
                            }
                        }
                    } else {
                        if (!RMsgProcessor.TXActive && !Modem.receivingMsg
                                && Modem.modemState == Modem.RXMODEMRUNNING) {
                            Modem.sendToneSequence(0, "", false, false);
                        }
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

        // JD Initialize the WATERFALL ON/OFF button
        myButton = (Button) findViewById(R.id.button_waterfallONOFF);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (currentview == MODEMVIEWnoWF) {
                        displayModem(NORMAL, true);
                    } else {
                        displayModem(NORMAL, false);
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

        // JD Initialize the STOP TX button
        myButton = (Button) findViewById(R.id.button_stopTX);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (RMsgProcessor.TXActive) {
                        Modem.stopTX = true;
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog("Button Execution error: " + ex.getMessage(), null, true);
                }
            }
        });

    }


    // Display the About screen
    private void displayAbout() {
        currentview = ABOUTVIEW;

        setContentView(R.layout.about);
        TextView myversion = (TextView) findViewById(R.id.versiontextview);
        myversion.setText("          " + RMsgProcessor.version);

        // JD Initialize the I Agree button
        myButton = (Button) findViewById(R.id.button_returntomainscreen);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //Save agreement
                SharedPreferences.Editor editor = RadioMSG.mysp.edit();
                editor.putString("HASAGREED", "YES");
                editor.commit();
                //Go strait to message screen
                displaySms(BOTTOM);
            }
        });

        // JD Initialize the I Disagree button
        myButton = (Button) findViewById(R.id.button_idisagree);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //Save agreement
                SharedPreferences.Editor editor = RadioMSG.mysp.edit();
                editor.putString("HASAGREED", "NO");
                editor.commit();
                //Get out
                exitApplication();
            }
        });

        //
    }


    //Display Message in Alert window
    public void alertPopup(RMsgObject mMessage) {
        // LayoutInflater for popup windows
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        pwLayout = inflater.inflate(R.layout.alertmessagepopup,
                (ViewGroup) findViewById(R.id.alertpopupscreen));
        // create a large PopupWindow (it will be clipped automatically if larger than main window)
        final PopupWindow pw = new PopupWindow(pwLayout, LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT, true);
        //pw.setBackgroundDrawable(new BitmapDrawable()); // to
        // allow click event to be active, display the popup in the center
        pw.showAtLocation(pwLayout, Gravity.TOP, 0, 0);
        //Display info
        TextView myText = (TextView) pwLayout.findViewById(R.id.fromtext);
        String addrString = "From: " + mMessage.from + "\nTo: " +
                (mMessage.to.equals("*") ? "All" : mMessage.to);
        if (!mMessage.via.equals("")) {
            addrString += "\nVia: " + mMessage.via;
        }
        if (!mMessage.relay.equals("")) {
            addrString += "\nRelay by: " + mMessage.relay;
        }
        addrString += " @ " + mMessage.fileName.replace(".txt", "");
        myText.setText(addrString);
        myText = (TextView) pwLayout.findViewById(R.id.smstext);
        if (!mMessage.sms.equals("")) {
            myText.setText("Msg: " + RMsgMisc.unescape(mMessage.sms));
        } else {
            myText.setVisibility(View.GONE);
        }
        myText = (TextView) pwLayout.findViewById(R.id.positiontext);
        if (mMessage.msgHasPosition && mMessage.position != null) {
            //myText.setText("Pos: " + mMessage.getLatLongString());
            String formattedString = "Pos: " + mMessage.getLatLongString() + "\n" +
                    ((int) (mMessage.position.getSpeed() * 3.6));
            if (mMessage.positionAge > 0) {
                formattedString += " Km/h,  " +
                        mMessage.positionAge + " second(s) late";
            } else {
                formattedString += " Km/h";
            }
            myText.setText(formattedString);
        } else {
            myText.setVisibility(View.GONE);
        }
        myText = (TextView) pwLayout.findViewById(R.id.picturetext);
        if (!mMessage.pictureString.equals("")) {
            myText.setText("Pic: " + mMessage.pictureString);
        } else {
            myText.setVisibility(View.GONE);
        }
        //Do we have a picture
        if (mMessage.picture != null) {
            ImageView picView = (ImageView) pwLayout.findViewById(R.id.mmsimage);
            picView.setImageBitmap(mMessage.picture);
        }

        // Return button init
        myButton = (Button) pwLayout.findViewById(R.id.button_dismiss);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                pw.dismiss();
            }
        });

    }


    //Display Picture popup window
    public void rxPicturePopup(int picW, int picH) {
        // LayoutInflater for popup windows
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        pwLayout = inflater.inflate(R.layout.rxpicturepopup,
                (ViewGroup) findViewById(R.id.html_popup));
        // create a large PopupWindow (it will be clipped automatically if larger than main window)
        final PopupWindow pw = new PopupWindow(pwLayout, LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT, true);
        // allow click event to be active, display the popup in the center
        pw.showAtLocation(pwLayout, Gravity.TOP, 0, 0);
        ImageView imageView = (ImageView) RadioMSG.pwLayout.findViewById(R.id.imageView1);
        //TextView emailText = (TextView) layout.findViewById(R.id.emailtext);
        //mWebView = (WebView) layout.findViewById(R.id.imageView1);
        // Return button init
        myButton = (Button) pwLayout.findViewById(R.id.button_close);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //Free buffer when completed Rx
                //Modem.picBuffer = null; //let GC recover the memory
                //Reset values as we don't need them anymore (no de-slanting possible)
                //RMsgProcessor.lastTextMessage = null;
                pw.dismiss();
            }
        });
        // Slant Left button init
        myButton = (Button) pwLayout.findViewById(R.id.button_slant_left);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Modem.deSlant(+1);
            }
        });
        // Slant Right button init
        myButton = (Button) pwLayout.findViewById(R.id.button_slant_right);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Modem.deSlant(-1);
            }
        });
        // Save Again button init
        myButton = (Button) pwLayout.findViewById(R.id.button_descramble);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Modem.unscramblePicture();
            }
        });
        // Save Again button init
        myButton = (Button) pwLayout.findViewById(R.id.button_save_again);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Modem.savePictureAgain();
            }
        });
        // Shift Left button init
        myButton = (Button) pwLayout.findViewById(R.id.button_shift_left);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Modem.shiftPicture(+1);
            }
        });
        // Shift Right button init
        myButton = (Button) pwLayout.findViewById(R.id.button_shift_right);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Modem.shiftPicture(-1);
            }
        });
        // Revert button init
        myButton = (Button) pwLayout.findViewById(R.id.button_revert);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Modem.revertPictureToOriginal();
            }
        });

        //imageView.scrollBy(x, y);
    }


    //Display Picture popup window
    public void txPicturePopup(int picW, int picH) {
        // LayoutInflater for popup windows
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        pwLayout = inflater.inflate(R.layout.mmsview,
                (ViewGroup) findViewById(R.id.html_popup));
        final PopupWindow pw = new PopupWindow(pwLayout, LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT, true);
        // allow click event to be active, display the popup in the center
        pw.showAtLocation(pwLayout, Gravity.TOP, 0, 0);
        ImageView imageView = (ImageView) RadioMSG.pwLayout.findViewById(R.id.imageView1);
        //TextView emailText = (TextView) layout.findViewById(R.id.emailtext);
        //mWebView = (WebView) layout.findViewById(R.id.imageView1);
        // Return button init
        myButton = (Button) pwLayout.findViewById(R.id.button_close);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                pw.dismiss();
            }
        });
        // Return button init
        myButton = (Button) pwLayout.findViewById(R.id.button_close);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                pw.dismiss();
            }
        });
        // Return button init
        myButton = (Button) pwLayout.findViewById(R.id.button_close);
        setTextSize(myButton);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                pw.dismiss();
            }
        });

    }

    //Does a clean restart of the app/process
    public void doRestart(Context c) {
        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    /*
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 5000, mPendingIntent);
                        //kill the application
                        //System.exit(0);
                        if (ProcessorON) {
                            stopService(new Intent(RadioMSG.this,
                                    RMsgProcessor.class));
                            ProcessorON = false;
                        }
                        // Stop the GPS if running
                        if (locationManager != null) {
                            locationManager.removeUpdates(locationListener);
                        }
                        //Reset forced diversions of audio input / outputs
                        Modem.resetSoundToNormal();
                        //Unregisters receivers
                        if (bluetoothReceiver != null)
                            RadioMSG.myInstance.unregisterReceiver(bluetoothReceiver);
                        // Stop listening for phone state
                        if (mTelephonyManager != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (mCallStateCallback != null)
                                    mTelephonyManager.unregisterTelephonyCallback((TelephonyCallback) mCallStateCallback);
                            } else {
                                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                            }
                        }
                        // Stop listening for received SMSes
                        if (listeningForSMSs && smsReceiver != null)
                            RadioMSG.myInstance.unregisterReceiver(smsReceiver);
                        // Close that activity and return to previous screen
                        finish();
                        // Kill the process
                        android.os.Process.killProcess(android.os.Process.myPid());
                        //System.exit(0);
                    } else {
                        //Log.e(TAG, "Was not able to restart application, mStartActivity null");
                    }
                    */
                    if (ProcessorON) {
                        stopService(new Intent(RadioMSG.this,
                                RMsgProcessor.class));
                        ProcessorON = false;
                    }
                    // Stop the GPS if running
                    if (locationManager != null) {
                        locationManager.removeUpdates(locationListener);
                    }
                    //Reset forced diversions of audio input / outputs
                    Modem.resetSoundToNormal();
                    //Unregisters receivers
                    if (bluetoothReceiver != null)
                        RadioMSG.myInstance.unregisterReceiver(bluetoothReceiver);
                    // Stop listening for phone state
                    if (mTelephonyManager != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (mCallStateCallback != null)
                                mTelephonyManager.unregisterTelephonyCallback((TelephonyCallback) mCallStateCallback);
                        } else {
                            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                        }
                    }
                    // Stop listening for received SMSes
                    if (listeningForSMSs && smsReceiver != null)
                        RadioMSG.myInstance.unregisterReceiver(smsReceiver);
                    Intent intent = pm.getLaunchIntentForPackage(c.getPackageName());
                    ComponentName componentName = intent.getComponent();
                    Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                    c.startActivity(mainIntent);
                    Runtime.getRuntime().exit(0);
                } else {
                    //Log.e(TAG, "Was not able to restart application, PM null");
                }
            } else {
                //Log.e(TAG, "Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            //Log.e(TAG, "Was not able to restart application");
        }
    }

} // end of RadioMSG class
