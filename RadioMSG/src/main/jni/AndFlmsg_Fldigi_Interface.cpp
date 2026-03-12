/*
 *  Based on Fldigi source code version 3.21.82
 *
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
 *
 */
#include "AndFlmsg_Fldigi_Interface.h"
//#include "modem.h"
#include "psk.h"
#include "mt63.h"
#include "thor.h"
#include "mfsk.h"
#include "olivia.h"
#include "dominoex.h"
#include "rsid.h"
#include <jni.h>

#include "Oboe.h"
#include "AudioStream.h"

using std::string;

cRsId *RsidModem = NULL;
modem *active_modem = NULL;
char decodedBuffer[10000];
char utf8PartialBuffer[4];
int utfExpectedChars = 0;
int utfFoundChars = 0;
int lastCharPos;
//Debugging
bool haveFrame = false;
JNIEnv* gEnv = NULL;
jclass gJclass = NULL;
//Separate variable for Tx thread as Env are thread sensitive
//JNIEnv* txEnv = NULL;
//For Tx buffer handling
signed char* txDataBuffer = NULL;
int txDataBufferLength = 0;
int txCounter = 0;
//MFSK Picture Rx/Tx
int pictureRow[4096 * 3];
int pictureW = 0;
int pictureH = 0;
int rowCounter = 0;
//Progress indication
int progressRatio = 0;
int sperkerDeviceId = 3; //Default
double audioLevel = 0.0f;


//VK2ETA Needs to be made bullet proof
double audioBuffer[40000]; //5 seconds of audio, in case we have a delay in the Java rx thread
int sizeOfAudioBuffer = sizeof(audioBuffer) / sizeof(int);
int audioBufferIndex;


//***************** FROM C++ to JAVA ******************

//Get the value of Java boolean static variable newAmplReady of the Java Modem class
extern bool getNewAmplReady() {
    jclass myClass = NULL;
    myClass = gEnv->FindClass("com/RadioMSG/Modem");
    jfieldID myFid = NULL;
    //Find Java field newAmplReady of type boolean
    myFid = gEnv->GetStaticFieldID(myClass,"newAmplReady","Z");
    bool newAmplReady = gEnv->GetStaticBooleanField(myClass, myFid);
    return newAmplReady;
    return true;
}


//Update the waterfall array of amplitudes in Java class Modem (double array as input)
extern void updateWaterfallBuffer(double *aFFTAmpl) {

	//Find Java Modem class
	jclass cls = gEnv->FindClass("com/RadioMSG/Modem");
	//Find Static Java method updateWaterfall (argument is an array of double and returns Void)
	jmethodID mid = gEnv->GetStaticMethodID(cls, "updateWaterfall", "([D)V");
	//Create a Java array and copy the C++ array into it
	jdoubleArray jaFFTAmpl = gEnv->NewDoubleArray(RSID_FFT_SIZE);
	gEnv->SetDoubleArrayRegion(jaFFTAmpl, 0, RSID_FFT_SIZE, aFFTAmpl);
	//Call the method with the Java array
	gEnv->CallStaticVoidMethod(cls, mid, jaFFTAmpl);
	//Release the intermediate array.
	gEnv->DeleteLocalRef(jaFFTAmpl);
}


//Update the waterfall array of amplitudes in Java class Modem (float array as input)
extern void updateWaterfallBuffer(float *aFFTAmpl) {
	//Find Java Modem class
	jclass cls = gEnv->FindClass("com/RadioMSG/Modem");
	//Find Static Java method updateWaterfall (argument is an array of double and returns Void)
	jmethodID mid = gEnv->GetStaticMethodID(cls, "updateWaterfall", "([F)V");
	//Create a Java array and copy the C++ array into it
	jfloatArray jaFFTAmpl = gEnv->NewFloatArray(RSID_FFT_SIZE);
	gEnv->SetFloatArrayRegion(jaFFTAmpl, 0, RSID_FFT_SIZE, aFFTAmpl);
	//Call the method with the Java array
	gEnv->CallStaticVoidMethod(cls, mid, jaFFTAmpl);
	//Release the intermediate array.
	gEnv->DeleteLocalRef(jaFFTAmpl);
}


