package info.guardianproject.otr;

import info.guardianproject.otr.app.im.app.WarningDialogActivity;
import info.guardianproject.otr.app.im.engine.ChatSessionManager;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.service.ChatSessionAdapter;
import info.guardianproject.otr.app.im.service.ChatSessionManagerAdapter;
import info.guardianproject.otr.app.im.service.ImConnectionAdapter;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrKeyManagerListener;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionID;
import android.content.Context;
import android.content.Intent;

/* OtrEngineHostImpl is the connects this app and the OtrEngine
 * http://code.google.com/p/otr4j/wiki/QuickStart
 */
public class OtrEngineHostImpl implements OtrEngineHost {
	
	private List<ImConnectionAdapter> mConnections;
	private OtrPolicy mPolicy;
    
	private OtrAndroidKeyManagerImpl mOtrKeyManager;

	private final static String OTR_KEYSTORE_PATH ="otr_keystore";
	
	private Context mContext;
	
	public OtrEngineHostImpl(OtrPolicy policy, Context context) throws IOException 
	{
		mPolicy = policy;
		mContext = context;
		
		File storeFile = new File(context.getFilesDir(), OTR_KEYSTORE_PATH);		
		mOtrKeyManager = OtrAndroidKeyManagerImpl.getInstance(storeFile.getAbsolutePath());
		
		mOtrKeyManager.addListener(new OtrKeyManagerListener() {
			public void verificationStatusChanged(SessionID session) {
				
				String msg = session + ": verification status=" + mOtrKeyManager.isVerified(session);
				
				OtrDebugLogger.log( msg);
			}
		});
		
		mConnections = new ArrayList<ImConnectionAdapter>();
	}
	
	public void addConnection (ImConnectionAdapter connection)
	{
		mConnections.add(connection);
	}
	
        public void removeConnection (ImConnectionAdapter connection)
        {
                mConnections.remove(connection);
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
        
	public OtrAndroidKeyManagerImpl getKeyManager ()
	{
		return mOtrKeyManager;
	}
	
	public void storeRemoteKey (SessionID sessionID, PublicKey remoteKey)
	{
		mOtrKeyManager.savePublicKey(sessionID, remoteKey);
	}
	
	public boolean isRemoteKeyVerified (SessionID sessionID)
	{
		return mOtrKeyManager.isVerified(sessionID);
	}
	
	public String getLocalKeyFingerprint (SessionID sessionID)
	{
		return mOtrKeyManager.getLocalFingerprint(sessionID);
	}
	
	public String getRemoteKeyFingerprint (SessionID sessionID)
	{
		return mOtrKeyManager.getRemoteFingerprint(sessionID);
	}
	
	@Override
	public KeyPair getKeyPair(SessionID sessionID) {
		KeyPair kp = null;
		kp = mOtrKeyManager.loadLocalKeyPair(sessionID);
		
		if (kp == null)
		{
         	mOtrKeyManager.generateLocalKeyPair(sessionID);	
         	kp = mOtrKeyManager.loadLocalKeyPair(sessionID);
		}
     	return kp;
	}

	@Override
	public OtrPolicy getSessionPolicy(SessionID sessionID) {
		return mPolicy;
	}
	
	public void setSessionPolicy (OtrPolicy policy)
	{
		mPolicy = policy;
	}
	
	private void sendMessage (SessionID sessionID, String body)
	{
	        ImConnectionAdapter connection = findConnection(sessionID.getAccountID());
		ChatSessionManagerAdapter chatSessionManagerAdapter = (ChatSessionManagerAdapter)connection.getChatSessionManager();
		ChatSessionAdapter chatSessionAdapter = (ChatSessionAdapter)chatSessionManagerAdapter.getChatSession(sessionID.getUserID());
		ChatSessionManager chatSessionManager = chatSessionManagerAdapter.getChatSessionManager();
		
		Message msg = new Message(body);
		
		msg.setFrom(connection.getLoginUser().getAddress());
		msg.setTo(chatSessionAdapter.getAdaptee().getParticipant().getAddress());
		msg.setDateTime(new Date());
		msg.setID(msg.getFrom() + ":" + msg.getDateTime().getTime());
		chatSessionManager.sendMessageAsync(chatSessionAdapter.getAdaptee(), msg);
		
	}
	
	@Override
	public void injectMessage(SessionID sessionID, String text) {
		OtrDebugLogger.log( sessionID.toString() + ": injecting message: " + text);
		
		sendMessage(sessionID,text);
		
	}

	@Override
	public void showError(SessionID sessionID, String error) {
		OtrDebugLogger.log( sessionID.toString() + ": ERROR=" + error);
		
    	showDialog ("Encryption Error", error);
	}

	@Override
	public void showWarning(SessionID sessionID, String warning) {
		OtrDebugLogger.log( sessionID.toString() + ": WARNING=" +  warning);
		
    	showDialog ("Encryption Warning", warning);

	}
	
	private void showDialog (String title, String msg)
	{

		Intent nIntent = new Intent(mContext, WarningDialogActivity.class);
		nIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		nIntent.putExtra("title", title);
		nIntent.putExtra("msg", msg);
		
		mContext.startActivity(nIntent);
		
	}

}
