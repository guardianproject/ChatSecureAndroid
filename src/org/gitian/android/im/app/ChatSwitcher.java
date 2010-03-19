/*
 * Copyright (C) 2008 Google Inc.
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

package org.gitian.android.im.app;

import java.util.ArrayList;
import java.util.List;

import org.gitian.android.im.R;
import org.gitian.android.im.plugin.BrandingResourceIDs;
import org.gitian.android.im.provider.Imps;

import android.app.Activity;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ChatSwitcher {
    private static final boolean LOCAL_DEBUG = true;

    private static final String[] PROVIDER_CATEGORY_PROJECTION = new String[] {
            Imps.Provider.CATEGORY
    };
    private static final int PROVIDER_CATEGORY_COLUMN = 0;

    private boolean mPreferMenuShortcut = false;
    public  SwitcherAdapter mSwitcherAdapter;
    private Activity mActivity;
    private Handler mHandler;
    private ImApp mApp;
    private String mQuerySelection;
    private String mQuerySelectionArgs[];
    private LayoutInflater mInflater;
    private SwitcherRunnable mSwitcherCallback;
    private ArrayList<View> mViews = new ArrayList<View>();
    private boolean mOkToShowEmptyView;
    private ChatSwitcherDialog mChatSwitcherDialog;

    private AsyncQueryHandler mQueryHandler;
    private static final int sQueryToken = 1;
    private static final long sPeriodicUpdatePeriod = 60000;


    public  int mContactIdColumn;
    public  int mProviderIdColumn;
    public  int mAccountIdColumn;
    public  int mUsernameColumn;
    public  int mNicknameColumn;
    public  int mAvatarDataColumn;
    public  int mPresenceStatusColumn;
    public  int mLastUnreadMessageColumn;
    public  int mShortcutColumn;
    public  int mLastChatColumn;
    public  int mGroupChatColumn;

    public interface OnQueryCompleteRunnable {
        public void onComplete(Cursor c);
    }

    public interface SwitcherRunnable {
        public boolean switchTo(String contact, long account, Intent intent);
    }

    private DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            if (isOpen()) {
                if (LOCAL_DEBUG) log("Observer.onChanged: update");
                update();
            }
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            if (isOpen()) {
                if (LOCAL_DEBUG) log("Observer.onInvalidated: update");
                update();
            }
        }
    };

    private void cancelPreviousQuery() {
        mQueryHandler.cancelOperation(sQueryToken);
    }

    private void startQuery() {
        startQuery(new OnQueryCompleteRunnable() {
            public void onComplete(Cursor c) {
                mSwitcherAdapter.changeCursor(c);
            }
        });
    }

    private void startQuery(OnQueryCompleteRunnable runnable) {
        cancelPreviousQuery();
        mQueryHandler.startQuery(
                sQueryToken,
                runnable,
                Imps.Contacts.CONTENT_URI_CHAT_CONTACTS,
                null, /*projection*/
                mQuerySelection,
                mQuerySelectionArgs,
                null  /*orderBy*/);
    }

    /**
     * Onscreen dialog used to show the chat switcher
     */
    private class ChatSwitcherDialog extends Dialog {
        public ChatSwitcher mSwitcher;
        private ViewGroup mContainer;
        private View mEmptyView;

        public ChatSwitcherDialog(Context context, ChatSwitcher switcher) {
            super(context, R.style.Theme_ChatSwitcher);
            mSwitcher = switcher;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.chat_switcher_dialog);
            mContainer = (ViewGroup) findViewById(R.id.pseudogallery);
            mEmptyView = findViewById(R.id.empty);
        }

        public void updateTimes() {
            final Cursor c = mSwitcherAdapter.getCursor();
            if (c != null) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    if (!mSwitcherAdapter.validateCursor(c)) {
                        if (LOCAL_DEBUG) log("populateGallery: validate cursor failed");
                        return;    // return true so we don't draw the empty text
                    }

                    View v = mViews.get(c.getPosition());
                    mSwitcherAdapter.bindView(v, mActivity, c);
                }
            }
        }

        private void updateViewListeners(View v, final Cursor c, final int position) {
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    select(c, position);
                }});

            v.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) {
                        return false;
                    }
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                        select(c, position);
                        return true;
                    }
                    return false;
                }
            });
        }

        public void update() {
            int focusPos = 0;
            int sz = mViews.size();
            for (int i = 0; i < sz; i++) {
                View v = mViews.get(i);
                if (v == null) {
                    continue;
                }
                if (v.getVisibility() != View.VISIBLE) {
                    continue;
                }

                if (v.hasFocus()) {
                    focusPos = i;
                    break;
                }
            }

            int pos = 0;
            final Cursor c = mSwitcherAdapter.getCursor();
            if (c != null) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    if (!mSwitcherAdapter.validateCursor(c)) {
                        if (LOCAL_DEBUG) log("populateGallery: validate cursor failed");
                        return;    // return true so we don't draw the empty text
                    }

                    View v;
                    if (pos >= mViews.size()) {
                        mInflater.inflate(R.layout.chat_switcher_item, mContainer, true);
                        v = mContainer.getChildAt(mContainer.getChildCount() - 1);
                        mViews.add(v);
                        v.setFocusable(true);
                    } else {
                        v = mViews.get(pos);
                    }
                    
                    updateViewListeners(v, c, pos);

                    v.setVisibility(View.VISIBLE);
                    mSwitcherAdapter.bindView(v, mActivity, c);
                    v.setTag(c.getLong(mProviderIdColumn));

                    pos++;
                }

                int viewCount = mViews.size();
                int cursorCount = c.getCount();
                for (int i = cursorCount; i <viewCount; i++) {
                    mViews.get(i).setVisibility(View.GONE);
                }

                if (focusPos < mViews.size()) {
                    mViews.get(focusPos).requestFocus();
                }
            }

            mEmptyView.setVisibility(pos > 0 || !mOkToShowEmptyView ? View.GONE : View.VISIBLE);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    mSwitcher.close();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MENU:
                // if we don't have a keyboard then we don't use the
                // fancy keyboard shortcuts for the chat switcher
                if (0 != (mActivity.getResources().getConfiguration().keyboard &
                        Configuration.KEYBOARD_QWERTY)) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        mSwitcher.mPreferMenuShortcut = true;
                        mSwitcher.update();
                    } else if (event.getAction() == KeyEvent.ACTION_UP) {
                        mSwitcher.mPreferMenuShortcut = false;
                        mSwitcher.update();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_0:
                mSwitcher.handleShortcut('0');
                break;
            case KeyEvent.KEYCODE_1:
                mSwitcher.handleShortcut('1');
                break;
            case KeyEvent.KEYCODE_2:
                mSwitcher.handleShortcut('2');
                break;
            case KeyEvent.KEYCODE_3:
                mSwitcher.handleShortcut('3');
                break;
            case KeyEvent.KEYCODE_4:
                mSwitcher.handleShortcut('4');
                break;
            case KeyEvent.KEYCODE_5:
                mSwitcher.handleShortcut('5');
                break;
            case KeyEvent.KEYCODE_6:
                mSwitcher.handleShortcut('6');
                break;
            case KeyEvent.KEYCODE_7:
                mSwitcher.handleShortcut('7');
                break;
            case KeyEvent.KEYCODE_8:
                mSwitcher.handleShortcut('8');
                break;
            case KeyEvent.KEYCODE_9:
                mSwitcher.handleShortcut('9');
                break;

            }
            return super.dispatchKeyEvent(event);
        }
    }

    /**
     * The cursor adapter for the chatting contacts
     */
    public class SwitcherAdapter extends CursorAdapter {
        private String mMenuPlus;

        private int mLayout;
        private android.database.ContentObserver mContentObserver = 
            new android.database.ContentObserver(null) {
                @Override
                public void onChange(boolean selfChange) {
                    if (isOpen())
                        startQuery();
                }
        };

        public SwitcherAdapter(Cursor c, Activity a) {
            // use false as the third parameter to the CursorAdapter constructor
            // to indicate that we should not auto-requery the cursor 
            super(a, c, false);
            mLayout = R.layout.chat_switcher_item; 
            mActivity = a;
            mMenuPlus = a.getString(R.string.menu_plus);

            setupObservers(null);
        }

        private void setupObservers(Cursor oldCursor) {
            if (oldCursor != null) {
                oldCursor.unregisterContentObserver(mContentObserver);
            }

            Cursor c = getCursor();
            if (c == null)
                return;

            c.registerContentObserver(mContentObserver);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(mLayout, parent, false);
        }

        @Override
        public void changeCursor(Cursor c) {
            Cursor oldCursor = getCursor();

            super.changeCursor(c);
            setupObservers(oldCursor);
        }

        public boolean validateCursor(Cursor c) {
            long accountId = c.getLong(mAccountIdColumn);
            return accountId != 0;
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            long providerId = c.getLong(mProviderIdColumn);
            String nickname = c.getString(mNicknameColumn);
            //if (LOCAL_DEBUG) log("bindView: nickname=" + nickname);

            TextView t = (TextView) view.findViewById(R.id.name);
            t.setText(nickname);

            int presenceMode = c.getInt(mPresenceStatusColumn);
            String s = c.getString(mLastUnreadMessageColumn);

            BrandingResources brandingRes = mApp.getBrandingResource(providerId);
            //  If there is unread text, indicate that, otherwise, show presence
            Drawable presence;
            if (s != null) {
                presence = brandingRes.getDrawable(
                        BrandingResourceIDs.DRAWABLE_UNREAD_CHAT);
            } else {
                presence = brandingRes.getDrawable(
                        PresenceUtils.getStatusIconId(presenceMode));
            }
            ImageView presenceView = (ImageView) view.findViewById(R.id.presence);
            presenceView.setImageDrawable(presence);

            //  If there is a shortcut assigned to this chat, then show it, otherwise
            //  hide the shortcut text view.
            long shortcut = c.getLong(mShortcutColumn);
            TextView shortcutView = (TextView) view.findViewById(R.id.shortcut);

            shortcutView.setVisibility(mPreferMenuShortcut ? View.VISIBLE : View.GONE);
            if (shortcut >= 0 && shortcut < 10) {
                shortcutView.setText(mMenuPlus + shortcut);
            } else {
                shortcutView.setText("");
            }

            ImageView avatarView = (ImageView) view.findViewById(R.id.avatar);
            Drawable avatar = DatabaseUtils.getAvatarFromCursor(c, mAvatarDataColumn);

            if (avatar == null) {
                avatarView.setImageResource(R.drawable.avatar_unknown);
            } else {
                avatarView.setImageDrawable(avatar);
            }

            TextView tv = (TextView) view.findViewById(R.id.when);
            tv.setText(android.text.format.DateUtils.getRelativeTimeSpanString(
                        c.getLong(mLastChatColumn),
                        System.currentTimeMillis(),
                        android.text.format.DateUtils.MINUTE_IN_MILLIS, 
                        android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE));
            tv.setVisibility(mPreferMenuShortcut ? View.GONE : View.VISIBLE);
        }
    }


    public ChatSwitcher(
            final Activity a,
            final Handler handler,
            final ImApp app,
            final LayoutInflater inflater,
            final SwitcherRunnable switcher) {
        mActivity = a;
        mApp = app;
        mHandler = handler;
        mInflater = inflater;
        mSwitcherCallback = switcher;

        buildQueryParams();

        mSwitcherAdapter = new SwitcherAdapter(null, a);

        mQueryHandler = new AsyncQueryHandler(mActivity.getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

                if (cursor != null) {
                    mContactIdColumn = cursor.getColumnIndexOrThrow(Imps.Contacts._ID);
                    mProviderIdColumn = cursor.getColumnIndexOrThrow(Imps.Contacts.PROVIDER);
                    mAccountIdColumn = cursor.getColumnIndexOrThrow(Imps.Contacts.ACCOUNT);
                    mUsernameColumn = cursor.getColumnIndexOrThrow(Imps.Contacts.USERNAME);
                    mNicknameColumn = cursor.getColumnIndexOrThrow(Imps.Contacts.NICKNAME);
                    mPresenceStatusColumn = cursor.getColumnIndexOrThrow(Imps.Contacts.PRESENCE_STATUS);
                    mLastUnreadMessageColumn = cursor.getColumnIndexOrThrow(Imps.Chats.LAST_UNREAD_MESSAGE);
                    mAvatarDataColumn = cursor.getColumnIndexOrThrow(Imps.Contacts.AVATAR_DATA);
                    mShortcutColumn = cursor.getColumnIndexOrThrow(Imps.Chats.SHORTCUT);
                    mLastChatColumn = cursor.getColumnIndexOrThrow(Imps.Chats.LAST_MESSAGE_DATE);
                    mGroupChatColumn = cursor.getColumnIndexOrThrow(Imps.Chats.GROUP_CHAT);
                }

                mOkToShowEmptyView = true;
                update();

                OnQueryCompleteRunnable r = (OnQueryCompleteRunnable) cookie;
                if (r != null) {
                    r.onComplete(cursor);
                }
            }

            @Override
            protected void onUpdateComplete(int token, Object cookie, int result) {
                super.onUpdateComplete(token, cookie, result);
            }
        };
    };

    private void buildQueryParams() {
        StringBuilder buf = new StringBuilder("(");
        List<ProviderDef> list = mApp.getProviders();

        mQuerySelectionArgs = new String[list.size()];

        int i = 0;
        for (ProviderDef providerDef : list) {
            //Log.i(ImApp.LOG_TAG, "[ChatSwitcher] buildQueryParams: provider " + providerDef.mName +
            //        ", " + providerDef.mId);

            if (i > 0) {
                buf.append(" OR ");
            }
            
            buf.append(Imps.Contacts.PROVIDER).append("=?");
            mQuerySelectionArgs[i] = String.valueOf(providerDef.mId);
            i++;
        }

        buf.append(')');
        mQuerySelection = buf.toString();

        //Log.i(ImApp.LOG_TAG, "[ChatSwitcher] buildQueryParams: selection => " + mQuerySelection);
    }

    private static String findCategory(ContentResolver resolver, long providerId) {
        // find the provider category for this chat
        Cursor providerCursor = resolver.query(
                Imps.Provider.CONTENT_URI,
                PROVIDER_CATEGORY_PROJECTION,
                "_id = " + providerId,
                null /* selection args */,
                null /* sort order */
        );
        String category = null;

        try {
            if (providerCursor.moveToFirst()) {
                category = providerCursor.getString(PROVIDER_CATEGORY_COLUMN);
            }
        } finally {
            providerCursor.close();
        }

        return category;
    }

    public static Intent makeChatIntent(ContentResolver resolver, long provider, long account,
            String contact, long contactId, int groupChat) {
        Intent i = new Intent(Intent.ACTION_VIEW,
                ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, contactId));
        i.addCategory(findCategory(resolver, provider));
        i.putExtra("from", contact);
        i.putExtra("providerId", provider);
        i.putExtra("accountId", account);

        if (groupChat != 0) {
            i.putExtra("groupChat", groupChat);
        }

        return i;
    }

    public void open() {
        mChatSwitcherDialog = new ChatSwitcherDialog(mActivity, this);
        mChatSwitcherDialog.show();

        mOkToShowEmptyView = false;
        mPreferMenuShortcut = false;

        mSwitcherAdapter.registerDataSetObserver(mObserver);
        startQuery();

        mHandler.postDelayed(mSimpleUpdateRunnable, sPeriodicUpdatePeriod);
    }

    private Runnable mSimpleUpdateRunnable = new Runnable() {
        public void run() {
            if (mActivity.isFinishing()) {
                return;
            }

            if (!isOpen()) {
                return;
            }

            if (mChatSwitcherDialog != null) {
                mChatSwitcherDialog.updateTimes();
            }
            mHandler.postDelayed(this, sPeriodicUpdatePeriod);
        }
    };

    public void close() {
        if (isOpen()) {
            mChatSwitcherDialog.dismiss();
            mChatSwitcherDialog = null;

            // if we're in the midst of querying then don't
            cancelPreviousQuery();

            // clean up the adapter, close cursors, etc.
            mSwitcherAdapter.unregisterDataSetObserver(mObserver);
            mSwitcherAdapter.changeCursor(null);

            mViews.clear();

            mHandler.removeCallbacks(mSimpleUpdateRunnable);
        }
    }

    public void onResume() {
        if (isOpen() && !mActivity.isFinishing()) {
            mChatSwitcherDialog.updateTimes();
        }
    }

    private void update() {
        if (mChatSwitcherDialog != null) {
            mChatSwitcherDialog.update();
        }
    }

    public void select(Cursor c, int position) {
        if (!c.moveToPosition(position)) {
            Log.e(ImApp.LOG_TAG, "select: moved to pos=" + position + " failed");
            return;
        }

        long contactId = c.getLong(mContactIdColumn);
        String contact = c.getString(mUsernameColumn);
        long accountId = c.getLong(mAccountIdColumn);
        long providerId = c.getLong(mProviderIdColumn);
        int groupChat = c.getInt(mGroupChatColumn);
        Intent intent = ChatSwitcher.makeChatIntent(
                mActivity.getContentResolver(), providerId, accountId,
                contact, contactId, groupChat);

        if (mSwitcherCallback == null || !mSwitcherCallback.switchTo(contact, accountId, intent)) {
            mActivity.startActivity(intent);
            mActivity.finish();
        }
        close();
    }

    public boolean isOpen() {
        return mChatSwitcherDialog != null;
    }

    private void handleShortcut(Cursor c, int key) {
        if (mActivity.isFinishing()) {
            return;
        }

        c.moveToPosition(-1);
        while (c.moveToNext()) {
            long shortcut = c.getLong(mShortcutColumn);
            if (shortcut >= 0 && shortcut < 10 && key == (shortcut + '0')) {
                select(c, c.getPosition());
                break;
            }
        }
    }

    public void handleShortcut(final int key) {
        if (mSwitcherAdapter.getCursor() != null) {
            handleShortcut(mSwitcherAdapter.getCursor(), key);
        } else {
            startQuery(new OnQueryCompleteRunnable() {
                public void onComplete(Cursor c) {
                    handleShortcut(c, key);
                    c.close();
                }
            });
        }
    }

    private int findCurrent(Cursor c, String contact, long accountId, long providerId) {
        c.moveToPosition(-1);
        while (c.moveToNext()) {
            if (c.getLong(mAccountIdColumn) != accountId) {
                continue;
            }
            if (c.getLong(mProviderIdColumn) != providerId) {
                continue;
            }
            if (!c.getString(mUsernameColumn).equals(contact)) {
                continue;
            }

            return c.getPosition();
        }
        return -1;
    }

    private void rotateChat(Cursor c, int direction, String contact, long accountId, long providerId) {
        int count = c.getCount();
        if (count < 2) {
            return;
        }

        int position = findCurrent(c, contact, accountId, providerId);
        if (position == -1) {
            return;
        }

        position += direction;
        if (position == count) {
            position = 0;
        } else if (position == -1) {
            position = count - 1;
        }

        select(c, position);
    }

    /*
     * +1 to go forward, -1 to go backward
     */
    public void rotateChat(final int direction, final String contact, final long accountId, final long providerId) {
        if (direction != 1 && direction != -1) {
            return;
        }
        if (mSwitcherAdapter.getCursor() != null) {
            rotateChat(mSwitcherAdapter.getCursor(), direction, contact, accountId, providerId);
        } else {
            startQuery(new OnQueryCompleteRunnable() {
                public void onComplete(Cursor c) {
                    rotateChat(c, direction, contact, accountId, providerId);
                    c.close();
                }
            });
        }
    }

    private void log(String msg) {
        Log.d(ImApp.LOG_TAG, "[ChatSwitcher] " + msg);
    }
}

