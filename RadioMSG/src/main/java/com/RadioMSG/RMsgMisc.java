/*
 * RMsgMisc.java
 *   
 * Copyright (C) 2011 John Douyere (VK2ETA)  
 * Translated and adapted into Java class from Fldigi
 * as per Fldigi code from Dave Freese, W1HKJ and Stelios Bounanos, M0GLD
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

import java.util.Calendar;
import java.util.TimeZone;

public class RMsgMisc {

	public static double decayavg(double average, double input, double weight) {
        if (weight <= 1.0) {
            return input;
        }
        return input * (1.0 / weight) + average * (1.0 - (1.0 / weight));
    }

	
	//efficient java memset for short[]
	public static void memset(short[] myarray, short j) {
		int len = myarray.length;
		if (len > 0)
			myarray[0] = j;
		for (int i = 1; i < len; i += i) {
			System.arraycopy( myarray, 0, myarray, i, ((len - i) < i) ? (len - i) : i);
		}
	}
	
	//memset equivalent for int[]
	public static void memset(int[] myarray, int j) {
		int len = myarray.length;
		if (len > 0)
			myarray[0] = j;
		for (int i = 1; i < len; i += i) {
			System.arraycopy( myarray, 0, myarray, i, ((len - i) < i) ? (len - i) : i);
		}
	}
	
	//memset equivalent for float[]
	public static void memset(float[] myarray, float j) {
		int len = myarray.length;
		if (len > 0)
			myarray[0] = j;
		for (int i = 1; i < len; i += i) {
			System.arraycopy( myarray, 0, myarray, i, ((len - i) < i) ? (len - i) : i);
		}
	}
	
	//memset equivalent for double[]
	public static void memset(double[] myarray, double j) {
		int len = myarray.length;
		if (len > 0)
			myarray[0] = j;
		for (int i = 1; i < len; i += i) {
			System.arraycopy( myarray, 0, myarray, i, ((len - i) < i) ? (len - i) : i);
		}
	}



    //Time convertion from seconds to hours, minutes, seconds.
    public static String secToTime(int sec) {
        int second = sec % 60;
        int minute = sec / 60;
        if (minute >= 60) {
            int hour = minute / 60;
            minute %= 60;
            return hour + " Hour " + (minute < 10 ? "0" + minute : minute) + " Min";
        } else if (sec >= 60) {
            minute = sec / 60;
            second = sec % 60;
            return minute + " Min " + (second < 10 ? "0" + second : second) + " Sec";
        }
        return "" + second + " Sec";
    }



    //Distance convertion from meters to seconds to KM if big enough.
    public static String metrestoDistance(int m) {
        int km = m / 1000;
        int metres = 0;
        if (km >= 1) {
            metres = m % 1000;
            if (metres >= 100) {
                return "" + km + "." + (metres / 100) + " km";
            } else {
                return "" + km + " km";
            }
        }
        return "" + m + " m";

    }



    //Remove CR and escape New Lines
	public static String escape(String s) {

		s = s.replace("\r", "").replace("\n", "\\n");

		return s;
	}



    //Un-escape New Lines
	public static String unescape (String s) {

		s = s.replace("\\n", "\n");

		return s;
	}



    //Remove CR
	public static String nocr(String s) {

		s = s.replace("\r", "");

		return s;
	}


	//Left trim a string
	public static String ltrim(String s) {
		int i = 0;
		while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		return s.substring(i);
	}

	//Left trim a string from leading zeros (E.g. phone number 0412345678 becomes 412345678 for comparison)
	public static String lTrimZeros(String s) {
		int i = 0;
		while (i < s.length() && (s.charAt(i) == '0')) {
			i++;
		}
		return s.substring(i);
	}


}
