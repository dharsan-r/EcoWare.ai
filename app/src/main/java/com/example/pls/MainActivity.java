package com.example.pls;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;



import com.example.pls.ml.Model;

import org.tensorflow.lite.DataType;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    ImageButton camera,gallery,logo, battery;
    ScrollView scrollView;
    ImageView imageView, grey_bin, blue_bin, green_bin,arrow;
    TextView result, bin_name;
    int imageSize = 300;
    String city_name = "Kingston, ON";
    String bin_label = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);
        logo = findViewById(R.id.button3);
        battery = findViewById(R.id.button4);

        result = findViewById(R.id.result);
        bin_name = findViewById(R.id.bin_name);
        imageView = findViewById(R.id.imageView);
        grey_bin = findViewById(R.id.grey_bin);
        blue_bin = findViewById(R.id.blue_bin);
        green_bin = findViewById(R.id.green_bin);
        arrow = findViewById(R.id.arrow);

        scrollView = findViewById(R.id.scrollView);





        camera.setOnClickListener(view -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 3);
                scrollView.scrollTo(0, 0);
                blue_bin.setVisibility(View.GONE);
                green_bin.setVisibility(View.GONE);
                grey_bin.setVisibility(View.GONE);

            }else{
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent, 1);
                scrollView.scrollTo(0, 0);
                blue_bin.setVisibility(View.GONE);
                green_bin.setVisibility(View.GONE);
                grey_bin.setVisibility(View.GONE);
            }
        });
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://github.com/dharsan-r/EcoClean"); // missing 'http://' will cause crashed
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                scrollView.scrollTo(0, 0);
            }
        });
        battery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://www.cityofkingston.ca/resident/garbage-recycling/household/batteries"); // missing 'http://' will cause crashed
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

    }


    public void classifyImage(Bitmap image){
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 300, 300, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize *imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            for(int i = 0; i<imageSize; i++){
                for(int j = 0; j<imageSize; j++){
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();

            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }

            String[] classes = {"Battery",
                    "Cardboard" ,
                    "Food Waste" ,
                    "Glass" ,
                    "Metal" ,
                    "Paper" ,
                    "Plastic" ,
                    "Trash"};
            result.setText(displayGarbageDisposalBin(classes[maxPos]));
            if (bin_label != null){
                bin_name.setText(bin_label);
            }

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == 3){
                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);

            }else{
                assert data != null;
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    public String displayGarbageDisposalBin(String item){
        String bin = "";
        switch(item){

            case "Cardboard":
            case "Plastic" :
            case "Paper":
                bin = "grey recycling";
                break;

            case "Glass":
            case "Metal":
                bin = "blue recycling";
                break;

            case "Food Waste":
                bin = "green compost";
                break;

            case "Trash":
                bin = "garbage";
                break;

            case "Battery":
                bin = "battery";
                break;
        }
        String s = item + "\n" + "City: " + city_name + "\n";
        String j = "\nYou may place " + item + " in the " + bin + " bin.\n";

        if(bin.equals("battery")) {
            arrow.setVisibility(View.VISIBLE);
            s = s + "\nYou may discard batteries at specific locations that allow for battery disposal.\n";
            bin_label = "\nClick the battery button bellow to find battery recycling centers:\n";
            battery.setVisibility(View.VISIBLE);

        }else if(bin.equals("grey recycling")){
            arrow.setVisibility(View.VISIBLE);
            grey_bin.setVisibility(View.VISIBLE);
            s = s+j;
            bin_label = "\nThis is the grey recycling bin:\n";

        }else if(bin.equals("blue recycling")){
            arrow.setVisibility(View.VISIBLE);
            blue_bin.setVisibility(View.VISIBLE);
            s = s+j;
            bin_label = "\nThis is the blue recycling bin:\n";
        }
        else if(bin.equals("green compost")){
            arrow.setVisibility(View.VISIBLE);
            green_bin.setVisibility(View.VISIBLE);
            s = s+j;
            bin_label = "\nThis is the green compost bin:\n";
        }
    return s;
    }
}