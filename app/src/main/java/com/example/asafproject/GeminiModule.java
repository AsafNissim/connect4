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

/**
 * GeminiModule - המחלקה שאחראית על התקשורת עם הבינה המלאכותית של גוגל (Gemini).
 * היא שולחת את מצב הלוח לענן ומקבלת בחזרה את המהלך הכי טוב שה-AI מציע.
 */
public class GeminiModule {

    private static final String TAG = "GEMINI";

    // מפתח ה-API האישי לצורך גישה לשירותי גוגל
    private static final String API_KEY = "AIzaSyAfZCdrWoQkY-kJGCoh-IHzSchol8Q9M8k";

    // שם המודל שבו אנחנו משתמשים (גרסה מהירה וחכמה)
    private static final String MODEL = "gemini-1.5-flash";

    // כתובת ה-URL של השרת אליו שולחים את הבקשה
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=" + API_KEY;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // הגדרת לקוח ה-Network (OkHttp) עם זמני המתנה (Timeout) מוגדרים
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(12, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build();

    // ממשק (Interface) לקבלת התשובה מה-AI (הצלחה או שגיאה)
    public interface MoveCallback {
        void onMove(int col);      // מהלך חוקי בין 0 ל-6
        void onError(String msg);  // הודעת שגיאה במקרה של תקלה
    }

    // ממשק לקבלת מצב הלוח הנוכחי בפורמט טקסט
    public interface CellStateProvider {
        String boardToGeminiText();
    }

    /**
     * הפעולה העיקרית: שולחת את מצב הלוח ל-Gemini ומבקשת מהלך.
     */
    public void requestHardMove(CellStateProvider provider, MoveCallback callback) {
        try {
            // קבלת ייצוג הטקסט של הלוח מהלוגיקה
            String boardText = provider.boardToGeminiText();

            // --- בניית גוף הבקשה (JSON) לפי הפורמט שגוגל דורשת ---
            JSONObject input = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject textPart = new JSONObject();

            // הפרומפט (ההוראה) לבינה המלאכותית: איך להתנהג ומה להחזיר
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

            // הגדרות נוספות למודל (טמפרטורה נמוכה = תשובות עקביות ולא יצירתיות מדי)
            JSONObject gen = new JSONObject();
            gen.put("temperature", 0.0);
            gen.put("maxOutputTokens", 256);
            input.put("generationConfig", gen);

            RequestBody body = RequestBody.create(input.toString(), JSON);

            // יצירת בקשת ה-HTTP
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // שליחת הבקשה בצורה אסינכרונית (ברקע)
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // נקרא במקרה של בעיית אינטרנט או שרת שלא עונה
                    Log.e(TAG, "onFailure: " + e.getMessage(), e);
                    postError(callback, "Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // קבלת התשובה מהשרת
                    String resp = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "HTTP " + response.code() + " RAW=" + resp);

                    if (!response.isSuccessful()) {
                        postError(callback, "HTTP " + response.code() + ": " + resp);
                        return;
                    }

                    try {
                        // ניתוח ה-JSON שחזר מגוגל כדי לחלץ את הטקסט שה-AI כתב
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
                        JSONArray outParts = outContent.optJSONArray("parts");
                        String outText = outParts.getJSONObject(0).optString("text", "").trim();
                        
                        Log.d(TAG, "MODEL_TEXT=" + outText);

                        // המרת הטקסט שה-AI שלח למספר עמודה (0-6)
                        int col = parseColAny(outText);
                        if (col < 0 || col > 6) {
                            postError(callback, "Bad output: '" + outText + "'");
                            return;
                        }

                        // החזרת המהלך ל-Activity
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

    /**
     * פונקציית עזר שמנתחת את התשובה של המודל. 
     * היא יודעת לחפש מספר גם אם המודל כתב "col=3" או סתם "3".
     */
    private int parseColAny(String text) {
        if (text == null) return -1;

        // ניסיון ראשון: בדיקה אם זה JSON תקין
        try {
            JSONObject obj = new JSONObject(text);
            if (obj.has("col")) {
                int c = obj.getInt("col");
                return (c >= 0 && c <= 6) ? c : -1;
            }
        } catch (Exception ignore) {}

        String t = text.toLowerCase();

        // ניסיון שני: חיפוש תבנית של "col=X"
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

        // ניסיון שלישי: פשוט לחפש את הספרה הראשונה שמופיעה בטקסט
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch >= '0' && ch <= '6') return ch - '0';
        }

        return -1;
    }

    /**
     * מעבירה את התוצאה חזרה ל-UI Thread (החוט הראשי) כדי שנוכל לעדכן את המסך.
     */
    private void postMove(MoveCallback cb, int col) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onMove(col));
    }

    /**
     * מעבירה את הודעת השגיאה חזרה ל-UI Thread.
     */
    private void postError(MoveCallback cb, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onError(msg));
    }
}
