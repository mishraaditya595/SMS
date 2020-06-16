package com.example.SMS;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 */
public class GroupsFragment extends Fragment {

    private Button createGroupButton;
    private DatabaseReference RootRef,GroupRef;
    private View groupFragView;
    private ListView list_View;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> list_of_groups=new ArrayList<>();
    private Context context;

    public GroupsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        groupFragView=inflater.inflate(R.layout.fragment_groups, container, false);
        RootRef= FirebaseDatabase.getInstance().getReference();
        GroupRef=FirebaseDatabase.getInstance().getReference().child("Groups");

        context = groupFragView.getContext();
        context.setTheme(android.R.style.Theme_Black);//these two lines are to make the text in the listview white

        InitializeFields();

        createGroupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RequestNewGroup();
                }
            });

        RetrieveGroups();//this method is to retrieve data from our database on the groups created

        list_View.setOnItemClickListener(new AdapterView.OnItemClickListener() { //to perform a task when an item from the list is tapped
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String currentGroupName=adapterView.getItemAtPosition(position).toString();
                SendUserToGroupChatActivity(currentGroupName);
            }
        });

        return groupFragView;
    }


    private void InitializeFields() {
        createGroupButton=groupFragView.findViewById(R.id.CreateGroupButton);
        list_View=groupFragView.findViewById(R.id.list_view);
        arrayAdapter=new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,list_of_groups);
        list_View.setAdapter(arrayAdapter);
    }

    private void RequestNewGroup() {
        final Activity activity = getActivity();
        AlertDialog.Builder builder=new AlertDialog.Builder(activity,R.style.AlertDialog);
            builder.setTitle("Enter Group Name:");
            final EditText groupName=new EditText(activity);
            groupName.setHint("e.g. Group Name");
            builder.setView(groupName);
            builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String groupNameStr = groupName.getText().toString();
                    if(groupNameStr.isEmpty()){
                        Toast.makeText(activity,"Group Name cannot be left blank",Toast.LENGTH_SHORT);
                    }
                    else{
                        CreateNewGroup(groupNameStr);
                    }

                }

                private void CreateNewGroup(final String groupNameStr) {
                    //to update database with the group details
                    RootRef.child("Groups").child(groupNameStr).setValue("")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful())
                                        Toast.makeText(activity,groupNameStr+" creation is successful",Toast.LENGTH_SHORT).show();
                                }
                            });

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
    private void RetrieveGroups() {
        GroupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> set=new HashSet<>();
                Iterator iterator=dataSnapshot.getChildren().iterator(); //to read the groups from the database
                while (iterator.hasNext()){
                    set.add(((DataSnapshot)iterator.next()).getKey()); //to get the group names
                }
                list_of_groups.clear();
                list_of_groups.addAll(set);
                arrayAdapter.notifyDataSetChanged();
                //the above three lines are to update the list view with the group names
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendUserToGroupChatActivity(String currentGroupName) {
        Intent groupChatIntent=new Intent(getContext(),GroupChatActivity.class);
        groupChatIntent.putExtra("Group Name",currentGroupName);
        startActivity(groupChatIntent);
    }

    }