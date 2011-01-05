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

/**
 * A generic exception that is thrown by the IM engine. If it's caused by an
 * error condition returned by the IM server, an IMError is associated with
 * it.
 */
public class ImException extends Exception {
    private ImErrorInfo mError;

    /**
     * Creates a new ImException with the specified detail message.
     *
     * @param message the detail message.
     */
    public ImException(String message) {
        super(message);
    }

    /**
     * Creates a new ImException with the IMError which was the cause of the
     * exception.
     *
     * @param error the cause of the exception.
     */
    public ImException(ImErrorInfo error) {
        super(error.getDescription());
        mError = error;
    }

    /**
     * Creates a new ImException with the specified cause.
     *
     * @param cause the cause.
     */
    public ImException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new ImException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public ImException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new ImException with specified IM error code and description
     *
     * @param imErrorCode
     * @param string
     */
    public ImException(int imErrorCode, String description) {
        this(new ImErrorInfo(imErrorCode, description));
    }

    /**
     * Gets the IMError which caused the exception or <code>null</code> if
     * there isn't one.
     *
     * @return the IMError which caused the exception.
     */
    public ImErrorInfo getImError() {
        return mError;
    }

    public void printStackTrace() {
        System.err.println("ImError: " + mError);
        super.printStackTrace();
    }
}
