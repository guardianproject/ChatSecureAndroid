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

import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

public class ChatBackgroundMaker {
    private final Drawable mIncomingBg;
    private final Drawable mDivider;
    private final Rect mPadding;

    public ChatBackgroundMaker(Context context) {
        Resources res = context.getResources();
        mIncomingBg = res.getDrawable(R.drawable.textfield_im_received);
        mDivider = res.getDrawable(R.drawable.text_divider_horizontal);
        mPadding = new Rect();
        mIncomingBg.getPadding(mPadding);
    }

    public void setBackground(MessageView view, String contact, int type) {

    }
}
