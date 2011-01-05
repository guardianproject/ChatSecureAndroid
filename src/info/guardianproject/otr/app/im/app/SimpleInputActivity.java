/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

import info.guardianproject.otr.app.im.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SimpleInputActivity extends Activity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PROMPT = "prompt";
    public static final String EXTRA_DEFAULT_CONTENT = "content";
    public static final String EXTRA_OK_BUTTON_TEXT = "button_ok";

    TextView mPrompt;
    EditText mEdit;
    Button mBtnOk;
    Button mBtnCancel;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setTheme(android.R.style.Theme_Dialog);
        setContentView(R.layout.simple_input_activity);

        Bundle extras = getIntent().getExtras();

        CharSequence title = extras.getCharSequence(EXTRA_TITLE);
        if (title != null) {
            setTitle(title);
        } else {
            setTitle(R.string.default_input_title);
        }

        CharSequence prompt = extras.getCharSequence(EXTRA_PROMPT);
        mPrompt = (TextView) findViewById(R.id.prompt);
        if (prompt != null) {
            mPrompt.setText(prompt);
        } else {
            mPrompt.setVisibility(View.GONE);
        }

        mEdit = (EditText) findViewById(R.id.edit);
        CharSequence defaultText = extras.getCharSequence(EXTRA_DEFAULT_CONTENT);
        if (defaultText != null) {
            mEdit.setText(defaultText);
        }

        mBtnOk = (Button) findViewById(R.id.btnOk);
        mBtnOk.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_OK,
                        (new Intent()).setAction(mEdit.getText().toString()));
                finish();
            }
        });
        CharSequence okText = extras.getCharSequence(EXTRA_OK_BUTTON_TEXT);
        if (okText != null) {
            mBtnOk.setText(okText);
        }

        mBtnCancel = (Button) findViewById(R.id.btnCancel);
        mBtnCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        // XXX Hack from GoogleLogin.java. The android:layout_width="fill_parent"
        // defined in the layout xml doesn't seem to work for LinearLayout.
        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    }

}
