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

public class MainActivity extends Activity {

    private WebView webView;
    private TextView coordsDisplay;
    private Button openOsmandBtn, openOrganicBtn, openMagicBtn, copyBtn;
    private String coordinates = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        coordsDisplay = findViewById(R.id.coordsDisplay);
        openOsmandBtn = findViewById(R.id.openOsmandBtn);
        openOrganicBtn = findViewById(R.id.openOrganicBtn);
        openMagicBtn = findViewById(R.id.openMagicBtn);    // Magic Earth Button
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
        openMagicBtn.setOnClickListener(v -> openInMap("com.generalmagic.magicearth", "Magic Earth"));
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
                extractCoordinatesFromPage();
            }
        });

        webView.loadUrl("https://www.google.com/maps");
    }

    private void extractCoordinatesFromPage() {

        String js =
                "javascript:(function() {" +
                "  var url = window.location.href;" +

                // Pattern 1: @lat,lng
                "  var m1 = url.match(/@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)/);" +
                "  if (m1) { Android.setCoordinates(m1[1] + ',' + m1[2]); return; }" +

                // Pattern 2: /place/.../@lat,lng
                "  var m2 = url.match(/place\\/[^\\/]+\\/@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)/);" +
                "  if (m2) { Android.setCoordinates(m2[1] + ',' + m2[2]); return; }" +

                // Pattern 3: ?q=lat,lng
                "  var m3 = url.match(/[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)/);" +
                "  if (m3) { Android.setCoordinates(m3[1] + ',' + m3[2]); }" +
                "})();";

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
                // fallback to any map app
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("geo:" + coordinates + "?z=17"));
                startActivity(intent);

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

