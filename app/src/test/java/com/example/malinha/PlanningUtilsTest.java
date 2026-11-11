package com.example.malinha;
import com.example.malinha.utils.PlanningUtils;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlanningUtilsTest {
 @Test public void acceptsBrazilianAndKeyboardAmounts(){
  assertEquals("1500.50",PlanningUtils.parseBudget("1.500,50").toPlainString());
  assertEquals("1500.50",PlanningUtils.parseBudget("1500.50").toPlainString());
  assertEquals("1500",PlanningUtils.parseBudget("1.500").toPlainString());
  assertEquals("0",PlanningUtils.parseBudget("0").toPlainString());
  assertEquals("12.5",PlanningUtils.parseBudget(" 12,5 ").toPlainString());
 }
 @Test public void rejectsMalformedAndNegativeAmounts(){
  for(String input:new String[]{"","-1","NaN","1,234","12.34,56","1e3",".","2,,0"}){
   try{PlanningUtils.parseBudget(input);fail("Accepted: "+input);}catch(IllegalArgumentException expected){}
  }
 }
}
