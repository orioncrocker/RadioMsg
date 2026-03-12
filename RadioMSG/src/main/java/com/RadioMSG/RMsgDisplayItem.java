package com.RadioMSG;

/**
 * Created by jdouyere on 07/02/17.
 */

public class RMsgDisplayItem {

    public RMsgObject mMessage;
    public float currentDistance;
    public float previousDistance;
    public boolean inRange = false;
    public boolean myOwn = false;

    public RMsgDisplayItem(RMsgObject mNewMessage, float currentDistance, float previousDistance, boolean inRange, boolean myOwn) {
        this.mMessage = mNewMessage;
        this.currentDistance = currentDistance;
        this.previousDistance = previousDistance;
        this.inRange = inRange;
        this.myOwn = myOwn;
    }

}
