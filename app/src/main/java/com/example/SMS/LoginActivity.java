package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private Button LoginButton, PhoneLoginButton;
    private EditText UserEmail, UserPassword;
    private TextView NeedNewAccount, ForgetPassword;
    private FirebaseAuth mAuth;
    private ProgressDialog progressBar;
    private DatabaseReference UsersRef;


    public LoginActivity(){
        //zero argument constructor to fix the app crash caused
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        //the above line of code is to hide the keyboard popups everytime one launches this activity

        mAuth=FirebaseAuth.getInstance();
        UsersRef= FirebaseDatabase.getInstance().getReference().child("Users");


        InitializeFields(); //method to initialize Buttons, texts etc.
        NeedNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToRegisterActivity(); //to redirect user for registration
            }
        });
        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AllowUserToLogin();
            }
        });

        PhoneLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToPhoneLoginActivity();
            }
        });
    }

    private void InitializeFields() {
        LoginButton=findViewById(R.id.login_button);
        PhoneLoginButton=findViewById(R.id.phone_login_button);
        UserEmail=findViewById(R.id.login_email);
        UserPassword=findViewById(R.id.login_password);
        NeedNewAccount=findViewById(R.id.need_new_account_link);
        ForgetPassword=findViewById(R.id.forget_password_link);
        progressBar=new ProgressDialog(this);
    }

    private void AllowUserToLogin() {
        String email=UserEmail.getText().toString(); //to get the text from user input
        String password=UserPassword.getText().toString();
        if(email.isEmpty())
            Toast.makeText(this,"Email field cannot be left empty.",Toast.LENGTH_SHORT).show();
        if(password.isEmpty())
            Toast.makeText(this,"Password field cannot be left empty.",Toast.LENGTH_SHORT).show();
        //the above lines are to display a small duration pop up that email and password field cannot be left empty
        else{
            progressBar.setTitle("Signing In.");
            progressBar.setMessage("Please wait while we verify your credentials with our database.");
            progressBar.setCanceledOnTouchOutside(true); //so that the progress bar does not close until account is created
            progressBar.show();

            //to authenticate user credentials with firebase
            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        String currentUID=mAuth.getCurrentUser().getUid();
                        String deviceToken=FirebaseInstanceId.getInstance().getToken();
                        UsersRef.child(currentUID).child("device_token").setValue(deviceToken)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful())
                                        {
                                            SendUserToMainActivity();
                                            Toast.makeText(LoginActivity.this,"Login Successful",Toast.LENGTH_SHORT).show();
                                            progressBar.dismiss();
                                        }
                                    }
                                });
                    }
                    else {
                            String message=task.getException().toString(); //to get the error message
                            Toast.makeText(LoginActivity.this,"Error: "+message+".",Toast.LENGTH_SHORT).show();
                            progressBar.dismiss();
                        }
                    }
            });
        }
    }

    private void SendUserToMainActivity() {
        //to redirect user to homescreen i.e. MainActivity
            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainIntent);
            finish();
    }

    private void SendUserToRegisterActivity() {
        //to redirect user to homescreen i.e. MainActivity
        Intent registerIntent=new Intent(LoginActivity.this,RegisterActivity.class);
        startActivity(registerIntent);
    }

    private void SendUserToPhoneLoginActivity() {
        Intent Intent=new Intent(LoginActivity.this,PhoneLoginActivity.class);
        startActivity(Intent);
    }
}
