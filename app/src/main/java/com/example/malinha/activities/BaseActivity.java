package com.example.malinha.activities;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.malinha.R;
/** Shares safe-area handling across screens, including edge-to-edge devices. */
public abstract class BaseActivity extends AppCompatActivity {
 protected void applyInsets() {
  com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.planning_toolbar);
  if (toolbar != null) setSupportActionBar(toolbar);
  android.view.View root = findViewById(R.id.root);
  final int left = root.getPaddingLeft(), top = root.getPaddingTop();
  final int right = root.getPaddingRight(), bottom = root.getPaddingBottom();
  ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, insets) -> {
   androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
   view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
   return WindowInsetsCompat.CONSUMED;
  });
 }
 @Override public boolean onCreateOptionsMenu(android.view.Menu menu) {
  getMenuInflater().inflate(R.menu.app_options, menu); return true;
 }
 @Override public boolean onOptionsItemSelected(android.view.MenuItem item) {
  int title, body;
  if (item.getItemId() == R.id.menu_about) { title = R.string.menu_about; body = R.string.about_body; }
  else if (item.getItemId() == R.id.menu_help) { title = R.string.menu_help; body = R.string.help_body; }
  else return super.onOptionsItemSelected(item);
  androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(title).setMessage(body)
    .setPositiveButton(R.string.close_dialog, null).show();
  if (item.getItemId() == R.id.menu_about) {
   android.widget.TextView message = dialog.findViewById(android.R.id.message);
   if (message != null) android.text.util.Linkify.addLinks(message, android.text.util.Linkify.WEB_URLS);
  }
  return true;
 }
}
