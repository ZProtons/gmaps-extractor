#!/bin/bash
# GMaps Coordinate Extractor v2 - With WebView

set -e

echo "Updating project with WebView and better extraction..."

cd ~/Desktop/G2O/GMapsExtractor

# Update MainActivity.java with WebView
cat > app/src/main/java/com/foss/gmapsextractor/MainActivity.java << 'EOF'
package com.foss.gmapsextractor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    
    private WebView webView;
    private TextView coordsDisplay;
    private Button openOsmandBtn, openOrganicBtn, copyBtn;
    private String coordinates = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        webView = findViewById(R.id.webView);
        coordsDisplay = findViewById(R.id.coordsDisplay);
        openOsmandBtn = findViewById(R.id.openOsmandBtn);
        openOrganicBtn = findViewById(R.id.openOrganicBtn);
        copyBtn = findViewById(R.id.copyBtn);
        
        setupWebView();
        
        copyBtn.setOnClickListener(v -> {
            if (!coordinates.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("coordinates", coordinates);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied: " + coordinates, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No coordinates yet", Toast.LENGTH_SHORT).show();
            }
        });
        
        openOsmandBtn.setOnClickListener(v -> openInMap("net.osmand.plus", "OsmAnd"));
        openOrganicBtn.setOnClickListener(v -> openInMap("app.organicmaps", "Organic Maps"));
    }
    
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
        
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inject JavaScript to extract coordinates
                extractCoordinatesFromPage();
            }
        });
        
        // Load Google Maps
        webView.loadUrl("https://www.google.com/maps");
    }
    
    private void extractCoordinatesFromPage() {
        String js = "javascript:(function() {" +
            "  var url = window.location.href;" +
            "  var match1 = url.match(/@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)/);" +
            "  if (match1) {" +
            "    Android.setCoordinates(match1[1] + ',' + match1[2]);" +
            "    return;" +
            "  }" +
            "  var match2 = url.match(/place\\/[^\\/]+\\/@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)/);" +
            "  if (match2) {" +
            "    Android.setCoordinates(match2[1] + ',' + match2[2]);" +
            "    return;" +
            "  }" +
            "  var match3 = url.match(/[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)/);" +
            "  if (match3) {" +
            "    Android.setCoordinates(match3[1] + ',' + match3[2]);" +
            "  }" +
            "})()";
        
        webView.evaluateJavascript(js, null);
        
        // Re-check every 2 seconds
        webView.postDelayed(this::extractCoordinatesFromPage, 2000);
    }
    
    public class WebAppInterface {
        @JavascriptInterface
        public void setCoordinates(String coords) {
            runOnUiThread(() -> {
                coordinates = coords;
                coordsDisplay.setText("📍 " + coordinates);
                coordsDisplay.setVisibility(View.VISIBLE);
            });
        }
    }
    
    private void openInMap(String packageName, String appName) {
        if (coordinates.isEmpty()) {
            Toast.makeText(this, "No coordinates available yet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("geo:" + coordinates + "?z=17"));
            intent.setPackage(packageName);
            startActivity(intent);
        } catch (Exception e) {
            try {
                // Fallback to any map app
                Intent geoIntent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("geo:" + coordinates + "?z=17"));
                startActivity(geoIntent);
            } catch (Exception e2) {
                Toast.makeText(this, appName + " not installed", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
EOF

# Update layout with WebView
cat > app/src/main/res/layout/activity_main.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#FFFFFF">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="8dp"
        android:background="#4CAF50">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="G2O - Maps to OpenStreetMap"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="#FFFFFF"
            android:gravity="center"
            android:paddingBottom="4dp" />

        <TextView
            android:id="@+id/coordsDisplay"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📍 Waiting for coordinates..."
            android:textSize="14sp"
            android:textColor="#FFFFFF"
            android:gravity="center"
            android:padding="8dp"
            android:background="#388E3C"
            android:visibility="visible" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="8dp">

            <Button
                android:id="@+id/copyBtn"
                android:layout_width="0dp"
                android:layout_height="40dp"
                android:layout_weight="1"
                android:text="Copy"
                android:textSize="12sp"
                android:textColor="#FFFFFF"
                android:background="#2196F3"
                android:layout_marginEnd="4dp" />

            <Button
                android:id="@+id/openOsmandBtn"
                android:layout_width="0dp"
                android:layout_height="40dp"
                android:layout_weight="1"
                android:text="OsmAnd"
                android:textSize="12sp"
                android:textColor="#FFFFFF"
                android:background="#FF9800"
                android:layout_marginEnd="4dp" />

            <Button
                android:id="@+id/openOrganicBtn"
                android:layout_width="0dp"
                android:layout_height="40dp"
                android:layout_weight="1"
                android:text="Organic"
                android:textSize="12sp"
                android:textColor="#FFFFFF"
                android:background="#9C27B0" />
        </LinearLayout>
    </LinearLayout>

    <WebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

</LinearLayout>
EOF

# Update AndroidManifest to add INTERNET permission (already there but let's ensure)
cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.foss.gmapsextractor">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="G2O Maps"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="G2O Maps"
            android:configChanges="orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
EOF

echo ""
echo "✅ Updated to version 2 with:"
echo "   - Built-in WebView (browse Google Maps directly)"
echo "   - Auto-extracts coordinates from URL"
echo "   - Buttons for OsmAnd and Organic Maps"
echo "   - Copy coordinates to clipboard"
echo ""
echo "Now commit and push:"
echo "  git add ."
echo "  git commit -m 'v2: Add WebView, better UX, Organic Maps support'"
echo "  git push"
