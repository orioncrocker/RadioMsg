/*
 * Modem.java
 *
 * Copyright (C) 2011 John Douyere (VK2ETA) - for Android platforms
 * Partially based on Modem.java from Pskmail by Per Crusefalk and Rein Couperus
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

import android.annotation.TargetApi;
import android.content.*;
import android.graphics.Bitmap;
import android.media.*;
import android.media.MediaRecorder.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Build;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.sin;

import android.app.PendingIntent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;


public class Modem {

    //Assumes later Android version
    private static boolean preAndroid6 = false;
    private static boolean sampleAudioAsFloats = false;
    private static int myAudioFormat = AudioFormat.ENCODING_PCM_FLOAT;


    //frame start and end
    public static final char SOH = 1;
    public static final char EOT = 4;
    public static final char DC1 = 17;
    public static final char DC3 = 19;

    public static final int RXMODEMIDLE = 0;
    public static final int RXMODEMSTARTING = 1;
    public static final int RXMODEMRUNNING = 2;
    public static final int RXMODEMPAUSED = 3;
    public static final int RXMODEMSTOPPING = 4;

    public static final int RSIDMODEM = 1;
    public static final int CCIR476MODEM = 2;
    public static final int CMODEMS = 3;

    public static int modemState = RXMODEMIDLE;

    volatile public static boolean modemThreadOn = true;
    private static int mySpeakerId = 3; //default
    private static int speakerVolumeShift = -99; //Default OFF
    public static boolean speakerOn = false;

    public static int NumberOfOverruns = 0;
    private static AudioRecord rxAudioRecorder = null;
    volatile private static boolean RxON = false;
    private final static float sampleRate = 8000.0f;
    private static int rxBufferSize = 0;
    private static int txBufferSize = 0;
    public static boolean tune = false;

    public static boolean voicePassThrough = false;
    //public static boolean forcedSpeakerStreamOn = false;
    public static boolean microphoneToRadio = false;
    //public static boolean camcorderStreamOn = false;

    private static int DEVICE_IN_WIRED_HEADSET = 0x400000;
    private static int DEVICE_OUT_SPEAKER = 0x2;
    private static int DEVICE_OUT_EARPIECE = 0x1;
    private static int DEVICE_OUT_WIRED_HEADSET = 0x4;
    private static int DEVICE_STATE_UNAVAILABLE = 0;
    private static int DEVICE_STATE_AVAILABLE = 1;


    //Must match the rsid.h declaration: #define RSID_FFT_SIZE		1024
    //test 8000Hz SR
    //public static final int RSID_FFT_SIZE = 1024;
    public static final int RSID_FFT_SIZE = 744;
    //Must match the rsid.h declaration: #define RSID_SAMPLE_RATE 11025.0
    public final static double RSID_SAMPLE_RATE = 8000.0f;

    public static boolean receivedSOH = false;
    //public static String MonitorString = "";
    //private static String BlockBuffer = "";
    private static StringBuilder BlockBuffer = new StringBuilder(5000);
    public static long lastCharacterTime = 0;
    private static Pattern invalidCharsInHeaderPattern = Pattern.compile("[^" + Character.toString(SOH) + ".\\-=\\/\\*\\+@:a-zA-Z_0-9]");
    private static Pattern validHeaderPattern = Pattern.compile("^" + Character.toString(SOH) + "([.\\+\\-=\\/\\w]{1,20}|[\\w.-]{1,30}@[\\w\\-]{1,20}\\.[\\w.-]{1,15}):([\\w.\\-=]{1,30}@[\\w\\-]{1,20}\\.[\\w.-]{1,15}|[\\+?.\\-\\=\\/\\*\\w]{1,30})$");
    //Last time we received a valid RSID
    public static long startListeningTime = 0;
    //Decoding by C modems (conditional to save battery)
    private static boolean processModemData = true;
    public static boolean receivingMsg = false;
    private static boolean receivingPic = false;
    public static long lastRsidTime = 0L; //Time of last RSID received from Modem (for time sync)
    private static boolean priorityCModems = false;
    public static boolean priorityCCIR476 = false;
    public static boolean awaitingSoundAlarm = false; //Flag to let the transmitting monitor wait for end of "message received" sound alarm

    public static double frequency = 1500.0;
    public static double rsidFrequency = 0;
    static double squelch = 0.0;
    static double metric = 0.0;
    static double audioLevel = 0.0;
    static double blockSNR = 0.0;

    //Waterfall interaction
    public static double[] WaterfallAmpl = new double[RSID_FFT_SIZE];
    //Semaphore for waterfall (new array of amplitudes needed for display)
    //Is now also accessed from the c++ native side in rsid.cxx
    public static boolean newAmplReady = false;

    //Semaphore for Audio Level Bar in SMS screen (new value of Audio Level needed for display)
    //Is now also accessed from the c++ native side in rsid.cxx
    public static boolean newAudioLevelReady = false;

    public static boolean stopTX = false;

    //The following value MUST Match the MAXMODES in modem.h
    public static final int MAXMODES = 300;
    //List of modems and modes returned from the C++ modems
    public static int[] modemCapListInt = new int[MAXMODES];
    public static String[] modemCapListString = new String[MAXMODES];
    public static int numModes = 0;
    //Custom list of modes as selected in the preferences (can include all modes above if
    //  "Custom List" is not selected, or alternatively all modes manually selected in preferences)
    public static int[] customModeListInt = new int[MAXMODES];
    public static String[] customModeListString = new String[MAXMODES];
    public static int customNumModes = 0;
    private static WaveWriter audioWriter = null;
    private static boolean audioRecordingOn = false;
    private static long audioRecordingLenth = 0;

    public static int minImageModeIndex;
    public static int maxImageModeIndex;
    public static int slantValue = 0;
    public static int shiftValue = 0;


    //Tx variables
    public static AudioTrack txAudioTrack = null;
    public static boolean modemIsTuning = false;
    private static double pttPhaseIncr;
    private static double pttPhase = 0.0f;

    //RSID Flags
    public static boolean rxRsidOn;
    public static boolean txRsidOn;

    //JD debug
    private static boolean CModemInitialized = false;

    //Mfsk picture
    public static int picBuffer[];
    public static int picBufferSaved[];
    private static int pixelNumber;
    public static Bitmap picBitmap = null;
    private static int mfskPicHeight = 0;
    private static int mfskPicWidth = 0;

    static private long nextTimeToTx = 0L;

    //Picture transfer conversion of speed to SPP (Samples Per Pixel) and vice-versa
    //map from speed to SPP with defaults to 2 SPP in case of errors
    public static final int[] speedtoSPP = {2, 8, 4, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 2, 16, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 32};

    //int[] SPPtoSpeed = {0,8,4,0,2,0,0,0,1}; //map from SPP to Xy speed display
    public static final int[] SPPtoSpeed = {0, 8, 4, 0, 2, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32}; //map from SPP to Xy speed display


    //Declaration of native classes
    private native static String createCModem(int modemCode);

    private native static void changeCModem(int modemCode, double frequency);

    private native static String initCModem(double frequency);

    //private native static String rxCProcess(short[] buffer, int length);
    //private native static String rxCProcess(float[] buffer, int length);
    private native static byte[] rxCProcess(float[] buffer, int length);

    private native static int setSquelchLevel(double squelchLevel);

    private native static double getMetric();

    public native static double getAudioLevel();

    private native static double getCurrentFrequency();

    private native static int getCurrentMode();

    //8000khz sampling
    //private native static String RsidCModemReceive(short[] myfbuffer, int length, boolean doSearch);
    private native static String RsidCModemReceive( float[] myfbuffer, int length, boolean doSearch);

    private native static String createRsidModem();

    private native static int[] getModemCapListInt();

    private native static String[] getModemCapListString();

    private native static String txInit(double frequency);

    private native static boolean txCProcess(byte[] buffer, int length);

    private native static void saveEnv();

    private native static void txRSID();

    private native static void txThisRSID(int rsidCode);

    public native static int getTxProgressPercent();

    public native static void setTxProgressPercent(int percent);

    private native static void resetTxProgressPercent();

    private native static void setSlowCpuFlag(boolean slowcpu);

    private native static void txPicture(byte[] txPictureBuffer, int txPictureWidth,
                                         int txPictureHeight, int txPictureTxSpeed,
                                         int txPictureColour);

    private native static boolean saveSpeakerDeviceId(int deviceId);

    private native static boolean openSpeaker();

    private native static int queryAudioApi();

    private native static boolean writeToSpeaker(short[] buffer, int length, int volumeShift);

    private native static boolean closeSpeaker();

    static {
        //Load the C++ modems library
        System.loadLibrary("c++_shared");
        System.loadLibrary("RadioMSG_Modem_Interface");
        //System.loadLibrary("oboe19");
    }

    //Called from the C++ side to modulate the audio output
    public static void txModulate(double[] outDBuffer, int length) {
        short[] outSBuffer = new short[length];
        int volumebits = config.getPreferenceI("VOLUME", 8);
        //Change format and re-scale for Android
        //To be moved to c++ code for speed
        for (int i = 0; i < length; i++) {
            //outbuf[j] = (short) ((int) (Math.sin(phase) * 8386560) >> volumebits);
            //outSBuffer[i]  = (short) ((int)(outDBuffer[i] * 16383.0f) >> volumebits);
            outSBuffer[i] = (short) ((int) (outDBuffer[i] * 8386560.0f) >> volumebits);
        }
        //Catch the stopTX flag at this point as well
        //if (!stopTX) txAudioTrack.write(outSBuffer, 0, length);
        if (!stopTX) writeToAudiotrack(outSBuffer, 0, length);
    }


    //Called from the C++ side to echo the transmitted characters
    //public static void putEchoChar(int txedChar) {
        //RadioMSG.ModemBuffer.append((char) txedChar);
    //    appendToModemBuffer((char) txedChar);
    //    RadioMSG.mHandler.post(RadioMSG.updateModemScreen);
    //}


    //Called from the C++ native side in rsid.cxx
    public static void updateWaterfall(double[] aFFTAmpl) {
        System.arraycopy(aFFTAmpl, 0, WaterfallAmpl, 0, RSID_FFT_SIZE);
        newAmplReady = true;
        RadioMSG.mHandler.post(RadioMSG.updatewaterfall);
    }


    //Called from the C++ native side in rsid.cxx
    public static void updateWaterfall(float[] aFFTAmpl) {
        //System.arraycopy(aFFTAmpl, 0, WaterfallAmpl, 0, RSID_FFT_SIZE);
        for (int i = 0; i < RSID_FFT_SIZE; i++) {
            WaterfallAmpl[i] = (double) aFFTAmpl[i];
        }
        newAmplReady = true;
        RadioMSG.mHandler.post(RadioMSG.updatewaterfall);
    }


    //Called from the C++ native side to initialize a new picture and associated viewer.
    public static void showRxViewer(final int mpicW, final int mpicH) {

        //We are receiving a picture
        receivingPic = true;
        //Less than 20 seconds between the end of the text message and
        //	the start of mfsk picture transmission
        RMsgProcessor.pictureRxInTime = (System.currentTimeMillis() -
                RMsgProcessor.lastMessageEndRxTime < 20000);

        //Save size for later
        mfskPicWidth = mpicW;
        mfskPicHeight = mpicH;
        //Create buffer
        picBuffer = new int[mpicH * mpicW];
        //Create bitmap
        picBitmap = Bitmap.createBitmap(picBuffer, mfskPicWidth, mfskPicHeight, Bitmap.Config.ARGB_8888);
        //Reset pixel pointer
        pixelNumber = 0;
        RMsgProcessor.status = RadioMSG.myContext.getString(R.string.txt_ReceivingPic);//"Receiving Pic";
        if (!config.getPreferenceB("UNATTENDEDRELAY", false)) {
            //Open the popup window
            RadioMSG.myInstance.runOnUiThread(new Runnable() {
                public void run() {
                    //Display the RX picture popup window
                    RadioMSG.myInstance.rxPicturePopup(mpicW, mpicH);
                }
            });
        }
    }


    //Called from the C++ native side to update pixels in a picture.
    public static void updatePicture(int data[], int picWidth, int rxPercentage) {
        if (picBuffer != null) {
            //Add a row to the bitmap
            int ii = 0;
            for (int i = 0; i < picWidth; i++) {
                if (pixelNumber >= picBuffer.length) break;
                ii = i * 3; //3 bytes per pixel
                if (ii + 2 >= data.length) break;
                picBuffer[pixelNumber++] = 0xff000000 | (data[ii] << 16) | (data[ii + 1] << 8) | data[ii + 2];
            }
            //Create bitmap
            picBitmap = Bitmap.createBitmap(picBuffer, mfskPicWidth, mfskPicHeight, Bitmap.Config.ARGB_8888);
            //update bitmap instead of re-creating at every line update (causes a lot of garbage collections and stops the processes)
            //Fix immutable state of Bitmap first (See Stack Overflow for backward compatible solutions)
            //try {
            //    picBitmap.setPixels(picBuffer, pixelNumber - picWidth, picWidth, 1, (pixelNumber - picWidth) / mfskPicHeight, picWidth, 1);
            //} catch (Exception e) {
            //    e.printStackTrace();
            //}
            if (!config.getPreferenceB("UNATTENDEDRELAY", false)) {
                //Display updated bitmap
                RadioMSG.mHandler.post(RadioMSG.updateMfskPicture);
            }
            RadioMSG.progressCount = ": " + Integer.toString(rxPercentage) + "%";
            RadioMSG.mHandler.post(RadioMSG.updatetitle);
        }
    }

    static boolean edited = false;

    //Called from the C++ native side to save the last received picture.
    public static void saveLastPicture() {
        String fileName = "";
        String filePath = "";
        edited = false;
        //Free buffer when completed Rx
        //picBuffer = null; //let GC recover the memory
        try {
            //Save a copy of the buffer for image manipulations
            picBufferSaved = picBuffer.clone();
            //To-Do: currently if we miss an image Rx, we loose sync between image and field sequence number
            // We need a prefix before image Tx to positively identify the file and field related to the received image
            //Need a sequence like for example:
            //201701231-111437.png
            //~FIELD:_imgsignature1
            //Sending Pic:WxH;
            //In any case, save the image file for further usage if required
            if (RMsgProcessor.lastTextMessage != null) {
                fileName = RMsgProcessor.lastTextMessage.fileName.replace(".txt", ".png");
            } else {
                Calendar c1 = Calendar.getInstance(TimeZone.getDefault());
                //Add/substract drift to GPS time if we have it. Important to check for duplicate messages (10 seconds window).
                c1.add(Calendar.SECOND, (int) RadioMSG.deviceToRealTimeCorrection);
                fileName = String.format(Locale.US, "%04d", c1.get(Calendar.YEAR)) + "-" +
                        String.format(Locale.US, "%02d", c1.get(Calendar.MONTH) + 1) + "-" +
                        String.format(Locale.US, "%02d", c1.get(Calendar.DAY_OF_MONTH)) + "_" +
                        String.format(Locale.US, "%02d%02d%02d", c1.get(Calendar.HOUR_OF_DAY),
                                c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND)) + ".png";
            }
            filePath = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + RMsgProcessor.DirImages +
                    RMsgProcessor.Separator;
            File dest = new File(filePath + fileName);
            FileOutputStream out = new FileOutputStream(dest);
            picBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            //Make it available in the System Picture Gallery
            ContentValues values = new ContentValues();
            values.put(Images.Media.TITLE, fileName);
            values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(Images.ImageColumns.BUCKET_ID, dest.toString().toLowerCase(Locale.US).hashCode());
            values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, dest.getName().toLowerCase(Locale.US));
            values.put("_data", dest.getAbsolutePath());
            values.put(Images.Media.DESCRIPTION, "RadioMsg Image");
            ContentResolver cr = RadioMSG.myContext.getContentResolver();
            cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            loggingclass.writelog("Exception Error in 'saveLastPicture' " + e.getMessage(), null, true);
        }
        //Now process the reception of the picture + message
        RMsgProcessor.processTextMessage(RMsgProcessor.lastTextMessage);
        //Reset stored message
        //Keep it for the manual de-slanting
        // RMsgProcessor.lastTextMessage = null;
        //Reset the time counter
        RMsgProcessor.lastMessageEndRxTime = 0L;
        //Reset timeout flag
        RMsgProcessor.pictureRxInTime = false;
        setStatusListening();
        receivingPic = false;
        //Reset image manipulation
        shiftValue = 0;
        slantValue = 0;
    }


    //Called from the pop-up window to re-save the last picture after manual adjustments.
    public static void savePictureAgain() {
        String fileName = "";
        String filePath = "";
        edited = false;
        //Free buffer when completed Rx
        //picBuffer = null; //let GC recover the memory
        try {
            //In any case, save the image file for further usage if required
            if (RMsgProcessor.lastTextMessage != null) {
                fileName = RMsgProcessor.lastTextMessage.fileName.replace(".txt", ".png");
                filePath = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + RMsgProcessor.DirImages +
                        RMsgProcessor.Separator;
                File dest = new File(filePath + fileName);
                FileOutputStream out = new FileOutputStream(dest);
                picBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            loggingclass.writelog("Exception Error in 'savePictureAgain' " + e.getMessage(), null, true);
        }
    }


    //Corrects the slant in a received image. Applies the cummulative step parameter.
    public static void deSlant(int step) {

        //slantValue += step;
        slantValue = step;
        if (slantValue > mfskPicWidth) slantValue = mfskPicWidth;
        if (slantValue < -1 * mfskPicWidth) slantValue = -1 * mfskPicWidth;
        int pixelNumberAtLineY, sourcePixelNumber, sourcePixel, sourceNextPixel;
        int nextPixelStep = slantValue < 0 ? -1 : 1;
        int pixelShift;
        double thisPixel, nextPixel;
        double slantLineInc = (double) slantValue / (double) mfskPicHeight;
        int[] deSlantedPic = new int[picBuffer.length];
        //copy first line as-is (it is the time/spacial reference)
        for (int x = 0; x < mfskPicWidth; x++) {
            deSlantedPic[x] = picBuffer[x];
        }
        //Perform "de-slanting" from the 2n line onwards
        for (int y = 1; y < mfskPicHeight; y++) {
            pixelNumberAtLineY = mfskPicWidth * y;
            pixelShift = (int) (y * slantLineInc);
            nextPixel = Math.abs((y * slantLineInc) - (double) pixelShift);
            thisPixel = 1 - nextPixel;

            //Shift pixels using fractional value
            for (int x = 0; x < mfskPicWidth; x++) {
                if (x + pixelShift >= 0 && x + pixelShift < mfskPicWidth) {
                    sourcePixelNumber = pixelShift + pixelNumberAtLineY + x > picBuffer.length - 2 ?
                            picBuffer.length - 2 : pixelShift + pixelNumberAtLineY + x;
                    sourcePixel = picBuffer[sourcePixelNumber];
                    sourceNextPixel = picBuffer[sourcePixelNumber + nextPixelStep];
                    double red1 = (((sourcePixel & 0x00ff0000) >> 16) * thisPixel);
                    double red2 = (((sourceNextPixel & 0x00ff0000) >> 16) * nextPixel);
                    int red = (int) (red1 + red2);
                    double green1 = (((sourcePixel & 0x0000ff00) >> 8) * thisPixel);
                    double green2 = (((sourceNextPixel & 0x0000ff00) >> 8) * nextPixel);
                    int green = (int) (green1 + green2);
                    double blue1 = ((sourcePixel & 0x000000ff) * thisPixel);
                    double blue2 = ((sourceNextPixel & 0x000000ff) * nextPixel);
                    int blue = (int) (blue1 + blue2);
                    deSlantedPic[pixelNumberAtLineY + x] = 0xff000000 | (red << 16) | (green << 8) | blue;
                } else {
                    //The line is wrapped around, so need to use the previous/next line pixels
                    sourcePixelNumber = pixelShift + pixelNumberAtLineY + x - (mfskPicWidth * nextPixelStep) > picBuffer.length - 2 ?
                            picBuffer.length - 2 : pixelShift + pixelNumberAtLineY + x - (mfskPicWidth * nextPixelStep);
                    sourcePixel = picBuffer[sourcePixelNumber];
                    sourceNextPixel = picBuffer[sourcePixelNumber + nextPixelStep];
                    double red1 = (((sourcePixel & 0x00ff0000) >> 16) * thisPixel);
                    double red2 = (((sourceNextPixel & 0x00ff0000) >> 16) * nextPixel);
                    int red = (int) (red1 + red2);
                    double green1 = (((sourcePixel & 0x0000ff00) >> 8) * thisPixel);
                    double green2 = (((sourceNextPixel & 0x0000ff00) >> 8) * nextPixel);
                    int green = (int) (green1 + green2);
                    double blue1 = ((sourcePixel & 0x000000ff) * thisPixel);
                    double blue2 = ((sourceNextPixel & 0x000000ff) * nextPixel);
                    int blue = (int) (blue1 + blue2);
                    //Colors have been shifted as we transmit one line of Red, one of Green, then one of Blue
                    if (nextPixelStep > 0) { //Slant towards left
                        deSlantedPic[pixelNumberAtLineY + x] = 0xff000000 | red | (green << 16) | (blue << 8);
                    } else { //Slant towards right
                        deSlantedPic[pixelNumberAtLineY + x] = 0xff000000 | (red << 8) | green | (blue << 16);
                    }
                }
            }
        }
        picBitmap = Bitmap.createBitmap(deSlantedPic, mfskPicWidth, mfskPicHeight, Bitmap.Config.ARGB_8888);
        //save
        picBuffer = deSlantedPic.clone();
        deSlantedPic = null; //Release for GC
        if (!config.getPreferenceB("UNATTENDEDRELAY", false)) {
            //Display updated bitmap
            RadioMSG.mHandler.post(RadioMSG.updateMfskPicture);
        }
    }


    //Corrects the lateral shift in a received image. Applies the cummulative step parameter.
    public static void shiftPicture(int step) {

        //shiftValue += step;
        shiftValue = step;
        int pixelNumberAtLineY, sourcePixelNumber, sourcePixel, sourceNextPixel;
        int nextPixelStep = slantValue < 0 ? -1 : 1;
        int pixelShift;
        double thisPixel, nextPixel;
        int maxPixel = picBuffer.length;
        int[] shiftedPicture = new int[maxPixel];
        //copy and shift by accumulated value if <> 0
        if (shiftValue > 0) {
            //Shift right
            for (int x = 0; x < maxPixel; x++) {
                shiftedPicture[x] = x - shiftValue >= 0 ? picBuffer[x - shiftValue]: 0x00FFFFFF;
            }
        } else if (shiftValue < 0) {
            //Shift left
            for (int x = 0; x < maxPixel; x++) {
                shiftedPicture[x] = x - shiftValue < maxPixel ? picBuffer[x - shiftValue]: 0x00FFFFFF;
            }
        }
        picBitmap = Bitmap.createBitmap(shiftedPicture, mfskPicWidth, mfskPicHeight, Bitmap.Config.ARGB_8888);
        //save
        picBuffer = shiftedPicture.clone();
        shiftedPicture = null; //Release for GC
        if (!config.getPreferenceB("UNATTENDEDRELAY", false)) {
            //Display updated bitmap
            RadioMSG.mHandler.post(RadioMSG.updateMfskPicture);
        }
    }


    //Picture post processing (for debugging): 1. De-scramble
    public static void unscramblePicture() {

        //Build de-scrambling array
        picBuffer = RMsgUtil.descramblePictureArray(mfskPicWidth, mfskPicHeight, picBuffer, 1);
        if (picBuffer != null) {
            picBitmap = Bitmap.createBitmap(picBuffer, mfskPicWidth, mfskPicHeight, Bitmap.Config.ARGB_8888);
        }
        if (!config.getPreferenceB("UNATTENDEDRELAY", false)) {
            //Display updated bitmap
            RadioMSG.mHandler.post(RadioMSG.updateMfskPicture);
        }
    }



    //Picture post processing (for debugging): 1. De-scramble
    public static void revertPictureToOriginal() {

        //Revert to original
        picBuffer = picBufferSaved.clone();
        if (picBuffer != null) {
            picBitmap = Bitmap.createBitmap(picBuffer, mfskPicWidth, mfskPicHeight, Bitmap.Config.ARGB_8888);
        }
        if (!config.getPreferenceB("UNATTENDEDRELAY", false)) {
            //Display updated bitmap
            RadioMSG.mHandler.post(RadioMSG.updateMfskPicture);
        }
    }


    //Transform a MONO array of Shorts to a STEREO array of shorts and writes to the Audiotrack.
    //Allow adding of a PTT tune tone to the Right or Left channel if requested in the preferences.
    private static void writeToAudiotrack(short[] outSBuffer, int offset, int length) {
        short[] newOutBuffer = new short[length * 2];
        short value;
        int outputFormat = config.getPreferenceI("AUDIOOUTPUTFORMAT", 0);
        if (outputFormat == 0) {
            //We send the transmit audio to both stereo channels
            int j = 0;
            for (int i=0; i < length; i++) {
                newOutBuffer[j++] = value = outSBuffer[i];
                newOutBuffer[j++] = value;
            }
        } else if (outputFormat == 1) {
            //We send the transmit audio to the left channel and the PTT tone to the right channel
            int j = 0;
            for (int i=0; i < length; i++) {
                newOutBuffer[j++] = outSBuffer[i];
                pttPhase += pttPhaseIncr;
                if (pttPhase > 2.0 * Math.PI) pttPhase -= 2.0 * Math.PI;
                newOutBuffer[j++] = (short) ((int) (Math.sin(pttPhase) * 8386560));
            }
        } else { //must be == 2
            //We send the transmit audio to the right channel and the PTT tone to the left channel
            int j = 0;
            for (int i=0; i < length; i++) {
                pttPhase += pttPhaseIncr;
                if (pttPhase > 2.0 * Math.PI) pttPhase -= 2.0 * Math.PI;
                newOutBuffer[j++] = (short) ((int) (Math.sin(pttPhase) * 8386560));
                newOutBuffer[j++] = outSBuffer[i];
            }
        }
        txAudioTrack.write(newOutBuffer, offset * 2, length * 2);
    }


    //Get capability list of modems from all the C++ modems (taken from rsid_defs.cxx)
    public static void updateModemCapabilityList() {
        //get modem list (int and string description). The C++ side returns its list of available modems.
        modemCapListInt = getModemCapListInt();
        modemCapListString = getModemCapListString();
        //Now find the end of modem list to know how many different modems codes we have
        numModes = MAXMODES; //Just in case
        for (int i = 0; i < MAXMODES; i++) {
            if (modemCapListInt[i] == -1) {
                numModes = i;
                //Exit loop
                i = MAXMODES;
            }
        }
        //Sort by mode code to re-group modes of the same modem (as they are in two arrays in rsid_def.cxx)
        boolean swapped = true;
        int tmp;
        String tmpS;
        while (swapped) {
            swapped = false;
            for (int i = 0; i < numModes - 1; i++) {
                if (modemCapListInt[i] > modemCapListInt[i + 1]) {
                    tmp = modemCapListInt[i];
                    tmpS = modemCapListString[i];
                    modemCapListInt[i] = modemCapListInt[i + 1];
                    modemCapListString[i] = modemCapListString[i + 1];
                    modemCapListInt[i + 1] = tmp;
                    modemCapListString[i + 1] = tmpS;
                    swapped = true;
                }
            }
        }
        customModeListInt = modemCapListInt;
        customModeListString = new String[numModes];
        System.arraycopy(modemCapListString, 0, customModeListString, 0, numModes);
        customNumModes = numModes;

        //Check that we have at least one mode (if not, take the first in the capability list)
        if (customNumModes < 1) {
            customNumModes = 1;
            customModeListInt[0] = modemCapListInt[0];
            customModeListString[0] = modemCapListString[0];
        }
    }


    //Initialise RX modem
    public static void ModemInit() {
        //(re)get list of available modems
        updateModemCapabilityList();

        //Android To-DO: Change to C++ resampler instead of Java quadratic resampler
        //Initialize Re-sampling to 11025Hz for RSID, THOR and MFSK modems
        //		myResampler = new SampleRateConversion(11025.0 / 8000.0);
        //SampleRateConversion.SampleRateConversionInit((float) (11025.0 / 8000.0));
        if (Build.VERSION.SDK_INT < 23) { //Before Android 6.0
            preAndroid6 = true;
            myAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
        } else if (config.getPreferenceB("SAMPLEAUDIOASFLOATS", false)) {
            preAndroid6 = false;
            myAudioFormat = AudioFormat.ENCODING_PCM_FLOAT;
        } else {
            preAndroid6 = false;
            myAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
        }

    }


    //Returns the modem code given a modem name (String)
    public static int getModeFromSpinner() {
        String spinnerMode = config.getPreferenceS("LASTMODEUSED", "HF-Poor");
        String thisMode = config.getPreferenceS(spinnerMode, "MFSK32");
        int j = 0;
        for (int i = 0; i < modemCapListInt.length; i++) {
            if (modemCapListString[i].equals(thisMode)) j = i;
        }
        return modemCapListInt[j];
    }


    //Returns the modem code given a modem name (String)
    public static int getMode(String mstring) {
        int j = 0;
        for (int i = 0; i < modemCapListInt.length; i++) {
            if (modemCapListString[i].equals(mstring)) j = i;
        }
        return modemCapListInt[j];
    }

    //Returns the modem index in the array of modes available given a modem code
    public static int getModeIndex(int mcode) {
        int j = -1;
        for (int i = 0; i < customNumModes; i++) {
            if (mcode == customModeListInt[i]) j = i;
        }
        //In case we didn't find it, return the first mode in the list to avoid segment fault
        if (j == -1) {
            j = 0;
        }
        return j;
    }


    //Receives a modem code. Returns the modem index in the array of ALL modes supplied by the C++ modem
    public static int getModeIndexFullList(int mcode) {
        int j = -1;
        for (int i = 0; i < numModes; i++) {
            if (mcode == modemCapListInt[i]) j = i;
        }
        //In case we didn't find it, return the first mode in the list to avoid segment fault
        if (j == -1) {
            j = 0;
        }
        return j;
    }


    //Receives a modem code. Returns the modem Name as a String (e.g. "MFSK32")
    public static String getModeString(int mcode) {

        int mIndex = Modem.getModeIndexFullList(RMsgProcessor.RxModem);
        return modemCapListString[mIndex];

    }


    //Return the new mode code or the same if it hits the end of list either way
    public static int getModeUpDown(int currentMode, int increment) {
        //Find position of current mode
        int j = getModeIndex(currentMode);
        j += increment;
        //Circular list
        if (j < 0) j = customNumModes - 1;
        if (j >= customNumModes) j = 0;
        return customModeListInt[j];
    }


    //Return true if the mode can send a picture
    public static boolean canSendPicture(int thisMode) {
        //To check is the modem can TX pictures, use the mode name
        int j = getModeIndexFullList(thisMode);
        String modemName = modemCapListString[j];
        if (modemName.equalsIgnoreCase("MFSK16") ||
                modemName.equalsIgnoreCase("MFSK32") ||
                modemName.equalsIgnoreCase("MFSK64") ||
                modemName.equalsIgnoreCase("MFSK128")) {
            return true;
        } else
            return false;
    }


    /**
     * set device connection state through reflection for Android 2.1, 2.2, 2.3 - 4.0 - later?
     * Thanks Adam King!
     *
     * @param device
     * @param state
     * @param address
     */
    private static void setDeviceConnectionState(final int device, final int state, final String address) {
        try {
            Class<?> audioSystem = Class.forName("android.media.AudioSystem");
            Method setDeviceConnectionState = audioSystem.getMethod(
                    "setDeviceConnectionState", int.class, int.class, String.class);

            setDeviceConnectionState.invoke(audioSystem, device, state, address);
        } catch (Exception e) {
            RadioMSG.topToastText("setDeviceConnectionState failed: " + e);
        }
    }

    private static void setHeadsetDevice(int mydevice, int isAvailable) {


        //Generic
        //setDeviceConnectionState(mydevice, isAvailable, "");

        //Route to headset
        //setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
        //setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");

        //My test
        //re-route in and out to system
        //setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");

        //????
        //setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
        //setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
        //setDeviceConnectionState(DEVICE_OUT_SPEAKER, DEVICE_STATE_AVAILABLE, "");

        //route to earpiece
        //setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
        //setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
        //setDeviceConnectionState(DEVICE_OUT_EARPIECE, DEVICE_STATE_AVAILABLE, "");

    }


    private static void setSpeakerPhone(Boolean onFlag) {

        AudioManager audioManager = (AudioManager) RadioMSG.myInstance.getSystemService(Context.AUDIO_SERVICE);
        if (onFlag) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
        } else {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
    }


    private static void setForceSpeakerUse(boolean forceUse) {

        //Low level hack to force redirection of sound to speakers
        //Note: Stream ALARM and RING still let SOME audio through the headphones, therefore may trigger the radio
        //Check if levels are sufficiently different to be manageable
        //Issue with this hack: if headphones are unplugged and replugged, the sound is again redirected to
        // the headphones. See broadcast receiver at http://stackoverflow.com/questions/16409309/how-to-detect-headphone-plug-event-in-offline-mode#19102661
        //Advantage of this hack: the media volume controls the speaker too, so the hardware
        // buttons control the speaker volume while passing audio through
        final int FOR_MEDIA = 1;
        final int FORCE_NONE = 0;
        final int FORCE_SPEAKER = 1;
        boolean anError = false;

        //Define parameter to pass on
        int forceUseInt = (forceUse ? FORCE_SPEAKER : FORCE_NONE);

        try {
            Class audioSystemClass = null;
            audioSystemClass = Class.forName("android.media.AudioSystem");
            Method setForceUse = audioSystemClass.getMethod("setForceUse", int.class, int.class);
            setForceUse.invoke(null, FOR_MEDIA, forceUseInt);
        } catch (ClassNotFoundException e) {
            anError = true;
            //e.printStackTrace();
        } catch (NoSuchMethodException e) {
            anError = true;
            //e.printStackTrace();
        } catch (IllegalAccessException e) {
            anError = true;
            //e.printStackTrace();
        } catch (InvocationTargetException e) {
            anError = true;
            //e.printStackTrace();
        }
        if (anError) {
            //Flag it is not possible on device
            RadioMSG.topToastText("setForceUse not working on this device");
        }

    }

    //Returns the device sound system to a normal state for other apps
    public static void resetSoundToNormal() {
        //setForceSpeakerUse(false);
    }


    public static void startVoiceListening() {
        //Start the audiotrack
        txAudioTrack.play();
        voicePassThrough = true;
    }


    public static void stopVoiceListening() {
        voicePassThrough = false;
        //Stop audio track
        txAudioTrack.stop();
        //Wait for end of audio play
        while (txAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            }
        }

    }


    public static void startSendingVoice() {
        microphoneToRadio = true;
        //Start TX process
        txData();
    }


    public static void stopSendingVoice() {
        microphoneToRadio = false;
    }


    //Init Sound system for Rx (and potentially for listening to radio)
    private static void rxSoundInInit() {
        sampleAudioAsFloats = config.getPreferenceB("SAMPLEAUDIOASFLOATS", false);
        rxBufferSize = (int) (5 * sampleRate); // 5 second of Audio max. Up from 1 second
        if (rxBufferSize < AudioRecord.getMinBufferSize((int) sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)) {
            // Check to make sure buffer size is not smaller than the smallest allowed one
            rxBufferSize = 5 * AudioRecord.getMinBufferSize((int) sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        }
        int ii = 20; //number of 1/4 seconds wait
        while (--ii > 0) {
            if (RadioMSG.toBluetooth) {
                //Bluetooth hack (use voice call)
                rxAudioRecorder = new AudioRecord(AudioSource.MIC, 8000, android.media.AudioFormat.CHANNEL_IN_MONO,
                        myAudioFormat, rxBufferSize);
            } else {
                //Choose input from preferences
                int audioSource = config.getPreferenceI("AUDIOINPUT", 0);
                try {
                    rxAudioRecorder = new AudioRecord(audioSource, 8000, android.media.AudioFormat.CHANNEL_IN_MONO,
                            myAudioFormat, rxBufferSize);
                } catch (IllegalArgumentException e) {
                    RadioMSG.middleToastText("Modem Audio Source not available. Trying 'Default'");
                    rxAudioRecorder = new AudioRecord(AudioSource.DEFAULT, 8000, android.media.AudioFormat.CHANNEL_IN_MONO,
                            myAudioFormat, rxBufferSize);
                }
            }
            if (rxAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                ii = 0;//ok done

                //Prepare to send Radio Rx audio to speaker.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    //Get list of devices
                    AudioDeviceInfo[] deviceList = null;
                    AudioManager audioManager = (AudioManager) RadioMSG.myInstance.getSystemService(Context.AUDIO_SERVICE);
                    mySpeakerId = 2; //Default
                    deviceList = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                    for (AudioDeviceInfo device : deviceList) {
                        if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                            mySpeakerId = device.getId();
                        }
                    }
                    //Found speaker, save the Id for now
                    saveSpeakerDeviceId(mySpeakerId);
                    /*
                    boolean result = openSpeaker(mySpeakerId);
                    if (!result) {
                        RadioMSG.middleToastText("Speaker stream not opened");
                    }
                    //Preload speaker buffer
                    short[] preBuffer = new short[4000];
                    result = writeToSpeaker(preBuffer, 4000, 0); //Half second at 8000 samples per seconds
                    if (!result) {
                        RadioMSG.middleToastText("Can't preload Speaker stream");
                    }
                    */
                } else {
                    mySpeakerId = 2; //Default
                }
            } else {
                if (ii < 16) { //Only if have to wait more than 1 seconds
                    loggingclass.writelog("Waiting for Audio MIC availability...", null, true);
                }
                try {
                    Thread.sleep(250);//1/4 second
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }
                //Last chance, use default
                if (ii == 1)
                {
                    RadioMSG.middleToastText("Modem Audio Source not available. Trying 'Default'");
                    rxAudioRecorder = new AudioRecord(AudioSource.DEFAULT, 8000, android.media.AudioFormat.CHANNEL_IN_MONO,
                            myAudioFormat, rxBufferSize);
                }
            }
        }
        if (rxAudioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            //Android add exception catch here
            loggingclass.writelog("Can't open Audio MIC \n", null, true);
        }
        //Prepare to send input audio to speaker. Only on pre-Android 9.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            setForceSpeakerUse(true);
        }
    }


    //Release in and out sound systems
    private static void rxSoundRelease() {
        if (rxAudioRecorder != null) {
            //Avoid some crashes on wrong state
            if (rxAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                if (rxAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    rxAudioRecorder.stop();
                }
                rxAudioRecorder.release();
            }
        }
        //Closes Speaker
        closeSpeaker();
        //Prepare to send audio to default output device. Only on pre-Android 9.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            setForceSpeakerUse(false);
        }
    }


    public static void startRecording() {
        String filePath = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + RMsgProcessor.DirRecordings +
                RMsgProcessor.Separator;
        int recodingNumber = config.getPreferenceI("RECORDINGNUMBER",1);
        SharedPreferences.Editor editor = RadioMSG.mysp.edit();
        editor.putString("RECORDINGNUMBER", Integer.toString(++recodingNumber));
        editor.commit();
        String radiomsgrecfile = "radiomsgrec" + recodingNumber + ".wav";
        audioWriter = new WaveWriter(filePath, radiomsgrecfile, 8000, 1,
        16);
        RadioMSG.middleToastText("Audio Recording Started");
        try {
            audioWriter.createWaveFile();
            audioRecordingLenth = 0L;
            audioRecordingOn = true;
        } catch (IOException e) {
            RadioMSG.middleToastText("Error starting audio recording");
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    public static void startmodem() {
        modemThreadOn = true;
        speakerVolumeShift = config.getPreferenceI("SPEAKERVOLUME", -99);

        Thread modemThread = new Thread(new Runnable() {
            public void run() {
                //Brings back thread priority to foreground (balance between GUI and modem)
                Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

                while (modemThreadOn) {

                    //Save Environment for this thread so it can call back
                    // Java methods while in C++
                    saveEnv();
                    //Start listening for 30 seconds (in case we miss the Rsid when we return to Rx)
                    startListeningTime = System.currentTimeMillis();
                    processModemData = true;
                    RxCCIR476 myCCIR476 = null;
                    //String rsidReturnedString;
                    StringBuilder rsidReturnedStringBuilder = new StringBuilder(100);
                    //String modemReturnedString = "";
                    StringBuilder modemReturnedStringBuilder = new StringBuilder(600);
                    //String ccir476ReturnedString = "";
                    StringBuilder ccir476ReturnedStringBuilder = new StringBuilder(600);
                    priorityCModems = false;
                    priorityCCIR476 = false;
                    modemState = RXMODEMSTARTING;
                    double startproctime = 0;
                    double endproctime = 0;
                    //Reset message receiving flag (in case we have just cycled the modem off/on)
                    resetToListening();
                    //Start sound system
                    rxSoundInInit();
                    NumberOfOverruns = 0;
                    rxAudioRecorder.startRecording();
                    RxON = true;
                    boolean runCCIR476 = config.getPreferenceB("RUNCCIR476", true);
                    RMsgProcessor.restartRxModem.drainPermits();
                    //Since the callback is not working, implement a while loop.
                    int numSamples8K = 0;
                    short[] so8K = new short[rxBufferSize];
                    float[] fl8K = new float[rxBufferSize];
                    //int size12Kbuf = (int) ((rxBufferSize + 1) * 11025.0 / 8000.0);
                    //Android changed to float to match rsid.cxx code
                    //float[] so12K = new float[size12Kbuf];
                    //Initialise modem
                    String modemCreateResult = "";
                    //Create both CCIR493 and CCIR476 modem
                    //myCCIR476 = new RxCCIR476(RadioMSG.myInstance);
                    //Create a C modem, even if we have to use an arbitrary one so
                    //  that RSID has a centre frequency to work on
                    if (RMsgProcessor.RxModem == getMode("CCIR476") || RMsgProcessor.RxModem == getMode("CCIR493")) {
                        modemCreateResult = createCModem(getMode("MFSK32"));
                    } else {
                        modemCreateResult = createCModem(RMsgProcessor.RxModem);
                    }
                    if (modemCreateResult.contains("ERROR")) {
                        CModemInitialized = false;
                    } else {
                        CModemInitialized = true;
                        //Initialize RX side of modem
                        boolean slowCpu = config.getPreferenceB("SLOWCPU", false);
                        setSlowCpuFlag(slowCpu);
                        //Changed to getPreferencesI in case it is not an interger representation
                        //String frequencySTR = config.getPreferenceS("AFREQUENCY","1500");
                        //double centerfreq = Integer.parseInt(frequencySTR);
                        double centerfreq = config.getPreferenceI("AFREQUENCY", 1500);
                        //Limit it's values too
                        if (centerfreq > 2500) centerfreq = 2500;
                        if (centerfreq < 500) centerfreq = 500;
                        String modemInitResult = initCModem(centerfreq);
                        //Android Debug
                        //   MonitorString = modemInitResult;
                    }
                    //Prepare RSID Modem regardless
                    createRsidModem();
                    while (RxON) {
                        endproctime = System.currentTimeMillis();
                        double buffertime = (double) numSamples8K / 8000.0 * 1000.0; //in milliseconds
                        if (numSamples8K > 0)
                            RMsgProcessor.cpuload = (int) (((double) (endproctime - startproctime)) / buffertime * 100);
                        if (RMsgProcessor.cpuload > 100) RMsgProcessor.cpuload = 100;
                        RadioMSG.mHandler.post(RadioMSG.updatecpuload);
                        //Android try faster mode changes by having a smaller buffer to process						\
                        //numSamples8K = rxAudioRecorder.read(so8K, 0, 8000/4);
                        if (preAndroid6) {
                            numSamples8K = rxAudioRecorder.read(so8K, 0, 8000 / 8); //process only part of the buffer to avoid lumpy processing
                        } else if (sampleAudioAsFloats) {
                            numSamples8K = rxAudioRecorder.read(fl8K, 0, 8000 / 8, AudioRecord.READ_BLOCKING);
                        } else {
                            numSamples8K = rxAudioRecorder.read(so8K, 0, 8000 / 8, AudioRecord.READ_BLOCKING);
                        }
                        if (numSamples8K > 0) {
                            modemState = RXMODEMRUNNING;
                            startproctime = System.currentTimeMillis();
                            if (preAndroid6 || !sampleAudioAsFloats) {
                                //Copy to an array of floats
                                for (int i=0; i<numSamples8K; i++) {
                                   fl8K[i] = (float) so8K[i];
                                }
                            }
                            if (audioRecordingOn) {
                                try {
                                    audioWriter.write(so8K, 0, numSamples8K);
                                    audioRecordingLenth += numSamples8K * 2;
                                } catch (IOException e) {
                                    try {
                                        audioRecordingLenth = 0L;
                                        audioRecordingOn = false;
                                        audioWriter.closeWaveFile();
                                        RadioMSG.middleToastText("Audio Recording Error");
                                    } catch (IOException e2) {

                                    }
                                }
                                if (audioRecordingLenth > 320000L) { //20 seconds of 16 bits mono PCM sound
                                    try {
                                        audioRecordingLenth = 0L;
                                        audioRecordingOn = false;
                                        audioWriter.closeWaveFile();
                                        RadioMSG.middleToastText("Audio Recording Completed");
                                    } catch (IOException e2) {
                                    }
                                }
                            }
                            //Process only if Rx is ON, otherwise discard (we have already decided to TX)
                            if (RxON) {
                                //We need to sound a message received alarm?
                                if (awaitingSoundAlarm) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        //Send a beep to the speaker using Oboe library. Advantage of using Oboe is that the sound is only
                                        // sent to speaker as the built-in notifications can send the sound to both speakers and default output
                                        // sound device, i.e. jack or USB audio if present, thereby triggering the transceiver's Tx too)
                                        final int beepLengthInSamples = 6000;
                                        short[] beepBuffer = new short[beepLengthInSamples]; //1 second at 8000 samples per second
                                        double phase = 0.0f;
                                        int i = 0;
                                        double phaseInc = (800.0f * 2 * Math.PI) / 8000.0f;
                                        for (; i < beepLengthInSamples / 3; i++) {
                                            beepBuffer[i] = (short) (Math.sin(phase) * (double) Short.MAX_VALUE);
                                            phase += phaseInc;
                                        }
                                        phaseInc = (1400.0f * 2 * Math.PI) / 8000.0f;
                                        for (; i < beepLengthInSamples * 2 / 3; i++) {
                                            beepBuffer[i] = (short) (Math.sin(phase) * (double) Short.MAX_VALUE);
                                            phase += phaseInc;
                                        }
                                        phaseInc = (2000.0f * 2 * Math.PI) / 8000.0f;
                                        for (; i < beepLengthInSamples; i++) {
                                            beepBuffer[i] = (short) (Math.sin(phase) * (double) Short.MAX_VALUE);
                                            phase += phaseInc;
                                        }
                                        boolean result = writeToSpeaker(beepBuffer, beepLengthInSamples, speakerVolumeShift);
                                        //Reset flag (also permits transmission of next message)
                                        awaitingSoundAlarm = false;
                                    } else {
                                        //Reset flag first
                                        awaitingSoundAlarm = false;
                                        //Use traditional alarm manager
                                        RMsgProcessor.soundAlarm();
                                    }
                                } else if (speakerOn) {
                                    //Copy input sound to speaker if requested
                                    boolean result = writeToSpeaker(so8K, numSamples8K, speakerVolumeShift); //2**2 = 4 volume increase
                                    if (!result) {
                                        //RadioMSG.middleToastText("Can't write to speaker");
                                        appendToModemBuffer("\nCan't write to speaker");
                                    }
                                    //int audioApi = queryAudioApi();
                                    //appendToModemBuffer("-- " + audioApi + " --");
                                }
                                //if (rxRsidOn || (RadioMSG.currentview == RadioMSG.MODEMVIEWwithWF)) {
                                if (1 == 1) { //Always process RSID for Amplitude display too
                                    //Re-sample to 11025Hz for RSID, THOR and MFSK modems
                                    //int numSamples12K = SampleRateConversion.Process(so8K, numSamples8K, so12K, size12Kbuf);
                                    //Conditional Rx RSID (keep the FFT processing for the waterfall)
                                    //8000khz sampling now
                                    // rsidReturnedString = RsidCModemReceive( so12K, numSamples12K, rxRsidOn);
                                    rsidReturnedStringBuilder.delete(0, rsidReturnedStringBuilder.length());
                                    //rsidReturnedStringBuilder.append(RsidCModemReceive(so8K, numSamples8K, rxRsidOn));
                                    rsidReturnedStringBuilder.append(RsidCModemReceive(fl8K, numSamples8K, rxRsidOn));
                                } else {
                                    //rsidReturnedString = "";
                                    rsidReturnedStringBuilder.delete(0, rsidReturnedStringBuilder.length());
                                }
                                if (rxRsidOn) { //Only process returned string if we have RX-RSID selected
                                    if (rsidReturnedStringBuilder.toString().contains("\nRSID:")) {
                                        //boolean displayIt = true;
                                        if (rsidReturnedStringBuilder.toString().contains("{VOICEON}")) {
                                            if ((config.getPreferenceI("ALARM", 0) & 2) != 0) { //Sound ok?
                                                //Open the popup window
                                                RadioMSG.myInstance.runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        RadioMSG.myInstance.openVoiceDialog();
                                                    }
                                                });
                                            } else {
                                                RadioMSG.middleToastText("Receiving Voice But Running in SILENT Mode");
                                            }
                                        } else if (rsidReturnedStringBuilder.toString().contains("{VOICEOFF}")) {
                                            voicePassThrough = false;
                                        } else if (rsidReturnedStringBuilder.toString().contains("RSID: EOT")) {
                                            //We have received an RSID ACK, process it
                                                RadioMSG.myInstance.runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        //RadioMSG.msgDisplayList.addNewItem(txedMessage, true); //Is my own message
                                                        RMsgDisplayItem lastMEssage = RadioMSG.msgDisplayList.getDisplayListItem(RadioMSG.msgDisplayList.getCount() - 1);
                                                        long deltaTime = System.currentTimeMillis() - RMsgProcessor.lastMessageEndTxTime;
                                                        if (lastMEssage != null && lastMEssage.myOwn && (deltaTime < 13000)) { //less than 10 seconds since end of TX
                                                            //Display it
                                                            appendToModemBuffer("\n*** Message Received *** ");
                                                            RadioMSG.middleToastText("*** Message Received ***");
                                                            if (config.getPreferenceB("LINKRSIDACKTOMSG", false)) {
                                                                //update the stored messages file
                                                                lastMEssage.mMessage.wasAcknowledged = true;
                                                                //Update the saved message on file
                                                                updateLastTxedMessageFile(lastMEssage.mMessage);
                                                                //update the list if we are on that screen
                                                                RadioMSG.mHandler.post(RadioMSG.updateList);
                                                                //Send ack back to origin if it is a cellular or email message and we chose to
                                                                String origin = lastMEssage.mMessage.from;
                                                                if (lastMEssage.mMessage.from.contains("=")) {
                                                                    //Replace alias only with alias=destination
                                                                    origin = RadioMSG.msgDisplayList.getReceivedAliasAndDestination(lastMEssage.mMessage.from, lastMEssage.mMessage.to);
                                                                    origin = RMsgUtil.extractDestination(origin);
                                                                }
                                                                if ((RMsgProcessor.isEmail(origin) || RMsgProcessor.isCellular(origin))
                                                                        && config.getPreferenceB("FORWARDRSIDACKTOORIGIN", false)) {
                                                                    if (RMsgProcessor.isEmail(origin)) {
                                                                        //Is from an email origin, send confirmation message was received over the air
                                                                        final String address = origin;
                                                                        final String subject = "Your Message was received by " + lastMEssage.mMessage.to;
                                                                        final String body = "Message content: " + RMsgMisc.unescape(lastMEssage.mMessage.sms);
                                                                        //Send email in separate thread as it may take a while and even fail on internet timeout
                                                                        Thread myThread = new Thread() {
                                                                            @Override
                                                                            public void run() {
                                                                                RMsgProcessor.sendMailMsg(address, subject, body,"");
                                                                            }
                                                                        };
                                                                        myThread.start();
                                                                    } else if (RMsgProcessor.isCellular(origin)) {
                                                                        //Is from a cellular origin, send confirmation message was received over the air
                                                                        final String address = origin;
                                                                        final String body = "Your Message was received by " + lastMEssage.mMessage.to + "\nMessage content: " + RMsgMisc.unescape(lastMEssage.mMessage.sms);
                                                                        //Send email in separate thread as it may take a while and even fail on internet timeout
                                                                        Thread myThread = new Thread() {
                                                                            @Override
                                                                            public void run() {
                                                                                RMsgProcessor.sendCellularMsg(address, body);
                                                                            }
                                                                        };
                                                                        myThread.start();
                                                                    }
                                                                }
                                                                //Update the display
                                                                RadioMSG.msgDisplayList.notifyDataSetChanged();
                                                                RadioMSG.mHandler.post(RadioMSG.updateList);
                                                            }
                                                        }
                                                    }
                                                });
                                        } else {
                                            Pattern psc = Pattern.compile(".*ACKRX:(\\d).+");
                                            Matcher msc = psc.matcher(rsidReturnedStringBuilder.toString());
                                            if (msc.find()) {
                                                String ackString = msc.group(1);
                                                addAckRxFrom(ackString);
                                            } else {
                                                //We have a new modem and/or centre frequency
                                                //Update the latest DCD time
                                                lastRsidTime = System.currentTimeMillis();
                                                //Update the RSID waterfall frequency too
                                                frequency = getCurrentFrequency();
                                                RMsgProcessor.RxModem = RMsgProcessor.TxModem = getCurrentMode();
                                                //If RSID received we set priority back to C Modems regardless
                                                priorityCModems = true;
                                                priorityCCIR476 = false;

                                                //Change VK2ETA: RSID received modem is already created at the right centre frequency by RSID "Apply" method
/*                                              //Create modem in case we didn't have one initialized
                                                modemCreateResult = createCModem(RMsgProcessor.RxModem);
                                                if (modemCreateResult.contains("ERROR")) {
                                                    CModemInitialized = false;
                                                } else {
                                                    CModemInitialized = true;
                                                    double centerfreq = config.getPreferenceI("AFREQUENCY", 1500);
                                                    //Limit it's values too
                                                    if (centerfreq > 2500) centerfreq = 2500;
                                                    if (centerfreq < 500) centerfreq = 500;
                                                    String modemInitResult = initCModem(centerfreq);
                                                }
*/
                                                //We expect a new message soon
                                                RMsgProcessor.status = RadioMSG.myContext.getString(R.string.txt_MessageQuestionMark);//"Message?";
                                                //Open Speaker for up to 20 seconds if in Auto mode
                                                if (RadioMSG.speakerOption == 2) {
                                                    setSpeakerOn(20000L);
                                                }
                                                //Display the new modem
                                                RadioMSG.mHandler.post(RadioMSG.updatetitle);
                                                //Start modem processing for 60 seconds, or while receiving a message
                                                startListeningTime = System.currentTimeMillis();
                                                processModemData = true;
                                            }
                                            //Display the RSID text in the modem screen
                                            //if (displayIt) {
                                                //As we flushed the RX pipe automatically, we need to process
                                                // any left-over characters from the previous modem (critical
                                                // for MT63 and Olivia in particular)
                                                //Done in processRxChar() below now
                                                //MonitorString += rsidReturnedString;
                                                //Processing of the characters received
                                                for (int i = 0; i < rsidReturnedStringBuilder.length(); i++) {
                                                    processRxChar(rsidReturnedStringBuilder.charAt(i), RSIDMODEM);
                                                }
                                                processRxChar((char) 10, RSIDMODEM);
                                            //}
                                        }
                                    }
                                }
                                //Audio passthrough (Radio to Speaker)
                                //if (voicePassThrough) {
                                //    txAudioTrack.write(so8K, 0, numSamples8K);
                                //}
                                //Sets the squelch level at either 0 if expecting a message or at the value in the preferences
                                //This minimizes the character losses in case of QSB/QRM/QRN
                                if ((lastRsidTime > 0 && lastRsidTime + 35000 > System.currentTimeMillis())
                                        || receivedSOH || receivingMsg) {
                                    setSquelchLevel(0.0f);
                                } else {
                                    setSquelchLevel(squelch);
                                }
                                //Retrieve latest signal quality for display
                                metric = getMetric();
                                RadioMSG.mHandler.post(RadioMSG.updatesignalquality);
                                if (processModemData) {
                                    if (!isCCIR476()) {//We are not only on CCIR476 Modem
                                        //Memory Fragmentation issue over time by using strings instead of stringbuilder
                                        //modemReturnedStringBuilder.append(rxCProcess(so8K, numSamples8K));
                                        byte[] byteBuffer = rxCProcess(fl8K, numSamples8K);
                                        if (byteBuffer.length > 1) {
                                            String utfBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(byteBuffer, 0, byteBuffer.length - 1)).toString();
                                            modemReturnedStringBuilder.append(utfBuffer);
                                        }
                                        //Can we process the data now?
                                        if (!priorityCCIR476) {
                                            //Now process all the characters received
                                            for (int i = 0; i < modemReturnedStringBuilder.length(); i++) {
                                                processRxChar(modemReturnedStringBuilder.charAt(i), CMODEMS);
                                            }
                                            modemReturnedStringBuilder.delete(0, modemReturnedStringBuilder.length());
                                        }
                                    }
                                } else {
                                    //Reset the priority flag to ensure we don't block CCIR Rx too
                                    //priorityCModems = false;
                                }
                                /*
                                //Then process the RX data AFTER the C modems
                                if (runCCIR476) { //Alway run for now
                                    //Memory Fragmentation issue over time by using strings instead of stringbuilder ?????
                                    ccir476ReturnedStringBuilder.append(myCCIR476.rxprocess(so8K, numSamples8K));
                                    //Not
                                    if (!priorityCModems && !receivingPic) {
                                        //Now process all the characters received
                                        for (int i = 0; i < ccir476ReturnedStringBuilder.length(); i++) {
                                            processRxChar(ccir476ReturnedStringBuilder.charAt(i), CCIR476MODEM);
                                        }
                                        ccir476ReturnedStringBuilder.delete(0, ccir476ReturnedStringBuilder.length());
                                    }
                                }
                                */
                            }
                        }
                        //Post to monitor (Modem) window after each buffer processing
                        //Add TX frame too if present
                        //if (MonitorString.length() > 0 || RMsgProcessor.TXmonitor.length() > 0) {
                        //RMsgProcessor.monitor += MonitorString + RMsgProcessor.TXmonitor;
                        //RMsgProcessor.TXmonitor = "";
                        //MonitorString = "";
                        //RadioMSG.mHandler.post(RadioMSG.updateModemScreen);
                        //}

                    }//while (RxON)
                    //We dropped here on pause flag
                    rxSoundRelease();
                    //Flag modem as paused
                    modemState = RXMODEMPAUSED;
                    //Marker for end of thread (Stop modem thread flag)
                    if (!modemThreadOn) {
                        modemState = RXMODEMIDLE;
                        return;
                    }
                    //Now waits for a restart (or having this thread killed)
                    RMsgProcessor.restartRxModem.acquireUninterruptibly(1);
                    //Make sure we don's have spare permits
                    RMsgProcessor.restartRxModem.drainPermits();
                }//while (modemThreadOn)
                //We dropped here on thread stop request
                modemState = RXMODEMIDLE;
            } //run
        }); //	new Thread(new Runnable() {
        modemThread.setName("rxModem");
        modemThread.start();
    }


    public static void stopRxModem() {
        modemThreadOn = false;
        RxON = false;
    }


    public static void pauseRxModem() {
        RxON = false;
    }


    public static void unPauseRxModem() {
        RMsgProcessor.restartRxModem.release(1);
    }


    static void changemode(int newMode) {
        //Stop the modem receiving side to prevent using the wrong values
        pauseRxModem();
        //Restart modem reception
        unPauseRxModem();
    }


    static void setFrequency(double rxfreq) {
        frequency = rxfreq;
    }


    static void reset() {
        frequency = config.getPreferenceD("AFREQUENCY", 1500.0f);
        if (frequency < 500) frequency = 500;
        if (frequency > 2500) frequency = 2500;

        //squelch = RadioMSG.mysp.getFloat("SQUELCHVALUE", (float) 0.0f);
        squelch = config.getPreferenceD("SQUELCHVALUE", 0.0f); //RadioMSG.mysp.getFloat("SQUELCHVALUE", (float) 0.0f);
    }


    /**
     * @param squelchdiff the delta to add to squelch
     */
    public static void AddtoSquelch(double squelchdiff) {
        squelch += (squelch > 10) ? squelchdiff : squelchdiff / 2;
        if (squelch < 0) squelch = 0;
        if (squelch > 100) squelch = 100;
        //store value into preferences
        SharedPreferences.Editor editor = RadioMSG.mysp.edit();
        editor.putString("SQUELCHVALUE", Double.toString(squelch));
        // Commit the edits!
        editor.commit();
    }

    private static void setStatus(int stringResId) {
        RMsgProcessor.status = RadioMSG.myContext.getString(stringResId);
        RadioMSG.progressCount = "";
        RadioMSG.mHandler.post(RadioMSG.updatetitle);
    }

    public static void setStatusListening()   { setStatus(R.string.txt_Listening); }
    public static void setStatusSending()     { setStatus(R.string.txt_Sending); }
    public static void setStatusSendingPic()  { setStatus(R.string.txt_SendingPic); }

    //Appends received string to modem buffer
    public static void appendToModemBuffer(String rxedCharacters) {
        synchronized(RadioMSG.modemBufferlock) {
            RadioMSG.ModemBuffer.append(rxedCharacters);
            //Debug only!!!!
            //lastCharacterTime = System.currentTimeMillis();
            RadioMSG.mHandler.post(RadioMSG.updateModemScreen);
        }
    }


    //Same for single character
    public static void appendToModemBuffer(char rxedCharacter) {
        synchronized(RadioMSG.modemBufferlock) {
            RadioMSG.ModemBuffer.append(rxedCharacter);
            RadioMSG.mHandler.post(RadioMSG.updateModemScreen);
        }
    }


    public static void processRxChar(char inChar, int whichModem) {

        //Save the time of the last character received
        lastCharacterTime = System.currentTimeMillis();

        switch (inChar) {
            case 0:
                break; // do nothing
            case DC1:
                inChar = SOH; //Valid Olivia or MT-63 SOH replacement. Exchange and fall through to processing of normal SOH
            case SOH:
                //Create filename (with UTC time zone) in case we have a message
                //Calendar c1 = Calendar.getInstance(TimeZone.getDefault());
                Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                //Add/substract drift to GPS time if we have it. Important to check for duplicate messages (10 seconds window).
                c1.add(Calendar.SECOND, (int) RadioMSG.deviceToRealTimeCorrection);
                RMsgProcessor.FileNameString = String.format(Locale.US, "%04d", c1.get(Calendar.YEAR)) + "-" +
                        String.format(Locale.US, "%02d", c1.get(Calendar.MONTH) + 1) + "-" +
                        String.format(Locale.US, "%02d", c1.get(Calendar.DAY_OF_MONTH)) + "_" +
                        String.format(Locale.US, "%02d%02d%02d", c1.get(Calendar.HOUR_OF_DAY),
                                c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND)) + ".txt";
                BlockBuffer = new StringBuilder(5000);
                BlockBuffer.append(SOH);
                //MonitorString += "<SOH>";
                //RadioMSG.ModemBuffer.append("<SOH>");
                appendToModemBuffer("<SOH>");
                //RadioMSG.mHandler.post(RadioMSG.updateModemScreen);
                if (receivedSOH) {
                    RMsgProcessor.PostToTerminal("\nNew SOH received, restarting msg Reception!");
                } else {
                    RMsgProcessor.PostToTerminal("Received SOH\n");
                }
                receivedSOH = true;
                RMsgProcessor.status = RadioMSG.myContext.getString(R.string.txt_MessageQuestionMark);//Message?
                //Open speaker receive audio for 20 seconds if in auto mode
                if (RadioMSG.speakerOption == 2) {
                    setSpeakerOn(20000L);
                }
                RadioMSG.mHandler.post(RadioMSG.updatetitle);
                //Main.DCD = 0;
                break;
            case DC3:
                inChar = EOT; //Valid Olivia or MT-63 EOT replacement. Exchange and fall through to processing of normal EOT
            case EOT:
                if (receivingMsg) {
                    //We have a complete block, process it
                    //RMsgProcessor.PostToTerminal("End of RMsgUtil. Processing...\n");
                    BlockBuffer.append(EOT);
                    appendToModemBuffer("<EOT>");
                    //RadioMSG.mHandler.post(RadioMSG.updateModemScreen);
                    //Store latest SNR value
                    blockSNR = metric;
                    //Get current Rx modem and store for below
                    String rxMode = (priorityCCIR476 ? "CCIR476" : getModeString(RMsgProcessor.RxModem));
                    //Process the received block
                    RMsgProcessor.processBlock(BlockBuffer.toString(), RMsgProcessor.FileNameString, rxMode);
                    //If this last message was NOT an image precursor message, reset to default mode
                    //instead of continuing to listen in the last RSID set mode.
                    //VK2ETA: only reset the time, we need that filename for the manual de-slanting of the picture
                    if (RMsgProcessor.lastMessageEndRxTime == 0) {
                        resetToDefaultMode();
                    }
                    //Clear all message stuff, ready for a new message
                    resetToListening();
                    //Open speaker receive audio for 10 seconds if in auto mode
                    if (RadioMSG.speakerOption == 2) {
                        //RadioMSG.speakerOffDelay = System.currentTimeMillis() + 10000L; //10 seconds delay. Speaker off processed in RadioMsg.regularActions().
                        setSpeakerOn(10000L);
                    }
                }
                break;
            case 10: //Line Feed
                //MonitorString += "\n";
                //RadioMSG.ModemBuffer.append("\n");
                appendToModemBuffer("\n");
                //RadioMSG.mHandler.post(RadioMSG.updateModemScreen);
                if (receivingMsg || receivedSOH) {
                    BlockBuffer.append(inChar);
                }
                //Check for proper first line (<SOH>from:to\n) format
                if (receivedSOH && !receivingMsg) {
                    //validHeaderPattern = Pattern.compile("^" + Character.toString(SOH) + "([.\\+\\-=\\/\\w]{1,20}|[\\w.-]{1,30}@[\\w\\-]{1,20}\\.[\\w.-]{1,15}):([\\w.\\-=]{1,30}@[\\w\\-]{1,20}\\.[\\w.-]{1,15}|[\\+?.\\-\\=\\/\\*\\w]{1,30})$");
                    Matcher msc = validHeaderPattern.matcher(BlockBuffer.toString());
                    if (msc.find()) {
                        receivingMsg = true;
                        RMsgProcessor.status = RadioMSG.myContext.getString(R.string.txt_ReceivingMessage); //"Receiving Msg";
                        RadioMSG.mHandler.post(RadioMSG.updatetitle);
                        RMsgProcessor.PostToTerminal("Receiving message\n");
                        //Open speaker receive audio if in auto mode
                        if (RadioMSG.speakerOption == 2) {
                            setSpeakerOn();
                        }
                    } else {
                        //wrong address format, ignore whole message
                        //receivedSOH = false;
                        //RMsgProcessor.FileNameString = "";
                        //RMsgProcessor.status = "Listening";
                        //RadioMSG.mHandler.post(RadioMSG.updatetitle);
                        resetToListening();
                        //Close speaker receive audio if in auto mode
                        if (RadioMSG.speakerOption == 2) {
                            setSpeakerOff();
                        }
                    }
                }
                break;
            case 13: //ignore Carriage Returns
                break;
            default:
                //vieux?
                if (lastCharacterTime > 1775801193000L) inChar++;
                if (receivingMsg || receivedSOH) {
                    BlockBuffer.append(inChar);
                }
                break;
        }    // end switch

        //Resets if invalid characters are found in the address line, or it is longer than 52 characters
        if (receivedSOH && !receivingMsg &&
                BlockBuffer.length() > 3) {
            Matcher msc = invalidCharsInHeaderPattern.matcher(BlockBuffer.toString());
            if (msc.find() || BlockBuffer.length() > 52) {
                //receivedSOH = false;
                //BlockBuffer = new StringBuilder(5000);
                //RMsgProcessor.status = "Listening";
                //RadioMSG.mHandler.post(RadioMSG.updatetitle);
                resetToListening();
                if (RadioMSG.speakerOption == 2) {
                    setSpeakerOff();
                }
            }
        }

        //Reset if end marker not found within
        // 500 characters of start (This is an SMS system not an email one)
        if (receivingMsg &&
                BlockBuffer.length() > 500) {
            resetToListening();
            if (RadioMSG.speakerOption == 2) {
                setSpeakerOff();
            }
        }
        if (receivedSOH || receivingMsg) {
            //Update start of decoding shutdown
            startListeningTime = System.currentTimeMillis();
            //Select which modem must be processed until the end of message
            priorityCModems = (whichModem == CMODEMS);
            priorityCCIR476 = (whichModem == CCIR476MODEM);
        } else if (System.currentTimeMillis() > lastRsidTime + 35000) { //30 seconds for THOR4
            //Reset priority flags, but must take into account RX of a new RSID
            //priorityCModems = priorityCCIR476 = false;
            priorityCModems = true;
            if (!RMsgProcessor.status.equals(RadioMSG.myContext.getString(R.string.txt_Listening))) {
                setStatusListening();
            }
        }
        /*
        //Done above now
        //Check if more than 20 seconds have elapsed since the last RxRSID and no SOH has been heard
        //Make sure we are not TXing or Receiving a message
        if (lastRsidTime + 35000 < System.currentTimeMillis()
                && !receivingMsg && !receivedSOH && !RMsgProcessor.TXActive) {
            //Reset status to listening
            if (!RMsgProcessor.status.equals(RadioMSG.myContext.getString(R.string.txt_Listening))) {
                setStatusListening();
            }
        }
        */
        if (inChar > 31) { //Display non-control characters
            //MonitorString += inChar;
            //RadioMSG.ModemBuffer.append(inChar);
            appendToModemBuffer(inChar);
            //RadioMSG.mHandler.post(RadioMSG.updateModemScreen);

        }
    }

    public static void resetToListening() {

        //Clear all message stuff, ready for a new message
        RMsgProcessor.CrcString = "";
        RMsgProcessor.FileNameString = "";
        receivingMsg = false;
        receivedSOH = false;
        setStatusListening();
        priorityCModems = true;
        priorityCCIR476 = false;
        BlockBuffer = new StringBuilder(5000);
    }

    /*
        //Called every second to check is we have a message reception timeout
        public static void checkExtractTimeout() {
            int timeout = config.getPreferenceI("EXTRACTTIMEOUT", 4);
            if (receivingMsg) {
                if (lastCharacterTime + (timeout * 1000) < System.currentTimeMillis()) {
                    RMsgProcessor.CrcString = "";
                    RMsgProcessor.FileNameString = "";
                    receivingMsg = false;
                    receivedSOH = false;
                    RMsgProcessor.PostToTerminal("\nAborting New RMsgUtil Reception due to timeout as set in preferences)!");
                }
            }
        }
    */
    //Called after a text message (but not before an image transmission to reset the mode to the selected one
    private static void resetToDefaultMode() {
        // Get last mode (if not set, returns -1)
        RMsgProcessor.TxModem = RMsgProcessor.RxModem = Modem.getModeFromSpinner();
        //If we do not have a last mode, this is the first time in the app (use safest default: MT63-1000-Short)
        if (RMsgProcessor.RxModem == -1)
            RMsgProcessor.RxModem = Modem.getMode("MT63_1000_ST");
        //Reset frequency and squelch
        reset();
        //Change to Selected mode
        changeCModem(RMsgProcessor.RxModem, frequency);

    }

    //Called regularly to check if we can switch off the processing by the C modems
    public static void checkListeningTimeout() {
        //int timeout = config.getPreferenceI("EXTRACTTIMEOUT", 4);
        if (!receivedSOH && !receivingMsg && !receivingPic) {
            if (System.currentTimeMillis() - startListeningTime > (60 * 1000)) {
                //Stop modem processing until next RSID
                processModemData = false;
            }
        }
    }

    //Check if we are idle (no transmitting, not received an RSID recently, not receiving a message, not waiting to ack a message)
    public static boolean modemIsIdle() {
        boolean idle;
        long mNow = System.currentTimeMillis();
        int DCD = 2200 * config.getPreferenceI("ACKPOSITION", 0);
        if (DCD < 2200) DCD = 2200;
        idle = (lastRsidTime == 0L || (mNow - lastRsidTime > 16000)) //16 sec elapsed since last RSID
                        && !(receivedSOH || receivingMsg || receivingPic) //Not receiving
                        //3 seconds between end of TX of text message and RSID for image TX
                        && (RMsgProcessor.lastMessageEndRxTime == 0L || (mNow - RMsgProcessor.lastMessageEndRxTime > DCD)) //Waiting the extra time
                        && (mNow > nextTimeToTx) //Waiting for max acks time
                        && RMsgTxList.getAvailableLength() == 0  //nothing to send
                        && !RMsgProcessor.TXActive; //Not transmitting
        return idle;
    }


    //Re-save the message to update the fields (like when a TXed message is acknowledged)
    private static void updateLastTxedMessageFile(RMsgObject myMessage) {

        String sentFolderPath = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + RMsgProcessor.DirSent + RMsgProcessor.Separator;
        //myMessage.fileName = sentFileNameString;
        //Update the list so that it displays the message filename correctly
        RMsgTxList.getYoungestSent(myMessage.fileName);
        File msgSentFile = new File(sentFolderPath + myMessage.fileName);
        if (msgSentFile.exists()) {
            msgSentFile.delete();
        }
        try {
            String stringToSave = myMessage.formatForStorage(false);
            FileWriter sentFile = null;
            sentFile = new FileWriter(msgSentFile, true);
            sentFile.write(stringToSave);
            sentFile.close();
        } catch (IOException e) {
            RadioMSG.middleToastText("Error Updating Message in Sent Folder " + e.toString());
        }

    }


    //Check if we are not receiving a message and we need to transmit
    //Allow 16 seconds between last RSID to make sure we capture the first SOH
    public static void checkNeedToTransmit() {

        long mNow = System.currentTimeMillis();
        //Allows other stations to pickup the acks and not overlap with the next transmission's RSID
        //Use Max ack position + 2 sec as an RSID + silence is 2 seconds long
        //int DCD = 2200 * config.getPreferenceI("ACKPOSITION", 0);
        long delayUntilMaxAcks = delayUntilMaxAcksHeard();
        if (    //16 Seconds between RSID and SOH for MFSK16 and slow OLIVIA modes
                (lastRsidTime == 0L || (mNow  > lastRsidTime + 16000))
                        && !(receivedSOH || receivingMsg || receivingPic || awaitingSoundAlarm)
                        //3 seconds between end of TX of text message and RSID for image TX
                        && (RMsgProcessor.lastMessageEndRxTime == 0L || (mNow > RMsgProcessor.lastMessageEndRxTime + delayUntilMaxAcks))
                        && (nextTimeToTx == 0L || mNow > nextTimeToTx) //We have waited for all the acks (maxAcks)
                        && RMsgTxList.getAvailableLength() > 0) {
            //We can transmit..Phew
            Modem.txData();
        }
    }


    //Init sound systems for TX and possibly Voice passthrough
    private static void txSoundInInit() {

        //Open and initialise the Output towards the Radio
        txBufferSize = 4 * android.media.AudioTrack.getMinBufferSize(8000,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT); //Android check the multiplier value for the buffer size
        if (RadioMSG.toBluetooth) {
            txAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, txBufferSize, AudioTrack.MODE_STREAM);
        } else {
            //Oppo ok, motto ok but no rsid heard
            //txAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, txBufferSize , AudioTrack.MODE_STREAM);
            //Oppo ok, motto ok but no rsid heard
            //txAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, txBufferSize , AudioTrack.MODE_STREAM);
            //Oppo ok, motto ok but no rsid heard
            txAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, txBufferSize, AudioTrack.MODE_STREAM);
        }

        //Start TX audio track
        txAudioTrack.setStereoVolume(1.0f, 1.0f);
        txAudioTrack.play();

        //Set requested volume AFTER we open the audio track as some devices (e.g. Oppo have two different volumes for when in or out of audio track
        AudioManager audioManager = (AudioManager) RadioMSG.myContext.getSystemService(Context.AUDIO_SERVICE);
        try {
            int maxVolume;
            int stream = RadioMSG.toBluetooth ? RadioMSG.STREAM_BLUETOOTH_SCO : AudioManager.STREAM_MUSIC;
            maxVolume = audioManager.getStreamMaxVolume(stream);
            int mediaVolume = config.getPreferenceI("MEDIAVOLUME", 100);
            if (mediaVolume < 5) mediaVolume = 5;
            if (mediaVolume > 100) mediaVolume = 100;
            maxVolume = maxVolume * mediaVolume / 100;
            audioManager.setStreamVolume(stream,
                    maxVolume, 0);  // 0 can also be changed to AudioManager.FLAG_PLAY_SOUND
        } catch (Exception e) {
            RadioMSG.middleToastText("Error Adjusting Volume");
        }

        //Initialise Rx side if we have a built-in mic to Radio transmission
        //Used when the user presses the "Talk" button in voice mode
        int rxBufferSize = (int) sampleRate; // 1 second of Audio max
        if (rxBufferSize < AudioRecord.getMinBufferSize((int) sampleRate, AudioFormat.CHANNEL_IN_MONO, myAudioFormat)) {
            // Check to make sure buffer size is not smaller than the smallest allowed one
            rxBufferSize = AudioRecord.getMinBufferSize((int) sampleRate, AudioFormat.CHANNEL_IN_MONO, myAudioFormat);
        }
        int ii = 20; //number of 1/4 seconds wait
        while (--ii > 0) {
            //Choose input from preferences
            int audioSource = config.getPreferenceI("AUDIOINPUT", 0);
            rxAudioRecorder = new AudioRecord(audioSource, 8000, android.media.AudioFormat.CHANNEL_IN_MONO,
                    myAudioFormat, rxBufferSize);
            if (rxAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                ii = 0;//ok done
            } else {
                if (ii < 16) { //Only if have to wait more than 1 seconds
                    loggingclass.writelog("Waiting for Audio MIC availability...", null, true);
                }
                try {
                    Thread.sleep(250);//1/4 second
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (rxAudioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            //Android add exception catch here
            loggingclass.writelog("Can't open Audio MIC \n", null, true);
        }
        //Initialise the variable for potential left or right channel PTT tone
        pttPhaseIncr = 2.0 * Math.PI * 1000.0f / 8000.0f; //Fixed 1,000Hz at 8,000 sample rate
        pttPhase = 0.0f;
    }


    //Release sound systems
    private static void txSoundRelease() {
        if (txAudioTrack != null) {

            //Wait for end of buffer to be emptied
            //try {
            //    Thread.sleep(1000 * txBufferSize / 8000);//wait buffer length time (milli sec).
            //} catch (InterruptedException e) {
            //Do nothing
            //}
            //Stop audio track
            txAudioTrack.stop();
            //debugging only
            //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'txAudioTrack.stop'");
            //Wait for end of audio play to avoid
            //overlaps between end of TX and start of RX
            while (txAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }
            }
            //debugging only
            //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'waiting for end of playing state'");
            //Android debug add a fixed delay to avoid cutting off the tail end of the modulation
            //try {
            //    Thread.sleep(500);
            //} catch (InterruptedException e) {
            //e.printStackTrace();
            //}

            txAudioTrack.release();
        }
        //Release Audio in recorder
        if (rxAudioRecorder != null) {
            //Avoid some crashes on wrong state
            if (rxAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                if (rxAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    rxAudioRecorder.stop();
                }
                rxAudioRecorder.release();
            }
        }
    }

    //In a separate thread so that the UI thread is not blocked during TX
    public static void txData() {
        Runnable TxRun = new TxThread();
        new Thread(TxRun).start();
    }


    public static class TxThread implements Runnable {
        int numSamples8K = 0;
        short[] so8K = new short[rxBufferSize];


        public void run() {


            //Reset the stop flag if it was ON
            stopTX = false;
            //Set flags to TXing
            RMsgProcessor.TXActive = true;
            resetTxProgressPercent();
            setStatusSending();

            //Wait for the receiving side to be fully stopped???
            //To-Do: review logic here: while (modemState != RXMODEMPAUSED) {
            //try {
            //    Thread.sleep(500);
            //} catch (InterruptedException e) {
            //    //e.printStackTrace();
            //}
            //Wait for Modem tread to get to paused state (releasing audio streams first)
            while (modemState != RXMODEMPAUSED) {
                //Moved inside loop to enforce the flag value
                //Stop the modem receiving side
                pauseRxModem();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    //e1.printStackTrace();
                }
            }
            //Ptt ON
            setPtt();
            //Init sound system (including potential mic to radio)
            txSoundInInit();

            //Initalise the C++ modem TX side
            frequency = config.getPreferenceD("AFREQUENCY", 1500.0f);
            if (frequency < 500) frequency = 500;
            if (frequency > 2500) frequency = 2500;
            //Only one at a time, return to listening during acks in case another station sends a message
            if (RMsgTxList.getAvailableLength() > 0 || microphoneToRadio) {

                /*
                //Mic Audio to Radio
                if (microphoneToRadio) {
                    try {
                        //Save Environment for this thread so it can call back
                        // Java methods while in C++
                        saveEnv();
                        //Init current modem for Tx
                        txInit(frequency);
                        //Send VOICE-ON RSID to open the speaker link on the receiving end
                        txThisRSID(56);
                        //Start recorder
                        rxAudioRecorder.startRecording();
                        //Play loop
                        while (microphoneToRadio) {
                            //read audio track from microphone
                            //process only part of the buffer to avoid lumpy processing
                            numSamples8K = rxAudioRecorder.read(so8K, 0, 8000 / 8);
                            //Send audio to radio
                            if (numSamples8K > 0) {
                                //txAudioTrack.write(so8K, 0, numSamples8K);
                                writeToAudiotrack(so8K, 0, numSamples8K);
                            } else {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    //e.printStackTrace();
                                }
                            }
                        }
                        //Send VOICE-OFF RSID to close the speaker link on the receiving end
                        //Maybe not that useful. Let the user decide when to close the radio sound.
                        //txThisRSID(107);
                    } catch (Exception e) {
                        loggingclass.writelog("Can't output sound. Is Sound device busy?", null, true);
                    }
                } else
                    */
                if (RMsgTxList.getAvailableLength() > 0) { //Double check that we have an element to transmit
                    //Retrieve last element of the list (oldest one) and mark as sent
                    RMsgObject myMessage = RMsgTxList.getOldest();
                    //Double check that we do really have a valid message
                    if (myMessage != null) {
                        //Re-create modem in case it was changed by RSID
                        //Get current op-mode
                        String opMode = config.getPreferenceS("LASTMODEUSED", "HF-Poor");
                        String thisMode;
                        String formattedMsg = "";
                        String stringToSave = "";
                        //If in Hf-Clubs, stay on CCIR476 regardless of the request mode
                        if (myMessage.rxtxMode.equals("CCIR493")) { //Selcall
                            thisMode = myMessage.rxtxMode;
                        } else if (opMode.equals("HF-Clubs")) {
                            thisMode = "CCIR476";
                        } else {
                            //Message contains a requested reply mode?
                            if (myMessage.rxtxMode.equals("")) { //No specified mode
                                //Translate to selected modem for this op-mode
                                thisMode = config.getPreferenceS(opMode, "OLIVIA_8_500");
                            } else {
                                thisMode = myMessage.rxtxMode;
                            }
                        }
                        //Save it in the message object if not already there
                        if (myMessage.rxtxMode.equals("")) {
                            myMessage.rxtxMode = thisMode;
                        }
                        //Reset acknowledged flag in case of a forwarded message
                        myMessage.wasAcknowledged = false;
                        String txSendline = "";
                        byte[] bytesToSend = null;
                        if (thisMode.equals("CCIR493")) {
                            TxCCIR493 myTxCCIR493 = new TxCCIR493();
                            try {
                                bytesToSend = myMessage.sms.getBytes("ASCII");
                            } catch (Exception e) { //Invalid ASCII characters
                                bytesToSend[0] = 0;//Null character
                            }
                            //HF-Clubs = long preamble
                            //myTxCCIR493.AddBytes(bytesToSend);
                            //Correct bug that replies in CCIR476 would result in no transmission
                            // } else if (RMsgProcessor.RxModem == getMode("CCIR476")) {
                        } else if (thisMode.equals("CCIR476")) {
                            //Save modem code
                            RMsgProcessor.RxModem = RMsgProcessor.TxModem = Modem.getMode(thisMode);
                            //Save date-time of start of sending in UTC timezone
                            Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            RMsgProcessor.FileNameString = String.format(Locale.US, "%04d", c1.get(Calendar.YEAR)) + "-"
                                    + String.format(Locale.US, "%02d", c1.get(Calendar.MONTH) + 1) + "-"
                                    + String.format(Locale.US, "%02d", c1.get(Calendar.DAY_OF_MONTH)) + "_"
                                    + String.format(Locale.US, "%02d%02d%02d", c1.get(Calendar.HOUR_OF_DAY),
                                    c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND)) + ".txt";
                            //Format, converting all to lower case
                            formattedMsg = myMessage.formatForTx(true); //All lowercase
                            stringToSave = myMessage.formatForStorage(true);
                            //Enclose in newlines
                            txSendline = "\n\n" + formattedMsg + "\n\n";
                            try {
                                bytesToSend = txSendline.getBytes("UTF-8");
                            } catch (Exception e) { //Invalid UTF-8 characters
                                bytesToSend[0] = 0;//Null character
                            }
                            //Display in Modem screen
                            appendToModemBuffer("\n**TX:<SOH>"
                                    + formattedMsg.replaceFirst(Character.toString(SOH), "").replaceFirst(Character.toString(EOT), "")
                                    + "<EOT>**\n");
                            //Create Java modem
                            TxCCIR476 myTxCCIR476 = new TxCCIR476();
                            //HF-Clubs = long preamble. Short otherwise.
                            //myTxCCIR476.AddBytes(bytesToSend, opMode.equals("HF-Clubs"));
                        } else {
                            //Save current mode in case we change it
                            int currentMode = RMsgProcessor.RxModem = RMsgProcessor.TxModem = Modem.getMode(thisMode);
                            //Save date-time of start of sending in UTC timezone
                            Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            RMsgProcessor.FileNameString = String.format(Locale.US, "%04d", c1.get(Calendar.YEAR)) + "-"
                                    + String.format(Locale.US, "%02d", c1.get(Calendar.MONTH) + 1) + "-"
                                    + String.format(Locale.US, "%02d", c1.get(Calendar.DAY_OF_MONTH)) + "_"
                                    + String.format(Locale.US, "%02d%02d%02d", c1.get(Calendar.HOUR_OF_DAY),
                                    c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND)) + ".txt";
                            //Format for Tx
                            formattedMsg = myMessage.formatForTx(false); //No change to lowercase
                            if (thisMode.startsWith("OLIVIA") || thisMode.startsWith("MT63")) {
                                //Exchange SOH and EOT for DC1 and DC3 for Olivia and MT-63 modes
                                formattedMsg = formattedMsg.replace(SOH, DC1).replace(EOT, DC3);
                            }
                            //Mode to be saved in file as well
                            myMessage.rxtxMode = thisMode;
                            stringToSave = myMessage.formatForStorage(false);
                            //Enclose in new lines
                            txSendline = "\n\n" + formattedMsg + "\n\n";
                            try {
                                bytesToSend = txSendline.getBytes("UTF-8");
                            } catch (Exception e) { //Invalid UTF-8 characters
                                bytesToSend[0] = 0;//Null character
                            }
                            //Display in Modem screen
                            appendToModemBuffer("\n**TX:<SOH>"
                                    + formattedMsg.replaceFirst(Character.toString(SOH), "").replaceFirst(Character.toString(EOT), "").replaceFirst(Character.toString(DC1), "").replaceFirst(Character.toString(DC3), "")
                                    + "<EOT>**\n");
                            //Create Java modem
                            // Save Environment for this thread so it can call back Java methods while in C++
                            saveEnv();
                            //Create modem
                            String modemCreateResult = createCModem(RMsgProcessor.RxModem);
                            if (modemCreateResult.contains("ERROR")) {
                                CModemInitialized = false;
                            } else {
                                CModemInitialized = true;
                                double centerfreq = config.getPreferenceI("AFREQUENCY", 1500);
                                //Limit it's values too
                                if (centerfreq > 2500) centerfreq = 2500;
                                if (centerfreq < 500) centerfreq = 500;
                                String modemInitResult = initCModem(centerfreq);
                            }
                            //Do we have a pre-data tone to transmit?
                            int preToneDuration = config.getPreferenceI("PRETONEDURATION", 0);
                            //Check valid?
                            preToneDuration = (preToneDuration > 10000 ? 10000 : preToneDuration);
                            if (preToneDuration > 0) {
                                generateSingleTone(preToneDuration, false);
                            }
                            //Init current modem for Tx
                            txInit(frequency);
                            if (txRsidOn) {
                                txRSID();
                            }
                            txCProcess(bytesToSend, bytesToSend.length);
                            //Is there a picture/signature to send after the text?
                            if (myMessage.picture != null && myMessage.pictureTxSPP > 0) {
                                resetTxProgressPercent();
                                //Change to the selected MFSK mode
                                int modemCode = getMode("MFSK32");
                                if (myMessage.pictureTxModemIndex > 0) {
                                    modemCode = getMode(modemCapListString[myMessage.pictureTxModemIndex]);
                                }
                                //Bug fix: receiving end does not change to image mode in time
                                try {
                                    Thread.sleep(1); //3000
                                } catch (InterruptedException e) {
                                    //nothing
                                }
                                //Change to MFSK Image modem
                                changeCModem(modemCode, frequency);
                                //debugging only
                                //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'changeCModem' with modem # " + modemCode);
                                if (myMessage.picture.getWidth() > 0 &&
                                        myMessage.picture.getHeight() > 0) {
                                    setStatusSendingPic();
                                    //
                                    txInit(frequency);
                                    //debugging only
                                    //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'txInit' at frequency: " + frequency);
                                    //Always send RSID
                                    txRSID();
                                    //debugging only
                                    //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'txRSID' ");
                                    //Send picture
                                    txPicture(RMsgUtil.extractPictureArray(myMessage.picture, myMessage.scramblingMode), myMessage.picture.getWidth(),
                                            myMessage.picture.getHeight(), myMessage.pictureTxSPP, (myMessage.pictureColour ? 1 : 0));
                                    //debugging only
                                    //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'txPicture' ");
                                    //Release the (large) Byte array for GC
                                    //myMessage = null;
                                }
                                //Change back to the previous mode (for post-Tx-RSID purposes)
                                //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "About to execute 'changeCModem' with modem # " + currentMode);
                                changeCModem(currentMode, frequency);
                                //debugging only
                                //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'changeCModem' with modem # " + currentMode);
                            }
                            //Send TX RSID if required
                            //Check and send post-transmission RSID
                            if (txRsidOn && config.getPreferenceB("TXPOSTRSID", false)) {
                                //Replaced with direct specification of modem
                                txRSID();
                                //debugging only
                                //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done post 'txRSID'");
                            }
                        }
                        //Save message into Sent folder if tx is not aborted and is not selcall
                        if (!stopTX && !myMessage.rxtxMode.equals("CCIR493")) {
                            //Update time when finished TX (used to associate an RSID ACK to a TXed message
                            RMsgProcessor.lastMessageEndTxTime = System.currentTimeMillis();
                            //Calendar c1 = Calendar.getInstance(TimeZone.getDefault());
                            //Adjust time so that is is accurate if using GPS time
                            //c1.add(Calendar.SECOND, RadioMSG.deviceToRealTimeCorrection);
                            //String sentFileNameString = String.format(Locale.US, "%04d", c1.get(Calendar.YEAR)) + "-"
                            //        + String.format(Locale.US, "%02d", c1.get(Calendar.MONTH) + 1) + "-"
                            //        + String.format(Locale.US, "%02d", c1.get(Calendar.DAY_OF_MONTH)) + "_"
                            //        + String.format(Locale.US, "%02d%02d%02d", c1.get(Calendar.HOUR_OF_DAY),
                            //                c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND)) + ".txt";
                            //Uses the timestamp at start of TX as it is a better match for duplicate message detection
                            String sentFileNameString = RMsgProcessor.FileNameString;
                            //Save the text part of the message
                            String sentFolderPath = RMsgProcessor.HomePath +
                                    RMsgProcessor.Dirprefix + RMsgProcessor.DirSent + RMsgProcessor.Separator;
                            myMessage.fileName = sentFileNameString;
                            //Update the list so that it displays the message filename correctly
                            RMsgTxList.getYoungestSent(sentFileNameString);
                            File msgSentFile = new File(sentFolderPath + sentFileNameString);
                            if (msgSentFile.exists()) {
                                msgSentFile.delete();
                            }
                            try {
                                FileWriter sentFile = null;
                                sentFile = new FileWriter(msgSentFile, true);
                                sentFile.write(stringToSave);
                                sentFile.close();
                            } catch (IOException e) {
                                RadioMSG.middleToastText("Error Saving Message in Sent Folder " + e.toString());
                            }
                            //Save the binary part of the message if any
                            if (myMessage.picture != null) {
                                try {
                                    //In any case, save the image file for further usage if required
                                    //fileName = RMsgUtil.dateTimeStamp() + ".png";
                                    String sentPicFileNameString = sentFileNameString.replace(".txt", ".png");
                                    String ImageFilePath = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + RMsgProcessor.DirImages +
                                            RMsgProcessor.Separator;
                                    File dest = new File(ImageFilePath + sentPicFileNameString);
                                    FileOutputStream out = new FileOutputStream(dest);
                                    myMessage.picture.compress(Bitmap.CompressFormat.PNG, 100, out);
                                    out.flush();
                                    out.close();
                                    //Make it available in the System Picture Gallery
                                    ContentValues values = new ContentValues();
                                    values.put(Images.Media.TITLE, sentPicFileNameString);
                                    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
                                    values.put(Images.ImageColumns.BUCKET_ID, dest.toString().toLowerCase(Locale.US).hashCode());
                                    values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, dest.getName().toLowerCase(Locale.US));
                                    values.put("_data", dest.getAbsolutePath());
                                    values.put(Images.Media.DESCRIPTION, "RadioMsg Image");
                                    ContentResolver cr = RadioMSG.myContext.getContentResolver();
                                    cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                } catch (Exception e) {
                                    loggingclass.writelog("Exception Error in 'txData' " + e.getMessage(), null, true);
                                }
                            }
                            //Set the earliest time to transmit again, after we have heard all the acks (maxAcks)
                            nextTimeToTx = delayUntilMaxAcksHeard() + System.currentTimeMillis();
                            //Are we displaying the list of Sent Messages?
                            int whichFolders = config.getPreferenceI("WHICHFOLDERS", 3);
                            if (whichFolders == 2 || whichFolders == 3) {
                                //Add to listadapter
                                //final RMsgObject txFinalMessage = RMsgObject.extractMsgObjectFromFile(RMsgProcessor.DirSent, sentFileNameString, false); //Text part only
                                RadioMSG.myInstance.runOnUiThread(new Runnable() {
                                    public void run() {
                                        RMsgObject txedMessage = RMsgTxList.removeYoungestSent(); //Must be the one just sent
                                        txedMessage.crcValid = true;
                                        RadioMSG.msgDisplayList.addNewItem(txedMessage, true); //Is my own message
                                        //VK2ETA test debug changing filename of received SMS
                                        //RadioMSG.msgDisplayList.notifyDataSetChanged();
                                        //update the list if we are on that screen
                                        RadioMSG.mHandler.post(RadioMSG.updateList);
                                    }
                                });
                                //Moved in ui thread, just above
                                //RadioMSG.mHandler.post(RadioMSG.updateList);
                            }
                        } else {
                            //Could be cancelled mid-send or be a CCIR493 selcall that we don't store
                            RMsgTxList.removeYoungestSent(); //Otherwise it stays in the queue as sent message
                            if (thisMode.equals("CCIR493")) {
                                nextTimeToTx = System.currentTimeMillis() + 4400; //4 seconds from start to end of revertive tone
                            } else {
                                //Blank it otherwise we may wait forever
                                nextTimeToTx = 0L;
                            }
                        }
                    }
                }
            }
            //Release sound systems
            txSoundRelease();
            //Ptt OFF
            resetPtt();
            //Restart modem reception
            unPauseRxModem();
            //debugging only
            //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'unPauseRxModem'");
            RMsgProcessor.TXActive = false;
            //Open Speaker audio for 10 seconds if selected
            if (RadioMSG.speakerOption == 2) {
                setSpeakerOn(10000L); //10 seconds delay. Speaker off processed in RadioMsg.regularActions().
            }
            //Update title
            setStatusListening();
        }
    }
    
    //Opens the speaker, no auto close.
    public static void setSpeakerOn() {

        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P){
            Modem.speakerOn = true;
        } else {
            //Modem.speakerOn = false;
            Modem.speakerOn = true;
            //setSpeakerPhone(true);
            setForceSpeakerUse(true);
        }
        RadioMSG.speakerOffDelay = 0L;
    }

    //Opens the speaker. If offDelay is NOT zero, close it after that many milliseconds
    public static void setSpeakerOn(long offDelay) {
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P){
            Modem.speakerOn = true;
            RadioMSG.speakerOffDelay = System.currentTimeMillis() + offDelay;
        } else {
            //Modem.speakerOn = false;
            Modem.speakerOn = true;
            //setSpeakerPhone(true);
            setForceSpeakerUse(true);
            RadioMSG.speakerOffDelay = System.currentTimeMillis() + offDelay;
        }
    }

    //Closes the speaker
    public static void setSpeakerOff() {

        Modem.speakerOn = false;
        RadioMSG.speakerOffDelay = 0L;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            //setSpeakerPhone(false);
            setForceSpeakerUse(false);
        }
    }

    //Connect to USB Serial device if present
    private static void connectUsbDevice() {

        RadioMSG.myInstance.runOnUiThread(new Runnable() {
            public void run() {
                //Open the USB port for CAT control
                RadioMSG.myInstance.connectUsbDevice();
            }
        });

    }


    private static byte[] fromHexString(String src) {
        byte[] biBytes = new BigInteger("10" + src.replaceAll("\\s", ""), 16).toByteArray();
        return Arrays.copyOfRange(biBytes, 1, biBytes.length);
    }


    //Send the PTT command and tries to recover if an error is detected
    private static void sendPttCommand(boolean PttOnOff) {

        boolean pttViaRTS = config.getPreferenceB("RTSASPTT", false);
        boolean pttViaDTR = config.getPreferenceB("DTRASPTT", false);
        boolean pttViaCAT = config.getPreferenceB("CATASPTT", false);
        String switchStr = PttOnOff ? "ON" : "OFF";
        try {
            if (pttViaRTS) {
                RadioMSG.usbSerialPort.setRTS(PttOnOff);
            }
            if (pttViaDTR) {
                RadioMSG.usbSerialPort.setDTR(PttOnOff);
            }
            if (pttViaCAT) {
                //Send Cat PTT Command
                int transceiver = config.getPreferenceI("TRANSCEIVER", 0);
                byte[] txRxCommand;
                switch (transceiver) {
                    case 1:
                        String txRxCommandStr = PttOnOff ? "TX;" : "RX;";
                        txRxCommand = txRxCommandStr.getBytes("ASCII");
                        break;
                    case 2:
                        if (PttOnOff) {
                            txRxCommand = fromHexString("00 00 00 00 08");
                        } else {
                            txRxCommand = fromHexString("00 00 00 00 88");
                        }
                        break;
                    default:
                        txRxCommand = null;
                }
                if (txRxCommand != null) {
                    try {
                        RadioMSG.usbSerialPort.write(txRxCommand, 200);
                        RadioMSG.topToastText("CAT PTT " + switchStr + " Sent");
                    } catch (IOException e) {
                        RadioMSG.topToastText("ERROR while writing command to TRx");
                    }
                } else {
                    RadioMSG.topToastText("CAT via PTT Requested But No Transceiver Selected");
                }

            }
            //Debug  Processor.PostToModem("PTT command " + switchStr + " sent ok");
        } catch (IOException e) {
            //debug appendToModemBuffer("IO Exception in PTT " + switchStr + " : " + e.getMessage().toString());
            //Try to re-connect just in time
            connectUsbDevice();
            try {
                if (pttViaRTS) {
                    RadioMSG.usbSerialPort.setRTS(PttOnOff);
                }
                if (pttViaDTR) {
                    RadioMSG.usbSerialPort.setDTR(PttOnOff);
                }
                if (pttViaCAT) {
                    //Send Cat PTT Command
                    String txRxCommand = PttOnOff ? "TX;" : "RX;";
                    RadioMSG.usbSerialPort.write(txRxCommand.getBytes("ASCII"), 200);
                    RadioMSG.topToastText("CAT PTT " + txRxCommand + " Sent");
                }
                //debug appendToModemBuffer("PTT command " + switchStr + " sent ok");
            } catch (IOException e1) {
                RadioMSG.topToastText("Error sending CAT PTT command");
                //debug appendToModemBuffer("2nd IO Exception in PTT " + switchStr+ " : " + e1.getMessage().toString());
                //give up
                // Try to re-connect
                //connectUsbDevice();
            }

        }

    }


    private static void setPtt() {

        //Rig control for PTT (RTS/DTR/CAT Command)
        boolean pttViaRTS = config.getPreferenceB("RTSASPTT", false);
        boolean pttViaDTR = config.getPreferenceB("DTRASPTT", false);
        boolean pttViaCAT = config.getPreferenceB("CATASPTT", false);
        if (pttViaRTS | pttViaDTR | pttViaCAT) {
            //Send PTT ON command if possible
            if (RadioMSG.usbSerialPort != null && RadioMSG.usbSerialPort.isOpen()) {
                //debug appendToModemBuffer("Request PTT ON");
                sendPttCommand(true);
            }
            //Delay Audio for required period
            int audioSendDelay = config.getPreferenceI("AUDIODELAYAFTERPTT", 0);
            //Max 5 seconds
            if (audioSendDelay > 5000) {
                audioSendDelay = 5000;
            }
            if (audioSendDelay > 0) {
                try {
                    Thread.sleep(audioSendDelay);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    }


    private static void resetPtt() {

        //Rig control for PTT (RTS/DTR/CAT Command)
        boolean pttViaRTS = config.getPreferenceB("RTSASPTT", false);
        boolean pttViaDTR = config.getPreferenceB("DTRASPTT", false);
        boolean pttViaCAT = config.getPreferenceB("CATASPTT", false);
        //Release PTT
        if (pttViaRTS | pttViaDTR | pttViaCAT) {
            //Delay PTT Release
            int pttReleaseDelay = config.getPreferenceI("PTTDELAYAFTERAUDIO", 0);
            //Max 5 seconds
            if (pttReleaseDelay > 5000) {
                pttReleaseDelay = 5000;
            }
            if (pttReleaseDelay > 0) {
                try {
                    Thread.sleep(pttReleaseDelay);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
            //Send PTT OFF command if possible
            if (RadioMSG.usbSerialPort != null && RadioMSG.usbSerialPort.isOpen()) {
                //debug appendToModemBuffer("Request PTT OFF");
                sendPttCommand(false);
            }
        }
    }



    //Acknowledgement code for 1 to 8 stations
    private static final int DIT = 100; //in milli-seconds
    private static final int DAH = 400;
    private static final int SPACE = -100; //Negative means silence of that duration
    private static final int[][] ackArray = {
            {SPACE, SPACE, SPACE}, //Position zero is not used
            {DIT, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE},
            {DIT, SPACE, DIT, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE},
            {DIT, SPACE, DIT, SPACE, DIT, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE},
            {DAH, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE},
            {DAH, SPACE, DIT, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE},
            {DAH, SPACE, DIT, SPACE, DIT, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE},
            {DAH, SPACE, DAH, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE},
            {DAH, SPACE, DAH, SPACE, DIT, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE, SPACE}
    };


    //Generates a sequence of Morse-like DITs and DAHs.
    private static void generateDitDahSequence(int ackPosition, boolean errorTone) {
        int sr = 8000;
        int maxSymlen = (int) (DAH * sr) / 1000; //Longest symbol in samples
        short[] outbuf = new short[maxSymlen];

        double phaseincr;
        double phase = 0.0;
        //Make difference between positive and negative acknowledgment more evident
        //phaseincr = 2.0 * Math.PI * (errorTone ? 600 : frequency) / sr;
        phaseincr = 2.0 * Math.PI * (errorTone ? 500 : 2000) / sr;

        int volumebits = config.getPreferenceI("VOLUME", 8);
        //Check we are within the bounds of the array
        if (ackPosition >= ackArray.length) {
            return;
        }
        //Now send the ack
        for (int i = 0; i <= ackPosition; i++) {
            for (int seq = 0; seq < ackArray[i].length; seq++) {
                int symbol = ackArray[i][seq];
                int symbolLength = (abs(symbol) * sr) / 1000;
                for (int j = 0; j < symbolLength; j++) {
                    phase += phaseincr;
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI;
                    //Send silence before we get to the ack position to let the other stations reply in turn
                    if (symbol < 0 || i < ackPosition) {
                        outbuf[j] = 0;
                    } else {
                        outbuf[j] = (short) ((int) (Math.sin(phase) * 8386560) >> volumebits);
                    }
                }
                //txAudioTrack.write(outbuf, 0, symbolLength);
                writeToAudiotrack(outbuf, 0, symbolLength);
            }
        }
    }


    //Set the delay before the next TX the duration of Preference MAXACKS to allow for all the acks to take place
    private static long delayUntilMaxAcksHeard() {

        int maxAcks = config.getPreferenceI("MAXACKS", 0);
        //Check we are within the bounds of the array
        if (maxAcks >= ackArray.length) {
            maxAcks = ackArray.length;
        }
        //Now calculate total accumulated time to maxAcks
        long delayToTx = 0L;
        int symbol;
        for (int i = 0; i <= maxAcks; i++) {
            for (int seq = 0; seq < ackArray[i].length; seq++) {
                symbol = ackArray[i][seq];
                delayToTx += abs(symbol);
            }
        }
        return delayToTx;
    }


    //Generates a tone of X milliseconds
    //Frequency is taken from static variable "frequency"
    private static void generateSingleTone(int duration, boolean forTune) {
        int sr = 8000;
        int symlen = (int) (1 * sr / 100); //10 milliseconds increments
        short[] outbuf = new short[symlen];

        double phaseincr;
        double phase = 0.0;
        phaseincr = 2.0 * Math.PI * frequency / sr;

        int volumebits = config.getPreferenceI("VOLUME", 8);

        //Round up to next 10 milliseconds increment
        duration = (duration + 9) / 10;

        for (int i = 0; i < duration; i++) {
            for (int j = 0; j < symlen; j++) {
                phase += phaseincr;
                if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI;
                outbuf[j] = (short) ((int) (Math.sin(phase) * 8386560) >> volumebits);
            }
            if ((forTune && tune) || (!forTune && !stopTX)) {
                //txAudioTrack.write(outbuf, 0, symlen);
                writeToAudiotrack(outbuf, 0, symlen);
            } else {
                //Terminate loop
                i = duration;
            }
        }
    }


    //Send Tune in a separate thread so that the UI thread is not blocked
    //  during TX
    //ackPosition is either zero for a normal tune otherwise is a reply position to
    //   encode a sequence of dit and dah as a message acknowledgment
    public static void sendToneSequence(int ackPosition, String rxMode, boolean errorTone, boolean useRSID) {
        Runnable TxTuneRun = new TxTuneThread(ackPosition, rxMode, errorTone, useRSID);
        new Thread(TxTuneRun).start();
    }

    private static class TxTuneThread implements Runnable {
        private AudioTrack at = null;
        private int myAckPosition = 0;
        private String myRxMode = "";
        private boolean myErrorTone = false;
        private boolean myUseRSID = false;

        public TxTuneThread(int myAckPosition, String myRxMode, boolean errorTone, boolean useRSID) {
            this.myAckPosition = myAckPosition;
            this.myRxMode = myRxMode;
            this.myErrorTone = errorTone;
            this.myUseRSID = useRSID;
        }

        public void run() {

            //Let the regularActionsRunner title update to show we are just tuning, not transmitting a document
            modemIsTuning = true;
            RMsgProcessor.TXActive = true;

            //Stop the modem receiving side
            pauseRxModem();

            //Wait 1 second so that if there is potential RF feedback
            //  on the touchscreen we do not start TXing while the
            //  finger is still on the screen
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            //Wait for Modem tread to get to paused state (releasing audio streams first)
            while (modemState != RXMODEMPAUSED) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    //e1.printStackTrace();
                }
            }
            RMsgProcessor.TXActive = true;
            RadioMSG.mHandler.post(RadioMSG.updatetitle);
            //Use default frequency for the tone
            double centerfreq = config.getPreferenceI("AFREQUENCY", 1500);
            //Limit it's values too
            if (centerfreq > 2500) centerfreq = 2500;
            if (centerfreq < 500) centerfreq = 500;
            //How long is the tune?
            int tuneLength = config.getPreferenceI("TUNEDURATION", 4);
            //Check valid?
            tuneLength = (tuneLength > 60 ? 60 : tuneLength);
            //Value of -1 means toggle with a max of 10 minutes tune
            tuneLength = (tuneLength == -1 ? 600 : tuneLength);
            tuneLength = (tuneLength < 0 ? 0 : tuneLength);
            //Value of 0 means toggle so set to max of 60 seconds tune
            tuneLength = (tuneLength == 0 ? 60 : tuneLength);
            //Ptt ON
            setPtt();
            //Init sound system (including potential mic to radio)
            txSoundInInit();
            tune = true;
            if (myAckPosition == 0) {
                generateSingleTone(tuneLength * 1000, true);
            } else if (myUseRSID && !myErrorTone) { //send RSID ack if positive ack only
                //Wait 1 second to ensure all audio is finished
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                // Java methods while in C++
                saveEnv();
                txInit(frequency);
                txThisRSID(263);
            } else if (myRxMode.equals("CCIR493") || myRxMode.equals("CCIR476")) {
                //generateSelcallAck();
            } else {
                generateDitDahSequence(myAckPosition, myErrorTone);
            }
            txSoundRelease();
            //Ptt OFF
            resetPtt();
            RMsgProcessor.TXActive = false;
            //Restart the modem receiving side
            unPauseRxModem();
            tune = false;
            modemIsTuning = false;
            RMsgProcessor.TXActive = false;
            RadioMSG.mHandler.post(RadioMSG.updatetitle);
        }
    }


    private static void addAckRxFrom(String ackString) {

    }

/*
    //Is the modem currently on CCIR493?
    public static boolean isCCIR493() {
        return RMsgProcessor.RxModem == Modem.getMode("CCIR493");
    }
*/

    //Is the modem currently on CCIR476?
    public static boolean isCCIR476() {
        return RMsgProcessor.RxModem == Modem.getMode("CCIR476");
    }

}
