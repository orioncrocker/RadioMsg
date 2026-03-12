package com.RadioMSG;

import android.content.Context;
import android.location.Location;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jdouyere on 07/02/17.
 */

public class RMsgDisplayList extends ArrayAdapter<RMsgDisplayItem> {

    ArrayList<RMsgDisplayItem> displayList = new ArrayList<RMsgDisplayItem>();

    public RMsgDisplayList(Context context, int textViewResourceId, ArrayList<RMsgDisplayItem> objects) {
        super(context, textViewResourceId, objects);
        displayList = objects;
    }

    @Override
    synchronized public int getCount() {
        return super.getCount();
    }

    @Override
    synchronized public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.smslistview, null);
        TextView textView = (TextView) v.findViewById(R.id.textmsg);
        RMsgDisplayItem mDisplayItem = displayList.get(position);
        String msgText;
        if (mDisplayItem.myOwn) {
            //Sent by me, shift to the right
            TextView paddingView = (TextView) v.findViewById(R.id.padding);
            paddingView.setVisibility(View.VISIBLE);
            //paddingView.setBackgroundColor(RadioMSG.myInstance.getResources().getColor(R.color.BLACK));
            //paddingView.setText("  ");
        }
        if (mDisplayItem.mMessage.position != null && mDisplayItem.inRange) {
            msgText = mDisplayItem.mMessage.formatForList(true, false); //Tracking and not for managing message list
            float firstWarningDistance = (float) config.getPreferenceD("TRACKINGFIRSTWARNING", 350.0f);
            float secondWarningDistance = (float) config.getPreferenceD("TRACKINGSECONDWARNING", 200.0f);
            float thirdWarningDistance = (float) config.getPreferenceD("TRACKINGTHIRDWARNING", 50.0f);
            if (mDisplayItem.currentDistance <= thirdWarningDistance) {
                textView.setBackgroundColor(ContextCompat.getColor(RadioMSG.myContext, R.color.YELLOW));//getColor(R.color.YELLOW));
                textView.setTextColor(ContextCompat.getColor(RadioMSG.myContext, R.color.BLACK));//getColor(R.color.BLACK));
            } else if (mDisplayItem.currentDistance <= secondWarningDistance) {
                textView.setBackgroundColor(ContextCompat.getColor(RadioMSG.myContext, R.color.RED));//RED));
                textView.setTextColor(ContextCompat.getColor(RadioMSG.myContext, R.color.WHITE));//getColor(R.color.WHITE));
            } else if (mDisplayItem.currentDistance <= firstWarningDistance) {
                textView.setBackgroundColor(ContextCompat.getColor(RadioMSG.myContext, R.color.DARKGREEN));//getColor(R.color.DARKGREEN));
                textView.setTextColor(ContextCompat.getColor(RadioMSG.myContext, R.color.WHITE));//WHITE));
            }
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        } else {
            msgText = mDisplayItem.mMessage.formatForList(false, false); //Not tracking and not for managing message list
            if (!mDisplayItem.mMessage.crcValid && !mDisplayItem.mMessage.crcValidWithRelayPW && !mDisplayItem.mMessage.crcValidWithIotPW && !mDisplayItem.myOwn) {
                textView.setBackgroundColor(ContextCompat.getColor(RadioMSG.myContext, R.color.DARKRED));//getColor(R.color.DARKRED));
            }
        }
        textView.setText(msgText);
        //ImageView imageView = (ImageView) v.findViewById(R.id.imageView);
        //imageView.setImageResource(displayList.get(position).getAnimalImage());
        return v;

    }


    synchronized public int getsize() {
        return displayList.size();
    }


    //Update closest message entry index we are converging to (reducing distance)
    synchronized public void updateClosestPois() {

        int closest = -1;
        float minDistance = 999999f; //1000KM max
        //Save value so that it is consistent during the loop processing
        Location mLocation = RadioMSG.currentLocation;
        RMsgDisplayItem mDisplayItem;
        for (int i = 0; i < displayList.size(); i++) {
            mDisplayItem = displayList.get(i);
            //Only for items with a location
            if (mDisplayItem.mMessage.position != null) {
                //Shift current to previous distance
                mDisplayItem.previousDistance = mDisplayItem.currentDistance;
                //Calculate new distance
                mDisplayItem.currentDistance = mDisplayItem.mMessage.position.distanceTo(mLocation);
                if (mDisplayItem.currentDistance < minDistance && mDisplayItem.currentDistance < mDisplayItem.previousDistance) {
                    minDistance = mDisplayItem.currentDistance;
                    closest = i;
                }
                displayList.set(i, mDisplayItem);
            }
        }
        int longestRange = (int) config.getPreferenceD("TRACKINGFIRSTWARNING", 350.0f);
        //Now mark all the POIs which fit within a given radius (50M) and to which we are converging to
        for (int i = 0; i < displayList.size(); i++) {
            mDisplayItem = displayList.get(i);
            //Only for items with a location
            if (mDisplayItem.mMessage.position != null) {
                //Include all items within an extra third warning zone distance to show close sequence of warnings
                if (i == closest || (mDisplayItem.currentDistance <= longestRange && mDisplayItem.currentDistance <
                        mDisplayItem.previousDistance)) {
                    mDisplayItem.inRange = true;
                } else { //reset flag
                    mDisplayItem.inRange = false;
                }
                //update list
                displayList.set(i, mDisplayItem);
            }
        }
        //Request update of listview
        if (RadioMSG.msgDisplayList != null) {
            RadioMSG.msgDisplayList.notifyDataSetChanged();
        }
        //If not set, set at zero
        //closest = closest < 0 ? 0 : closest;
        RadioMSG.closestPoi = closest;

    }


    //Add new entry in list
    synchronized public void addNewItem(RMsgObject mMessage, boolean myOwn) {
        //mDisplayItem.currentDistance = 999999f; //1000KM
        //mDisplayItem.previousDistance = 990000f; //990KM, more than current so not highlighted
        //mDisplayItem.inRange = false;
        //mDisplayItem.mMessage = mMessage;
        RMsgDisplayItem mDisplayItem = new RMsgDisplayItem(mMessage, 999999f, 0.0f, false, myOwn);
        displayList.add(mDisplayItem);
        this.notifyDataSetChanged();
    }


    //Get one message object from the list at the given position
    synchronized public RMsgDisplayItem getDisplayListItem(int atPosition) {
        if (atPosition >= 0 && atPosition < displayList.size()) {
            return displayList.get(atPosition);
        }
        return null;
    }


    //To prevent sending full details over the air every time for brievety and protection, we
    // allow the To address destination to be aliased. The first transmission is expected to
    // contain the full information in the format "alias=destination". Subsequent transmissions
    // may only contain "alias=" as the To address destination. A backward (lastest to earliest)
    // scan of the received message list allows to find the previous match to convert an alias only to
    // an alias=destination format which is then stored in that message (for potentially being used
    // as a match later on). Each From callsign have their own alias=destination combination.
    //In addition, when a full alias=phonenumber is passed, it is returned with the phone number converted
    //  to international format for consistency
    synchronized public String getReceivedAliasAndDestination(String toAlias, String fromStr) {
        String mToAlias = toAlias;
        Pattern psc = Pattern.compile("^\\s*(.+)\\s*=(.*)\\s*$");
        Pattern pscf = Pattern.compile("^\\s*(.+)\\s*=(.+)\\s*$");
        Matcher msc = psc.matcher(mToAlias);
        if (msc.lookingAt()) {
            String group2 = msc.group(2);
            //if the 2nd part of the address is a cellular number, convert to international number so that we can store it properly
            if (RMsgProcessor.isCellular(group2)) {
                mToAlias =  msc.group(1) + "=" + RMsgProcessor.convertNumberToE164(group2);
            }
            if (group2.equals("")) {
                //We have an alias alone, try to find a previously received message with "alias=destination"
                int listLength = displayList.size();
                RMsgDisplayItem mDisplayItem = null;
                if (listLength > 0) {
                    //Iterate through the list and search for a previous full alias=destination entry (for that sending callsign)
                    for (int ii = listLength - 1; ii >= 0; ii--) {
                        mDisplayItem = displayList.get(ii);
                        if (!mDisplayItem.myOwn) {
                            msc = pscf.matcher(mDisplayItem.mMessage.to);
                            if (msc.lookingAt()) {
                                if (toAlias.equals(msc.group(1) + "=")
                                        && !msc.group(2).equals("")
                                        && !msc.group(2).equals("**unknown**")
                                        && mDisplayItem.mMessage.from.equals(fromStr)) {
                                    //We have a match
                                    return toAlias + msc.group(2);
                                }
                            }
                        }
                    }
                }
                //No records or No match, then return an error message for further processing
                return toAlias + "**unknown**";
            }
        }
        //We have a (new) full alias and destination combination
        //  OR  we have a strait callsign/phone number/email address, return as-is
        return mToAlias;
    }


    //As above but for messages we receive from an email or sms relay. The from was aliased to if a record exist.
    //  E.g. I send an sms to joe, I send the alias only ("joe=", which translates at the relay to joebloggs@someemail.com).
    //  When joe responds, I get as the sender: "joe=". I need to workout the full address by looking at the previously sent messages
    synchronized public String getSentDestinationFromAlias(String fromAlias) {

        Pattern psc = Pattern.compile("^\\s*(.+)\\s*=(.*)\\s*$");
        Pattern pscf = Pattern.compile("^\\s*(.+)\\s*=(.+)\\s*$");
        Matcher msc = psc.matcher(fromAlias);
        if (msc.lookingAt()) {
            String group2 = msc.group(2);
            //First look into the To preferences to see if any exist
            String keyStr = fromAlias.replaceAll("=", "");
            for (int i=0; i < RadioMSG.toArray.length; i++) {
                if (RadioMSG.toArray[i].equals(keyStr)) {
                    return RadioMSG.toAliasArray[i];
                }
            }
            //Otherwise might have been deleted, look into message history
            if (group2.equals("")) {
                //We have an alias alone, try to find a previously sent message with "alias=destination"
                int listLength = displayList.size();
                RMsgDisplayItem mDisplayItem = null;
                if (listLength > 0) {
                    //Iterate through the sent list and search for a previous full alias=destination entry
                    for (int ii = listLength - 1; ii >= 0; ii--) {
                        mDisplayItem = displayList.get(ii);
                        if (mDisplayItem.myOwn) { //Sent message
                            msc = pscf.matcher(mDisplayItem.mMessage.to);
                            if (msc.lookingAt()) {
                                if (fromAlias.equalsIgnoreCase(msc.group(1) + "=")
                                        && !msc.group(2).equals("")
                                        && !msc.group(2).equals("**unknown**")) {
                                    //We have a match
                                    return fromAlias + msc.group(2);
                                }
                            }
                        }
                    }
                }
                //No records or No match, then return an error message for further processing
                return fromAlias + "**unknown**";
            }
        }
        //We have a strait callsign address, return as-is
        return fromAlias;
    }


    //Reverse look-up of origin to alias. Used when sending an email or SMS from this relay to others over the air.
    //Returns origin if no match (the email address or cellular number will be transmitted as-is).
    synchronized public String getAliasFromOrigin(String origin, String toStr) {

        Pattern psc = Pattern.compile("(^[\\w.-]+@\\w+\\.[\\w.-]+)|(^\\+?\\d{8,16})");
        Pattern pscf = Pattern.compile("^\\s*(.+)\\s*=(.+)\\s*$");
        Matcher msc = psc.matcher(origin);
        if (msc.lookingAt()) {
            //Try to find a previous message with "alias=origin" from this callsign
            int listLength = displayList.size();
            RMsgDisplayItem mDisplayItem = null;
            if (listLength > 0) {
                //Iterate through the list and search for a previous full alias=destination entry (for that destination callsign)
                for (int ii = listLength - 1; ii >= 0; ii--) {
                    mDisplayItem = displayList.get(ii);
                    if (!mDisplayItem.myOwn) {
                        msc = pscf.matcher(mDisplayItem.mMessage.to);
                        if (msc.lookingAt()) {
                            if (origin.equals(msc.group(2))
                                    && !msc.group(1).equals("")
                                    && mDisplayItem.mMessage.from.equals(toStr)) {
                                //We have a match, return "alias=" only
                                return msc.group(1) + "=";
                            }
                        }
                    }
                }
            }
            //We have no match, return email address or cellular number as is
            return origin;

        }
        //This does not look like a cellular number or email address, return as-is
        return origin;
    }


    //Detects duplicate messages in two cases:
    //1. when a message is sent via a relay, the receiving end
    // can receive the message directly and via the relay moments later.
    // Uses the message id information send with the relayed message which consist of the three
    // least significant digits of the minutes and seconds (E.g: 752 for minute 7 and 52 seconds)
    // end of RX time at the relay. Since both the relay and receiving station
    // would have received the original message at the same time, the allowed time difference
    // is 10 seconds to account for time drifts and different CPU speeds at both stations
    //
    //2. When re request messages to be resent (they can be radio messages or relayed message or emails/SMSs messages)
    //   The "ro" field is received with the offset in seconds since this message was received by the sender
    //   This can be the receive time fr a radio message or the email receive time at the email provider.
    //   A duplicate is a message whose content (text, positions, etc...) is identical to an already
    //     received message AND has a calculated received time within 60 seconds of each other (we
    //     need to account for the RSID and FEC/Interleaver delay).
    synchronized public boolean isDuplicate(RMsgObject newMessage) {
        boolean isDuplicate = false;
        int listLength = displayList.size();
        RMsgObject oldMessage = null;
        RMsgDisplayItem recDisplayItem = null;
        if (listLength > 0) {
            //Iterate through the last 3 received items in the list
            //int trialCount = 0;
            for (int ii = listLength - 1; ii >= 0; ii--) {
                //Need to check until the end or until a duplicate is found
                if (isDuplicate) break;
                recDisplayItem = displayList.get(ii);
                //Only received messages
                if (!recDisplayItem.myOwn) {
                    //No need to check past the three previous messages
                    //Not true anymore, need to check until the end or until a duplicate is found
                    //if (++trialCount > 3) break;
                    oldMessage = recDisplayItem.mMessage;
                    //Looks like the same message was received direct and via a relay?
                    if (oldMessage.from.equals(newMessage.from)
                            && oldMessage.to.equals(newMessage.to) //added for when resending with full alias
                            && oldMessage.sms.equals(newMessage.sms)
                            && oldMessage.msgHasPosition == newMessage.msgHasPosition) {
                        if (!oldMessage.msgHasPosition ||
                                (oldMessage.position != null && newMessage.position != null
                                        //Check position accuracy within 1.1 meter
                                        && (Math.abs(oldMessage.position.getLatitude() - newMessage.position.getLatitude()) < 0.00001)
                                        && (Math.abs(oldMessage.position.getLongitude() - newMessage.position.getLongitude()) < 0.00001))) {
                            //Check the time difference
                            if (newMessage.receiveDate != null) {
                                if (oldMessage.receiveDate != null) {
                                    //We have two look alike messages, both with a received date, check delta time
                                    Long previousDateInMillis = oldMessage.receiveDate.getTimeInMillis();
                                    Long recDateInMillis = newMessage.receiveDate.getTimeInMillis();
                                    Long deltaTime = Math.abs(recDateInMillis - previousDateInMillis) / 1000;
                                    if (deltaTime < 61L) {
                                        //60 seconds leeway
                                        isDuplicate = true;
                                    }
                                } else {
                                    //We have two look alike messages, the latest with a
                                    // received date, the old one not, use its file name as a timestamp
                                    //Example = "2017-10-25_113958";
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss", Locale.US);
                                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    try {
                                        Date recDate = sdf.parse(oldMessage.fileName.replaceAll(".txt", ""));
                                        Long previousDateInMillis = recDate.getTime();
                                        Long recDateInMillis = newMessage.receiveDate.getTimeInMillis();
                                        Long deltaTime = Math.abs(recDateInMillis - previousDateInMillis) / 1000;
                                        if (deltaTime < 61L) {
                                            //60 seconds leeway
                                            isDuplicate = true;
                                        }
                                    } catch (ParseException e) {
                                        //Nothing, bad Juju if the filename is not good
                                    }
                                }
                            } else if (newMessage.timeId.trim().length() == 3
                                    && oldMessage.via.equals(newMessage.relay)) {
                                //We have a radio message sent by the relay, check if is was already received directly
                                try {
                                    int newTimeIdMin = Integer.parseInt(newMessage.timeId.substring(0, 1));
                                    int newTimeIdSec = Integer.parseInt(newMessage.timeId.substring(1));
                                    int newTimeId = newTimeIdMin * 60 + newTimeIdSec;
                                    //Obtain the old message date from the filename
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss", Locale.US);
                                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    //Date oldDate = new Date(sdf.parse(oldMessage.fileName.replaceAll(".txt", "")).getTime() + (1000 * Main.deviceToRefTimeCorrection.intValue()));
                                    Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                    c1.setTime(sdf.parse(oldMessage.fileName.replaceAll(".txt", "")));
                                    //Adjust for a received time reference (e.g. from this remote station)
                                    c1.add(Calendar.SECOND, (int) RadioMSG.deviceToRealTimeCorrection);
                                    //Rebuild the corrected time Id of the old (stored) message
                                    int oldMinutes = c1.get(Calendar.MINUTE) % 10; //Keep least digit only
                                    int oldSeconds = c1.get(Calendar.SECOND);
                                    int oldTimeId = oldMinutes * 60 + oldSeconds;
                                    //Calculate difference
                                    int deltaTime = 0;
                                    if (newTimeId > oldTimeId) {
                                        deltaTime = newTimeId - oldTimeId;
                                    } else {
                                        deltaTime = oldTimeId - newTimeId;
                                    }
                                    //Wrap around when getting past 9 Minutes 59 Seconds
                                    if (deltaTime > 59) {
                                        deltaTime = 600 - deltaTime;
                                    }
                                    if (deltaTime < 11) {
                                        //10 seconds leeway for local clock differences. Since the two messages were received
                                        // using the same mode, the receive delays are the same (ignoring the processing
                                        // speed difference of the two devices)
                                        isDuplicate = true;
                                    }
                                } catch (Exception e) {
                                    //nothing we can do as we have bad information
                                }
                            }
                        }
                    }
                }
            }
        }
        return isDuplicate;
    }


}
