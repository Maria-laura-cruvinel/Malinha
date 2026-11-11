package com.example.malinha;
import com.example.malinha.models.Atividade;
import com.example.malinha.utils.ItineraryUtils;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class ItineraryUtilsTest {
 @Test public void reorderingNeverMovesActivitiesAcrossDays() {
  ArrayList<Atividade> all=new ArrayList<>();
  Atividade a=new Atividade(),b=new Atividade(),c=new Atividade();
  b.day=1;all.add(a);all.add(b);all.add(c);
  assertTrue(ItineraryUtils.move(all,c.id,-1));
  assertSame(c,all.get(0));assertSame(b,all.get(1));assertSame(a,all.get(2));
  assertFalse(ItineraryUtils.move(all,c.id,-1));
  assertEquals(2,ItineraryUtils.forDay(all,0).size());
  assertEquals(1,ItineraryUtils.forDay(all,1).size());
  assertTrue(ItineraryUtils.forDay(all,2).isEmpty());
  assertTrue(ItineraryUtils.move(all,c.id,1));assertSame(a,all.get(0));
 }
 @Test public void daysIncludeBothEndsAcrossDaylightSaving() {
  TimeZone previous=TimeZone.getDefault();
  try {
   TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
   Calendar start=Calendar.getInstance();start.clear();start.set(2026,Calendar.MARCH,7,12,0);
   Calendar end=(Calendar)start.clone();end.add(Calendar.DAY_OF_MONTH,2);
   assertEquals(3,ItineraryUtils.days(start.getTimeInMillis(),end.getTimeInMillis()).size());
   assertEquals(1,ItineraryUtils.days(start.getTimeInMillis(),start.getTimeInMillis()).size());
  } finally {TimeZone.setDefault(previous);}
 }
 @Test public void formatsTimeWithLeadingZeros(){assertEquals("09:05",ItineraryUtils.time(545));assertEquals("23:59",ItineraryUtils.time(1439));}
}
