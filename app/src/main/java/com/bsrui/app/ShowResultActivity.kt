package com.bsrui.app

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_showresults.*
import java.io.File


class ShowResultActivity:AppCompatActivity(){

    val TAG="ShowResultActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_showresults)
        Toast.makeText(this@ShowResultActivity,"Press Back For Exit",Toast.LENGTH_LONG)
        val imageName=intent.getStringExtra("imgName")!!
        val result=intent.getStringExtra("result")!!
        Log.d(TAG,"$result $imageName")

        result_viewclick.setText(result.capitalize())

        val file_path: String = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/Constems_Image_Classifier"
        val dir = File(file_path)
        val imgFile = File(dir,imageName )

        if (imgFile.exists()) {
            val myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath())
            val myImage: ImageView = findViewById<View>(R.id.imgView) as ImageView
            myImage.setImageBitmap(myBitmap)
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}