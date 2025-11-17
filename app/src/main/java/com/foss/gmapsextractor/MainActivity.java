package com.foss.gmapsextractor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    
    private WebView webView;
    private TextView coordsDisplay, methodDisplay;
    private Button openOsmandBtn, openOrganicBtn, openMagicBtn, copyBtn;
    private String coordinates = "";
    private String lastMethod = "Waiting...";
    private SharedPreferences prefs;
    
    // Extraction methods counter
    private int method1Success = 0;
    private int method2Success = 0;
    private int method3Success = 0;
    private int method4Success = 0;
    private int method5Success = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("G2OPrefs", MODE_PRIVATE);
        applyTheme();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        webView = findViewById(R.id.webView);
        coordsDisplay = findViewById(R.id.coordsDisplay);
        methodDisplay = findViewById(R.id.methodDisplay);
        openOsmandBtn = findViewById(R.id.openOsmandBtn);
        openOrganicBtn = findViewById(R.id.openOrganicBtn);
        openMagicBtn = findViewById(R.id.openMagicBtn);
        copyBtn = findViewById(R.id.copyBtn);
        
        setupWebView();
        
        copyBtn.setOnClickListener(v -> copyCoordinates());
        openOsmandBtn.setOnClickListener(v -> openInMap("net.osmand.plus", "OsmAnd"));
        openOrganicBtn.setOnClickListener(v -> openInMap("app.organicmaps", "Organic Maps"));
        openMagicBtn.setOnClickListener(v -> openInMap("com.generalmagic.magicearth", "Magic Earth"));
    }
    
    private void applyTheme() {
        String theme = prefs.getString("theme", "material");
        switch(theme) {
            case "aero":
                setTheme(R.style.AeroTheme);
                break;
            case "skeumorphic":
                setTheme(R.style.SkeumorphicTheme);
                break;
            case "material_dark":
                setTheme(R.style.MaterialTheme_Dark);
                break;
            default:
                setTheme(R.style.MaterialTheme);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Theme");
        menu.add(0, 2, 0, "API Key");
        menu.add(0, 3, 0, "Stats");
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case 1:
                showThemeDialog();
                return true;
            case 2:
                showApiKeyDialog();
                return true;
            case 3:
                showStatsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showThemeDialog() {
        String[] themes = {"Material", "Material Dark", "Aero (Glossy)", "Skeumorphic"};
        String[] themeKeys = {"material", "material_dark", "aero", "skeumorphic"};
        
        new AlertDialog.Builder(this)
            .setTitle("Choose Theme")
            .setItems(themes, (dialog, which) -> {
                prefs.edit().putString("theme", themeKeys[which]).apply();
                recreate(); // Restart activity to apply theme
            })
            .show();
    }
    
    private void showApiKeyDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter Google Maps API Key");
        input.setText(prefs.getString("api_key", ""));
        
        new AlertDialog.Builder(this)
            .setTitle("Google Maps API Key")
            .setMessage("Optional: For Places API fallback\nGet key from: console.cloud.google.com")
            .setView(input)
            .setPositiveButton("Save", (dialog, which) -> {
                String key = input.getText().toString().trim();
                prefs.edit().putString("api_key", key).apply();
                Toast.makeText(this, "API Key saved", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showStatsDialog() {
        String stats = "Extraction Method Success Rates:\n\n" +
                       "Method 1 (URL @coords): " + method1Success + "\n" +
                       "Method 2 (URL place): " + method2Success + "\n" +
                       "Method 3 (URL query): " + method3Success + "\n" +
                       "Method 4 (DOM parsing): " + method4Success + "\n" +
                       "Method 5 (API fallback): " + method5Success;
        
        new AlertDialog.Builder(this)
            .setTitle("Extraction Stats")
            .setMessage(stats)
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                extractCoordinates(url);
            }
        });
        
        webView.loadUrl("https://www.google.com/maps");
    }
    
    // METHOD 1: URL pattern @lat,lng
    private boolean method1_UrlPattern(String url) {
        Pattern p = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            setCoords(m.group(1) + "," + m.group(2), "URL Pattern @");
            method1Success++;
            return true;
        }
        return false;
    }
    
    // METHOD 2: URL place pattern
    private boolean method2_PlacePattern(String url) {
        Pattern p = Pattern.compile("place/[^/]+/@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            setCoords(m.group(1) + "," + m.group(2), "Place Pattern");
            method2Success++;
            return true;
        }
        return false;
    }
    
    // METHOD 3: Query parameter
    private boolean method3_QueryParam(String url) {
        Pattern p = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            setCoords(m.group(1) + "," + m.group(2), "Query Param");
            method3Success++;
            return true;
        }
        return false;
    }
    
    // METHOD 4: JavaScript DOM parsing
    private void method4_DomParsing() {
        String js = "javascript:(function() {" +
            "  try {" +
            "    var metas = document.getElementsByTagName('meta');" +
            "    for(var i=0; i<metas.length; i++) {" +
            "      if(metas[i].getAttribute('property') === 'og:latitude') {" +
            "        var lat = metas[i].getAttribute('content');" +
            "        var lng = document.querySelector('meta[property=\"og:longitude\"]').getAttribute('content');" +
            "        if(lat && lng) { Android.setCoordinatesMethod4(lat + ',' + lng); return; }" +
            "      }" +
            "    }" +
            "    var scripts = document.getElementsByTagName('script');" +
            "    for(var i=0; i<scripts.length; i++) {" +
            "      var text = scripts[i].textContent;" +
            "      var match = text.match(/\\[null,null,(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)\\]/);" +
            "      if(match) { Android.setCoordinatesMethod4(match[1] + ',' + match[2]); return; }" +
            "    }" +
            "  } catch(e) {}" +
            "})()";
        
        webView.evaluateJavascript(js, null);
    }
    
    // METHOD 5: Google Places API fallback
    private void method5_ApiCall(String placeId) {
        String apiKey = prefs.getString("api_key", "");
        if (apiKey.isEmpty()) return;
        
        new Thread(() -> {
            try {
                String urlStr = "https://maps.googleapis.com/maps/api/place/details/json?place_id=" 
                              + placeId + "&key=" + apiKey;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JSONObject json = new JSONObject(response.toString());
                JSONObject location = json.getJSONObject("result")
                                          .getJSONObject("geometry")
                                          .getJSONObject("location");
                String lat = location.getString("lat");
                String lng = location.getString("lng");
                
                runOnUiThread(() -> {
                    setCoords(lat + "," + lng, "API Fallback");
                    method5Success++;
                });
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void extractCoordinates(String url) {
        // Try all methods in order
        if (method1_UrlPattern(url)) return;
        if (method2_PlacePattern(url)) return;
        if (method3_QueryParam(url)) return;
        
        // Method 4: DOM parsing (async)
        method4_DomParsing();
        
        // Method 5: Extract place_id and try API
        Pattern placeIdPattern = Pattern.compile("place_id:([^,&]+)");
        Matcher m = placeIdPattern.matcher(url);
        if (m.find()) {
            method5_ApiCall(m.group(1));
        }
        
        // Re-check every 2 seconds
        webView.postDelayed(() -> extractCoordinates(webView.getUrl()), 2000);
    }
    
    private void setCoords(String coords, String method) {
        coordinates = coords;
        lastMethod = method;
        coordsDisplay.setText("📍 " + coordinates);
        methodDisplay.setText("Method: " + method);
        coordsDisplay.setVisibility(View.VISIBLE);
        methodDisplay.setVisibility(View.VISIBLE);
    }
    
    public class WebAppInterface {
        @JavascriptInterface
        public void setCoordinatesMethod4(String coords) {
            runOnUiThread(() -> {
                if (coordinates.isEmpty() || !coordinates.equals(coords)) {
                    setCoords(coords, "DOM Parsing");
                    method4Success++;
                }
            });
        }
    }
    
    private void copyCoordinates() {
        if (!coordinates.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("coordinates", coordinates);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied: " + coordinates, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No coordinates yet", Toast.LENGTH_SHORT).show();
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
