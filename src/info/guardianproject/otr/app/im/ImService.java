package info.guardianproject.otr.app.im;

import android.content.Context;

public interface ImService {
    public void showToast(CharSequence text, int duration);
    public Context getApplicationContext();
}
