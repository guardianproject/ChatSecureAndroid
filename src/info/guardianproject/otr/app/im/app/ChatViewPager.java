package info.guardianproject.otr.app.im.app;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;

public class ChatViewPager extends ViewPager {

    public ChatViewPager(Context context) {
        super(context);

    }


    public ChatViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
       if(v != this && v instanceof ViewPager) {
          return true;
       }
       else if (v instanceof CompoundButton)
       {
           return false;
       }
       else
           return super.canScroll(v, checkV, dx, x, y);
    }
}
