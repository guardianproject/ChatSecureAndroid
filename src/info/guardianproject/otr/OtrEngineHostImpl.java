package info.guardianproject.otr;

import info.guardianproject.otr.app.im.ImService;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.WarningDialogActivity;
import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.ChatSessionManager;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.service.ChatSessionAdapter;
import info.guardianproject.otr.app.im.service.ChatSessionManagerAdapter;
import info.guardianproject.otr.app.im.service.ImConnectionAdapter;
import info.guardianproject.otr.app.im.service.RemoteImService;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrKeyManagerListener;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionID;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/*
 * OtrEngineHostImpl is the connects this app and the OtrEngine
 * http://code.google.com/p/otr4j/wiki/QuickStart
 */
public class OtrEngineHostImpl implements OtrEngineHost {

    private List<ImConnectionAdapter> mConnections;
    private OtrPolicy mPolicy;

    private OtrAndroidKeyManagerImpl mOtrKeyManager;

    private final static String OTR_KEYSTORE_PATH = "otr_keystore";

    private ImService mContext;

    private Hashtable<SessionID, String> mSessionResources;

    public OtrEngineHostImpl(OtrPolicy policy, ImService context) throws IOException {
        mPolicy = policy;
        mContext = context;

        mSessionResources = new Hashtable<SessionID, String>();

        File storeFile = new File(context.getApplicationContext().getFilesDir(), OTR_KEYSTORE_PATH);
        mOtrKeyManager = OtrAndroidKeyManagerImpl.getInstance(storeFile.getAbsolutePath());

        mOtrKeyManager.addListener(new OtrKeyManagerListener() {
            public void verificationStatusChanged(SessionID session) {
                String msg = session + ": verification status="
                             + mOtrKeyManager.isVerified(session);

                OtrDebugLogger.log(msg);
            }

            public void remoteVerifiedUs(SessionID session) {
                String msg = session + ": remote verified us";

                OtrDebugLogger.log(msg);
                if (!isRemoteKeyVerified(session))
                    showWarning(session, mContext.getApplicationContext().getString(R.string.remote_verified_us));
            }
        });

        mConnections = new ArrayList<ImConnectionAdapter>();
    }

    public void addConnection(ImConnectionAdapter connection) {
        mConnections.add(connection);
    }

    public void removeConnection(ImConnectionAdapter connection) {
        mConnections.remove(connection);
    }

    public void putSessionResource(SessionID session, String resource) {
        mSessionResources.put(session, resource);
    }

    public void removeSessionResource(SessionID session) {
        mSessionResources.remove(session);
    }

    public Address appendSessionResource(SessionID session, Address to) {
        String resource = mSessionResources.get(session);
        return to.appendResource(resource);
    }

    public ImConnectionAdapter findConnection(String localAddress) {
        for (ImConnectionAdapter connection : mConnections) {
            Contact user = connection.getLoginUser();
            if (user != null) {
                if (user.getAddress().getFullName().equals(localAddress))
                    return connection;
            }
        }
        return null;
    }

    public OtrAndroidKeyManagerImpl getKeyManager() {
        return mOtrKeyManager;
    }

    public void storeRemoteKey(SessionID sessionID, PublicKey remoteKey) {
        mOtrKeyManager.savePublicKey(sessionID, remoteKey);
    }

    public boolean isRemoteKeyVerified(SessionID sessionID) {
        return mOtrKeyManager.isVerified(sessionID);
    }

    public String getLocalKeyFingerprint(SessionID sessionID) {
        return mOtrKeyManager.getLocalFingerprint(sessionID);
    }

    public String getRemoteKeyFingerprint(SessionID sessionID) {
        return mOtrKeyManager.getRemoteFingerprint(sessionID);
    }

    public KeyPair getKeyPair(SessionID sessionID) {
        KeyPair kp = null;
        kp = mOtrKeyManager.loadLocalKeyPair(sessionID);

        if (kp == null) {
            mOtrKeyManager.generateLocalKeyPair(sessionID);
            kp = mOtrKeyManager.loadLocalKeyPair(sessionID);
        }
        return kp;
    }

    public OtrPolicy getSessionPolicy(SessionID sessionID) {
        return mPolicy;
    }

    public void setSessionPolicy(OtrPolicy policy) {
        mPolicy = policy;
    }

    private void sendMessage(SessionID sessionID, String body) {
        ImConnectionAdapter connection = findConnection(sessionID.getAccountID());
        ChatSessionManagerAdapter chatSessionManagerAdapter = (ChatSessionManagerAdapter) connection
                .getChatSessionManager();
        ChatSessionAdapter chatSessionAdapter = (ChatSessionAdapter) chatSessionManagerAdapter
                .getChatSession(sessionID.getUserID());
        ChatSessionManager chatSessionManager = chatSessionManagerAdapter.getChatSessionManager();

        Message msg = new Message(body);

        msg.setFrom(connection.getLoginUser().getAddress());
        final Address to = chatSessionAdapter.getAdaptee().getParticipant().getAddress();
        msg.setTo(appendSessionResource(sessionID, to));
        msg.setDateTime(new Date());
        msg.setID(msg.getFrom() + ":" + msg.getDateTime().getTime());
        chatSessionManager.sendMessageAsync(chatSessionAdapter.getAdaptee(), msg);
    }

    public void injectMessage(SessionID sessionID, String text) {
        OtrDebugLogger.log(sessionID.toString() + ": injecting message: " + text);

        sendMessage(sessionID, text);
    }

    public void showError(SessionID sessionID, String error) {
        OtrDebugLogger.log(sessionID.toString() + ": ERROR=" + error);

        showDialog("Encryption Error", sessionID.getUserID() + " : " + error);
    }

    public void showWarning(SessionID sessionID, String warning) {
        OtrDebugLogger.log(sessionID.toString() + ": WARNING=" + warning);
        
        if (!warning.contains(sessionID.getUserID()))
            warning = "[" + sessionID.getUserID() + "] " + warning; 

        showToast(warning);
    }

    private void showDialog(String title, String msg) {
        Intent nIntent = new Intent(mContext.getApplicationContext(), WarningDialogActivity.class);
        nIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        nIntent.putExtra("title", title);
        nIntent.putExtra("msg", msg);

        mContext.getApplicationContext().startActivity(nIntent);
    }
    
    private void showToast(String msg) {
        mContext.showToast(msg, Toast.LENGTH_LONG);
    }
}