//Access to Java config class methods for accessing String preferences
extern string getPreferenceS(string preferenceString, string defaultValue) {

	jobject returnedObject;
	jstring jprefStr;
	jstring jdefVal;

	//Check that we have valid data
	if (gEnv ==NULL) return NULL;
	if (gJclass ==NULL) return NULL;

	//Find the Java class
	jclass cls = gEnv->FindClass("com/RadioMSG/config");

	//Find the static Java method (see signature)
	jmethodID mid = gEnv->GetStaticMethodID(cls, "getPreferenceS", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");

	//Convert strings to jstrings
	jprefStr = gEnv->NewStringUTF(preferenceString.c_str());
	jdefVal = gEnv->NewStringUTF(defaultValue.c_str());
	//Call the method
	returnedObject = gEnv->CallStaticObjectMethod(cls, mid, jprefStr, jdefVal);
	//Convert the returned jstring to C string
	const char* str = gEnv->GetStringUTFChars((jstring) returnedObject, NULL);

	//Release the intermediate variable
	gEnv->DeleteLocalRef(returnedObject);
	gEnv->DeleteLocalRef(jprefStr);
	gEnv->DeleteLocalRef(jdefVal);

	//Return the result...that simple...phew
	return str;
}


//Access to Java config class methods for accessing Boolean preferences
extern bool getPreferenceB(string preferenceString, bool defaultValue) {
	jboolean returnedValue;
	jstring jprefStr;

	//Check that we have valid data
	if (gEnv == NULL) return false;
	if (gJclass == NULL) return false;

	//Find the Java class
	jclass cls = gEnv->FindClass("com/RadioMSG/config");

	//Find the static Java method (see signature)
	jmethodID mid = gEnv->GetStaticMethodID(cls, "getPreferenceB", "(Ljava/lang/String;Z)Z");

	//Convert strings to jstrings
	jprefStr = gEnv->NewStringUTF(preferenceString.c_str());
	jboolean jDefault = defaultValue;
	//Call the method
	returnedValue = gEnv->CallStaticBooleanMethod(cls, mid, jprefStr, jDefault);

	//Release the intermediate variable
	gEnv->DeleteLocalRef(jprefStr);

	//Return the result
	return returnedValue;
}


//Access to Java config class methods for accessing Double preferences
extern double getPreferenceD(string preferenceString, double defaultValue) {
	jdouble returnedValue;
	jstring jprefStr;

	//Check that we have valid data
	if (gEnv == NULL) return 0.0f;
	if (gJclass == NULL) return 0.0f;

	//Find the Java class
	jclass cls = gEnv->FindClass("com/RadioMSG/config");

	//Find the static Java method (see signature)
	jmethodID mid = gEnv->GetStaticMethodID(cls, "getPreferenceD", "(Ljava/lang/String;D)D");

	//Convert strings to jstrings
	jprefStr = gEnv->NewStringUTF(preferenceString.c_str());
	jdouble jDefault = defaultValue;
	//Call the method
	returnedValue = gEnv->CallStaticDoubleMethod(cls, mid, jprefStr, jDefault);

	//Release the intermediate variable
	gEnv->DeleteLocalRef(jprefStr);

	//Return the result
	return returnedValue;
}


//Access to Java config class methods for accessing Int preferences
extern int getPreferenceI(string preferenceString, int defaultValue) {
	jint returnedValue;
	jstring jprefStr;

	//Check that we have valid data
	if (gEnv == NULL) return 0;
	if (gJclass == NULL) return 0;

	//Find the Java class
	jclass cls = gEnv->FindClass("com/RadioMSG/config");

	//Find the static Java method (see signature)
	jmethodID mid = gEnv->GetStaticMethodID(cls, "getPreferenceI", "(Ljava/lang/String;I)I");

	//Convert strings to jstrings
	jprefStr = gEnv->NewStringUTF(preferenceString.c_str());
	jint jDefault = defaultValue;
	//Call the method
	returnedValue = gEnv->CallStaticIntMethod(cls, mid, jprefStr, jDefault);

	//Release the intermediate variable
	gEnv->DeleteLocalRef(jprefStr);

	//Return the result
	return returnedValue;
}

//RX Section of the C++ modems
extern void put_rx_char(int receivedChar) {
	//Only send complete UTF-8 sequences to Java
	//Behaviour on invalid combinations: discard and re-sync only on valid characters to
	//  avoid exceptions in upstream methods
	//decodedBuffer[lastCharPos++] = receivedChar & 0x7f;
	if (utfExpectedChars < 1) { //Normally zero
		//We are not in a multi-byte sequence yet
		if ((receivedChar & 0xE0) == 0xC0) {
			utfExpectedChars = 1; //One extra characters
			utfFoundChars = 0;
			utf8PartialBuffer[utfFoundChars++] = receivedChar;
		} else if ((receivedChar & 0xF0) == 0xE0) {
			utfExpectedChars = 2; //Two extra characters
			utfFoundChars = 0;
			utf8PartialBuffer[utfFoundChars++] = receivedChar;
		} else if ((receivedChar & 0xF8) == 0xF0) {
			utfExpectedChars = 3; //Three extra characters
			utfFoundChars = 0;
			utf8PartialBuffer[utfFoundChars++] = receivedChar;
		} else if ((receivedChar & 0xC0) == 0x80) { //Is it a follow-on character?
			//Should not be there (missing first character in sequence), discard and reset just in case
			utfExpectedChars = utfFoundChars = 0;
		} else if ((receivedChar & 0x80) == 0x00){ //Could be a single Chareacter, check it is legal
			decodedBuffer[lastCharPos++] = receivedChar;
			//Debug
			// if (receivedChar == 1) {
            //    haveFrame = true;
			//}
			//if (receivedChar == 4) {
			//    haveFrame = false;
			//}
			utfExpectedChars = utfFoundChars = 0; //Reset to zero in case counter is negative (just in case)
		} else { //Not a legal case, ignore and reset counter
			utfExpectedChars = utfFoundChars = 0; //Reset to zero in case counter is negative (just in case)
		}
	} else { //we are still expecting follow-up UTF-8 characters
		if ((receivedChar & 0xC0) == 0x80) { //Valid follow-on character, store it
			utfExpectedChars--;
			utf8PartialBuffer[utfFoundChars++] = receivedChar;
		} else { //Invalid sequence, discard it and start from scratch
			utfExpectedChars = utfFoundChars = 0;
		}
		//If we have a complete sequence, add to receive buffer
		if (utfExpectedChars < 1 && utfFoundChars > 0) {
			for (int i = 0; i < utfFoundChars; i++) {
				decodedBuffer[lastCharPos++] = utf8PartialBuffer[i];
			}
			utfExpectedChars = utfFoundChars = 0; //Reset

		}
	}
/* Code for Debugging
 * 		decodedBuffer[lastCharPos++] = '(';
		decodedBuffer[lastCharPos++] = 'U';
		decodedBuffer[lastCharPos++] = ')';
		//debug
		 	 	 char hexstr[10];
			 	 int cvlen = sprintf(hexstr, "%0004x", utf8PartialBuffer[i]);
				decodedBuffer[lastCharPos++] = '[';
				for (int i = 0; i < cvlen; i++) {
					decodedBuffer[lastCharPos++] = hexstr[i];
				}
				decodedBuffer[lastCharPos++] = ']';
 *
 */

}



//Call the Java class that echoes the transmitted characters
extern void put_echo_char(unsigned int txedChar) {
//Do nothing for now
	//Find Java Modem class
	//	jclass cls = gEnv->FindClass("com/RadioMSG/Modem");
	//Find Static Java method updateWaterfall (argument is an array of double and returns Void)
	//	jmethodID mid = gEnv->GetStaticMethodID(cls, "putEchoChar", "(I)V");
	//Call the method
	//	gEnv->CallStaticVoidMethod(cls, mid, (txedChar & 0x7F));
}


//Prepare a new Picture receiving file and display screen for MFSK picture Rx
void androidShowRxViewer(int picW, int picH) {
	//Save picture size for later
	pictureW = picW;
	pictureH = picH;
	//Reset progress counter
	rowCounter = 0;
	//Find the Java class
	jclass cls = gEnv->FindClass("com/RadioMSG/Modem");
	//Find the static Java method (see signature)
	jmethodID mid = gEnv->GetStaticMethodID(cls, "showRxViewer", "(II)V");
	//Call the method with the parameters
	gEnv->CallStaticVoidMethod(cls, mid, picW, picH);
	//Release the variables
	gEnv->DeleteLocalRef(cls);
}


//Save the last Picture to file (MFSK picture Rx)
void androidSaveLastPicture() {
	//Find the Java class
	jclass cls = gEnv->FindClass("com/RadioMSG/Modem");
	//Find the static Java method (see signature)
	jmethodID mid = gEnv->GetStaticMethodID(cls, "saveLastPicture", "()V");
	//Call the method with the parameters
	gEnv->CallStaticVoidMethod(cls, mid);
	//Release the variables
	gEnv->DeleteLocalRef(cls);
	//Reset progress counter
	rowCounter = 0;

}


//Update one datum of Current Rx picture(MFSK picture Rx)
void androidUpdateRxPic(int data, int pixelNumber) {

	if (pixelNumber < 4096 * 3) {
		pictureRow[pixelNumber] = data;
	}
	if (pixelNumber == 3 * pictureW - 1) { //Full row of bytes (3 per pixels), update bitmap.
	    rowCounter++;
		//Find the Java class
		jclass cls = gEnv->FindClass("com/RadioMSG/Modem");
		//Find the static Java method (see signature)
		jmethodID mid = gEnv->GetStaticMethodID(cls, "updatePicture", "([III)V");
		//Create a Java array and copy the C++ array into it
		jintArray jPictureRow = gEnv->NewIntArray(pictureW * 3);
		gEnv->SetIntArrayRegion(jPictureRow, 0, pictureW * 3, pictureRow);
		//Call the method with the parameters
		gEnv->CallStaticVoidMethod(cls, mid, jPictureRow, pictureW, (100 * rowCounter) / pictureH);
		//Release the variables
		gEnv->DeleteLocalRef(cls);
		gEnv->DeleteLocalRef(jPictureRow);
	}

}



//----- TX section of the C++ modems ----

//Get one character from the input buffer
extern int get_tx_char() {
	int returnValue;

	returnValue = GET_TX_CHAR_ETX;
	if (txDataBuffer != NULL) {
		if (txCounter < txDataBufferLength) {
			returnValue = (int) (*txDataBuffer++);
			txCounter++;
            if (txDataBufferLength > 0)
                progressRatio = (100 * txCounter) / txDataBufferLength;
		}
	}

	return returnValue;
}


//Calls the Java Method that sends the buffer to the sound device
void txModulate(double *buffer, int len) {
	//Accumulate samples
	for (int i = 0; i < len; i++) {
		//Skip samples if we can't transmit fast enough. There is no point in accumulating any further.
		if (i < (sizeOfAudioBuffer - 1)) {
			audioBuffer[audioBufferIndex + i] = buffer[i];
		}
	}
	audioBufferIndex += len;
	if (audioBufferIndex > 2000) {//2000/8000 = 1/4 second of sound samples at 8Khz audio
		//Find the Java class
		jclass cls = gEnv->FindClass("com/RadioMSG/Modem");
		//Find the static Java method (see signature)
		jmethodID mid = gEnv->GetStaticMethodID(cls, "txModulate", "([DI)V");
		//Create a Java array and copy the C++ array into it
		jdoubleArray jBuffer = gEnv->NewDoubleArray(audioBufferIndex);
		gEnv->SetDoubleArrayRegion(jBuffer, 0, audioBufferIndex, audioBuffer);
		//Call the method with the Java array
		gEnv->CallStaticVoidMethod(cls, mid, jBuffer, audioBufferIndex);
		//Release the intermediate array and the other variables
		//Debug: crash at next line when debugging????
		gEnv->DeleteLocalRef(jBuffer);
		gEnv->DeleteLocalRef(cls);
		audioBufferIndex = 0;
	}
}


//Flushes the end of the sound buffer
void flushTxSoundBuffer() {
	if (audioBufferIndex > 0) {
		//Find the Java class
		jclass cls = gEnv->FindClass("com/RadioMSG/Modem");
		//Find the static Java method (see signature)
		jmethodID mid = gEnv->GetStaticMethodID(cls, "txModulate", "([DI)V");
		//Create a Java array and copy the C++ array into it
		jdoubleArray jBuffer = gEnv->NewDoubleArray(audioBufferIndex);
		gEnv->SetDoubleArrayRegion(jBuffer, 0, audioBufferIndex, audioBuffer);
		//Call the method with the Java array
		gEnv->CallStaticVoidMethod(cls, mid, jBuffer, audioBufferIndex);
		//Release the intermediate array and the other variables
		gEnv->DeleteLocalRef(jBuffer);
		gEnv->DeleteLocalRef(cls);
		audioBufferIndex = 0;
	}
}


extern void change_CModem(int modemCode, double newFrequency) {
	//Delete previously created modem to recover memory used
	if (active_modem) {
		//Test flush before deleting to allow for new modem to be created
		active_modem->rx_flush();
		delete active_modem;
		active_modem = NULL;
	}
	//Now re-create with new mode
	if (modemCode >= MODE_PSK_FIRST && modemCode <= MODE_PSK_LAST) {
		active_modem = new psk(modemCode);
	} else if (modemCode >= MODE_DOMINOEX_FIRST && modemCode <= MODE_DOMINOEX_LAST) {
		active_modem = new dominoex(modemCode);
	} else if (modemCode >= MODE_THOR_FIRST && modemCode <= MODE_THOR_LAST) {
		active_modem = new thor(modemCode);
	} else if (modemCode >= MODE_MFSK_FIRST && modemCode <= MODE_MFSK_LAST) {
		active_modem = new mfsk(modemCode);
	} else if (modemCode >= MODE_MT63_FIRST && modemCode <= MODE_MT63_LAST) {
		active_modem = new mt63(modemCode);
	} else if (modemCode >= MODE_OLIVIA_FIRST && modemCode <= MODE_OLIVIA_LAST) {
		active_modem = new olivia(modemCode);
	}
	//Do the initializations
	if (active_modem != NULL) {
		//First overall modem initilisation
		active_modem->init();
		//Then init of RX side
		active_modem->rx_init();
		//Then set RX frequency
		active_modem->set_freq(newFrequency);
	}
}



//***************** FROM JAVA to C++  ******************
std::shared_ptr<oboe::AudioStream> mStream = nullptr;

bool openSpeaker () {
    //Create stream
    oboe::AudioStreamBuilder builder;
    builder.setAudioApi(oboe::AudioApi::AAudio);
    builder.setDeviceId(sperkerDeviceId);
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::PowerSaving);
    builder.setSharingMode(oboe::SharingMode::Shared);
    builder.setFormat(oboe::AudioFormat::I16);
    builder.setSampleRate(8000);
    builder.setChannelConversionAllowed(true);
    builder.setFormatConversionAllowed(true); // enables format conversions. This is false by default.
    builder.setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Low);
    builder.setChannelCount(oboe::ChannelCount::Mono);
    //Open it
    oboe::Result result = builder.openStream(mStream);
    int channelCount = mStream->getChannelCount();
    int gotStreamId = -22;
    gotStreamId = mStream->getDeviceId();
    if (gotStreamId != 1000) {
        gotStreamId = channelCount + 1 - 1;
    }
    oboe::AudioFormat format = mStream->getFormat();
    int32_t  sampleRate = mStream->getSampleRate();
    if (result != oboe::Result::OK) {
        mStream = nullptr;
        return false;
        //LOGE("Failed to create stream. Error: %s", oboe::convertToText(result));
    }
    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        return false;
    }
    //result = mStream->requestFlush();
    //if (result != oboe::Result::OK) {
    //    return false;
    //}
    return true;
}


