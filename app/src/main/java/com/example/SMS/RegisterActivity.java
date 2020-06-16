package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class RegisterActivity extends AppCompatActivity {
    private Button CreateAccountButton;
    private EditText UserEmail, UserPassword;
    private TextView AlreadyHaveAccount;
    private FirebaseAuth mAuth;
    private ProgressDialog progressBar;
    private DatabaseReference RootRef;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth=FirebaseAuth.getInstance(); //initialising Firebase Authentication
        database=FirebaseDatabase.getInstance();
        RootRef= database.getReference(); //Initialising Firebase Runtime Database

        InitializeFields(); //to initialize the buttons and texts

        AlreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToLoginActivity();
            }
        });

        CreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    CreateNewAccount();
            }
        });
    }

    @SuppressLint("ShowToast")
    private void CreateNewAccount(){
        String email=UserEmail.getText().toString(); //to get the text from user input
        String password=UserPassword.getText().toString();
        if(email.isEmpty())
            Toast.makeText(RegisterActivity.this,"Email field cannot be left empty.",Toast.LENGTH_SHORT).show();
        if(password.isEmpty())
            Toast.makeText(RegisterActivity.this,"Password field cannot be left empty.",Toast.LENGTH_SHORT).show();
        //the above lines are to display a small duration pop up that email and password field cannot be left empty
        else {
            progressBar.setTitle("New account creation in progress.");
            progressBar.setMessage("Please wait while we update our database with your details.");
            progressBar.setCanceledOnTouchOutside(true); //so that the progress bar does not close until account is created
            progressBar.show();
            //To create a new user account based on entered user email and password
            mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        String deviceToken= FirebaseInstanceId.getInstance().getToken();

                        String currentUserID=mAuth.getCurrentUser().getUid();
                        RootRef.child("Users").child(currentUserID).setValue(currentUserID);
                        //the above two lines are to store UID of the registered user in our firebase database

                        RootRef.child("Users").child(currentUserID).child("device_token")
                                .setValue(deviceToken);
                        SendUserToMainActivity();
                        Toast.makeText(RegisterActivity.this, "Account Created.", Toast.LENGTH_SHORT).show();
                        progressBar.dismiss();
                    }
                    else{
                        String message=task.getException().toString(); //to get the error message
                        Toast.makeText(RegisterActivity.this,"Error: "+message+".",Toast.LENGTH_SHORT).show();
                        progressBar.dismiss();
                    }

                }

                private void SendUserToMainActivity() {
                    Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainIntent);
                    finish();
                }

            });
        }

    }

    private void InitializeFields() {
        CreateAccountButton = findViewById(R.id.register_button);
        UserEmail = findViewById(R.id.register_email);
        UserPassword = findViewById(R.id.register_password);
        AlreadyHaveAccount = findViewById(R.id.already_have_account_link);
        progressBar=new ProgressDialog(this);
    }

    private void SendUserToLoginActivity() {
        //to redirect user to homescreen i.e. MainActivity
        Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }
}
