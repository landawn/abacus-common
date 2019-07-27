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

/**
 * This class provides a solution for safety data transfer between server/client.
 * First, let other DTOs extends this class and call encrypt method before sending message to server.
 * Don't set userName/password/nonce/created properties manually. they will be set after encrypt method is called.
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public abstract class SecurityDTO implements Serializable {
    private static final long serialVersionUID = -386078752923025834L;

    private static final JSONSerializationConfig jsc = JSC.of(false, false);

    private String userName;
    private String password;
    private String nonce;
    private String created;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    /**
     * The encrypted password will set after encrypt method is called
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getCreated() {
        return created;
    }

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

    public boolean decrypt(final String userName, final String password) {
        return decrypt(userName, password.getBytes(Charsets.UTF_8), MessageEncryption.NONE);
    }

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

    @Override
    public String toString() {
        return "{userName=" + userName + ", password=" + password + ", nonce=" + nonce + ", created=" + created + "}";
    }
}
