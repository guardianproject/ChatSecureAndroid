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

import java.util.List;

import info.guardianproject.otr.app.im.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * A general image list adapter.
 */
public class ImageListAdapter extends BaseAdapter implements ListAdapter {

    public static interface ImageListItem {
        public Drawable getDrawable();

        public CharSequence getText();
    }

    private final LayoutInflater mInflater;
    private final List<? extends ImageListItem> mData;
    private final int mItemViewId;
    private final int mImageId;
    private final int mTextId;
    private final boolean mAreAllItemsSelectable;
    private final int mSeparatorId;

    public ImageListAdapter(Context context, List<? extends ImageListItem> data) {
        this(context, data, R.layout.imglist_item, R.id.image, R.id.text, R.id.separator);
    }

    public ImageListAdapter(Context context, List<? extends ImageListItem> data,
            int itemViewId, int imgId, int textId, int separatorId) {
        mData = data;
        mItemViewId = itemViewId;
        mImageId = imgId;
        mTextId = textId;
        mAreAllItemsSelectable = !data.contains(null);
        mSeparatorId = separatorId;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return mData.size();
    }

    public Object getItem(int position) {
        return mData.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = mInflater.inflate(mItemViewId, parent, false);
        } else {
            v = convertView;
        }
        setupView(position, v);
        return v;
    }

    private void setupView(int position, View view) {
        ImageView iv = (ImageView) view.findViewById(mImageId);
        TextView tv = (TextView)view.findViewById(mTextId);
        View separator = view.findViewById(mSeparatorId);

        if (!isEnabled(position)) {
            if (iv != null) {
                iv.setVisibility(View.GONE);
            }
            if (tv != null) {
                tv.setVisibility(View.GONE);
            }
            if (separator != null) {
                separator.setVisibility(View.VISIBLE);
            }
        } else {
            if (separator != null) {
                separator.setVisibility(View.GONE);
            }
            final ImageListItem item = mData.get(position);
            if (iv != null) {
                iv.setVisibility(View.VISIBLE);
                iv.setImageDrawable(item.getDrawable());
            }
            if (tv != null) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(item.getText());
            }
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mAreAllItemsSelectable;
    }

    @Override
    public boolean isEnabled(int position) {
        return mData.get(position) != null;
    }

}
