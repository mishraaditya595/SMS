package com.example.SMS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    private String messageReceiverID, messageReceiverName,
            messageReceiverPic, messageSenderID;
    private TextView userName, userLastSeen;
    private CircleImageView userPicture;
    private Toolbar ChatToolbar;
    private ImageButton SendMsgBtn, SendFilesBtn;
    private EditText MessageInputText;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef, EKeyRef;
    private final List<Messages> messagesList=new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;
    private String saveCurrentTime, saveCurrentDate;
    private String checker="",myURL="";
    private Uri fileUri;
    private StorageTask uploadTask;
    private ProgressDialog LoadingBar;
    private byte[] EncryptionKey=KeyInititialisation();
    private Cipher cipher, decipher;
    private SecretKeySpec secretKeySpec;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth=FirebaseAuth.getInstance();
        messageSenderID=mAuth.getCurrentUser().getUid();
        RootRef= FirebaseDatabase.getInstance().getReference();
        EKeyRef=FirebaseDatabase.getInstance().getReference("Messages");

        messageReceiverID=getIntent().getExtras().get("visit_UID").toString();
        messageReceiverName=getIntent().getExtras().get("user_name").toString();
        messageReceiverName=getIntent().getExtras().get("user_picture").toString();
        //the above three lines will get the UID, picture and name of the person on whose
        // ID the user have tapped from the chats fragment.

        InitialiseField();


        userName.setText(messageReceiverName);
        Picasso.get().load(messageReceiverPic).placeholder(R.drawable.profile_image).into(userPicture);

        DisplayLastSeen();

        SendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    SendMessage();
            }
        });
        SendFilesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendFiles();
            }
        });
    }

    byte[] getEncryptionKey(){
        return EncryptionKey;
    }

    public void InitialiseField() {
        ChatToolbar=findViewById(R.id.chat_toolbar);
        setSupportActionBar(ChatToolbar);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater= (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView=layoutInflater.inflate(R.layout.custom_chat_bar,null);
        actionBar.setCustomView(actionBarView);

        userPicture=findViewById(R.id.custom_profile_picture);
        userName=findViewById(R.id.custom_profile_name);
        userLastSeen=findViewById(R.id.custom_last_seen);

        SendMsgBtn=findViewById(R.id.send_message_button);
        SendFilesBtn=findViewById(R.id.send_files_button);
        MessageInputText=findViewById(R.id.input_msg);

        messageAdapter=new MessageAdapter(messagesList);
        userMessagesList=(RecyclerView) findViewById(R.id.message_list);
        linearLayoutManager=new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        Calendar calendar=Calendar.getInstance();
        SimpleDateFormat currentDate=new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate=currentDate.format(Calendar.getInstance().getTime());

        SimpleDateFormat currentTime=new SimpleDateFormat("hh:mm a");
        saveCurrentTime=currentTime.format(Calendar.getInstance().getTime());

        LoadingBar=new ProgressDialog(this);

    }

    public byte[] KeyInititialisation()
    {
        byte[] eKey = new byte[16];
        Random rand = new Random();
        rand.nextBytes(eKey);
        return eKey;
    }

    @Override
    protected void onStart() {
        super.onStart();

        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Messages messages=dataSnapshot.getValue(Messages.class);
                        messagesList.add(messages);
                        messageAdapter.notifyDataSetChanged();

                        userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

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

    private void DisplayLastSeen()
    {
        RootRef.child("Users").child(messageSenderID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child("userState").hasChild("state"))
                        {
                            String state=dataSnapshot.child("userState").child("state").getValue().toString();
                            String date=dataSnapshot.child("userState").child("date").getValue().toString();
                            String time=dataSnapshot.child("userState").child("time").getValue().toString();

                            if(state.equalsIgnoreCase("Online"))
                            {
                                userLastSeen.setText(state);
                            }
                            else if(state.equalsIgnoreCase("Offline"))
                            {
                                userLastSeen.setText("Last Seen: "+ time + "; " +date);
                            }
                        }
                        else
                        {
                            userLastSeen.setText("Offline for a while");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void SendMessage() {
        String msg_text=MessageInputText.getText().toString();
        if(msg_text.isEmpty())
        {
            Toast.makeText(this, "Cannot send empty message.", Toast.LENGTH_SHORT).show();
        }
        else
        {
           String messageSenderRef="Messages/"+ messageSenderID + "/" + messageReceiverID;
           String messageReceiverRef="Messages/"+ messageReceiverID + "/" + messageSenderID;

            DatabaseReference userMessageKeyRef=RootRef.child("Messages")
                    .child(messageSenderID).child(messageReceiverID).push();

            String messagePushID=userMessageKeyRef.getKey();

            Map messageTextBody=new HashMap();
            messageTextBody.put("message", msg_text);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);
            messageTextBody.put("to", messageReceiverID);
            messageTextBody.put("messageID", messagePushID);
            messageTextBody.put("time", saveCurrentTime);
            messageTextBody.put("date", saveCurrentDate);

            Map messageBodyDetails=new HashMap();
            messageBodyDetails.put(messageSenderRef+"/"+messagePushID,messageTextBody);
            messageBodyDetails.put(messageReceiverRef+"/"+messagePushID,messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful())
                    {
                        Toast.makeText(ChatActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, "Unable not send message.", Toast.LENGTH_SHORT).show();
                    }
                    MessageInputText.setText(null);
                }
            });
        }
    }
    private void SendFiles() {
        CharSequence options[]=new CharSequence[]
                {
                        "Images",
                        "PDF files",
                        "Document files"
                };// this contains all the options that will show up when the send files button is tapped
        AlertDialog.Builder builder=new AlertDialog.Builder(ChatActivity.this);
        builder.setTitle("Send File");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch(i)
                {
                    case 0:
                        checker="image";
                        Intent imgIntent=new Intent();
                        imgIntent.setAction(Intent.ACTION_GET_CONTENT);
                        imgIntent.setType("image/*");
                        startActivityForResult(imgIntent.createChooser(imgIntent,"Select Image"),438);
                        break;
                    case 1:
                        checker="pdf";
                        Intent pdfIntent=new Intent();
                        pdfIntent.setAction(Intent.ACTION_GET_CONTENT);
                        pdfIntent.setType("application/pdf");
                        startActivityForResult(pdfIntent.createChooser(pdfIntent,"Select pdf file"),438);
                        break;
                    case 2:
                        checker="docx";
                        Intent docIntent=new Intent();
                        docIntent.setAction(Intent.ACTION_GET_CONTENT);
                        docIntent.setType("application/msword");
                        startActivityForResult(docIntent.createChooser(docIntent,"Select document file"),438);
                        break;
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==438 && resultCode==RESULT_OK && data!=null && data.getData()!=null)
        {
            LoadingBar.setTitle("Sending File");
            LoadingBar.setMessage("Please wait while your file is being sent.");
            LoadingBar.setCanceledOnTouchOutside(false);
            LoadingBar.show();
            fileUri=data.getData();
            if(!checker.equals("image"))
            {
                StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("Document Files");
                final String messageSenderRef="Messages/"+ messageSenderID + "/" + messageReceiverID;
                final String messageReceiverRef="Messages/"+ messageReceiverID + "/" + messageSenderID;

                DatabaseReference userMessageKeyRef=RootRef.child("Messages")
                        .child(messageSenderID).child(messageReceiverID).push();

                final String messagePushID=userMessageKeyRef.getKey();

                final StorageReference filePath= storageReference.child(messagePushID + "."+ checker);

                filePath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful())
                        {
                            String downloadUrl=task.getResult().getMetadata().getReference().getDownloadUrl().toString();



                            Map messageDocBody=new HashMap();
                            messageDocBody.put("message", downloadUrl);
                            messageDocBody.put("name",fileUri.getLastPathSegment());
                            messageDocBody.put("type", checker);
                            messageDocBody.put("from", messageSenderID);
                            messageDocBody.put("to", messageReceiverID);
                            messageDocBody.put("messageID", messagePushID);
                            messageDocBody.put("time", saveCurrentTime);
                            messageDocBody.put("date", saveCurrentDate);

                            Map messageBodyDetails=new HashMap();
                            messageBodyDetails.put(messageSenderRef+"/"+messagePushID,messageDocBody);
                            messageBodyDetails.put(messageReceiverRef+"/"+messagePushID,messageDocBody);

                            RootRef.updateChildren(messageBodyDetails);
                            LoadingBar.dismiss();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        LoadingBar.dismiss();
                        String exceptionMessage=e.getMessage();
                        Toast.makeText(ChatActivity.this,exceptionMessage,Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        double p=(100.0*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                        LoadingBar.setMessage((int)p+"% uploaded.");
                    }
                });
            }
            else if(checker.equals("image"))
            {
                StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("Image Files");
                final String messageSenderRef="Messages/"+ messageSenderID + "/" + messageReceiverID;
                final String messageReceiverRef="Messages/"+ messageReceiverID + "/" + messageSenderID;

                DatabaseReference userMessageKeyRef=RootRef.child("Messages")
                        .child(messageSenderID).child(messageReceiverID).push();

                final String messagePushID=userMessageKeyRef.getKey();

                final StorageReference filePath= storageReference.child(messagePushID + ".jpg");
                uploadTask= filePath.putFile(fileUri);
                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if(!task.isSuccessful())
                        {
                            throw task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful())
                        {
                            Uri downloadUrl=task.getResult();
                            myURL=downloadUrl.toString();

                            Map messageImageBody=new HashMap();
                            messageImageBody.put("message", myURL);
                            messageImageBody.put("name",fileUri.getLastPathSegment());
                            messageImageBody.put("type", checker);
                            messageImageBody.put("from", messageSenderID);
                            messageImageBody.put("to", messageReceiverID);
                            messageImageBody.put("messageID", messagePushID);
                            messageImageBody.put("time", saveCurrentTime);
                            messageImageBody.put("date", saveCurrentDate);

                            Map messageBodyDetails=new HashMap();
                            messageBodyDetails.put(messageSenderRef+"/"+messagePushID,messageImageBody);
                            messageBodyDetails.put(messageReceiverRef+"/"+messagePushID,messageImageBody);

                            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                @Override
                                public void onComplete(@NonNull Task task) {
                                    if(task.isSuccessful())
                                    {
                                        LoadingBar.dismiss();
                                        Toast.makeText(ChatActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        LoadingBar.dismiss();
                                        Toast.makeText(ChatActivity.this, "Unable not send message.", Toast.LENGTH_SHORT).show();
                                    }
                                    MessageInputText.setText(null);
                                }
                            });
                        }
                    }
                });
            }
            else
            {
                LoadingBar.dismiss();
                Toast.makeText(this,"Error! You have not selected any image file",Toast.LENGTH_SHORT).show();
            }
        }
    }
}
