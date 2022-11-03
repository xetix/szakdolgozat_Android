package hu.unideb.ik.kovacsgergo.szakdolgozat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

class ProgramsAdapter extends RecyclerView.Adapter<ProgramsAdapter.ViewHolder> {
    private List<Program> mPrograms;

    public ProgramsAdapter(List<Program> programs) {
        mPrograms = programs;
    }

    @NonNull
    @Override
    public ProgramsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View contactView = inflater.inflate(R.layout.programs_row, parent, false);

        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Program program = mPrograms.get(position);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference programsDb = database.getReference(user.getUid()+"/programs");
        TextView dayTxt = holder.dayTxt;
        TextView timeTxt = holder.timeTxt;
        TextView spTxt = holder.spTxt;
        ImageButton editItem = holder.editItem;
        ImageButton deleteItem = holder.deleteItem;
        dayTxt.setText(program.getDayHun());
        timeTxt.setText("Kezdés: "+program.getHourFull());
        spTxt.setText(program.getSetpoint()+"°c");
        editItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder inputDialog = new AlertDialog.Builder(view.getContext());
                final EditText input = new EditText(view.getContext());
                final TextView errorMsg = new TextView(view.getContext());

                inputDialog.setTitle("Új szetpont megadása:");
                input.setGravity(Gravity.CENTER);
                inputDialog.setView(input);
                LinearLayout container = new LinearLayout(view.getContext());
                container.setPadding(100,8,100,8);
                container.setOrientation(LinearLayout.VERTICAL);
                container.addView(input);
                container.addView(errorMsg);
                inputDialog.setView(container);
                input.setText(program.getSetpoint()+"");
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setTextColor(Color.WHITE);
                errorMsg.setTextColor(Color.RED);

                inputDialog.setPositiveButton("Beállítás", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        FirebaseAuth mAuth = FirebaseAuth.getInstance();
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
                        DatabaseReference programs = mDatabase.getReference(currentUser.getUid()+"/programs");
                        String string = input.getText().toString();
                        if(string.length() < 1) string = "0";
                        program.setSetpoint(Math.round( Double.parseDouble(string)* 10.0) / 10.0);
                        programs.child(program.getDay()).child(program.getHour()).setValue(program.getSetpoint());
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
                        if( value < 5 ){
                            input.setTextColor(Color.RED);
                            errorMsg.setText("Az értéknek minimum "+5.0+"°C-nak kell lennie.");
                            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }else if( value > 40 ){
                            input.setTextColor(Color.RED);
                            errorMsg.setText("Az értéknek maximum "+40.0+"°C-nak kell lehet.");
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
        });
        deleteItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                programsDb.child(program.getDay()+"/"+program.getHour()).removeValue();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPrograms.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView dayTxt;
        public TextView timeTxt;
        public TextView spTxt;
        public ImageButton editItem;
        public ImageButton deleteItem;

        public ViewHolder(View itemView) {
            super(itemView);
            dayTxt = (TextView) itemView.findViewById(R.id.dayTxt);
            timeTxt = (TextView) itemView.findViewById(R.id.timeTxt);
            spTxt = (TextView) itemView.findViewById(R.id.spTxt);
            editItem = (ImageButton) itemView.findViewById(R.id.editBtn);
            deleteItem = (ImageButton) itemView.findViewById(R.id.deleteBtn);
        }
    }
}
