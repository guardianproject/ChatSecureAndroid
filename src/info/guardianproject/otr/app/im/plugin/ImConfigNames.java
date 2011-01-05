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
package info.guardianproject.otr.app.im.plugin;

/**
 * Defines the configuration names for the IM engine.
 *
 */
public interface ImConfigNames {
    /**
     * The name of the protocol.
     */
    public static final String PROTOCOL_NAME = "im.protocol";

    /**
     * The default domain.
     */
    public static final String DEFAULT_DOMAIN = "im.default-domain";

    /**
     * The path of the plugin.
     */
    public static final String PLUGIN_PATH = "im.plugin.path";

    /**
     * The class name of the plugin implementation.
     */
    public static final String PLUGIN_CLASS = "im.plugin.class";

    /**
     * The version of the plugin.
     */
    public static final String PLUGIN_VERSION = "im.plugin.version";
}
