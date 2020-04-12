/*
 * Copyright 2016 - 2020 Gregor Dschung
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

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

interface CertificateService {
    fun isCertificateValidInFuture(cert: X509Certificate, duration: Duration): Boolean
    fun addCertificateToKeystore(ks: KeyStore, cert: X509Certificate, alias: String)
    fun loadCertificate(file: Path): X509Certificate
    fun newKeystore(): KeyStore
    fun storeKeystore(ks: KeyStore, file: Path, password: String)
}

class DefaultCertificateService : CertificateService {

    var clock: Clock = Clock.systemDefaultZone()

    val cf: CertificateFactory = CertificateFactory.getInstance("X.509")

    override fun isCertificateValidInFuture(cert: X509Certificate, duration: Duration): Boolean {
        val notAfterInstant = cert.notAfter.toInstant()
        val notAfter = LocalDateTime.ofInstant(notAfterInstant, ZoneOffset.UTC)

        val thresholdDateTime = LocalDateTime.now(clock).plus(duration)

        return thresholdDateTime.isBefore(notAfter)
    }

    override fun addCertificateToKeystore(ks: KeyStore, cert: X509Certificate, alias: String) {
        val certEntry = KeyStore.TrustedCertificateEntry(cert)
        ks.setEntry(alias, certEntry, null)
    }

    fun getCertificateFromKeystore(ks: KeyStore, alias: String): X509Certificate {
        if (!ks.containsAlias(alias)) {
            throw IllegalArgumentException("The keystore does not contain a certificate for alias $alias")
        }

        if (!ks.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry::class.java)) {
            throw UnsupportedOperationException("Certificate extraction is currently only implemented for TrustedCertificateEntry")
        }

        val entry = ks.getEntry(alias, null) as KeyStore.TrustedCertificateEntry
        return entry.trustedCertificate as X509Certificate
    }

    override fun loadCertificate(file: Path): X509Certificate {
        val inputStream = Files.newInputStream(file)
        return cf.generateCertificate(inputStream) as X509Certificate
    }

    fun fingerprintSha1(cert: X509Certificate): String {
        val messageDigest = MessageDigest.getInstance("SHA1")
        messageDigest.update(cert.encoded)
        val sha1 = messageDigest.digest()
        return sha1.map { String.format("%02X", (it.toInt() and 0xFF)) }.joinToString(separator = ":")
    }

    override fun newKeystore(): KeyStore {
        val keystore = KeyStore.getInstance("JKS")
        keystore.load(null)
        return keystore
    }

    fun loadKeystore(file: Path, password: String): KeyStore {
        val keystore = KeyStore.getInstance("JKS")
        Files.newInputStream(file).use { inputStream ->
            keystore.load(inputStream, password.toCharArray())
        }
        return keystore
    }

    override fun storeKeystore(ks: KeyStore, file: Path, password: String) {
        Files.newOutputStream(file).use { outputStream ->
            ks.store(outputStream, password.toCharArray())
        }
    }
}
