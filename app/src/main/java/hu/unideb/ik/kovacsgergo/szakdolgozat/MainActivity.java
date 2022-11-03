package hu.unideb.ik.kovacsgergo.szakdolgozat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    protected String measuredTimeStamp;
    protected Float measuredValue = (float)0.0;
    protected Boolean heaterState;
    protected Boolean manualMode = true;
    protected Boolean priorityOn;
    protected Boolean syncSuccess = false;
    protected Float actualSp = (float)0.0;
    protected Float actualHys;
    protected Timer timer;
    protected List<Program> programsList;
    protected double programSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView mv = (TextView) findViewById(R.id.measuredValue);
        TextView stateTxt = (TextView)findViewById(R.id.stateTxt);
        ImageView heaterIcon = (ImageView)findViewById(R.id.heaterIcon);
        TextView syncText = (TextView) findViewById(R.id.syncTxt);
        ImageView syncIcon = (ImageView)findViewById(R.id.syncIcon);

        Button spBtn = (Button) findViewById(R.id.spBtn);
        Button hysBtn = (Button) findViewById(R.id.hysBtn);
        Button modeBtn = (Button) findViewById(R.id.modeBtn);
        Button priorityOnBtn = (Button) findViewById(R.id.priorityOnBtn);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        programsList = new ArrayList<Program>();

        syncText.setText("Termosztát keresése...");
        syncIcon.setColorFilter(Color.YELLOW);

        if(currentUser == null){
            mAuth.signInWithEmailAndPassword("kovacsgergo8303@gmail.com", "jelszo_1324")
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                            } else {
                                popToast("Authentication failed.");
                            }
                        }
                    });
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference controll = database.getReference(currentUser.getUid()+"/controll");
        DatabaseReference measure = database.getReference(currentUser.getUid()+"/meassure");
        DatabaseReference programs = database.getReference(currentUser.getUid()+"/programs");

        spBtn.setText("Kívánt\n--.-°c");
        hysBtn.setText("Érzékenység\n-.-°c");
        modeBtn.setText("---\nüzemmód");

        controll.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if( snapshot.exists() ){
                    Float setPoint = snapshot.child("setpoint").getValue(Float.class);
                    Float hysteresis = snapshot.child("hysteresis").getValue(Float.class);
                    manualMode = snapshot.child("manual_mode").getValue(Boolean.class);
                    priorityOn = snapshot.child("priority_on").getValue(Boolean.class);
                    hysBtn.setText("Érzékenység\n"+hysteresis+"°c");
                    modeBtn.setText((manualMode) ? "Kézi\nüzemmód" : "Automata\nüzemmód");
                    priorityOnBtn.setText((priorityOn) ? "Direkt fűtés\nkikapcsolása" : "Fűtés indítása\nmost!");
                    spBtn.setEnabled(manualMode);
                    actualSp = setPoint;
                    actualHys = hysteresis;
                    if(manualMode){
                        spBtn.getBackground().setColorFilter(null);
                        spBtn.setText("Kívánt\n"+actualSp+"°c");
                    }else{
                        spBtn.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
                        spBtn.setText("Kívánt\n"+programSp+"°c");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("controll", "Failed to read value.", error.toException());
            }
        });

        measure.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    measuredValue = dataSnapshot.child("value").getValue(Float.class);
                    measuredTimeStamp = dataSnapshot.child("time").getValue(String.class);
                    heaterState = dataSnapshot.child("status").getValue(Boolean.class);
                    if(syncSuccess){
                        mv.setText(measuredValue+"°c");
                        stateTxt.setText("Fűtés "+((heaterState)? "bekapcsolva" : "kikapcsolva"));
                        heaterIcon.setVisibility(View.VISIBLE);
                        heaterIcon.setColorFilter((heaterState) ? Color.rgb(233,30,99) : Color.LTGRAY);
                    }else{
                        stateTxt.setText("Fűtés állapota ismeretlen.");
                        heaterIcon.setVisibility(View.INVISIBLE);
                    }

                }else{
                    popToast("Sikertelen adatlekérés.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w("meassure", "Failed to read value.", error.toException());
            }
        });

        programs.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                programsList = new ArrayList<Program>();
                for(DataSnapshot day : snapshot.getChildren()) {
                    String keyForDay = day.getKey();
                    for( DataSnapshot hour : day.getChildren() ){
                        String keyForHour = hour.getKey();
                        Double value = hour.getValue(Double.class);
                        programsList.add(new Program(keyForDay,keyForHour,value));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        modeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                controll.child("manual_mode").setValue(!manualMode);
            }
        });

        priorityOnBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Boolean on = !priorityOn;
                controll.child("priority_on").setValue(on);
            }
        });

        spBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                alertView(
                        "Kívánt hőmérséklet beállítása:",
                        actualSp,
                        Float.parseFloat("5.0"),
                        Float.parseFloat("40.0"),
                        "setpoint"
                );
            }
        });

        hysBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertView(
                        "Kívánt érzékenység beállítása:",
                        actualHys,
                        Float.parseFloat("0.1"),
                        Float.parseFloat("1.0"),
                        "hysteresis"
                );
            }
        });

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                syncron();
            }

        }, 0, 1*60*1000);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updatePrograms();
            }
        },0,1000);
    }

    public void goToPrograms(View v) {
        Intent intent = new Intent(MainActivity.this, Programs.class);
        startActivity(intent);
    }

    private void popToast(String s){
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void alertView(String title, Float initVal, Float minVal, Float maxVal, String target) {
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
        final EditText input = new EditText(this);
        final TextView errorMsg = new TextView(this);

        inputDialog.setTitle(title);
        input.setGravity(Gravity.CENTER);
        inputDialog.setView(input);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(100,8,100,8);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input);
        container.addView(errorMsg);
        inputDialog.setView(container);
        input.setText(initVal+"");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setTextColor(Color.WHITE);
        errorMsg.setTextColor(Color.RED);

        inputDialog.setPositiveButton("Beállítás", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
                DatabaseReference controll = mDatabase.getReference(currentUser.getUid()+"/controll");
                String string = input.getText().toString();
                if(string.length() < 1) string = "0";
                controll.child(target).setValue(Math.round( Double.parseDouble(string)* 10.0) / 10.0);
            }
        });

        inputDialog.setNegativeButton("Mégsem", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = inputDialog.create();
        dialog.show();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String string = charSequence.toString();
                if(string.length() < 1) string = "0";
                Float value = Float.parseFloat(string);
                if( value < minVal ){
                    input.setTextColor(Color.RED);
                    errorMsg.setText("Az értéknek minimum "+minVal+"°C-nak kell lennie.");
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }else if( value > maxVal ){
                    input.setTextColor(Color.RED);
                    errorMsg.setText("Az értéknek maximum "+maxVal+"°C-nak kell lehet.");
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }else{
                    input.setTextColor(Color.WHITE);
                    errorMsg.setText("");
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void updatePrograms(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView actDay = (TextView)findViewById(R.id.actDay);
                TextView actHour = (TextView)findViewById(R.id.actHour);
                TextView actSp = (TextView)findViewById(R.id.actSp);
                TextView nextDay = (TextView)findViewById(R.id.nextDay);
                TextView nextHour = (TextView)findViewById(R.id.nextHour);
                TextView nextSp = (TextView)findViewById(R.id.nextSp);
                Button spBtn = (Button)findViewById(R.id.spBtn);

                List<Program> programs = new ArrayList<Program>();
                programs.addAll(programsList);
                Format f = new SimpleDateFormat("EEEE", Locale.ENGLISH);
                String actualDay = f.format(new Date());
                SimpleDateFormat formatter = new SimpleDateFormat("kk");
                String actualHour = formatter.format(new Date());
                Program now = new Program(actualDay,actualHour+"x", 0.0);
                programs.add(now);
                Collections.sort(programs);
                int i = programs.indexOf(now);
                if( programs.size() == 1 ){
                    actDay.setText("");
                    actHour.setText("Nincs program.");
                    actSp.setText("");
                    nextDay.setText("");
                    nextHour.setText("Nincs program.");
                    nextSp.setText("");
                    programSp = actualSp;
                }else if( programs.size() == 2 ){
                    int i2 = ( i == 0 ) ? 1 : 0;
                    actDay.setText(programs.get(i2).getDayHun());
                    actHour.setText(programs.get(i2).getHourFull());
                    actSp.setText(programs.get(i2).getSetpoint()+"°c");
                    nextDay.setText("");
                    nextHour.setText("Nincs következő.");
                    nextSp.setText("");
                    programSp = programs.get(i2).getSetpoint();
                }else{
                    int i2, i3;
                    if( i == 0 ){
                        i2 = programs.size()-1;
                        i3 = 1;
                    }else if( i == programs.size()-1 ){
                        i2 = i-1;
                        i3 = 0;
                    }else{
                        i2 = i-1;
                        i3 = i+1;
                    }
                    actDay.setText(programs.get(i2).getDayHun());
                    actHour.setText(programs.get(i2).getHourFull());
                    actSp.setText(programs.get(i2).getSetpoint()+"°c");
                    nextDay.setText(programs.get(i3).getDayHun());
                    nextHour.setText(programs.get(i3).getHourFull());
                    nextSp.setText(programs.get(i3).getSetpoint()+"°c");
                    programSp = programs.get(i2).getSetpoint();
                }
                if(manualMode){
                    spBtn.getBackground().setColorFilter(null);
                    spBtn.setText("Kívánt\n"+actualSp+"°c");
                }else{
                    spBtn.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
                    spBtn.setText("Kívánt\n"+programSp+"°c");
                }
            }
        });
    }

    private void syncron(){
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView syncText = (TextView) findViewById(R.id.syncTxt);
                ImageView syncIcon = (ImageView) findViewById(R.id.syncIcon);
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseUser user = auth.getCurrentUser();
                DatabaseReference syncron = database.getReference(user.getUid() + "/syncron");
                syncron.child("syn").setValue(true);
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        syncron.child("ack").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                TextView mvTV = (TextView) findViewById(R.id.measuredValue);
                                TextView stateTxt = (TextView) findViewById(R.id.stateTxt);
                                ImageView heaterIcon = (ImageView)findViewById(R.id.heaterIcon);
                                if (!task.isSuccessful()) {
                                    Log.e("firebase", "Error getting data", task.getException());
                                } else {
                                    if (task.getResult().getValue(Boolean.class)) {
                                        syncText.setText("Termosztát aktív");
                                        syncIcon.setColorFilter(Color.GREEN);
                                        syncSuccess = true;
                                        mvTV.setText(measuredValue+"°c");
                                        stateTxt.setText("Fűtés "+((heaterState)? "bekapcsolva" : "kikapcsolva"));
                                        heaterIcon.setVisibility(View.VISIBLE);
                                        heaterIcon.setColorFilter((heaterState) ? Color.rgb(233,30,99) : Color.LTGRAY);
                                    } else {
                                        syncText.setText("Termosztát inaktív");
                                        syncIcon.setColorFilter(Color.RED);
                                        syncSuccess = false;
                                        mvTV.setText("--.-°c");
                                        stateTxt.setText("Fűtés állapota ismeretlen.");
                                        heaterIcon.setVisibility(View.INVISIBLE);
                                    }
                                    syncron.child("syn").setValue(false);
                                    syncron.child("ack").setValue(false);
                                }
                            }
                        });
                    }
                },10*1000);
            }
        });
    }
}