package info.guardianproject.otr.app.lang;

import info.guardianproject.otr.app.im.R;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.RadioButton;

public class BhoRadioButtonListAdapter extends BaseAdapter implements ListAdapter {
    private final List<? extends BhoOptions> mData;
    private final int mTextId, mSeparatorId, mItemViewId, mRadioButtonId;
    private final boolean mAreAllItemsSelectable;
    private final LayoutInflater mInflater;
    private int mSelectedPosition;
    private Object mCallback;
    
    public BhoRadioButtonListAdapter(Object callback, Context context, List<? extends BhoOptions> data, int itemViewId,
            int textId, int radioButtonId, int separatorId) {
        mData = data;
        mAreAllItemsSelectable = !data.contains(null);
        mSeparatorId = separatorId;
        mTextId = textId;
        mRadioButtonId = radioButtonId;
        mItemViewId = itemViewId;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mCallback = callback;
        
        int i = 0;
        for(BhoOptions o : mData) {
            if(o.getChecked())
                mSelectedPosition = i;
            i++;
        }
    }
    
    public BhoRadioButtonListAdapter(Object callback, Context context, List<? extends BhoOptions> data) {
        this(callback, context, data, R.layout.bho_radiobuttonlist, R.id.text, R.id.radio_button, R.id.separator);
    }
    
    public interface OnBhoSelectedListener {
        public void onItemSelected(int which);
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
    
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        OnBhoSelectedListener bsl = (OnBhoSelectedListener) mCallback;
        bsl.onItemSelected(mSelectedPosition);
    }
    
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        convertView = mInflater.inflate(mItemViewId, parent, false);;
        
        BhoTextView tv = (BhoTextView) convertView.findViewById(mTextId);
        RadioButton rb = (RadioButton) convertView.findViewById(mRadioButtonId);
        View separator = convertView.findViewById(mSeparatorId);

        if (!isEnabled(position)) {
            if (tv != null) {
                tv.setVisibility(View.GONE);
            }
            if (separator != null) {
                separator.setVisibility(View.VISIBLE);
            }
            if (rb != null) {
                rb.setVisibility(View.GONE);
            }
        } else {
            if (separator != null) {
                separator.setVisibility(View.GONE);
            }
            
            BhoOptions option = mData.get(position);
            if (tv != null) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(option.getLabel());
            }
            
            if (rb != null) {
                rb.setVisibility(View.VISIBLE);
                if(position == mSelectedPosition) {
                    rb.setChecked(true);
                    option.setChecked(true);
                } else {
                    rb.setChecked(false);
                    option.setChecked(false);
                }
                                
                rb.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mSelectedPosition = position;
                        BhoRadioButtonListAdapter.this.notifyDataSetChanged();
                    }
                    
                });
            }
        }
        
        return convertView;
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
