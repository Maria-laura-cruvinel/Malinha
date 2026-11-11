package com.example.malinha;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.malinha.activities.MapActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
@RunWith(AndroidJUnit4.class)
public class MapsFallbackTest {
 @Test public void missingKeyShowsExternalAlternativeAndSurvivesRecreation() throws Exception {
  Context context=ApplicationProvider.getApplicationContext();
  String key=context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA).metaData.getString("com.google.android.geo.API_KEY","");
  assumeTrue(key.isEmpty() || key.equals("YOUR_API_KEY"));
  Intent intent=new Intent(context,MapActivity.class).putExtra("query","Museu do Louvre, Paris");
  try(ActivityScenario<MapActivity> scenario=ActivityScenario.launch(intent)){
   scenario.onActivity(activity->{
    assertEquals(activity.getString(R.string.map_not_configured),((TextView)activity.findViewById(R.id.map_status)).getText().toString());
    assertEquals(View.GONE,activity.findViewById(R.id.map_container).getVisibility());
    assertTrue(activity.findViewById(R.id.map_directions).isEnabled());
   });
   scenario.recreate();
   scenario.onActivity(activity->assertEquals("Museu do Louvre, Paris",((TextView)activity.findViewById(R.id.map_query)).getText().toString()));
  }
 }
}
