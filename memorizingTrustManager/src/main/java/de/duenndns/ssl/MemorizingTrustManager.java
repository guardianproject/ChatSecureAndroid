/* MemorizingTrustManager - a TrustManager which asks the user about invalid
 *  certificates and memorizes their decision.
 *
 * Copyright (c) 2010 Georg Lukas <georg@op-co.de>
 *
 * MemorizingTrustManager.java contains the actual trust manager and interface
 * code to create a MemorizingActivity and obtain the results.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.duenndns.ssl;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;
import android.os.Handler;

import java.io.File;
import java.security.cert.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * A X509 trust manager implementation which asks the user about invalid
 * certificates and memorizes their decision.
 * <p>
 * The certificate validity is checked using the system default X509
 * TrustManager, creating a query Dialog if the check fails.
 * <p>
 * <b>WARNING:</b> This only works if a dedicated thread is used for
 * opening sockets!
 */
public class MemorizingTrustManager implements X509TrustManager {
	final static String TAG = "MemorizingTrustManager";
	final static String DECISION_INTENT = "de.duenndns.ssl.DECISION";
	final static String DECISION_INTENT_ID     = DECISION_INTENT + ".decisionId";
	final static String DECISION_INTENT_CERT   = DECISION_INTENT + ".cert";
	final static String DECISION_INTENT_CHOICE = DECISION_INTENT + ".decisionChoice";
	final static String DECISION_TITLE_ID      = DECISION_INTENT + ".titleId";
	private final static int NOTIFICATION_ID = 100509;

	final static String NO_TRUST_ANCHOR = "Trust anchor for certification path not found.";
	
	static String KEYSTORE_DIR = "KeyStore";
	static String KEYSTORE_FILE = "KeyStore.bks";

	Context master;
	Activity foregroundAct;
	NotificationManager notificationManager;
	private static int decisionId = 0;
	private static SparseArray<MTMDecision> openDecisions = new SparseArray<MTMDecision>();

	Handler masterHandler;
	private File keyStoreFile;
	private KeyStore appKeyStore;
	private X509TrustManager defaultTrustManager;
	private X509TrustManager appTrustManager;

	/** Creates an instance of the MemorizingTrustManager class that falls back to a custom TrustManager.
	 *
	 * You need to supply the application context. This has to be one of:
	 *    - Application
	 *    - Activity
	 *    - Service
	 *
	 * The context is used for file management, to display the dialog /
	 * notification and for obtaining translated strings.
	 *
	 * @param m Context for the application.
	 * @param defaultTrustManager Delegate trust management to this TM. If null, the user must accept every certificate.
	 */
	public MemorizingTrustManager(Context m, X509TrustManager defaultTrustManager) {
		init(m);
		this.appTrustManager = getTrustManager(appKeyStore);
		this.defaultTrustManager = defaultTrustManager;
	}

	/** Creates an instance of the MemorizingTrustManager class using the system X509TrustManager.
	 *
	 * You need to supply the application context. This has to be one of:
	 *    - Application
	 *    - Activity
	 *    - Service
	 *
	 * The context is used for file management, to display the dialog /
	 * notification and for obtaining translated strings.
	 *
	 * @param m Context for the application.
	 */
	public MemorizingTrustManager(Context m) {
		init(m);
		this.appTrustManager = getTrustManager(appKeyStore);
		this.defaultTrustManager = getTrustManager(null);
	}

	void init(Context m) {
		master = m;
		masterHandler = new Handler(m.getMainLooper());
		notificationManager = (NotificationManager)master.getSystemService(Context.NOTIFICATION_SERVICE);

		Application app;
		if (m instanceof Application) {
			app = (Application)m;
		} else if (m instanceof Service) {
			app = ((Service)m).getApplication();
		} else if (m instanceof Activity) {
			app = ((Activity)m).getApplication();
		} else throw new ClassCastException("MemorizingTrustManager context must be either Activity or Service!");

		File dir = app.getDir(KEYSTORE_DIR, Context.MODE_PRIVATE);
		keyStoreFile = new File(dir + File.separator + KEYSTORE_FILE);

		appKeyStore = loadAppKeyStore();
	}

	
	/**
	 * Returns a X509TrustManager list containing a new instance of
	 * TrustManagerFactory.
	 *
	 * This function is meant for convenience only. You can use it
	 * as follows to integrate TrustManagerFactory for HTTPS sockets:
	 *
	 * <pre>
	 *     SSLContext sc = SSLContext.getInstance("TLS");
	 *     sc.init(null, MemorizingTrustManager.getInstanceList(this),
	 *         new java.security.SecureRandom());
	 *     HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	 * </pre>
	 * @param c Activity or Service to show the Dialog / Notification
	 */
	public static X509TrustManager[] getInstanceList(Context c) {
		return new X509TrustManager[] { new MemorizingTrustManager(c) };
	}

