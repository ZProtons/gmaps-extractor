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
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    
    private WebView webView;
    private TextView coordsDisplay, methodDisplay;
    private Button openOsmandBtn, openOrganicBtn, openMagicBtn, copyBtn, refreshBtn;
    private String coordinates = "";
    private String lastMethod = "Waiting...";
    private SharedPreferences prefs;
    
    private int method1Success = 0;
    private int method2Success = 0;
    private int method3Success = 0;
    private int method4Success = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("G2OPrefs", MODE_PRIVATE);
        applyTheme();
        
        super.onCreate(savedInstanceState);
        
        // Set layout based on theme
        String theme = prefs.getString("theme", "material");
        switch(theme) {
            case "aero":
                setContentView(R.layout.activity_main_aero);
                break;
            case "skeumorphic":
                setContentView(R.layout.activity_main_skeu);
                break;
            default:
                setContentView(R.layout.activity_main);
        }
        
        webView = findViewById(R.id.webView);
        coordsDisplay = findViewById(R.id.coordsDisplay);
        methodDisplay = findViewById(R.id.methodDisplay);
        openOsmandBtn = findViewById(R.id.openOsmandBtn);
        openOrganicBtn = findViewById(R.id.openOrganicBtn);
        openMagicBtn = findViewById(R.id.openMagicBtn);
        copyBtn = findViewById(R.id.copyBtn);
        refreshBtn = findViewById(R.id.refreshBtn);
        
        setupWebView();
        
        copyBtn.setOnClickListener(v -> copyCoordinates());
        openOsmandBtn.setOnClickListener(v -> openInMap("net.osmand.plus", "OsmAnd"));
        openOrganicBtn.setOnClickListener(v -> openInMap("app.organicmaps", "Organic Maps"));
        openMagicBtn.setOnClickListener(v -> openInMap("com.generalmagic.magicearth", "Magic Earth"));
        refreshBtn.setOnClickListener(v -> refreshCoordinates());
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
    
    private void refreshCoordinates() {
        coordinates = "";
        lastMethod = "Refreshing...";
        coordsDisplay.setText("🔄 Refreshing...");
        methodDisplay.setText("Method: Refreshing...");
        
        // Force re-extraction
        if (webView.getUrl() != null) {
            extractCoordinates(webView.getUrl());
        }
        
        Toast.makeText(this, "Refreshing coordinates...", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "🎨 Theme");
        menu.add(0, 2, 0, "📊 Stats");
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case 1:
                showThemeDialog();
                return true;
            case 2:
                showStatsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showThemeDialog() {
        String[] themes = {"Material Light", "Material Dark", "Aero (Windows 7)", "Skeumorphic (iOS 6)"};
        String[] themeKeys = {"material", "material_dark", "aero", "skeumorphic"};
        
        new AlertDialog.Builder(this)
            .setTitle("Choose Theme")
            .setItems(themes, (dialog, which) -> {
                prefs.edit().putString("theme", themeKeys[which]).apply();
                recreate();
            })
            .show();
    }
    
    private void showStatsDialog() {
        String stats = "Extraction Method Success:\n\n" +
                       "Method 1 (URL @): " + method1Success + "\n" +
                       "Method 2 (Place): " + method2Success + "\n" +
                       "Method 3 (Query): " + method3Success + "\n" +
                       "Method 4 (DOM): " + method4Success;
        
        new AlertDialog.Builder(this)
            .setTitle("Extraction Statistics")
            .setMessage(stats)
            .setPositiveButton("OK", null)
            .setNeutralButton("Reset", (d, w) -> {
                method1Success = method2Success = method3Success = method4Success = 0;
                Toast.makeText(this, "Stats reset", Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");
        
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                if (url.startsWith("intent://")) {
                    Toast.makeText(MainActivity.this, "Blocked app redirect", Toast.LENGTH_SHORT).show();
                    return true;
                }
                
                return false;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                extractCoordinates(url);
            }
        });
        
        webView.loadUrl("https://www.google.com/maps");
    }
    
    private boolean method1_UrlPattern(String url) {
        Pattern p = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            setCoords(m.group(1) + "," + m.group(2), "URL @");
            method1Success++;
            return true;
        }
        return false;
    }
    
    private boolean method2_PlacePattern(String url) {
        Pattern p = Pattern.compile("place/[^/]+/@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            setCoords(m.group(1) + "," + m.group(2), "Place");
            method2Success++;
            return true;
        }
        return false;
    }
    
    private boolean method3_QueryParam(String url) {
        Pattern p = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            setCoords(m.group(1) + "," + m.group(2), "Query");
            method3Success++;
            return true;
        }
        return false;
    }
    
    private void method4_DomParsing() {
        String js = "javascript:(function() {" +
            "  try {" +
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
    
    private void extractCoordinates(String url) {
        if (method1_UrlPattern(url)) return;
        if (method2_PlacePattern(url)) return;
        if (method3_QueryParam(url)) return;
        
        method4_DomParsing();
        
        webView.postDelayed(() -> {
            if (webView.getUrl() != null && coordinates.isEmpty()) {
                extractCoordinates(webView.getUrl());
            }
        }, 2000);
    }
    
    private void setCoords(String coords, String method) {
        coordinates = coords;
        lastMethod = method;
        coordsDisplay.setText("📍 " + coordinates);
        methodDisplay.setText("Via: " + method);
    }
    
    public class WebAppInterface {
        @JavascriptInterface
        public void setCoordinatesMethod4(String coords) {
            runOnUiThread(() -> {
                if (coordinates.isEmpty() || !coordinates.equals(coords)) {
                    setCoords(coords, "DOM");
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
            Toast.makeText(this, "📋 Copied: " + coordinates, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, appName + " not installed", Toast.LENGTH_LONG).show();
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
