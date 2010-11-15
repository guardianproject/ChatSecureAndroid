/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gitian.android.im.plugin;

import org.gitian.android.im.engine.ImException;

/**
 * The password digest method used in IMPS login transaction.
 */
public interface PasswordDigest {
    /**
     * Gets an array of supported digest schema.
     *
     * @return an array of digest schema
     */
    String[] getSupportedDigestSchema();

    /**
     * Generates the digest bytes of the password.
     *
     * @param schema The digest schema to use.
     * @param nonce The nonce string returned by the server.
     * @param password The user password.
     * @return The digest bytes of the password.
     * @throws ImException
     */
    String digest(String schema, String nonce, String password) throws ImException;
}
