package com.example.malinha.activities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import com.example.malinha.R;
public class SplashActivity extends BaseActivity {
 private final Handler handler = new Handler(Looper.getMainLooper());
 private final Runnable openHome = () -> { startActivity(new Intent(this, HomeActivity.class)); finish(); };
 @Override protected void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_splash); applyInsets(); }
 @Override protected void onResume() { super.onResume(); handler.postDelayed(openHome, 1200); }
 @Override protected void onPause() { handler.removeCallbacks(openHome); super.onPause(); }
}