//Opens a stream for a device
extern "C" JNIEXPORT void
Java_com_RadioMSG_Modem_saveSpeakerDeviceId( JNIEnv* env, jclass thishere, jint deviceId)
{
    sperkerDeviceId = deviceId;
}


//Opens a stream for a device
extern "C" JNIEXPORT jboolean
Java_com_RadioMSG_Modem_openSpeaker( JNIEnv* env, jclass thishere)
{
    return openSpeaker();
}

//Opens a stream for a device
extern "C" JNIEXPORT jint
Java_com_RadioMSG_Modem_queryAudioApi(JNIEnv* env, jclass thishere)
{
    oboe::AudioApi result = (oboe::AudioApi) -1;
    if (mStream != nullptr) {
        result = mStream->getAudioApi();
    }
    return (int) result;
}


//Closes the audio stream
extern "C" JNIEXPORT jboolean
Java_com_RadioMSG_Modem_closeSpeaker( JNIEnv* env, jclass thishere)
{
    if (mStream != nullptr) {
        oboe::Result result = mStream->requestStop();
        if (result != oboe::Result::OK) {
            return false;
        }
        result = mStream->close();
        if (result != oboe::Result::OK) {
            return false;
        }
    }
    return true;
}


//Writes to the audio stream
extern "C" JNIEXPORT jboolean
Java_com_RadioMSG_Modem_writeToSpeaker( JNIEnv* env, jclass thishere,
                                        jshortArray myjbuffer, jint length, jint volumeShift)
{
    //Convert to C++ type
    jshort *shortBuffer = env->GetShortArrayElements(myjbuffer, 0);
    //Process the buffer in C++
    if (mStream != nullptr) {
        jshort* end = shortBuffer + length;
        jshort mult = 1 << volumeShift;
        if (volumeShift > 0) {
            for (jshort* sPtr = shortBuffer; sPtr < end; sPtr++) {
                //shortBuffer[i] = shortBuffer[i] << volumeShift;
                //*sPtr <<= volumeShift;
                *sPtr *= mult;
            }
        }
        //jfloat* aBuffer = env->GetFloatArrayElements(myjbuffer, 0);
        //oboe::Result result = oboe::Result::OK;
        oboe::Result result = mStream->write(shortBuffer, length, 100000000); //1 second timeout
        if (result == oboe::Result::OK) {
            //Release the passed parameters
            env->ReleaseShortArrayElements(myjbuffer, shortBuffer, JNI_ABORT);
            return true;
        } else if (result == oboe::Result::ErrorClosed || result == oboe::Result::ErrorDisconnected) { //Can be disconnected for many reasons
            //Try to (re-)connect, close it first if opened
            mStream->close();
            openSpeaker();
            //And write again
            result = mStream->write(shortBuffer, length, 100000000);
            //Release the passed parameters
            env->ReleaseShortArrayElements(myjbuffer, shortBuffer, JNI_ABORT);
            //env->ReleaseFloatArrayElements(myjbuffer, aBuffer, JNI_ABORT);
            return (result == oboe::Result::OK);
        } else {
            int i = 22;
            i = i +1;
        }
    } else {
        //open it first
        openSpeaker();
        //And write again
        oboe::Result result = mStream->write(shortBuffer, length, 100000000);
        //Release the passed parameters
        env->ReleaseShortArrayElements(myjbuffer, shortBuffer, JNI_ABORT);
        //env->ReleaseFloatArrayElements(myjbuffer, aBuffer, JNI_ABORT);
        return (result == oboe::Result::OK);
    }
    //Release the passed parameters
    env->ReleaseShortArrayElements(myjbuffer, shortBuffer, JNI_ABORT);
    //env->ReleaseFloatArrayElements(myjbuffer, aBuffer, JNI_ABORT);
    return false;
}


