package com.example.asafproject;


import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class InstructionActivity extends AppCompatActivity {
    TextToSpeech textToSpeech;
    TextView tvS;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruction);
        tvS = findViewById(R.id.tvS);
        tvS.setText("• Each player takes turns dropping a disc into a column.\n" +
                "• The disc falls to the lowest available space in that column.\n" +
                "• The goal: create a line of 4 discs in your color.\n" +
                "• The line can be horizontal, vertical, or diagonal.\n" +
                "\n" +
                "Good luck!");

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS)
                {
                    int lang = textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }

        });


       tvS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textToSpeech.speak("• Each player takes turns dropping a disc into a column.\n" +
                        "• The disc falls to the lowest available space in that column.\n" +
                        "• The goal: create a line of 4 discs in your color.\n" +
                        "• The line can be horizontal, vertical, or diagonal.\n" +
                        "\n" +
                        "Good luck!", TextToSpeech.QUEUE_FLUSH, null);
            }
        });

    }
    }