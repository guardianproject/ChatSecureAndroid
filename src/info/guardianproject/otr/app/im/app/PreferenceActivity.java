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

package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.plugin.ImConfigNames;
import info.guardianproject.otr.app.im.plugin.ImpsConfigNames;
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.HashMap;

import info.guardianproject.otr.app.im.R;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

public class PreferenceActivity extends Activity {

    RadioGroup mRgDataChannel;
    RadioGroup mRgDataEncoding;
    RadioGroup mRgCirChannel;

    EditText mEdtHost;
    EditText mEdtMsisdn;

    long mProviderId;
    String mProviderName;
    HashMap<String, String> mPref;

    static final void log(String log) {
         Log.d(ImApp.LOG_TAG, "<PreferenceActivity> " + log);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        resolveIntent();
        setTitle(getString(R.string.preference_title, mProviderName));
        setContentView(R.layout.preference_activity);

        mRgDataChannel  = (RadioGroup) findViewById(R.id.rgDataChannel);
        mRgDataEncoding = (RadioGroup) findViewById(R.id.rgDataEncoding);
        mRgCirChannel   = (RadioGroup) findViewById(R.id.rgCirChannel);
        mEdtHost        = (EditText) findViewById(R.id.etHost);
        mEdtMsisdn      = (EditText) findViewById(R.id.etMsisdn);

        /*
        String dataChannel = getPreference(ImpsConfigNames.DATA_CHANNEL,
                TransportType.HTTP.name());
        if (TransportType.HTTP.name().equals(dataChannel)) {
            mRgDataChannel.check(R.id.DATA_HTTP);
        } else if (TransportType.SMS.name().equals(dataChannel)) {
            mRgDataChannel.check(R.id.DATA_SMS);
        }

        String cirChannel = getPreference(ImpsConfigNames.CIR_CHANNEL,
                CirMethod.STCP.name());
        if (CirMethod.STCP.name().equals(cirChannel)) {
            mRgCirChannel.check(R.id.CIR_STCP);
        } else if (CirMethod.SHTTP.name().equals(cirChannel)) {
            mRgCirChannel.check(R.id.CIR_SHTTP);
        } else if (CirMethod.SSMS.name().equals(cirChannel)) {
            mRgCirChannel.check(R.id.CIR_SSMS);
        }

        String dataEncoding = getPreference(ImpsConfigNames.DATA_ENCODING,
                EncodingType.XML.name());
        if (EncodingType.XML.name().equals(dataEncoding)) {
            mRgDataEncoding.check(R.id.ENC_XML);
        } else if (EncodingType.WBXML.name().equals(dataEncoding)) {
            mRgDataEncoding.check(R.id.ENC_WBXML);
        } else if (EncodingType.SMS.name().equals(dataEncoding)) {
            mRgDataEncoding.check(R.id.ENC_SMS);
        }

        mEdtHost.setText(getPreference(ImpsConfigNames.HOST, "http://"));
        mEdtMsisdn.setText(getPreference(ImpsConfigNames.MSISDN, ""));

        final Button btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                savePreferences();
            }
        });
        */
    }

    private String getPreference(String prefName, String defaultValue) {
        String value = mPref.get(prefName);

        return value == null ? defaultValue : value;
    }

    void resolveIntent() {
        Intent i = getIntent();
        if(i.getData() == null){
            Log.w(ImApp.LOG_TAG, "No data passed to PreferenceActivity");
            finish();
        } else {
            Cursor c = getContentResolver().query(i.getData(),
                    new String[]{Imps.Provider._ID, Imps.Provider.NAME}, null, null, null);
            if (c == null || !c.moveToFirst()) {
                Log.w(ImApp.LOG_TAG, "Can't query data from given URI.");
                finish();
            } else {
                mProviderId   = c.getLong(0);
                mProviderName = c.getString(1);

                c.close();

                mPref = Imps.ProviderSettings.queryProviderSettings(getContentResolver(), mProviderId);
            }
        }
    }

    void savePreferences() {
    	/* TODO
        TransportType dataChannel;
        switch (mRgDataChannel.getCheckedRadioButtonId()) {
        case R.id.DATA_HTTP:
            dataChannel = TransportType.HTTP;
            break;
        case R.id.DATA_SMS:
            dataChannel = TransportType.SMS;
            break;
        default:
            Log.w(ImApp.LOG_TAG, "Unexpected dataChannel button ID; defaulting to HTTP");
            dataChannel = TransportType.HTTP;
            break;
        }

        CirMethod cirChannel;
        switch (mRgCirChannel.getCheckedRadioButtonId()) {
        case R.id.CIR_STCP:
            cirChannel = CirMethod.STCP;
            break;
        case R.id.CIR_SHTTP:
            cirChannel = CirMethod.SHTTP;
            break;
        case R.id.CIR_SSMS:
            cirChannel = CirMethod.SSMS;
            break;
        default:
            Log.w(ImApp.LOG_TAG, "Unexpected cirChannel button ID; defaulting to STCP");
            cirChannel = CirMethod.STCP;
            break;
        }

        EncodingType dataEncoding;
        switch (mRgDataEncoding.getCheckedRadioButtonId()) {
        case R.id.ENC_WBXML:
            dataEncoding = EncodingType.WBXML;
            break;
        case R.id.ENC_XML:
            dataEncoding = EncodingType.XML;
            break;
        case R.id.ENC_SMS:
            dataEncoding = EncodingType.SMS;
            break;
        default:
            Log.w(ImApp.LOG_TAG, "Unexpected dataEncoding button ID; defaulting to WBXML");
            dataEncoding = EncodingType.WBXML;
            break;
        }

        String host = mEdtHost.getText().toString();
        String msisdn = mEdtMsisdn.getText().toString();

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
            log("set connection preference, DataChannel: " + dataChannel
                    + ", CirChannel: " + cirChannel
                    + ", DataEncoding: " + dataEncoding
                    + ", Host: " + host
                    + ", MSISDN: " + msisdn);
        }
        ContentValues[] valuesList = new ContentValues[7];
        valuesList[0] = getValues(ImConfigNames.PROTOCOL_NAME, "IMPS");
        valuesList[1] = getValues(ImpsConfigNames.DATA_CHANNEL, dataChannel.name());
        valuesList[2] = getValues(ImpsConfigNames.DATA_ENCODING, dataEncoding.name());
        valuesList[3] = getValues(ImpsConfigNames.CIR_CHANNEL, cirChannel.name());
        valuesList[4] = getValues(ImpsConfigNames.HOST, host);
        valuesList[6] = getValues(ImpsConfigNames.MSISDN, msisdn);

        getContentResolver().bulkInsert(Imps.ProviderSettings.CONTENT_URI, valuesList);

        finish();
        */
    }

    private ContentValues getValues(String name, String value) {
        ContentValues values = new ContentValues();
        values.put(Imps.ProviderSettings.PROVIDER, mProviderId);
        values.put(Imps.ProviderSettings.NAME, name);
        values.put(Imps.ProviderSettings.VALUE, value);

        return values;
    }
}
