/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
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
package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

public class DummyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w(ImApp.LOG_TAG, "DummyActivity launched by swipe");
        super.onCreate(savedInstanceState);
        ((ImApp)getApplication()).maybeInit(this);
        finish();
    }

    // Unused for now
    void showDialog() {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.im_label).setMessage(R.string.swipe_alert)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        })
        .setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        })
        .show();
    }
}
