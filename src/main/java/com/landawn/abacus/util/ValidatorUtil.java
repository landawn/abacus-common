/*
 * Copyright (C) 2016 HaiYang Li
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

package com.landawn.abacus.util;

import java.util.regex.Pattern;

/**
 * The Class ValidatorUtil.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class ValidatorUtil {

    /** The Constant emailPattern. */
    private static final Pattern emailPattern = Pattern
            .compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");

    // http://howtodoinjava.com/regex/how-to-build-regex-based-password-validator-in-java/
    //    (?=.*[a-z])      : This matches the presence of at least one lower case letter.
    //    (?=.*[A-Z])      : This matches the presence of at least one capital letter.
    //    (?=.*[0-9])      : This matches the presence of at least one digit i.e. 0-9.
    //    (?=.*[`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?])     : This matches the presence of at least one special character.
    /** The Constant passwordPattern. */
    //    {6,16}           : This limits the length of password from minimum 6 letters to maximum 16 letters.
    private static final Pattern passwordPattern = Pattern
            .compile("((?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[" + escapeSpeicalChars("`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?") + "]).{6,16})");

    /**
     * Checks if is valid email address.
     *
     * @param emailAddress
     * @return true, if is valid email address
     */
    public static boolean isValidEmailAddress(String emailAddress) {
        if (N.isNullOrEmpty(emailAddress) || emailPattern.matcher(emailAddress).matches() == false) {
            return false;
        }

        return true;
    }

    /**
     * Returns <code>true</code> if the specified password contains: 1) At least one lower case letter, And 2) at least one capital letter, And 3) at least one digit i.e. 0-9, and 4)at least one special character, and 5) the length of password from minimum 6 letters to maximum 16 letters.
     * Otherwise, <code>false</code> is returned.  
     *
     * @param password
     * @return true, if is valid password
     */
    public static boolean isValidPassword(String password) {
        if (N.isNullOrEmpty(password) || passwordPattern.matcher(password).matches() == false) {
            return false;
        }

        return true;
    }

    /**
     * Builds the password validator.
     *
     * @param forceCapitalLetter
     * @param forceNumber
     * @param specialChars
     * @param minLength
     * @param maxLength
     * @return
     */
    public static Validator<String> buildPasswordValidator(boolean forceCapitalLetter, boolean forceNumber, String specialChars, int minLength, int maxLength) {
        final StringBuilder sb = new StringBuilder("((?=.*[a-z])");

        if (forceCapitalLetter) {
            sb.append("(?=.*[A-Z])");
        }

        if (forceNumber) {
            sb.append("(?=.*[0-9])");
        }

        if (N.notNullOrEmpty(specialChars)) {
            sb.append(escapeSpeicalChars(specialChars));
        }

        sb.append(".{" + minLength + "," + maxLength + "})");

        return new Validator<String>() {
            private final Pattern pwdPattern = Pattern.compile(sb.toString());

            @Override
            public boolean isValid(String value) {
                return N.notNullOrEmpty(value) && pwdPattern.matcher(value).matches();
            }
        };
    }

    /**
     * Escape speical chars.
     *
     * @param regexString
     * @return
     */
    private static String escapeSpeicalChars(String regexString) {
        return regexString.replaceAll("([\\\\\\.\\[\\{\\(\\*\\+\\?\\^\\$\\|\\)\\]\\}])", "\\\\$1");
    }

    /**
     * The Interface Validator.
     *
     * @param <T>
     */
    public static interface Validator<T> {

        /**
         * Checks if is valid.
         *
         * @param value
         * @return true, if is valid
         */
        public boolean isValid(T value);
    }
}
