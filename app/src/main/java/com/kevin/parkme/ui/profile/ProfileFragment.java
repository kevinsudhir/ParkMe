package com.kevin.parkme.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.kevin.parkme.R;
import com.kevin.parkme.RegisterLogin.LoginActivity;
import com.kevin.parkme.classes.User;
import com.kevin.parkme.utils.AppConstants;
import com.kevin.parkme.utils.BasicUtils;
import com.kevin.parkme.utils.network.CardPayment;
import com.kevin.parkme.ui.profile.CardDetailsActivity;
import com.kevin.parkme.utils.notifications.AlarmUtils;
import com.kevin.parkme.utils.notifications.NotificationReceiver;
import com.kevin.parkme.utils.services.MyParkingService;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    LinearLayout personalDetailsBtn,changePasswordBtn,aboutMeBtn,logoutBtn,accountDetailsBtn;
    TextView nameText;
    User userObj;
    AppConstants globalClass;
    FirebaseAuth auth;
    BasicUtils utils=new BasicUtils();

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        initComponents(root);
        attachListeners();

        if(!utils.isNetworkAvailable(getActivity().getApplication())){
            Toast.makeText(getActivity(), "No Network Available!", Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    private void initComponents(View root) {
        globalClass=(AppConstants)getActivity().getApplicationContext();
        userObj=globalClass.getUserObj();
        logoutBtn = root.findViewById(R.id.logoutBtn);
        nameText = root.findViewById(R.id.nameText);
        personalDetailsBtn = root.findViewById(R.id.personalDetailsBtn);
        changePasswordBtn = root.findViewById(R.id.changePasswordBtn);
        aboutMeBtn = root.findViewById(R.id.aboutMeBtn);
        accountDetailsBtn = root.findViewById(R.id.accountDetailsBtn);
        nameText.setText(userObj.name);
    }

    private void attachListeners() {
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                getActivity().stopService(new Intent(getActivity(), MyParkingService.class));
                AlarmUtils.cancelAllAlarms(getActivity(),new Intent(getActivity(), NotificationReceiver.class));
                Toast.makeText(getActivity(), "Logout Success", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getActivity(), LoginActivity.class));
                getActivity().finish();
            }
        });

        personalDetailsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), PersonalDetailsActivity.class));
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        accountDetailsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(userObj.userType==2)
                {
                    startActivity(new Intent(getActivity(), OwnerAccountActivity.class));
                    getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
                else
                {
                    startActivity(new Intent(getActivity(), CardDetailsActivity.class));
                    getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }

            }
        });

        changePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), ChangePasswordActivity.class));
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        aboutMeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AboutMeActivity.class));
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

}