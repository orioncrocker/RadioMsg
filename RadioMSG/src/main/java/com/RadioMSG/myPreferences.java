/*
 * Preferences.java  
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

/**
 *
 * @author John Douyere <vk2eta@gmail.com>
 */



import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;


public class myPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

/*No theme for the preferences as the checkboxes do not show on some devices. Use system default instead.
	//Set the Activity's Theme
	int myTheme = config.getPreferenceI("APPTHEME", 0);
	switch (myTheme) {
	case 1:
	    setTheme(R.style.andFlmsgStandardDark);
	    break;
	    
	case 2:
	    setTheme(R.style.andFlmsgSmallScreen);
	    break;
	default:
	    setTheme(R.style.andFlmsgStandard);
	    break;
	}
*/
		//Start from the fixed section of the preferences
		addPreferencesFromResource(R.xml.preferences);
		//Set HF-Club to it's single option
		ListPreference HFClubsCategory = (ListPreference) findPreference("HF-Clubs");
		//Now for the HF and UHF classes of frequencies
		String[] frequencyClass = {"HF", "UHF"};
		String[] pathQuality = {"Poor", "Good", "Fast"};
		ListPreference myListPref;
		for (String freq : frequencyClass) {
			for (String quality : pathQuality) {
				myListPref = (ListPreference) findPreference(freq + "-" + quality);
				if (myListPref != null) {
					myListPref.setEntries(Modem.customModeListString);
					myListPref.setEntryValues(Modem.customModeListString);
					myListPref.setSummary(myListPref.getValue());
				}
			}
		}

		//Set the summary field to the content value for quick messages
		EditTextPreference quickMsgPref;
		for (int i=1; i<31; i++) {
			quickMsgPref = (EditTextPreference) findPreference("QUICKSMS" + i);
			if (quickMsgPref != null) {
				quickMsgPref.setSummary(quickMsgPref.getText());
			}
		}
	}



	@Override
	protected void onResume() {
		super.onResume();

		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
		RadioMSG.splistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				// Implementation
				if (key.equals("AFREQUENCY") || key.equals("SLOWCPU") ||
						key.equals("RSID_ERRORS") || key.equals("RSIDWIDESEARCH") ) {
					RadioMSG.RXParamsChanged = true;
				}
				if (key.startsWith("QUICKSMS")) {
					EditTextPreference quickMsgPref = (EditTextPreference) findPreference(key);
					if (quickMsgPref != null) {
						quickMsgPref.setSummary(quickMsgPref.getText());
					}
				}
				if (key.startsWith("HF-") || key.startsWith("UHF-")) {
					ListPreference myListPref = (ListPreference) findPreference(key);
					if (myListPref != null) {
						myListPref.setSummary(myListPref.getValue());
					}
				}
			}
		};

		RadioMSG.mysp.registerOnSharedPreferenceChangeListener(RadioMSG.splistener);
	}
}




