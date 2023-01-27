package com.kevin.parkme.ui.profile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kevin.parkme.R;
import com.kevin.parkme.classes.PaymentInfo;
import com.kevin.parkme.classes.User;
import com.kevin.parkme.utils.AppConstants;
import com.kevin.parkme.utils.BasicUtils;
import com.manojbhadane.PaymentCardView;

public class CardDetailsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseDatabase db;

    PaymentInfo paymentInfo;
    User userObj;
    String userID;

    BasicUtils utils=new BasicUtils();

    AppConstants globalClass;
    AppCompatEditText creditCard, year, month, cardNumber, cvvNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_details);

        initComponents();
        attachListeners();

        if(!utils.isNetworkAvailable(getApplication())){
            Toast.makeText(CardDetailsActivity.this, "No Network Available!", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("WrongViewCast")
    private void initComponents() {
        getSupportActionBar().setTitle("Account Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        auth= FirebaseAuth.getInstance();
        db= FirebaseDatabase.getInstance();

        globalClass=(AppConstants)getApplicationContext();
        userID=auth.getCurrentUser().getUid();

        userObj=globalClass.getUserObj();

        month=findViewById(R.id.sprmonth);
        year=findViewById(R.id.spryear);
        cardNumber=findViewById(R.id.edtCardNumber);
        cvvNumber=findViewById(R.id.edtCvvNumber);
    }

    private void attachListeners() {
        //Init
        PaymentCardView paymentCard = (PaymentCardView) findViewById(R.id.creditCard);

        db.getReference().child("PaymentInfo").child(userID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                paymentInfo=snapshot.getValue(PaymentInfo.class);
                if(paymentInfo!=null){
                    month.setText(paymentInfo.month);
                    month.setSelection(month.getText().length());
                    year.setText(paymentInfo.year);
                    year.setSelection(year.getText().length());
                    cardNumber.setText(paymentInfo.cardNumber);
                    cardNumber.setSelection(cardNumber.getText().length());
                    cvvNumber.setText(paymentInfo.cvv);
                    cvvNumber.setSelection(cvvNumber.getText().length());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        paymentCard.setOnPaymentCardEventListener(new PaymentCardView.OnPaymentCardEventListener() {
            @Override
            public void onCardDetailsSubmit(String month, String year, String cardNumber, String cvv) {
                if(month.isEmpty() || year.isEmpty() || cardNumber.isEmpty() || cvv.isEmpty()){
                    Toast.makeText(CardDetailsActivity.this, "Please fill all details!", Toast.LENGTH_SHORT).show();
                }else {
                    paymentInfo=new PaymentInfo(month, year, cardNumber, cvv);
                    db.getReference("PaymentInfo")
                            .child(userID)
                            .setValue(paymentInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(CardDetailsActivity.this, "Success", Toast.LENGTH_SHORT).show();
                                        finish();
                                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);  //slide from left to right
                                    } else {
                                        Toast.makeText(CardDetailsActivity.this, "Failed to add account details", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CardDetailsActivity.this, error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelClick() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);  //slide from left to right
            }
        });
    }
}
