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

package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.provider.Imps;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import info.guardianproject.otr.app.im.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MessageView extends LinearLayout {

    private TextView mMessage;

    private Resources mResources;

    public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMessage = (TextView) findViewById(R.id.message);

        mResources = getResources();
    }

    public URLSpan[] getMessageLinks() {
        return mMessage.getUrls();
    }

    public void bindIncomingMessage(String contact, String body, Date date,
            Markup smileyRes, boolean scrolling) {
        CharSequence message =  formatMessage(contact, body, date, smileyRes, scrolling);
        mMessage.setText(message);
        mMessage.setTextColor(mResources.getColor(R.color.chat_msg));
    }

    public void bindOutgoingMessage(String body, Date date, Markup smileyRes, boolean scrolling) {
        String contact = mResources.getString(R.string.me);
        CharSequence message = formatMessage(contact, body, date, smileyRes, scrolling);
        mMessage.setText(message);
        mMessage.setTextColor(mResources.getColor(R.color.chat_msg));
    }

    public void bindPresenceMessage(String contact, int type, boolean isGroupChat,
            boolean scrolling) {
        CharSequence message = formatPresenceUpdates(contact, type, isGroupChat, scrolling);
        mMessage.setText(message);
        mMessage.setTextColor(mResources.getColor(R.color.chat_msg_presence));
    }

    public void bindErrorMessage(int errCode) {
        mMessage.setText(R.string.msg_sent_failed);
        mMessage.setTextColor(mResources.getColor(R.color.error));
    }

    private CharSequence formatMessage(String contact, String body,
            Date date, Markup smileyRes, boolean scrolling) {
        if (body.indexOf('\r') != -1) {
            // first convert \r\n pair to \n, then single \r to \n.
            // here we can't use HideReturnsTransformationMethod because
            // it does only 1 to 1 transformation and is unable to handle
            // the "\r\n" case.
            body = body.replace("\r\n", "\n").replace('\r', '\n');
        }

        SpannableStringBuilder buf = new SpannableStringBuilder(contact);
        buf.append(": ");
        if (scrolling) {
            buf.append(body);
        } else {
            buf.setSpan(ChatView.STYLE_BOLD, 0, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

            buf.append(smileyRes.markup(body));

            if (date != null) {
                appendTimeStamp(buf, date);
            }
        }
        return buf;
    }

    private void appendTimeStamp(SpannableStringBuilder buf, Date date) {
        DateFormat format = new SimpleDateFormat(mResources.getString(R.string.time_stamp));
        String dateStr = format.format(date);
        SpannableString spanText = new SpannableString(dateStr);
        int len = spanText.length();
        spanText.setSpan(new StyleSpan(Typeface.ITALIC),
                0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanText.setSpan(new RelativeSizeSpan(0.8f),
                0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanText.setSpan(new ForegroundColorSpan(
                mResources.getColor(android.R.color.darker_gray)),
                0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        buf.append('\n');
        buf.append(spanText);
    }

    private CharSequence formatPresenceUpdates(String contact, int type,
            boolean isGroupChat, boolean scrolling) {
        String body;
        switch (type) {
            case Imps.MessageType.PRESENCE_AVAILABLE:
                body = mResources.getString(isGroupChat ? R.string.contact_joined
                        : R.string.contact_online, contact);
                break;

            case Imps.MessageType.PRESENCE_AWAY:
                body = mResources.getString(R.string.contact_away, contact);
                break;

            case Imps.MessageType.PRESENCE_DND:
                body = mResources.getString(R.string.contact_busy, contact);
                break;

            case Imps.MessageType.PRESENCE_UNAVAILABLE:
                body = mResources.getString(isGroupChat ? R.string.contact_left
                        : R.string.contact_offline, contact);
                break;

            default:
                return null;
        }

        if (scrolling) {
            return body;
        } else {
            SpannableString spanText = new SpannableString(body);
            int len = spanText.length();
            spanText.setSpan(new StyleSpan(Typeface.ITALIC),
                    0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanText.setSpan(new RelativeSizeSpan((float)0.8),
                    0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spanText;
        }
    }
}
