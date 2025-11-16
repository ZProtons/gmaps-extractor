package com.foss.gmapsextractor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    
    private EditText inputUrl;
    private TextView resultText;
    private Button extractBtn, copyBtn, openOsmandBtn, clearBtn;
    private String coordinates = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        inputUrl = findViewById(R.id.inputUrl);
        resultText = findViewById(R.id.resultText);
        extractBtn = findViewById(R.id.extractBtn);
        copyBtn = findViewById(R.id.copyBtn);
        openOsmandBtn = findViewById(R.id.openOsmandBtn);
        clearBtn = findViewById(R.id.clearBtn);
        
        // Check if launched from share intent
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                inputUrl.setText(uri.toString());
                extractCoordinates();
            }
        }
        
        extractBtn.setOnClickListener(v -> extractCoordinates());
        
        copyBtn.setOnClickListener(v -> {
            if (!coordinates.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("coordinates", coordinates);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
        
        openOsmandBtn.setOnClickListener(v -> {
            if (!coordinates.isEmpty()) {
                try {
                    // Try OsmAnd geo: intent
                    Intent osmandIntent = new Intent(Intent.ACTION_VIEW, 
                        Uri.parse("geo:" + coordinates + "?z=17"));
                    osmandIntent.setPackage("net.osmand.plus");
                    startActivity(osmandIntent);
                } catch (Exception e) {
                    // Fallback to generic geo: intent
                    try {
                        Intent geoIntent = new Intent(Intent.ACTION_VIEW, 
                            Uri.parse("geo:" + coordinates + "?z=17"));
                        startActivity(geoIntent);
                    } catch (Exception e2) {
                        Toast.makeText(this, "No map app found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        
        clearBtn.setOnClickListener(v -> {
            inputUrl.setText("");
            resultText.setText("Paste Google Maps URL above");
            coordinates = "";
        });
    }
    
    private void extractCoordinates() {
        String url = inputUrl.getText().toString().trim();
        
        if (url.isEmpty()) {
            Toast.makeText(this, "Please paste a URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Pattern 1: @lat,long,zoom format
        Pattern pattern1 = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher matcher1 = pattern1.matcher(url);
        
        if (matcher1.find()) {
            String lat = matcher1.group(1);
            String lon = matcher1.group(2);
            coordinates = lat + "," + lon;
            resultText.setText("Coordinates:\n" + coordinates);
            Toast.makeText(this, "Extracted!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Pattern 2: /place/name/@lat,long format
        Pattern pattern2 = Pattern.compile("place/[^/]+/@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher matcher2 = pattern2.matcher(url);
        
        if (matcher2.find()) {
            String lat = matcher2.group(1);
            String lon = matcher2.group(2);
            coordinates = lat + "," + lon;
            resultText.setText("Coordinates:\n" + coordinates);
            Toast.makeText(this, "Extracted!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Pattern 3: q=lat,long format
        Pattern pattern3 = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher matcher3 = pattern3.matcher(url);
        
        if (matcher3.find()) {
            String lat = matcher3.group(1);
            String lon = matcher3.group(2);
            coordinates = lat + "," + lon;
            resultText.setText("Coordinates:\n" + coordinates);
            Toast.makeText(this, "Extracted!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        resultText.setText("No coordinates found in URL");
        Toast.makeText(this, "Invalid URL format", Toast.LENGTH_SHORT).show();
    }
}
