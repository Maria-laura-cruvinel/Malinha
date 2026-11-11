package com.example.malinha;

import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.ai.type.GenerateContentResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

/** Explicitly opt in with the liveGemini instrumentation argument. */
@RunWith(AndroidJUnit4.class)
public final class FirebaseAILiveTest {
    private static final String TAG = "MalinhaAI";
    private static final String PROMPT = "Sugira três pontos turísticos para visitar em Paris, França";

    @Test public void geminiRespondsToParisPrompt() {
        Assume.assumeTrue("Use liveGemini=true para executar a chamada real.",
            "true".equals(InstrumentationRegistry.getArguments().getString("liveGemini")));

        try {
            Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false), 25, TimeUnit.SECONDS);
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            reportFailure("App Check: verifique a conexão e o registro do token debug no Firebase Console.");
            return;
        }

        Log.i(TAG, "Consultando " + FirebaseAIService.MODEL + " pela Gemini Developer API.");
        ListenableFuture<GenerateContentResponse> response = null;
        String answer;
        try {
            response = new FirebaseAIService().generate(PROMPT);
            answer = response.get(60, TimeUnit.SECONDS).getText();
        } catch (Exception error) {
            if (response != null) response.cancel(true);
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            reportFailure(safeError(error));
            return;
        }

        if (answer == null || answer.trim().isEmpty()) {
            reportFailure("O Gemini retornou uma resposta sem texto.");
            return;
        }
        Log.i(TAG, "Resposta do Gemini:\n" + answer);
    }

    private static String safeError(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String type = cause.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            String message = String.valueOf(cause.getMessage()).toLowerCase(Locale.ROOT);
            if (cause instanceof TimeoutException || cause instanceof IOException || type.contains("network")) {
                return "Falha de conexão ou tempo limite ao consultar o Gemini.";
            }
            if (message.contains("app check") || message.contains("appcheck") || type.contains("appcheck")) {
                return "O serviço rejeitou a verificação do App Check.";
            }
            if (type.contains("quota") || message.contains("resource_exhausted")
                    || message.contains("quota") || message.contains("429")) {
                return "Cota ou limite de requisições atingido na Gemini Developer API.";
            }
            if (type.contains("permission") || message.contains("unauthenticated")
                    || message.contains("permission_denied") || message.contains("401")
                    || message.contains("403") || message.contains("api_key")) {
                return "Falha de autorização. Verifique o Firebase AI Logic e a Gemini Developer API no projeto.";
            }
            if (type.contains("notfound") || message.contains("404") || message.contains("not_found")) {
                return "O modelo não está disponível para este projeto ou backend.";
            }
        }
        return "A consulta ao Gemini falhou. Confira a configuração do Firebase AI Logic.";
    }

    private static void reportFailure(String reason) {
        Log.e(TAG, reason);
        fail(reason);
    }
}
