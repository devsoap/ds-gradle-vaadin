/*
 * Copyright 2018-2019 Devsoap Inc.
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
package com.devsoap.vaadinflow.util

/**
 * Http request utilities
 *
 * @author John Ahlroos
 * @since 1.0
 */
class HttpUtils {

    private static final String MINUS_ONE_STRING = '-1'
    private static final String LOCALHOST = 'localhost'

    /**
     * Get the HTTP Proxy url
     */
    static String getHttpProxy() {
        int proxyPort = Integer.parseInt(System.getProperty('http.proxyPort', MINUS_ONE_STRING))
        if (proxyPort > 0) {
            String proxyScheme = System.getProperty('http.proxyScheme', 'http')
            String proxyHost = System.getProperty('http.proxyHost', LOCALHOST)
            String proxyUser = System.getProperty('http.proxyUser', null)
            if (proxyUser) {
                String proxyPass = System.getProperty('http.proxyPassword', null)
                return "$proxyScheme://$proxyUser:$proxyPass@$proxyHost:$proxyPort"
            }
            return "$proxyScheme://$proxyHost:$proxyPort"
        }
    }

    /**
     * Get the HTTPS Proxy url
     */
    static String getHttpsProxy() {
        int proxyPort = Integer.parseInt(System.getProperty('https.proxyPort', MINUS_ONE_STRING))
        if (proxyPort > 0) {
            String proxyScheme = System.getProperty('https.proxyScheme', 'https')
            String proxyHost = System.getProperty('https.proxyHost', LOCALHOST)
            String proxyUser = System.getProperty('https.proxyUser', null)
            if (proxyUser) {
                String proxyPass = System.getProperty('https.proxyPassword', null)
                return "$proxyScheme://$proxyUser:$proxyPass@$proxyHost:$proxyPort"
            }
            return "$proxyScheme://$proxyHost:$proxyPort"
        }
    }
}
