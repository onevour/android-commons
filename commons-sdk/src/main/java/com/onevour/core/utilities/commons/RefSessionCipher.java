package com.onevour.core.utilities.commons;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypts the RefSession secrets (the *Secure methods) with an AES-GCM key kept in the Android Keystore: the key
 * never leaves the device's secure hardware, so a copied preferences file (backup, rooted phone)
 * cannot be read elsewhere.
 */
final class RefSessionCipher {

    private static final String KEYSTORE = "AndroidKeyStore";

    static final String KEY_ALIAS = "onevour_ref_session";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int IV_BYTES = 12;

    private static final int TAG_BITS = 128;

    /** Format of what is stored: a version, so the scheme can change later. */
    private static final String PREFIX = "v1:";

    private RefSessionCipher() {
    }

    static String encrypt(String plain) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] sealed = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[iv.length + sealed.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(sealed, 0, out, iv.length, sealed.length);
        return PREFIX + Base64.encodeToString(out, Base64.NO_WRAP);
    }

    static String decrypt(String stored) throws GeneralSecurityException {
        if (!stored.startsWith(PREFIX)) throw new GeneralSecurityException("unknown format");
        byte[] in = Base64.decode(stored.substring(PREFIX.length()), Base64.NO_WRAP);
        if (in.length <= IV_BYTES) throw new GeneralSecurityException("too short");
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(in, 0, IV_BYTES)));
        byte[] plain = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /** Forgets the key: every secure value becomes unreadable (tests, or a full reset). */
    static void deleteKey() throws GeneralSecurityException {
        KeyStore keyStore = keyStore();
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS);
    }

    private static synchronized SecretKey key() throws GeneralSecurityException {
        KeyStore keyStore = keyStore();
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private static KeyStore keyStore() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            return keyStore;
        } catch (java.io.IOException e) {
            throw new GeneralSecurityException(e);
        }
    }
}