//Save Tx thread environment
extern "C" JNIEXPORT void
Java_com_RadioMSG_Modem_saveEnv( JNIEnv* env, jclass thishere)
{
    gEnv = env;
}


//Fast modem change (for Txing image for example) change_CModem
extern "C" JNIEXPORT void
Java_com_RadioMSG_Modem_changeCModem( JNIEnv* env, jclass thishere, jint modemCode, jdouble newFrequency)
{
	//change_CModem(modemCode, active_modem->get_freq()); //Same frequency as previous modem
	change_CModem(modemCode, newFrequency); //Same frequency as previous modem
}


//Creates the RSID receive Modem
extern "C" JNIEXPORT jstring
Java_com_RadioMSG_Modem_createRsidModem(JNIEnv* env, jclass thishere) {
	//Save environment if we need to call the preference methods in Java
	gEnv = env;
	gJclass = thishere;

	//Delete previously created RSID modem to recover memory used
	if (RsidModem) {
		delete RsidModem;
		RsidModem = NULL;
	}
	RsidModem = new cRsId();
	if (RsidModem != NULL) {
		return (env)->NewStringUTF("RSID Modem created");
	} else {
		return (env)->NewStringUTF("ERROR: RSID Modem Not created");
	}
}


//Returns an array of modem names (uses the RSID list in rsid_def.cxx)
extern "C" JNIEXPORT jobjectArray
Java_com_RadioMSG_Modem_getModemCapListString(JNIEnv* env, jclass thishere)
{
	char *modeCapListString[MAXMODES];
	jobjectArray returnedArray;
	int i;
	int j = 0;

	returnedArray = (jobjectArray)env->NewObjectArray(MAXMODES,
			env->FindClass("java/lang/String"),
			env->NewStringUTF(""));
	for (i=0; i < RsidModem->rsid_ids_size1; i++) {
		if (RsidModem->rsid_ids_1[i].mode != NUM_MODES) {
			env->SetObjectArrayElement(returnedArray,j++,env->NewStringUTF(RsidModem->rsid_ids_1[i].name));
		}
	}

	//do the same for 2nd list
	for (i=0; i < RsidModem->rsid_ids_size2; i++) {
		if (RsidModem->rsid_ids_2[i].mode != NUM_MODES) {
			env->SetObjectArrayElement(returnedArray,j++,env->NewStringUTF(RsidModem->rsid_ids_2[i].name));
		}
	}
    //Android debug  env->SetObjectArrayElement(returnedArray,j++,env->NewStringUTF("Bpsk31"));

	//Finalise the array
	env->SetObjectArrayElement(returnedArray,j,env->NewStringUTF("END"));

	return(returnedArray);

}


