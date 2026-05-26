package com.example.asafproject;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * FB (Firebase) - המחלקה שאחראית על התקשורת עם מסד הנתונים בענן.
 * היא מאפשרת לשני שחקנים בטלפונים שונים לשחק אחד נגד השני על ידי סנכרון המהלכים.
 * המחלקה משתמשת בתבנית עיצוב מסוג Singleton (מופע יחיד).
 */
public class FB {
    private static FB instance; // המופע היחיד של המחלקה

    FirebaseDatabase database;  // אובייקט הגישה הראשי ל-Firebase
    DatabaseReference myRef;    // הצבעה למיקום ספציפי בבסיס הנתונים (הצומת "play")
    private static Context context;

    // קונסטרקטור פרטי - אי אפשר ליצור אובייקט של FB מבחוץ עם new
    private FB() {

        // התחברות למסד הנתונים של Firebase
        database = FirebaseDatabase.getInstance();

        // הגדרת הצומת (Key) שבו נשמור את המהלכים - נקרא "play"
        myRef = database.getReference("play"); 

        // הוספת מאזין (Listener) שפועל בכל פעם שהנתונים בענן משתנים
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // קבלת הערך (מספר העמודה) שנכתב בענן
                Integer col = snapshot.getValue(Integer.class);

                if (col == null) return;

                // בדיקה: אם הערך הוא לא 10 (ערך 10 משמש לאיפוס ולא נחשב למהלך)
                if(col != 10)
                {
                    // שליחת המהלך שהתקבל מהענן ללוח המשחק ב-GameActivity
                    ((GameActivity)context).boardGame.newColFromFirebase(col);
                    
                    // איפוס הערך ב-Firebase חזרה ל-10 כדי שהמהלך לא יתבצע שוב ושוב בטעות
                    setPlay(10);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // נקרא במקרה של שגיאה בתקשורת (למשל חוסר הרשאות)
            }
        });
    }

    /**
     * פעולה סטטית לקבלת המופע של FB.
     * מוודא שיהיה רק "דוור" אחד שמנהל את התקשורת בכל האפליקציה.
     */
    public static FB getInstance(Context context) {
        if (null == instance) {
            FB.context = context;
            instance = new FB();
        }
        return instance;
    }

    /**
     * עדכון המהלך ב-Firebase.
     * @param col מספר העמודה שהשחקן בחר.
     */
    public void setPlay(int col)
    {
        // כתיבת הערך לתוך הצומת "play" בענן
        myRef.setValue(col);
    }
}
