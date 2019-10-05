/*
 * Copyright 2016 Gregor Dschung
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
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.stream.Collectors

import groovy.transform.PackageScope

@PackageScope
class CertificateService {

    Clock clock = Clock.systemDefaultZone()

    CertificateFactory cf = CertificateFactory.getInstance("X.509")

    boolean isCertificateValidInFuture(X509Certificate cert, Duration duration) {
        def notAfterInstant = cert.notAfter.toInstant()
        def notAfter = LocalDateTime.ofInstant(notAfterInstant, ZoneOffset.UTC)

        def thresholdDateTime = LocalDateTime.now(clock).plus(duration)

        thresholdDateTime.isBefore(notAfter)
    }

    void addCertificateToKeystore(KeyStore ks, X509Certificate cert, String alias) {
        def certEntry = new KeyStore.TrustedCertificateEntry(cert)
        ks.setEntry(alias, certEntry, null)
    }

    X509Certificate getCertificateFromKeystore(KeyStore ks, String alias) {
        if (! ks.containsAlias(alias)) {
            throw new IllegalArgumentException("The keystore does not contain a certificate for alias $alias")
        }

        if (! ks.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry)) {
            throw new UnsupportedOperationException("Certificate extraction is currently only implemented for TrustedCertificateEntry")
        }

        def entry = ks.getEntry(alias, null)
        return ((KeyStore.TrustedCertificateEntry)entry).getTrustedCertificate()
    }

    X509Certificate loadCertificate(Path file) {
        def inputStream = file.newInputStream()
        cf.generateCertificate(inputStream)
    }

    String fingerprintSha1(X509Certificate cert) {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1")
        messageDigest.update(cert.getEncoded())
        byte[] sha1 = messageDigest.digest()
        sha1.toList()
                .stream()
                .map { b -> ([b] as byte[]).encodeHex().toString().toUpperCase() }
                .collect(Collectors.joining(":"))
    }

    KeyStore newKeystore() {
        def keystore = KeyStore.getInstance("JKS")
        keystore.load(null)
        return keystore
    }

    KeyStore loadKeystore(Path file, String password) {
        def keystore = KeyStore.getInstance("JKS")
        file.withInputStream { is ->
            keystore.load(is, password.toCharArray())
        }
        return keystore
    }

    void storeKeystore(KeyStore ks, Path file, String password) {
        file.withOutputStream { os ->
            ks.store(os, password.toCharArray())
        }
    }
}
