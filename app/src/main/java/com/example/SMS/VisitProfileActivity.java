package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class VisitProfileActivity extends AppCompatActivity {

    private String receiverUserID,current_stats,senderUID;
    private CircleImageView userProfilePic;
    private TextView userProfileName,userProfileBio;
    private Button sendMsgReqButton, declineMsgReqButton;
    private DatabaseReference UserRef, MsgReqRef, ContactsRef, NotificationRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_profile);

        mAuth=FirebaseAuth.getInstance();
        UserRef= FirebaseDatabase.getInstance().getReference().child("Users");
        MsgReqRef= FirebaseDatabase.getInstance().getReference().child("Message Request");
        ContactsRef= FirebaseDatabase.getInstance().getReference().child("Contacts");
        NotificationRef=FirebaseDatabase.getInstance().getReference().child("Notifications");



        receiverUserID=getIntent().getExtras().get("visit_user_ID").toString();//to receive the user ID passed by the Find Friends Activity through intent
        senderUID=mAuth.getCurrentUser().getUid();

        InitialiseFields();
        
        RetrieveUserInfo();
    }

    private void InitialiseFields() {
        userProfilePic=findViewById(R.id.visit_profile_image);
        userProfileName=findViewById(R.id.visit_user_name);
        userProfileBio=findViewById(R.id.visit_profile_bio);
        sendMsgReqButton=findViewById(R.id.send_message_req_button);
        declineMsgReqButton=findViewById(R.id.decline_message_req_button);
        sendMsgReqButton.setText("Send Message Request");
        current_stats="new";
    }

    private void RetrieveUserInfo() {
        UserRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("image")) //this is if the user has his/her profile picture set
                {
                    String image=dataSnapshot.child("image").getValue().toString();
                    String name=dataSnapshot.child("name").getValue().toString();
                    String bio=dataSnapshot.child("bio").getValue().toString();
                    //the above three lines are to retrieve the profile pic, name and bio from our database

                    Picasso.get().load(image).placeholder(R.drawable.profile_image).into(userProfilePic);//to set the retrieve profile picture
                    userProfileName.setText(name);
                    userProfileBio.setText(bio);

                    ManageMessageRequests();
                }
                else //this is if user does not have his/her profile picture set
                {
                    String name=dataSnapshot.child("name").getValue().toString();
                    String bio=dataSnapshot.child("bio").getValue().toString();
                    //the above two lines are to retrieve the name and bio from our database

                    userProfileName.setText(name);
                    userProfileBio.setText(bio);

                    ManageMessageRequests();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void ManageMessageRequests() {
        MsgReqRef.child(senderUID).addValueEventListener(new ValueEventListener() {
            //this is to check if the user has already sent request to that person
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(receiverUserID))
                {
                    String request_type=dataSnapshot.child(receiverUserID).child("request_type").getValue().toString();
                    if(request_type.equals("sent"))
                    {
                        //this is for the person who is sending the request
                        current_stats="request_sent";
                        sendMsgReqButton.setText("Cancel Message Request");
                    }
                    else if(request_type.equals("received"))
                    {
                        //this is for the person who is receiving the request
                        current_stats="request_received";
                        sendMsgReqButton.setText("Accept Message Request");
                        declineMsgReqButton.setVisibility(View.VISIBLE);
                        declineMsgReqButton.setEnabled(true);
                        declineMsgReqButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DeclineMessageRequest();
                            }
                        });
                    }
                }
                else
                {
                    ContactsRef.child(senderUID)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.hasChild(receiverUserID))
                                    {
                                        current_stats="friends";
                                        sendMsgReqButton.setText("Remove Contact");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(! senderUID.equals(receiverUserID))
        {
            sendMsgReqButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMsgReqButton.setEnabled(false);
                    if(current_stats.equals("new"))
                    {
                        SendMsgReq();
                    }
                    if(current_stats.equals("request_sent"))
                    {
                        CancelMsgReq();
                    }
                    if(current_stats.equals("request_received"))
                    {
                        AcceptMessageRequest();
                    }
                    if(current_stats.equals("friends"))
                    {
                        RemoveContact();
                    }
                }
            });
        }
        else //this section is if the user opens his/her own account
        {
            sendMsgReqButton.setVisibility(View.INVISIBLE);
        }
    }

    private void SendMsgReq() {
        MsgReqRef.child(senderUID).child(receiverUserID).child("request_type")
                .setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                {
                    MsgReqRef.child(receiverUserID).child(senderUID).child("request_type")
                            .setValue("received").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                            {
                                HashMap<String,String> messageNotificationMap=new HashMap<>();
                                messageNotificationMap.put("from",senderUID);
                                messageNotificationMap.put("type","request");
                                NotificationRef.child(receiverUserID).push()
                                        .setValue(messageNotificationMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful())
                                        {
                                            sendMsgReqButton.setEnabled(true);
                                            current_stats="request_sent";
                                            sendMsgReqButton.setText("Cancel Message Request");
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void CancelMsgReq() {
        //this is for the user who has mistakenly sent a message request and wants to cancel it
        MsgReqRef.child(senderUID).child(receiverUserID).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {
                            MsgReqRef.child(receiverUserID).child(senderUID).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful())
                                            {
                                                sendMsgReqButton.setEnabled(true);
                                                current_stats="new";
                                                sendMsgReqButton.setText("Send Message Request");
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void AcceptMessageRequest() {
        //this is when the user wants to accept someone's message request and start messaging
        ContactsRef.child(senderUID).child(receiverUserID).child("Contacts").setValue("Saved") //to save the profile as a contact in the msg req sender's account
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {
                            ContactsRef.child(receiverUserID).child(senderUID).child("Contacts").setValue("Saved") //to save as a contact in the msg req receiver's account
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful())
                                            {
                                                MsgReqRef.child(senderUID).child(receiverUserID).removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() { //to delete the message request from the sender as they are contacts now
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful())
                                                                {
                                                                    MsgReqRef.child(receiverUserID).child(senderUID).removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() { //to delete the message request from the receiver as they are contacts now
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    sendMsgReqButton.setEnabled(true);
                                                                                    current_stats="friends";
                                                                                    sendMsgReqButton.setText("Remove Contact");

                                                                                    declineMsgReqButton.setVisibility(View.INVISIBLE);
                                                                                    declineMsgReqButton.setEnabled(false);
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void DeclineMessageRequest() {
        //this is for the user who has received the request and wants to decline it
    }

    private void RemoveContact() {
        //this method is when the user wants to remove his/her contact
        ContactsRef.child(senderUID).child(receiverUserID).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {
                            ContactsRef.child(receiverUserID).child(senderUID).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful())
                                            {
                                                sendMsgReqButton.setEnabled(true);
                                                current_stats="new";
                                                sendMsgReqButton.setText("Send Message Request");
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}
