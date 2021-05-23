package com.app.cameraxdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    ConstraintLayout camera_ui_container,camera_container;
    ImageButton camera_switch_button,camera_capture_button,photo_view_button;
    PreviewView view_finder;
    File outputDirectory;
    ImageCapture imageCapture;
    ImageAnalysis imageAnalyzer;
    Camera camera;
    int displayId=-1;
    ProcessCameraProvider cameraProvider;
    private int PERMISSIONS_REQUEST_CODE = 10;
    private String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    String[] PERMISSIONS_REQUIRED=new String[]{Manifest.permission.CAMERA};
    ExecutorService cameraExecutor;
    int lensFacing = CameraSelector.LENS_FACING_BACK;
    File demoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if(allPermissionsGranted())
        {
            startCamera();
        }
        else
        {
            ActivityCompat.requestPermissions(this,PERMISSIONS_REQUIRED,PERMISSIONS_REQUEST_CODE);
        }
//        updateCameraUi();
        outputDirectory=getOutputDirectory();
        Log.e("output_directory",outputDirectory+"");
        cameraExecutor = Executors.newSingleThreadExecutor();


    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateCameraUi();
        updateCameraSwitchButton();
    }

    void initView()
    {
        camera_ui_container=findViewById(R.id.camera_ui_container);
        camera_container=findViewById(R.id.camera_container);
        camera_switch_button=findViewById(R.id.camera_switch_button);
        camera_capture_button=findViewById(R.id.camera_capture_button);
        view_finder=findViewById(R.id.view_finder);
        camera_capture_button.setOnClickListener(this);
        camera_switch_button.setOnClickListener(this);
    }
    void setGalleryThumbnail(Uri uri)
    {
     photo_view_button=findViewById(R.id.photo_view_button);
     photo_view_button.setOnClickListener(this);
     photo_view_button.post(new Runnable() {
         @Override
         public void run() {
             photo_view_button.setPadding((int)getResources().getDimension(R.dimen.stroke_small),(int)getResources().getDimension(R.dimen.stroke_small),(int)getResources().getDimension(R.dimen.stroke_small),(int)getResources().getDimension(R.dimen.stroke_small));
             Glide.with(photo_view_button).load(uri).apply(RequestOptions.circleCropTransform()).into(photo_view_button);
         }
     });

    }
    boolean allPermissionsGranted()
    {
        for(String permission:PERMISSIONS_REQUIRED)
        {
            if(ContextCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED)
            {
                return  false;
            }
        }
        return  true;
    }
    void startCamera()
    {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider= cameraProviderFuture.get();
                    Preview preview = new Preview.Builder()
                            .build();
                    preview.setSurfaceProvider(view_finder.getSurfaceProvider());
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();
                    int rotation = view_finder.getDisplay().getRotation();
                    try {
                        if(hasBackCamera())
                        {
                            lensFacing=CameraSelector.LENS_FACING_BACK;
                        }
                        if(hasFrontCamera())
                        {
                            lensFacing=CameraSelector.LENS_FACING_FRONT;

                        }
                    } catch (CameraInfoUnavailableException e) {
                        e.printStackTrace();
                    }
                    updateCameraSwitchButton();

                    imageCapture = new ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            // We request aspect ratio but no resolution to match preview config, but letting
                            // CameraX optimize for whatever specific resolution best fits our use cases
                            // Set initial target rotation, we will have to call this again if rotation changes
                            // during the lifecycle of this use case
                            .setTargetRotation(rotation)
                            .build();


                    // Bind use cases to camera
//                 Camera camera = ((ProcessCameraProvider) cameraProvider).bindToLifecycle((LifecycleOwner)this, cameraSelector, preview);
//                 cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle((LifecycleOwner) MainActivity.this, cameraSelector, preview,imageCapture);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        },ContextCompat.getMainExecutor(this));

    }
    void takePhoto()
    {
        if(imageCapture==null)
        {
            return;
        }

        File photoFile = new File(
                outputDirectory,
                new SimpleDateFormat(FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".jpg");
         demoFile=photoFile;
        ImageCapture.Metadata metadata=new ImageCapture.Metadata();
        if(lensFacing==CameraSelector.LENS_FACING_FRONT)
        {
            metadata.setReversedHorizontal(true);

        }


        ImageCapture.OutputFileOptions outputOptions =new  ImageCapture.OutputFileOptions.Builder(photoFile).setMetadata(metadata).build();
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Toast.makeText(MainActivity.this, "Image Saved Successfully", Toast.LENGTH_SHORT).show();
                if(outputFileResults.getSavedUri()!=null)
                {
                    Log.e("image_uri_not_null",outputFileResults.getSavedUri()+"");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // Update the gallery thumbnail with latest picture taken
                        setGalleryThumbnail(outputFileResults.getSavedUri());
                    }
                }
                else
                {
                    Log.e("image_uri_null", Uri.fromFile(photoFile)+"");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // Update the gallery thumbnail with latest picture taken
                        setGalleryThumbnail(Uri.fromFile(photoFile));
                    }

                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(MainActivity.this, exception.getImageCaptureError(), Toast.LENGTH_SHORT).show();
            }
        });


    }
    File getOutputDirectory()
    {
        return this.getExternalFilesDir(null);
    }


//    private fun getOutputDirectory(): File {
//        val mediaDir = externalMediaDirs.firstOrNull()?.let {
//            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
//        return if (mediaDir != null && mediaDir.exists())
//            mediaDir else filesDir
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
    File createFile(File baseFolder,String format,String extension)
    {
      return new  File(baseFolder, new SimpleDateFormat(format, Locale.US)
              .format(System.currentTimeMillis()) + extension);
    }
    void updateCameraSwitchButton()
    {
        try {
            if(hasBackCamera()&&hasFrontCamera())
            {
                camera_switch_button.setEnabled(true);
            }
        }
        catch (CameraInfoUnavailableException e)
        {
            camera_switch_button.setEnabled(false);
        }
    }
    boolean hasBackCamera() throws CameraInfoUnavailableException {
        if(cameraProvider!=null)
        {
            if(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
            {
               return  true;
            }
            else
            {
                return  false;
            }
        }
        else
        {
            return false;
        }
    }
    /** Returns true if the device has an available front camera. False otherwise */

    boolean hasFrontCamera() throws CameraInfoUnavailableException {
        if(cameraProvider!=null)
        {
            if(cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))
            {
                return  true;
            }
            else
            {
                return  false;
            }
        }
        else
        {
            return false;
        }
    }
    void updateCameraUi()
    {
        updateCameraSwitchButton();
        if(Uri.fromFile(demoFile)!=null)
        {
            setGalleryThumbnail(Uri.fromFile(demoFile));
        }
    }

    @Override
    public void onClick(View view) {
        int id=view.getId();
        if(id==R.id.camera_capture_button)
        {
            takePhoto();
        }
        else if(id==R.id.camera_switch_button)
        {updateCameraSwitchButton();

        }
        else if( id==R.id.photo_view_button)
        {

        }
    }

}