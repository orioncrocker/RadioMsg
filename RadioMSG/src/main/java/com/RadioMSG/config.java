/*
 * config.java
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.Map;

public class config {


    /**
     * @param Key
     * @return
     */
    public static String getPreferenceS(String Key) {
        String myReturn = "";

        try {
            myReturn = RadioMSG.mysp.getString(Key, "");
        } catch (Exception e) {
            myReturn = "";
        }
        return myReturn;
    }

    /**
     * Get the saved value, if its not there then use the default value
     *
     * @param Key
     * @param Default
     * @return
     */
    public static String getPreferenceS(String Key, String Default) {
        String myReturn = "";

        try {
            myReturn = RadioMSG.mysp.getString(Key, Default);
            //if (myReturn.equals("")) myReturn = Default;
        } catch (Exception e) {
            myReturn = Default;
        }
        return myReturn;
    }

    //Reads an integer from preferences, with default value
    public static int getPreferenceI(String Key, int Default) {
        int myReturn = 0;
        String myPref = "";

        try {
            myPref = RadioMSG.mysp.getString(Key, "");
            if (myPref.equals("")) {
                myReturn = Default;
            } else {
                //Try integer conversion
                try {
                    myReturn = Integer.parseInt(myPref);
                } catch (NumberFormatException ex) {
                    //Return zero is probably the best logic here since we cannot interact with the user anyway
                    loggingclass.writelog("Cannot convert preference [" + Key + "] to a number" + ex.getMessage(),
                            null, true);
                    myReturn = Default;
                }
            }
        } catch (Exception e) {
            myReturn = Default;
        }
        return myReturn;
    }

    //Reads a double from preferences, with default value
    public static double getPreferenceD(String Key, double Default) {
        double myReturn = 0;
        String myPref = "";

        try {
            myPref = RadioMSG.mysp.getString(Key, "");
            if (myPref.equals("")) {
                myReturn = Default;
            } else {
                //Try double conversion
                try {
                    myReturn = Double.parseDouble(myPref);
                } catch (NumberFormatException ex) {
                    //Return zero is probably the best logic here since we cannot interract with the user anyway
                    loggingclass.writelog("Cannot convert preference [" + Key + "] to a number" + ex.getMessage(),
                            null, true);
                    myReturn = 0.0f;
                }
            }
        } catch (Exception e) {
            //No value entered or no preference not found
            myReturn = Default;
        }
        return myReturn;
    }


    /**
     * @param Key
     * @return
     */
    public static boolean getPreferenceB(String Key) {
        boolean myReturn = false;

        try {
            myReturn = RadioMSG.mysp.getBoolean(Key, false);
        } catch (Exception e) {
            myReturn = false;
        }
        return myReturn;
    }

    /**
     * Get the saved value, if its not there then use the default value
     *
     * @param Key
     * @param Default
     * @return
     */
    public static boolean getPreferenceB(String Key, boolean Default) {
        boolean myReturn = false;

        try {
            myReturn = RadioMSG.mysp.getBoolean(Key, Default);
        } catch (Exception e) {
            myReturn = Default;
        }

        return myReturn;
    }


    /**
     * Sets the passes value into the assed preference Key, if its not there do nothing
     *
     * @param key
     * @param newValue
     * @return
     */
    public static Boolean setPreferenceS(String key, String newValue) {
        Boolean myReturn = true;

        try {
            //store value into preferences
            SharedPreferences.Editor editor = RadioMSG.mysp.edit();
            editor.putString(key, newValue);
            // Commit the edits!
            editor.commit();
        } catch (Exception e) {
            myReturn = false;
        }
        return myReturn;
    }


    //For storing Boolean preferences
    public static boolean setPreferenceB(String pref, boolean flag) {
        Boolean myReturn = true;
        try {
            //store value into preferences
            SharedPreferences.Editor editor = RadioMSG.mysp.edit();
            editor.putBoolean(pref, flag);
            // Commit the edits!
            editor.commit();
        } catch (Exception e) {
            myReturn = false;
        }
        return myReturn;
    }



    private static boolean writePrefFile(File dst) {
        boolean res = false;

        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            output.writeObject(RadioMSG.mysp.getAll());
            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }


    //Backup all preferences to file
    public static boolean saveSharedPreferencesToFile(String fileName, boolean overwrite) {
        boolean res = false;

        String fullFileName = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + fileName;
        final File dst = new File(fullFileName);
        if (dst.exists() && !overwrite) {
            //Ask if we want to override
            AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(
                    RadioMSG.myInstance);
            myAlertDialog.setMessage("Are you sure you want to overwrite the saved preferences?");
            myAlertDialog.setCancelable(false);
            myAlertDialog.setPositiveButton("Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            writePrefFile(dst);
                        }
                    });
            myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            myAlertDialog.show();

        } else {
            //just write it
            writePrefFile(dst);
        }
        return res;
    }


    //Ask if we want to restore values from file
    public static void loadSharedPreferencesFromFile(final String fileName) {

        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(
                RadioMSG.myInstance);
        myAlertDialog.setMessage("Are you sure you want to overwrite the current preferences with the ones in the backup?");
        myAlertDialog.setCancelable(false);
        myAlertDialog.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        doLoadSharedPreferencesFromFile(fileName);
                    }
                });
        myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        myAlertDialog.show();
    }


    //Read backup preference file and restore values
    public static void doLoadSharedPreferencesFromFile(String fileName) {
        final String fullFileName = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + fileName;
        final File src = new File(fullFileName);
        ObjectInputStream input = null;

        SharedPreferences.Editor editor = RadioMSG.mysp.edit();
        try {
            editor.clear();
            input = new ObjectInputStream(new FileInputStream(src));
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();
                if (v instanceof Boolean)
                    editor.putBoolean(key, ((Boolean) v).booleanValue());
                else if (v instanceof Float)
                    editor.putFloat(key, ((Float) v).floatValue());
                else if (v instanceof Integer)
                    editor.putInt(key, ((Integer) v).intValue());
                else if (v instanceof Long)
                    editor.putLong(key, ((Long) v).longValue());
                else if (v instanceof String)
                    editor.putString(key, ((String) v));
            }
            editor.commit();
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            RadioMSG.topToastText("No backup file found.\n\n Use \"Save Preferences\" option first");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public static void restoreSettingsToDefault() {

        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(
                RadioMSG.myInstance);
        myAlertDialog.setMessage("Are you sure you want to Restore the KEY settings to default? \n\n " +
                "Personal Data and pre-set messages will be preserved");
        myAlertDialog.setCancelable(false);
        myAlertDialog.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences.Editor editor = RadioMSG.mysp.edit();

                        //Restore RX and TX RSID in case they were disabled by error
                        editor.putBoolean("RXRSID", true);
                        editor.putBoolean("TXRSID", true);

                        //General and GUI
                        editor.putBoolean("EXPERTMODE", false);
                        editor.putString("APPTHEME", "0");
                        editor.putString("WHICHFOLDERS", "3");
                        editor.putString("ALARM", "0");
                        editor.putString("LASTMODEUSED", "HF-Poor");
                        editor.putBoolean("SHORTPRESSONLIST", false);
                        editor.putBoolean("SHORTPRESSONQUICKSMS", false);
                        //Default Modes for each OPmode
                        int index = 0;
                        for (String thisOpMode : RadioMSG.opModes) {
                            editor.putString(thisOpMode, RadioMSG.defaultModes[index]);
                            index++;
                        }
                        //Modem
                        editor.putBoolean("SLOWCPU", false);
                        editor.putBoolean("IDLEMODEMOFF", true);
                        editor.putString("VOLUME", "8");
                        editor.putString("AFREQUENCY", "1500");
//                        editor.putBoolean("RXRSID", true);
//                        editor.putBoolean("TXRSID", true);
                        editor.putString("RSID_ERRORS", "2");
                        editor.putBoolean("RSIDWIDESEARCH", true);
                        editor.putBoolean("MT63INTEGRATION", false);
                        editor.putBoolean("MT63AT500", true);//Means "Allow Manual Tuning"
                        editor.putBoolean("MT63USETONES", false);
                        editor.putString("MT63TONEDURATION", "4");
                        editor.putBoolean("TXPOSTRSID", false);
                        editor.putString("TUNEDURATION", "4");
                        editor.putString("PRETONEDURATION", "0");
                        //GPS and tracking
                        editor.putBoolean("KEEPGPSWARM", false);
                        editor.putString("GPSINTERVAL", "60");
                        editor.putString("GPSTRACKINGINTERVAL", "2");
                        editor.putString("TRACKINGFIRSTWARNING", "350");
                        editor.putString("TRACKINGSECONDWARNING", "200");
                        editor.putString("TRACKINGTHIRDWARNING", "50");
                        //Relaying
                        editor.putBoolean("UNATTENDEDRELAY", false);
                        editor.putBoolean("RADIORELAY", false);
                        editor.putBoolean("SMSSENDRELAY", false);
                        editor.putBoolean("LISTENFORSMS", false);
                        editor.putBoolean("RELAYRECEIVEDSMS", false);
                        editor.putString("SMSLISTENINGFILTER", "");
                        editor.putString("DAYSTOKEEPLINK", "90");
                        editor.putBoolean("EMAILRELAY", false);
                        editor.putString("RELAYEMAILADDRESS", "");
                        editor.putString("RELAYEMAILPASSWORD", "");

                        //Reset first run flag
                        editor.putBoolean("HAVERUNONCEBEFORE", false);

                        editor.commit();

                    }
                });
        myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        myAlertDialog.show();

    }

}

