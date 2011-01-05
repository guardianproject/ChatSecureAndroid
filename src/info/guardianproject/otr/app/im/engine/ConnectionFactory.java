/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

package info.guardianproject.otr.app.im.engine;

import info.guardianproject.otr.app.im.plugin.loopback.LoopbackConnection;
import info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection;

import java.util.Map;



/**
 * The factory used to create an instance of ImConnection.
 */
public class ConnectionFactory {
    private static ConnectionFactory sInstance;

    private ConnectionFactory() {
    }

    /**
     * Gets the singleton instance of the factory.
     *
     * @return the singleton instance.
     */
    public synchronized static ConnectionFactory getInstance() {
        if (sInstance == null) {
            sInstance = new ConnectionFactory();
        }
        return sInstance;
    }

    /**
     * Creates a new ImConnection.
     *
     * @return the new ImConnection.
     * @throws IMException if an error occurs during creating a connection.
     */
    public ImConnection createConnection(Map<String, String> settings) throws ImException {
    	if ("XMPP".equals(settings.get("im.protocol"))) {
        	return new XmppConnection();
    	} 
    	/*else if ("LOOPBACK".equals(settings.get("im.protocol"))) {
    		return new LoopbackConnection();
        } */
    	else {
            throw new ImException("Unsupported protocol");
        }
    }
}
