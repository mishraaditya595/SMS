package com.example.SMS;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Messages> userMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private ChatActivity encryptionObject=new ChatActivity();
    private SecretKeySpec secretKeySpec;
    private Cipher decipher;


    public MessageAdapter(List<Messages> userMessagesList)
    {
        this.userMessagesList=userMessagesList;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder
    {
        public TextView senderMessageText, receiverMessageText;
        public CircleImageView receiverProfilePic;
        public ImageView messageSenderPic, messageReceiverPic;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessageText=itemView.findViewById(R.id.sender_message_text);
            receiverMessageText=itemView.findViewById(R.id.receiver_message_text);
            receiverProfilePic=itemView.findViewById(R.id.message_profile_pic);
            messageSenderPic=itemView.findViewById(R.id.message_sender_image);
            messageReceiverPic=itemView.findViewById(R.id.message_receiver_image);
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_messages_layout,parent,false);

        mAuth=FirebaseAuth.getInstance();


        return new MessageViewHolder(view);

        //this method is to showcase our custom message layout
    }


    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, final int position) throws NullPointerException {
        String messageSenderID=mAuth.getCurrentUser().getUid();
        Messages messages=userMessagesList.get(position);
        String fromUserID=messages.getFrom();
        String fromMessageType=messages.getType();
        usersRef= FirebaseDatabase.getInstance().getReference().child("Users")
                .child(fromUserID);
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("image"))
                {
                    //if statement is to check if the user has a profile picture or not
                    String receiverPic=dataSnapshot.child("image").getValue().toString();
                    Picasso.get().load(receiverPic).placeholder(R.drawable.profile_image)
                            .into(holder.receiverProfilePic);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverProfilePic.setVisibility(View.GONE);
        holder.senderMessageText.setVisibility(View.GONE);
        holder.messageSenderPic.setVisibility(View.GONE);
        holder.messageReceiverPic.setVisibility(View.GONE);

        if(fromMessageType.equals("text"))
        {

            if(fromUserID.equals(messageSenderID))
            {
                holder.senderMessageText.setVisibility(View.VISIBLE);
                holder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                holder.senderMessageText.setText(messages.getMessage() + "\n \n" + messages.getTime() +" - "+ messages.getDate());

            }
            else
            {
                holder.receiverProfilePic.setVisibility(View.VISIBLE);
                holder.receiverMessageText.setVisibility(View.VISIBLE);

                holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                holder.receiverMessageText.setText(messages.getMessage() + "\n \n" + messages.getTime() +" - "+ messages.getDate());
            }
        }
        else if(fromMessageType.equals("image"))
        {
            if(fromUserID.equals(messageSenderID))
            {
                holder.messageSenderPic.setVisibility(View.VISIBLE);
                Picasso.get().load(messages.getMessage()).into(holder.messageSenderPic);
            }
            else
            {
                holder.receiverProfilePic.setVisibility(View.VISIBLE);
                holder.messageReceiverPic.setVisibility(View.VISIBLE);

                Picasso.get().load(messages.getMessage()).into(holder.messageReceiverPic);
            }
        }
        else
        {
            //this is for any other file type like doc and pdf
            if(fromUserID.equals(messageSenderID))
            {
                holder.messageSenderPic.setVisibility(View.VISIBLE);
                holder.messageSenderPic.setBackgroundResource(R.drawable.file);

                holder.itemView.setOnClickListener(new View.OnClickListener() { //this is to download the file on the device
                    @Override
                    public void onClick(View v) {
                        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                        holder.itemView.getContext().startActivity(intent);
                    }
                });
            }
            else
            {
                holder.receiverProfilePic.setVisibility(View.VISIBLE);
                holder.messageReceiverPic.setVisibility(View.VISIBLE);

                holder.messageReceiverPic.setBackgroundResource(R.drawable.file);

                holder.itemView.setOnClickListener(new View.OnClickListener() { //this is to download the file on the device
                    @Override
                    public void onClick(View v) {
                        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                        holder.itemView.getContext().startActivity(intent);
                    }
                });
            }
        }

    }



    @Override
    public int getItemCount() {
        return userMessagesList.size();
        //this method returns the number of messages between two users
    }
}
