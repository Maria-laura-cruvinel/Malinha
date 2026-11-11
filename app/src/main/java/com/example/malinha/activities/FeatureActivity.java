package com.example.malinha.activities;
import android.os.Bundle;
import android.widget.ImageView;
import android.content.Intent;
import com.example.malinha.R;
import com.google.android.material.appbar.MaterialToolbar;
/** Honest scaffold: feature behavior will be implemented in its scheduled stage. */
public abstract class FeatureActivity extends BaseActivity {
 protected abstract int titleResource();
 protected abstract int iconResource();
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state); setContentView(R.layout.activity_feature); applyInsets();
  MaterialToolbar toolbar=findViewById(R.id.toolbar);
  toolbar.setTitle(titleResource());
  ((ImageView)findViewById(R.id.feature_icon)).setImageResource(iconResource());
  findViewById(R.id.back_home).setOnClickListener(v -> {
   Intent intent = new Intent(this, HomeActivity.class);
   intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
   startActivity(intent); finish();
  });
 }
}