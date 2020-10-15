package com.example.ecom2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class AdminAddNewPrdctActivity extends AppCompatActivity {

    private String CategoryName, Description, Price, Pname;//:-productName
    private String saveCurrentDate, saveCurrentTime, productRandomKey;
    private String downloadImageUrl;
    private ImageView InputProductImage;
    private Button AddNewProductButton;
    private EditText InputProductName, InputProductDescription, InputProductPrice;
    private static final int GalleryPick = 1;
    //create image url datatype
    private Uri ImageUri;
    private StorageReference ProductImagesRef;
    private DatabaseReference ProductsRef;
    private ProgressDialog loadingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_new_prdct);


       CategoryName = getIntent().getExtras().get("category").toString();
       ProductImagesRef = FirebaseStorage.getInstance().getReference().child("Product Images");
       ProductsRef = FirebaseDatabase.getInstance().getReference().child("Products");

       AddNewProductButton = (Button) findViewById(R.id.add_new_prdct);
       InputProductImage = (ImageView) findViewById(R.id.select_prdct_image);
       InputProductName = (EditText) findViewById(R.id.product_name);
       InputProductDescription = (EditText) findViewById(R.id.product_descript);
       InputProductPrice = (EditText) findViewById(R.id.product_price);
       loadingBar = new ProgressDialog(this);


       InputProductImage.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view)
           {
                openGallery();
           }
       });

       AddNewProductButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view)
           {
                ValidateProductData();
           }
       });

    }

    private void openGallery()
    {
            Intent galleryIntent = new Intent();
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");  //static sign
            startActivityForResult(galleryIntent, GalleryPick);


        //        galleryIntent.setType("image/*");  //static sign
        //        startActivityForResult(galleryIntent, GalleryPick);
        //gallerypick will store thepik selectedby admin
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        //if (requestCode==GalleryPick && requestCode==RESULT_OK && data!=null)
        if (resultCode==RESULT_OK && requestCode==GalleryPick && data!=null)
        {
            ImageUri = data.getData();
            //display selectd image
            InputProductImage.setImageURI(ImageUri);
        }
    }

    private void ValidateProductData()
    {
        Description = InputProductDescription.getText().toString();
        Price = InputProductPrice.getText().toString();
        Pname = InputProductName.getText().toString();

        if(ImageUri== null)
        {
            Toast.makeText(this, "Must add product image", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Description))
        {
            Toast.makeText(this, "write product description", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Price))
        {
            Toast.makeText(this, "write product price", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Pname))
        {
            Toast.makeText(this, "write product name", Toast.LENGTH_SHORT).show();
        }
        else
        {
            storeProductInformation();
        }
    }

    private void storeProductInformation()
    {
        loadingBar.setTitle("add new products");
        loadingBar.setMessage("wait while new products are added");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();


        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        //SimpleDateFormat currentDate = new SimpleDateFormat("MM DD, YYYY");
        //saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        //SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        //        saveCurrentTime = currentTime.format(calendar.getTime());

        productRandomKey = saveCurrentDate + saveCurrentTime; //this wil make ti uniqeKey
        //getlastpathsegent getsimageDeflt name
        final StorageReference filePath = ProductImagesRef.child(ImageUri.getLastPathSegment() + productRandomKey +".jpg");
        //name ofimage storedin storage

        final UploadTask uploadTask = filePath.putFile(ImageUri);

        //if any failure occur
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                String message = e.toString();
                Toast.makeText(AdminAddNewPrdctActivity.this, "Error: "+message, Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                Toast.makeText(AdminAddNewPrdctActivity.this, "Product image added successfully", Toast.LENGTH_SHORT).show();
                //get and store the link of image in firestorage.
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception
                    {
                        if (!task.isSuccessful())
                        {
                            throw task.getException();
                        }

                        downloadImageUrl = filePath.getDownloadUrl().toString();
                        return filePath.getDownloadUrl();

                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task)
                    {
                        if (task.isSuccessful())
                        {
                            downloadImageUrl = task.getResult().toString();

                            Toast.makeText(AdminAddNewPrdctActivity.this, "product image url successfully got", Toast.LENGTH_SHORT).show();

                            SaveProductInfoToDatabase();
                        }
                    }
                });
            }
        });
    }

    private void SaveProductInfoToDatabase()
    {
        HashMap<String, Object> productMap = new HashMap<>();
        productMap.put("pid",productRandomKey);
        productMap.put("date", saveCurrentDate);
        productMap.put("time",saveCurrentTime);
        productMap.put("description", Description);
        productMap.put("image",downloadImageUrl);
        productMap.put("category", CategoryName);
        productMap.put("price", Price);
        productMap.put("pname", Pname);

        ProductsRef.child(productRandomKey).updateChildren(productMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if(task.isSuccessful())
                        {
                            Intent intent = new Intent(AdminAddNewPrdctActivity.this, AdminCatgryActivity.class);
                            startActivity(intent);

                            loadingBar.dismiss();
                            Toast.makeText(AdminAddNewPrdctActivity.this, "Product added successfully", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            loadingBar.dismiss();
                            String message = task.getException().toString();
                            Toast.makeText(AdminAddNewPrdctActivity.this, "Error:  "+message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

}

