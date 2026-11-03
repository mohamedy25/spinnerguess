package com.example.spinnerguess;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePic;
    private EditText etProfilePhone, etProfileAddress;
    private Button btnSelectImage, btnSaveProfile, btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ivProfilePic = findViewById(R.id.ivProfilePic);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        etProfileAddress = findViewById(R.id.etProfileAddress);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnBack = findViewById(R.id.btnBack);

        // launcher لاختيار صورة من الاستوديو
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        ivProfilePic.setImageURI(uri);
                    }
                }
        );

        // launcher لالتقاط صورة بالكاميرا
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        ivProfilePic.setImageBitmap(bitmap);
                        selectedImageUri = getImageUriFromBitmap(bitmap);
                    }
                }
        );

        btnSelectImage.setOnClickListener(v -> showImageImportDialog());
        btnSaveProfile.setOnClickListener(v -> saveProfileData());
        btnBack.setOnClickListener(v -> finish());

        loadUserProfile();
    }

    // دايلوج للاختيار بين الكاميرا والاستوديو
    private void showImageImportDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                cameraLauncher.launch(null); // فتح الكاميرا
            } else if (which == 1) {
                galleryLauncher.launch("image/*"); // فتح المعرض
            }
        });
        builder.show();
    }

    // تحويل الصورة الملتقطة بالكاميرا (Bitmap) إلى Uri لرفعها على Firebase
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfilePic_" + System.currentTimeMillis(), null);
        return Uri.parse(path);
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (documentSnapshot.exists()) {
                        String phone = documentSnapshot.getString("phone");
                        String address = documentSnapshot.getString("address");
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        if (phone != null) etProfilePhone.setText(phone);
                        if (address != null) etProfileAddress.setText(address);

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(ProfileActivity.this)
                                    .load(imageUrl)
                                    .placeholder(android.R.drawable.ic_menu_camera)
                                    .into(ivProfilePic);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(ProfileActivity.this, "Error loading: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String phone = etProfilePhone.getText().toString().trim();
        String address = etProfileAddress.getText().toString().trim();

        if (selectedImageUri != null) {
            StorageReference imageRef = storage.getReference().child("profile_images/" + uid + ".jpg");
            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveToFirestore(uid, phone, address, uri.toString());
                    }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            saveToFirestore(uid, phone, address, null);
        }
    }

    private void saveToFirestore(String uid, String phone, String address, String imageUrl) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("phone", phone);
        userData.put("address", address);
        if (imageUrl != null) {
            userData.put("imageUrl", imageUrl);
        }

        // حفظ البيانات في مجموعة users مع استخدام معرّف المשתخدم (uid)
        firestore.collection("users").document(uid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}