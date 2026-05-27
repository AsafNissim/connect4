package com.example.asafproject;

import android.os.Handler; 
import android.os.Looper;  
import android.util.Log;    
import org.json.JSONArray; 
import org.json.JSONObject; 
import java.io.IOException; 
import java.util.concurrent.TimeUnit; 
import okhttp3.*; 

/**
 * GeminiModule - מחלקה מפושטת לתקשורת עם ה-AI של גוגל (Gemini).
 */
public class GeminiModule {

    private static final String TAG = "GEMINI"; 
    private static final String API_KEY = "AIzaSyAfZCdrWoQkY-kJGCoh-IHzSchol8Q9M8k";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(15, TimeUnit.SECONDS)
            .build();

    public interface MoveCallback {
        void onMove(int col); 
        void onError(String msg); 
    }

    public interface CellStateProvider {
        String boardToGeminiText();
    }

    /**
     * פונקציה לשליחת בקשה ל-AI לקבלת מהלך "קשה" (חכם)
     */
    public void requestHardMove(CellStateProvider provider, final MoveCallback callback) {
        try {
            String boardText = provider.boardToGeminiText();
            String prompt = "You are a Connect-4 expert playing YELLOW. Analyze the board and return ONLY the best column (0-6).\n" + boardText;

            JSONObject requestJson = new JSONObject()
                .put("contents", new JSONArray().put(new JSONObject()
                    .put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))));

            RequestBody body = RequestBody.create(requestJson.toString(), JSON_TYPE);
            Request request = new Request.Builder().url(API_URL).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // החלפת ה-Lambda ב-Runnable אנונימי מפורש
                    runOnMain(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("חיבור נכשל");
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String resp = response.body().string();
                        if (!response.isSuccessful()) {
                            runOnMain(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError("שגיאת שרת");
                                }
                            });
                            return;
                        }

                        JSONObject json = new JSONObject(resp);
                        String aiText = json.getJSONArray("candidates").getJSONObject(0)
                                .getJSONObject("content").getJSONArray("parts")
                                .getJSONObject(0).getString("text").trim();

                        final int col = extractColumn(aiText);
                        if (col >= 0) {
                            runOnMain(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onMove(col);
                                }
                            });
                        } else {
                            runOnMain(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError("תשובה לא תקינה");
                                }
                            });
                        }
                    } catch (Exception e) {
                        runOnMain(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("שגיאה בניתוח הנתונים");
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("שגיאה כללית");
        }
    }

    private int extractColumn(String text) {
        for (char c : text.toCharArray()) { 
            if (c >= '0' && c <= '6') return c - '0'; 
        }
        return -1; 
    }

    private void runOnMain(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }
}
