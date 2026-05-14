package com.example.asafproject;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

// google explanations
// https://firebase.google.com/docs/database/android/lists-of-data#java_1


public class FB {
    private static FB instance;

    FirebaseDatabase database;
    DatabaseReference myRef;
    private static Context context;

    private FB() {

        //database = FirebaseDatabase.getInstance("https://fbrecordssingletone-default-rtdb.firebaseio.com/");
        database = FirebaseDatabase.getInstance();

        myRef = database.getReference("play"); //

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange( DataSnapshot snapshot) {
                int col = snapshot.getValue(Integer.class);

                if(col != 10)//איפוס פייר בייס לערך לא הגיוני לתיקון באג
                {
                    ((GameActivity)context).boardGame.newColFromFirebase(col);
                    setPlay(10);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


    }

    public static FB getInstance(Context context) {
        if (null == instance) {
            FB.context = context;

            instance = new FB();
        }
        return instance;
    }

    public void setPlay(int col)
    {
        // Write a message to the database
        myRef.setValue(col);
    }
}
//