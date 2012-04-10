package info.guardianproject.otr;

import info.guardianproject.bouncycastle.util.encoders.Hex;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.OtrKeyManagerListener;
import net.java.otr4j.OtrKeyManagerStore;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.SessionID;

import org.jivesoftware.smack.util.Base64;

public class OtrAndroidKeyManagerImpl implements OtrKeyManager {

	private SimplePropertiesStore store;

	private OtrCryptoEngineImpl cryptoEngine;
	
	private final static String KEY_ALG = "DSA";
	private final static int KEY_SIZE = 1024;
	
	private static OtrAndroidKeyManagerImpl _instance;
	
	public static synchronized OtrAndroidKeyManagerImpl getInstance (String filepath) throws IOException
	{
		if (_instance == null)
		{
			_instance = new OtrAndroidKeyManagerImpl(filepath);
		}
		
		return _instance;
	}
	
	private OtrAndroidKeyManagerImpl(String filepath) throws IOException {
		this.store = new SimplePropertiesStore(filepath);

		cryptoEngine = new OtrCryptoEngineImpl();
	}

	class SimplePropertiesStore implements OtrKeyManagerStore {
		private Properties properties = new Properties();
		private File mStoreFile;
		
		public SimplePropertiesStore(String filepath) {
			mStoreFile = new File(filepath);
			properties.clear();
			
			load();
		}
		
		private void load() 
		{
			try
			{

				FileInputStream fis = new FileInputStream(mStoreFile);
				
				InputStream in = new BufferedInputStream(fis);
				try {
					properties.load(in);
				} finally {
					in.close();
				}
			}
			catch (FileNotFoundException fnfe)
			{
				OtrDebugLogger.log("Properties store file not found: First time?");
				mStoreFile.getParentFile().mkdirs();
				
				try
				{
						store();
				}
				catch (Exception ioe)
				{
					OtrDebugLogger.log("Properties store error",ioe);
				}
				
			}
			catch (IOException ioe)
			{
				OtrDebugLogger.log("Properties store error",ioe);
			}
		}


		public void setProperty(String id, boolean value) {
			properties.setProperty(id, "true");
			try {
				this.store();
			} catch (Exception e) {
				OtrDebugLogger.log("Properties store error",e);
			}
		}

		private void store() throws FileNotFoundException, IOException {
			
			
			//Log.d(TAG,"saving otr keystore to: " + filepath);
			

			FileOutputStream fos = new FileOutputStream(mStoreFile);

			properties.store(fos, null);
			
			
			fos.close();
		}

		public void setProperty(String id, byte[] value) {
			properties.setProperty(id, new String(Base64.encodeBytes(value)));
			
			try {
				this.store();
			} catch (Exception e) {
				OtrDebugLogger.log("store not saved",e);
			}
		}

		// Store as hex bytes
		public void setPropertyHex(String id, byte[] value) {
			properties.setProperty(id, new String(Hex.encode(value)));
			
			try {
				this.store();
			} catch (Exception e) {
				OtrDebugLogger.log("store not saved",e);
			}
		}

		public void removeProperty(String id) {
			properties.remove(id);

		}

		public byte[] getPropertyBytes(String id) {
			String value = properties.getProperty(id);
			
			if (value != null)
				return Base64.decode(value);
			return 
				null;
		}


		// Load from hex bytes
		public byte[] getPropertyHexBytes(String id) {
			String value = properties.getProperty(id);
			
			if (value != null)
				return Hex.decode(value);
			return 
				null;
		}

		public boolean getPropertyBoolean(String id, boolean defaultValue) {
			try {
				return Boolean.valueOf(properties.get(id).toString());
			} catch (Exception e) {
				return defaultValue;
			}
		}
	}


	private List<OtrKeyManagerListener> listeners = new Vector<OtrKeyManagerListener>();

	public void addListener(OtrKeyManagerListener l) {
		synchronized (listeners) {
			if (!listeners.contains(l))
				listeners.add(l);
		}
	}

	public void removeListener(OtrKeyManagerListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	public void generateLocalKeyPair(SessionID sessionID) {
		if (sessionID == null)
			return;

		String accountID = sessionID.getAccountID();
		
		generateLocalKeyPair(accountID);
	}
	
	public void generateLocalKeyPair(String accountID) {

		OtrDebugLogger.log( "generating local key pair for: " + accountID);
		
		KeyPair keyPair;
		try {
			
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALG);
			kpg.initialize(KEY_SIZE);
			
			keyPair = kpg.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			OtrDebugLogger.log("no such algorithm",e);
			return;
		}

		OtrDebugLogger.log( "SUCCESS! generating local key pair for: " + accountID);

		// Store Public Key.
		PublicKey pubKey = keyPair.getPublic();
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(pubKey
				.getEncoded());

		this.store.setProperty(accountID + ".publicKey", x509EncodedKeySpec
				.getEncoded());

		// Store Private Key.
		PrivateKey privKey = keyPair.getPrivate();
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
				privKey.getEncoded());

		this.store.setProperty(accountID + ".privateKey", pkcs8EncodedKeySpec
				.getEncoded());

