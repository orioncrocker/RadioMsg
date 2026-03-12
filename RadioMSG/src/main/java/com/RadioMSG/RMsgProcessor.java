/*
 * RMsgProcessor.java
 *
 * Copyright (C) 2011 John Douyere (VK2ETA)
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.io.*;
import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.telephony.SmsManager;
import java.util.*;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.MailSSLSocketFactory;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;
import androidx.core.content.ContextCompat;

/**
 * @author John Douyere (VK2ETA)
 */
public class RMsgProcessor extends Service {

    static String application = "RadioMSG 2.3.0.3";
    static String version = "Version 2.3.0.3, 2025-04-10";

    static boolean onWindows = true;
    static String ModemPreamble = "";  // String to send before any Tx Buffer
    static String ModemPostamble = ""; // String to send after any Tx buffer
    static String HomePath = "";
    static String Dirprefix = "";
    static final String DirInbox = "Inbox";
    static final String DirArchive = "Archive";
    static final String DirSent = "Sent";
    static final String DirTemp = "Temp";
    static final String DirLogs = "Logs";
    static final String DirImages = "RadioMsg-Images";
    static final String DirRecordings = "Recordings";
    static final String messageLogFile = "messagelog.txt";
    static String Separator = "";
    static String CrcString = "";
    static String FileNameString = "";
    static boolean TXActive = false;
    //File name of last message received for appending a newly received picture
    static public String lastReceivedMessageFname = "";
    static public long lastMessageEndRxTime = 0;
    static public long lastMessageEndTxTime = 0;
    //Less than 20 seconds between the end of the text message and
    //	the start of mfsk picture transmission
    static boolean pictureRxInTime = false;
    public static RMsgObject lastTextMessage = null;

    //Stop gap measure: init as first modem in list
    static int TxModem = Modem.customModeListInt[0];
    static int RxModem = Modem.customModeListInt[0];

    static int imageTxModemIndex = 0;


    //Semaphores to instruct the RxTx Thread to start or stop
    public static Semaphore restartRxModem = new Semaphore(1, false);

    // globals to pass info to gui windows
    static String monitor = "";
    static String TXmonitor = "";
    static String TermWindow = "";
    static String status = "Listening";
    static int cpuload;

    // Error handling and logging object
    static loggingclass log;


    public static void processor() {
        //Nothing as this is a service
    }

    @Override
    public void onCreate() {

        // Create error handling class
        log = new loggingclass("RadioMSG");

        // Get settings and initialize
        handleinitialization();

        //Initialize Modem (creates the various type of modem objects)
        Modem.ModemInit();

        //Check that we have a current mode, otherwise take the first one in the list (useful when we have a NIL list of custom modes)
        RMsgProcessor.RxModem = RMsgProcessor.TxModem = Modem.customModeListInt[Modem.getModeIndex(RMsgProcessor.RxModem)];

        //Set the image modes defaults and limits
        imageTxModemIndex = Modem.getModeIndexFullList(Modem.getMode("MFSK64"));
        Modem.minImageModeIndex = Modem.getModeIndexFullList(Modem.getMode("MFSK16"));
        Modem.maxImageModeIndex = Modem.getModeIndexFullList(Modem.getMode("MFSK128"));

        //Reset frequency and squelch
        Modem.reset();

        //Make sure the display strings are blank
        //RMsgProcessor.monitor = "";
        RMsgProcessor.TXmonitor = "";
        RMsgProcessor.TermWindow = "";
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the RxThread
        Modem.startmodem();

        //Make sure Android keeps this running even if resources are limited
        //Display the notification in the system bar at the top at the same time
        startForeground(1, RadioMSG.myNotification);

        // Keep this service running until it is explicitly stopped, so use sticky.
        //VK2ETA To-DO: Check if START_STICKY causes the service restart on ACRA report
        //VK2ETA as a server we want it restart if shutdown by the system return START_NOT_STICKY;
        //Sticky is not working as it restarts with the same error, needs to have a fresh start (exit the process and then relaunch the app)
        //return START_STICKY;
        return START_NOT_STICKY;

    }


    @Override
    public void onDestroy() {
        // Kill the Rx Modem thread
        Modem.stopRxModem();
    }


    //Post to main terminal window
    public static void PostToTerminal(String text) {
        RMsgProcessor.TermWindow += text;
        RadioMSG.mHandler.post(RadioMSG.addtoterminal);
    }


    //Post to main terminal window
    //public static void PostToModem(String text) {
    //    RMsgProcessor.monitor += text;
    //    RadioMSG.mHandler.post(RadioMSG.updateModemScreen);
    //}


    //Check if is hf-Clubs
    public static boolean opModeIsHfClubs() {

        return config.getPreferenceS("LASTMODEUSED", "HF-Poor").equals("HF-Clubs");
    }


    //return call to be used in From and Relay info
    public static String getCall() {

        //Changed so that only CCIR493 uses Selcall number as "From"
        //return (opModeIsHfClubs() ? config.getPreferenceS("SELCALL").trim() : config.getPreferenceS("CALL")).trim();
        return config.getPreferenceS("CALL").trim();
        //Keep using call only for now
        //return config.getPreferenceS("CALL");
    }


    //Check if we have a match (strict or with ALL allowed) to our call or selcallview
    public static boolean matchMyCallWith(String call, boolean allowALL) {

        boolean mMatch1 = (allowALL && call.equals("9999")) || call.equals(config.getPreferenceS("SELCALL", "9999").trim());
        boolean mMatch2 = (allowALL && call.equals("*"))
                || call.toLowerCase(Locale.US).equals(config.getPreferenceS("CALL", "NoCall").trim().toLowerCase(Locale.US));

        return mMatch1 || mMatch2;
    }


    //Check if we have a match (strict or with ALL allowed) to our call or selcallview
    public static boolean matchThisCallWith(String thisCall, String call, boolean allowALL) {

        boolean mMatch1 = (allowALL && call.equals("9999")) || call.equals(thisCall.trim());
        boolean mMatch2 = (allowALL && call.equals("*"))
                || call.toLowerCase(Locale.US).equals(thisCall.trim().toLowerCase(Locale.US));

        return mMatch1 || mMatch2;
    }


