package com.example.malinha;

import android.app.Instrumentation;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.KeyEvent;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.malinha.activities.*;
import com.example.malinha.services.TravelSearchService;
import com.google.android.material.appbar.MaterialToolbar;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TravelMenuTest {
 @Test public void searchLinksPreserveAccentsAndFilters(){
  Uri hotel=TravelSearchService.hotels("São Paulo & Santos",1798804800000L,1798891200000L,3);
  assertEquals("https",hotel.getScheme());assertEquals("www.booking.com",hotel.getHost());
  assertEquals("São Paulo & Santos",hotel.getQueryParameter("ss"));assertEquals("3",hotel.getQueryParameter("group_adults"));
  assertNotEquals(hotel.getQueryParameter("checkin"),hotel.getQueryParameter("checkout"));
  Uri flight=TravelSearchService.flights("São Paulo","Paris",1798804800000L,1798891200000L,2,"Executiva");
  assertEquals("www.google.com",flight.getHost());assertTrue(flight.getQueryParameter("q").contains("2 adultos, classe Executiva"));
  assertEquals("Museu & Café, Paris",TravelSearchService.maps("Museu & Café, Paris").getQueryParameter("query"));
 }
 @Test public void explorePrefillsPlanning(){
  Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
  Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(PlanTripActivity.class.getName(),null,false);
  try(ActivityScenario<ExploreActivity> scenario=ActivityScenario.launch(ExploreActivity.class)){
   instrumentation.waitForIdleSync();
   scenario.onActivity(activity->{
    androidx.recyclerview.widget.RecyclerView list=activity.findViewById(R.id.destinations_list);
    assertTrue(list.getAdapter().getItemCount() > 1);
    ((androidx.recyclerview.widget.LinearLayoutManager) list.getLayoutManager()).scrollToPositionWithOffset(1, 0);
   });
   androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.destination_rio))
       .perform(androidx.test.espresso.action.ViewActions.click());
   androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.discover_use))
       .perform(androidx.test.espresso.action.ViewActions.click());
   android.app.Activity opened=instrumentation.waitForMonitorWithTimeout(monitor,5000);assertNotNull(opened);
   instrumentation.runOnMainSync(()->{
    assertEquals(opened.getString(R.string.destination_rio),((EditText)opened.findViewById(R.id.input_destination)).getText().toString());opened.finish();
   });
  }finally{instrumentation.removeMonitor(monitor);}
 }
 @Test public void menuOpensHelpAndAbout(){
  Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
  try(ActivityScenario<HomeActivity> scenario=ActivityScenario.launch(HomeActivity.class)){
   instrumentation.waitForIdleSync();
   scenario.onActivity(activity->{
    MaterialToolbar toolbar=activity.findViewById(R.id.planning_toolbar);
    assertNotNull(toolbar.getMenu().findItem(R.id.menu_about));
    assertTrue(toolbar.getMenu().performIdentifierAction(R.id.menu_about,0));
   });
   instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);instrumentation.waitForIdleSync();
   scenario.onActivity(activity->{
    MaterialToolbar toolbar=activity.findViewById(R.id.planning_toolbar);
    assertTrue(toolbar.getMenu().performIdentifierAction(R.id.menu_help,0));
   });
   instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
  }
 }
 @Test public void hotelFormSurvivesRecreation(){
  try(ActivityScenario<HotelsActivity> scenario=ActivityScenario.launch(HotelsActivity.class)){
   scenario.onActivity(activity->{((EditText)activity.findViewById(R.id.search_destination)).setText("Recife");((EditText)activity.findViewById(R.id.passengers)).setText("2");});
   scenario.recreate();
   scenario.onActivity(activity->{
    assertEquals("Recife",((EditText)activity.findViewById(R.id.search_destination)).getText().toString());
    assertEquals("2",((EditText)activity.findViewById(R.id.passengers)).getText().toString());
    assertEquals(View.GONE,activity.findViewById(R.id.search_origin).getVisibility());
   });
  }
 }
}
