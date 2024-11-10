package com.example.assignmentfive;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button phototaggerbutton = findViewById(R.id.phototagger);
        Button sketchtaggerbutton = findViewById(R.id.sketchtagger);
        Button storytellerbutton = findViewById(R.id.storyteller);

        phototaggerbutton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PhotoTaggerActivity.class);
            startActivity(intent);
        });

        sketchtaggerbutton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SketchTaggerActivity.class);
            startActivity(intent);
        });

        storytellerbutton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StoryTellerActivity.class);
            startActivity(intent);
        });
    }
}