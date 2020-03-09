package com.pd.videodownloader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.pd.videodownloader.R;

public class MainActivity extends AppCompatActivity {

    EditText ed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ed=findViewById(R.id.url_ed);

        findViewById(R.id.dwn_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String surl=ed.getText().toString().trim();
                if (surl!= null
                        && (surl.contains("://youtu.be/") || surl.contains("youtube.com/watch?v="))) {
                    startActivity(new Intent(MainActivity.this,DownloadActivity.class)
                            .putExtra("url",surl));
                } else {
                    Toast.makeText(MainActivity.this,"Not a valid YouTube link!", Toast.LENGTH_LONG).show();
                    ed.setError("Enter Valid Youtube Link");
                }

            }
        });
    }
}
