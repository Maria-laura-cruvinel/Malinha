package com.example.malinha;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.malinha.activities.MapActivity;
import com.google.android.gms.maps.SupportMapFragment;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/** Opt-in network check. Never prints API credentials or writes itinerary data. */
@RunWith(AndroidJUnit4.class)
public class MapsLiveTest {
 @Test public void mapTilesLoadWithConfiguredKey() throws Exception {
  assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveMaps")));
  Context context=ApplicationProvider.getApplicationContext();
  CountDownLatch loaded=new CountDownLatch(1);
  Intent intent=new Intent(context,MapActivity.class).putExtra("query","Museu do Louvre, Paris");
  try(ActivityScenario<MapActivity> scenario=ActivityScenario.launch(intent)){
   scenario.onActivity(activity->{
    SupportMapFragment fragment=(SupportMapFragment)activity.getSupportFragmentManager().findFragmentById(R.id.map_container);
    assertNotNull("Mapa indisponível: confira chave e Google Play Services",fragment);
    fragment.getMapAsync(map->map.setOnMapLoadedCallback(loaded::countDown));
   });
   assertTrue("O SDK não confirmou o carregamento do mapa em 40 segundos. Verifique rede e autorização da chave.",loaded.await(40,TimeUnit.SECONDS));
  }
 }
}
