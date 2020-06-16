package com.example.SMS;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {
    private Button msgReqButton;
    private View ChatFragRef;
    private RecyclerView chatsList;
    private DatabaseReference ChatsRef, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUID;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ChatFragRef=inflater.inflate(R.layout.fragment_chats, container, false);

        mAuth=FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {

            Intent goToLogin = new Intent(getContext(), LoginActivity.class);
            startActivity(goToLogin);
        }

        currentUID=mAuth.getCurrentUser().getUid();
        ChatsRef= FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUID);
        UsersRef=FirebaseDatabase.getInstance().getReference().child("Users");

        InitialiseFields();

        msgReqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToMessageRequestActivity();
            }
        });
        return ChatFragRef;
    }

    private void InitialiseFields() {
        msgReqButton=ChatFragRef.findViewById(R.id.message_requests_button);
        chatsList=ChatFragRef.findViewById(R.id.chats_list);
        chatsList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options
                =new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(ChatsRef, Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts, ChatsViewHolder> adapter
                =new FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatsViewHolder holder, int position, @NonNull Contacts model) {
                final String usersIDs=getRef(position).getKey();
                final String[] retrieveImg = {"defaultImage"};
                UsersRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            if(dataSnapshot.hasChild("image"))
                            {
                                //for users having profile image
                                retrieveImg[0] =dataSnapshot.child("image").getValue().toString();
                                Picasso.get().load(retrieveImg[0]).placeholder(R.drawable.profile_image).into(holder.profilePic);
                            }

                            final String retrieveName=dataSnapshot.child("name").getValue().toString();
                            final String retrieveBio=dataSnapshot.child("bio").getValue().toString();

                            holder.userName.setText(retrieveName);

                            if(dataSnapshot.child("userState").hasChild("state"))
                            {
                                String state=dataSnapshot.child("userState").child("state").getValue().toString();
                                String date=dataSnapshot.child("userState").child("date").getValue().toString();
                                String time=dataSnapshot.child("userState").child("time").getValue().toString();

                                if(state.equalsIgnoreCase("Online"))
                                {
                                    holder.userBio.setText(state);
                                }
                                else if(state.equalsIgnoreCase("Offline"))
                                {
                                    holder.userBio.setText("Last Seen: "+ time + "; " +date);
                                }
                            }
                            else
                            {
                                holder.userBio.setText("Offline");
                            }


                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent chatIntent=new Intent(getContext(),ChatActivity.class);
                                    chatIntent.putExtra("visit_UID",usersIDs);
                                    chatIntent.putExtra("user_name",retrieveName);
                                    chatIntent.putExtra("user_picture", retrieveImg[0]);
                                    startActivity(chatIntent);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent,false);
                return new ChatsViewHolder(view);
            }
        };

        chatsList.setAdapter(adapter);
        adapter.startListening();
    }

    private void SendUserToMessageRequestActivity() {
        Intent intent= new Intent(getContext(), MessageRequests.class);
        startActivity(intent);
    }

    class ChatsViewHolder extends RecyclerView.ViewHolder
    {
        CircleImageView profilePic;
        TextView userName, userBio;
        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            profilePic=itemView.findViewById(R.id.users_profile_pic);
            userBio=itemView.findViewById(R.id.user_bio);
            userName=itemView.findViewById(R.id.user_profile_name);
        }
    }
}
