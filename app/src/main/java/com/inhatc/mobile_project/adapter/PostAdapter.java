package com.inhatc.mobile_project.adapter;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.inhatc.mobile_project.R;
import com.inhatc.mobile_project.db.Post;
import com.inhatc.mobile_project.activity.MapActivity;
import com.inhatc.mobile_project.activity.WriteActivity;

import org.parceler.Parcels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder>{

    private Geocoder geocoder;
    private ArrayList<Post> postItems;
    private Context mContext;
    private DatabaseReference pDatabase;


    private int index;
    private FirebaseUser user;
    private boolean isLikePost;

    private Map<String, Boolean> stars = new HashMap<>();

    public PostAdapter(ArrayList<Post> postItems, Context context) {
        this.postItems = postItems;
        //this.subDatabaseRef = subDatabaseRef;
        this.mContext = context;
    }

    public ArrayList<Post> getItems() {
        return postItems;
    }


    @Override
    public int getItemCount() {
        return (postItems != null ? postItems.size() : 0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View holder = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        mContext = parent.getContext();
        geocoder = new Geocoder(mContext);
        user = FirebaseAuth.getInstance().getCurrentUser();
        pDatabase = FirebaseDatabase.getInstance().getReference();
        return new ViewHolder(holder);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.onBind(holder, postItems.get(position), position);

        //view ????????? ?????? ??? ??????holder.itemView
        // ????????? ?????? ??? ????????? holder.postImage.setOnClickListener??? ????????? ???
        holder.postImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(mContext, MapActivity.class);
                // ?????? ?????? ??? ????????????
                i.putExtra("latitude", postItems.get(position).getmLatitude());
                i.putExtra("longitude", postItems.get(position).getmLongitude());
                mContext.startActivity(i);

            }
        });
    }


    //????????? ??????
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView poster_id, tv_contents, tv_location, likeCounter;
        //private int index;
        private ImageView postImage, likeImg, posterProfile, btn_edit;

        public ViewHolder(@NonNull View view) {
            super(view);
            poster_id = view.findViewById(R.id.item_name);
            tv_contents = view.findViewById(R.id.item_content);
            tv_location = view.findViewById(R.id.item_place);
            postImage = view.findViewById(R.id.item_image);
            posterProfile = view.findViewById(R.id.item_profile);
            likeCounter = view.findViewById(R.id.item_goodCount);
            likeImg = view.findViewById(R.id.item_goodMark);
            btn_edit = view.findViewById(R.id.edtiBtn);

        }


        public void onBind(@NonNull ViewHolder holder, Post post, int position) {
            index = position;

            //?????????, ????????? ??????, ????????? ??????, ?????? ?????? setting
            Glide.with(holder.itemView)
                    .load(post.getDownloadImgUri())
                    .into(postImage);
            Glide.with(holder.itemView)
                    .load(post.getProfileImg())
                    .into(posterProfile);
            poster_id.setText(post.getAuthor());
            tv_contents.setText(post.getPostcontent());

            //????????? ??????
            try {
                List<Address> addressList = geocoder.getFromLocation(post.getmLatitude(), post.getmLongitude(), 10);
                tv_location.setText(addressList.get(0).getAddressLine(0)+"??????");
            } catch (IOException e) {
                e.printStackTrace();
            }
            // ????????? ????????? ??????
            likeCounter.setText(String.valueOf(post.getStarCount()));
            
            //????????? ????????? ????????? ????????? ??????
            stars = post.getStars();
            if(stars.containsKey(user.getUid()) && stars.get(user.getUid())){
                //????????? ?????? ?????????
                isLikePost = true;
                likeImg.setImageResource(R.drawable.icon_good);
            }else {
                isLikePost = false;
                likeImg.setImageResource(R.drawable.icon_empty_good);
            }

            //?????? ???????????? ???????????? ??? ???????????? ?????? ???
            //????????? ??????
            if(!(post.getUid().equals(user.getUid()))){

                //????????? ?????????
                likeImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stars = post.getStars();
                        if(stars.get(user.getUid()) != null){
                            isLikePost = stars.get(user.getUid());
                        }else {
                            isLikePost = false;
                        }

                        //????????? ??????...
                        //????????? ??? ???????????? ???????????? ????????? ??????
                        //????????????????????? uid??? true flase ??? ??????, ????????? ????????? ??? ??????
                        if(isLikePost){
                            //????????? ??????
                            isLikePost = false;
                            likeImg.setImageResource(R.drawable.icon_empty_good);
                            stars.put(user.getUid(), isLikePost);
                            post.setStarCount(post.getStarCount()-1);

                            Map<String, Object> taskMap = new HashMap<String, Object>();
                            taskMap.put("/posts/" + post.getPostId() + "/stars", stars);
                            taskMap.put("/posts/" + post.getPostId() + "/starCount", post.getStarCount());

                            taskMap.put("/user-posts/" + post.getUid() + "/" + post.getPostId() + "/stars", stars);
                            taskMap.put("/user-posts/" + post.getUid() + "/" + post.getPostId() + "/starCount", post.getStarCount());
                            pDatabase.updateChildren(taskMap);

                        }else {
                            //?????????
                            isLikePost = true;
                            likeImg.setImageResource(R.drawable.icon_good);
                            post.setStarCount(post.getStarCount()+1);
                            likeCounter.setText(String.valueOf(post.getStarCount()));
                            stars.put(user.getUid(), isLikePost);


                            Map<String, Object> taskMap = new HashMap<String, Object>();
                            taskMap.put("/posts/" + post.getPostId() + "/stars", stars);
                            taskMap.put("/posts/" + post.getPostId() + "/starCount", post.getStarCount());

                            taskMap.put("/user-posts/" + post.getUid() + "/" + post.getPostId() + "/stars", stars);
                            taskMap.put("/user-posts/" + post.getUid() + "/" + post.getPostId() + "/starCount", post.getStarCount());
                            pDatabase.updateChildren(taskMap);

                        }

                    }
                });
            }

            //?????? ????????? ?????? ?????? ?????? ??????
            if(user.getUid().equals(post.getUid())){
                btn_edit.setEnabled(true);
                btn_edit.setVisibility(View.VISIBLE);
                btn_edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //?????? ?????? ?????? ???????????? ?????? ??????
                        PopupMenu popup = new PopupMenu(mContext, v);
                        popup.getMenuInflater().inflate(R.menu.edit_menu, popup.getMenu());

                        //?????? ?????? ?????? ?????????
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.action_remove:
                                        //??????
                                        onDeleteContent(position);
                                        break;
                                    case R.id.action_modify:
                                        //????????? ??? ?????? ?????? position??? post?????? bundle??? ?????? 
                                        // WriteActivity??? ???????????? ??????
                                        Bundle bundle = new Bundle();
                                        bundle.putParcelable("UpdatePostData", Parcels.wrap(post));

                                        Intent intent = new Intent(mContext, WriteActivity.class);
                                        intent.putExtra("UpdatePostData", bundle);
                                        mContext.startActivity(intent);
                                        break;
                                    default:
                                        break;
                                }
                                return false;
                            }
                        });
                        popup.show();
                    }
                });

            }else {
                btn_edit.setEnabled(false);
                btn_edit.setVisibility(View.INVISIBLE);
            }

        }


    }

    //?????? ?????????
    private void onDeleteContent(int position)
    {

        Map<String, Object> childUpdates = new HashMap<>();
        String postId = postItems.get(position).getPostId();
        String userID = postItems.get(position).getUid();

        try{
            childUpdates.put("/posts/" + postId, null);
            childUpdates .put("/user-posts/" + userID + "/" + postId, null);
            pDatabase.updateChildren(childUpdates);
            FirebaseStorage storage = FirebaseStorage.getInstance();
            storage.getReference().child("photos/"+ userID +"/" + postId+".png").delete().addOnSuccessListener(new OnSuccessListener<Object>() {
                @Override
                public void onSuccess(Object o) {
                    Toast.makeText(mContext, "?????? ??????", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(mContext, "?????? ??????", Toast.LENGTH_SHORT).show();
                }
            });

        }catch (Exception e){
            Toast.makeText(mContext, "?????? ??????", Toast.LENGTH_SHORT).show();
        }

    }


    // ?????? ????????? ?????????
    public void setItem(ArrayList<Post> data) {
        postItems = data;
        notifyDataSetChanged();
    }



}


