/*
 * Copyright 2016 - 2022 Gregor Dschung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.Path
import java.security.KeyStore

class KeystoreAssertions {

    private static DefaultCertificateService certificateService = new DefaultCertificateService()

    static void assertFingerprintOfKeystoreEntry(Path keystore, String password, String alias, String expectedFingerprint) {
        def ks = certificateService.loadKeystore(keystore, password)
        assertFingerprintOfKeystoreEntry(ks, alias, expectedFingerprint)
    }

    static void assertFingerprintOfKeystoreEntry(KeyStore ks, String alias, String expectedFingerprint) {
        def cert = certificateService.getCertificateFromKeystore(ks, alias)
        def fingerprint = certificateService.fingerprintSha1(cert)

        assert fingerprint == expectedFingerprint
    }

    static void assertNumberOfEntriesInKeystore(Path keystore, String password, int expectedNumber) {
        def ks = certificateService.loadKeystore(keystore, password)
        assert ks.size() == expectedNumber
    }
}
