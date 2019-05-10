package com.example.opencv;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button cameraInit = (Button) findViewById(R.id.cameraInit);
        cameraInit.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        if (PermissionsUtil.isRequestCodeForCamera(requestCode)) {
            if (PermissionsUtil.isCameraGranted(this)) {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                startActivity(EdgeDetection.newIntent(this));
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(final View v) {
        if (PermissionsUtil.isCameraGranted(this)) {
            startActivity(EdgeDetection.newIntent(this));
        } else {
            PermissionsUtil.requestCaneraPermission(this);
        }
    }

    public native String stringFromJNI();
}