//Returns an array of modem codes (uses the RSID list in rsid_def.cxx)
extern "C" JNIEXPORT jintArray
Java_com_RadioMSG_Modem_getModemCapListInt(JNIEnv* env, jclass thishere)
{
	jintArray returnedArray;
	int i;
	int j = 0;

	returnedArray = env->NewIntArray(MAXMODES);
	jint temp[MAXMODES];

	for (i=0; i < RsidModem->rsid_ids_size1; i++) {
		if (RsidModem->rsid_ids_1[i].mode != NUM_MODES) {
			temp[j++] = RsidModem->rsid_ids_1[i].mode;
		}
	}

	//do the same for 2nd list
	for (i=0; i < RsidModem->rsid_ids_size2; i++) {
		if (RsidModem->rsid_ids_2[i].mode != NUM_MODES) {
			temp[j++] = RsidModem->rsid_ids_2[i].mode;
		}
	}

	//Finalise the array with a negtive value
	temp[j] = -1;

	//Copy to Java structure
	env->SetIntArrayRegion(returnedArray, 0, MAXMODES, temp);

	return(returnedArray);

}


//Creates the requested modem in C++
extern "C" JNIEXPORT jstring
Java_com_RadioMSG_Modem_createCModem( JNIEnv* env, jclass thishere,
		jint modemCode)
{
	//Save environment if we need to call the preference methods in Java
	gEnv = env;
	gJclass = thishere;

	//Delete previously created modem to recover memory used
	if (active_modem) {
		delete active_modem;
		active_modem = NULL;
	}

	//Now re-create with new mode
	if (modemCode >= MODE_PSK_FIRST && modemCode <= MODE_PSK_LAST) {
		active_modem = new psk(modemCode);
	} else if (modemCode >= MODE_DOMINOEX_FIRST && modemCode <= MODE_DOMINOEX_LAST) {
		active_modem = new dominoex(modemCode);
	} else if (modemCode >= MODE_THOR_FIRST && modemCode <= MODE_THOR_LAST) {
		active_modem = new thor(modemCode);
	} else if (modemCode >= MODE_MFSK_FIRST && modemCode <= MODE_MFSK_LAST) {
		active_modem = new mfsk(modemCode);
	} else if (modemCode >= MODE_MT63_FIRST && modemCode <= MODE_MT63_LAST) {
		active_modem = new mt63(modemCode);
	} else if (modemCode >= MODE_OLIVIA_FIRST && modemCode <= MODE_OLIVIA_LAST) {
		active_modem = new olivia(modemCode);
	} else {
		return (env)->NewStringUTF("ERROR: Modem NOT created");
	}
	return (env)->NewStringUTF("Modem created");
}


