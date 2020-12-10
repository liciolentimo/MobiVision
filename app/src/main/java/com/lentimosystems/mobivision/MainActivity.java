package com.lentimosystems.mobivision;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    ImageView mImageView;
    Button btn_image, btn_text_to_speech;
    TextView txt_image;
    public static int REQUEST_CODE = 123;
    SeekBar mSeekBar;
    TextToSpeech mTextToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.image);
        btn_image = findViewById(R.id.btnImage);
        txt_image = findViewById(R.id.txt_image);
        btn_text_to_speech = findViewById(R.id.btn_text_to_speech);
        mSeekBar = findViewById(R.id.seekbar);

        btn_image.setOnClickListener(view -> chooseImage());

        //Create an object of TextToSpeech class
        mTextToSpeech = new TextToSpeech(getApplicationContext(), i -> {
            if (i == TextToSpeech.SUCCESS) {
                int lang = mTextToSpeech.setLanguage(Locale.US);

                if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(MainActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                } else {
                    //Toast.makeText(MainActivity.this, "Language Supported", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_text_to_speech.setOnClickListener(view -> {
            String data = txt_image.getText().toString();
            if (data.isEmpty()){
                Toast.makeText(MainActivity.this, "No clear text recognized", Toast.LENGTH_SHORT).show();
            } else {
                float speed = (float) mSeekBar.getProgress() / 50;
                if (speed < 0.1) speed = 0.1f;
                mTextToSpeech.setSpeechRate(speed);
                mTextToSpeech.speak(data,TextToSpeech.QUEUE_FLUSH,null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTextToSpeech != null){
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
    }

    private void chooseImage() {
        //Check permission
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //Grant permission
            requestPermissions(new String[]{Manifest.permission.CAMERA},REQUEST_CODE);
        }
        //Open the camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent,REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Extract the image
        Bundle bundle = data.getExtras();
        Bitmap bitmap = (Bitmap) bundle.get("data");
        mImageView.setImageBitmap(bitmap);

        //Create a FirebaseVisionImage object from your image/bitmap.
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVision firebaseVision = FirebaseVision.getInstance();
        FirebaseVisionTextRecognizer firebaseVisionTextRecognizer = firebaseVision.getOnDeviceTextRecognizer();

        //Process the Image
        Task<FirebaseVisionText> task = firebaseVisionTextRecognizer.processImage(firebaseVisionImage);

        task.addOnSuccessListener(firebaseVisionText -> {
            //Set recognized text from image in our TextView
            String text = firebaseVisionText.getText();
            txt_image.setText(text);
        });
        task.addOnFailureListener(e -> {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}