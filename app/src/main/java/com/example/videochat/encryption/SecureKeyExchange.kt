package com.example.videochat.encryption

import android.util.Log
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecureKeyExchange {
    private const val ALGORITHM_DH = "X25519"
    private const val PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME
    private const val ALGORITHM_AES = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val NONCE_SIZE = 12
    private const val AAD_CONTEXT = "e2ee_group_key_wrap_v1"

    fun generateDhKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(ALGORITHM_DH, PROVIDER_NAME)
        return kpg.generateKeyPair()
    }

    fun deriveSharedSecret(
        localPrivate: java.security.PrivateKey,
        remotePublic: java.security.PublicKey
    ): ByteArray {
        val ka = KeyAgreement.getInstance(ALGORITHM_DH, PROVIDER_NAME)
        ka.init(localPrivate)
        ka.doPhase(remotePublic, true)
        val secret = ka.generateSecret()
        Log.d("SecureKeyExchange", "deriveSharedSecret: result length=${secret.size}, first8=${secret.take(8).joinToString("") { "%02x".format(it) }}")
        return secret
    }

    fun wrapGroupKey(sharedSecret: ByteArray, groupKey: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(ALGORITHM_AES)
        val nonce = SecureRandom().generateSeed(NONCE_SIZE)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(sharedSecret, "AES"), spec)
        cipher.updateAAD(AAD_CONTEXT.toByteArray())
        return Pair(cipher.doFinal(groupKey), nonce)
    }

    fun unwrapGroupKey(
        sharedSecret: ByteArray,
        wrappedKey: ByteArray,
        nonce: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM_AES)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(sharedSecret, "AES"), spec)
        cipher.updateAAD(AAD_CONTEXT.toByteArray())
        return cipher.doFinal(wrappedKey)
    }

    fun generateGroupKey(): ByteArray = SecureRandom().generateSeed(32)

    fun encodePublicKey(publicKey: java.security.PublicKey): String {
        return java.util.Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    fun decodePublicKey(encodedKey: String): java.security.PublicKey {
        val keyBytes = java.util.Base64.getDecoder().decode(encodedKey)
        val keySpec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance(ALGORITHM_DH, PROVIDER_NAME).generatePublic(keySpec)
    }

    fun generateDhKeyPairAndEncodePublic(): Pair<KeyPair, String> {
        val keyPair = generateDhKeyPair()
        val encoded = java.util.Base64.getEncoder().encodeToString(keyPair.public.encoded)
        return Pair(keyPair, encoded)
    }
}