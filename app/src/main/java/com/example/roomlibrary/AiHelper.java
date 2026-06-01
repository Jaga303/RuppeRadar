package com.example.roomlibrary;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiHelper {
    // Replace with your actual API Key from Google AI Studio
    private static final String API_KEY = "REPLACE_WITH_YOUR_GEMINI_API_KEY";

    public interface AiResponseListener {
        void onSuccess(String response);
        void onError(String error);
    }

    public static void getFinancialInsights(String data, AiResponseListener listener) {
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder()
                .addText("You are a professional financial advisor for an app called RuppeRadar. " +
                        "Analyze the following monthly transaction data (Category: Amount) and provide: " +
                        "1. A brief summary. 2. Three specific suggestions to save money. " +
                        "3. A prediction for next month's total expense based on trends.\n\nData:\n" + data)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                new Handler(Looper.getMainLooper()).post(() -> listener.onSuccess(resultText));
            }

            @Override
            public void onFailure(Throwable t) {
                new Handler(Looper.getMainLooper()).post(() -> listener.onError(t.getMessage()));
            }
        }, executor);
    }
}
