package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {


    private Toolbar mToolbar;
    private ViewPager myViewPager;
    private TabLayout myTabLayout;
    private TabsAccessorAdaptor myTabsAccessorAdapter;

    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private Button signoutButton, createGroup;
    private ImageButton profileButton;
    private String currentUID;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth=FirebaseAuth.getInstance();

        RootRef= FirebaseDatabase.getInstance().getReference();

        mToolbar=findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
         //To set the name of the app on the homescreen

        myViewPager=findViewById(R.id.main_tabs_pager);
        myTabsAccessorAdapter=new TabsAccessorAdaptor(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsAccessorAdapter); //to have different tabs of chat,groups

        myTabLayout=findViewById(R.id.main_tabs);
        myTabLayout.setupWithViewPager(myViewPager); //to have different tab page for chats,groups

        profileButton=findViewById(R.id.Profile);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToProfileActivity();
            }
        });
    }

    public void setSupportActionBar(Toolbar mToolbar) {
    }

    @Override
    protected void onStart() {
        super.onStart();
        //to check if the user is logged in or not. null signifies not logged in
        FirebaseUser currentUser=mAuth.getCurrentUser();
        if(currentUser==null){
            SendUserToLoginActivity();
        }
        else
        {
            updateUserStatus("Online");
            VerifyUserExistence();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //onStop method works when the app is stopped i.e. minimised or closed.
        FirebaseUser currentUser=mAuth.getCurrentUser();
        if(currentUser!=null)
        {
            updateUserStatus("Offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //onDestroy() works when the app encounters any error and stops working
        FirebaseUser currentUser=mAuth.getCurrentUser();
        if(currentUser!=null)
        {
            updateUserStatus("Offline");
        }
    }

    private void VerifyUserExistence() {
        String currentUserID=mAuth.getCurrentUser().getUid(); //to get the user ID of the current user
        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("name").exists()) {
                    Toast.makeText(MainActivity.this, "Welcome", Toast.LENGTH_SHORT);
                } else {
                    SendUserToProfileActivity();
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendUserToProfileActivity() {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void SendUserToLoginActivity() { //to send user for login
        Intent loginIntent=new Intent(MainActivity.this,LoginActivity.class);
        startActivity(loginIntent);
    }

    private void SignOutUser() {
        mAuth.signOut();
        SendUserToLoginActivity();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //to create the overlay menu
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item) {

        super.onOptionsItemSelected(item);
        int choice=item.getItemId();//to find which item was choosen
        if(choice==R.id.signout_option){
            updateUserStatus("Offline");
            mAuth.signOut();
            SendUserToLoginActivity();
        }
        return true;
    }

    private void updateUserStatus(String state){
        //this method is for getting information of user's last seen status
        String saveCurrentTime, saveCurrentDate;
        Calendar calendar=Calendar.getInstance();
        SimpleDateFormat currentDate=new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate=currentDate.format(Calendar.getInstance().getTime());

        SimpleDateFormat currentTime=new SimpleDateFormat("hh:mm a");
        saveCurrentTime=currentTime.format(Calendar.getInstance().getTime());

        HashMap<String, Object> onlineStateMap=new HashMap<>();
        onlineStateMap.put("time",saveCurrentTime);
        onlineStateMap.put("date",saveCurrentDate);
        onlineStateMap.put("state",state);

        currentUID=mAuth.getCurrentUser().getUid();
        RootRef.child("Users").child(currentUID).child("userState")
                .updateChildren(onlineStateMap);
    }




}
