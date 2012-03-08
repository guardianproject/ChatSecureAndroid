/*
 * Copyright (C) 2011 The Guardian Project
 */

package info.guardianproject.otr;

interface IOtrKeyManager {
 
    /**
     * Verify the key for a given address.
     */
    void verifyKey(String address);

    /**
     * Revoke the verification for the key for a given address.
     */
    void unverifyKey(String address);

    /**
     * Tells if the fingerprint of the remote user address has been verified.
     */
    boolean isKeyVerified(String address);

    /**
     * Returns the fingerprint for the local user's key for a given account address.
     */
    String getLocalFingerprint();

    /**
     * Returns the fingerprint for a remote user's key for a given account address.
     */
    String getRemoteFingerprint();

    /**
     * generate a new local private/public key pair.
     */
    void generateLocalKeyPair();
        

}
