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

import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;

import info.guardianproject.otr.app.im.R;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BlockedContactView extends LinearLayout {
    private ImageView mAvatar;
    private ImageView mBlockedIcon;
    private TextView  mLine1;
    private TextView  mLine2;

    public BlockedContactView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mAvatar = (ImageView) findViewById(R.id.avatar);
        mBlockedIcon = (ImageView)findViewById(R.id.blocked);
        mLine1  = (TextView) findViewById(R.id.line1);
        mLine2  = (TextView) findViewById(R.id.line2);
    }

    public void bind(Cursor cursor, Context mContext) {
        long providerId = cursor.getLong(BlockedContactsActivity.PROVIDER_COLUMN);
        String username = cursor.getString(BlockedContactsActivity.USERNAME_COLUMN);
        String nickname = cursor.getString(BlockedContactsActivity.NICKNAME_COLUMN);

        Drawable avatar = DatabaseUtils.getAvatarFromCursor(cursor,
                BlockedContactsActivity.AVATAR_COLUMN);

        if (avatar != null) {
            mAvatar.setImageDrawable(avatar);
        } else {
            mAvatar.setImageResource(R.drawable.avatar_unknown);
        }
        ImApp app = ImApp.getApplication((Activity)mContext);
        BrandingResources brandingRes = app.getBrandingResource(providerId);
        mBlockedIcon.setImageDrawable(brandingRes.getDrawable(BrandingResourceIDs.DRAWABLE_BLOCK));
        mLine1.setText(nickname);
        mLine2.setText(ImpsAddressUtils.getDisplayableAddress(username));
    }
}
