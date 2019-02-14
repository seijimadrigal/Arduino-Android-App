package com.example.madri.bleservice;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
public class MainActivity extends AppCompatActivity {
    private String TAG = "MAIN ACTIVITY";
    TextView titleView;
    Button nextBtn;
    Button previousBtn;
    Button playPauseBtn;
    public static boolean tryConnect;
    public static boolean buttonNext = false;
    public static boolean buttonPlay = false;
    public static boolean buttonPrevious = false;
    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onResume() {
        //RESTART THE BLUETOOTH SERVICE //
        Intent startService = new Intent(this, BluetoothService.class);
        startService(startService);
        super.onResume();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        titleView = findViewById(R.id.titleView);
        nextBtn = findViewById(R.id.spotifyBtn);
        previousBtn = findViewById(R.id.previousBtn);
        playPauseBtn = findViewById(R.id.pauseBtn);
        tryConnect = false;

        //START THE BLUETOOTH SERVICE //
        Intent startService = new Intent(this, BluetoothService.class);
        startService(startService);

        // THE BUTTONS ON THE UI //
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonNext = true;
            }
        });
        previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonPrevious = true;
            }
        });
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonPlay = true;
            }
        });
    }
}

