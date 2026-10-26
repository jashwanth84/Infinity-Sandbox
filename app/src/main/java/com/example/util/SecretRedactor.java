package com.example.util;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to scrub and redact sensitive API keys, auth headers, and tokens from UI text,
 * logs, errors, and chat history.
 */
public final class SecretRedactor {

    private static final String REDACTED_MARKER = "[REDACTED_API_KEY]";
    private static final Pattern NVAPI_PATTERN = Pattern.compile("nvapi-[A-Za-z0-9_\\-]{20,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER_PATTERN = Pattern.compile("Bearer\\s+[A-Za-z0-9_\\-\\.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERIC_API_KEY_PATTERN = Pattern.compile("(key|secret|token|password)=\"[^\"]+\"", Pattern.CASE_INSENSITIVE);

    private static final Set<String> DYNAMIC_SECRETS = new CopyOnWriteArraySet<>();

    private SecretRedactor() {
        // Utility class
    }

    /**
     * Registers a secret token or key so it will be masked anywhere in the app.
     *
     * @param secret The secret string to redact.
     */
    public static void registerSecret(String secret) {
        if (secret != null && secret.trim().length() > 5) {
            DYNAMIC_SECRETS.add(secret.trim());
        }
    }

    /**
     * Redacts known keys, nvapi keys, and bearer headers from any given text.
     *
     * @param input Raw text (logs, errors, model outputs).
     * @return Sanitized text with secrets masked.
     */
    public static String redact(String input) {
        if (input == null) {
            return null;
        }

        String result = input;

        // 1. Redact specific registered secrets
        for (String secret : DYNAMIC_SECRETS) {
            if (result.contains(secret)) {
                result = result.replace(secret, REDACTED_MARKER);
            }
        }

        // 2. Redact nvapi-* keys
        Matcher nvapiMatcher = NVAPI_PATTERN.matcher(result);
        if (nvapiMatcher.find()) {
            result = nvapiMatcher.replaceAll(REDACTED_MARKER);
        }

        // 3. Redact Bearer tokens in headers/logs
        Matcher bearerMatcher = BEARER_PATTERN.matcher(result);
        if (bearerMatcher.find()) {
            result = bearerMatcher.replaceAll("Bearer " + REDACTED_MARKER);
        }

        return result;
    }
}
