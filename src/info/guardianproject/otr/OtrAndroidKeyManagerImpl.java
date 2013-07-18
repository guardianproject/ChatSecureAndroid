package info.guardianproject.otr;

import info.guardianproject.bouncycastle.util.encoders.Hex;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.util.Version;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.OtrKeyManagerListener;
import net.java.otr4j.OtrKeyManagerStore;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.SessionID;

import org.jivesoftware.smack.util.Base64;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.Sets;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class OtrAndroidKeyManagerImpl implements OtrKeyManager {

    private static final boolean REGENERATE_LOCAL_PUBLIC_KEY = false;

    private SimplePropertiesStore store;

    private OtrCryptoEngineImpl cryptoEngine;

    private final static String KEY_ALG = "DSA";
    private final static int KEY_SIZE = 1024;
    private final static Version CURRENT_VERSION = new Version("1.0.0");

    private static OtrAndroidKeyManagerImpl _instance;

    public static synchronized OtrAndroidKeyManagerImpl getInstance(Context context)
            throws IOException {
        if (_instance == null) {
            File f = new File(context.getApplicationContext().getFilesDir(), "otr_keystore");
            _instance = new OtrAndroidKeyManagerImpl(f);
        }

        return _instance;
    }

    private OtrAndroidKeyManagerImpl(File filepath) throws IOException {
        this.store = new SimplePropertiesStore(filepath);
        upgradeStore();

        cryptoEngine = new OtrCryptoEngineImpl();
    }
    
    private void upgradeStore() {
        String version = store.getPropertyString("version");

        if (version == null || new Version(version).compareTo(new Version("1.0.0")) < 0) {
            // Add verified=false entries for TOFU sync purposes
            Set<Object> keys = Sets.newHashSet(store.getProperties().keySet()); 
            for (Object keyObject : keys) {
                String key = (String)keyObject;
                if (key.endsWith(".fingerprint")) {
                    String fullUserId = key.replaceAll(".fingerprint$", "");
                    String fingerprint = store.getPropertyString(key);
                    String verifiedKey = buildPublicKeyVerifiedId(fullUserId, fingerprint);
                    if (!store.getProperties().contains(verifiedKey)) {
                        // Avoid save
                        store.getProperties().setProperty(verifiedKey, "false");
                    }
                }
            }
            // This will save
            store.setProperty("version", CURRENT_VERSION.toString());
        }
    }

    static class SimplePropertiesStore implements OtrKeyManagerStore {
        private Properties mProperties = new Properties();
        private File mStoreFile;

        public SimplePropertiesStore(File storeFile) {
            mStoreFile = storeFile;
            mProperties.clear();

            load();
        }

        public SimplePropertiesStore(File storeFile, final String password) {
            OtrDebugLogger.log("Loading store from encrypted file");
            mStoreFile = storeFile;
            mProperties.clear();

            loadAES(password);
        }

        private void loadAES(final String password) {
            String decoded;
            try {
                decoded = AES_256_CBC.decrypt(mStoreFile, password);
                mProperties.load(new ByteArrayInputStream(decoded.getBytes()));
            } catch (IOException ioe) {
                OtrDebugLogger.log("Properties store error", ioe);
            }
        }

        private void load() {
            try {

                FileInputStream fis = new FileInputStream(mStoreFile);

                InputStream in = new BufferedInputStream(fis);
                try {
                    mProperties.load(in);
                } finally {
                    in.close();
                }
            } catch (FileNotFoundException fnfe) {
                OtrDebugLogger.log("Properties store file not found: First time?");
                mStoreFile.getParentFile().mkdirs();

                try {
                    save();
                } catch (Exception ioe) {
                    OtrDebugLogger.log("Properties store error", ioe);
                }

            } catch (IOException ioe) {
                OtrDebugLogger.log("Properties store error", ioe);
            }
        }
        
        public Properties getProperties ()
        {
            return mProperties;
        }

        public void setProperty(String id, String value) {
            mProperties.setProperty(id, value);
            try {
                this.save();
            } catch (Exception e) {
                OtrDebugLogger.log("Properties store error", e);
            }
        }

        public void setProperty(String id, boolean value) {
            mProperties.setProperty(id, Boolean.toString(value));
            try {
                this.save();
            } catch (Exception e) {
                OtrDebugLogger.log("Properties store error", e);
            }
        }

        private void save() throws FileNotFoundException, IOException {

            //Log.d(TAG,"saving otr keystore to: " + filepath);

            FileOutputStream fos = new FileOutputStream(mStoreFile);

            mProperties.store(fos, null);

            fos.close();
        }

        private void saveAES (String password)
        {
            
        }
        
        public void setProperty(String id, byte[] value) {
            mProperties.setProperty(id, new String(Base64.encodeBytes(value)));

            try {
                this.save();
            } catch (Exception e) {
                OtrDebugLogger.log("store not saved", e);
            }
        }

        // Store as hex bytes
        public void setPropertyHex(String id, byte[] value) {
            mProperties.setProperty(id, new String(Hex.encode(value)));

            try {
                this.save();
            } catch (Exception e) {
                OtrDebugLogger.log("store not saved", e);
            }
        }

        public void removeProperty(String id) {
            mProperties.remove(id);

        }

        public String getPropertyString(String id) {
            return mProperties.getProperty(id);
        }
        
        public byte[] getPropertyBytes(String id) {
            String value = mProperties.getProperty(id);

            if (value != null)
                return Base64.decode(value);
            return null;
        }

        // Load from hex bytes
        public byte[] getPropertyHexBytes(String id) {
            String value = mProperties.getProperty(id);

            if (value != null)
                return Hex.decode(value);
            return null;
        }

        public boolean getPropertyBoolean(String id, boolean defaultValue) {
            try {
                return Boolean.valueOf(mProperties.get(id).toString());
            } catch (Exception e) {
                return defaultValue;
            }
        }

        public boolean hasProperty(String id) {
            return mProperties.containsKey(id);
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

    public void regenerateLocalPublicKey(KeyFactory factory, String accountID, DSAPrivateKey privKey) {
        BigInteger x = privKey.getX();
        DSAParams params = privKey.getParams();
        BigInteger y = params.getG().modPow(x, params.getP());
        DSAPublicKeySpec keySpec = new DSAPublicKeySpec(y, params.getP(), params.getQ(), params.getG());
        PublicKey pubKey;
        try {
            pubKey = factory.generatePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        storeLocalPublicKey(accountID, pubKey);
    }
    
    public void generateLocalKeyPair(String accountID) {

        OtrDebugLogger.log("generating local key pair for: " + accountID);

        KeyPair keyPair;
        try {

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALG);
            kpg.initialize(KEY_SIZE);

            keyPair = kpg.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            OtrDebugLogger.log("no such algorithm", e);
            return;
        }

        OtrDebugLogger.log("SUCCESS! generating local key pair for: " + accountID);

        // Store Private Key.
        PrivateKey privKey = keyPair.getPrivate();
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privKey.getEncoded());

        this.store.setProperty(accountID + ".privateKey", pkcs8EncodedKeySpec.getEncoded());

        // Store Public Key.
        PublicKey pubKey = keyPair.getPublic();
        storeLocalPublicKey(accountID, pubKey);
    }

    private void storeLocalPublicKey(String accountID, PublicKey pubKey) {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(pubKey.getEncoded());

        this.store.setProperty(accountID + ".publicKey", x509EncodedKeySpec.getEncoded());

        // Stash fingerprint for consistency.
        try {
            String fingerprintString = new OtrCryptoEngineImpl().getFingerprint(pubKey);
            this.store.setPropertyHex(accountID + ".fingerprint", Hex.decode(fingerprintString));
        } catch (OtrCryptoException e) {
            e.printStackTrace();
        }
    }
    
    public void importKeyStore(File filePath, String password, boolean overWriteExisting, boolean deleteImportedFile) throws IOException
    {
        SimplePropertiesStore storeNew = null;

        if (filePath.getName().endsWith(".ofcaes")) {
            //TODO implement GUI to get password via QR Code, and handle wrong password
            storeNew = new SimplePropertiesStore(filePath, password);
            deleteImportedFile = true; // once its imported, its no longer needed
        } else
            storeNew = new SimplePropertiesStore(filePath);
        
        Properties pNew = storeNew.getProperties();
        Enumeration<Object> enumKeys = pNew.keys();
        
        Object key;
        
        while (enumKeys.hasMoreElements())
        {
            key = enumKeys.nextElement();
            
            boolean hasKey = store.getProperties().containsKey(key);
            
        //    Log.d("OTR","importing key: " + key.toString());
            
            if (!hasKey || overWriteExisting)
                store.getProperties().put(key, pNew.get(key));
            
        }

        store.save();

        if (deleteImportedFile)
            filePath.delete();
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
        return getRemoteFingerprint(sessionID.getFullUserID());
    }

    public String getRemoteFingerprint(String userId) {
        if (!Address.hasResource(userId))
            return null;
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
            String fingerprintString = new OtrCryptoEngineImpl().getFingerprint(remotePublicKey);
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
        String fullUserID = sessionID.getFullUserID();
        if (!Address.hasResource(fullUserID))
            throw new IllegalArgumentException("User ID is not full JID");
        String pubKeyVerifiedToken = buildPublicKeyVerifiedId(userId, getRemoteFingerprint(fullUserID));

        return this.store.getPropertyBoolean(pubKeyVerifiedToken, false);
    }

    public boolean isVerifiedUser(String fullUserId) {
        if (fullUserId == null)
            return false;

        if (!Address.hasResource(fullUserId))
            return false;

        String userId = Address.stripResource(fullUserId);
        String pubKeyVerifiedToken = buildPublicKeyVerifiedId(userId, getRemoteFingerprint(fullUserId));

        return this.store.getPropertyBoolean(pubKeyVerifiedToken, false);
    }

    public KeyPair loadLocalKeyPair(SessionID sessionID) {
        if (sessionID == null)
            return null;

        String accountID = sessionID.getAccountID();
        return loadLocalKeyPair(accountID);
    }

    private KeyPair loadLocalKeyPair(String accountID) {
        PublicKey publicKey;
        PrivateKey privateKey;


        try {
            // Load Private Key.

            byte[] b64PrivKey = this.store.getPropertyBytes(accountID + ".privateKey");
            if (b64PrivKey == null)
                return null;

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(b64PrivKey);

            // Generate KeyPair.
            KeyFactory keyFactory;
            keyFactory = KeyFactory.getInstance(KEY_ALG);
            privateKey = keyFactory.generatePrivate(privateKeySpec);

            if (REGENERATE_LOCAL_PUBLIC_KEY) {
                regenerateLocalPublicKey(keyFactory, accountID, (DSAPrivateKey)privateKey);
            }
            
            // Load Public Key.
            byte[] b64PubKey = this.store.getPropertyBytes(accountID + ".publicKey");
            if (b64PubKey == null)
                return null;

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(b64PubKey);
            publicKey = keyFactory.generatePublic(publicKeySpec);
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

        return loadRemotePublicKeyFromStore(sessionID.getFullUserID());
    }

    private PublicKey loadRemotePublicKeyFromStore(String userId) {
        if (!Address.hasResource(userId))
            throw new IllegalArgumentException("User ID is not full JID");
        byte[] b64PubKey = this.store.getPropertyBytes(userId + ".publicKey");
        if (b64PubKey == null) {
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

        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(pubKey.getEncoded());

        String userId = sessionID.getFullUserID();
        if (!Address.hasResource(userId))
            throw new IllegalArgumentException("User ID is not full JID");
        
        this.store.setProperty(userId + ".publicKey", x509EncodedKeySpec.getEncoded());
        // Stash the associated fingerprint.  This saves calculating it in the future
        // and is useful for transferring rosters to other apps.
        try {
            String fingerprintString = new OtrCryptoEngineImpl().getFingerprint(pubKey);
            String verifiedToken = buildPublicKeyVerifiedId(userId, fingerprintString.toLowerCase());
            if (!this.store.hasProperty(verifiedToken))
                this.store.setProperty(verifiedToken, false);
            this.store.setPropertyHex(userId + ".fingerprint", Hex.decode(fingerprintString));
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

        this.store.setProperty(buildPublicKeyVerifiedId(userId, getRemoteFingerprint(sessionID)), false);

        for (OtrKeyManagerListener l : listeners)
            l.verificationStatusChanged(sessionID);

    }

    public void unverifyUser(String userId) {
        if (userId == null)
            return;

        if (!isVerifiedUser(userId))
            return;

        this.store.setProperty(buildPublicKeyVerifiedId(userId, getRemoteFingerprint(userId)), false);

        //	for (OtrKeyManagerListener l : listeners)
        //	l.verificationStatusChanged(sessionID);

    }

    public void verify(SessionID sessionID) {
        if (sessionID == null)
            return;

        if (this.isVerified(sessionID))
            return;

        String userId = sessionID.getUserID();

        this.store
                .setProperty(buildPublicKeyVerifiedId(userId, getRemoteFingerprint(sessionID)), true);

        for (OtrKeyManagerListener l : listeners)
            l.verificationStatusChanged(sessionID);
    }

    public void remoteVerifiedUs(SessionID sessionID) {
        if (sessionID == null)
            return;

        for (OtrKeyManagerListener l : listeners)
            l.remoteVerifiedUs(sessionID);
    }

    private static String buildPublicKeyVerifiedId(String userId, String fingerprint) {
        if (fingerprint == null)
            throw new IllegalArgumentException("No fingerprint");

        return Address.stripResource(userId) + "." + fingerprint + ".publicKey.verified";
    }

    public void verifyUser(String userId) {
        if (userId == null)
            return;

        if (this.isVerifiedUser(userId))
            return;

        this.store
                .setProperty(buildPublicKeyVerifiedId(userId, getRemoteFingerprint(userId)), true);

        //for (OtrKeyManagerListener l : listeners)
        //l.verificationStatusChanged(userId);

    }

    public static boolean checkForKeyImport (Intent intent, Activity activity)
    {
        boolean doKeyStoreImport = false;
        
        // if otr_keystore.ofcaes is in the SDCard root, import it
        File otrKeystoreAES = new File(Environment.getExternalStorageDirectory(),
                "otr_keystore.ofcaes");
        if (otrKeystoreAES.exists()) {
            //Log.i(TAG, "found " + otrKeystoreAES + "to import");
            doKeyStoreImport = true;
            importOtrKeyStore(otrKeystoreAES, activity);
        }
        else if (intent.getData() != null)
        {
            Uri uriData = intent.getData();
            String path = null;
            
            if(uriData.getScheme() != null && uriData.getScheme().equals("file"))
            {
                path = uriData.toString().replace("file://", "");
            
                File file = new File(path);
                
                doKeyStoreImport = true;
                
                importOtrKeyStore(file, activity);
            }
        }
        
        return doKeyStoreImport;
    }
    
    
    public static void importOtrKeyStore (final File fileOtrKeyStore, final Activity activity)
    {
     
        try
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

            prefs.edit().putString("keystoreimport", fileOtrKeyStore.getCanonicalPath()).commit();
        }
        catch (IOException ioe)
        {
            Log.e("TAG","problem importing key store",ioe);
            return;
        }

        Dialog.OnClickListener ocl = new Dialog.OnClickListener ()
        {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                
                //launch QR code intent
                IntentIntegrator.initiateScan(activity);
                
            }
        };
        

        new AlertDialog.Builder(activity).setTitle(R.string.confirm)
                  .setMessage(R.string.detected_Otr_keystore_import)
                  .setPositiveButton(R.string.yes, ocl) // default button
                  .setNegativeButton(R.string.no, null).setCancelable(true).show();
      
      
    }
    
    public static void importOtrKeyStoreWithPassword (final File fileOtrKeyStore, String password, Activity activity) throws IOException
    {

        OtrAndroidKeyManagerImpl oakm = OtrAndroidKeyManagerImpl.getInstance(activity);
        boolean overWriteExisting = true;
        boolean deleteImportedFile = true;
        oakm.importKeyStore(fileOtrKeyStore, password, overWriteExisting, deleteImportedFile);
        
    
    }
    
    public static boolean handleKeyScanResult (int requestCode, int resultCode, Intent data, Activity activity)
    {
        IntentResult scanResult =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data); 
        
        if  (scanResult != null) 
        { 
            
            String otrKeyPassword = scanResult.getContents();
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

            String otrKeyStorePath = prefs.getString("keystoreimport", null);
            
            Log.d("OTR","got password: " + otrKeyPassword + " for path: " + otrKeyStorePath);
            
            if (otrKeyPassword != null && otrKeyStorePath != null)
            {
                
                otrKeyPassword = otrKeyPassword.replace("\n","").replace("\r", ""); //remove any padding, newlines, etc
                
                try
                {
                    File otrKeystoreAES = new File(otrKeyStorePath);
                    if (otrKeystoreAES.exists()) {
                        OtrAndroidKeyManagerImpl.importOtrKeyStoreWithPassword(otrKeystoreAES, otrKeyPassword, activity);
                        return true;
                    }
                }
                catch (IOException e)
                {
                    Toast.makeText(activity, "unable to open keystore for import", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
            else
            {
                Log.d("OTR","no key store path saved");
                return false;
            }
            
        } 
        
        return false;
    }

}
