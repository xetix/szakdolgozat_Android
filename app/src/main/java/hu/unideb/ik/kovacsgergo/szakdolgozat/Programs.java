package hu.unideb.ik.kovacsgergo.szakdolgozat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Programs extends AppCompatActivity {
    protected HashMap<String,HashMap<String,Double>> programok = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programs);

        FirebaseAuth auth = (FirebaseAuth) FirebaseAuth.getInstance();
        FirebaseUser user = (FirebaseUser) auth.getCurrentUser();
        FirebaseDatabase database = (FirebaseDatabase) FirebaseDatabase.getInstance();
        DatabaseReference programs = database.getReference(user.getUid()+"/programs");
        ImageView addBtn = (ImageView) findViewById(R.id.addBtn);

        programs.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Program> programokList = new ArrayList<Program>();
                for(DataSnapshot day : dataSnapshot.getChildren()) {
                    String keyForDay = day.getKey();
                    for( DataSnapshot hour : day.getChildren() ){
                        String keyForHour = hour.getKey();
                        Double value = hour.getValue(Double.class);
                        programokList.add(new Program(keyForDay,keyForHour,value));
                    }
                }
                RecyclerView rvPrograms = (RecyclerView) findViewById(R.id.programsRv);
                TextView noDataTxt = (TextView) findViewById(R.id.noDataText);
                Collections.sort(programokList);
                noDataTxt.setVisibility((programokList.isEmpty()) ? View.VISIBLE : View.INVISIBLE);
                ProgramsAdapter adapter = new ProgramsAdapter(programokList);
                rvPrograms.setAdapter(adapter);
                rvPrograms.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Spinner daySelect = new Spinner(Programs.this);
                final Spinner hourSelect = new Spinner(Programs.this);
                final EditText setpointEditText = new EditText(Programs.this);
                final TextView unitTexView = new TextView(Programs.this);
                final LinearLayout mainContainer = new LinearLayout(Programs.this);
                final LinearLayout container = new LinearLayout(Programs.this);
                final TextView errorTexView = new TextView(Programs.this);
                final String[] days = Program.getDays();
                final String[] hours = Program.getHours();

                AlertDialog.Builder builder = new AlertDialog.Builder(Programs.this);
                builder.setTitle("Új program hozzáadása:");

                mainContainer.setOrientation(LinearLayout.VERTICAL);
                container.setPadding(16,64,16,0);
                container.setOrientation(LinearLayout.HORIZONTAL);
                mainContainer.addView(container);
                builder.setView(mainContainer);

                final ArrayAdapter<String> adapterForDays = new ArrayAdapter<String>(
                        Programs.this,
                        android.R.layout.simple_spinner_item, days
                );
                daySelect.setAdapter(adapterForDays);
                container.addView(daySelect);

                final ArrayAdapter<String> adapterForHours = new ArrayAdapter<String>(
                        Programs.this,
                        android.R.layout.simple_spinner_item, hours
                );
                hourSelect.setAdapter(adapterForHours);
                container.addView(hourSelect);

                setpointEditText.setText("22.0");
                setpointEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                setpointEditText.setTextColor(Color.WHITE);
                container.addView(setpointEditText);

                unitTexView.setText("°c");
                unitTexView.setTextColor(Color.WHITE);
                container.addView(unitTexView);

                errorTexView.setTextColor(Color.RED);
                errorTexView.setPadding(44,0,44,0);
                errorTexView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                errorTexView.setText("");

                mainContainer.addView(errorTexView);

                builder.setPositiveButton("Hozzáadás", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String iDay = Program.translateDay(daySelect.getSelectedItem().toString());
                        String iHour = hourSelect.getSelectedItem().toString().substring(0,2);
                        Double iSetpoint = Math.round(Double.parseDouble(setpointEditText.getText().toString())* 10.0) / 10.0;
                        programs.child(iDay).child(iHour).setValue(iSetpoint);
                    }
                });

                builder.setNegativeButton("Mégsem", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });

                final AlertDialog alertDialog = builder.create();
                alertDialog.show();

                setpointEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String string = charSequence.toString();
                        if(string.length() < 1) string = "0";
                        Float value = Float.parseFloat(string);
                        if( value < 5 ){
                            setpointEditText.setTextColor(Color.RED);
                            errorTexView.setText("Az értéknek minimum "+5.0+"°C-nak kell lennie.");
                            ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }else if( value > 40 ){
                            setpointEditText.setTextColor(Color.RED);
                            errorTexView.setText("Az értéknek maximum "+40.0+"°C-nak kell lehet.");
                            ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }else{
                            setpointEditText.setTextColor(Color.WHITE);
                            errorTexView.setText("");
                            ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
            }
        });
    }

    public void back(View v){
        onBackPressed();
    }
}