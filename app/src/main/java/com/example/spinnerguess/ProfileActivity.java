package com.example.spinnerguess;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePic;
    private EditText etProfilePhone, etProfileAddress;
    private Button btnSelectImage, btnSaveProfile, btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private String imageBase64String = null; // النص الذي سيحتوي على الصورة

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ivProfilePic = findViewById(R.id.ivProfilePic);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        etProfileAddress = findViewById(R.id.etProfileAddress);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnBack = findViewById(R.id.btnBack);

        // 1. اختيار صورة من المعرض وتحويلها لـ Base64
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ivProfilePic.setImageURI(uri);
                        imageBase64String = convertUriToBase64(uri);
                    }
                }
        );

        // 2. التقاط صورة بالكاميرا وتحويلها لـ Base64
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        ivProfilePic.setImageBitmap(bitmap);
                        imageBase64String = convertBitmapToBase64(bitmap);
                    }
                }
        );

        // 3. طلب إذن الكاميرا
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        cameraLauncher.launch(null);
                    } else {
                        Toast.makeText(this, "Camera permission required!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnSelectImage.setOnClickListener(v -> showImageImportDialog());
        btnSaveProfile.setOnClickListener(v -> saveProfileData());
        btnBack.setOnClickListener(v -> finish());

        loadUserProfile();
    }

    private void showImageImportDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else if (which == 1) {
                galleryLauncher.launch("image/*");
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // تحويل Bitmap إلى Base64 String
    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // ضغط الصورة حتى لا يتجاوز حجم الحقل 1 ميغابايت في Firestore
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // تحويل Uri إلى Base64 String
    private String convertUriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            return convertBitmapToBase64(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
                        String base64Image = documentSnapshot.getString("imageBase64");

                        if (phone != null) etProfilePhone.setText(phone);
                        if (address != null) etProfileAddress.setText(address);

                        // عرض صورة Base64 بواسطة Glide
                        if (base64Image != null && !base64Image.isEmpty()) {
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Glide.with(ProfileActivity.this)
                                    .load(decodedString)
                                    .placeholder(android.R.drawable.ic_menu_camera)
                                    .into(ivProfilePic);
                        }
                    }
                });
    }

    private void saveProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String phone = etProfilePhone.getText().toString().trim();
        String address = etProfileAddress.getText().toString().trim();

        btnSaveProfile.setEnabled(false);
        Toast.makeText(this, "Saving Profile...", Toast.LENGTH_SHORT).show();

        Map<String, Object> userData = new HashMap<>();
        userData.put("phone", phone);
        userData.put("address", address);

        // إذا تم تغيير الصورة نضع نص Base64 الجديد
        if (imageBase64String != null) {
            userData.put("imageBase64", imageBase64String);
        }

        // حفظ مباشر وموحد في Firestore
        firestore.collection("users").document(uid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}