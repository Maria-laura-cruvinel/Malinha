package com.example.malinha;
import com.example.malinha.services.FlightPriceService;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class FlightPriceParserTest {
 @Test public void acceptsAirportMatchAndRejectsOtherDatesAndUnsafeLink() throws Exception {
  String row="{\"origin\":\"SAO\",\"origin_airport\":\"GRU\",\"destination\":\"LIS\",\"price\":2345,\"departure_at\":\"2027-01-10T12:00:00Z\",\"return_at\":\"2027-01-17T12:00:00Z\",\"link\":\"https://invalid.example/\"}";
  String body="{\"success\":true,\"currency\":\"brl\",\"queried_at\":\"2026-11-04T12:00:00Z\",\"data\":["+row+"]}";
  FlightPriceService.Result result=FlightPriceService.parse(body,"GRU","LIS","2027-01-10","2027-01-17");
  assertEquals(1,result.offers.size());assertNull(result.offers.get(0).link);
  assertTrue(FlightPriceService.parse(body,"GRU","LIS","2027-01-11","2027-01-17").offers.isEmpty());
  assertTrue(FlightPriceService.parse(body,"GIG","LIS","2027-01-10","2027-01-17").offers.isEmpty());
  try{FlightPriceService.parse(body.replace("brl","usd"),"GRU","LIS","2027-01-10","2027-01-17");fail("Wrong currency accepted");}catch(java.io.IOException expected){}
 }
}
