package com.wordmatch.assist.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Device-local settings for the optional multi-provider unknown-word matcher. */
public final class AiMatchSettings {
    public static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    public static final int DEFAULT_TIMEOUT_MS = 5000;
    public static final int MIN_TIMEOUT_MS = 1000;
    public static final int MAX_TIMEOUT_MS = 15000;

    private static final String PREFERENCES = "ai_match_preferences";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_MODEL = "model";
    private static final String KEY_TIMEOUT = "timeout_ms";
    private static final String KEY_API_CIPHER = "api_key_cipher";
    private static final String KEY_API_IV = "api_key_iv";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "wordmatch_ai_api_key";

    private AiMatchSettings() {
    }

    public static boolean isEnabled(Context context) {
        return preferences(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static Settings load(Context context) {
        SharedPreferences preferences = preferences(context);
        String endpoint = preferences.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT);
        String savedProvider = preferences.getString(KEY_PROVIDER, "");
        AiProvider provider = savedProvider == null || savedProvider.trim().isEmpty()
                ? AiProvider.detectFromEndpoint(endpoint)
                : AiProvider.fromId(savedProvider);
        String model = preferences.getString(KEY_MODEL, "");
        int timeoutMs = preferences.getInt(KEY_TIMEOUT, DEFAULT_TIMEOUT_MS);
        timeoutMs = Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, timeoutMs));
        String apiKey = decryptApiKey(preferences);
        return new Settings(
                preferences.getBoolean(KEY_ENABLED, false),
                provider,
                provider.normalizeEndpoint(endpoint == null ? DEFAULT_ENDPOINT : endpoint),
                model == null ? "" : model.trim(),
                apiKey,
                timeoutMs
        );
    }

    /** Empty apiKeyInput keeps the previously saved key. */
    public static boolean save(
            Context context,
            String providerId,
            String endpoint,
            String model,
            String apiKeyInput,
            int timeoutMs
    ) {
        AiProvider provider = AiProvider.fromId(providerId);
        String cleanEndpoint = provider.normalizeEndpoint(endpoint);
        String cleanModel = model == null ? "" : model.trim();
        String cleanApiKey = apiKeyInput == null ? "" : apiKeyInput.trim();
        Settings saved = load(context);
        if (cleanApiKey.isEmpty() && saved.hasApiKey()
                && !AiTransportPolicy.canReuseKey(saved.getEndpoint(), cleanEndpoint)) {
            return false;
        }
        boolean hasEffectiveKey = !cleanApiKey.isEmpty() || saved.hasApiKey();
        if (!AiTransportPolicy.isValidEndpoint(cleanEndpoint)
                || cleanModel.isEmpty()
                || timeoutMs < MIN_TIMEOUT_MS
                || timeoutMs > MAX_TIMEOUT_MS
                || (provider.isApiKeyRequired() && !hasEffectiveKey)) {
            return false;
        }
        SharedPreferences.Editor editor = preferences(context).edit()
                .putString(KEY_PROVIDER, provider.getId())
                .putString(KEY_ENDPOINT, cleanEndpoint)
                .putString(KEY_MODEL, cleanModel)
                .putInt(KEY_TIMEOUT, timeoutMs);
        if (!cleanApiKey.isEmpty()) {
            try {
                EncryptedValue encrypted = encrypt(cleanApiKey);
                editor.putString(KEY_API_CIPHER, encrypted.cipherText)
                        .putString(KEY_API_IV, encrypted.iv);
            } catch (GeneralSecurityException error) {
                return false;
            }
        }
        editor.apply();
        return true;
    }

    public static Settings createTransient(
            String providerId,
            String endpoint,
            String model,
            String apiKey,
            int timeoutMs
    ) {
        AiProvider provider = AiProvider.fromId(providerId);
        return new Settings(
                false,
                provider,
                provider.normalizeEndpoint(endpoint),
                model == null ? "" : model.trim(),
                apiKey == null ? "" : apiKey.trim(),
                timeoutMs
        );
    }

    public static void clearApiKey(Context context) {
        preferences(context).edit()
                .remove(KEY_API_CIPHER)
                .remove(KEY_API_IV)
                .apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private static EncryptedValue encrypt(String plainText) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return new EncryptedValue(
                Base64.encodeToString(encrypted, Base64.NO_WRAP),
                Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
        );
    }

    private static String decryptApiKey(SharedPreferences preferences) {
        String cipherText = preferences.getString(KEY_API_CIPHER, "");
        String iv = preferences.getString(KEY_API_IV, "");
        if (cipherText == null || cipherText.isEmpty() || iv == null || iv.isEmpty()) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateSecretKey(),
                    new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            );
            byte[] decrypted = cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException error) {
            return "";
        }
    }

    private static SecretKey getOrCreateSecretKey() throws GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        try {
            keyStore.load(null);
        } catch (java.io.IOException error) {
            throw new GeneralSecurityException(error);
        } catch (java.security.cert.CertificateException error) {
            throw new GeneralSecurityException(error);
        }
        java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) {
            return (SecretKey) existing;
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    public static final class Settings {
        private final boolean enabled;
        private final AiProvider provider;
        private final String endpoint;
        private final String model;
        private final String apiKey;
        private final int timeoutMs;

        private Settings(
                boolean enabled,
                AiProvider provider,
                String endpoint,
                String model,
                String apiKey,
                int timeoutMs
        ) {
            this.enabled = enabled;
            this.provider = provider == null ? AiProvider.COMPATIBLE : provider;
            this.endpoint = endpoint;
            this.model = model;
            this.apiKey = apiKey;
            this.timeoutMs = timeoutMs;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isConfigured() {
            return AiTransportPolicy.isValidEndpoint(endpoint)
                    && !model.isEmpty()
                    && timeoutMs >= MIN_TIMEOUT_MS
                    && timeoutMs <= MAX_TIMEOUT_MS
                    && (!provider.isApiKeyRequired() || hasApiKey());
        }

        public AiProvider getProvider() {
            return provider;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public String getModel() {
            return model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public boolean hasApiKey() {
            return !apiKey.isEmpty();
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }
    }

    private static final class EncryptedValue {
        private final String cipherText;
        private final String iv;

        private EncryptedValue(String cipherText, String iv) {
            this.cipherText = cipherText;
            this.iv = iv;
        }
    }
}
