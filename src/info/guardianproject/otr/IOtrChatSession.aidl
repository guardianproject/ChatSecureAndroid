/*
 * Copyright (C) 2011 The Guardian Project
 */

package info.guardianproject.otr;

interface IOtrChatSession {
    /**
     * Start the OTR encryption on this chat session.
     */
    void startChatEncryption();

    /**
     * Stop the OTR encryption on this chat session.
     */
    void stopChatEncryption();

    /**
     * Tells if the chat session has OTR encryption running.
     */
    boolean isChatEncrypted();
    
    /** OTR session status - ordinal of SessionStatus */
    int getChatStatus();
    
    /**
     * start the SMP verification process
     */
    void initSmpVerification(String question, String answer);
    
    /**
     * respond to the SMP verification process
     */
    void respondSmpVerification(String answer);
 
 
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
