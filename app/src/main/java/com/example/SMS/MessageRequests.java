package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageRequests extends AppCompatActivity {
    private RecyclerView myRequestsList;
    private DatabaseReference MessageRequestsRef,UsersRef,ContactsRef;
    private FirebaseAuth mAuth;
    private String currentUID;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_requests);

        MessageRequestsRef= FirebaseDatabase.getInstance().getReference().child("Message Request");
        mAuth=FirebaseAuth.getInstance();
        currentUID=mAuth.getCurrentUser().getUid();
        UsersRef=FirebaseDatabase.getInstance().getReference().child("Users");
        ContactsRef=FirebaseDatabase.getInstance().getReference().child("Contacts");

        InitialiseFields();
    }

    private void InitialiseFields() {
        myRequestsList=findViewById(R.id.message_requests_list);
        myRequestsList.setLayoutManager(new LinearLayoutManager(MessageRequests.this));

        mToolbar=findViewById(R.id.FindFriendstoolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Message Requests");
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options
                =new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(MessageRequestsRef.child(currentUID), Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,RequestsViewHolder> adapter
                =new FirebaseRecyclerAdapter<Contacts, RequestsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestsViewHolder holder, int position, @NonNull Contacts model) {
                holder.itemView.findViewById(R.id.request_accept_button).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.request_cancel_button).setVisibility(View.VISIBLE);

                final String list_user_id=getRef(position).getKey(); //to get the UID of the user who have sent the message request
                DatabaseReference getReqTypeRef=getRef(position).child("request_type").getRef(); //to get thr type of request like if it is sent, received or accepted

                getReqTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            String requestType=dataSnapshot.getValue().toString();
                            if(requestType.equals("received"))
                            {
                                //if the request is received, we can retrieve his info and display it
                                UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.hasChild("image"))
                                        {
                                            //this is for those users who have their profile pic set

                                            final String requestUserImage=dataSnapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestUserImage).placeholder(R.drawable.profile_image).into(holder.profilepic);
                                        }
                                        final String requestUserName=dataSnapshot.child("name").getValue().toString();
                                        final String requestUserBio=dataSnapshot.child("bio").getValue().toString();

                                        holder.username.setText(requestUserName);
                                        holder.userbio.setText(requestUserBio);


                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[]=new CharSequence[]{"Accept","Cancel"};
                                                AlertDialog.Builder builder=new AlertDialog.Builder(MessageRequests.this);
                                                builder.setTitle(requestUserName+" has sent you a message request");
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int i) {
                                                        if(i==0)
                                                        {
                                                         // 0 indicates user has selected the first item from the char sequence i.e. Accept
                                                            ContactsRef.child(currentUID).child(list_user_id) //list_user_id contains the sender's UID
                                                            .child("Contacts").setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful())
                                                                    {
                                                                        ContactsRef.child(list_user_id).child(currentUID)
                                                                                .child("Contacts").setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            //used the same set of codes for the 2nd time to save the contact in both senders and receivers contact list
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if(task.isSuccessful())
                                                                                {
                                                                                    MessageRequestsRef.child(currentUID).child(list_user_id)
                                                                                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                        @Override
                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                            if(task.isSuccessful())
                                                                                            {
                                                                                                MessageRequestsRef.child(list_user_id).child(currentUID)
                                                                                                        .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                    @Override
                                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                                        if(task.isSuccessful())
                                                                                                            Toast.makeText(MessageRequests.this,"Contact added successfully.",Toast.LENGTH_SHORT).show();
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
                                                        if(i==1)
                                                        {
                                                            // 1 indicates user has selected the second item from the char sequence i.e. Deny
                                                            MessageRequestsRef.child(currentUID).child(list_user_id)
                                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful())
                                                                    {
                                                                        MessageRequestsRef.child(list_user_id).child(currentUID)
                                                                                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if(task.isSuccessful())
                                                                                    Toast.makeText(MessageRequests.this,"Request deleted successfully.",Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        }

                                                    }
                                                });
                                                builder.show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                            else if(requestType.equals("sent"))
                            {
                                Button request_sent_btn=holder.itemView.findViewById(R.id.request_accept_button);
                                request_sent_btn.setText("Request Sent");
                                holder.itemView.findViewById(R.id.request_cancel_button).setVisibility(View.INVISIBLE);

                                UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.hasChild("image"))
                                        {
                                            //this is for those users who have their profile pic set

                                            final String requestUserImage=dataSnapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestUserImage).placeholder(R.drawable.profile_image).into(holder.profilepic);
                                        }
                                        final String requestUserName=dataSnapshot.child("name").getValue().toString();
                                        final String requestUserBio=dataSnapshot.child("bio").getValue().toString();

                                        holder.username.setText(requestUserName);
                                        holder.userbio.setText(requestUserBio);


                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[]=new CharSequence[]{"Cancel Message Request"};
                                                AlertDialog.Builder builder=new AlertDialog.Builder(MessageRequests.this);
                                                builder.setTitle("You have sent a message request to "+requestUserName);
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int i) {
                                                        if(i==0)
                                                        {
                                                            // 1 indicates user has selected the second item from the char sequence i.e. Deny
                                                            MessageRequestsRef.child(currentUID).child(list_user_id)
                                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful())
                                                                    {
                                                                        MessageRequestsRef.child(list_user_id).child(currentUID)
                                                                                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if(task.isSuccessful())
                                                                                    Toast.makeText(MessageRequests.this,"Request cancelled.",Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        }

                                                    }
                                                });
                                                builder.show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                //this contains from where we are getting the layout content for our Recycler View
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout,parent,false);
                RequestsViewHolder holder=new RequestsViewHolder(view);
                return holder;
            }
        };

        myRequestsList.setAdapter(adapter);
        adapter.startListening();
    }

    class RequestsViewHolder extends RecyclerView.ViewHolder
    {
        TextView username, userbio;
        CircleImageView profilepic;
        Button acceptButton, cancelButton;

        public RequestsViewHolder(@NonNull View itemView) {
            super(itemView);
            username=itemView.findViewById(R.id.user_profile_name);
            userbio=itemView.findViewById(R.id.user_bio);
            profilepic=itemView.findViewById(R.id.users_profile_pic);
            acceptButton=itemView.findViewById(R.id.request_accept_button);
            cancelButton=itemView.findViewById(R.id.request_cancel_button);
        }


    }
}
