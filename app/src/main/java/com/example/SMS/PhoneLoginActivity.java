package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {
    private Button SendVerificationCode,SubmitButton;
    private EditText InputPhoneNo,InputVerificationCode;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        mAuth=FirebaseAuth.getInstance();

        InitializeFields();

        SendVerificationCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber=InputPhoneNo.getText().toString();
                if(phoneNumber.isEmpty())
                    Toast.makeText(PhoneLoginActivity.this,"Phone number cannot be left blank",Toast.LENGTH_SHORT).show();
                else
                {
                    loadingBar.setTitle("Phone Number Verification");
                    loadingBar.setMessage("Please wait while we are authenticating your input.");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            phoneNumber,        // Phone number to verify
                            60,                 // Timeout duration
                            TimeUnit.SECONDS,   // Unit of timeout
                            PhoneLoginActivity.this,               // Activity (for callback binding)
                            callbacks);        // OnVerificationStateChangedCallbacks
                }
            }
        });

        SubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendVerificationCode.setVisibility(View.INVISIBLE);
                InputPhoneNo.setVisibility(View.INVISIBLE);

                String verificationCode=InputVerificationCode.getText().toString();
                if(verificationCode.isEmpty())
                    Toast.makeText(PhoneLoginActivity.this,"Verification Code cannot be left blank",Toast.LENGTH_SHORT).show();
                else{
                    loadingBar.setTitle("Code Verification");
                    loadingBar.setMessage("Please wait while we are authenticating your input.");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });

        callbacks=new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this,"Invalid Phone Number!",Toast.LENGTH_SHORT).show();
                SendVerificationCode.setVisibility(View.VISIBLE);
                InputPhoneNo.setVisibility(View.VISIBLE);
                SubmitButton.setVisibility(View.INVISIBLE);
                InputVerificationCode.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this,"Verification Code has been sent.",Toast.LENGTH_SHORT).show();

                SendVerificationCode.setVisibility(View.INVISIBLE);
                InputPhoneNo.setVisibility(View.INVISIBLE);
                SubmitButton.setVisibility(View.VISIBLE);
                InputVerificationCode.setVisibility(View.VISIBLE);
            }
        };
    }

    private void InitializeFields() {
        SendVerificationCode=findViewById(R.id.send_verification_code);
        SubmitButton=findViewById(R.id.submit_button);
        InputPhoneNo=findViewById(R.id.phone_number_input);
        InputVerificationCode=findViewById(R.id.verification_code_input);
        loadingBar=new ProgressDialog(this);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            loadingBar.dismiss();
                            Toast.makeText(PhoneLoginActivity.this,"Registration Successful!",Toast.LENGTH_SHORT).show();
                            SendUserToMainActivity();
                        }
                        else {
                            // Sign in failed, display a message and update the UI
                            String errorMsg=task.getException().toString();
                            Toast.makeText(PhoneLoginActivity.this,"Error: "+errorMsg,Toast.LENGTH_SHORT).show();
                                // The verification code entered was invalid
                        }
                    }
                });

    }

    private void SendUserToMainActivity() {
        Intent intent=new Intent(PhoneLoginActivity.this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}
