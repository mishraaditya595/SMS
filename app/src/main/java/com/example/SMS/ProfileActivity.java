package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private Button UpdateAccountSettings;
    private EditText username, userbio;
    private CircleImageView userprofilepic;
    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private FirebaseDatabase database;
    private static final int GalleryPick=1;
    private StorageReference UserProfileImagesRef;
    private ProgressDialog LoadingBar;
    private androidx.appcompat.widget.Toolbar profileToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        //the above line of code is to hide the keyboard popups everytime one launches this activity

        mAuth=FirebaseAuth.getInstance();
        currentUserID=mAuth.getCurrentUser().getUid();
        database=FirebaseDatabase.getInstance();
        RootRef= ((FirebaseDatabase) database).getReference(); //Initialising Firebase Runtime Database
        UserProfileImagesRef= FirebaseStorage.getInstance().getReference().child("Profile Images");

        initializeFields();

        UpdateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });

        RetrieveUserInfo();

        userprofilepic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //to send user to gallery to select a picture for profile pic
                Intent galleryIntent=new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GalleryPick);
            }
        });
    }

    private void initializeFields() {
        UpdateAccountSettings=findViewById(R.id.update_settings_button); //need to use root object to call findViewById() in a fragment
        username=findViewById(R.id.set_username);
        userbio=findViewById(R.id.set_profile_status);
        userprofilepic=findViewById(R.id.set_profile_image);
        LoadingBar=new ProgressDialog(this);
        profileToolbar=findViewById(R.id.ProfileActivityToolbar);

        setSupportActionBar(profileToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Profile");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==GalleryPick && resultCode==RESULT_OK && data!=null)
        {
            Uri ImageUri=data.getData();
            // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).
                    setAspectRatio(1,1).start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode==RESULT_OK)
            {
                LoadingBar.setTitle("Profile Picture");
                LoadingBar.setMessage("Please wait while we upload our database with your profile picture.");
                LoadingBar.setCanceledOnTouchOutside(false);
                LoadingBar.show();

                Uri resultUri=result.getUri(); // resultUri contains the cropped image
                StorageReference filePath=UserProfileImagesRef.child(currentUserID + ".jpg");
                filePath.putFile(resultUri). //to store the image in the Firebase Storage
                        addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                            final String downloadUrl=task.getResult().getStorage().getDownloadUrl().toString();// it stores the link of the profile image from Firebase
                            RootRef.child("Users").child(currentUserID).child("image").setValue(downloadUrl) //to store the image to our database
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful())
                                            {
                                                Toast.makeText(ProfileActivity.this,"Image saved in our database successfully.",Toast.LENGTH_SHORT).show();
                                                LoadingBar.dismiss();
                                            }
                                            else
                                            {
                                                String errorMsg=task.getException().toString();
                                                Toast.makeText(ProfileActivity.this,"Error: "+errorMsg,Toast.LENGTH_SHORT).show();
                                                LoadingBar.dismiss();
                                            }
                                        }
                                    });
                        }
                        else
                        {
                            String errorMsg=task.getException().toString();
                            Toast.makeText(ProfileActivity.this,"Error: "+errorMsg,Toast.LENGTH_SHORT).show();
                            LoadingBar.dismiss();
                        }
                    }
                });
            }
        }
    }

    private void RetrieveUserInfo() {
        RootRef.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists() && dataSnapshot.hasChild("name") &&
                                dataSnapshot.hasChild("bio") && dataSnapshot.hasChild("image")){
                            //to retrieve info if user has both name and profile picture already set
                            String retrieveUserName=dataSnapshot.child("name").getValue().toString();
                            String retrieveUserBio=dataSnapshot.child("bio").getValue().toString();
                            String retrieveProfilePic=dataSnapshot.child("image").getValue().toString();

                            username.setText(retrieveUserName);
                            userbio.setText(retrieveUserBio);
                            Picasso.get()
                                    .load(retrieveProfilePic) //it contains the link to the profile picture
                                    .into(userprofilepic);
                        }
                        else if(dataSnapshot.exists() && dataSnapshot.hasChild("name")){
                            //to retrieve info if user has name set but no profile picture
                            String retrieveUserName=dataSnapshot.child("name").getValue().toString();
                            String retrieveUserBio=dataSnapshot.child("bio").getValue().toString();

                            username.setText(retrieveUserName);
                            userbio.setText(retrieveUserBio);
                        }
                        else {
                            // if user has nothing set
                            Toast.makeText(ProfileActivity.this,"You need to update your profile first",Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void UpdateSettings() {
        String setUsername=username.getText().toString();
        String setBio=userbio.getText().toString();
        if(setUsername.isEmpty()) {
            Toast.makeText(ProfileActivity.this, "Username cannot be left blank", Toast.LENGTH_SHORT).show();
        }
        if(setBio.isEmpty()){
            Toast.makeText(ProfileActivity.this,"Describe yourself in few words",Toast.LENGTH_SHORT);
        }
        else
        {
            HashMap<String, Object> profilemap=new HashMap<>();
            profilemap.put("uid", currentUserID);
            profilemap.put("name", setUsername);
            profilemap.put("bio", setBio);
            RootRef.child("Users").child(currentUserID).updateChildren(profilemap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                SendUserToMainActivity();
                                Toast.makeText(ProfileActivity.this,"Profile Updated",Toast.LENGTH_SHORT).show();
                            }
                            else{
                                String message=task.getException().toString(); //to get the error message
                                Toast.makeText(ProfileActivity.this,"Error: "+message+".",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

    }

    private void SendUserToMainActivity() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        startActivity(intent);
    }
}


