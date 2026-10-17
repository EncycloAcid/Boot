package com.ty.cleatc;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.View;
import android.view.WindowManager;
public class MainActivity extends Activity {
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    WebView web = new WebView(this);
    WebSettings s = web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
    web.setWebChromeClient(new WebChromeClient()); web.setOverScrollMode(View.OVER_SCROLL_NEVER);
    setContentView(web); web.loadUrl("file:///android_asset/index.html");
  }
}
