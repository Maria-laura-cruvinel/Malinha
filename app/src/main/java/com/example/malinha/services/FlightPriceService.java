package com.example.malinha.services;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.net.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Calls our proxy: the Travelpayouts account token never belongs in the APK. */
public class FlightPriceService {
 public static class Offer {
  public BigDecimal price; public String departure, returning, airline, link; public int stops;
 }
 public static class Result { public String queriedAt; public ArrayList<Offer> offers=new ArrayList<>(); }
 public static String endpoint(Context context) {
  try {return context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA).metaData.getString("malinha.FLIGHTS_PROXY_URL","");}
  catch(PackageManager.NameNotFoundException error){return "";}
 }
 public Result fetch(String endpoint,String origin,String destination,String departure,String returning) throws Exception {
  Uri uri=Uri.parse(endpoint);
  if (!"https".equals(uri.getScheme()) && !("http".equals(uri.getScheme()) && "127.0.0.1".equals(uri.getHost()))) throw new IOException("invalid_endpoint");
  URL url=new URL(uri.buildUpon().appendQueryParameter("origin",origin).appendQueryParameter("destination",destination)
   .appendQueryParameter("departure_at",departure).appendQueryParameter("return_at",returning).build().toString());
  HttpURLConnection connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(20000);
  connection.setInstanceFollowRedirects(false);
  try{
   if(connection.getResponseCode()!=200)throw new IOException("HTTP "+connection.getResponseCode());
   StringBuilder json=new StringBuilder();
   try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),StandardCharsets.UTF_8))){
    String line;while((line=reader.readLine())!=null){json.append(line);if(json.length()>1000000)throw new IOException("response_too_large");}
   }
   return parse(json.toString(),origin,destination,departure,returning);
  }finally{connection.disconnect();}
 }
 public static Result parse(String text,String origin,String destination,String departure,String returning) throws Exception {
  JSONObject body=new JSONObject(text);
  if(!body.getBoolean("success") || !"brl".equalsIgnoreCase(body.getString("currency")))throw new IOException("invalid_currency");
  Result result=new Result();result.queriedAt=body.getString("queried_at");
  JSONArray rows=body.getJSONArray("data");
  for(int i=0;i<Math.min(rows.length(),20);i++){
   JSONObject row=rows.getJSONObject(i);
   if((!origin.equals(row.optString("origin")) && !origin.equals(row.optString("origin_airport"))) ||
      (!destination.equals(row.optString("destination")) && !destination.equals(row.optString("destination_airport"))))continue;
   String outbound=row.optString("departure_at"),inbound=row.optString("return_at");
   if(!outbound.startsWith(departure+"T") || !inbound.startsWith(returning+"T"))continue;
   try{
    Offer offer=new Offer();offer.price=new BigDecimal(row.getString("price"));if(offer.price.signum()<=0)continue;
    offer.departure=outbound;offer.returning=inbound;offer.airline=row.optString("airline");offer.stops=row.optInt("transfers",-1);
    String path=row.optString("link");
    if(path.startsWith("/search/") && !path.contains("\\"))offer.link="https://www.aviasales.com"+path;
    result.offers.add(offer);
   }catch(NumberFormatException | JSONException invalid){/* Discard malformed records, never fabricate a price. */}
  }
  result.offers.sort((a,b)->a.price.compareTo(b.price));return result;
 }
}
