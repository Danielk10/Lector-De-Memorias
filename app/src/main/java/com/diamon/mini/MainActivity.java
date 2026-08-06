package com.diamon.mini;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.diamon.mini.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'mini' library on application startup.
    static {
        System.loadLibrary("mini");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'mini' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}