	/**
	 * Binds an Activity to the MTM for displaying the query dialog.
	 *
	 * This is useful if your connection is run from a service that is
	 * triggered by user interaction -- in such cases the activity is
	 * visible and the user tends to ignore the service notification.
	 *
	 * You should never have a hidden activity bound to MTM! Use this
	 * function in onResume() and @see unbindDisplayActivity in onPause().
	 *
	 * @param act Activity to be bound
	 */
	public void bindDisplayActivity(Activity act) {
		foregroundAct = act;
	}

	/**
	 * Removes an Activity from the MTM display stack.
	 *
	 * Always call this function when the Activity added with
	 * @see bindDisplayActivity is hidden.
	 *
	 * @param act Activity to be unbound
	 */
	public void unbindDisplayActivity(Activity act) {
		// do not remove if it was overridden by a different activity
		if (foregroundAct == act)
			foregroundAct = null;
	}

	/**
	 * Changes the path for the KeyStore file.
	 *
	 * The actual filename relative to the app's directory will be
	 * <code>app_<i>dirname</i>/<i>filename</i></code>.
	 *
	 * @param dirname directory to store the KeyStore.
	 * @param filename file name for the KeyStore.
	 */
	public static void setKeyStoreFile(String dirname, String filename) {
		KEYSTORE_DIR = dirname;
		KEYSTORE_FILE = filename;
	}

