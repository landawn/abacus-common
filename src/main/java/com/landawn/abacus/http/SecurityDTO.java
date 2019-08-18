/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.http;

import java.io.Serializable;

import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.WSSecurityUtil;

// TODO: Auto-generated Javadoc
/**
 * This class provides a solution for safety data transfer between server/client.
 * First, let other DTOs extends this class and call encrypt method before sending message to server.
 * Don't set userName/password/nonce/created properties manually. they will be set after encrypt method is called.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class SecurityDTO implements Serializable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -386078752923025834L;

    /** The Constant jsc. */
    private static final JSONSerializationConfig jsc = JSC.of(false, false);

    /** The user name. */
    private String userName;

    /** The password. */
    private String password;

    /** The nonce. */
    private String nonce;

    /** The created. */
    private String created;

    /**
     * Gets the user name.
     *
     * @return
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user name.
     *
     * @param userName the new user name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets the password.
     *
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * The encrypted password will set after encrypt method is called.
     *
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the nonce.
     *
     * @return
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Sets the nonce.
     *
     * @param nonce the new nonce
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * Gets the created.
     *
     * @return
     */
    public String getCreated() {
        return created;
    }

    /**
     * Sets the created.
     *
     * @param created the new created
     */
    public void setCreated(String created) {
        this.created = created;
    }

    /**
     * Encrypt this request with specified userName and password.
     *
     * @param userName
     * @param password
     */
    public void encrypt(final String userName, final String password) {
        encrypt(userName, password.getBytes(Charsets.UTF_8), MessageEncryption.NONE);
    }

    /**
     * Encrypt this request with specified userName and password.
     *
     * @param userName
     * @param password
     * @param msgEncryption
     */
    public void encrypt(final String userName, final byte[] password, final MessageEncryption msgEncryption) {
        setUserName(null);
        setPassword(null);
        setNonce(null);
        setCreated(null);

        byte[] nonce = null;

        switch (msgEncryption) {
            case NONE: {
                nonce = WSSecurityUtil.generateNonce(32);

                break;
            }

            case JSON: {
                final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

                try {
                    HTTP.jsonParser.serialize(os, this, jsc);
                    nonce = os.toByteArray();
                } finally {
                    Objectory.recycle(os);
                }

                break;
            }

            case KRYO: {
                final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

                try {
                    HTTP.kryoParser.serialize(os, this);
                    nonce = os.toByteArray();
                } finally {
                    Objectory.recycle(os);
                }

                break;
            }

            default:
                throw new IllegalArgumentException("Unsupported Message encryption way: " + msgEncryption);
        }

        final String created = String.valueOf(System.currentTimeMillis());

        setUserName(userName);
        setPassword(WSSecurityUtil.doPasswordDigest(nonce, created.getBytes(Charsets.UTF_8), password));
        setNonce(N.base64Encode(nonce));
        setCreated(created);
    }

    /**
     * Decrypt.
     *
     * @param userName
     * @param password
     * @return true, if successful
     */
    public boolean decrypt(final String userName, final String password) {
        return decrypt(userName, password.getBytes(Charsets.UTF_8), MessageEncryption.NONE);
    }

    /**
     * Decrypt.
     *
     * @param userName
     * @param password
     * @param msgEncryption
     * @return true, if successful
     */
    public boolean decrypt(final String userName, final byte[] password, final MessageEncryption msgEncryption) {
        long now = System.currentTimeMillis();
        long ceratedTime = Long.valueOf(getCreated());

        if (((now - ceratedTime) > (3 * 60 * 1000)) || !getUserName().equals(userName)) {
            return false;
        }

        String orgUserName = getUserName();
        String orgPassword = getPassword();
        String orgNonce = getNonce();
        String orgCreated = getCreated();

        setUserName(null);
        setPassword(null);
        setNonce(null);
        setCreated(null);

        try {
            byte[] nonce = null;

            switch (msgEncryption) {
                case NONE: {
                    nonce = N.base64Decode(orgNonce);

                    break;
                }

                case JSON: {
                    final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

                    try {
                        HTTP.jsonParser.serialize(os, this, jsc);
                        nonce = os.toByteArray();
                    } finally {
                        Objectory.recycle(os);
                    }

                    break;
                }

                case KRYO: {
                    final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

                    try {
                        HTTP.kryoParser.serialize(os, this);
                        nonce = os.toByteArray();
                    } finally {
                        Objectory.recycle(os);
                    }

                    break;
                }

                default:
                    throw new IllegalArgumentException("Unsupported Message encryption way: " + msgEncryption);
            }

            return orgPassword.equals(WSSecurityUtil.doPasswordDigest(nonce, orgCreated.getBytes(Charsets.UTF_8), password));
        } finally {
            setUserName(orgUserName);
            setPassword(orgPassword);
            setNonce(orgNonce);
            setCreated(orgCreated);
        }
    }

    /**
     * Hash code.
     *
     * @return
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((userName == null) ? 0 : userName.hashCode());
        result = (prime * result) + ((password == null) ? 0 : password.hashCode());
        result = (prime * result) + ((nonce == null) ? 0 : nonce.hashCode());
        result = (prime * result) + ((created == null) ? 0 : created.hashCode());

        return result;
    }

    /**
     * Equals.
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof SecurityDTO) {
            SecurityDTO other = (SecurityDTO) obj;

            return N.equals(userName, other.userName) && N.equals(password, other.password) && N.equals(nonce, other.nonce) && N.equals(created, other.created);
        }

        return false;
    }

    /**
     * To string.
     *
     * @return
     */
    @Override
    public String toString() {
        return "{userName=" + userName + ", password=" + password + ", nonce=" + nonce + ", created=" + created + "}";
    }
}
