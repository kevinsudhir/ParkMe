package com.kevin.parkme.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.kevin.parkme.NormalUser.MainNormalActivity;
import com.kevin.parkme.OwnerUser.MainOwnerActivity;
import com.kevin.parkme.R;
import com.kevin.parkme.classes.BookedSlots;
import com.kevin.parkme.classes.ParkingArea;
import com.kevin.parkme.classes.User;
import com.kevin.parkme.utils.AppConstants;
import com.kevin.parkme.utils.BasicUtils;
import com.kevin.parkme.utils.notifications.AlarmUtils;
import com.kevin.parkme.utils.notifications.NotificationHelper;
import com.kevin.parkme.utils.notifications.NotificationReceiver;
import com.kevin.parkme.utils.pdf.InvoiceGenerator;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stripe.android.Stripe;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class BookingDetailsActivity extends AppCompatActivity implements View.OnClickListener {
    TextView placeText,wheelerText,amountText,checkoutDateText,checkoutTimeText,endDateText,endTimeText,startDateText,startTimeText,numberPlateSpinner;
    FloatingActionButton checkoutBtn,payBtn, cancelBtn;

    SupportMapFragment supportMapFragment;
    GoogleMap gMap;
    LatLng globalLatLng=null;
    MarkerOptions options;

    BookedSlots bookingSlot;
    User userObj;
    NotificationHelper mNotificationHelper;
    AppConstants globalClass;
    BasicUtils utils=new BasicUtils();

    FirebaseAuth auth;
    FirebaseDatabase db;

    private Stripe stripe;

    String UUID;
    Boolean run_once=false;
    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.INTERNET,
    };

    ParkingArea parkingArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        initComponents();
        attachListeners();

        if(!utils.isNetworkAvailable(getApplication())){
            Toast.makeText(BookingDetailsActivity.this, "No Network Available!", Toast.LENGTH_SHORT).show();
        }

        askCameraFilePermission();
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
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        Bundle bundle = getIntent().getExtras();
        UUID=bundle.getString("UUID");

        getSupportActionBar().setTitle(UUID);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        globalClass=(AppConstants)getApplicationContext();
        userObj=globalClass.getUserObj();

        placeText = findViewById(R.id.placeText);
        numberPlateSpinner = findViewById(R.id.vehicleSelect);
        endDateText = findViewById(R.id.endDateText);
        endTimeText = findViewById(R.id.endTimeText);
        startDateText = findViewById(R.id.startDateText);
        startTimeText = findViewById(R.id.startTimeText);
        checkoutDateText = findViewById(R.id.checkoutDateText);
        checkoutTimeText = findViewById(R.id.checkoutTimeText);
        checkoutBtn = findViewById(R.id.checkoutBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
        payBtn = findViewById(R.id.payBtn);
        wheelerText = findViewById(R.id.wheelerText);
        amountText = findViewById(R.id.amountText);
        mNotificationHelper=new NotificationHelper(this);
    }

    private void attachListeners() {
        db.getReference().child("BookedSlots").child(UUID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingSlot=snapshot.getValue(BookedSlots.class);
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("hh:mm a");
                startTimeText.setText(simpleDateFormat.format(bookingSlot.startTime));
                endTimeText.setText(simpleDateFormat.format(bookingSlot.endTime));
                checkoutTimeText.setText(simpleDateFormat.format(bookingSlot.checkoutTime));
                simpleDateFormat=new SimpleDateFormat("dd-MM-yyyy");
                startDateText.setText(simpleDateFormat.format(bookingSlot.startTime));
                endDateText.setText(simpleDateFormat.format(bookingSlot.endTime));
                checkoutDateText.setText(simpleDateFormat.format(bookingSlot.checkoutTime));
                numberPlateSpinner.setText(bookingSlot.numberPlate);
                wheelerText.setText(String.valueOf(bookingSlot.wheelerType));
                if(bookingSlot.hasPaid==0){
                    amountText.setText(String.valueOf(bookingSlot.amount).concat(" (Not Paid)"));
                }else{
                    amountText.setText(String.valueOf(bookingSlot.amount).concat(" (Paid)"));
                }
                
                updatePayCheckoutUI();

                findViewById(R.id.openInvoicePdf).setOnClickListener(BookingDetailsActivity.this);
                findViewById(R.id.shareInvoicePdf).setOnClickListener(BookingDetailsActivity.this);

                if(!run_once){
                    run_once=true;
                    attachParkingListeners();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        db.getReference().child("BookedSlots").child(UUID).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if(snapshot.getKey().equals("hasPaid")){
                    try{
                        bookingSlot.hasPaid=snapshot.getValue(int.class);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    Log.e(String.valueOf(BookingDetailsActivity.this.getClass()),"Fetched updated BookedSlots:"+  String.valueOf(snapshot.getKey())+snapshot.getValue(int.class));
                }
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(utils.isNetworkAvailable(getApplication())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(BookingDetailsActivity.this);
                    builder.setCancelable(true);
                    builder.setTitle("Confirm Cancel Booking");
                    if(userObj.userType==2)
                        builder.setMessage("Confirm to cancel the user booking?");
                    else
                        builder.setMessage("Confirm cancel for this area?");
                    builder.setPositiveButton("Confirm",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancelBookingData();
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else{
                    Toast.makeText(BookingDetailsActivity.this, "No Network Available!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        checkoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(utils.isNetworkAvailable(getApplication())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(BookingDetailsActivity.this);
                    builder.setCancelable(true);
                    builder.setTitle("Confirm Checkout");
                    if(userObj.userType==2)
                        builder.setMessage("Confirm to checkout the user vehicle?");
                    else
                        builder.setMessage("Confirm checkout for this area?");
                    builder.setPositiveButton("Confirm",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkoutData();
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else{
                    Toast.makeText(BookingDetailsActivity.this, "No Network Available!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(utils.isNetworkAvailable(getApplication())){
                    if(parkingArea.availableSlots>0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(BookingDetailsActivity.this);
                        builder.setCancelable(true);
                        builder.setTitle("Confirm Payment");
                        if(userObj.userType==2)
                            builder.setMessage("Confirm cash payment by user?");
                        else
                            builder.setMessage("Confirm to proceed with payment?");
                        builder.setPositiveButton("Confirm",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        parkingArea.allocateSpace();
                                        db.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                                        String note ="Payment for ".concat(bookingSlot.placeID).concat(" and number ").concat(bookingSlot.numberPlate);
                                        if(userObj.userType==2){
                                            bookingSlot.hasPaid=1;
                                            payData();
                                        }else {
                                        }
                                    }
                                });
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }else{
                        Toast.makeText(BookingDetailsActivity.this,"Failed! Slots are full.",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(BookingDetailsActivity.this, "No Network Available!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updatePayCheckoutUI() {
        if(bookingSlot.hasPaid==1){
            checkoutBtn.setVisibility(View.VISIBLE);
            payBtn.setVisibility(View.GONE);
            cancelBtn.setVisibility(View.VISIBLE);
        }else if(bookingSlot.hasPaid==0){
            payBtn.setVisibility(View.VISIBLE);
            checkoutBtn.setVisibility(View.GONE);
            cancelBtn.setVisibility(View.VISIBLE);
        }
        if(bookingSlot.checkout!=0){
            checkoutBtn.setVisibility(View.GONE);
            payBtn.setVisibility(View.GONE);
            cancelBtn.setVisibility(View.GONE);
        }
    }

    private void attachParkingListeners(){
        db.getReference().child("ParkingAreas").child(bookingSlot.placeID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        if(snapshot.getKey().equals("availableSlots") || snapshot.getKey().equals("occupiedSlots") || snapshot.getKey().equals("totalSlots")){
                            try{
                                parkingArea.setData(snapshot.getKey(),snapshot.getValue(int.class));
                                updatePayCheckoutUI();
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                            Log.e(String.valueOf(BookingDetailsActivity.this.getClass()),"Fetched updated parking Area:"+  String.valueOf(snapshot.getKey())+snapshot.getValue(int.class));
                        }
                    }
                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        db.getReference().child("ParkingAreas").child(bookingSlot.placeID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ParkingArea parkingArea = snapshot.getValue(ParkingArea.class);
                        setAddValues(parkingArea);
                        Log.e(String.valueOf(BookingDetailsActivity.this.getClass()),"Fetched parking Area:"+ String.valueOf(snapshot.getKey()));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }


    @Override
    public void onClick(View view) {
        InvoiceGenerator invoiceGenerator=new InvoiceGenerator();
        switch(view.getId()){
            case R.id.openInvoicePdf:
                Toast.makeText(BookingDetailsActivity.this, "Open File", Toast.LENGTH_SHORT).show();
                //invoiceGenerator.downloadFile(bookingSlot.userID,UUID,BookingDetailsActivity.this,getApplication());
                invoiceGenerator.openFile(BookingDetailsActivity.this);
                break;
            case R.id.shareInvoicePdf:
                Toast.makeText(BookingDetailsActivity.this, "Share File", Toast.LENGTH_SHORT).show();
                invoiceGenerator.downloadFile(bookingSlot.userID,UUID,BookingDetailsActivity.this,getApplication());
                invoiceGenerator.shareFile(BookingDetailsActivity.this);
                break;
        }
    }

    private void setAddValues(ParkingArea parkingArea) {
        this.parkingArea=parkingArea;
        placeText.setText(parkingArea.name);
        globalLatLng=new LatLng(parkingArea.latitude,parkingArea.longitude);
        options=new MarkerOptions().position(globalLatLng)
                .title(parkingArea.name);
        supportMapFragment=(SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                gMap=googleMap;
                gMap.clear();
                gMap.addMarker(options);
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(globalLatLng,30));
            }
        });
    }

    private void payData() {
        bookingSlot.notificationID=Math.abs((int) Calendar.getInstance().getTimeInMillis());
        bookingSlot.slotNo=parkingArea.allocateSlot(bookingSlot.numberPlate);
        db.getReference("BookedSlots").child(UUID).setValue(bookingSlot).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    db.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                    Toast.makeText(BookingDetailsActivity.this,"Success",Toast.LENGTH_SHORT).show();
                    File file = new File(BookingDetailsActivity.this.getExternalCacheDir(), File.separator + "invoice.pdf");
                    InvoiceGenerator invoiceGenerator=new InvoiceGenerator(bookingSlot,parkingArea,UUID,userObj,file);
                    invoiceGenerator.create();
                    invoiceGenerator.uploadFile(BookingDetailsActivity.this,getApplication());
                    Intent intent;
                    if(userObj.userType==3)
                        intent = new Intent(BookingDetailsActivity.this, MainOwnerActivity.class);
                    else
                        intent = new Intent(BookingDetailsActivity.this, MainNormalActivity.class);
                    intent.putExtra("FRAGMENT_NO", 0);
                    startActivity(intent);
                    finish();
                }else{
                    Toast.makeText(BookingDetailsActivity.this,"Failed",Toast.LENGTH_SHORT).show();
                    parkingArea.deallocateSpace();
                    parkingArea.deallocateSlot(bookingSlot.slotNo);
                    db.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                }
            }
        });
    }

    private void cancelBookingData()
    {
        Calendar calendar=new GregorianCalendar();
        bookingSlot.checkout=1;
        parkingArea.deallocateSpace();
        bookingSlot.checkoutTime=calendar.getTime();
        parkingArea.deallocateSlot(bookingSlot.slotNo);

        db.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    db.getReference("BookedSlots").child(UUID).setValue(bookingSlot).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(BookingDetailsActivity.this,"Booking Cancelled",Toast.LENGTH_SHORT).show();
                                Intent notifyIntent = new Intent(getApplicationContext(), NotificationReceiver.class);
                                AlarmUtils.cancelAlarm(BookingDetailsActivity.this,notifyIntent,bookingSlot.notificationID);
                                Intent intent;
                                if(userObj.userType==3)
                                    intent = new Intent(BookingDetailsActivity.this, MainNormalActivity.class);
                                else
                                    intent = new Intent(BookingDetailsActivity.this, MainOwnerActivity.class);
                                intent.putExtra("FRAGMENT_NO", 0);
                                startActivity(intent);
                                finish();
                            }else{
                                Toast.makeText(BookingDetailsActivity.this,"Failed",Toast.LENGTH_SHORT).show();
                                parkingArea.allocateSpace();
                                db.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                            }
                        }
                    });
                }
            }
        });
    }

    private void checkoutData() {
        Calendar calendar=new GregorianCalendar();
        bookingSlot.checkout=1;
        bookingSlot.checkoutTime=calendar.getTime();
        parkingArea.deallocateSpace();
        parkingArea.deallocateSlot(bookingSlot.slotNo);
        db.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        db.getReference("BookedSlots").child(UUID).setValue(bookingSlot).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(BookingDetailsActivity.this,"Success",Toast.LENGTH_SHORT).show();
                                    Intent notifyIntent = new Intent(getApplicationContext(), NotificationReceiver.class);
                                    AlarmUtils.cancelAlarm(BookingDetailsActivity.this,notifyIntent,bookingSlot.notificationID);
                                    Intent intent;
                                    if(userObj.userType==3)
                                        intent = new Intent(BookingDetailsActivity.this, MainNormalActivity.class);
                                    else
                                        intent = new Intent(BookingDetailsActivity.this, MainOwnerActivity.class);
                                    intent.putExtra("FRAGMENT_NO", 0);
                                    startActivity(intent);
                                    finish();
                                }else{
                                    Toast.makeText(BookingDetailsActivity.this,"Failed",Toast.LENGTH_SHORT).show();
                                    parkingArea.allocateSpace();
                                    db.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                                }
                            }
                        });
                    }
                }
            });
    }


    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void askCameraFilePermission() {
        if (!hasPermissions(BookingDetailsActivity.this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(BookingDetailsActivity.this, PERMISSIONS, AppConstants.SCAN_PERMISSION_ALL);
        }else{
//            openCamera();
        }
    }
}