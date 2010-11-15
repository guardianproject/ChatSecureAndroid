/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gitian.android.im.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.gitian.android.im.engine.HeartbeatService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Handler.Callback;
import android.util.SparseArray;

public class AndroidHeartBeatService extends BroadcastReceiver
        implements HeartbeatService {

    private static final String WAKELOCK_TAG = "IM_HEARTBEAT";

    private static final String HEARTBEAT_INTENT_ACTION
            = "org.gitian.android.im.intent.action.HEARTBEAT";
    private static final Uri HEARTBEAT_CONTENT_URI
            = Uri.parse("content://im/heartbeat");
    private static final String HEARTBEAT_CONTENT_TYPE
            = "vnd.android.im/heartbeat";

    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();

    private final Context mContext;
    private final AlarmManager mAlarmManager;
    /*package*/ PowerManager.WakeLock mWakeLock;

    static class Alarm {
        public PendingIntent mAlaramSender;
        public Callback mCallback;
    }

    private final SparseArray<Alarm> mAlarms;

    public AndroidHeartBeatService(Context context) {
        mContext = context;
        mAlarmManager = (AlarmManager)context.getSystemService(
                Context.ALARM_SERVICE);
        PowerManager powerManager = (PowerManager)context.getSystemService(
                Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                WAKELOCK_TAG);
        mAlarms = new SparseArray<Alarm>();
    }

    public synchronized void startHeartbeat(Callback callback, long triggerTime) {
        Alarm alarm = findAlarm(callback);
        if (alarm == null) {
            alarm = new Alarm();
            int id = nextId();
            alarm.mCallback = callback;
            Uri data = ContentUris.withAppendedId(HEARTBEAT_CONTENT_URI, id);
            Intent i = new Intent(HEARTBEAT_INTENT_ACTION)
                            .setDataAndType(data, HEARTBEAT_CONTENT_TYPE);
            alarm.mAlaramSender = PendingIntent.getBroadcast(mContext, 0, i, 0);
            if (mAlarms.size() == 0) {
                mContext.registerReceiver(this, IntentFilter.create(
                        HEARTBEAT_INTENT_ACTION, HEARTBEAT_CONTENT_TYPE));
            }
            mAlarms.append(id, alarm);
        }
        setAlarm(alarm, triggerTime);
    }

    public synchronized void stopHeartbeat(Callback callback) {
        Alarm alarm = findAlarm(callback);
        if (alarm != null) {
            cancelAlarm(alarm);
        }
    }

    public synchronized void stopAll() {
        for (int i = 0; i < mAlarms.size(); i++) {
            Alarm alarm = mAlarms.valueAt(i);
            cancelAlarm(alarm);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = (int)ContentUris.parseId(intent.getData());
        Alarm alarm = mAlarms.get(id);
        if (alarm == null) {
            return;
        }
        sExecutor.execute(new Worker(alarm));
    }

    private class Worker implements Runnable {
        private final Alarm mAlarm;

        public Worker(Alarm alarm) {
            mAlarm = alarm;
        }

        public void run() {
            mWakeLock.acquire();
            try {
                Callback callback = mAlarm.mCallback;
                long nextSchedule = callback.sendHeartbeat();
                if (nextSchedule <= 0) {
                    cancelAlarm(mAlarm);
                } else {
                    setAlarm(mAlarm, nextSchedule);
                }
            } finally {
                mWakeLock.release();
            }
        }
    }

    private Alarm findAlarm(Callback callback) {
        for (int i = 0; i < mAlarms.size(); i++) {
            Alarm alarm = mAlarms.valueAt(i);
            if (alarm.mCallback == callback) {
                return alarm;
            }
        }
        return null;
    }

    /*package*/ synchronized void setAlarm(Alarm alarm, long offset) {
        long triggerAtTime = SystemClock.elapsedRealtime() + offset;
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime,
                alarm.mAlaramSender);
    }

    /*package*/  synchronized void cancelAlarm(Alarm alarm) {
        mAlarmManager.cancel(alarm.mAlaramSender);
        int index = mAlarms.indexOfValue(alarm);
        if (index >= 0) {
            mAlarms.delete(mAlarms.keyAt(index));
        }

        // Unregister the BroadcastReceiver if there isn't a alarm anymore.
        if (mAlarms.size() == 0) {
            mContext.unregisterReceiver(this);
        }
    }

    private static int sNextId = 0;
    private static synchronized int nextId() {
        return sNextId++;
    }
}
