package com.RadioMSG;

import android.graphics.Bitmap;
import android.location.Location;
import android.text.format.Time;

import java.text.DecimalFormat;
import java.util.LinkedList;

import static java.lang.Math.abs;

/**
 * Created by jdouyere on 31/12/16.
 *
 * Message objects list for gps position updates (when available) and TXing
 */

public class RMsgTxList {

    public static LinkedList messageList = new LinkedList();


    //Get length of message list
    synchronized public static int getLength() {
        int notYetSent = 0;
        RMsgObject messageObject = null;
        int listLength = messageList.size();

        //Iterate through the list and count only the locations with updated gps coordinates
        for (int ii=0; ii < listLength; ii++) {
            messageObject = (RMsgObject) messageList.get(ii);
            if (!messageObject.sent) {
                notYetSent++;
            }
        }

        return notYetSent;
    }



    //Get length of message list
    synchronized public static int getAvailableLength() {
        int availableforTx = 0;
        RMsgObject messageObject = null;
        int listLength = messageList.size();

        //Iterate through the list and count only the locations with updated gps coordinates AND in the selected TX minute
        long nowInMilli = System.currentTimeMillis();
        Time mytime = new Time();
        //now +/- GPS correction if any + 5 seconds delay to ensure the Radio has changed frequency
        mytime.set(nowInMilli + (RadioMSG.deviceToRealTimeCorrection * 1000) - 5000L);
        int thisTxMinute = mytime.minute % 5; //5 minutes cycle repeated
        for (int ii=0; ii < listLength; ii++) {
            messageObject = (RMsgObject) messageList.get(ii);
            if ((!messageObject.msgHasPosition || (messageObject.msgHasPosition && messageObject.position != null))
                    && !messageObject.sent
                    && (RadioMSG.selectedTxMinute == -1 || (RadioMSG.selectedTxMinute == thisTxMinute
                    && mytime.second < 50))) { //Not within the last 5 seconds of the minute remembering that this time is 5 seconds behind
                availableforTx++;
            }
        }
        return availableforTx;
    }



    //Get last message of list (the oldest in the FIFO queue)
    //Must be consistent in regards to gps location information
    // Therefore ignore objects that are awaiting location updates
    synchronized public static RMsgObject getOldest() {
        RMsgObject messageObject = null;
        int listLength = messageList.size();

        if (listLength > 0) {
            //Iterate through the list and count only the locations with updated gps coordinates AND in the selected TX minute
            long nowInMilli = System.currentTimeMillis();
            Time mytime = new Time();
            //now +/- GPS correction if any + 5 seconds delay to ensure the Radio has changed frequency
            mytime.set(nowInMilli + (RadioMSG.deviceToRealTimeCorrection * 1000) - 5000L);
            int thisTxMinute = mytime.minute % 5; //5 minutes cycle repeated
            for (int ii=listLength - 1; ii >= 0; ii--) {
                messageObject = (RMsgObject) messageList.get(ii);
                if ((!messageObject.msgHasPosition || (messageObject.msgHasPosition && messageObject.position != null))
                        && !messageObject.sent
                        && (RadioMSG.selectedTxMinute == -1 || (RadioMSG.selectedTxMinute == thisTxMinute
                        && mytime.second < 50))) {
                    //Mark the object as sent as this is called from the send function
                    messageObject.sent = true;
                    messageList.set(ii, messageObject);
                    return messageObject;  //.remove(ii);
                }
            }
        }
        return null;
    }

    //Get last message of list (the oldest in the FIFO queue) which has been marked as SENT
    synchronized public static RMsgObject getYoungestSent(String sentFilename) {
        RMsgObject messageObject = null;
        int listLength = messageList.size();

        if (listLength > 0) {
            //Iterate through the list and return the oldest item marked as sent
            for (int ii = 0; ii < listLength; ii++) {
                messageObject = (RMsgObject) messageList.get(ii);
                if (messageObject.sent) {
                    messageObject.fileName = sentFilename;
                    messageList.set(ii, messageObject);
                    return messageObject;
                }
            }
        }
        return null;
    }


    synchronized public static RMsgObject removeYoungestSent() {
        RMsgObject messageObject = null;
        int listLength = messageList.size();

        if (listLength > 0) {
            //Iterate through the list and return the oldest item marked as sent
            for (int ii = 0; ii < listLength; ii++) {
                messageObject = (RMsgObject) messageList.get(ii);
                if (messageObject.sent) {
                    return (RMsgObject) messageList.remove(ii);
                }
            }
        }
        return null;
    }



    //Get (and remove) the earliest message in the list (the youngest in the FIFO queue)
    // Includes objects that are awaiting location updates
    synchronized public static RMsgObject getLatest() {

        int listLength = messageList.size();

        if (listLength > 0) {
            return (RMsgObject) messageList.remove(0);
        }
        return null;
    }



    //Iterate through the list and update the locations with currently
    // passed location if the boolean msgHasPosition is true AND the location is null
    synchronized public static void updateLocation(Location msgLocation) {
        RMsgObject messageObject = null;
        int listLength = messageList.size();

        //Iterate through the list and update the locations with currently
        // passed location if the boolean msgHasPosition is true AND the location is null
        for (int ii=0; ii < listLength; ii++) {
            messageObject = (RMsgObject) messageList.get(ii);
            if (messageObject.msgHasPosition && messageObject.position == null) {
                messageObject.positionAge = (int)((System.currentTimeMillis() - messageObject.positionRequestTime) / 1000);
                messageObject.position = msgLocation;
                messageList.set(ii, messageObject);
            }
        }
    }



    //Without image data
    synchronized public static void addMessageToList(String msgTo, String via, String msgSms,
                                                          Boolean msgHasPosition, Location msgLocation, long positionRequestTime,
                                                          Short[] msgVoiceMessage) {

        RMsgObject messageObject = new RMsgObject(msgTo, via, msgSms, null, 0, false, 0, msgHasPosition,
                msgLocation, positionRequestTime, msgVoiceMessage);
        messageObject.from = RMsgProcessor.getCall();
        messageList.addFirst(messageObject);
    }



    //With image data
    //Added flag for scambled (pseudo random) image sending
    synchronized public static void addMessageToList(String msgTo, String via, String msgSms,
                                                          Bitmap msgPicture, int pictureTxSPP, boolean pictureColour, int imageTxModemIndex,
                                                          Boolean msgHasPosition, Location msgLocation, long positionRequestTime, int scramblingdMode) {
        RMsgObject messageObject = null;

        messageObject = new RMsgObject(msgTo, via, msgSms,
                msgPicture, pictureTxSPP, pictureColour, imageTxModemIndex,
                msgHasPosition, msgLocation, positionRequestTime, null);
        messageObject.from = RMsgProcessor.getCall();
        messageObject.scramblingMode = scramblingdMode;
        messageList.addFirst(messageObject);
    }



    //Add pre-created message object (e.g when forwarding)
    synchronized public static void addMessageToList(RMsgObject mMessage) {

        messageList.addFirst(mMessage);
    }



}
