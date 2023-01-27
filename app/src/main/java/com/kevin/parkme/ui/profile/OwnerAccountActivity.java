package com.kevin.parkme.ui.profile;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;

import com.kevin.parkme.R;
import com.kevin.parkme.classes.OwnerAccountInfo;;
import com.kevin.parkme.classes.User;
import com.kevin.parkme.utils.AppConstants;
import com.kevin.parkme.utils.BasicUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OwnerAccountActivity extends AppCompatActivity{
    private FirebaseAuth auth;
    private FirebaseDatabase db;

    AppCompatEditText accountNoText,sortNumText;
    Button bt_submit;

    OwnerAccountInfo ownerInfo;
    User userObj;
    String userID;

    BasicUtils utils=new BasicUtils();

    AppConstants globalClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owneracc_details);

        initComponents();
        attachListeners();
        if(!utils.isNetworkAvailable(getApplication())){
            Toast.makeText(OwnerAccountActivity.this, "No Network Available!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);  //slide from left to right
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);  //slide from left to right
    }

    private void initComponents() {
        getSupportActionBar().setTitle("Account Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        auth= FirebaseAuth.getInstance();
        db= FirebaseDatabase.getInstance();

        globalClass=(AppConstants)getApplicationContext();
        userID=auth.getCurrentUser().getUid();

        userObj=globalClass.getUserObj();

        accountNoText=findViewById(R.id.accNumText);
        sortNumText=findViewById(R.id.sortCodeText);
        bt_submit=findViewById(R.id.bt_submit);
    }

    private void attachListeners() {
        db.getReference().child("OwnerAccountInfo").child(userID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ownerInfo=snapshot.getValue(OwnerAccountInfo.class);
                if(ownerInfo!=null){
                    accountNoText.setText(ownerInfo.accountNo);
                    accountNoText.setSelection(accountNoText.getText().length());
                    sortNumText.setText(ownerInfo.SortCode);
                    sortNumText.setSelection(sortNumText.getText().length());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });


        bt_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String accountNumber = accountNoText.getText().toString();
                String sortCode = sortNumText.getText().toString();

                if(accountNumber.isEmpty() || sortCode.isEmpty()){
                    Toast.makeText(OwnerAccountActivity.this, "Please fill all details!", Toast.LENGTH_SHORT).show();
                }else{
                    ownerInfo=new OwnerAccountInfo(accountNumber,sortCode);
                    db.getReference("ownerInfo")
                            .child(userID)
                            .setValue(ownerInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(OwnerAccountActivity.this, "Success", Toast.LENGTH_SHORT).show();
                                        finish();
                                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);  //slide from left to right
                                    } else {
                                        Toast.makeText(OwnerAccountActivity.this, "Failed to add account details", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
    }
}