	/**
	 * Get a list of all certificate aliases stored in MTM.
	 *
	 * @return an {@link Enumeration} of all certificates
	 */
	public Enumeration<String> getCertificates() {
		try {
			return appKeyStore.aliases();
		} catch (KeyStoreException e) {
			// this should never happen, however...
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a certificate for a given alias.
	 *
	 * @param alias the certificate's alias as returned by {@link getCertificates}.
	 *
	 * @return the certificate associated with the alias or <tt>null</tt> if none found.
	 */
	public Certificate getCertificate(String alias) {
		try {
			return appKeyStore.getCertificate(alias);
		} catch (KeyStoreException e) {
			// this should never happen, however...
			throw new RuntimeException(e);
		}
	}

	/**
	 * Removes the given certificate from MTMs key store.
	 *
	 * <p>
	 * <b>WARNING</b>: this does not immediately invalidate the certificate. It is
	 * well possible that (a) data is transmitted over still existing connections or
	 * (b) new connections are created using TLS renegotiation, without a new cert
	 * check.
	 * </p>
	 * @param alias the certificate's alias as returned by {@link getCertificates}.
	 *
	 * @throws KeyStoreException if the certificate could not be deleted.
	 */
	public void deleteCertificate(String alias) throws KeyStoreException {
		appKeyStore.deleteEntry(alias);
		keyStoreUpdated();
	}

	/**
	 * Creates a new hostname verifier supporting user interaction.
	 *
	 * <p>This method creates a new {@link HostnameVerifier} that is bound to
	 * the given instance of {@link MemorizingTrustManager}, and leverages an
	 * existing {@link HostnameVerifier}. The returned verifier performs the
	 * following steps, returning as soon as one of them succeeds:
	 *  </p>
	 *  <ol>
	 *  <li>Success, if the wrapped defaultVerifier accepts the certificate.</li>
	 *  <li>Success, if the server certificate is stored in the keystore under the given hostname.</li>
	 *  <li>Ask the user and return accordingly.</li>
	 *  <li>Failure on exception.</li>
	 *  </ol>
	 *
	 * @param defaultVerifier the {@link HostnameVerifier} that should perform the actual check
	 * @return a new hostname verifier using the MTM's key store
	 *
	 * @throws IllegalArgumentException if the defaultVerifier parameter is null
	 */
	public HostnameVerifier wrapHostnameVerifier(final HostnameVerifier defaultVerifier) {
		if (defaultVerifier == null)
			throw new IllegalArgumentException("The default verifier may not be null");
		
		return new MemorizingHostnameVerifier(defaultVerifier);
	}
	
	X509TrustManager getTrustManager(KeyStore ks) {
		try {
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
			tmf.init(ks);
			for (TrustManager t : tmf.getTrustManagers()) {
				if (t instanceof X509TrustManager) {
					return (X509TrustManager)t;
				}
			}
		} catch (Exception e) {
			// Here, we are covering up errors. It might be more useful
			// however to throw them out of the constructor so the
			// embedding app knows something went wrong.
			Log.e(TAG, "getTrustManager(" + ks + ")", e);
		}
		return null;
	}

	KeyStore loadAppKeyStore() {
		KeyStore ks;
		try {
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
		} catch (KeyStoreException e) {
			Log.e(TAG, "getAppKeyStore()", e);
			return null;
		}
		try {
			ks.load(null, null);
			ks.load(new java.io.FileInputStream(keyStoreFile), "MTM".toCharArray());
		} catch (java.io.FileNotFoundException e) {
			Log.i(TAG, "getAppKeyStore(" + keyStoreFile + ") - file does not exist");
		} catch (Exception e) {
			Log.e(TAG, "getAppKeyStore(" + keyStoreFile + ")", e);
		}
		return ks;
	}

	void storeCert(String alias, Certificate cert) {
		try {
			appKeyStore.setCertificateEntry(alias, cert);
		} catch (KeyStoreException e) {
			Log.e(TAG, "storeCert(" + cert + ")", e);
			return;
		}		
		keyStoreUpdated();
	}
	
	void storeCert(X509Certificate cert) {
		storeCert(cert.getSubjectDN().toString(), cert);
	}

	void keyStoreUpdated() {
		// reload appTrustManager
		appTrustManager = getTrustManager(appKeyStore);

		// store KeyStore to file
		try {
			java.io.FileOutputStream fos = new java.io.FileOutputStream(keyStoreFile);
			appKeyStore.store(fos, "MTM".toCharArray());
			fos.close();
		} catch (Exception e) {
			Log.e(TAG, "storeCert(" + keyStoreFile + ")", e);
		}
	}

	// if the certificate is stored in the app key store, it is considered "known"
	private boolean isCertKnown(X509Certificate cert) {
		try {
			return appKeyStore.getCertificateAlias(cert) != null;
		} catch (KeyStoreException e) {
			return false;
		}
	}

	private boolean isExpiredException(Throwable e) {
		do {
			if (e instanceof CertificateExpiredException)
				return true;
			e = e.getCause();
		} while (e != null);
		return false;
	}

	public void checkCertTrusted(X509Certificate[] chain, String authType, boolean isServer)
		throws CertificateException
	{
		Log.d(TAG, "checkCertTrusted(" + chain + ", " + authType + ", " + isServer + ")");
		try {
			Log.d(TAG, "checkCertTrusted: trying appTrustManager");
			if (isServer)
				appTrustManager.checkServerTrusted(chain, authType);
			else
				appTrustManager.checkClientTrusted(chain, authType);
		} catch (CertificateException ae) {
			// if the cert is stored in our appTrustManager, we ignore expiredness
			ae.printStackTrace();
			if (isExpiredException(ae)) {
				Log.i(TAG, "checkCertTrusted: accepting expired certificate from keystore");
				return;
			}
			if (isCertKnown(chain[0])) {
				Log.i(TAG, "checkCertTrusted: accepting cert already stored in keystore");
				return;
			}
			try {
				if (defaultTrustManager == null)
					throw ae;
				Log.d(TAG, "checkCertTrusted: trying defaultTrustManager");
				if (isServer)
					defaultTrustManager.checkServerTrusted(chain, authType);
				else
					defaultTrustManager.checkClientTrusted(chain, authType);
			} catch (CertificateException e) {
				e.printStackTrace();
				interactCert(chain, authType, e);
			}
		}
	}

	public void checkClientTrusted(X509Certificate[] chain, String authType)
		throws CertificateException
	{
		checkCertTrusted(chain, authType, false);
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType)
		throws CertificateException
	{
		checkCertTrusted(chain, authType, true);
	}

	public X509Certificate[] getAcceptedIssuers()
	{
		Log.d(TAG, "getAcceptedIssuers()");
		return defaultTrustManager.getAcceptedIssuers();
	}

	private int createDecisionId(MTMDecision d) {
		int myId;
		synchronized(openDecisions) {
			myId = decisionId;
			openDecisions.put(myId, d);
			decisionId += 1;
		}
		return myId;
	}

	private static String hexString(byte[] data) {
		StringBuffer si = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			si.append(String.format("%02x", data[i]));
			if (i < data.length - 1)
				si.append(":");
		}
		return si.toString();
	}

	private static String certHash(final X509Certificate cert, String digest) {
		try {
			MessageDigest md = MessageDigest.getInstance(digest);
			md.update(cert.getEncoded());
			return hexString(md.digest());
		} catch (java.security.cert.CertificateEncodingException e) {
			return e.getMessage();
		} catch (java.security.NoSuchAlgorithmException e) {
			return e.getMessage();
		}
	}

	private void certDetails(StringBuffer si, X509Certificate c) {
		SimpleDateFormat validityDateFormater = new SimpleDateFormat("yyyy-MM-dd");
		si.append("\n");
		si.append(c.getSubjectDN().toString());
		si.append("\n");
		si.append(validityDateFormater.format(c.getNotBefore()));
		si.append(" - ");
		si.append(validityDateFormater.format(c.getNotAfter()));
		si.append("\nSHA-256: ");
		si.append(certHash(c, "SHA-256"));
		si.append("\nSHA-1: ");
		si.append(certHash(c, "SHA-1"));
		si.append("\nSigned by: ");
		si.append(c.getIssuerDN().toString());
		si.append("\n");
	}
	
	private String certChainMessage(final X509Certificate[] chain, CertificateException cause) {
		Throwable e = cause;
		Log.d(TAG, "certChainMessage for " + e);
		StringBuffer si = new StringBuffer();
		if (e.getCause() != null) {
			e = e.getCause();
			// HACK: there is no sane way to check if the error is a "trust anchor
			// not found", so we use string comparison.
			if (NO_TRUST_ANCHOR.equals(e.getMessage())) {
				si.append(master.getString(R.string.mtm_trust_anchor));
			} else
				si.append(e.getLocalizedMessage());
			si.append("\n");
		}
		si.append("\n");
		si.append(master.getString(R.string.mtm_connect_anyway));
		si.append("\n\n");
		si.append(master.getString(R.string.mtm_cert_details));
		for (X509Certificate c : chain) {
			certDetails(si, c);
		}
		return si.toString();
	}

	private String hostNameMessage(X509Certificate cert, String hostname) {
		StringBuffer si = new StringBuffer();

		si.append(master.getString(R.string.mtm_hostname_mismatch, hostname));
		si.append("\n\n");
		try {
			Collection<List<?>> sans = cert.getSubjectAlternativeNames();
			if (sans == null) {
				si.append(cert.getSubjectDN());
				si.append("\n");
			} else for (List<?> altName : sans) {
				Object name = altName.get(1);
				if (name instanceof String) {
					si.append("[");
					si.append((Integer)altName.get(0));
					si.append("] ");
					si.append(name);
					si.append("\n");
				}
			}
		} catch (CertificateParsingException e) {
			e.printStackTrace();
			si.append("<Parsing error: ");
			si.append(e.getLocalizedMessage());
			si.append(">\n");
		}
		si.append("\n");
		si.append(master.getString(R.string.mtm_connect_anyway));
		si.append("\n\n");
		si.append(master.getString(R.string.mtm_cert_details));
		certDetails(si, cert);
		return si.toString();
	}

	void startActivityNotification(Intent intent, int decisionId, String certName) {
		Notification n = new Notification(android.R.drawable.ic_lock_lock,
				master.getString(R.string.mtm_notification),
				System.currentTimeMillis());
		PendingIntent call = PendingIntent.getActivity(master, 0, intent, 0);
		n.setLatestEventInfo(master.getApplicationContext(),
				master.getString(R.string.mtm_notification),
				certName, call);
		n.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(NOTIFICATION_ID + decisionId, n);
	}

	/**
	 * Returns the top-most entry of the activity stack.
	 *
	 * @return the Context of the currently bound UI or the master context if none is bound
	 */
	Context getUI() {
		return (foregroundAct != null) ? foregroundAct : master;
	}

	int interact(final String message, final int titleId) {
		/* prepare the MTMDecision blocker object */
		MTMDecision choice = new MTMDecision();
		final int myId = createDecisionId(choice);

		masterHandler.post(new Runnable() {
			public void run() {
				Intent ni = new Intent(master, MemorizingActivity.class);
				ni.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				ni.setData(Uri.parse(MemorizingTrustManager.class.getName() + "/" + myId));
				ni.putExtra(DECISION_INTENT_ID, myId);
				ni.putExtra(DECISION_INTENT_CERT, message);
				ni.putExtra(DECISION_TITLE_ID, titleId);

				// we try to directly start the activity and fall back to
				// making a notification
				try {
					getUI().startActivity(ni);
				} catch (Exception e) {
					Log.e(TAG, "startActivity: " + e);
					startActivityNotification(ni, myId, message);
				}
			}
		});

		Log.d(TAG, "openDecisions: " + openDecisions);
		Log.d(TAG, "waiting on " + myId);
		try {
			synchronized(choice) { choice.wait(); }
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "finished wait on " + myId + ": " + choice.state);
		return choice.state;
	}
	
	void interactCert(final X509Certificate[] chain, String authType, CertificateException cause)
			throws CertificateException
	{
		switch (interact(certChainMessage(chain, cause), R.string.mtm_accept_cert)) {
		case MTMDecision.DECISION_ALWAYS:
			storeCert(chain[0]); // only store the server cert, not the whole chain
		case MTMDecision.DECISION_ONCE:
			break;
		default:
			throw (cause);
		}
	}

	boolean interactHostname(X509Certificate cert, String hostname)
	{
		switch (interact(hostNameMessage(cert, hostname), R.string.mtm_accept_servername)) {
		case MTMDecision.DECISION_ALWAYS:
			storeCert(hostname, cert);
		case MTMDecision.DECISION_ONCE:
			return true;
		default:
			return false;
		}
	}

	protected static void interactResult(int decisionId, int choice) {
		MTMDecision d;
		synchronized(openDecisions) {
			 d = openDecisions.get(decisionId);
			 openDecisions.remove(decisionId);
		}
		if (d == null) {
			Log.e(TAG, "interactResult: aborting due to stale decision reference!");
			return;
		}
		synchronized(d) {
			d.state = choice;
			d.notify();
		}
	}
	
	class MemorizingHostnameVerifier implements HostnameVerifier {
		private HostnameVerifier defaultVerifier;
		
		public MemorizingHostnameVerifier(HostnameVerifier wrapped) {
			defaultVerifier = wrapped;
		}

		@Override
		public boolean verify(String hostname, SSLSession session) {
			Log.d(TAG, "hostname verifier for " + hostname + ", trying default verifier first");
			// if the default verifier accepts the hostname, we are done
			if (defaultVerifier.verify(hostname, session)) {
				Log.d(TAG, "default verifier accepted " + hostname);
				return true;
			}
			// otherwise, we check if the hostname is an alias for this cert in our keystore
			try {
				X509Certificate cert = (X509Certificate)session.getPeerCertificates()[0];
				//Log.d(TAG, "cert: " + cert);
				if (cert.equals(appKeyStore.getCertificate(hostname.toLowerCase(Locale.US)))) {
					Log.d(TAG, "certificate for " + hostname + " is in our keystore. accepting.");
					return true;
				} else {
					Log.d(TAG, "server " + hostname + " provided wrong certificate, asking user.");
					return interactHostname(cert, hostname);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
}
