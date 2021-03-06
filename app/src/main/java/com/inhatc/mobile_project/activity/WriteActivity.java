package com.inhatc.mobile_project.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.inhatc.mobile_project.R;
import com.inhatc.mobile_project.db.MemberInfo;
import com.inhatc.mobile_project.db.Post;

import org.parceler.Parcels;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriteActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText edtPlace, txtcontent;
    private Button btnPlaceDialog, btnPlaceSearch;
    private Dialog placeDialog;
    private TextView tvCheckPlace;
    private Button btnAddPost;
    private ImageView postimage;


    public static final int GALLEY_CODE = 10;

    private Geocoder geocoder;
    private List<Address> addressList;

    private static final String TAG = "NewPostFragment";
    private static final String REQUIRED = "Required";

    private int GALLERY_CODE = 10;

    private DatabaseReference mDatabase;
    private FirebaseStorage storage;
    private FirebaseDatabase database;
    private Bundle userBundle = new Bundle();
    private Bundle postBundle = new Bundle();

//    private String userName, userProfile;


    private MemberInfo userInfo = new MemberInfo();
    private FirebaseUser user;

    private String imageUrl="";
    private String path;
    private Uri filePath;

    private ImageView selectedImageVIew;
    private EditText selectedEditText;
    private ArrayList<String> pathList = new ArrayList<>();

    private Post postItems = new Post();
    private boolean isUpdate = false;

    private String key;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);

        txtcontent = findViewById(R.id.insertContent);
        btnAddPost = findViewById(R.id.insertBtn);
        postimage = findViewById(R.id.insertImg);

        btnAddPost.setOnClickListener(this);
        postimage.setOnClickListener(this);

        //?????? ????????? uid ????????? userInfo ????????????
        user = FirebaseAuth.getInstance().getCurrentUser();
        //userInfo.bringMemberInfo(user.getUid());

        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnPlaceDialog = (Button) findViewById(R.id.btnPlaceDialog);        // ?????? ?????? ????????? ??????
        btnPlaceDialog.setOnClickListener(this);
        geocoder = new Geocoder(this);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        Intent intent = getIntent();

        userBundle = intent.getBundleExtra("userInfoData");
        postBundle = intent.getBundleExtra("UpdatePostData");
        //?????? ??????
        if(userBundle != null){
            Object value = Parcels.unwrap(userBundle.getParcelable("userInfoData"));
            userInfo = (MemberInfo) value;
            isUpdate = false;


        }
        //?????? ??????
        if(postBundle != null){
            Object value = Parcels.unwrap(postBundle.getParcelable("UpdatePostData"));
            postItems = (Post) value;
            isUpdate = true;
            userInfo.setProfimageURL(postItems.getProfileImg());
            userInfo.setName(postItems.getAuthor());
            initView();

        }

    }

    // ????????? view ??????
    private void initView() {
        txtcontent.setText(postItems.getPostcontent());
        Glide.with(this)
                .load(postItems.getDownloadImgUri())
                .into(postimage);

        try {
            addressList = geocoder.getFromLocation(postItems.getmLatitude(), postItems.getmLongitude(), 10);
            btnPlaceDialog.setText(addressList.get(0).getAdminArea()+"??????");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPlaceDialog :          // '?????? ?????? ?????????' ??????
                showPlaceDialog();              // ??????????????? ????????????
                break;
            case R.id.pDia_btnSearchPlace :     // ?????????????????? ?????? ?????? ??????
                if(btnPlaceSearch.getText().toString().equals("?????? ??????")) {
                    String address = edtPlace.getText().toString();     // ???????????? ????????? ?????? ???
                    replaceLatLng(address);
                } else {
                    // addressList ???????????? ??????, ?????? ????????????????????? ??????
                    btnPlaceDialog.setText(addressList.get(0).getAdminArea()+"???!");
                    placeDialog.dismiss();
                }
                break;
            case R.id.insertImg :
                //?????? ??????????????? ??????
                lodadAlbum();
                break;
            case R.id.insertBtn :          // ??????
//                uploadImage(imageUrl);
                if(txtcontent.getText() != null && addressList != null){
                    if(filePath != null || postItems.getDownloadImgUri() != null){
                        pthotURL(user.getUid(), userInfo.getName(),txtcontent.getText().toString(), filePath);
                        finish();
                    }
                }else{
                    Toast.makeText(WriteActivity.this, "?????? ?????? ????????? ??????????????????", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void lodadAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, GALLEY_CODE);
    }

    //?????? ?????? ??? ???????????? ??????
    //?????? ???????????? ?????????
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_CODE)
        {
            //?????? ?????? ??????
            filePath = data.getData();
            //?????? ????????? ?????? ?????????
            try{
                InputStream in = getContentResolver().openInputStream(data.getData());
                Bitmap img = BitmapFactory.decodeStream(in);
                in.close();
                postimage.setImageBitmap(img);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // ????????? ??????
    private void writeNewPost(Post post){
        try {
            Map<String, Object> postValues = post.toMap();
            Map<String, Object> childUpdates = new HashMap<>();
            //?????? ????????? ??????
            //post-postuid-??????

            childUpdates.put("/posts/" + post.getPostId(), postValues);
            //???????????? ????????? ??????
            //user-posts-?????????uid-??????
            childUpdates .put("/user-posts/" + post.getUid() + "/" + post.getPostId(), postValues);
            mDatabase.updateChildren(childUpdates);
            Toast.makeText(WriteActivity.this, "????????? ??????", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Toast.makeText(WriteActivity.this, "????????? ??????", Toast.LENGTH_SHORT).show();
        }


    }

    //?????? ????????? ?????? URL??? ??????
    private void pthotURL(String uId, String name, String content, Uri filePath) {

        //????????? ?????? ????????????
        if(!isUpdate){
            key = mDatabase.child("posts").push().getKey();
        }else {
            key = postItems.getPostId();
        }

        
        //???????????? ???????????? ????????? ?????????
        if(filePath != null){
            try{
                StorageReference storageRef = storage.getReference();
                StorageReference riversRef = storageRef.child("photos/"+uId+"/"+key+".png");
                UploadTask uploadTask = riversRef.putFile(filePath);

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if(!task.isSuccessful()){
                            throw task.getException();
                        }
                        // Continue with the task to get the download URL
                        return riversRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful()){
                            Log.d("phtoURL ??????", "??????");

                            @SuppressWarnings("VisibleForTests")
                            Uri downloadUrl = task.getResult();
                            Post post = new Post(key, uId, name, addressList.get(0).getLatitude(), addressList.get(0).getLongitude(), content, downloadUrl.toString(), userInfo.getProfimageURL());
                            writeNewPost(post);


                        }else{
                            Log.d("phtoURL ??????", "??????");
                        }
                    }
                });

            }catch (NullPointerException e)
            {
                Log.d("phtoURL ??????", "????????? ?????? ??????");
            }
        }else {
            Post post = new Post(key, uId, name, addressList.get(0).getLatitude(), addressList.get(0).getLongitude(), content, postItems.getDownloadImgUri(), userInfo.getProfimageURL());
            writeNewPost(post);
        }

    }

    // ??????????????? ????????????
    public void showPlaceDialog() {
        placeDialog = new Dialog(this);
        placeDialog.setContentView(R.layout.dialog_place);
        placeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        placeDialog.show();

        edtPlace = placeDialog.findViewById(R.id.pDia_edtPlace);
        tvCheckPlace = placeDialog.findViewById(R.id.pDia_tvCheck);
        btnPlaceSearch = placeDialog.findViewById(R.id.pDia_btnSearchPlace);
        btnPlaceSearch.setOnClickListener(this);

        edtPlace.addTextChangedListener(new TextWatcher() {         // EditText ??? ?????? ???
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCheckPlace.setText("");
                btnPlaceSearch.setText("?????? ??????");
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    // ???????????? ????????? ??????, ????????? ??????
    public void replaceLatLng(String address) {
        addressList = null;

        try {
            addressList = geocoder.getFromLocationName(address, 10);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("test", "????????? ?????? - ???????????? ??????????????? ?????? ??????");
        }

        if (addressList != null) {
            if (addressList.size() == 0) {
                Toast.makeText(this, "???????????? ?????? ????????? ????????????. ?????? ??????????????????.", Toast.LENGTH_LONG).show();
            } else {
                tvCheckPlace.setText(String.format("%s ???(???) ???????????", addressList.get(0).getAddressLine(0).toString()));
                btnPlaceSearch.setText("??????");
            }
        }
    }
}