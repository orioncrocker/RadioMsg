/*
 * Message.java  
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
 *
 */


package com.RadioMSG;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.graphics.Bitmap;


public class RMsgUtil {




    public static void addEntryToLog(String entry) {
        String logFileName = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + RMsgProcessor.DirLogs +
                RMsgProcessor.Separator + RMsgProcessor.messageLogFile;
        File logFile = new File(logFileName);
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                loggingclass.writelog("IO Exception Error in Create file in 'addEntryToLog' " + e.getMessage(), null, true);
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(entry);
            buf.newLine();
            buf.flush();
            buf.close();
        }
        catch (IOException e)
        {
            loggingclass.writelog("IO Exception Error in add line in 'addEntryToLog' " + e.getMessage(), null, true);
        }
    }



    public static boolean saveMessageAsFile(String filePath, String fileName, String dataString) {
        FileWriter out = null;
        File fileToSave = new File(filePath + fileName);
        try
        {
            if (fileToSave.exists()) {
                fileToSave.delete();
            }
            out = new FileWriter(fileToSave, true);
            out.write(dataString);
            out.close();
            //Update the serial number used for the file name
            if (config.getPreferenceB("SERNBR_FNAME", true)) {
                int serNbr = config.getPreferenceI("SERNBR", 1);
                config.setPreferenceS("SERNBR", Integer.toString(++serNbr));
            }
            RadioMSG.topToastText("\nSaved file: " + fileName + "\n");
            //addEntryToLog(RMsgUtil.dateTimeStamp() + ": Saved RMsgUtil file " + fileName);
        }
        catch (IOException e)
        {
            loggingclass.writelog("Error creating file", e, true);
            return false;
        }
        return true;
    }



    //Save a datastring into a file as specified
    public static boolean saveDataStringAsFile(String filePath, String fileName, String dataString) {
        FileWriter out = null;
        File fileToSave = new File(filePath + fileName);
        try
        {
            if (fileToSave.exists()) {
                fileToSave.delete();
            }
            out = new FileWriter(fileToSave, true);
            out.write(dataString);
            out.close();
            RadioMSG.topToastText("\nSaved file: " + fileName + "\n");
            //addEntryToLog(RMsgUtil.dateTimeStamp() + ": Saved file " + fileName);
        }
        catch (IOException e)
        {
            loggingclass.writelog("Error creating file", e, true);
            return false;
        }
        return true;
    }



    //Delete file
    public static boolean deleteFile(String mFolder, String fileName, boolean adviseDeletion) {

        String fullFileName = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + mFolder + RMsgProcessor.Separator + fileName;
        File n = new File(fullFileName);
        if (!n.isFile()) {
            RadioMSG.middleToastText("Message file " + fileName + " Not Found. Deleted?");
            return false;
        } else {
            n.delete();
            if (adviseDeletion) {
                RadioMSG.middleToastText("Message Deleted...");
                //addEntryToLog(RMsgUtil.dateTimeStamp() + ": Deleted file " + fileName);
            }
            return true;
        }
    }



    //Copy binary or text files from one folder to another CONSERVING THE NAME and CONDITIONALLY LOGGING THE ACTION
    public static boolean copyAnyFile(String originFolder, String fileName, String destinationFolder, boolean adviseCopy) {

        File dir = new File(RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + destinationFolder);
        if (dir.exists()) {
            String fullFileName = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + originFolder + RMsgProcessor.Separator + fileName;
            File mFile = new File(fullFileName);
            if (!mFile.isFile()) {
                RMsgProcessor.PostToTerminal("File " + fileName + " not found in " + destinationFolder + "\n");
                return false;
            } else {
                FileOutputStream fileOutputStrm = null;
                FileInputStream fileInputStrm = null;
                try {
                    fileInputStrm = new FileInputStream(fullFileName);
                    String fullDestinationFileName = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + destinationFolder + RMsgProcessor.Separator + fileName;
                    fileOutputStrm = new FileOutputStream(fullDestinationFileName);
                    byte[] mBytebuffer = new byte[256];
                    int byteCount = 0;
                    while ((byteCount = fileInputStrm.read(mBytebuffer)) != -1) {
                        fileOutputStrm.write(mBytebuffer, 0, byteCount);
                    }
                }
                catch (FileNotFoundException e) {
                    RMsgProcessor.PostToTerminal("File not found: " + fullFileName + "\n");
                }
                catch (IOException e) {
                    RMsgProcessor.PostToTerminal("Error copying: " + fileName + " " + e + "\n");
                }
                finally {
                    try {
                        if (fileInputStrm != null) {
                            fileInputStrm.close();
                        }
                        if (fileOutputStrm != null) {
                            fileOutputStrm.close();
                        }
                    }
                    catch (IOException e) {
                        RMsgProcessor.PostToTerminal("File close error: " + e + "\n");
                    }
                }
                //if (adviseCopy)
                //    addEntryToLog(RMsgUtil.dateTimeStamp() + ": Copied file " + fileName + " to " + destinationFolder);
                return true;
            }
        } else {
            RMsgProcessor.PostToTerminal("Directory not found: " + destinationFolder + "\n");
            return false;
        }
    }


    //Copy binary or text files from one folder to another WHILE CHANGING THE NAME
    public static boolean copyAnyFile(String originFolder, String fileName,
                                      String destinationFolder, String newFileName) {
        boolean result = true;

        File dir = new File(RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + destinationFolder);
        if (dir.exists()) {
            String fullFileName = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + originFolder + RMsgProcessor.Separator + fileName;
            File mFile = new File(fullFileName);
            if (!mFile.isFile()) {
                result = false;
            } else {
                FileOutputStream fileOutputStrm = null;
                FileInputStream fileInputStrm = null;
                try {
                    fileInputStrm = new FileInputStream(fullFileName);
                    String fullDestinationFileName = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + destinationFolder + RMsgProcessor.Separator + newFileName;
                    fileOutputStrm = new FileOutputStream(fullDestinationFileName);
                    byte[] mBytebuffer = new byte[256];
                    int byteCount = 0;
                    while ((byteCount = fileInputStrm.read(mBytebuffer)) != -1) {
                        fileOutputStrm.write(mBytebuffer, 0, byteCount);
                    }
                }
                catch (FileNotFoundException e) {
                    RMsgProcessor.PostToTerminal("File not found: " + fullFileName + "\n");
                    result = false;
                }
                catch (IOException e) {
                    RMsgProcessor.PostToTerminal("Error copying: " + fileName + " " + e + "\n");
                    result = false;
                }
                finally {
                    try {
                        if (fileInputStrm != null) {
                            fileInputStrm.close();
                        }
                        if (fileOutputStrm != null) {
                            fileOutputStrm.close();
                        }
                    }
                    catch (IOException e) {
                        RMsgProcessor.PostToTerminal("File close error: " + e + "\n");
                        result = false;
                    }
                }
            }
        } else {
            RMsgProcessor.PostToTerminal("Directory not found: " + destinationFolder + "\n");
            result = false;
        }
        //if (result) addEntryToLog(RMsgUtil.dateTimeStamp() + ": Copied file " + newFileName + " to " + destinationFolder);
        return result;
    }



    public static double Round(double Rval, int Rpl) {
        double p = (double)Math.pow(10,Rpl);
        Rval = Rval * p;
        double tmp = Math.round(Rval);
        return tmp/p;
    }



    //Queues a GPS position fix and sends position
    public static void sendPosition(String smsMessage) {
        long positionRequestTime = System.currentTimeMillis();

        RMsgTxList.addMessageToList (RadioMSG.selectedTo, RadioMSG.selectedVia, smsMessage,
                true, null, positionRequestTime,
                null);
        //Now request fast periodic updates
        //The cancellation of these requests is done on first receipt of a location fix
        RadioMSG.myInstance.requestQuickGpsUpdate();
    }



    //Queues a GPS position fix and sends position
    public static void replyWithPosition(String from, String to, String via, String rxMode) {

        long positionRequestTime = System.currentTimeMillis();

        //RMsgTxList.addMessageToList (to, via, "",
        //       true, null, positionRequestTime,
        //       null);

        RMsgObject replyMessage = new RMsgObject();
        //Reply in the same mode as the request
        replyMessage.rxtxMode = rxMode;
        String ccirFrom = from.equals("") ? config.getPreferenceS("SELCALL").trim() : from;
        replyMessage.from = rxMode.equals("CCIR493") ? ccirFrom : RMsgProcessor.getCall();
        replyMessage.to = to;
        replyMessage.via = via;
        replyMessage.msgHasPosition = true;
        replyMessage.positionRequestTime = positionRequestTime;
        //Queue
        RMsgTxList.addMessageToList (replyMessage);
        //Now request fast periodic updates
        //The cancellation of these requests is done on first receipt of a location fix
        RadioMSG.myInstance.requestQuickGpsUpdate();
    }


    //Queues a message with a reply text
    public static void replyWithText(RMsgObject mMessage, String replyText) {

        RMsgObject replyMessage = new RMsgObject();
        replyMessage.sms = replyText;
        //Reply in the same mode as the request
        replyMessage.rxtxMode = mMessage.rxtxMode;
        replyMessage.from = config.getPreferenceS("CALL", "N0CAL").trim();
        replyMessage.to = mMessage.from;
        replyMessage.via = mMessage.via;
        RMsgTxList.addMessageToList(replyMessage);
    }


    //Queues a message with this device's time reference
    public static void replyWithTime(RMsgObject mMessage) {

        RMsgObject replyMessage = new RMsgObject();
        //Reply in the same mode as the request
        replyMessage.rxtxMode = mMessage.rxtxMode;
        replyMessage.from = config.getPreferenceS("CALL", "N0CAL").trim();
        replyMessage.to = mMessage.from;
        replyMessage.via = "";
        replyMessage.sendMyTime = true;
        RMsgTxList.addMessageToList(replyMessage);
    }


    //Queues a message with this device's time reference
    public static void replyWithSNR(RMsgObject mMessage) {

        RMsgObject replyMessage = new RMsgObject();
        //Reply in the same mode as the request
        replyMessage.rxtxMode = mMessage.rxtxMode;
        replyMessage.from = config.getPreferenceS("CALL", "N0CAL").trim();
        replyMessage.to = mMessage.from;
        replyMessage.via = mMessage.relay;
        replyMessage.sendMyTime = false;
        replyMessage.sms = (int)Modem.blockSNR + "%";
        RMsgTxList.addMessageToList(replyMessage);
    }


    //Extract the destination from an "alias=destination" To address field
    public static String extractDestination(String toAliasAndDestination) {

        Pattern psc = Pattern.compile("^\\s*(.+)\\s*=(.*)\\s*$");
        Matcher msc = psc.matcher(toAliasAndDestination);
        if (msc.lookingAt()) {
            if (!msc.group(2).equals("")) {
                return msc.group(2);
            }
        }
        //We must have a strait callsign address, return as-is
        return toAliasAndDestination;
    }


    //Extract the alias from an "alias=destination" address field, return string in "alias=" format
    public static String extractAliasOnly(String toAliasAndDestination) {

        Pattern psc = Pattern.compile("^\\s*(.+)\\s*=(.*)\\s*$");
        Matcher msc = psc.matcher(toAliasAndDestination);
        if (msc.lookingAt()) {
            if (!msc.group(1).equals("")) {
                //Found, return "alias="
                return msc.group(1) + "=";
            }
        }
        //No alias, return blank
        return "";
    }


    //Extract the destination from an "alias=destination" To address field
    public static String getDestinationFromAliasAndDest(String toAliasAndDestination) {

        Pattern psc = Pattern.compile("^\\s*(.+)\\s*=(.*)\\s*$");
        Matcher msc = psc.matcher(toAliasAndDestination);
        if (msc.lookingAt()) {
            if (!msc.group(2).equals("")) {
                return msc.group(2);
            }
        }
        //We must have a strait callsign or email or SMS address without an alias, return as-is
        return toAliasAndDestination;
    }


    //Takes the temp bitmap and creates a byte array ready for TX by MFSK
    public static byte[] extractPictureArray(Bitmap myBitmap, int scramblingMode) {
        String attachmentBuffer;

        try {
            //Propose GC at this point as the ByteBuffer allocation is memory intensive
            System.gc();
            //For resizing
            //Bitmap.createScaledBitmap(yourBitmap, 50, 50, true); // Width and Height in pixel e.g. 50
            //Size of buffer containing the Bitmap. Three bytes for R, G and B, and one for Alpha
            int attachedPictureWidth = myBitmap.getWidth();
            int attachedPictureHeight = myBitmap.getHeight();
            int pictureArraySize =  attachedPictureWidth *
                    attachedPictureHeight * 4;
            //Extract RGB array from Bitmap
            ByteBuffer byteBuffer = ByteBuffer.allocate(pictureArraySize);
            myBitmap.copyPixelsToBuffer(byteBuffer);
            //Blank it out as we are finished with it
            //NO, Keep it in case we press send again
            // RadioMSG.tempImageBitmap = null;
            //Possible future action includes savings as-is ready for re-send
            System.gc();
            byte[] attachedPicture = new byte[pictureArraySize];
            byteBuffer.rewind();
            byteBuffer.get(attachedPicture);
            byteBuffer = null;
            System.gc();
            //If scrambled, re-arrange data at that point
            if (scramblingMode == 1) {
                byte[] scrambledPicture = new byte[pictureArraySize];
                //Get UNIQUE pseudo random sequence: EACH value between 0 and (pictureSize - 1) MUST be returned ONCE in the sequence)
                Random randomPic = new Random(12345678);
                //Arrays.setAll(indices, i -> i); //Can't use as requires min API of 24
                int pixelCount = pictureArraySize / 4;
                Integer[] indices = new Integer[pixelCount];
                for (int i = 0; i < pixelCount; i++) {
                    indices[i] = i;
                }
                List<Integer> shuffledList = Arrays.asList(indices);
                Collections.shuffle(Arrays.asList(indices), randomPic);
                //Skip for now ..... Collections.shuffle(shuffledList, randomPic);
                //Collections.shuffle(shuffledList);
                indices = (Integer[]) shuffledList.toArray();
                //Can't use this line below as requires min API of 24
                //return Arrays.stream(indices).mapToInt(Integer::intValue).toArray();
                int j;
                int scrambledIndex;
                for (int i = 0; i < pixelCount; i++) {
                    //Move all 4 bytes of RGBA together for now
                    j = i * 4;
                    scrambledIndex = indices[i] * 4;
                    scrambledPicture[j] = attachedPicture[scrambledIndex];
                    scrambledPicture[j + 1] = attachedPicture[scrambledIndex + 1];
                    scrambledPicture[j + 2] = attachedPicture[scrambledIndex + 2];
                    //Ignore alpha, not used???
                    scrambledPicture[j + 3] = attachedPicture[scrambledIndex + 3];
                }
                return scrambledPicture;
            } else {
                return attachedPicture;
            }
        }
        catch (Exception e)
        {
            //Invalid bitmap data
            loggingclass.writelog("General Exception Error in 'extractPictureArray' " + e.getMessage(), null, true);
            return null;
        }
    }


    //Takes the picture int array and de-scramble
    public static int[] descramblePictureArray(int attachedPictureWidth, int attachedPictureHeight, int[] scrambledArray, int scramblingMode) {
        String attachmentBuffer;

        try {
            //Propose GC at this point as the ByteBuffer allocation is memory intensive
            System.gc();
            //Size of buffer containing the Bitmap. One Integer per pixel, packed with 8 bits shifted for R, G and B
            int pixelCount =  attachedPictureWidth *
                    attachedPictureHeight;
            //If scrambled, re-arrange data at that point
            if (scramblingMode == 1) {
                int[] deScrambledPicture = new int[pixelCount];
                //Get UNIQUE pseudo random sequence: EACH value between 0 and (pictureSize - 1) MUST be returned ONCE in the sequence)
                Random randomPic = new Random(12345678);
                //Arrays.setAll(indices, i -> i); //Can't use as requires min API of 24
                Integer[] indices = new Integer[pixelCount];
                for (int i = 0; i < pixelCount; i++) {
                    indices[i] = i;
                }
                List<Integer> shuffledList = Arrays.asList(indices);
                //Collections.shuffle(Arrays.asList(indices), randomPic);
                Collections.shuffle(shuffledList, randomPic);
                //Collections.shuffle(shuffledList);
                indices = (Integer[]) shuffledList.toArray();
                //Can't use this line below as requires min API of 24
                //return Arrays.stream(indices).mapToInt(Integer::intValue).toArray();
                //Check for completeness and uniqueness
                int[] testArray = new int[pixelCount];
                for (int z = 0; z < pixelCount; z++) {
                    testArray[z] = -1;
                }
                for (int z = 0; z < pixelCount; z++) {
                    testArray[indices[z]] = z;
                }
                int blankcount = 0; //number of un-allocated indices[] cells
                for (int z = 0; z < pixelCount; z++) {
                    if (testArray[z] == -1) blankcount++;
                }
                //End test completeness now test uniqueness
                testArray = new int[pixelCount];
                for (int z = 0; z < pixelCount; z++) {
                    testArray[z] = -1;
                }
                int duplicates = 0; //number of un-allocated indices[] cells
                for (int z = 0; z < pixelCount; z++) {
                    if (testArray[indices[z]] == -1) {
                        testArray[indices[z]] = indices[z];
                    } else {
                        duplicates++;
                    }
                }
                int k = duplicates + blankcount;
                for (int i = 0; i < pixelCount; i++) {
                    deScrambledPicture[indices[i]] = scrambledArray[i];
                }
                return deScrambledPicture;
            } else {
                return scrambledArray;
            }
        }
        catch (Exception e)
        {
            //Invalid bitmap data
            loggingclass.writelog("General Exception Error in 'descramblePictureArray' " + e.getMessage(), null, true);
            return null;
        }
    }





    //Takes a directory for the data file, a data file name
    //Returns the text content of the file
    public static String readFile(String mDir, String mFileName) {
        String dataString = new String();
        String readString = new String();

        try
        {
            //First separate the header from the data fields (Headers are never compressed)
            //For this read the data file until we find the form information
            File fi = new File(RMsgProcessor.HomePath + RMsgProcessor.Dirprefix
                    + mDir + RMsgProcessor.Separator + mFileName);
            FileInputStream fileISi = new FileInputStream(fi);
            BufferedReader buf = new BufferedReader(new InputStreamReader(fileISi));
            dataString = "";
            boolean found_form = false;
            //Handles both hard-coded forms and custom forms
            while ((readString = buf.readLine()) != null)
            {
                dataString += readString + "\n";
            }
        }
        catch (FileNotFoundException e)
        {
            RadioMSG.middleToastText("Message file " + mFileName + " Not Found. Deleted?");
        }
        catch (IOException e)
        {
            loggingclass.writelog("IO Exception Error in 'readFile' " + e.getMessage(), null, true);
        }
        return dataString;
    }



    // Creates an array of flmsg smsview file names
    public static String[] createFileListFromFolder(String mFolder)
    {
        String[] fileNamesArray = null;
        try
        {
            // Get the list of files in the designated folder
            File dir = new File(RMsgProcessor.HomePath + RMsgProcessor.Dirprefix
                    + mFolder);
            File[] files = dir.listFiles();
            FileFilter fileFilter = new FileFilter() {
                public boolean accept(File file)
                {
                    return file.isFile();
                }
            };

            // We should now have an array of strings containing the file names
            files = dir.listFiles(fileFilter);
            if (files.length > 0) {
                fileNamesArray = new String[files.length] ;
                for (int i = 0; i < files.length; i++)
                {
                    fileNamesArray[i] = files[i].getName();
                }
            }
        }
        catch (Exception e)
        {
            loggingclass.writelog("Error when listing Folder: " + mFolder + "\nDetails: ", e, true);
        }
        return fileNamesArray;
    }


    //Send a single beep or double beep by sending a Tune command to the modem for short periods of time
    public static void sendAcks(final RMsgObject ackMessage, final boolean positiveAck, final boolean useRSID) {

        //In a separate thread
        Thread myThread = new Thread() {
            @Override
            public void run() {
                try {
                    //Simple Bip (later can be RSID or Codan Ack)
                    //store current rx mode to determine response
                    int ackPosition = config.getPreferenceI("ACKPOSITION", 0);
                    boolean onlyForAlerts = config.getPreferenceB("ACKONLYALERTS", false);
                    if (ackPosition > 0 || ackPosition == -99) {
                        if ((!onlyForAlerts || ackMessage.sms.matches("(?i:.*(Alert[:; ,=]).*)"))
                                && !(ackMessage.to.equals("*") &&
                                (ackMessage.rxtxMode.equals("CCIR476") || ackMessage.rxtxMode.equals("CCIR493")))) {
                            Modem.sendToneSequence(ackPosition, ackMessage.rxtxMode, !positiveAck, useRSID);
                        }
                    }


                } catch (Exception e) {
                    loggingclass.writelog("Exception Error in 'sendAcks' " + e.getMessage(), null, true);
                }
            }
        };
        myThread.start();
    }


    // Compare two strings for equality, ignoring case
    public static Boolean stringsEqualsCaseUnsensitive(String str1, String str2) {

        return str1.toLowerCase(Locale.US).equals(str2.toLowerCase(Locale.US));
    }


    // Compare two strings for "StartsWith", ignoring case
    public static Boolean stringsStartsWithCaseUnsensitive(String str1, String str2) {

        return str1.toLowerCase(Locale.US).startsWith(str2.toLowerCase(Locale.US));
    }


    // Compare two strings for "Contains", ignoring case
    public static Boolean stringsContainsCaseUnsensitive(String str1, String str2) {

        return str1.toLowerCase(Locale.US).contains(str2.toLowerCase(Locale.US));
    }




}
