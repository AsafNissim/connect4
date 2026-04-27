package com.example.asafproject;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiModule {

    private static final String TAG = "GEMINI";

    // ⚠️ אל תעלה את זה לגיטהאב. עדיף לשים בקובץ מקומי/BuildConfig, אבל כרגע נשאיר פשוט.
    private static final String API_KEY = "AIzaSyAfZCdrWoQkY-kJGCoh-IHzSchol8Q9M8k";

    // ✅ זה המודל שכבר ראינו שעונה אצלך HTTP 200
    private static final String MODEL = "gemini-2.5-flash";

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=" + API_KEY;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(12, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build();

    public interface MoveCallback {
        void onMove(int col);      // 0-6
        void onError(String msg);
    }

    public interface CellStateProvider {
        String boardToGeminiText();
    }

    public void requestHardMove(CellStateProvider provider, MoveCallback callback) {
        try {
            String boardText = provider.boardToGeminiText();

            // --- Build request ---
            JSONObject input = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject textPart = new JSONObject();

            // ✅ פרומפט קצר כדי לא “לבזבז” טוקנים
            // ✅ מבקש תשובה בפורמט פשוט (col=3) אבל גם מאפשר JSON
            textPart.put("text",
                    "You are CONNECT-4 engine playing YELLOW.\n" +
                            "You MUST analyze at least 3 candidate columns and look ahead 4 plies (Y,R,Y,R).\n" +
                            "If you can win now, do it. If you must block, do it.\n" +
                            "Output ONLY: col=<0-6>\n" +
                            "Board:\n" + boardText
            );

            parts.put(textPart);
            content.put("parts", parts);
            contents.put(content);
            input.put("contents", contents);

            // ✅ חשוב: מספיק tokens כדי שלא ייחתך ב-MAX_TOKENS כמו שהיה לך
            JSONObject gen = new JSONObject();
            gen.put("temperature", 0.0);
            gen.put("maxOutputTokens", 256);
            // אל תשים responseMimeType/JSON חובה — אצלך זה התחיל להתנהג מוזר.
            // נשאיר תשובה חופשית קצרה ונפרסר חכם.
            input.put("generationConfig", gen);

            RequestBody body = RequestBody.create(input.toString(), JSON);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "onFailure: " + e.getMessage(), e);
                    postError(callback, "Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resp = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "HTTP " + response.code() + " RAW=" + resp);

                    if (!response.isSuccessful()) {
                        postError(callback, "HTTP " + response.code() + ": " + resp);
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(resp);

                        if (json.has("error")) {
                            postError(callback, "API error: " + json.getJSONObject("error").toString());
                            return;
                        }

                        JSONArray candidates = json.optJSONArray("candidates");
                        if (candidates == null || candidates.length() == 0) {
                            postError(callback, "No candidates. RAW=" + resp);
                            return;
                        }

                        JSONObject first = candidates.getJSONObject(0);
                        JSONObject outContent = first.optJSONObject("content");
                        if (outContent == null) {
                            postError(callback, "No content. finishReason=" + first.optString("finishReason") + " RAW=" + resp);
                            return;
                        }

                        JSONArray outParts = outContent.optJSONArray("parts");
                        if (outParts == null || outParts.length() == 0) {
                            postError(callback, "No parts. finishReason=" + first.optString("finishReason") + " RAW=" + resp);
                            return;
                        }

                        String outText = outParts.getJSONObject(0).optString("text", "").trim();
                        Log.d(TAG, "MODEL_TEXT=" + outText);

                        int col = parseColAny(outText);
                        if (col < 0 || col > 6) {
                            postError(callback, "Bad output: '" + outText + "'");
                            return;
                        }

                        postMove(callback, col);

                    } catch (Exception ex) {
                        postError(callback, "Parse error: " + ex.getMessage() + " RAW=" + resp);
                    }
                }
            });

        } catch (Exception e) {
            postError(callback, "Build request error: " + e.getMessage());
        }
    }

    // ✅ פרסר “סלחני”: JSON {"col":3} / col=3 / "3"
    private int parseColAny(String text) {
        if (text == null) return -1;

        // JSON
        try {
            JSONObject obj = new JSONObject(text);
            if (obj.has("col")) {
                int c = obj.getInt("col");
                return (c >= 0 && c <= 6) ? c : -1;
            }
        } catch (Exception ignore) {}

        String t = text.toLowerCase();

        // col=3
        int idx = t.indexOf("col=");
        if (idx != -1) {
            idx += 4;
            StringBuilder num = new StringBuilder();
            while (idx < t.length() && Character.isDigit(t.charAt(idx))) {
                num.append(t.charAt(idx));
                idx++;
            }
            if (num.length() > 0) {
                int c = Integer.parseInt(num.toString());
                return (c >= 0 && c <= 6) ? c : -1;
            }
        }

        // מספר בודד איפשהו
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch >= '0' && ch <= '6') return ch - '0';
        }

        return -1;
    }

    private void postMove(MoveCallback cb, int col) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onMove(col));
    }

    private void postError(MoveCallback cb, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onError(msg));
    }
}
