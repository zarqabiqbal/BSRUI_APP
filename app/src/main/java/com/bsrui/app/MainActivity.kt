package com.bsrui.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()
        open_cameraLive.setOnClickListener{
            if (checkPermissions()) {
                val intent = Intent(this@MainActivity, CameraLiveActivity::class.java)
                startActivity(intent)
            }
        }
        open_cameraClick.setOnClickListener{
            if (checkPermissions()) {
                val intent = Intent(this@MainActivity, CameraClickActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun requestPermissionsforApp()
    {
        ActivityCompat.requestPermissions(this@MainActivity,
            arrayOf( Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
            2)
    }

    fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsforApp()
            return false
//
        }else if(ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissionsforApp()
            return false
        }
        else if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsforApp()
            return false
        }

        return true
    }
}
