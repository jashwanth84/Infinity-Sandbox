package com.example.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to sanitize, strip markdown code fences, and parse/repair LLM JSON responses.
 */
public final class JsonSanitizer {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)\\s*```",
            Pattern.CASE_INSENSITIVE
    );

    private JsonSanitizer() {
        // Utility class
    }

    /**
     * Strips Markdown backticks and attempts to locate the primary JSON object or array.
     *
     * @param raw Raw string from the AI model.
     * @return Cleaned JSON substring or original string if no delimiters found.
     */
    public static String cleanJsonString(String raw) {
        if (raw == null) {
            return "{}";
        }

        String text = raw.trim();

        // 1. Check for markdown code fences
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            text = matcher.group(1).trim();
        }

        // 2. Extract outermost { ... }
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1).trim();
            return text;
        }

        // 3. Extract outermost [ ... ]
        int firstBracket = text.indexOf('[');
        int lastBracket = text.lastIndexOf(']');
        if (firstBracket != -1 && lastBracket > firstBracket) {
            text = text.substring(firstBracket, lastBracket + 1).trim();
            return text;
        }

        return text;
    }

    /**
     * Attempts to parse the input as a JSONObject, applying cleaning and minor repair if needed.
     *
     * @param raw Raw input text from model.
     * @return Parsed JSONObject.
     * @throws JSONException if parsing fails after repair attempts.
     */
    public static JSONObject parseJsonObject(String raw) throws JSONException {
        String cleaned = cleanJsonString(raw);
        try {
            return new JSONObject(cleaned);
        } catch (JSONException e) {
            // Attempt mild heuristic repairs: trailing commas before '}' or ']'
            String repaired = cleaned.replaceAll(",\\s*([}\\]])", "$1");
            return new JSONObject(repaired);
        }
    }

    /**
     * Attempts to parse the input as a JSONArray.
     *
     * @param raw Raw input text from model.
     * @return Parsed JSONArray.
     * @throws JSONException if parsing fails.
     */
    public static JSONArray parseJsonArray(String raw) throws JSONException {
        String cleaned = cleanJsonString(raw);
        try {
            return new JSONArray(cleaned);
        } catch (JSONException e) {
            String repaired = cleaned.replaceAll(",\\s*([}\\]])", "$1");
            return new JSONArray(repaired);
        }
    }

    /**
     * Safely checks if the string can be parsed as a valid JSONObject.
     */
    public static boolean isValidJsonObject(String raw) {
        try {
            parseJsonObject(raw);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
