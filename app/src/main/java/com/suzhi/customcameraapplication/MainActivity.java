package com.suzhi.customcameraapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
public class MainActivity extends Activity {
    Camera mCamera;
    private Camera.PictureCallback mPicture;
    private static final String TAG = "CameraApp";
    final int REQUEST_CODE_CAMERA = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int MY_PERMISSIONS_REQUEST_CAMERA = 1000;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission is not granted already.");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                Log.d(TAG, "User has denied the permission previously. Hence showing explanation to the user this time to add more clarity.");
                //TODO: Include logic to show explanation to the user
            } else {
                Log.d(TAG, "User hasn't denied the request before. Requesting permission for the first time.");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            }
        } else {
            Log.d(TAG, "Permission has been already granted.");
        }


        boolean hasCamera = checkCameraHardware(getApplicationContext());
        Log.d(TAG, String.valueOf(hasCamera));

        mCamera = getCameraInstance();
        Log.d(TAG, String.valueOf(mCamera));
        int noOfCameras = Camera.getNumberOfCameras();
        Log.d(TAG, String.valueOf(noOfCameras));

        startCameraPreviewWrapper();

        Button captureButton = findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        //Release the camera
        //releaseCamera(mCamera);
    }


    /*
     * This method checks if the device has camera hardware or not
     */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /*
     * This method returns an instance of the Camera object, if the camera is free and accessible; Else returns null.
     */
    public static Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
        }
        return camera; // returns null if camera is unavailable
    }

    /*
     * Release camera, so that applications can access it, whenever required
     */
    public static void releaseCamera(Camera camera) {
        camera.release();
        Log.d(TAG, "Releasing the camera.");
    }

    /*
     * Create a File for saving an image file
     */
    private static File getOutputMediaFile(int type) {
        File mediaFile = null;
        String state = Environment.getExternalStorageState();
        if (state != null && state.equals(Environment.MEDIA_MOUNTED)) {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CustomCameraApplication");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d(TAG, "Failed to create directory.");
                    return null;
                }
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
            } else {
                return null;
            }
        }
        return mediaFile;
    }


    /*
     * This method starts the camera preview
     */
    private void startCameraPreview() {
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camera_preview);
        Log.d(TAG, "Showing camera preview in the FrameLayout");
        preview.addView(mPreview);

        mPicture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null) {
                    Log.d(TAG, "Error creating media file, check storage permissions");
                    return;
                }

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                }
            }
        };
    }


    /*
     * This is a wrapper method to start the camera preview
     */
    private void startCameraPreviewWrapper() {
        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+
            int hasCameraPermission = checkSelfPermission(Manifest.permission.CAMERA);
            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    showMessageOKCancel("You need to allow access to Camera",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= 23) {
                                        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
                                    }
                                }
                            });
                    return;
                }
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
                return;
            }
        }
        startCameraPreview();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    startCameraPreview();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "CAMERA permission denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

}