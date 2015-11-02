/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 DVDAndroid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dvd.android.xposed.enableambientdisplay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("deprecation")
public class MainActivity extends PreferenceActivity
		implements SharedPreferences.OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager()
				.setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.prefs);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);

		findPreference("root").setEnabled(!RootShell.isAccessGiven());
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		try {
			if (sharedPreferences.getString(key, "").equals("")) {
				if (key.equals("doze_pulse_duration_visible")) {
					sharedPreferences.edit().putString(key, "3000").apply();
				} else {
					sharedPreferences.edit().putString(key, "1000").apply();
				}
				EditTextPreference editTextPreference = (EditTextPreference) findPreference(
						key);
				editTextPreference
						.setText(sharedPreferences.getString(key, ""));
			}
		} catch (ClassCastException ignored) {
		}

		if (!key.equals("pick_up_enabled"))
			Toast.makeText(getApplicationContext(),
					getString(R.string.reboot_required), Toast.LENGTH_LONG)
					.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.reboot:
				try {
					RootShell.getShell(true).add(new Command(0, "reboot"));
				} catch (IOException | TimeoutException
						| RootDeniedException e) {
					Toast.makeText(MainActivity.this, e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.hot_reboot:
				try {
					RootShell.getShell(true).add(
							new Command(0, "busybox killall system_server"));
				} catch (IOException | TimeoutException
						| RootDeniedException e) {
					Toast.makeText(MainActivity.this, e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.startservice:
				if (!SensorService.isRunning)
					startService(
							new Intent().setClass(this, SensorService.class));
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {

		if (preference.getKey() != null)
			switch (preference.getKey()) {
				case "doze_small_icon_alpha":
					createAlert(preference.getKey(), "222", 255);
					break;
				case "config_screenBrightnessDoze":
					createAlert(preference.getKey(), "17", 100);
					break;
				case "doze_pulse_schedule_resets":
					createAlert(preference.getKey(), "1", 5);
					break;
				case "root":
					if (RootShell.isRootAvailable()) {
						Command command = new Command(0, "echo test");
						try {
							RootShell.getShell(true).add(command);
						} catch (IOException | TimeoutException e) {
							e.printStackTrace();
						} catch (RootDeniedException e) {
							Toast.makeText(MainActivity.this,
									R.string.grant_root, Toast.LENGTH_SHORT)
									.show();
						}
					} else {
						Toast.makeText(MainActivity.this,
								R.string.root_not_found, Toast.LENGTH_SHORT)
								.show();
					}
					break;
			}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	private void createAlert(final String key, String defaultValue,
			int maxValue) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(
				getResources().getIdentifier(key, "string", getPackageName()));
		final NumberPicker np = new NumberPicker(this);
		np.setMinValue(1);
		np.setMaxValue(maxValue);
		np.setValue(Integer.parseInt(getPreferenceManager()
				.getSharedPreferences().getString(key, defaultValue)));
		alert.setView(np);
		alert.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getPreferenceManager().getSharedPreferences().edit()
								.putString(key, Integer.toString(np.getValue()))
								.apply();
					}
				});
		alert.show();
	}
}