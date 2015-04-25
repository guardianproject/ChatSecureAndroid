package info.guardianproject.emoji;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
public class EmojiManager {
	
	
	private static EmojiManager mInstance = null;

	private Map<Pattern, Emoji> emoticons = new HashMap<Pattern, Emoji>();
	private Map<String, EmojiGroup> categories = new HashMap<String, EmojiGroup>();
	
	private Context mContext;
	
	private final static String PLUGIN_CONSTANT = "info.guardianproject.emoji.STICKER_PACK";
	
	private EmojiManager (Context context)
	{
		mContext = context;
	}
	
	public void addJsonPlugins () throws IOException, JsonSyntaxException
	{
		PackageManager packageManager = mContext.getPackageManager();
		Intent stickerIntent = new Intent(PLUGIN_CONSTANT);
		List<ResolveInfo> stickerPack = packageManager.queryIntentActivities(stickerIntent, 0);
		
		for (ResolveInfo ri : stickerPack)
		{
			
			try {
				Resources res = packageManager.getResourcesForApplication(ri.activityInfo.applicationInfo);
				
				String[] files = res.getAssets().list("");
				
				for (String file : files)
				{
					if (file.endsWith(".json"))
						addJsonDefinitions(file,file.substring(0,file.length()-5),"png",res);
				}
				
			} catch (NameNotFoundException e) {
				Log.e("emoji","unable to find application for emoji plugin");
			}
		}
		
	}
	
	public void addJsonDefinitions (String assetPathJson, String basePath, String fileExt) throws IOException, JsonSyntaxException
	{
		addJsonDefinitions (assetPathJson, basePath, fileExt, mContext.getResources());
	}
	
	public void addJsonDefinitions (String assetPathJson, String basePath, String fileExt, Resources res) throws IOException, JsonSyntaxException
	{
	
		Gson gson = new Gson();
		
		Reader reader = new InputStreamReader(res.getAssets().open(assetPathJson));
		
		Type collectionType = new TypeToken<ArrayList<Emoji>>(){}.getType();
		Collection<Emoji> emojis = gson.fromJson(reader, collectionType );
		
		for (Emoji emoji : emojis)
		{
			emoji.assetPath = basePath + '/' + emoji.name + '.' + fileExt;
			emoji.res = res;
			
			try
			{
				res.getAssets().open(emoji.assetPath);
				
				addPattern(':' + emoji.name + ':', emoji);
				
				if (emoji.moji != null)
					addPattern(emoji.moji, emoji);
				
				if (emoji.emoticon != null)
					addPattern(emoji.emoticon, emoji);

				
				if (emoji.category != null)
					addEmojiToCategory (emoji.category, emoji);
			}
			catch (FileNotFoundException fe)
			{
				//should not be added as a valid emoji
			}
		}
		
		
	}
	
	public Collection<EmojiGroup> getEmojiGroups ()
	{
		return categories.values();
	}
	
	public String getAssetPath (Emoji emoji)
	{
		return emoji.name;
	}
	
	public synchronized void addEmojiToCategory (String category, Emoji emoji)
	{
		EmojiGroup emojiGroup = categories.get(category);
		
		if (emojiGroup == null)
		{
			emojiGroup = new EmojiGroup();
			emojiGroup.category = category;
			emojiGroup.emojis = new ArrayList<Emoji>();
		}
		
		emojiGroup.emojis.add(emoji);
		
		categories.put(category, emojiGroup);
	}
	
	public static synchronized EmojiManager getInstance (Context context)
	{       
		
		if (mInstance == null)
			mInstance = new EmojiManager(context);
		
		return mInstance;
	}

	
	private void addPattern(String pattern, Emoji resource) {
		  
		emoticons.put(Pattern.compile(pattern,Pattern.LITERAL), resource);
		
	}
	
	private void addPattern(char charPattern, Emoji resource) {
		  
		emoticons.put(Pattern.compile(charPattern+"",Pattern.UNICODE_CASE), resource);
	}
	
	
	public boolean addEmoji(Context context, Spannable spannable) throws IOException {
		boolean hasChanges = false;
		for (Entry<Pattern, Emoji> entry : emoticons.entrySet()) 
		{
			Matcher matcher = entry.getKey().matcher(spannable);
			while (matcher.find()) {
				boolean set = true;
				for (ImageSpan span : spannable.getSpans(matcher.start(),
				        matcher.end(), ImageSpan.class))
					
				    if (spannable.getSpanStart(span) >= matcher.start()
				            && spannable.getSpanEnd(span) <= matcher.end())
				        spannable.removeSpan(span);
				    else {
				        set = false;
				        break;
				    }
				if (set) {
				    hasChanges = true;
				    
				    Emoji emoji = entry.getValue();
				    spannable.setSpan(new ImageSpan(context, BitmapFactory.decodeStream(emoji.res.getAssets().open(emoji.assetPath))),
				            matcher.start(), matcher.end(),
				            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
		}
		return hasChanges;
	}


}
