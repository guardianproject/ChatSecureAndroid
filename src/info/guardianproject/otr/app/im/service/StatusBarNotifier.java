/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package info.guardianproject.otr.app.im.service;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.NewChatActivity;
import info.guardianproject.otr.app.im.app.WelcomeActivity;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.SystemServices;

import java.util.HashMap;

import org.jsoup.Jsoup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

public class StatusBarNotifier {
    private static final boolean DBG = false;

    private static final long SUPPRESS_SOUND_INTERVAL_MS = 3000L;

    static final long[] VIBRATE_PATTERN = new long[] { 0, 250, 250, 250 };

    private Context mContext;
    private NotificationManager mNotificationManager;

    private Imps.ProviderSettings.QueryMap mGlobalSettings;
    private Handler mHandler;
    private HashMap<String, NotificationInfo> mNotificationInfos;
    private long mLastSoundPlayedMs;

    public StatusBarNotifier(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mHandler = new Handler();
        mNotificationInfos = new HashMap<String, NotificationInfo>();
    }

    public void onServiceStop() {
        if (mGlobalSettings != null)
            mGlobalSettings.close();
    }

    public void notifyChat(long providerId, long accountId, long chatId, String username,
            String nickname, String msg, boolean lightWeightNotify) {
        if (!isNotificationEnabled(providerId)) {
            if (DBG)
                log("notification for chat " + username + " is not enabled");
            return;
        }

        msg = html2text(msg); // strip tags for html client inbound msgs

        String title = nickname;
        String snippet = mContext.getString(R.string.new_messages_notify) + ' ' + nickname;// + ": " + msg;
        Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(
                Imps.Chats.CONTENT_URI, chatId));
        intent.addCategory(info.guardianproject.otr.app.im.app.ImApp.IMPS_CATEGORY);
        notify(username, title, snippet, msg, providerId, accountId, intent, lightWeightNotify, R.drawable.ic_stat_status);
    }

    public void notifyError(String username, String error) {

        Intent intent = new Intent(mContext, NewChatActivity.class);
        notify(username, error, error, error, -1, -1, intent, true, R.drawable.ic_stat_status);
    }

    public void notifySubscriptionRequest(long providerId, long accountId, long contactId,
            String username, String nickname) {
        if (!isNotificationEnabled(providerId)) {
            if (DBG)
                log("notification for subscription request " + username + " is not enabled");
            return;
        }
        String title = nickname;
        String message = mContext.getString(R.string.subscription_notify_text, nickname);
        Intent intent = new Intent(ImServiceConstants.ACTION_MANAGE_SUBSCRIPTION,
                ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, contactId));
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, providerId);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS, username);
        notify(username, title, message, message, providerId, accountId, intent, false, R.drawable.ic_stat_status);
    }

    public void notifyGroupInvitation(long providerId, long accountId, long invitationId,
            String username) {

        Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(
                Imps.Invitation.CONTENT_URI, invitationId));

        String title = mContext.getString(R.string.notify_groupchat_label);
        String message = mContext.getString(R.string.group_chat_invite_notify_text, username);
        notify(username, title, message, message, providerId, accountId, intent, false, R.drawable.ic_stat_status);
    }

    public void notifyLoggedIn(long providerId, long accountId) {

        Intent intent = new Intent(mContext, NewChatActivity.class);
        ;

        String title = mContext.getString(R.string.app_name);
        String message = mContext.getString(R.string.presence_available);
        notify(message, title, message, message, providerId, accountId, intent, false, R.drawable.ic_stat_status);
    }

    public void notifyLocked() {

        Intent intent = new Intent(mContext, WelcomeActivity.class);

        String title = mContext.getString(R.string.app_name);
        String message = mContext.getString(R.string.account_setup_pers_now_title);
        notify(message, title, message, message, -1, -1, intent, true, R.drawable.notify_chatsecure);


    }

    public void notifyDisconnected(long providerId, long accountId) {

        Intent intent = new Intent(mContext, NewChatActivity.class);
        ;

        String title = mContext.getString(R.string.app_name);
        String message = mContext.getString(R.string.presence_offline);
        notify(message, title, message, message, providerId, accountId, intent, false, R.drawable.ic_stat_status);
    }


    public void notifyFile(long providerId, long accountId, long id, String username,
            String nickname, String path, Uri uri, String type, boolean b) {
        String title = nickname;
        String message = mContext.getString(R.string.file_notify_text, path);
        Intent intent = SystemServices.Viewer.getViewIntent(uri, type);
        notify(message, title, message, message, providerId, accountId, intent, false, R.drawable.ic_stat_status);
    }

    public void dismissNotifications(long providerId) {

        synchronized (mNotificationInfos) {
            NotificationInfo info = mNotificationInfos.get(providerId);
            if (info != null) {
                mNotificationManager.cancel(info.computeNotificationId());
                mNotificationInfos.remove(providerId);
            }
        }
    }

    public void dismissChatNotification(long providerId, String username) {
        NotificationInfo info;
        boolean removed;
        synchronized (mNotificationInfos) {
            info = mNotificationInfos.get(username);
            if (info == null) {
                return;
            }
            removed = info.removeItem(username);
        }

        if (removed) {
            if (info.getMessage() == null) {
                if (DBG)
                    log("dismissChatNotification: removed notification for " + providerId);
                mNotificationManager.cancel(info.computeNotificationId());
            } else {
                if (DBG) {
                    log("cancelNotify: new notification" + " mTitle=" + info.getTitle()
                        + " mMessage=" + info.getMessage() + " mIntent=" + info.getIntent());
                }
                mNotificationManager.cancel(info.computeNotificationId());
            }
        }
    }


    private Imps.ProviderSettings.QueryMap getGlobalSettings() {
        if (mGlobalSettings == null) {

            ContentResolver contentResolver = mContext.getContentResolver();

            Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS)},null);

            if (cursor == null)
                return null;

            mGlobalSettings = new Imps.ProviderSettings.QueryMap(cursor, contentResolver, Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS, true, mHandler);
        }

        return mGlobalSettings;
    }


    private boolean isNotificationEnabled(long providerId) {
        return getGlobalSettings().getEnableNotification();
    }

    private void notify(String sender, String title, String tickerText, String message,
            long providerId, long accountId, Intent intent, boolean lightWeightNotify, int icon) {

       // intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        NotificationInfo info;

        synchronized (mNotificationInfos) {
            info = mNotificationInfos.get(sender);
            if (info == null) {
                info = new NotificationInfo(providerId, accountId);
                mNotificationInfos.put(sender, info);
            }
            info.addItem(sender, title, message, intent);            
        }

        mNotificationManager.notify(info.computeNotificationId(),
                info.createNotification(tickerText, lightWeightNotify, icon));

        
    }

    private void setRinger(long providerId, NotificationCompat.Builder builder) {
        String ringtoneUri = getGlobalSettings().getRingtoneURI();
        boolean vibrate = getGlobalSettings().getVibrate();

        Uri sound = TextUtils.isEmpty(ringtoneUri) ? null : Uri.parse(ringtoneUri);
        builder.setSound(sound);
        if (sound != null) {
            mLastSoundPlayedMs = SystemClock.elapsedRealtime();
        }

        if (DBG)
            log("setRinger: notification.sound = " + sound);

        if (vibrate) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
            if (DBG)
                log("setRinger: defaults |= vibrate");
        }
    }

    class NotificationInfo {
        class Item {
            String mTitle;
            String mMessage;
            Intent mIntent;

            public Item(String title, String message, Intent intent) {
                mTitle = title;
                mMessage = message;
                mIntent = intent;
            }
        }

      // private HashMap<String, Item> mItems;

        private Item lastItem;
        private long mProviderId;
        private long mAccountId;

        public NotificationInfo(long providerId, long accountId) {
            mProviderId = providerId;
            mAccountId = accountId;
           // mItems = new HashMap<String, Item>();
        }

        public int computeNotificationId() {
            if (lastItem == null)
                return (int)mProviderId;
            return lastItem.mTitle.hashCode();
        }

        public synchronized void addItem(String sender, String title, String message, Intent intent) {
            /*
            Item item = mItems.get(sender);
            if (item == null) {
                item = new Item(title, message, intent);
                mItems.put(sender, item);
            } else {
                item.mTitle = title;
                item.mMessage = message;
                item.mIntent = intent;
            }*/
            lastItem = new Item(title, message, intent);
        }

        public synchronized boolean removeItem(String sender) {
            /*
            Item item = mItems.remove(sender);
            if (item != null) {
                return true;
            }*/
            return true;
        }

        public Notification createNotification(String tickerText, boolean lightWeightNotify, int icon) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
            Intent intent = getIntent();
            builder
                .setSmallIcon(icon)
                .setTicker(lightWeightNotify ? null : tickerText)
                .setWhen(System.currentTimeMillis())
                .setLights(0xff00ff00, 300, 1000)
                .setContentTitle(getTitle())
                .setContentText(getMessage())
                .setContentIntent(PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT))
                .setAutoCancel(true);
                

            if (!(lightWeightNotify || shouldSuppressSoundNotification())) {
                setRinger(mProviderId, builder);
            }

            return builder.build();
        }

        private Intent getDefaultIntent() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType(Imps.Contacts.CONTENT_TYPE);
            intent.setClass(mContext, NewChatActivity.class);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
       //     intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            return intent;
        }

        private Intent getMultipleNotificationIntent() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(mContext, NewChatActivity.class);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_SHOW_MULTIPLE, true);
         //   intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            return intent;
        }

        public String getTitle() {
            /*
            int count = mItems.size();
            if (count == 0) {
                return null;
            } else if (count == 1) {
                Item item = mItems.values().iterator().next();
                return item.mTitle;
            } else {
                return mContext.getString(R.string.newMessages_label, Imps.Provider
                        .getProviderNameForId(mContext.getContentResolver(), mProviderId));
            }*/
            return lastItem.mTitle;
        }

        public String getMessage() {
            /*
            int count = mItems.size();
            if (count == 0) {
                return null;
            } else if (count == 1) {
                Item item = mItems.values().iterator().next();
                return item.mMessage;
            } else {
                return mContext.getString(R.string.num_unread_chats, count);
            }*/

            return lastItem.mMessage;
        }

        public Intent getIntent() {
            /*
            int count = mItems.size();
            if (count == 0) {
                return getDefaultIntent();
            } else if (count == 1) {
                Item item = mItems.values().iterator().next();
                return item.mIntent;
            } else {
                return getMultipleNotificationIntent();
            }*/

            return lastItem.mIntent;
        }
    }

    private static void log(String msg) {
        RemoteImService.debug("[StatusBarNotify] " + msg);
    }

    private boolean shouldSuppressSoundNotification() {
        return (SystemClock.elapsedRealtime() - mLastSoundPlayedMs < SUPPRESS_SOUND_INTERVAL_MS);
    }

    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }
}
