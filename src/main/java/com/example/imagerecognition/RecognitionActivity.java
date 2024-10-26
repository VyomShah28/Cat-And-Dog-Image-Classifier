package com.example.imagerecognition;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.imagerecognition.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;


public class RecognitionActivity extends Activity {


    ImageView openImageView;
    Uri imagePath;
    TextView title,answerview;
    Button predictButton;
    Bitmap image;
    int dimensions=64;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        openImageView=findViewById(R.id.openImageView);
        title=findViewById(R.id.title);
        predictButton=findViewById(R.id.predictButton);
        answerview=findViewById(R.id.answerview);

        openImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });

        predictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(image!=null) {
                    predict();
                }
            }
        });

    }

    public void openImage(){
        Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(intent,1);
    }
    @Override
    public void onActivityResult(int reqCode,int resCode,Intent Data){
        super.onActivityResult(reqCode,resCode,Data);
        if(reqCode==1 && resCode==RESULT_OK){
            imagePath=Data.getData();
            title.setVisibility(View.INVISIBLE);
            openImageView.setImageURI(imagePath);
            try {

                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imagePath);
                image = Bitmap.createScaledBitmap(image, dimensions, dimensions, false);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    public void predict(){
        try {
            Model model = Model.newInstance(this);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 64, 64, 3}, DataType.FLOAT32);

            ByteBuffer byteBuffer=ByteBuffer.allocate(4*dimensions*dimensions*3);
            byteBuffer.order(ByteOrder.nativeOrder());
            inputFeature0.loadBuffer(byteBuffer);

            int[] intValues=new int[dimensions*dimensions];
            image.getPixels(intValues,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());
            int pixel=0;
            for(int i=0;i<dimensions;i++){
                for(int j=0;j<dimensions;j++){
                    int val=intValues[pixel++]; //rgb value
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * 1.f);
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * 1.f);
                    byteBuffer.putFloat((val & 0xFF) * 1.f);
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            // Releases model resources if no longer used.
            float[]confidence=outputFeature0.getFloatArray();
           // int pos=0;
            //int max=0;
//            for(int i=0;i<confidence.length;i++){
//                if(confidence[i]>max){
//                    pos=i;
//                }
//            }
//            String[]classes={"Dog","Cat"};
//            String ans=classes[pos];

            if(confidence[0]>0.5){
                answerview.setText("Dog");
            }
            else{
                answerview.setText("Cat");
            }
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }

    }
}