package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

public class GroupChatActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private ImageButton SendMsgButton;
    private EditText usermsgInput;
    private ScrollView mScrollView;
    private TextView displayTextMsg;
    private String currentGroupName,currentUID,currentUserName,currentDate,currentTime;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef,groupNameRef,GroupMsgKeyRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        //the above line of code is to hide the keyboard popups everytime one launches this activity

        currentGroupName=getIntent().getExtras().get("Group Name").toString(); //to get the group name that user tapped on in Groups Fragment

        mAuth=FirebaseAuth.getInstance();
        currentUID=mAuth.getCurrentUser().getUid(); //to get the UID of the user
        usersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        groupNameRef=FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupName);

        InitialiseFields();
        getUserInfo();
        SendMsgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendMsgToDatabase();
                usermsgInput.setText(""); //to set edit text field to blank

                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                //to reach the buttom of the scroll view so that we can always see the latest msg without the need to scroll
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        groupNameRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(dataSnapshot.exists()){
                    DisplayMessages(dataSnapshot);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(dataSnapshot.exists()){
                    DisplayMessages(dataSnapshot);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void InitialiseFields() {
        mToolbar=findViewById(R.id.group_chat_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(currentGroupName);
        //the above lines are to set the Action Bar
        SendMsgButton=findViewById(R.id.send_msg_button);
        usermsgInput=findViewById(R.id.input_groupmsg);
        displayTextMsg=findViewById(R.id.group_chat_text_display);
        mScrollView=findViewById(R.id.groupchat_scrollview);
    }

    private void DisplayMessages(DataSnapshot dataSnapshot) {
        //to display all the existing messages in the group
        Iterator iterator=dataSnapshot.getChildren().iterator();
        while(iterator.hasNext()) //this condition is to check if there are more messages in our database or not
        {
            String chatDate=(String)((DataSnapshot)iterator.next()).getValue();
            String chatMessage=(String)((DataSnapshot)iterator.next()).getValue();
            String chatName=(String)((DataSnapshot)iterator.next()).getValue();
            String chatTime=(String)((DataSnapshot)iterator.next()).getValue();
            displayTextMsg.append(chatName+":\n"+chatMessage+"\n"+chatTime+"   "+chatDate+"\n \n \n");

            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            //to reach the buttom of the scroll view so that we can always see the latest msg without the need to scroll
        }
    }

    private void getUserInfo() {
        usersRef.child(currentUID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    //to check if the current UID exists
                    currentUserName=dataSnapshot.child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendMsgToDatabase() {
        String msg=usermsgInput.getText().toString();
        String msgKey=groupNameRef.push().getKey(); //to generate a key for each message
        if(msg.isEmpty())
            Toast.makeText(this,"Empty message cannot be sent",Toast.LENGTH_SHORT).show();
        else
        {
            Calendar calForDate=Calendar.getInstance();
            SimpleDateFormat currentDateFormat=new SimpleDateFormat("MMM dd, yyyy"); //to get the date when msg is sent
            currentDate=currentDateFormat.format(calForDate.getTime());

            Calendar calForTime=Calendar.getInstance();
            SimpleDateFormat currentTimeFormat=new SimpleDateFormat("hh:mm a"); //to get the time when msg is sent
            currentTime=currentTimeFormat.format(calForTime.getTime());

            HashMap<String,Object> groupMsgKey=new HashMap<>();
            groupNameRef.updateChildren(groupMsgKey);
            GroupMsgKeyRef=groupNameRef.child(msgKey);

            HashMap<String,Object> msgInfoMap=new HashMap<>();
            msgInfoMap.put("name",currentUserName);
            msgInfoMap.put("message",msg);
            msgInfoMap.put("date",currentDate);
            msgInfoMap.put("time",currentTime);
            GroupMsgKeyRef.updateChildren(msgInfoMap);

        }
    }


}
