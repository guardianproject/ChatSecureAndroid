package net.java.otr4j;

import info.guardianproject.bouncycastle.util.encoders.Base64;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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


import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.SessionID;

public class OtrKeyManagerImpl implements OtrKeyManager {

	private OtrKeyManagerStore store;

	public OtrKeyManagerImpl(OtrKeyManagerStore store) {
		this.store = store;
	}

	class DefaultPropertiesStore implements OtrKeyManagerStore {
		private final Properties properties = new Properties();
		private String filepath;

		public DefaultPropertiesStore(String filepath) throws IOException {
			if (filepath == null || filepath.length() < 1)
				throw new IllegalArgumentException();
			this.filepath = filepath;
			properties.clear();

			InputStream in = new BufferedInputStream(new FileInputStream(
					getConfigurationFile()));
			try {
				properties.load(in);
			} finally {
				in.close();
			}
		}

		private File getConfigurationFile() throws IOException {
			File configFile = new File(filepath);
			if (!configFile.exists())
				configFile.createNewFile();
			return configFile;
		}

		public void setProperty(String id, boolean value) {
			properties.setProperty(id, "true");
			try {
				this.store();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void store() throws FileNotFoundException, IOException {
			OutputStream out = new FileOutputStream(getConfigurationFile());
			properties.store(out, null);
			out.close();
		}

		public void setProperty(String id, byte[] value) {
			properties.setProperty(id, new String(Base64.encode(value)));
			try {
				this.store();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void removeProperty(String id) {
			properties.remove(id);

		}

		public byte[] getPropertyBytes(String id) {
			String value = properties.getProperty(id);
			return Base64.decode(value);
		}

		public boolean getPropertyBoolean(String id, boolean defaultValue) {
			try {
				return Boolean.valueOf(properties.get(id).toString());
			} catch (Exception e) {
				return defaultValue;
			}
		}
	}

	public OtrKeyManagerImpl(String filepath) throws IOException {
		this.store = new DefaultPropertiesStore(filepath);
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
		KeyPair keyPair;
		try {
			keyPair = KeyPairGenerator.getInstance("DSA").genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		}

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
	}

	public String getLocalFingerprint(SessionID sessionID) {
		KeyPair keyPair = loadLocalKeyPair(sessionID);

		if (keyPair == null)
			return null;

		PublicKey pubKey = keyPair.getPublic();

		try {
			return new OtrCryptoEngineImpl().getFingerprint(pubKey);
		} catch (OtrCryptoException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getRemoteFingerprint(SessionID sessionID) {
		PublicKey remotePublicKey = loadRemotePublicKey(sessionID);
		if (remotePublicKey == null)
			return null;
		try {
			return new OtrCryptoEngineImpl().getFingerprint(remotePublicKey);
		} catch (OtrCryptoException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean isVerified(SessionID sessionID) {
		if (sessionID == null)
			return false;

		return this.store.getPropertyBoolean(sessionID.getUserID()
				+ ".publicKey.verified", false);
	}

	public KeyPair loadLocalKeyPair(SessionID sessionID) {
		if (sessionID == null)
			return null;

		String accountID = sessionID.getAccountID();
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
			keyFactory = KeyFactory.getInstance("DSA");
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
		if (sessionID == null)
			return null;

		String userID = sessionID.getUserID();

		byte[] b64PubKey = this.store.getPropertyBytes(userID + ".publicKey");
		if (b64PubKey == null)
			return null;

		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(b64PubKey);

		// Generate KeyPair.
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance("DSA");
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

		String userID = sessionID.getUserID();
		this.store.setProperty(userID + ".publicKey", x509EncodedKeySpec
				.getEncoded());

		this.store.removeProperty(userID + ".publicKey.verified");
	}

	public void unverify(SessionID sessionID) {
		if (sessionID == null)
			return;

		if (!isVerified(sessionID))
			return;

		this.store
				.removeProperty(sessionID.getUserID() + ".publicKey.verified");

		for (OtrKeyManagerListener l : listeners)
			l.verificationStatusChanged(sessionID);

	}

	public void verify(SessionID sessionID) {
		if (sessionID == null)
			return;

		if (this.isVerified(sessionID))
			return;

		this.store.setProperty(sessionID.getUserID() + ".publicKey.verified",
				true);

		for (OtrKeyManagerListener l : listeners)
			l.verificationStatusChanged(sessionID);
	}

}
