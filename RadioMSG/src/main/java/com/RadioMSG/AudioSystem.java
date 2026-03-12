package com.RadioMSG;

/**
 * Based on code from Mattt1438 at WWW.stackoverflow.com. Thanks
 * Adapted by John Douyere (VK2ETA) on 06/01/17.
 */

class AudioSystem {

    private static final int REFLECTION_ERROR = -999;

    private static final String DEVICE_OUT_SPEAKER          = "DEVICE_OUT_SPEAKER";
    private static final String DEVICE_OUT_EARPIECE         = "DEVICE_OUT_EARPIECE";
    private static final String DEVICE_OUT_WIRED_HEADPHONE  = "DEVICE_OUT_WIRED_HEADPHONE";


    static Class<?> mAudioSystem;

    private static void setAudioSystem() {

        try {
            mAudioSystem = Class.forName("android.media.AudioSystem");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private static int getConstantValue(String s) {

        try {
            return ((Integer)mAudioSystem.getDeclaredField(s).get(int.class)).intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return REFLECTION_ERROR;

    }


    private static int setDeviceConnectionState(int i, int j, String s) {

        try {
            return (Integer) mAudioSystem.getMethod("setDeviceConnectionState", int.class, int.class, String.class).invoke(mAudioSystem, i, j, s);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return REFLECTION_ERROR;
    }

    private static int setDeviceConnectionState(String deviceName, Boolean state) {
        return setDeviceConnectionState(getConstantValue(deviceName), (state ? 1 : 0), "");
    }

    private static void forceWiredHeadphonesMedia() {
        setDeviceConnectionState(DEVICE_OUT_WIRED_HEADPHONE, true);
        setDeviceConnectionState(DEVICE_OUT_SPEAKER, false);
    }


    private static void forceSpeakerMedia() {
        setDeviceConnectionState(DEVICE_OUT_SPEAKER, true);
        setDeviceConnectionState(DEVICE_OUT_EARPIECE, true);
        setDeviceConnectionState(DEVICE_OUT_WIRED_HEADPHONE, false);
        setDeviceConnectionState(DEVICE_OUT_SPEAKER, true);
    }

    public static void setSpeakerOn(Boolean state) {
        setAudioSystem();
        if (state) {
            forceSpeakerMedia();
        } else {
            forceWiredHeadphonesMedia();
        }
    }
}