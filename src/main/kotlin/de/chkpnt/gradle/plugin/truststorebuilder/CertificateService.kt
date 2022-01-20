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
import javax.naming.ldap.LdapName

enum class KeyStoreType {
    JKS, PKCS12
}

interface CertificateService {
    fun isCertificateValidInFuture(cert: X509Certificate, duration: Duration): Boolean
    fun addCertificateToKeystore(ks: KeyStore, cert: X509Certificate, alias: String)
    fun loadCertificates(file: Path): List<X509Certificate>
    fun deriveAlias(cert: X509Certificate): String
    fun containsAlias(ks: KeyStore, alias: String): Boolean
    fun newKeyStore(type: KeyStoreType): KeyStore
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

    override fun loadCertificates(file: Path): List<X509Certificate> {
        val inputStream = Files.newInputStream(file)
        return cf.generateCertificates(inputStream).map { it as X509Certificate }
    }

    override fun deriveAlias(cert: X509Certificate): String {
        val dn = cert.subjectX500Principal.name
        val ldapName = LdapName(dn)
        val cn = ldapName.rdns.firstOrNull {
            it.type.equals("cn", true)
        }?.value?.toString() ?: dn
        val fingerprint = shortFingerprintSha1(cert)
        return "$cn [$fingerprint]"
    }

    private fun shortFingerprintSha1(cert: X509Certificate): String {
        val sha1 = sha1(cert)
        return sha1.map { String.format("%02X", (it.toInt() and 0xFF)) }
            .joinToString(separator = "")
            .take(7)
    }

    fun fingerprintSha1(cert: X509Certificate): String {
        val sha1 = sha1(cert)
        return sha1.map { String.format("%02X", (it.toInt() and 0xFF)) }
            .joinToString(separator = ":")
    }

    private fun sha1(cert: X509Certificate): ByteArray {
        val messageDigest = MessageDigest.getInstance("SHA1")
        messageDigest.update(cert.encoded)
        return messageDigest.digest()
    }

    override fun containsAlias(ks: KeyStore, alias: String): Boolean {
        return ks.containsAlias(alias)
    }

    override fun newKeyStore(type: KeyStoreType): KeyStore {
        val keystore = KeyStore.getInstance(type.name)
        keystore.load(null)
        return keystore
    }

    fun loadKeystore(file: Path, password: String): KeyStore {
        val type = file.keyStoreType() ?: throw IllegalArgumentException("Can't derive KeyStoreType for $file")
        val keystore = KeyStore.getInstance(type.name)
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