//Initializes the RX section of the requested modem in C++
extern "C" JNIEXPORT jstring
Java_com_RadioMSG_Modem_initCModem( JNIEnv* env, jclass thishere,
		jdouble frequency)
{
	//Save environment if we need to call the preference methods in Java
	gEnv = env;
	gJclass = thishere;

	if (active_modem != NULL) {
		//First overall modem initialisation
		active_modem->init();
		//Then init of RX side
		active_modem->rx_init();
		//Then set RX frequency
		active_modem->set_freq(frequency);
		lastCharPos = 0;
		//Debugging
        haveFrame = false;
		//Reset UTF-8 sequence monitor
		utfExpectedChars = utfFoundChars = 0;
		return (env)->NewStringUTF("Modem Initialized");
	} else {
		return (env)->NewStringUTF("ERROR: Modem NOT Initialized");
	}
}


/* Conversion from byte stream to UTF-8 String moved to JAVA code to avoid crashes on older devices (pre-Android 7 or even better processing in Android 10) as JAVA required mofified UTF-8 character set

 //Processes the audio buffer through the current modem in C++
extern "C" JNIEXPORT jstring
Java_com_RadioMSG_Modem_rxCProcess( JNIEnv* env, jclass thishere,
//       jshortArray myjbuffer, jint length)
									jfloatArray myjbuffer, jint length)
{
	//Save environment if we need to call any methods in Java
	gEnv = env;
	gJclass = thishere;

	//Reset received characters buffer pointer
	lastCharPos = 0;
	//Convert to C++ type
	//jshort* shortBuffer = env->GetShortArrayElements(myjbuffer, 0);
	jfloat* aBuffer = env->GetFloatArrayElements(myjbuffer, 0);

	//Process the buffer in C++
	if (active_modem != NULL) {
		active_modem->rx_process(aBuffer, length);
	} else {
		return (env)->NewStringUTF("ERROR: Modem NOT Initialized");
	}
	//Release the passed parameters
	//env->ReleaseShortArrayElements(myjbuffer, shortBuffer, JNI_ABORT);
	env->ReleaseFloatArrayElements(myjbuffer, aBuffer, JNI_ABORT);

	//Terminate the buffer
	decodedBuffer[lastCharPos] = 0;

	//Return the decoded data
	return env->NewStringUTF(decodedBuffer);
}
*/


//Processes the audio buffer through the current modem in C++, returns a byte array for encoding to UTF-8 (in Java)
extern "C" JNIEXPORT jbyteArray
Java_com_RadioMSG_Modem_rxCProcess( JNIEnv* env, jclass thishere,
//       jshortArray myjbuffer, jint length)
									jfloatArray myjbuffer, jint length)
{
	//Save environment if we need to call any methods in Java
	gEnv = env;
	gJclass = thishere;

	//Reset received characters buffer pointer
	lastCharPos = 0;
	//Convert to C++ type
	//jshort* shortBuffer = env->GetShortArrayElements(myjbuffer, 0);
	jfloat* aBuffer = env->GetFloatArrayElements(myjbuffer, 0);

	//Process the buffer in C++
	if (active_modem != NULL) {
		active_modem->rx_process(aBuffer, length);
	} else {
	    string errorMsg = "ERROR: Modem NOT Initialized";
	    strcpy(decodedBuffer, errorMsg.c_str());
	    lastCharPos = errorMsg.length();
	}


	//Terminate the buffer
	decodedBuffer[lastCharPos++] = 0;
	//Create Java Byte array and copy values
	//jbyteArray jDecodedBuffer = env->NewByteArray(lastCharPos);
    //env->SetByteArrayRegion(jDecodedBuffer, 0, lastCharPos, (jbyte*)decodedBuffer);
    jbyteArray data = env->NewByteArray(lastCharPos);
    jbyte *dataBytes = env->GetByteArrayElements(data, 0);
    int i;
    for (i = 0; i < lastCharPos; i++) {
        dataBytes[i] = decodedBuffer[i];
    }
    // move from the temp structure to the java structure
    env->SetByteArrayRegion(data, 0, lastCharPos, dataBytes);
	//Release the passed parameters
	//env->ReleaseShortArrayElements(myjbuffer, shortBuffer, JNI_ABORT);
	env->ReleaseFloatArrayElements(myjbuffer, aBuffer, JNI_ABORT);

	//Return the decoded data
	return data;
}


//Updates the modem with the latest GUI squelch level
extern "C" JNIEXPORT jint
Java_com_RadioMSG_Modem_setSquelchLevel( JNIEnv* env, jclass thishere,
		jdouble squelchLevel)
{
	if ( active_modem != NULL) {
			active_modem->set_squelchLevel(squelchLevel);
		}
	return 0;
}