		// Stash fingerprint for consistency.
		try {
			String fingerprintString = new OtrCryptoEngineImpl().getFingerprint(pubKey);
			this.store.setPropertyHex(accountID + ".fingerprint",
					Hex.decode(fingerprintString));
		} catch (OtrCryptoException e) {
			e.printStackTrace();
		}
	}

	public String getLocalFingerprint(SessionID sessionID) {
		return getLocalFingerprint(sessionID.getAccountID());
	}
	
	public String getLocalFingerprint(String userId) {
		KeyPair keyPair = loadLocalKeyPair(userId);

		if (keyPair == null)
			return null;

		PublicKey pubKey = keyPair.getPublic();

		try {
			String fingerprint = cryptoEngine.getFingerprint(pubKey);
			
			OtrDebugLogger.log("got fingerprint for: " + userId + "=" + fingerprint);
			
			return fingerprint;
			
		} catch (OtrCryptoException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String getRemoteFingerprint(SessionID sessionID) {
		return getRemoteFingerprint(sessionID.getUserID());
	}

	public String getRemoteFingerprint(String userId) {
		byte[] fingerprint = this.store.getPropertyHexBytes(userId + ".fingerprint");
		if (fingerprint != null) {
			// If we have a fingerprint stashed, assume it is correct.
			return new String(Hex.encode(fingerprint, 0, fingerprint.length));
		}
		PublicKey remotePublicKey = loadRemotePublicKeyFromStore(userId);
		if (remotePublicKey == null)
			return null;
		try {
			// Store the fingerprint, for posterity.
			String fingerprintString =
					new OtrCryptoEngineImpl().getFingerprint(remotePublicKey);
			this.store.setPropertyHex(userId + ".fingerprint", Hex.decode(fingerprintString));
			return fingerprintString;
		} catch (OtrCryptoException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean isVerified(SessionID sessionID) {
		if (sessionID == null)
			return false;

		String userId = sessionID.getUserID();
		String pubKeyVerifiedToken = buildPublicKeyVerifiedId(userId, getRemoteFingerprint(userId));
		
		return this.store.getPropertyBoolean(pubKeyVerifiedToken, false);
	}
	
	public boolean isVerifiedUser(String userId) {
		if (userId == null)
			return false;
		

		String pubKeyVerifiedToken = buildPublicKeyVerifiedId(userId, getRemoteFingerprint(userId));
		
		return this.store.getPropertyBoolean(pubKeyVerifiedToken, false);
	}

	public KeyPair loadLocalKeyPair(SessionID sessionID) {
		if (sessionID == null)
			return null;

		String accountID = sessionID.getAccountID();
		return loadLocalKeyPair(accountID);
	}
	
	public KeyPair loadLocalKeyPair(String accountID) {
		
		// Load Private Key.
		
		byte[] b64PrivKey = this.store.getPropertyBytes(accountID
				+ ".privateKey");
		if (b64PrivKey == null)
			return null;

		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(b64PrivKey);

		// Load Public Key.
		byte[] b64PubKey = this.store
				.getPropertyBytes(accountID + ".publicKey");
		if (b64PubKey == null)
			return null;

		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(b64PubKey);

		PublicKey publicKey;
		PrivateKey privateKey;

		// Generate KeyPair.
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance(KEY_ALG);
			publicKey = keyFactory.generatePublic(publicKeySpec);
			privateKey = keyFactory.generatePrivate(privateKeySpec);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			return null;
		}

		return new KeyPair(publicKey, privateKey);
	}

	public PublicKey loadRemotePublicKey(SessionID sessionID) {

		return loadRemotePublicKeyFromStore(sessionID.getUserID());
	}
	
	public PublicKey loadRemotePublicKeyFromStore(String userID) {
	

		byte[] b64PubKey = this.store.getPropertyBytes(userID + ".publicKey");
		if (b64PubKey == null)
		{
			return null;
		
		}

		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(b64PubKey);

		// Generate KeyPair from spec
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance(KEY_ALG);
			
			return keyFactory.generatePublic(publicKeySpec);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void savePublicKey(SessionID sessionID, PublicKey pubKey) {
		if (sessionID == null)
			return;

		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(pubKey
				.getEncoded());
		
		String userId = sessionID.getUserID();
		this.store.setProperty(userId + ".publicKey", x509EncodedKeySpec
				.getEncoded());
		// Stash the associated fingerprint.  This saves calculating it in the future
		// and is useful for transferring rosters to other apps.
		try {
			String fingerprintString = new OtrCryptoEngineImpl().getFingerprint(pubKey);
			this.store.setPropertyHex(userId + ".fingerprint",
					Hex.decode(fingerprintString));
		} catch (OtrCryptoException e) {
			e.printStackTrace();
		}
	}

	public void unverify(SessionID sessionID) {
		if (sessionID == null)
			return;

		if (!isVerified(sessionID))
			return;

		String userId = sessionID.getUserID();
		
		this.store
				.removeProperty(buildPublicKeyVerifiedId(userId, getRemoteFingerprint(userId)));

		for (OtrKeyManagerListener l : listeners)
			l.verificationStatusChanged(sessionID);

	}
	
	public void unverifyUser(String userId) {
		if (userId == null)
			return;

		if (!isVerifiedUser(userId))
			return;

		this.store
				.removeProperty(buildPublicKeyVerifiedId(userId, getRemoteFingerprint(userId)));

	//	for (OtrKeyManagerListener l : listeners)
		//	l.verificationStatusChanged(sessionID);

	}


	public void verify(SessionID sessionID) {
		if (sessionID == null)
			return;

		if (this.isVerified(sessionID))
			return;

		String userId = sessionID.getUserID();
		
		this.store.setProperty(buildPublicKeyVerifiedId(userId, getRemoteFingerprint(userId)),true);

		for (OtrKeyManagerListener l : listeners)
			l.verificationStatusChanged(sessionID);
	}
	
	private static String buildPublicKeyVerifiedId (String userId, String fingerprint)
	{
		return userId + "." + fingerprint + ".publicKey.verified";
	}
	
	public void verifyUser(String userId) {
		if (userId == null)
			return;

		if (this.isVerifiedUser(userId))
			return;

		this.store.setProperty(buildPublicKeyVerifiedId(userId, getRemoteFingerprint(userId)),
				true);
		
		//for (OtrKeyManagerListener l : listeners)
			//l.verificationStatusChanged(userId);
		
	
	}

}
