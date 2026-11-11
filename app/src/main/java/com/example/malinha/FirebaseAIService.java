package com.example.malinha;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.Schema;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Asynchronous client for the Gemini Developer API. */
public final class FirebaseAIService {
    public static final String MODEL = "gemini-3.1-flash-lite";

    public static int failureMessage(Throwable error, boolean checkingAppCheck) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String type = cause.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
            String message = String.valueOf(cause.getMessage()).toLowerCase(java.util.Locale.ROOT);
            if (cause instanceof java.io.IOException || type.contains("network")
                    || message.contains("unable to resolve host") || message.contains("timeout")) {
                return R.string.draft_ai_network;
            }
            if (message.contains("429") || message.contains("resource_exhausted") || message.contains("quota")) {
                return checkingAppCheck ? R.string.draft_ai_app_check : R.string.draft_ai_quota;
            }
            if (message.contains("app check") || message.contains("appcheck")) {
                return R.string.draft_ai_app_check;
            }
            if (!checkingAppCheck && (message.contains("404") || message.contains("not_found") || type.contains("notfound"))) {
                return R.string.draft_ai_model;
            }
            if (!checkingAppCheck && (message.contains("403") || message.contains("401")
                    || message.contains("permission_denied") || message.contains("api_key")
                    || type.contains("permission") || type.contains("unauthenticated"))) {
                return R.string.draft_ai_permission;
            }
        }
        return checkingAppCheck ? R.string.draft_ai_app_check : R.string.draft_ai_error;
    }

    public ListenableFuture<GenerateContentResponse> suggest(String prompt) {
        Map<String, Schema> fields = new LinkedHashMap<>();
        fields.put("name", Schema.str());
        fields.put("city", Schema.str());
        fields.put("country", Schema.str());
        fields.put("category", Schema.str());
        fields.put("description", Schema.str());
        fields.put("duration_minutes", Schema.numInt());
        fields.put("suggested_time", Schema.str());
        fields.put("cost", Schema.str());
        GenerationConfig.Builder config = new GenerationConfig.Builder();
        config.responseMimeType = "application/json";
        config.responseSchema = Schema.array(Schema.obj(fields, Collections.singletonList("cost")));
        GenerativeModelFutures model = GenerativeModelFutures.from(FirebaseAI
            .getInstance(GenerativeBackend.googleAI()).generativeModel(MODEL, config.build()));
        return model.generateContent(new Content.Builder().addText(prompt).build());
    }

    public ListenableFuture<GenerateContentResponse> generate(String prompt) {
        GenerativeModelFutures model = GenerativeModelFutures.from(
            FirebaseAI.getInstance(GenerativeBackend.googleAI()).generativeModel(MODEL));
        Content input = new Content.Builder().addText(prompt).build();
        return model.generateContent(input);
    }
}
