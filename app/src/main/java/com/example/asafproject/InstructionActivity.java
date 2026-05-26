package com.example.asafproject;


import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * InstructionActivity - מסך הוראות המשחק.
 * כולל טקסט הסבר ואפשרות להקראת ההוראות בקול (Text To Speech).
 */
public class InstructionActivity extends AppCompatActivity {
    
    TextToSpeech textToSpeech; // אובייקט האחראי על המרת טקסט לדיבור
    TextView tvS;              // רכיב להצגת הטקסט במסך

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruction); // טעינת העיצוב
        
        tvS = findViewById(R.id.tvS);
        
        // הגדרת טקסט ההוראות
        String instructions = "• Each player takes turns dropping a disc into a column.\n" +
                "• The disc falls to the lowest available space in that column.\n" +
                "• The goal: create a line of 4 discs in your color.\n" +
                "• The line can be horizontal, vertical, or diagonal.\n" +
                "\n" +
                "Good luck!";
        
        tvS.setText(instructions);

        // אתחול מנגנון ה-TextToSpeech (הקראה קולית)
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // בדיקה אם האתחול הצליח
                if(status == TextToSpeech.SUCCESS)
                {
                    // הגדרת השפה לאנגלית
                    int lang = textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        // הגדרת לחיצה על הטקסט כדי להפעיל את ההקראה
        tvS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // פקודה להקריא את הטקסט בקול
                // QUEUE_FLUSH - עוצר כל הקראה קודמת ומתחיל מחדש
                textToSpeech.speak(instructions, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
    }

    // שחרור משאבי ה-TTS בסגירת ה-Activity (חשוב כדי למנוע דליפות זיכרון)
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}