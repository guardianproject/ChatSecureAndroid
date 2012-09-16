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
    
}
