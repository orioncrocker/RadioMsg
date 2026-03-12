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
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class myPreferences extends AppCompatActivity
        implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new PrefsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        PrefsFragment fragment = new PrefsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        fragment.setArguments(args);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(pref.getKey())
                .commit();
        return true;
    }

    public static class PrefsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Set HF and UHF mode list entries from the modem's available modes
            String[] frequencyClass = {"HF", "UHF"};
            String[] pathQuality = {"Poor", "Good", "Fast"};
            for (String freq : frequencyClass) {
                for (String quality : pathQuality) {
                    ListPreference myListPref = findPreference(freq + "-" + quality);
                    if (myListPref != null) {
                        myListPref.setEntries(Modem.customModeListString);
                        myListPref.setEntryValues(Modem.customModeListString);
                        myListPref.setSummary(myListPref.getValue());
                    }
                }
            }

            // Set summary field to content value for quick messages
            for (int i = 1; i < 31; i++) {
                EditTextPreference quickMsgPref = findPreference("QUICKSMS" + i);
                if (quickMsgPref != null) {
                    quickMsgPref.setSummary(quickMsgPref.getText());
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            RadioMSG.splistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key.equals("AFREQUENCY") || key.equals("SLOWCPU") ||
                            key.equals("RSID_ERRORS") || key.equals("RSIDWIDESEARCH")) {
                        RadioMSG.RXParamsChanged = true;
                    }
                    if (key.startsWith("QUICKSMS")) {
                        EditTextPreference quickMsgPref = findPreference(key);
                        if (quickMsgPref != null) {
                            quickMsgPref.setSummary(quickMsgPref.getText());
                        }
                    }
                    if (key.startsWith("HF-") || key.startsWith("UHF-")) {
                        ListPreference myListPref = findPreference(key);
                        if (myListPref != null) {
                            myListPref.setSummary(myListPref.getValue());
                        }
                    }
                }
            };

            RadioMSG.mysp.registerOnSharedPreferenceChangeListener(RadioMSG.splistener);
        }
    }
}
