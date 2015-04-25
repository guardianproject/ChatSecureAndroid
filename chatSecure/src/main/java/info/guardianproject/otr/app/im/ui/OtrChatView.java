package info.guardianproject.otr.app.im.ui;

import info.guardianproject.otr.IOtrChatSession;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class OtrChatView {

    private final static String TAG = "OtrChatView";

    private static IOtrChatSession mOtrChatSession = null;

    public static IOtrChatSession getOtrChatSession() {
        return mOtrChatSession;
    }

    /** Class for interacting with the main interface of the service. */
    private ServiceConnection mOtrChatSessionConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            mOtrChatSession = IOtrChatSession.Stub.asInterface(service);

        }

        public void onServiceDisconnected(ComponentName className) {
            mOtrChatSession = null;

        }
    };

    private void bindChatService(Context context) {
        context.bindService(new Intent(IOtrChatSession.class.getName()), mOtrChatSessionConnection,
                Context.BIND_AUTO_CREATE);

    }

    private void unbindChatService(Context context) {
        context.unbindService(mOtrChatSessionConnection);

    }
}