    //Tries to find a match for a given combination of phone number and time
    // If none are found, create an entry for that Cellular number and now time combination
    // Automatically generated entries look like:
    // +61412345678,joe,1524632518000
    // This entry links "joe", the sender of a radio message, with the cellular number +61412345678
    //   it was relayed to, counting time from the epoch as shown after the last comma.
    public static void updateSMSFilter(String cellularNumber, String from) {
        String[] filterList = config.getPreferenceS("SMSLISTENINGFILTER", "*,*,0").split("\\|");
        boolean haveMatch = false;
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
                    lastCommTime = 1; //Any number non zero
                }
                String fromFilter = thisFilter[1].trim();
                String phoneFilter = thisFilter[0].trim();
                //int maxHoursSinceLastComm = config.getPreferenceI("MAXHOURSSINCELASTCOMM", 24);
                if ((fromFilter.equals("*") || from.trim().equals(fromFilter))
                        && (phoneFilter.equals("*") || RMsgMisc.lTrimZeros(phoneFilter).endsWith(RMsgMisc.lTrimZeros(cellularNumber)))) {
                    //Do we need to update that last communication time
                    if (lastCommTime > 1) {
                        //We had a time in here, not a zero or a mis-typed number
                        filterList[i] = phoneFilter + "," + thisFilter[1].trim() + "," + nowTime.toString();
                    }
                    haveMatch = true;
                    break;
                }
            }
        }
        String newSmsFilter = "";
        if (!haveMatch) {
            //add new entry at the top
            newSmsFilter = cellularNumber + "," + from + "," + nowTime.toString() + "|";
        }
        for (int j = 0; j < filterList.length; j++) {
            newSmsFilter = newSmsFilter + filterList[j] + "|";
        }
        SharedPreferences.Editor editor = RadioMSG.mysp.edit();
        editor.putString("SMSLISTENINGFILTER", newSmsFilter.replace("||", "|"));
        editor.commit();
        //Return the destination callsign for this number and time combination
        //Only the first one for now

    }


    //Tries to find a match for a given combination of email address and time
    // If none are found, create an entry for that email address and now time combination
    // Automatically generated entries look like:
    // myemail@myprovider.com.au,joe,1524632518000
    // This entry links "joe", the sender of a radio message, with the email address myemail@myprovider.com.au with joe
    //   it was relayed to, counting time from the epoch as shown after the last comma.
    public static void updateEmailFilter(String emailAddress, String from) {
        String[] filterList = config.getPreferenceS("EMAILLISTENINGFILTER", "*,*,0").split("\\|");
        boolean haveMatch = false;
        Long nowTime = System.currentTimeMillis();

        //Check valid email address format
        if (!emailAddress.matches("^[\\w.-]+@\\w+\\.[\\w.-]+")) {
            return;
        }
        //Iterate through list of entries
        for (int i = 0; i < filterList.length; i++) {
            //Match on time, then on incoming phone number
            String[] thisFilter = filterList[i].split(",");
            //Only properly formed filters
            if (thisFilter.length == 3) {
                long lastCommTime;
                try {
                    lastCommTime = Long.parseLong(thisFilter[2].trim());
                } catch (Exception e) {
                    lastCommTime = 1; //Any number non zero
                }
                String fromFilter = thisFilter[1].trim();
                String emailFilter = thisFilter[0].trim();
                //int maxHoursSinceLastComm = config.getPreferenceI("MAXHOURSSINCELASTCOMM", 24);
                if ((fromFilter.equals("*") || from.trim().equals(fromFilter))
                        && (emailFilter.equals("*") || emailFilter.toLowerCase(Locale.US).equals(emailAddress.toLowerCase(Locale.US)))) {
                    //Do we need to update that last communication time
                    if (lastCommTime > 1) {
                        //We had a time in here, not a zero or a mis-typed number
                        filterList[i] = emailFilter + "," + thisFilter[1].trim() + "," + nowTime.toString();
                    }
                    haveMatch = true;
                    break;
                }
            }
        }
        String newEmailFilter = "";
        if (!haveMatch) {
            //add new entry at the top
            newEmailFilter = emailAddress + "," + from + "," + nowTime.toString() + "|";
        }
        for (int j = 0; j < filterList.length; j++) {
            newEmailFilter = newEmailFilter + filterList[j] + "|";
        }
        SharedPreferences.Editor editor = RadioMSG.mysp.edit();
        editor.putString("EMAILLISTENINGFILTER", newEmailFilter.replace("||", "|"));
        editor.commit();
    }


    //Called when a client asked a callsign to be unlinked from am email or SMS
    public static boolean removeFilterEntries(String address, String from) {

        //In case the address is an Alias, find also the full address
        String fullAddress = RadioMSG.msgDisplayList.
                getReceivedAliasAndDestination(address + "=", from);
        if (fullAddress.contains("=")) {
            //extract the destination only (it can be an email or a cellular number
            fullAddress = RMsgUtil.extractDestination(fullAddress);
        }
        //First on Email filter
        String[] filterList = config.getPreferenceS("EMAILLISTENINGFILTER", "").split("\\|");
        //String[] filterList = {"*,*,0"}; //tbf config.getPreferenceS("EMAILLISTENINGFILTER", "*,*,0").split("\\|");
        boolean haveMatch = false;
        String newFilter = "";
        //Iterate through list of entries
        for (int i = 0; i < filterList.length; i++) {
            //Match on time, then on incoming phone number
            String[] thisFilter = filterList[i].split(",");
            //Only properly formed filters
            if (thisFilter.length == 3) {
                String fromFilter = thisFilter[1].trim();
                String emailFilter = thisFilter[0].trim();
                if (fromFilter.toLowerCase(Locale.US).equals(from.toLowerCase(Locale.US))
                        && (emailFilter.toLowerCase(Locale.US).equals(address.toLowerCase(Locale.US))
                        || emailFilter.toLowerCase(Locale.US).equals(fullAddress.toLowerCase(Locale.US)))) {
                    //Match, we delete by skipping it
                    haveMatch = true;
                } else {
                    //No match, store that entry
                    newFilter = newFilter + filterList[i] + "|";
                }
            }
        }
        //Store updated filter
        newFilter = newFilter.replace("||", "|");
        config.setPreferenceS("EMAILLISTENINGFILTER", newFilter);

        //Second on SMS filter
        filterList = config.getPreferenceS("SMSLISTENINGFILTER", "").split("\\|");
        //String[] filterList = {"*,*,0"}; //tbf config.getPreferenceS("EMAILLISTENINGFILTER", "*,*,0").split("\\|");
        newFilter = "";
        //Iterate through list of entries
        for (int i = 0; i < filterList.length; i++) {
            //Match on time, then on incoming phone number
            String[] thisFilter = filterList[i].split(",");
            //Only properly formed filters
            if (thisFilter.length == 3) {
                String fromFilter = thisFilter[1].trim();
                String smsFilter = thisFilter[0].trim();
                if (fromFilter.toLowerCase(Locale.US).equals(from.toLowerCase(Locale.US))
                        && (smsFilter.toLowerCase(Locale.US).equals(address.toLowerCase(Locale.US))
                        || smsFilter.toLowerCase(Locale.US).equals(fullAddress.toLowerCase(Locale.US)))) {
                    //Match, we delete by skipping it
                    haveMatch = true;
                } else {
                    //No match, store that entry
                    newFilter = newFilter + filterList[i] + "|";
                }
            }
        }
        //Store updated filter
        newFilter = newFilter.replace("||", "|");
        config.setPreferenceS("SMSLISTENINGFILTER", newFilter);

        return haveMatch;
    }


    //Forward message as email message
    public static void sendMailMsg(String addressStr, String subjectStr, String body, String attachmentPath) {

        String smtpServer = config.getPreferenceS("RELAYEMAILSMTPSERVER");
        String socketFactoryPort = config.getPreferenceS("RELAYEMAILSMTPPORT", "587");
        String socketFactoryClass = "javax.net.ssl.SSLSocketFactory";
        String smtpAuth = "true";
        String smtpPort = config.getPreferenceS("RELAYEMAILSMTPPORT", "587");
        final String fromAddress = config.getPreferenceS("RELAYEMAILADDRESS");
        final String userName = config.getPreferenceS("RELAYUSERNAME");
        final String password = config.getPreferenceS("RELAYEMAILPASSWORD");
        try {
            Properties props = System.getProperties();
            props.put("mail.debug", "true");
            props.put("mail.smtp.host", smtpServer);
            props.put("mail.smtp.socketFactory.port", socketFactoryPort);
            props.put("mail.smtp.socketFactory.class", socketFactoryClass);
            props.put("mail.smtp.auth", smtpAuth);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.from", fromAddress);
            //Startls OR ssl but not both?
            String smtpProtocol = config.getPreferenceS("SMTPPROTOCOL", "STARTTLS");
            if (smtpProtocol.equals("STARTTLS")) {
                props.put("mail.smtp.starttls.enable", "true");
            } else if (smtpProtocol.equals("STARTTLS")) {
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                //Must be NONE, do nothing for now (to be tested)
            }
            props.put("mail.smtp.ssl.trust", smtpServer);
            // Accept only TLS 1.1 and 1.2
            //props.setProperty("mail.smtp.ssl.protocols", "TLSv1.1 TLSv1.2");
            //props.put("mail.smtp.socketFactory.fallback", "false");
            //Get a new instance each time as default instance conflicts with the email read section
            //Session session = Session.getDefaultInstance(props, null);
            javax.mail.Session session = javax.mail.Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            });
            session.setDebug(true);
            // -- Create a new message --
            javax.mail.Message msg = new MimeMessage(session);
            // -- Set the FROM and TO fields --
            msg.setFrom(new InternetAddress(fromAddress));
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(addressStr, false));
            // -- We could include CC recipients too --
            // if (cc != null)
            // msg.setRecipients(RMsgUtil.RecipientType.CC
            // ,InternetAddress.parse(cc, false));
            // -- Set the subject and body text --
            msg.setSubject(subjectStr);
            // -- Set some other header information --
            msg.setHeader("X-Mailer", "Radio Message Relay");
            msg.setSentDate(new Date());
            //msg.setText(body);
            // creates message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            //messageBodyPart.setContent(body, "text/html");
            messageBodyPart.setContent(body, "text/plain; charset=UTF-8");
            // creates multi-part
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            // adds the attachment if path is present
            if (!attachmentPath.equals("")) {
                MimeBodyPart attachPart = new MimeBodyPart();
                try {
                    attachPart.attachFile(attachmentPath);
                    multipart.addBodyPart(attachPart);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            // sets the multi-part as e-mail's content
            msg.setContent(multipart);
            // Create an SMTP transport from the session
            SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
            // Connect to the server using credentials
            t.connect(smtpServer, userName, password);
            // Send the message
            t.sendMessage(msg, msg.getAllRecipients());
            //System.out.println("RMsgUtil sent OK.");
            //Done upstrem now
            //updateEmailFilter(mMessage.to, mMessage.from);
        } catch (Exception ex) {
            RadioMSG.middleToastText("Error relaying message as Email: " + ex.toString());
            //Save in log for debugging
            RMsgUtil.addEntryToLog("Error relaying message as Email: \n" + ex.toString());
        }
    }


    //Forward message as smsview/mmsview message
    public static void sendCellularMsg(String toPhoneNumber, String body) {
        //Check the permission for sending SMSs
        if (ContextCompat.checkSelfPermission(RadioMSG.myContext, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            //Convert number to international format to avoid issues
            toPhoneNumber = convertNumberToE164(toPhoneNumber);
            /* if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && mMessage.picture != null) {
                //NOT working -- Now requires app to be default SMS app on phone
                //Send MMS
                Settings settings = new Settings();
                settings.setUseSystemSending(true);
                Transaction transaction = new Transaction(RadioMSG.myContext, settings);
                String body = mMessage.formatForSmsOrEmail();
                com.klinker.android.send_message.Message message = new com.klinker.android.send_message.Message(body, mMessage.to);
                message.setImage(mMessage.picture);
                transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
                //smsMgr.sendMultimediaMessage (RadioMSG.myContext,
                //        Uri contentUri,
                //        String locationUrl,
                //        Bundle configOverrides,
                //        PendingIntent sentIntent);
                //
                //Send SMS instead of MMS (no picture)
                SmsManager smsMgr = SmsManager.getDefault();
                ArrayList<String> bodyParts = smsMgr.divideMessage(body);
                smsMgr.sendMultipartTextMessage(toPhoneNumber, null, bodyParts, null, null);

            } else {
            */
            //Send SMS
            SmsManager smsMgr = SmsManager.getDefault();
            ArrayList<String> bodyParts = smsMgr.divideMessage(body);
            //Use multiparts in case it is longer that one message
            //smsMgr.sendTextMessage(mMessage.to, null, body, null, null);
            smsMgr.sendMultipartTextMessage(toPhoneNumber, null, bodyParts, null, null);
            //}
        } else {
            RadioMSG.topToastText("Permission to send SMS not granted. Go to the device \"Settings/Apps\" and " +
                    "allow \"Send SMS\" for the RadioMsg app");
        }
    }


    public static void soundAlarm() {
        Thread myThread = new Thread() {
            @Override
            public void run() {
                int counter = 0;
                while (RMsgProcessor.TXActive) {
                    try {
                        //Wait until we stop Txing otherwise we send the ringtone to the transceiver
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    if (++counter > 200) { //Max 20 seconds then give up
                        RadioMSG.topToastText("Waited too long for TXActive");
                        return;
                    }
                }
                try {
                    //Wait to clear to tx buffer
                    Thread.sleep(3000);
                } catch (Exception e) {
                }
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone myRingtone = RingtoneManager.getRingtone(RadioMSG.myContext.getApplicationContext(), notification);
                myRingtone.play();
                //Wait to clear to audio buffer
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        };
        myThread.start();
    }


    //Process one block of received data. Called from Modem.RxBlock when a complete block is received.
    //Block format is:
    // <SOH>FROM:*|DEST\n with "*" meaning to ALL
    //[via:RELAYSTATION\n]
    //[sms:MESSAGE CONTENT]\n] stripped of CR and with escaped LF
    //[pos:[-]ll.lllllll,[-]LLL.LLLLLL,DDD,SSS]\n] with ll = latitude, LLL = longitude in decimal degrees, DDD = delay in acquiring GPS fix in seconds, SSS = speed in Km/h
    //[pos:PPPPPPPPDDD,SSS\n] with PPPPPPPP = compressed position as per APRS format, DDD = delay in acquiring GPS fix in seconds, SSS = speed in Km/h
    //[pic:WWWxHHH,B&W|Col,NX,MMMMM\n] Picture size in pixels, Greyscale or Colour, N = speed to transfer, MMMMM = mode used
    //NNNN<EOT> with NNNN = crc16 remapped to lowercase characters ONLY as they transmit faster via varicode
    public static void processBlock(String blockLine, String fileName, String rxMode) {

        //Create text part of message and see if we need to wait for other data like a picture
        RMsgObject mMessage = RMsgObject.extractMsgObjectFromString(blockLine, false, fileName, rxMode);//Only text information
        if (mMessage.to.contains("=")) {
            //Replace alias only with alias=destination. If we received an alias with a phone number convert to E164 international format
            mMessage.to = RadioMSG.msgDisplayList.getReceivedAliasAndDestination(mMessage.to, mMessage.from);
        }
        if (mMessage.from.contains("=")) {
            //Replace alias only with alias=destination
            mMessage.from = RadioMSG.msgDisplayList.getSentDestinationFromAlias(mMessage.from);
        }
        //If is a phone number (sms message)  gateway, convert the phone number to the international format
        String mDestination = RMsgUtil.extractDestination(mMessage.to);
        if (isCellular(mDestination)) {
            mDestination = convertNumberToE164(mDestination);
            mMessage.to = RMsgUtil.extractAliasOnly(mMessage.to) + mDestination;
        }
        if (!mMessage.pictureString.equals("")) {
            //We have a picture coming, save this message for later
            //Message will be saved when either Picture is received or Timeout occurred
            //Therefore save the time of end of Rx (any attached picture must be send within a set time)
            lastMessageEndRxTime = System.currentTimeMillis();
            lastTextMessage = mMessage;
        } else {
            //Text only, process immediately
            lastMessageEndRxTime = 0L;
            processTextMessage(mMessage);
            //Reset stored message
            lastTextMessage = null;
        }
    }

    //Extract alias if address contains a full alias like alias=origins, with origins being a phone number
    private static String getAliasFromFullAlias(String fullAlias) {
        String alias = fullAlias;
        Pattern pscf = Pattern.compile("^\\s*(.+)\\s*=(.+)\\s*$");

        Matcher mscf = pscf.matcher(fullAlias);
        if (mscf.lookingAt()) {
            String group2 = mscf.group(2);
            if (group2 != null && mscf.group(1) != null && (isCellular(group2) || isEmail(group2))) {
                alias = mscf.group(1) + "=";
            }
        }
        return alias;
    }

    //Builds the list of messages to send to the qtc request. For but to email requests as the messages are already in the received list of the app.
    private static ArrayList<RMsgObject> buildNonEmailResendList(RMsgObject mMessage, int numberOf, Long forLast, Boolean forAll, Boolean positionsOnly) {

        //Get list of messages to send
        int listCount = RadioMSG.msgDisplayList.getCount();
        ArrayList<RMsgObject> resendList = new ArrayList<RMsgObject>();
        RMsgDisplayItem recDisplayItem;
        RMsgObject recMessage;
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar localCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date dateNow = localCalendar.getTime();
        Boolean foundPreviousQtc = false;
        int reSentCount = 0;
        Boolean goodToResend;
        for (int i = listCount - 2; i >= 0; i--) {//Skip just received qtc? message
            goodToResend = false;
            recDisplayItem = RadioMSG.msgDisplayList.getDisplayListItem(i);
            recMessage = recDisplayItem.mMessage;
            //      Must be a received message
            if (matchThisCallWith(mMessage.from, recMessage.from, false) &&
                    recMessage.sms.startsWith("*qtc? ")) { //Ignore simple qtc requests "*qtc?"
                foundPreviousQtc = true;
            }
            if ((!recDisplayItem.myOwn && !matchMyCallWith(recMessage.from, false)) //Not my own message
                    //AND for all OR for this requesting callsign
                    && (forAll || matchThisCallWith(mMessage.from, recMessage.to, true))
                    //AND not from this requesting callsign
                    && (!matchThisCallWith(mMessage.from, recMessage.from, false))
                    //AND is NOT from a cellular number (We moved cellular number into the email list to be
                    //  consistent with the PC jPskmail implementation of RadioMsg)
                    && (!isCellular(RadioMSG.msgDisplayList.getSentDestinationFromAlias(recMessage.from)))
                    //AND be only a position message if requested so
                    && (!positionsOnly || recMessage.msgHasPosition)
                    && (!recMessage.sms.startsWith("*qtc?"))
                    && (!recMessage.sms.startsWith("*cmd"))
                    && (!recMessage.sms.startsWith("*pos?"))
                    && (!recMessage.sms.startsWith("No messages"))
                    && (!RMsgUtil.stringsStartsWithCaseUnsensitive(recMessage.sms, "Re-Sending ")) //already re-sent message
                    && (!RMsgUtil.stringsStartsWithCaseUnsensitive(recMessage.sms, "Scan is off for"))
                    && (!RMsgUtil.stringsStartsWithCaseUnsensitive(recMessage.sms, "Scan is on"))
                    && (!recDisplayItem.mMessage.sms.startsWith("*tim?"))
                    && (!recDisplayItem.mMessage.sms.startsWith("*Time Reference Received*"))
                    && (!recMessage.sms.matches("^\\d{1,3}\\%")) //Not an Inquire reply
                    && (!recMessage.rawRxString.endsWith("ssss" + Modem.EOT)) //Not a CCIR (selcall) message
                    && (!recMessage.rxtxMode.equals("CCIR493"))
                    && (recDisplayItem.mMessage.receiveDate == null)) { //New Method
                if (forLast > 0L) { //We have a time-based request
                    Date recMsgDate;
                    try {
                        recMsgDate = formatter.parse(recMessage.fileName.replaceAll(".txt", ""));
                    } catch (ParseException e) {
                        //Dummy date just to prevent failure
                        recMsgDate = dateNow;
                    }
                    if ((dateNow.getTime() - recMsgDate.getTime()) <= forLast) { //In the last X min, hours
                        goodToResend = true;
                    } else {
                        break; //No point in looking any further
                    }
                } else if (numberOf == -1) { //We want all messages since last QTC
                    if (!foundPreviousQtc) {
                        goodToResend = true;
                    } else {
                        break; //No point in looking any further
                    }
                } else { //Then we must have a number based request
                    if (reSentCount < numberOf) {
                        goodToResend = true;
                    } else if (numberOf > 0) { //We found the number of messages required
                        break; //No point in looking any further
                    }
                }
                if (goodToResend) {
                    if (++reSentCount >= 20) {
                        break; //Hard stop: enough to send
                    }
                    //Enqueue message for sorting. Get full message with binary data.
                    RMsgObject fullMessage = RMsgObject.extractMsgObjectFromFile(DirInbox, recMessage.fileName, true);
                    //Coming from this relay station
                    //Changed so that only CCIR493 mode uses Selcall
                    //fullMessage.relay = (fullMessage.rxtxMode.equals("CCIR476") ?
                    //        config.getPreferenceS("SELCALL").trim() : config.getPreferenceS("CALL")).trim();
                    fullMessage.relay = config.getPreferenceS("CALL").trim();
                    //Remove any origin data in the from field if an alias exists (when receiving SMSs)
                    fullMessage.from = getAliasFromFullAlias(fullMessage.from); //Return same string is no cellular full alias
                    //Remove via information to make sure it is not forwarded
                    fullMessage.via = "";
                    //Re-send/relay in the same mode we received in
                    fullMessage.rxtxMode = mMessage.rxtxMode;
                    //Add text to specify it was received not relayed
                    //Not needed anymore as we send the ro: field
                    //fullMessage.sms = "Re-Sending " + recDisplayItem.mMessage.fileName.replaceAll(".txt", "")
                    //        + ": " + fullMessage.sms;
                    //Set the receivedDate for the "ro:" information to be sent at TX time
                    try {
                        //Example = "2017-10-25_113958";
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        fullMessage.receiveDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        fullMessage.receiveDate.setTime(sdf.parse(fullMessage.fileName.replaceAll(".txt", "")));
                    } catch (ParseException e) {
                        //Debug
                        e.printStackTrace();
                    }
                    //Then send save Message in list for sorting and limiting the number
                    resendList.add(fullMessage);
                }
            }
        }

        return resendList;
    }


    //Builds the list of messages to send to the qtc request. Specific to email requests.
    //On Android we receive the SMS messages not from the email but directly from the phone SMS provider, therefore
    //  we need to scan and extract these messages as if they were received from the internet as well to be consistent
    //  with the jPskmail version
    private static ArrayList<RMsgObject> buildEmailResendList(RMsgObject mMessage, int numberOf, Long forLast, boolean fullSize, boolean forAll) {

        //Get list of messages to send
        int listCount = RadioMSG.msgDisplayList.getCount();
        ArrayList<RMsgObject> resendList = new ArrayList<RMsgObject>();
        RMsgDisplayItem recDisplayItem;
        RMsgObject recMessage;
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar localCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date dateNow = localCalendar.getTime();
        Date recMsgDate = null;
        //If requested since last QTC, find the previous one first and extract the time filter
        if (numberOf == -1 && forLast == 0L) {
            //Iterate from the 2nd last message received, backwards (back in time), skipping just received "*qtc?" message
            for (int i = listCount - 2; i >= 0; i--) {
                recDisplayItem = RadioMSG.msgDisplayList.getDisplayListItem(i);
                recMessage = recDisplayItem.mMessage;
                //Found previous QTC message (in case we request all messages since last request)
                if (matchThisCallWith(mMessage.from, recMessage.from, false) &&
                        recMessage.sms.startsWith("*qtc? ")) { //Ignore simple qtc requests "*qtc?"
                    //Extract time to use as filter on the imap email list
                    try {
                        recMsgDate = formatter.parse(recMessage.fileName.replaceAll(".txt", ""));
                    } catch (ParseException e) {
                        //Bad date format, set to NOW to prevent a fault if the filename is malformed
                        recMsgDate = localCalendar.getTime();
                    }
                    break; //No point in looking any further
                }
            }
        } else if (forLast != 0L) {
            //We have a time based request
            recMsgDate = new Date(System.currentTimeMillis() - forLast);
        }
        //No previous matching "*qtc?" message found despite wanting all messages since last
        //  request, skip emails, check SMSs only
        if ((numberOf == -1 && forLast == 0L && recMsgDate != null)
        || numberOf != -1 || forLast != 0L) {
            //Request emails from server
            IMAPFolder folder = null;
            Store store = null;
            final String imapServer = config.getPreferenceS("RELAYEMAILIMAPSERVER", "");
            final String imapPort = config.getPreferenceS("RELAYEMAILIMAPPORT", "993");
            int charLimit = fullSize ? 1000 : 150;
            //Store current time. To be used with increments of 1 seconds to differentiate the messages
            Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
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
                Message[] messages;
                if (numberOf == -1) {
                    //We must have a valid date to select messages from (date only, then select individually
                    SearchTerm st = new ReceivedDateTerm(ComparisonTerm.GE, recMsgDate);
                    messages = folder.search(st);
                } else {
                    //Fetch all messages
                    messages = folder.getMessages();
                }
                int msgCount = 0;
                //Process in the order of most recent to oldest
                for (int i = messages.length; i > 0; i--) {
                    //Filtering by date and by requester
                    Message msg = messages[i - 1];
                    if (numberOf == -1) {
                        //Check the time as the javamail filter only checks the date, not date and time
                        if (msg.getReceivedDate().before(recMsgDate)) {
                            continue;
                        }
                    }
                    //Save message time for making filenames later on
                    Date msgDateTime = msg.getReceivedDate();
                    c1.setTime(msgDateTime);
                    //set(msgDateTime.setTime().getYear(), msgDateTime.getMonth(), msgDateTime.getDay(), msgDateTime.getHours(),
                    //        msgDateTime.getMinutes(), msgDateTime.getSeconds());
                    //Check reply messages count
                    if (++msgCount > 20 //Hard stop
                            || (numberOf > 0 && msgCount > numberOf)) {
                        break; //Stop: enough to send
                    }
                    //From email address
                    String fromString = msg.getFrom()[0].toString();
                    //Remove name and only keep email address proper
                    String[] emailAdresses = fromString.split("[ <>]+");
                    for (String fromPart : emailAdresses) {
                        if (fromPart.indexOf("@") > 0) {
                            fromString = fromPart;
                            break;
                        }
                    }
                    String[] tos;
                    if (forAll) {
                        //Option of requesting all emails regardless of who they are for
                        //Just add the requester of the QTC
                        tos = new String[1];
                        tos[0] = mMessage.from;
                    } else {
                        //Add to reply list for each matching filter
                        tos = RadioMSG.passEmailFilter(fromString);
                    }
                    for (int j = 0; j < tos.length; j++) {
                        //Only if the filter matches the requesting callsign
                        if (tos[j].equals("*") || mMessage.from.toLowerCase(Locale.US).equals(tos[j].toLowerCase(Locale.US))) {
                            String smsString = getBodyTextFromMessage(msg);
                            if (smsString.startsWith("\n")) {
                                smsString = smsString.substring(1);
                            }
                            if (smsString.endsWith("\r\n\r\n")) {
                                smsString = smsString.substring(0, smsString.length() - 4);
                            }
                            if (smsString.length() > charLimit) {
                                smsString = smsString.subSequence(0, charLimit - 1) + " ...>";
                            }
                            String smsSubject = msg.getSubject();
                            //Not a reply to a previous radio message, add the subject line
                            if (!smsSubject.contains("Radio Message from ")
                                    && !smsSubject.contains("Reply from ")
                                    && !smsSubject.trim().equals("")) {
                                smsString = "Subj " + smsSubject + "\n" + smsString;
                            }
                            //Debug
                            //smsString = smsString + " Rec Date: " + msg.getReceivedDate() + "\n";

                            RMsgObject fullMessage = new RMsgObject(tos[j], "", smsString,
                                    null, 0, false, 0, false, null, 0L, null);
                            //Coming from this relay station
                            //Changed so that only CCIR493 mode uses Selcall
                            //fullMessage.relay = (fullMessage.rxtxMode.equals("CCIR476") ?
                            //        config.getPreferenceS("SELCALL").trim() : config.getPreferenceS("CALL")).trim();
                            fullMessage.relay = config.getPreferenceS("CALL").trim();
                            //Remove via information to make sure it is not forwarded
                            //fullMessage.via = "";
                            //Re-send/relay in the same mode we received in
                            fullMessage.rxtxMode = mMessage.rxtxMode;
                            //From email address
                            fullMessage.from = RadioMSG.msgDisplayList.getAliasFromOrigin(fromString, tos[j]);
                            //Then save Message in list for sorting and limiting the number
                            //Save received date for incoming message
                            fullMessage.receiveDate =  Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            fullMessage.receiveDate.setTime(c1.getTime()); //= c1;
                            fullMessage.fileName = String.format(Locale.US, "%04d", c1.get(Calendar.YEAR)) + "-" +
                                    String.format(Locale.US, "%02d", c1.get(Calendar.MONTH) + 1) + "-" +
                                    String.format(Locale.US, "%02d", c1.get(Calendar.DAY_OF_MONTH)) + "_" +
                                    String.format(Locale.US, "%02d%02d%02d", c1.get(Calendar.HOUR_OF_DAY),
                                            c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND)) + ".txt";
                            c1.add(Calendar.SECOND, 1);
                            resendList.add(fullMessage);
                            break; //No more messages for this email as they would be redundant
                        }
                    }
                }
            } catch (Exception e) {
                Exception e1 = e; //for debug
                RadioMSG.middleToastText(e.toString());
                //Save in log for debugging
                RMsgUtil.addEntryToLog("Error relaying message as Email: \n" + e.toString());
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
        //Now add SMSs received on this phone for this callsign (On Android they get stored automatically as a received
        // message on reception of the SMS, as opposed to the PC version which gets them via the email to SMS gateway)
        Boolean foundPreviousQtc = false;
        int reSentCount = 0;
        Boolean goodToResend;
        for (int i = listCount - 2; i >= 0; i--) {//Skip just received qtc? message
            goodToResend = false;
            recDisplayItem = RadioMSG.msgDisplayList.getDisplayListItem(i);
            recMessage = recDisplayItem.mMessage;
            //Found previous QTC message (in case we request all messages since last request)
            if (matchThisCallWith(mMessage.from, recMessage.from, false) &&
                    recMessage.sms.startsWith("*qtc? ")) { //Ignore simple qtc requests "*qtc?"
                foundPreviousQtc = true;
            }
            if ((!recDisplayItem.myOwn && !matchMyCallWith(recMessage.from, false)) //Not my own message
                    //AND for all OR for this requesting callsign
                    && (forAll || matchThisCallWith(mMessage.from, recMessage.to, true))
                    //AND not from this requesting callsign
                    && (!matchThisCallWith(mMessage.from, recMessage.from, false))
                    //AND is from a cellular number. This is to be consistent with the PC jPskmail
                    //  implementation of RadioMsg (here SMSs used to be included in the non-email resend list)
                    && (isCellular(RadioMSG.msgDisplayList.getSentDestinationFromAlias(recMessage.from)))
                    //AND is not a previously re-sent Cellular SMS message
                    && (recMessage.receiveDate == null)
                    //AND must not be a qtc? request
                    && (!recMessage.sms.startsWith("*qtc?"))) {
                if (forLast > 0L) { //We have a time-based request
                    //Date recMsgDate;
                    try {
                        recMsgDate = formatter.parse(recMessage.fileName.replaceAll(".txt", ""));
                    } catch (ParseException e) {
                        //Dummy date just to prevent failure
                        recMsgDate = dateNow;
                    }
                    if ((dateNow.getTime() - recMsgDate.getTime()) <= forLast) { //In the last X min, hours
                        goodToResend = true;
                    } else {
                        break; //No point in looking any further
                    }
                } else if (numberOf == -1) { //We want all messages since last QTC
                    if (!foundPreviousQtc) {
                        goodToResend = true;
                    } else {
                        break; //No point in looking any further
                    }
                } else if (numberOf > 0) { //Then we must have a number based request
                    if (reSentCount < numberOf) {
                        goodToResend = true;
                    } else if (numberOf > 0) { //We found the number of messages required
                        break; //No point in looking any further
                    }
                }
                //Found a valid candidate
                if (goodToResend) {
                    if (++reSentCount >= 20) {
                        break; //Hard stop: enough to send
                    }
                    //Enqueue message for sorting. Get full message with binary data.
                    RMsgObject fullMessage = RMsgObject.extractMsgObjectFromFile(DirInbox, recMessage.fileName, true);
                    //Coming from this relay station
                    //Changed so that only CCIR493 mode uses Selcall
                    //fullMessage.relay = (fullMessage.rxtxMode.equals("CCIR476") ?
                    //        config.getPreferenceS("SELCALL").trim() : config.getPreferenceS("CALL")).trim();
                    fullMessage.relay = config.getPreferenceS("CALL").trim();
                    //Remove any origin data in the from field if an alias exists (when receiving SMSs)
                    fullMessage.from = getAliasFromFullAlias(fullMessage.from); //Return same string is no cellular full alias
                    //Remove via information to make sure it is not forwarded
                    fullMessage.via = "";
                    //Re-send/relay in the same mode we received in
                    fullMessage.rxtxMode = mMessage.rxtxMode;
                    //Add text to specify it was received not relayed
                    //Not needed anymore as we store the receiveDate field below
                    //fullMessage.sms = "Re-Sending " + recMessage.fileName.replaceAll(".txt", "")
                    //        + ": " + fullMessage.sms;
                    try {
                        //File name example "2017-10-25_113958.txt";
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        fullMessage.receiveDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        fullMessage.receiveDate.setTime(sdf.parse(fullMessage.fileName.replaceAll(".txt", "")));
                        //Log.i(TAG, "time = " + cal.getTimeInMillis());
                    } catch (ParseException e) {
                        //Debug
                        e.printStackTrace();
                    }
                    //Then send save Message in list for sorting and limiting the number
                    resendList.add(fullMessage);
                }
            }
        }

        return resendList;
    }


    //Builds the list of SENT messages to reply to the qtc, instead of looking at the received list, and exclude already re-sent messages
    private static ArrayList<RMsgObject> buildTXedResendList(RMsgObject mMessage, int numberOf, Long forLast, Boolean forAll, Boolean positionsOnly) {

        //Get list of messages to send
        int listCount = RadioMSG.msgDisplayList.getCount();
        ArrayList<RMsgObject> resendList = new ArrayList<RMsgObject>();
        RMsgDisplayItem recDisplayItem;
        RMsgObject recMessage;
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar localCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date dateNow = localCalendar.getTime();
        Boolean foundPreviousQtc = false;
        int reSentCount = 0;
        Boolean goodToResend;
        for (int i = listCount - 2; i >= 0; i--) {//Skip just received qtc? message
            goodToResend = false;
            recDisplayItem = RadioMSG.msgDisplayList.getDisplayListItem(i);
            recMessage = recDisplayItem.mMessage;
            //      Must be a received message
            if (matchThisCallWith(mMessage.from, recMessage.from, false) &&
                    recMessage.sms.startsWith("*qtc? ")) { //Ignore simple qtc requests "*qtc?"
                foundPreviousQtc = true;
            }
            if (recDisplayItem.myOwn //My own message (a Sent message)
                    //AND for all OR for this requesting callsign
                    && (forAll || matchThisCallWith(mMessage.from, recMessage.to, true))
                    //AND not from this requesting callsign
                    && (!matchThisCallWith(mMessage.from, recMessage.from, false))
                    //AND be only a position message if requested so
                    && (!positionsOnly || recMessage.msgHasPosition)
                    //AND must not be a qtc?, command or previously re-sent message
                    && (!recMessage.sms.startsWith("*qtc?"))
                    && (!recMessage.sms.startsWith("*cmd"))
                    && (!recMessage.sms.startsWith("*pos?"))
                    && (!recMessage.sms.startsWith("No messages"))
                    && (!RMsgUtil.stringsStartsWithCaseUnsensitive(recMessage.sms, "Re-Sending ")) //already re-sent message
                    && (!RMsgUtil.stringsStartsWithCaseUnsensitive(recMessage.sms, "Scan is off for"))
                    && (!RMsgUtil.stringsStartsWithCaseUnsensitive(recMessage.sms, "Scan is on"))
                    && (!recDisplayItem.mMessage.sms.startsWith("*tim?"))
                    && (!recDisplayItem.mMessage.sms.startsWith("*Time Reference Received*"))
                    && (!recMessage.sms.matches("^\\d{1,3}\\%")) //Not an Inquire reply
                    && (!recMessage.rawRxString.endsWith("ssss" + Modem.EOT)) //Not a CCIR (selcall) message
                    && (recDisplayItem.mMessage.receiveDate == null) //New method
                    //AND must not be a position request message
                    && (!recMessage.sms.startsWith("*pos?"))
                    //AND must not be a "no messages" message
                    && (!RMsgUtil.stringsEqualsCaseUnsensitive(recMessage.sms, "No Messages"))) {
                if (forLast > 0L) { //We have a time-based request
                    Date recMsgDate;
                    try {
                        recMsgDate = formatter.parse(recMessage.fileName.replaceAll(".txt", ""));
                    } catch (ParseException e) {
                        //Dummy date just to prevent failure
                        recMsgDate = dateNow;
                    }
                    if ((dateNow.getTime() - recMsgDate.getTime()) <= forLast) { //In the last X min, hours
                        goodToResend = true;
                    } else {
                        break; //No point in looking any further
                    }
                } else if (numberOf == -1) { //We want all messages since last QTC
                    if (!foundPreviousQtc) {
                        goodToResend = true;
                    } else {
                        break; //No point in looking any further
                    }
                } else { //Then we must have a number based request
                    if (reSentCount < numberOf) {
                        goodToResend = true;
                    } else if (numberOf > 0) { //We found the number of messages required
                        break; //No point in looking any further
                    }
                }
                //Discard email or SMS that have been previously sent as we always re-check emails from the internet and SMSs from the list
                String msgFromAddress = recMessage.from;
                if (msgFromAddress.contains("=") || isEmail(msgFromAddress) || isCellular(msgFromAddress)) {
                    //Is an Alias OR is an email Address (w/o and alias) OR is a cellular number (w/o an alias), discard
                    goodToResend = false;
                }
                if (goodToResend) {
                    if (++reSentCount >= 20) {
                        break; //Hard stop: enough to send
                    }
                    //Enqueue message for sorting. Get full message with binary data.
                    RMsgObject fullMessage = RMsgObject.extractMsgObjectFromFile(DirSent, recMessage.fileName, true);
                    //through the same channel as the request (direct ot via the relay)

                    if (fullMessage.from.equals(config.getPreferenceS("CALL", "").toLowerCase(Locale.US).trim())) {
                        fullMessage.relay = "";
                    } else {
                        fullMessage.relay = config.getPreferenceS("CALL", "").toLowerCase(Locale.US).trim();
                    }
                    //Remove via information to make sure it is not forwarded
                    fullMessage.via = "";
                    //Re-send/relay in the same mode we received the repeat request in
                    fullMessage.rxtxMode = mMessage.rxtxMode;
                    //Add text to specify it was received not relayed
                    //fullMessage.sms = "Re-Sending " + recMessage.fileName.replaceAll(".txt", "")
                    //        + ": " + fullMessage.sms;
                    try {
                        //Example = "2017-10-25_113958.txt";
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        fullMessage.receiveDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        fullMessage.receiveDate.setTime(sdf.parse(fullMessage.fileName.replaceAll(".txt", "")));
                        //Log.i(TAG, "time = " + cal.getTimeInMillis());
                    } catch (ParseException e) {
                        //Debug
                        e.printStackTrace();
                    }

                    //Then send save Message in list for sorting and limiting the number
                    resendList.add(fullMessage);
                }
            }
        }

        return resendList;
    }


    //Returns the last Txed message (any type) in response a strait "*qtc?" request
    private static RMsgObject getLastTxed(RMsgObject mMessage) {

        //Get list of messages to send
        int listCount = RadioMSG.msgDisplayList.getCount();
        RMsgDisplayItem recDisplayItem;
        RMsgObject fullMessage = null;
        for (int i = listCount - 2; i >= 0; i--) {//Skip just received *qtc? message
            recDisplayItem = RadioMSG.msgDisplayList.getItem(i);
            //My own message (a Sent message) and is for the requester or for ALL?
            if (recDisplayItem.myOwn && matchThisCallWith(mMessage.from, recDisplayItem.mMessage.to, true)) {
                //Enqueue message for sorting. Get full message with binary data.
                fullMessage = RMsgObject.extractMsgObjectFromFile(DirSent, recDisplayItem.mMessage.fileName, true);
                //Coming from this relay station
                if (fullMessage.from.equals(config.getPreferenceS("CALL", "").toLowerCase(Locale.US).trim())) {
                    fullMessage.relay = "";
                } else {
                    fullMessage.relay = config.getPreferenceS("CALL", "").toLowerCase(Locale.US).trim();
                }
                //Remove via information to make sure it is not forwarded
                fullMessage.via = "";
                //Re-send/relay in the same mode we received in
                fullMessage.rxtxMode = mMessage.rxtxMode;
                if (fullMessage.receiveDate == null) {
                    //Get the receivedDate for the "ro:" information to be sent at TX time
                    try {
                        //Example = "2017-10-25_113958";
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        fullMessage.receiveDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        fullMessage.receiveDate.setTime(sdf.parse(fullMessage.fileName.replaceAll(".txt", "")));
                        //Log.i(TAG, "time = " + cal.getTimeInMillis());
                    } catch (ParseException e) {
                        //Debug
                        e.printStackTrace();
                    }
                }
                //Then send save Message in list for sorting and limiting the number
                return fullMessage;
            }
        }

        return fullMessage;
    }


    //Extract the first part's body and return a plain text string
    public static String getBodyTextFromMessage(Message message) throws Exception {
        // general spacers for time and date
        //String spacers = "[\\s,/\\.\\-]";
        // matches times
        //String timePattern  = "(?:[0-2])?[0-9]:[0-5][0-9](?::[0-5][0-9])?(?:(?:\\s)?[AP]M)?";
        // matches day of the week
        //String dayPattern   = "(?:(?:Mon(?:day)?)|(?:Tue(?:sday)?)|(?:Wed(?:nesday)?)|(?:Thu(?:rsday)?)|(?:Fri(?:day)?)|(?:Sat(?:urday)?)|(?:Sun(?:day)?))";
        // matches day of the month (number and st, nd, rd, th)
        //String dayOfMonthPattern = "[0-3]?[0-9]" + spacers + "*(?:(?:th)|(?:st)|(?:nd)|(?:rd))?";
        // matches months (numeric and text)
        //String monthPattern = "(?:(?:Jan(?:uary)?)|(?:Feb(?:ruary)?)|(?:Mar(?:ch)?)|(?:Apr(?:il)?)|(?:May)|(?:Jun(?:e)?)|(?:Jul(?:y)?)" +
        //                                      "|(?:Aug(?:ust)?)|(?:Sep(?:tember)?)|(?:Oct(?:ober)?)|(?:Nov(?:ember)?)|(?:Dec(?:ember)?)|(?:[0-1]?[0-9]))";
        // matches years (only 1000's and 2000's, because we are matching emails)
        //String yearPattern  = "(?:[1-2]?[0-9])[0-9][0-9]";
        // matches a full date
        //String datePattern     = "(?:" + dayPattern + spacers + "+)?(?:(?:" + dayOfMonthPattern + spacers + "+" + monthPattern + ")|" +
        //                                        "(?:" + monthPattern + spacers + "+" + dayOfMonthPattern + "))" +
        //                                         spacers + "+" + yearPattern;
        // matches a date and time combo (in either order)
        //String dateTimePattern = "(?:" + datePattern + "[\\s,]*(?:(?:at)|(?:@))?\\s*" + timePattern + ")|" +
        //                                        "(?:" + timePattern + "[\\s,]*(?:on)?\\s*"+ datePattern + ")";
        // matches a leading line such as
        // ----Original Message----
        // or simply
        // ------------------------
        // Allows underscores, = and + in a raw also
        //
        //String leadInLine    = "[-_+=]+\\s*(?:Original(?:\\sMessage)?)?\\s*[-_+=]+\n";
        // matches a header line indicating the date
        //String dateLine    = "(?:(?:date)|(?:sent)|(?:time)):\\s*"+ dateTimePattern + ".*\n";
        //String dateLine    = "(?:(?:date)|(?:sent)|(?:time)):.*\n";
        // matches a subject or address line
        //Correction , removed the "|"
        //String subjectOrAddressLine    = "((?:from)|(?:subject)|(?:b?cc)|(?:to))|:.*\n";
        //String subjectOrAddressLine    = "((?:from)|(?:subject)|(?:b?cc)|(?:to)):.*\n";
        // matches gmail style quoted text beginning, i.e.
        //On Mon Jun 7, 2010 at 8:50 PM, Simon wrote:
        //String gmailQuotedTextBeginning = "(On\\s+" + dateTimePattern + ".*wrote:\n)";
        // matches the start of a quoted section of an email
        //Pattern quotedTextStart = Pattern.compile("(?i)(?:(?:" + leadInLine + ")?" +
        //    "(?:(?:(>\\s{0,3})?" + subjectOrAddressLine + ")|(?:" + dateLine + ")){2,6})");

        //first extract compete message text
        String result = "";
        String xyz = message.getContentType();
        String abc = xyz + "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            int count = mimeMultipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    result = result + "\n" + bodyPart.getContent();
                    break;  //without break same text appears twice in my tests
                } else if (bodyPart.isMimeType("text/html")) {
                    String html = (String) bodyPart.getContent();
                    result = result + "\n" + Jsoup.parse(html).text();
                }
            }
        } else if (message.isMimeType("text/html")) {
            String html = message.getContent().toString();
            result = result + "\n" + Jsoup.parse(html).text();
        }
        //Remove the original message if this was a reply to an email
        //Essential for reducing message size and for keeping privacy (as To: and From:
        //  details are included in appended original message)
        Pattern omPatterm = Pattern.compile("(?:(?:^[-_+=]+(?:\\s*Original(?:\\s*Message)?\\s*)?[-_+=]+$)"
                + "|(?:^\\s*(?:>\\s{0,7})?(?:(?:(?:(?:from)|(?:subject)"
                + "|(?:b?cc)|(?:to)):.*)|(?:(?:(?:date)"
                + "|(?:sent)|(?:time)):.*$))))", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        //Sometimes the email trail email text is appended without a new line to the replied text.
        // Pattern gmailPatterm = Pattern.compile("(?:^On.{1,500}wrote:)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Pattern gmailPatterm = Pattern.compile("(?:On.{1,500}wrote:)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        if (result.length() > 0) {
            Matcher omMatcher = omPatterm.matcher(result);
            Matcher gmailMatcher = gmailPatterm.matcher(result);
            if (gmailMatcher.find()) {
                int quotedTextPos = gmailMatcher.start();
                result = result.substring(0, quotedTextPos);
            } else if (omMatcher.find()) {
                int quotedTextPos = omMatcher.start();
                int matchCount = 1;
                while (omMatcher.find()) {
                    matchCount++;
                }
                if (matchCount > 1) {
                    result = result.substring(0, quotedTextPos);
                }
            }
        }
        return result;
    }


    /*
    Parked code
    private boolean textIsHtml = false;

    /**
     * Return the primary text content of the message.
     *
    private String getText(Part p) throws
            MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getText(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }
*/

    //Is the string representative of an email
    public static boolean isEmail(String destination) {

        return destination.matches("^[\\w.-=]+@\\w+\\.[\\w.-]+");
    }

    //Is the string representative of a cellular number
    public static boolean isCellular(String destination) {

        return destination.matches("^([\\w-]+=)?\\+?\\d{7,16}"); //Java server was: "^\\+?\\d{7,16}"
    }


    //Return the potential access password (for relating access like SMSs or E-Mails) or the potential IOT password (for IOT commands)
    public static String getRequiredAccessPassword(RMsgObject txMessage) {
        String accessPw = "";
        String IOTaccessPw = "";

        if (!txMessage.via.equals("")) {
            for (int i = 0; i < RadioMSG.viaArray.length; i++) {
                if (RadioMSG.viaArray[i].equals("via " + txMessage.via)) {
                    accessPw = RadioMSG.viaPasswordArray[i];
                    IOTaccessPw = RadioMSG.viaIotPasswordArray[i];
                    break;
                }
            }
        }
        //Use the IOT password for IOT commands
        if (txMessage.sms.startsWith("*get? ") || txMessage.sms.startsWith("*ser? ")) {
            return IOTaccessPw;
        } else {
            return accessPw;
        }
    }



    //Converts a nationally formatted number (eg 0499888777)to an international one (eg +61499888777)
    public static String convertNumberToE164(String phoneNumber) {
        String resultNum = phoneNumber.replaceAll(" ", "");
        if (resultNum.startsWith("+")) {
            //Already formatted, return as-is with spaces removed
            return resultNum;
        }
        //Must be formatted as a National number, use library to format to E.164 format (+1888999...)
        //Java PC version: PhoneNumberUtil.getInstance();
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.createInstance(RadioMSG.myInstance);
        try {
            String countryCode = config.getPreferenceS("GATEWAYISOCOUNTRYCODE", "");
            if (countryCode.length()  == 0) {
                //Try the current ISO code of the network we are registered to (assumes a phone with SIM card here)
                //That may be the best option for a phone since a device without a sim card (like a WIFI tablet) can't do SMSs anyway.
                if (RadioMSG.mTelephonyManager  != null){
                    countryCode = RadioMSG.mTelephonyManager.getSimCountryIso().toUpperCase(Locale.US);
                }
                //Finally if all else fails, get the language country code
                if (countryCode != null && countryCode.length()  == 0) {
                    //Try via the Locale set on this device as a last resort
                    Locale currentLocale = Locale.getDefault();
                    countryCode = currentLocale.getCountry();
                }
            }
            Phonenumber.PhoneNumber mPhoneNum = phoneUtil.parse(resultNum, countryCode);
            resultNum = phoneUtil.format(mPhoneNum, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            System.err.println("NumberParseException was thrown: " + e.toString());
        }
        return resultNum;
    }


    //Process the message now that it is complete (may have had to wait for the picture component)
    public static void processTextMessage(final RMsgObject mMessage) {
        boolean saveAndDisplay;

        //Check message is non null
        if (mMessage == null)
            return;
        //Launch in new thread so that we don't block the receiving of messages
        //Check if message to ALL or to me only
        // or asked to be a relay
        // BUT is not a message from me forwarded/relayed by another station
        //AND is not a "*qtc?" message meaning request to repeat
        // save message and process it, ignore otherwise
        //Note: the test is case IN-Sensitive

        if ((matchMyCallWith(mMessage.to, true)
                || matchMyCallWith(mMessage.via, false))
                && !matchMyCallWith(mMessage.from, false)) {
            saveAndDisplay = true;
            //Check if this is a duplicate (E.g. We received the message direct and now via a relay)
            // the email/SMS message was already received, but let pos? requests go through as we don't answer on teh direct path
            if ((!mMessage.timeId.equals("") || (mMessage.receiveDate != null)) && !mMessage.sms.contains("*pos?")) {
                //Relayed and with timeId, check if it is a duplicate
                if (RadioMSG.msgDisplayList.isDuplicate(mMessage)) {
                    saveAndDisplay = false;
                    RadioMSG.middleToastText("Duplicate Message");
                }
            }
        } else {
            saveAndDisplay = false;
            if (matchMyCallWith(mMessage.from, false)) {
                RadioMSG.middleToastText("Message from myself, ignoring");
            }
        }
        final boolean finalSaveAndDisplay = saveAndDisplay;
        //Separate thread not to delay or block the Modem thread
        Thread myThread = new Thread() {
            @Override
            public void run() {
                FileWriter out = null;
                try {
                    //save if need be
                    if (finalSaveAndDisplay) {

                        String resultString = "";
                        //Time syncs requests most likely will not be in sync when requested
                        String relayingPassword = config.getPreferenceS("ACCESSPASSWORD", "");
                        if (relayingPassword.startsWith("_") && mMessage.sms.equals("*tim?")) {
                            //Force crc as valid
                            mMessage.crcValidWithRelayPW = true;
                        }
                        if (mMessage.crcValid || mMessage.crcValidWithRelayPW || mMessage.crcValidWithIotPW) {
                            //Re-build the message (included the potential alias translated to alias=destination format)
                            resultString = mMessage.formatForRx(false); //Always rebuild without access password for the CRC
                        } else {
                            //Save message as received, display it and see what we can do with it
                            resultString = mMessage.rawRxString;
                        }
                        String inboxFolderPath = RMsgProcessor.HomePath +
                                RMsgProcessor.Dirprefix + RMsgProcessor.DirInbox + RMsgProcessor.Separator;
                        File msgReceivedFile = new File(inboxFolderPath + mMessage.fileName);
                        if (msgReceivedFile.exists()) {
                            msgReceivedFile.delete();
                        }
                        out = new FileWriter(msgReceivedFile, true);
                        out.write(resultString);
                        out.close();

                        //Add to listadapter
                        final RMsgObject finalMessage = mMessage;
                        RadioMSG.myInstance.runOnUiThread(new Runnable() {
                            public void run() {
                                RadioMSG.msgDisplayList.addNewItem(finalMessage, false); //Is NOT my own message
                            }
                        });
                        //update the list if we are on that screen
                        RadioMSG.mHandler.post(RadioMSG.updateList);
                        //Advise reception of message
                        //PostToTerminal("\nFile integrity check: Checksum OK");
                        PostToTerminal("\nSaved File: " + mMessage.fileName);
                        RMsgUtil.addEntryToLog("Received RMsgUtil " + mMessage.fileName);
                        //Perform notification on speaker/lights/vibrate of new message
                        if (RadioMSG.currentview != RadioMSG.SMSVIEW) {
                            String msgToast = "Received from: " + mMessage.from
                                    + (mMessage.to.equals("*") ? ", To All" : ", To me") + "\n"
                                    + "Msg: " + RMsgMisc.unescape(mMessage.sms);
                            RadioMSG.longMiddleToastText(msgToast);
                        }
                        //Insert Sound Alarm between transmission of potential beep Acks/RDIS Acks and potential reply. To avoid interference and mis-direction of alarm
                        //if ((config.getPreferenceI("ALARM", 0) & 2) != 0) { //Sound ok?
                        //    soundAlarm();
                        //}
                        Modem.awaitingSoundAlarm = true;
                        //Vibrate?
                        if (matchMyCallWith(mMessage.to, true) && (config.getPreferenceI("ALARM", 0) & 1) != 0) { //Vibrate ok?
                            Vibrator mVibrator = (Vibrator) RadioMSG.myInstance.getSystemService(RadioMSG.myContext.VIBRATOR_SERVICE);
                            //Create "MSG" vibration pattern in morse code
                            int dot = 150;
                            int dash = 375;
                            int shortGap = 250;
                            int mediumGap = 500;
                            int longGap = 1000;
                            long[] vibratePattern = {
                                    0,  //No delay
                                    dash, shortGap, dash,                 // M
                                    mediumGap,
                                    dot, shortGap, dot, shortGap, dot,    // S
                                    mediumGap,
                                    dash, shortGap, dash, shortGap, dot,  // G
                                    longGap
                            };
                            //v.vibrate(1000);
                            mVibrator.vibrate(vibratePattern, -1);
                        }
                        //Alert pattern contained in sms part
                        if (!config.getPreferenceB("UNATTENDEDRELAY", false)) {
                            if (matchMyCallWith(mMessage.to, true) && mMessage.sms.matches("(?i:.*(Alert[:; ,=]).*)")) {
                                final RMsgObject fullMessage = RMsgObject.extractMsgObjectFromFile(DirInbox, mMessage.fileName, true);
                                RadioMSG.myInstance.runOnUiThread(new Runnable() {
                                    public void run() {
                                        //Display the Alert popup window
                                        RadioMSG.myInstance.alertPopup(fullMessage);
                                    }
                                });
                            }
                        }
                        //Do we request to have RSID ack (E.G. for ELWHA Transceiver with no audio feedback)
                        boolean useRsid = (config.getPreferenceB("ACKWITHRSID", false));
                        //Is it an authorized message
                        boolean messageAuthorized = ((config.getPreferenceS("ACCESSPASSWORD", "").length() == 0 && mMessage.crcValid)
                                || (config.getPreferenceS("ACCESSPASSWORD", "").length() > 0 && mMessage.crcValidWithRelayPW));
                        if (mMessage.sms.startsWith("*cmd")) {
                            //Via me as relay OR directed to me without a relay (Direct)
                            if (matchMyCallWith(mMessage.via, false) || (mMessage.via.length() == 0 &&
                                    matchMyCallWith(mMessage.to, false))) {
                                //A scan on/off command? "s on", "s off"
                                //Scan off can also have a duration after which it restarts automatically. E.g. "s off 3h"
                                boolean cmdOk = false;
                                String replyString = "";
                                Pattern psc = Pattern.compile("^\\*cmd\\s((?:s\\son)|(?:s\\soff))(?:\\s(\\d{1,3})\\s([mh]))?$");
                                Matcher msc = psc.matcher(mMessage.sms);
                                if (messageAuthorized && msc.find()) {
                                    /*
                                    String onOff = msc.group(1);
                                    String numberOff = "";
                                    if (msc.group(2) != null) numberOff = msc.group(2);
                                    String units = "";
                                    if (msc.group(3) != null) units = msc.group(3);
                                    if (onOff.equals("s on")) {
                                        RadioMSG.scanningEnabled = true;
                                        RadioMSG.restartScanAtEpoch = 0L;
                                        cmdOk = true;
                                    } else if (onOff.equals("s off") && !numberOff.equals("") && !units.equals("")) {
                                        RadioMSG.scanningEnabled = false;
                                        try {
                                            int durationToRestart = Integer.parseInt(numberOff);
                                            if (units.equals("h")) {
                                                //Time in hours in fact, max 24 hours
                                                durationToRestart = durationToRestart > 24 ? 24 : durationToRestart;
                                                durationToRestart *= 60;  //convert hours to minutes
                                            }
                                            RadioMSG.restartScanAtEpoch = System.currentTimeMillis() + (durationToRestart * 60000);
                                            cmdOk = true;
                                        } catch (Exception e) {
                                            //Bad syntax, re-enable scanning
                                            RadioMSG.scanningEnabled = true;
                                            RadioMSG.restartScanAtEpoch = 0L;
                                            RMsgUtil.sendAcks(mMessage, false);
                                            cmdOk = false;
                                        }
                                    }
                                    */
                                    cmdOk = true;
                                    replyString = "Scanning not implemented";
                                }
                                //A mute/unmute command? "mute", "unmute" to allow/disallow immediate forwarding of SMSs and Emails
                                psc = Pattern.compile("^\\*cmd\\s((?:mute)|(?:unmute))$");
                                msc = psc.matcher(mMessage.sms);
                                if (messageAuthorized && msc.find()) {
                                    String muteUnmute = msc.group(1);
                                    if (muteUnmute.equals("mute")) {
                                        RadioMSG.WantRelayEmailsImmediat = false;
                                        RadioMSG.WantRelaySMSsImmediat = false;
                                        cmdOk = true;
                                    } else if (muteUnmute.equals("unmute")) {
                                        RadioMSG.WantRelayEmailsImmediat = true;
                                        RadioMSG.WantRelaySMSsImmediat = true;
                                        cmdOk = true;
                                    }
                                }
                                //An "unlink" command to unsubscibe this callsign / address combination.
                                // Stops any auto-forwarding or received emails or SMSs for this callsign.
                                // Received emails/SMSs can still be accessed with a QTC request.
                                //E.g. "*cmd unlink vk2eta-2 joemail" OR "*cmd unlink vk2eta-2 joesemailaddress@hisprovider.com"
                                psc = Pattern.compile("^\\*cmd\\sunlink\\s(\\S+)\\s(\\S+)$");
                                String callToUnlink = "";
                                String addressToUnlink = "";
                                msc = psc.matcher(mMessage.sms);
                                if (messageAuthorized && msc.find()) {
                                    callToUnlink = msc.group(1);
                                    addressToUnlink = msc.group(2);
                                    cmdOk = true;
                                    if (removeFilterEntries(addressToUnlink, callToUnlink)) {
                                        replyString = "Unsubscribed for " + addressToUnlink;
                                    } else {
                                        replyString = "Address/Number/Alias not in subscriptions";
                                    }
                                }
                                //Use RSID only if requested and we have a positive ack to send
                                RMsgUtil.sendAcks(mMessage, cmdOk, useRsid && cmdOk);
                                //Reply with acknowledgment message
                                if (!replyString.equals("")) {
                                    RMsgUtil.replyWithText(mMessage, replyString);
                                }
                            }
                        } else if (mMessage.sms.startsWith("*qtc?")) {
                            //QTC via me as relay OR directed to me without a relay (Direct)
                            if (matchMyCallWith(mMessage.via, false) ||
                                    (matchMyCallWith(mMessage.to, false) && mMessage.via.equals(""))) {
                                //boolean directRequest = (matchMyCallWith(mMessage.to, false)
                                //        && mMessage.via.equals(""));
                                //Only properly received (and secured) messages
                                if (messageAuthorized ) {
                                    RMsgUtil.sendAcks(mMessage, true, useRsid);
                                    Boolean resendLast = false;
                                    //Read request (last X minutes or last N messages). Default is last ONE message if nothing is sent.
                                    int numberOf = 1;
                                    if (mMessage.sms.length() > 6 && mMessage.sms.substring(5, 6).equals(" ")) {
                                        try {
                                            //First remove all non digit characters after the "*qtc? "
                                            String extractedStr = mMessage.sms.substring(6).replaceAll("[^0-9]", "");
                                            //Any number following or preceding
                                            numberOf = Integer.valueOf(extractedStr);
                                        } catch (Exception e) {
                                            numberOf = 1;
                                        }
                                    } else if (mMessage.sms.toLowerCase(Locale.US).trim().equals("*qtc?")) {
                                        resendLast = true;
                                    }
                                    //Make sure we have at least 1
                                    numberOf = numberOf == 0 ? 1 : numberOf;
                                    //Any modifier after the numbers like "m" for last X minutes, "h" for last X hours, "d" for last X days,
                                    // "p" for last X position reports only, "a" for last X sms from "all" as in for a third party,
                                    // "e" for last e-Mails (short version), "f" for last e-Mails (fuller version),
                                    // "le" for last e-Mails since last qtc (short version), "lf" for last e-Mails since last qtc (fuller version),
                                    //"r" to force the relay of the "QTC" request. By default the QTC request stops at the relay station as it is supposed to hear everyone
                                    Long forLast = 0L;
                                    Boolean forAll = false;
                                    Boolean emailRequest = false;
                                    Boolean positionsOnly = false;
                                    Boolean forceRelaying = false;
                                    Boolean radioWaveOnly = false;
                                    String extractedStr = "";
                                    if (mMessage.sms.length() > 6) {
                                        extractedStr = mMessage.sms.substring(6).replaceAll("[^a-zA-Z]", "").toLowerCase(Locale.US);
                                        if (extractedStr.startsWith("m")) { //Minutes
                                            forLast = numberOf * 60000L;
                                            numberOf = 0;
                                        } else if (extractedStr.startsWith("h")) { //Hours
                                            forLast = numberOf * 3600000L;
                                            numberOf = 0;
                                        } else if (extractedStr.startsWith("d")) { //Days
                                            forLast = numberOf * 24 * 3600000L;
                                            numberOf = 0;
                                        } else if (extractedStr.startsWith("p")) { //Last X positions
                                            positionsOnly = true;
                                        } else if (extractedStr.equals("l")) { // "L" for last messages since last QTC request
                                            numberOf = -1;
                                        } else if (extractedStr.contains("a")) { //All messages even if for someone else
                                            forAll = true;
                                        } else if (extractedStr.startsWith("e") & config.getPreferenceB("EMAILRELAY", false)) { //E-Mail messages request (short version)
                                            emailRequest = true;
                                        } else if (extractedStr.startsWith("f") & config.getPreferenceB("EMAILRELAY", false)) { //E-Mail messages request (Full(er) version)
                                            emailRequest = true;
                                        } else if (extractedStr.startsWith("le") & config.getPreferenceB("EMAILRELAY", false)) { //E-Mail messages request (short version)
                                            emailRequest = true;
                                            numberOf = -1;
                                        } else if (extractedStr.startsWith("lf") & config.getPreferenceB("EMAILRELAY", false)) { //E-Mail messages request (Full(er) version)
                                            emailRequest = true;
                                            numberOf = -1;
                                        }
                                        if (extractedStr.contains("w")) { //only radio received/transmitted messages (no-email)
                                            radioWaveOnly = true;
                                        }
                                        if (extractedStr.contains("r")) { //Just force relaying this message to the final destination
                                            forceRelaying = true;
                                        }
                                    }
                                    //Are we asked to relay that QTC message to it's destination?
                                    if (forceRelaying && config.getPreferenceB("RADIORELAY", false) && !matchMyCallWith(mMessage.to, false)
                                            && matchMyCallWith(mMessage.via, false)) {
                                        //Remove the "r" from the string and forward the rest of the QTC request as is
                                        extractedStr = mMessage.sms.substring(6).replaceAll("r", "");
                                        //Get full message with binary data
                                        RMsgObject fullMessage = RMsgObject.extractMsgObjectFromFile(DirInbox, mMessage.fileName, true);
                                        //Add relay and remove via information
                                        fullMessage.relay = fullMessage.via;
                                        fullMessage.via = "";
                                        //Relay in the same mode we received in
                                        fullMessage.rxtxMode = mMessage.rxtxMode;
                                        fullMessage.sms = "*qtc? " + extractedStr;
                                        //Send the last 3 digits of the timestamp contained in the file name
                                        // This allows receiving stations to eliminate duplicates (direct Rx and Relayed Rx)
                                        int strLen = mMessage.fileName.length();
                                        fullMessage.timeId = mMessage.fileName.substring(strLen - 7, strLen - 4);
                                        //Wait for the ack to pass first
                                        Thread.sleep(500);
                                        //Then send Message
                                        RMsgTxList.addMessageToList(fullMessage);
                                    } else {
                                        //We are resending from this relay (default). Create blank list
                                        ArrayList<RMsgObject> resendList = new ArrayList();
                                        //What type of qtc did we receive?
                                        if (resendLast) {
                                            //We only want the last transmission (one messages only)
                                            RMsgObject lastOne = getLastTxed(mMessage);
                                            if (lastOne != null) {
                                                resendList.add(lastOne);
                                            }
                                        } else {
                                            //More complex request, start by processing non-email request
                                            if (config.getPreferenceB("RADIORELAY", false) && !emailRequest) {
                                                resendList = buildNonEmailResendList(mMessage, numberOf, forLast, forAll, positionsOnly);
                                            }
                                            //We are repeating existing received messages
                                            // & (Main.WantRelayEmails | Main.WantRelaySMSs | Main.WantRelayOverRadio)
                                            ArrayList<RMsgObject> emailList;
                                            if ((config.getPreferenceB("EMAILRELAY", false) || config.getPreferenceB("SMSSENDRELAY", false))
                                                    && !positionsOnly && !radioWaveOnly) {
                                                emailList = buildEmailResendList(mMessage, numberOf, forLast, extractedStr.endsWith("f"), forAll);
                                                resendList.addAll(emailList);
                                            }
                                            //Do not add if we ask for emails/sms only
                                            if (!emailRequest) {
                                                ArrayList<RMsgObject> TxList;
                                                TxList = buildTXedResendList(mMessage, numberOf, forLast, forAll, positionsOnly);
                                                resendList.addAll(TxList);
                                            }
                                        }
                                        //Now sort and send the list if not zero length
                                        if (resendList.size() > 0) {
                                            Collections.sort(resendList, new Comparator() {
                                                public int compare(Object o1, Object o2) {
                                                    RMsgObject m1 = (RMsgObject) o1;
                                                    RMsgObject m2 = (RMsgObject) o2;
                                                    return m1.fileName.compareToIgnoreCase(m2.fileName);
                                                }
                                            });
                                            //Send requested number of messages with a max of 20 messages. If numberof is <0, we have a time based request, limit to 20 too.
                                            int iMax = (numberOf > 0 && numberOf < 21) ? numberOf : 20;
                                            int count = 0;
                                            int i = 0;
                                            //If we have more than requested, start down the list to send the n most recent messages as they are sorted in increasing date order
                                            if (numberOf > 0 && (resendList.size() > numberOf)) {
                                                i = resendList.size() - numberOf;
                                            }
                                            //Copy up to requested number or in any case max 20
                                            for (; i < resendList.size() && count++ < iMax; i++) { //i is already initialised
                                                RMsgTxList.addMessageToList(resendList.get(i));
                                            }
                                        } else {
                                            //Notify of nothing to send
                                            mMessage.via = ""; //Blank via as it's from this device
                                            RMsgUtil.replyWithText(mMessage, "No messages");
                                        }
                                    }
                                } else {
                                    //Alert with low beep to indicate invalid message
                                    RMsgUtil.sendAcks(mMessage, false, false);
                                    //We have an access password missing
                                    //RMsgUtil.replyWithText(mMessage, "Sorry...Missing Access Password");
                                }
                            }
                        } else if (mMessage.sms.equals("*tim?")) {
                            //Time Sync request
                            if (matchMyCallWith(mMessage.to, false) ||
                                    (matchMyCallWith(mMessage.via, false) && mMessage.to.equals("*"))) {
                                RMsgUtil.sendAcks(mMessage, true, useRsid);
                                //Reply to the requesting station only
                                RMsgUtil.replyWithTime(mMessage);
                            }
                        } else if (matchMyCallWith(mMessage.via, false)) { //Am I asked to Relay messages?
                            //Check that if we received an alias we know what the real address is
                            if (!mMessage.to.contains("**unknown**")) {
                                //Extract final destination  (remove alias details)
                                String toStr = RMsgUtil.getDestinationFromAliasAndDest(mMessage.to);
                                //Looks like an email address? Send via internet as email
                                if (isEmail(toStr)) { //Regex: ^[\w.-]+@\w+\.[\w.-]+
                                    if (messageAuthorized && config.getPreferenceB("EMAILRELAY", false)) {
                                        RMsgUtil.sendAcks(mMessage, true, useRsid);
                                        //Get full message with binary data
                                        RMsgObject fullMessage = RMsgObject.extractMsgObjectFromFile(DirInbox, mMessage.fileName, true);
                                        //remove alias prefix
                                        fullMessage.to = RMsgUtil.getDestinationFromAliasAndDest(fullMessage.to);
                                        //Now update the filter even if not physically sent
                                        updateEmailFilter(fullMessage.to, fullMessage.from);
                                        //Forward properly received messages
                                        final String address = fullMessage.to;
                                        final String subject = "Radio Message from " + fullMessage.from;
                                        final String body = fullMessage.formatForEmail();
                                        final String fullPath;
                                        if (!mMessage.pictureString.equals("")) {
                                            fullPath = RMsgProcessor.HomePath +
                                                    RMsgProcessor.Dirprefix + RMsgProcessor.DirImages +
                                                    RMsgProcessor.Separator + mMessage.fileName.replace(".txt", ".png");
                                        } else {
                                            fullPath = "";
                                        }
                                        //Send email in separate thread as it may take a while and even fail on internet timeout
                                        Thread myThread = new Thread() {
                                            @Override
                                            public void run() {
                                                sendMailMsg(address, subject, body, fullPath);
                                            }
                                        };
                                        myThread.start();
                                    } else if (mMessage.crcValid && config.getPreferenceB("EMAILRELAY", false)) {
                                        //We have an access password missing
                                        RMsgUtil.replyWithText(mMessage, "Access Password?");
                                    } else if (!config.getPreferenceB("EMAILRELAY", false)) {
                                        //Relaying not enabled
                                        RMsgUtil.replyWithText(mMessage, "Relaying Disabled");
                                    } else {
                                        RMsgUtil.sendAcks(mMessage, false, false);
                                    }
                                } else if (isCellular(toStr)) { //Relaying messages as cellular SMS
                                    //At least 8 digits phone number? Send via SMS
                                    //Added +XXXnnnnnnnnnnn country code style
                                    if (messageAuthorized && config.getPreferenceB("SMSSENDRELAY", false)) {
                                        RMsgUtil.sendAcks(mMessage, true, useRsid);
                                        //Get the full message including any picture (as saved on file)
                                        RMsgObject fullMessage = RMsgObject.extractMsgObjectFromFile(DirInbox, mMessage.fileName, true);
                                        //remove alias prefix
                                        fullMessage.to = RMsgUtil.getDestinationFromAliasAndDest(fullMessage.to);
                                        String body = fullMessage.formatForSms();
                                        //Update the entry of cellular number, sender, and time for possible SMS replies over Cellular network
                                        updateSMSFilter(toStr, fullMessage.from);
                                        //Forward properly received messages
                                        sendCellularMsg(toStr, body);
                                    } else if (mMessage.crcValid && config.getPreferenceB("SMSSENDRELAY", false)) {
                                        //We have an access password missing
                                        RMsgUtil.replyWithText(mMessage, "Access Password?");
                                    } else if (!config.getPreferenceB("SMSSENDRELAY", false)) {
                                        //Relaying not enabled
                                        RMsgUtil.replyWithText(mMessage, "Relaying Disabled");
                                    } else {
                                        RMsgUtil.sendAcks(mMessage, false, false);
                                    }
                                } else if (toStr.equals("*") || mMessage.to.length() > 0) {
                                    //To ALL or at least one character call-sign or name? Send over Radio
                                    if (messageAuthorized && config.getPreferenceB("RADIORELAY", false)) {
                                        //Wait a few seconds for the Rx to be fully completed
                                        RMsgUtil.sendAcks(mMessage, true, useRsid);
                                        try {
                                            Thread.sleep(3000);
                                        } catch (Exception e) {
                                            //Nothing
                                        }
                                        //Get full message with binary data
                                        RMsgObject fullMessage = RMsgObject.extractMsgObjectFromFile(DirInbox, mMessage.fileName, true);
                                        //Add relay and remove via information
                                        fullMessage.relay = fullMessage.via;
                                        fullMessage.via = "";
                                        //Relay in the same mode we received in
                                        fullMessage.rxtxMode = mMessage.rxtxMode;
                                        //Send the last 3 digits of the timestamp contained in the file name
                                        // This allows receiving stations to eliminate duplicates (direct Rx and Relayed Rx)
                                        int strLen = mMessage.fileName.length();
                                        fullMessage.timeId = mMessage.fileName.substring(strLen - 7, strLen - 4);
                                        //Then send Message
                                        RMsgTxList.addMessageToList(fullMessage);
                                    } else if (mMessage.crcValid && config.getPreferenceB("RADIORELAY", false)) {
                                        //We have an access password missing
                                        RMsgUtil.replyWithText(mMessage, "Access Password?");
                                    } else if (!config.getPreferenceB("RADIORELAY", false)) {
                                        //Relaying not enabled
                                        RMsgUtil.replyWithText(mMessage, "Relaying Disabled");
                                    } else {
                                        RMsgUtil.sendAcks(mMessage, false, false);
                                    }
                                }
                            } else {
                                //We have an unknown alias, reply with a warning
                                mMessage.via = ""; //Blank "via" information are we, the relay, reply directly to the sender
                                RMsgUtil.replyWithText(mMessage, "I don't know who \""
                                        + mMessage.to.replace("=**unknown**", "")
                                        + "\" is. Send full Alias details.");
                            }
                            //When re-sent may not be in the beginning
                            // } else if (mMessage.sms.startsWith("*pos?")) { //Position request?
                        } else if (mMessage.sms.contains("*pos?")) { //Position request?
                            //For me, and ONLY for me? (positions requests from ALL are not allowed)
                            if (matchMyCallWith(mMessage.to, false)) {
                                if (mMessage.via.length() == 0) {
                                    //Wait a few seconds for the Rx to be fully completed
                                    RMsgUtil.sendAcks(mMessage, true, useRsid);
                                    //NO VIA information, sent direct or received via relay, reply ASAP
                                    try {
                                        //Wait 3 seconds for the Rx to be fully completed
                                        Thread.sleep(3000);
                                    } catch (Exception e) {
                                        //Nothing
                                    }
                                    //Reply to ALL unless the requester was an email or an SMS ,
                                    //  and may need to reply via relay station
                                    String replyTo = "*";
                                    if (isCellular(mMessage.from) || isEmail(mMessage.from)) {
                                        replyTo = getAliasFromFullAlias(mMessage.from);
                                    }
                                    RMsgUtil.replyWithPosition("", replyTo, mMessage.relay, mMessage.rxtxMode);
                                }
                            }
                        } else if (mMessage.sms.equals("*snr?")) {
                            //Inquire (His SNR) request
                            if (matchMyCallWith(mMessage.to, false) ||
                                    (matchMyCallWith(mMessage.via, false) && mMessage.to.equals("*"))) {
                                RMsgUtil.sendAcks(mMessage, true, useRsid);
                                //Reply to the requesting station only
                                RMsgUtil.replyWithSNR(mMessage);
                            }
                        } else if (mMessage.sms.toLowerCase(Locale.US).equals("*time reference received*")) {
                            //Time Sync data received, notify
                            if (matchMyCallWith(mMessage.to, false) && RadioMSG.newReferenceTimeReceived) {
                                RMsgUtil.sendAcks(mMessage, true, useRsid);
                                if (RadioMSG.deviceToRealTimeCorrection == 0L) {
                                    RadioMSG.middleToastText("This device clock is the same as " + RadioMSG.refTimeSource + "'s clock\n");
                                } else if (RadioMSG.deviceToRealTimeCorrection < 0L) {
                                    RadioMSG.middleToastText("This device clock is " + (-RadioMSG.deviceToRealTimeCorrection) + " seconds in front of " + RadioMSG.refTimeSource + "'s clock\n");
                                } else {
                                    RadioMSG.middleToastText("This device clock is " + RadioMSG.deviceToRealTimeCorrection + " seconds behind " + RadioMSG.refTimeSource + "'s clock\n");
                                }
                                //Reset source to mean "processed"
                                //RadioMSG.refTimeSource = "";
                            }
                        } else {
                            //Not an action message, send ack as appropriate
                            if (matchMyCallWith(mMessage.to, false)) {
                                //Directed to me and only to me, we can use a single RSID ack if requested
                                RMsgUtil.sendAcks(mMessage, (mMessage.crcValid || mMessage.crcValidWithRelayPW || mMessage.crcValidWithIotPW), useRsid);
                            } else if (matchMyCallWith(mMessage.to, true)) { //Must be to All then
                                //Don't use single RSID ack if to ALL, use beep sequence instead
                                RMsgUtil.sendAcks(mMessage, (mMessage.crcValid || mMessage.crcValidWithRelayPW || mMessage.crcValidWithIotPW), false);
                            }
                        }
                    }
                } catch (
                        Exception e) {
                    loggingclass.writelog("Exception Error in 'processTextMessage' " + e.getMessage(), null, true);
                }
            }
        };
        myThread.start();
    }


    //Check that we have started receiving the picture associated with a text message within the allocated time.

    public static void checkPictureReceptionTimeout() {
        //We have received a text message with expectation of a picture to follow
        //..and we have started the countdown to timeout
        //.. and we haven't started receiving an image
        //..and more than the timeout seconds have passed
        if (lastTextMessage != null &&
                lastMessageEndRxTime != 0L &&
                !pictureRxInTime &&
                (System.currentTimeMillis() - RMsgProcessor.lastMessageEndRxTime > 22000)) {
            //We timed out (the picture RX failed), save the text part of the message
            processTextMessage(lastTextMessage);
            //Reset stored message
            RMsgProcessor.lastTextMessage = null;
            //Reset the time counter
            RMsgProcessor.lastMessageEndRxTime = 0L;
            //Reset timeout flag
            RMsgProcessor.pictureRxInTime = false;
        }
    }


    /**
     * Create or check the necessary folder structure
     */
    public static void handlefolderstructure() {

        //VK2ETA To-Do: add exception here when there is no external storage
        final String defaultPath = RadioMSG.myContext.getExternalFilesDir(null).getAbsolutePath();
        // are we on Linux/Android OR Windows?
        if (File.separator.equals("/")) {
            Separator = "/";
            Dirprefix = "/RadioMSG.files/";
            onWindows = false;
        } else {
            Separator = "\\";
            Dirprefix = "\\RadioMSG.files\\";
            onWindows = true;
        }

        //Only use default path
        HomePath = defaultPath;
        makeDirectories(HomePath);

        //debug
        //Toast toast = Toast.makeText(RadioMSG.myContext, "Default: " + HomePath, Toast.LENGTH_LONG);
        //toast.show();

        /*Choose the location of the base directory if not defined yet (first run or after de-install)
        HomePath = config.getPreferenceS("BASEPATH", "");
        if (HomePath.length() == 0) {
            //if (HomePath.length() != 100) {   //debug test
            AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(RadioMSG.myContext);
            myAlertDialog.setMessage("Please choose the Location for the RadioMSG.files folder. \n\nNavigate to inside the desired folder then press the OK button");
            myAlertDialog.setCancelable(false);
            myAlertDialog.setPositiveButton("Use Default", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //Save new Home Folder
                    SharedPreferences.Editor editor = RadioMSG.mysp.edit();
                    editor.putString("BASEPATH", defaultPath);
                    // Commit the edits!
                    editor.commit();
                    //Make folders and copy forms
                    makeDirectories(defaultPath);
                }
            });
            myAlertDialog.setNegativeButton("Choose Folder", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    FileChooserDialog folderDialog = new FileChooserDialog(RadioMSG.myContext);
                    folderDialog.loadFolder(defaultPath);
                    folderDialog.setFolderMode(true);
                    folderDialog.addListener(onFileSelectedListener);
                    folderDialog.show();
                }
            });
            myAlertDialog.show();
        } else {
            makeDirectories(HomePath);
        }
        */
    }



    /* Not used
    // Handling of selected folder//
    private static FileChooserDialog.OnFileSelectedListener onFileSelectedListener = new FileChooserDialog.OnFileSelectedListener() {
        public void onFileSelected(Dialog source, File file) {
            source.hide();
            //Toast toast = Toast.makeText(RadioMSG.myContext, "Base Folder Selected: " + file.getAbsoluteFile().getAbsolutePath(), Toast.LENGTH_LONG);
            //toast.show();
            //Save new Home Folder
            SharedPreferences.Editor editor = RadioMSG.mysp.edit();
            editor.putString("BASEPATH", file.getAbsoluteFile().getAbsolutePath());
            // Commit the edits!
            editor.commit();
            //Now make the directories if not present and copy forms if at first run
            makeDirectories(file.getAbsoluteFile().getAbsolutePath());
        }
        public void onFileSelected(Dialog source, File folder, String name) {
            source.hide();
            Toast toast = Toast.makeText(RadioMSG.myContext, "ERROR: Should not ask to create file: " + folder.getName() + "/" + name, Toast.LENGTH_LONG);
            toast.show();
        }
    };



	private static void deleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            deleteRecursive(child);
	    fileOrDirectory.delete();
	}
     */

    private static void makeDirectories(String newHomePath) {
        try {

            //If different than default, make sure the default is removed first if present
            //if (!HomePath.equals(newHomePath)) {
            //File HomePathFile = new File(HomePath + Dirprefix);
            //Toast toast = Toast.makeText(RadioMSG.myContext, "Please remove the old Folder: " + HomePath + Dirprefix, Toast.LENGTH_LONG);
            //toast.show();
            //Not used: check permission and security issues if the sdcards is seletected
            //deleteRecursive(HomePathFile);
            //}

            //Then save the new path
            HomePath = newHomePath;

            //Check if base directory exists, create if not
            File baseDir = new File(HomePath + Dirprefix);
            if (!baseDir.isDirectory()) {
                baseDir.mkdir();
            }

            File thisDir;

            //Check if Inbox directory exists, create if not
            thisDir = new File(HomePath + Dirprefix + DirInbox + Separator);
            if (!thisDir.isDirectory()) {
                thisDir.mkdir();
            }

            //Check if Sent directory exists, create if not
            thisDir = new File(HomePath + Dirprefix + DirSent + Separator);
            if (!thisDir.isDirectory()) {
                thisDir.mkdir();
            }

            //Check if Logs directory exists, create if not
            thisDir = new File(HomePath + Dirprefix + DirLogs + Separator);
            if (!thisDir.isDirectory()) {
                thisDir.mkdir();
            }

            //Check if Images directory exists, create if not
            thisDir = new File(HomePath + Dirprefix + DirImages + Separator);
            if (!thisDir.isDirectory()) {
                thisDir.mkdir();
            }

            //Check if Archive directory exists, create if not
            thisDir = new File(HomePath + Dirprefix + DirArchive + Separator);
            if (!thisDir.isDirectory()) {
                thisDir.mkdir();
            }

            //Check if Recordings directory exists, create if not
            thisDir = new File(HomePath + Dirprefix + DirRecordings + Separator);
            if (!thisDir.isDirectory()) {
                thisDir.mkdir();
            }

            //Check if Temp directory exists, create if not
            thisDir = new File(HomePath + Dirprefix + DirTemp + Separator);
            if (!thisDir.isDirectory()) {
                thisDir.mkdir();
            }
            // Now clears the Temp directory of temporary files
            // This filter only returns files
            FileFilter fileFilter = new FileFilter() {
                public boolean accept(File file) {
                    return file.isFile();
                }
            };
            //Build array of strings containing the file names
            File[] files = thisDir.listFiles(fileFilter);
            //Delete
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }

            //   the HAVERUNONCEBEFORE flag is not set in the preferences
            Boolean haveRunOnceBefore = config.getPreferenceB("HAVERUNONCEBEFORE", false);
            if (!haveRunOnceBefore) {

                //Do something once on first run here
                //?????

                //Now save the flag that we have run once before
                SharedPreferences.Editor editor = RadioMSG.mysp.edit();
                editor.putBoolean("HAVERUNONCEBEFORE", true);
                // Commit the edits!
                editor.commit();
            }
        } catch (Exception ex) {
            loggingclass.writelog("Problem when handling RadioMSG folder structure.", ex, true);
        }
    }


    private static void handleinitialization() {

        try {
            // Initialize send queue
            ModemPreamble = config.getPreferenceS("MODEMPREAMBLE", "");
            ModemPostamble = config.getPreferenceS("MODEMPOSTAMBLE", "");
        } catch (Exception e) {
            loggingclass.writelog("Problems with config parameter.", e, true);
        }
    }


    static double decayaverage(double oldaverage, double newvalue, double factor) {

        double newaverage = oldaverage;
        if (factor > 1) {
            newaverage = (oldaverage * (1 - 1 / factor)) + (newvalue / factor);
        }
        return newaverage;
    }


    //Return current time as a String
    static String myTime() {
        // create a java calendar instance
        Calendar calendar = Calendar.getInstance();

        // get a java.util.Date from the calendar instance.
        // this date will represent the current instant, or "now".
        java.util.Date now = calendar.getTime();

        // a java current time (now) instance
        java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(now.getTime());

        return currentTimestamp.toString().substring(0, 16);
    }

    //Logging
    static void log(String logtext) {
        //           File consolelog = new File (HomePath + Dirprefix + "logfile");
        try {
            // Create file
            FileWriter logstream = new FileWriter(HomePath + Dirprefix + "logfile", true);
            BufferedWriter out = new BufferedWriter(logstream);

            out.write(myTime() + " " + logtext + "\n");
            //Close the output stream
            out.close();

        } catch (Exception e) {//Catch exception if any
            loggingclass.writelog("LogError " + e.getMessage(), null, true);
        }
        RMsgProcessor.PostToTerminal(myTime() + " " + logtext + "\n");
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // Nothing here, not used
        return null;
    }


}

