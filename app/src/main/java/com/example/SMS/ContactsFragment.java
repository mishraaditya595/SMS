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
import android.widget.ImageView;
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
public class ContactsFragment extends Fragment {
    private Button FindFriendsButton;
    private View ContactFragView;
    private RecyclerView myContactsList;
    private DatabaseReference ContactsRef, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUID;

    public ContactsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ContactFragView=inflater.inflate(R.layout.fragment_contacts, container, false);

        Initialisefields();

        mAuth=FirebaseAuth.getInstance();
        currentUID=mAuth.getCurrentUser().getUid();
        ContactsRef= FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUID);
        UsersRef=FirebaseDatabase.getInstance().getReference().child("Users");

        FindFriendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToFindFriendsAcivity();
            }
        });
        return ContactFragView;
    }

    private void Initialisefields() {
        FindFriendsButton=ContactFragView.findViewById(R.id.FindFriendsButton);

        myContactsList=ContactFragView.findViewById(R.id.contacts_list);
        myContactsList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions options=new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(ContactsRef, Contacts.class).build();

        FirebaseRecyclerAdapter<Contacts, ContactsViewHolder> adapter=
                new FirebaseRecyclerAdapter<Contacts, ContactsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final ContactsViewHolder holder, int position, @NonNull Contacts model) {
                        //this is where we basically retrieve the username, picture and bio
                        String UID=getRef(position).getKey(); //it will store the UID of the users
                        UsersRef.child(UID).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists())
                                {
                                    if(dataSnapshot.child("userState").hasChild("state"))
                                    {
                                        String state=dataSnapshot.child("userState").child("state").getValue().toString();
                                        String date=dataSnapshot.child("userState").child("date").getValue().toString();
                                        String time=dataSnapshot.child("userState").child("time").getValue().toString();

                                        if(state.equalsIgnoreCase("Online"))
                                        {
                                            holder.onlineIcon.setVisibility(View.VISIBLE);
                                        }
                                        else if(state.equalsIgnoreCase("Offline"))
                                        {
                                            holder.onlineIcon.setVisibility(View.INVISIBLE);
                                        }
                                    }
                                    else
                                    {
                                        holder.onlineIcon.setVisibility(View.INVISIBLE);
                                    }

                                    if(dataSnapshot.hasChild("image"))// this is if the user has a profile picture set
                                    {
                                        String profilePic=dataSnapshot.child("image").getValue().toString();
                                        String profileBio=dataSnapshot.child("bio").getValue().toString();
                                        String profileName=dataSnapshot.child("name").getValue().toString();

                                        holder.userName.setText(profileName);
                                        holder.userBio.setText(profileBio);
                                        Picasso.get().load(profilePic).placeholder(R.drawable.profile_image).into(holder.userPic);
                                    }
                                    else // this is if the user has not set any profile picture set
                                    {
                                        String profileBio=dataSnapshot.child("bio").getValue().toString();
                                        String profileName=dataSnapshot.child("name").getValue().toString();

                                        holder.userName.setText(profileName);
                                        holder.userBio.setText(profileBio);

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
                    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        //this is basically to know how the layout of the recycler view to look like
                        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
                        ContactsViewHolder viewHolder = new ContactsViewHolder(view);
                        return viewHolder;
                    }
                };

        myContactsList.setAdapter(adapter);
        adapter.startListening();
    }

    private void SendUserToFindFriendsAcivity() {
        Intent FindFriendsIntent=new Intent(getContext(),FindFriendsActivity.class);
        startActivity(FindFriendsIntent);
    }

    public static class ContactsViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName, userBio;
        CircleImageView userPic;
        ImageView onlineIcon;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            userName=itemView.findViewById(R.id.user_profile_name);
            userBio=itemView.findViewById(R.id.user_bio);
            userPic=itemView.findViewById(R.id.users_profile_pic);
            onlineIcon=itemView.findViewById(R.id.user_online_status);
        }
    }

}


