/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

import org.gitian.android.im.R;
import org.gitian.android.im.provider.Imps;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

/**
 * Activity used to pick a contact.
 */
public class ContactsPickerActivity extends ListActivity {
    public final static String EXTRA_EXCLUDED_CONTACTS = "excludes";

    public final static String EXTRA_RESULT_USERNAME = "result";

    private ContactsAdapter mAdapter;
    private String mExcludeClause;
    Uri mData;
    Filter mFilter;
    Cursor mCursor; // TODO

    private static final void log(String msg) {
        Log.d(ImApp.LOG_TAG, "<ContactsPickerActivity> " + msg);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.contacts_picker_activity);
        if(!resolveIntent()){
            if(Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("no data, finish");
            }
            finish();
            return;
        }

        EditText filter = (EditText)findViewById(R.id.filter);
        filter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mFilter.filter(s);
            }

            public void afterTextChanged(Editable s) {
            }
        });
    }

    private boolean resolveIntent() {
        Intent i = getIntent();
        mData = i.getData();

        if(mData == null) {
            return false;
        }
        mExcludeClause = buildExcludeClause(i.getStringArrayExtra(EXTRA_EXCLUDED_CONTACTS));
        Cursor cursor = managedQuery(mData, ContactView.CONTACT_PROJECTION,
                mExcludeClause, null, Imps.Contacts.DEFAULT_SORT_ORDER);
        if (cursor == null) {
            return false;
        }

        mAdapter = new ContactsAdapter(this, cursor);
        mFilter = mAdapter.getFilter();
        setListAdapter(mAdapter);
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor cursor = (Cursor)mAdapter.getItem(position);
        Intent data = new Intent();
        data.putExtra(EXTRA_RESULT_USERNAME,
                cursor.getString(ContactView.COLUMN_CONTACT_USERNAME));
        setResult(RESULT_OK, data);
        finish();
    }

    private static String buildExcludeClause(String[] excluded) {
        if (excluded == null || excluded.length == 0) {
            return null;
        }

        StringBuilder clause = new StringBuilder();
        clause.append(Imps.Contacts.USERNAME);
        clause.append(" NOT IN (");
        int len = excluded.length;
        for (int i = 0; i < len - 1; i++) {
            DatabaseUtils.appendValueToSql(clause, excluded[i]);
            clause.append(',');
        }
        DatabaseUtils.appendValueToSql(clause, excluded[len - 1]);
        clause.append(')');
        return clause.toString();
    }

    Cursor runQuery(CharSequence constraint) {
        String where;
        if (constraint == null) {
            where = mExcludeClause;
        } else {
            StringBuilder buf = new StringBuilder();
            if (mExcludeClause != null) {
                buf.append(mExcludeClause).append(" AND ");
            }

            buf.append(Imps.Contacts.NICKNAME);
            buf.append(" LIKE ");
            DatabaseUtils.appendValueToSql(buf, "%" + constraint + "%");

            where = buf.toString();
        }
        return managedQuery(mData, ContactView.CONTACT_PROJECTION, where, null,
                Imps.Contacts.DEFAULT_SORT_ORDER);
    }

    private class ContactsAdapter extends ResourceCursorAdapter {
        private String mConstraints;

        public ContactsAdapter(Context context, Cursor c) {
            super(context, R.layout.contact_view, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ContactView v = (ContactView)view;
            v.setPadding(0, 0, 0, 0);
            v.bind(cursor, mConstraints, false);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if(mCursor != null && mCursor != cursor) {
                mCursor.deactivate();
            }
            super.changeCursor(cursor);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            mConstraints = constraint.toString();

            return ContactsPickerActivity.this.runQuery(constraint);
        }
    }

}
