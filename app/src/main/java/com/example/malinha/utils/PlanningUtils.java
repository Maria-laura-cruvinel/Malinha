package com.example.malinha.utils;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public final class PlanningUtils {
 private PlanningUtils(){}
 public static String date(long timestamp){return new SimpleDateFormat("dd/MM/yyyy",new Locale("pt","BR")).format(new Date(timestamp));}
 /** Accepts 1500, 1500,50, 1500.50 and Brazilian grouping 1.500,50. */
 public static BigDecimal parseBudget(String input){
  String text=input.trim();
  if(text.matches("[0-9]{1,3}(\\.[0-9]{3})+(,[0-9]{1,2})?")) text=text.replace(".","").replace(',','.');
  else if(text.matches("[0-9]+([,.][0-9]{1,2})?")) text=text.replace(',','.');
  else throw new IllegalArgumentException("Invalid budget");
  return new BigDecimal(text);
 }
}
