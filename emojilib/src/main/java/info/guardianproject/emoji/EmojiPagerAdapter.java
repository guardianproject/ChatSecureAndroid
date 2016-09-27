package info.guardianproject.emoji;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;

public class EmojiPagerAdapter extends PagerAdapter {

	   
	   EmojiGridAdapter[] gias;
	   List<EmojiGroup> mEmojiGroups;
	   
	   Context mContext;
	   EditText mEditable;
	   
	   public EmojiPagerAdapter (Context context, EditText editable,List<EmojiGroup> emojiGroups)
	   {
		   super();
		   
		   mContext = context;
		   mEditable = editable;
		   
		   mEmojiGroups = emojiGroups;
		   gias = new EmojiGridAdapter[mEmojiGroups.size()];
		   
	   }
	   
	   @Override
	   public Object instantiateItem(View collection, int position) {
		
		   gias[position] = new EmojiGridAdapter(mContext,mEmojiGroups.get(position).emojis);
			
		   LayoutInflater inflater = (LayoutInflater)   mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);

		   GridView imagegrid = (GridView) inflater.inflate(R.layout.emojigrid, null);

		
			imagegrid.setAdapter(gias[position]);
			 
			 imagegrid.setOnItemClickListener(new OnItemClickListener () {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
							long arg3) {
						
						
						GridView gv = ((GridView)arg0);
						
						Emoji t = (Emoji)((EmojiGridAdapter)gv.getAdapter()).getItem(arg2);
						
						if (t.emoticon != null)
							mEditable.append(t.emoticon);
						else if (t.moji != null)
							mEditable.append(t.moji);
						else if (t.unicode == null)
							mEditable.append(':'+t.name+':');
						
						
						if (mEditable != null)
						{
							try
							{
								EmojiManager.getInstance(mContext.getApplicationContext()).addEmoji(mContext,  mEditable.getEditableText());
							}
							catch (IOException e)
							{
								Log.e("Emoji","error adding simle",e);
							}
						}
						
					}
					  
				  });     
			 
		        ((ViewPager)collection).addView(imagegrid);

		        
			 return imagegrid;
     	 
				  
	   }
	   
		@Override
		public int getCount() {
			return mEmojiGroups.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}
		
		@Override
		 public void destroyItem(ViewGroup collection, int position, Object arg2) {
		     ((ViewPager) collection).removeView((ViewGroup) arg2);}


		 @Override
		 public Parcelable saveState() {
		     return null;}


		@Override
		public void startUpdate(ViewGroup collection) {}

		@Override
		public void finishUpdate(ViewGroup collection) {}
		

        @Override
        public CharSequence getPageTitle(int position) {
           
        	return mEmojiGroups.get(position).category;
        	
        	
        }
	   
}