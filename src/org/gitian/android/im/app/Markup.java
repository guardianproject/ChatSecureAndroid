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

package org.gitian.android.im.app;

import org.gitian.android.im.plugin.BrandingResourceIDs;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.text.util.Linkify;

public class Markup {
    private BrandingResources mRes;
    private IntTrie mSmileys;

    public Markup(BrandingResources res) {
        mRes = res;
        mSmileys = new IntTrie(
                res.getStringArray(BrandingResourceIDs.STRING_ARRAY_SMILEY_TEXTS), res.getSmileyIcons());
    }

    public final CharSequence markup(CharSequence text) {
        SpannableString result;

        if (text instanceof SpannableString) {
            result = (SpannableString) text;
        } else {
            result = new SpannableString(text);
        }

        Linkify.addLinks(result, Linkify.ALL);
        applyEmoticons(result);

        return result;
    }

    public final CharSequence applyEmoticons(CharSequence text) {
        int offset = 0;
        final int len = text.length();
        SpannableString result = null;

        while (offset < len) {
            int index = offset;
            IntTrie.Node n = mSmileys.getNode(text.charAt(index++));
            int candidate = 0;
            int lastMatchEnd = -1;

            //  Search the trie until we stop matching
            while (n != null) {
                //  Record the value and position of the longest match
                if (n.mValue != 0) {
                    candidate = n.mValue;
                    lastMatchEnd = index;
                }

                //  Let's not run off the end of the input
                if (index >= len) {
                    break;
                }

                n = n.getNode(text.charAt(index++));
            }

            //  If we matched a smiley, apply its image over the text
            if (candidate != 0) {
                //  Lazy-convert the result text to a SpannableString if we have to
                if (result == null) {
                    if (text instanceof SpannableString) {
                        result = (SpannableString) text;
                    } else {
                        result = new SpannableString(text);
                        text = result;
                    }
                }
                Drawable smiley = mRes.getSmileyIcon(candidate);
                smiley.setBounds(0, 0, smiley.getIntrinsicWidth(), smiley.getIntrinsicHeight());
                result.setSpan(new ImageSpan(smiley, ImageSpan.ALIGN_BASELINE),
                    offset, lastMatchEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                candidate = 0;
            }

            //  if there was a match, start searching for the next one after it
            //  if no match, start at the next character
            if (lastMatchEnd != -1) {
                offset = lastMatchEnd;
                lastMatchEnd = -1;
            } else {
                offset++;
            }
        }

        //  If there were no modifications, return the original string
        if (result == null) {
            return text;
        }

        return result;
    }
}
