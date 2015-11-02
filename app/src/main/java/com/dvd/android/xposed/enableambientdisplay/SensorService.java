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

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.view.Display;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class SensorService extends Service implements SensorEventListener {

	public static boolean isRunning = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		SensorManager sensorManager = (SensorManager) getSystemService(
				SENSOR_SERVICE);
		Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		sensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		isRunning = true;
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
		int state = dm.getDisplay(0).getState();

		SharedPreferences prefs = getSharedPreferences(
				getPackageName() + "_preferences", MODE_PRIVATE);
		boolean enabled = prefs.getBoolean("pick_up_enabled", true);

		if (enabled && (int) event.values[0] == 5
				&& state != Display.STATE_ON) {
			Command command = new Command(0,
					"am broadcast -a com.android.systemui.doze.pulse");
			try {
				RootShell.getShell(true).add(command);
			} catch (IOException | TimeoutException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			} catch (RootDeniedException e) {
				Toast.makeText(this, R.string.grant_root, Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onDestroy() {
		isRunning = false;
		super.onDestroy();
	}
}