//Returns the latest signal metric from the current modem in C++
extern "C" JNIEXPORT jdouble
Java_com_RadioMSG_Modem_getMetric( JNIEnv* env, jclass thishere)
{
    double metric = 0.0;
    if ( active_modem != NULL) {
        metric = active_modem->get_metric();
    }
    return metric;
}


//Returns the latest audio signal strength from the current modem in C++
extern "C" JNIEXPORT jdouble
Java_com_RadioMSG_Modem_getAudioLevel( JNIEnv* env, jclass thishere)
{
    return audioLevel;
}


//Processes the audio buffer through the RSID modem
extern "C" JNIEXPORT jstring
Java_com_RadioMSG_Modem_RsidCModemReceive( JNIEnv* env, jclass thishere,
//Run RSID at 8000 sampling rate but with 1/2 symbol length (slightly faster
// , no sample rate conversion and more importantly no interference with existing RSID transmissions)
										   jfloatArray myfbuffer, jint length, jboolean doSearch)
//		jshortArray myfbuffer, jint length, jboolean doSearch)
{

	//Convert to C++ type
	//Memory.Leak test jboolean myjcopy = true;
	jfloat* floatBuffer = env->GetFloatArrayElements(myfbuffer, 0);
	//ML test jshort* shortBuffer = env->GetShortArrayElements(myfbuffer, &myjcopy);
	//jshort* shortBuffer = env->GetShortArrayElements(myfbuffer, 0);
	//Reset received characters buffer pointer
	lastCharPos = 0;
	//Process the buffer in C++
	RsidModem->receive(floatBuffer, length, doSearch);
	//RsidModem->receive(shortBuffer, length, doSearch);
	//Release the passed parameters
	env->ReleaseFloatArrayElements(myfbuffer, floatBuffer, 0);
	//M.L. test env->ReleaseShortArrayElements(myfbuffer, shortBuffer, 0);
	//env->ReleaseShortArrayElements(myfbuffer, floatBuffer , JNI_ABORT);
	//Terminate the buffer
	decodedBuffer[lastCharPos] = 0;

	return env->NewStringUTF(decodedBuffer);
}


//Returns the current modem frequency (for waterfall tracking after RSID)
extern "C" JNIEXPORT jdouble
Java_com_RadioMSG_Modem_getCurrentFrequency( JNIEnv* env, jclass thishere)
{
	return active_modem->get_freq();
}

//Returns the current mode (for updating the Java side)
extern "C" JNIEXPORT jint
Java_com_RadioMSG_Modem_getCurrentMode( JNIEnv* env, jclass thishere)
{
	return active_modem->get_mode();
}

//Returns the decoded characters resulting from the flushing of the rx pipe on RSID rx of new modem
extern "C" JNIEXPORT jint
Java_com_RadioMSG_Modem_getFlushedRxCharacters( JNIEnv* env, jclass thishere)
{
	return active_modem->get_mode();
}




//Transmit section-------------------



//Send RSID of current mode
extern "C" JNIEXPORT void
Java_com_RadioMSG_Modem_txRSID(JNIEnv* env, jclass thishere)
{
	if (RsidModem != NULL && active_modem != NULL) {
		RsidModem->send(true); //Always true as we handle post-rsid decision in higher level
	}
}


//Send specific RSID code
extern "C" JNIEXPORT void
Java_com_RadioMSG_Modem_txThisRSID(JNIEnv* env, jclass thishere, jint code)
{
	if (RsidModem != NULL && active_modem != NULL) {
		RsidModem->sendThis(code);
	}
}


//Initializes the TX section of the requested modem in C++
extern "C" JNIEXPORT jstring
Java_com_RadioMSG_Modem_txInit( JNIEnv* env, jclass thishere,
		jdouble frequency)
{
	//Init static variable
	audioBufferIndex = 0;

	if (active_modem != NULL) {
		//Then set TX frequency
		active_modem->set_freq(frequency);
		//Init of TX side
		active_modem->tx_init();
		return (env)->NewStringUTF("Tx Modem Initialized");
	} else {
		return (env)->NewStringUTF("ERROR: Tx Modem NOT Initialized");
	}
}



//Processes the data buffer for TX through the current modem in C++
extern "C" JNIEXPORT jboolean
Java_com_RadioMSG_Modem_txCProcess( JNIEnv* env, jclass thishere,
		jbyteArray myjbuffer, jint length)
{
	//Reset the static variables
	txCounter = 0;
	//Convert to C++ type
	txDataBuffer = env->GetByteArrayElements(myjbuffer, NULL);
	txDataBufferLength = length;
	//Process the buffer in C++
	if (active_modem != NULL) {
		//As there is no idle time in RadioMSG context, keep
		//  processing until we have exhausted the data buffer
		while (active_modem->tx_process() >= 0){  //Character processing is done in the test
		}
	} else {
		//Release the passed parameters
		//RadioMSG avoid seg fault on invalid addresses
		//env->ReleaseByteArrayElements(myjbuffer, txDataBuffer, JNI_ABORT);
	    env->DeleteLocalRef(myjbuffer);

		return JNI_FALSE;
	}
	//Flush the tx Sound Buffer
	flushTxSoundBuffer();
	//Release the passed parameters
	//RadioMSG avoid seg fault on invalid addresses
	//env->ReleaseByteArrayElements(myjbuffer, txDataBuffer, JNI_ABORT);
	env->DeleteLocalRef(myjbuffer);
	//Return the status
	return JNI_TRUE;
}



//Resets the percent progress of TXing the buffer in characters (not time)
extern "C" JNIEXPORT void
Java_com_RadioMSG_Modem_resetTxProgressPercent( JNIEnv* env, jclass thishere)
{
	progressRatio = 0;
}


