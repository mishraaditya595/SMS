package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private RecyclerView FindFriendsRecyclerList;
    private DatabaseReference UsersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        UsersRef= FirebaseDatabase.getInstance().getReference().child("Users");

        FindFriendsRecyclerList=findViewById(R.id.find_friends_recycler_list);
        FindFriendsRecyclerList.setLayoutManager(new LinearLayoutManager(this));

        mToolbar=findViewById(R.id.FindFriendstoolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Find Friends");
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options=
                new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(UsersRef,Contacts.class).build();

        FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder> adapter=
                new FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull FindFriendsViewHolder holder, final int position, @NonNull Contacts model) {
                        holder.username.setText(model.getName());
                        holder.userstatus.setText(model.getBio());
                        Picasso.get().load(model.getImage()).placeholder(R.drawable.profile_image).into(holder.profileImage);
                        //the above three lines are to show the username, user bio and profile picture in the find friends section

                        holder.itemView.setOnClickListener(new View.OnClickListener() { //the itemView object contains the username, bio and profile picture
                            @Override
                            public void onClick(View v) {
                                String visit_user_ID= getRef(position).getKey();//to get the user ID of the user whose profile is tapped on

                                SendUserToVisitProfileActivity(visit_user_ID);
                            }
                        });

                    }

                    @NonNull
                    @Override
                    public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout,parent,false);
                        FindFriendsViewHolder viewHolder=new FindFriendsViewHolder(view);
                        return viewHolder;
                    }
                };
        FindFriendsRecyclerList.setAdapter(adapter);
        adapter.startListening();
        }

    private void SendUserToVisitProfileActivity(String visit_user_id) {
        Intent profileIntent=new Intent(FindFriendsActivity.this,VisitProfileActivity.class);
        profileIntent.putExtra("visit_user_ID",visit_user_id); //to pass the user ID to the VisitProfileActivity via Profile Intent
        startActivity(profileIntent);
    }
}

    class FindFriendsViewHolder extends RecyclerView.ViewHolder{

    TextView username, userstatus;
    CircleImageView profileImage;
        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            //the itemView object contains the username, bio and profile picture

            username=itemView.findViewById(R.id.user_profile_name);
            userstatus=itemView.findViewById(R.id.user_bio);
            profileImage=itemView.findViewById(R.id.users_profile_pic);
        }
    }

