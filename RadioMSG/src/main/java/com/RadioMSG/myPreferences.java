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
import androidx.preference.Preference;
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

        private static final String[] EXPERT_ONLY_KEYS = {
            "modelistPS", "userInterfacePS", "relayPS",
            "bluetoothPS", "ackPS", "experimentalPS",
            "AFREQUENCY", "PRETONEDURATION", "TUNEDURATION",
            "SLOWCPU", "IDLEMODEMOFF", "SAMPLEAUDIOASFLOATS",
            "rsidPS", "mt63PS", "RELAYLIST"
        };

        private void applyExpertModeGating(boolean expertMode) {
            for (String key : EXPERT_ONLY_KEYS) {
                Preference pref = findPreference(key);
                if (pref != null) pref.setEnabled(expertMode);
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Programmatically apply expert-mode gating for preferences in sub-screens
            // where android:dependency cannot cross PreferenceScreen boundaries.
            boolean expertMode = getPreferenceManager().getSharedPreferences()
                    .getBoolean("EXPERTMODE", false);
            applyExpertModeGating(expertMode);

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
                    if (key.equals("EXPERTMODE")) {
                        applyExpertModeGating(prefs.getBoolean("EXPERTMODE", false));
                    }
                    if (key.equals("LOGTOFILE")) {
                        if (prefs.getBoolean("LOGTOFILE", false)) {
                            LogcatLogger.start(requireContext());
                        } else {
                            LogcatLogger.stop();
                        }
                    }
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