//Sets the percent progress of TXing the buffer in characters (not time)
//paramater "percent" is int between 0 and 100
extern "C" JNIEXPORT void
Java_com_RadioMSG_Modem_setTxProgressPercent( JNIEnv* env, jclass thishere, jint percent)
{

    if (percent > 100) {
        progressRatio = 100;
    } else {
        progressRatio = percent;
    }
}



//Returns the percent progress of TXing the buffer in characters (not time)
extern "C" JNIEXPORT jint
Java_com_RadioMSG_Modem_getTxProgressPercent( JNIEnv* env, jclass thishere)
{
	return progressRatio;
}



//Sets the Slow CPU Flag
extern "C" JNIEXPORT void
Java_com_RadioMSG_Modem_setSlowCpuFlag( JNIEnv* env, jclass thishere,
                                        jboolean mSlowCpu)
{
	progdefaults.slowcpu = mSlowCpu;
	return;
}



//Processes the picture data buffer for TX through the current MFSK modem in C++
extern "C" JNIEXPORT void
Java_com_RadioMSG_Modem_txPicture( JNIEnv* env, jclass thishere,
		jbyteArray txPictureBuffer, jint txPictureWidth, jint txPictureHeight,
		jint txPictureTxSpeed, jint txPictureColour)
{
	//Make sure we have the correct modem
	if (serviceme != active_modem) return;
	//Extract the data and a pointer
	signed char *inbuf = env->GetByteArrayElements(txPictureBuffer, 0);
	//Convert to C++ Picture Buffer
	int W, H, rowstart;
	W = txPictureWidth;
	H = txPictureHeight;
	serviceme->TXspp = txPictureTxSpeed;
    //Problem: boolean parameter not passed correctly
	bool colour = (txPictureColour != 0);
	//bool colour = false;
	//Clear old transmit buffer
	if (xmtpicbuff) {
		delete [] xmtpicbuff;
		xmtpicbuff = NULL;
	}
	//Android Bitmap library: we always have a 4 bytes per pixel in RGBA order
	//   representing the bitmap picture regardless
	//   of the colour depth. Discard the Alpha and keep the RGB as the RGB values are pre-multiplied.
	int iy, ix, value;
	if (colour) {
		//RGB = 3 bytes per pixel
		xmtpicbuff = new unsigned char [W*H*3];
		unsigned char *outbuf = xmtpicbuff;
		for (iy = 0; iy < H; iy++) {
			rowstart = iy * W;
			for (ix = 0; ix < W; ix++) {
				//Skip Alpha bytes and change from signed to unsigned char
				value = inbuf[(rowstart + ix)*4];
				outbuf[(rowstart*3 + ix)] = (unsigned char)(value < 0 ? value + 256 : value);
				value = inbuf[(rowstart + ix)*4 + 1];
				outbuf[(rowstart*3 + ix + W)] = (unsigned char)(value < 0 ? value + 256 : value);
				value = inbuf[(rowstart + ix)*4 + 2];
				outbuf[(rowstart*3 + ix + W + W)] = (unsigned char)(value < 0 ? value + 256 : value);
			}
		}
		serviceme->xmtbytes = W * H * 3;
		serviceme->color = true;
		if (serviceme->TXspp == 8)
			snprintf(serviceme->picheader, PICHEADER, "\nSending Pic:%dx%dC;", W, H);
		else {
		    //Test narrower modes/slower scan
		    int tempSpp = serviceme->TXspp;
		    if (tempSpp == 16) tempSpp = 5;//coded a single digits for the decoding routine
		    if (tempSpp == 32) tempSpp = 6;//coded a single digits for the decoding routine
			//snprintf(serviceme->picheader, PICHEADER, "\nSending Pic:%dx%dCp%d;", W, H,serviceme->TXspp);
			snprintf(serviceme->picheader, PICHEADER, "\nSending Pic:%dx%dCp%d;", W, H, tempSpp);
		}
	} else {
		//Grey-Scale = single byte per pixel
		xmtpicbuff = new unsigned char [W*H];
		unsigned char *outbuf = xmtpicbuff;
		int greyTotal;
		for (iy = 0; iy < H; iy++) {
			rowstart = iy * W;
			for (ix = 0; ix < W; ix++) {
				value = inbuf[(rowstart + ix)*4];
				greyTotal = 29 * (value < 0 ? value + 256 : value);
				value = inbuf[(rowstart + ix)*4 + 1];
				greyTotal  += 58 * (value < 0 ? value + 256 : value);
				value = inbuf[(rowstart + ix)*4 + 2];
				greyTotal  += 11 * (value < 0 ? value + 256 : value);
				outbuf[rowstart + ix] = (unsigned char)(greyTotal / 100);
			}
		}
		serviceme->xmtbytes = W * H;
		serviceme->color = false;
		if (serviceme->TXspp == 8)
			snprintf(serviceme->picheader, PICHEADER, "\nSending Pic:%dx%d;", W, H);
		else
			snprintf(serviceme->picheader, PICHEADER, "\nSending Pic:%dx%dp%d;", W, H,serviceme->TXspp);
	}
	serviceme->rgb = 0;
	serviceme->col = 0;
	serviceme->row = 0;
	serviceme->pixelnbr = 0;

	// start the transmission
	serviceme->startpic = true;

	//Process the buffer in C++
	while (active_modem->tx_process() >= 0){  //Character processing is done in the test
	}
	//Flush the tx Sound Buffer
	flushTxSoundBuffer();
	//Clear the transmit buffer
	if (xmtpicbuff) {
		delete [] xmtpicbuff;
		xmtpicbuff = NULL;
	}
	//Release the passed parameters
	//RadioSMS: avoid seg fault on release
	//env->ReleaseByteArrayElements(txPictureBuffer, inbuf, JNI_ABORT);
	env->DeleteLocalRef(txPictureBuffer);

}

