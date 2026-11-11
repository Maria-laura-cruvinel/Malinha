package com.example.malinha.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;
import com.example.malinha.R;
import com.example.malinha.services.CurrencyService;
import com.example.malinha.utils.PlanningUtils;
import com.google.android.material.appbar.MaterialToolbar;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrencyActivity extends BaseActivity {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private Spinner from,to;
 private EditText amount;
 private TextView result;
 private boolean busy;
 private int generation;
 private String completedText;
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);setContentView(R.layout.activity_currency);applyInsets();
  MaterialToolbar toolbar=findViewById(R.id.planning_toolbar);toolbar.setTitle(R.string.label_exchange);
  toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);toolbar.setNavigationContentDescription(R.string.back);toolbar.setNavigationOnClickListener(v->finish());
  from=findViewById(R.id.currency_from);to=findViewById(R.id.currency_to);amount=findViewById(R.id.currency_amount);result=findViewById(R.id.conversion_result);
  amount.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,"));
  if(state==null)to.setSelection(1);
  findViewById(R.id.convert).setOnClickListener(v->convert());
  AdapterView.OnItemSelectedListener selection=new AdapterView.OnItemSelectedListener(){
   @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){clearResult();}
   @Override public void onNothingSelected(AdapterView<?> parent){}
  };
  from.setOnItemSelectedListener(selection);to.setOnItemSelectedListener(selection);
  amount.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){clearResult();}public void afterTextChanged(Editable s){}});
 }
 private void clearResult(){if(!busy){completedText=null;result.setText(R.string.conversion_empty);}}
 private void setBusy(boolean value){
  busy=value;from.setEnabled(!value);to.setEnabled(!value);amount.setEnabled(!value);findViewById(R.id.convert).setEnabled(!value);
  findViewById(R.id.conversion_progress).setVisibility(value?View.VISIBLE:View.GONE);
 }
 private void convert(){
  final BigDecimal value;
  try{value=PlanningUtils.parseBudget(amount.getText().toString());}
  catch(IllegalArgumentException error){amount.setError(getString(R.string.conversion_value_error));Toast.makeText(this,R.string.conversion_value_error,Toast.LENGTH_LONG).show();return;}
  final String source=from.getSelectedItem().toString(),target=to.getSelectedItem().toString();
  completedText=null;
  if(source.equals(target)){showRate(value,source,target,new CurrencyService.Rate(BigDecimal.ONE,""));return;}
  setBusy(true);result.setText(R.string.converting);
  final int request=++generation;
  worker.execute(()->{
   try{
    CurrencyService.Rate rate=new CurrencyService().fetch(source,target);
    runOnUiThread(()->{if(isDestroyed() || isFinishing() || request!=generation)return;setBusy(false);showRate(value,source,target,rate);});
   }catch(Exception error){
    runOnUiThread(()->{if(isDestroyed() || isFinishing() || request!=generation)return;setBusy(false);result.setText(R.string.conversion_error);Toast.makeText(this,R.string.conversion_error,Toast.LENGTH_LONG).show();});
   }
  });
 }
 private void showRate(BigDecimal amount,String source,String target,CurrencyService.Rate rate){
  NumberFormat money=NumberFormat.getCurrencyInstance(new Locale("pt","BR"));
  Currency currency=Currency.getInstance(target);money.setCurrency(currency);money.setMinimumFractionDigits(currency.getDefaultFractionDigits());money.setMaximumFractionDigits(currency.getDefaultFractionDigits());
  String output=money.format(amount.multiply(rate.value));
  String details=source.equals(target)?getString(R.string.same_currency):getString(R.string.conversion_date,rate.date);
  if(rate.derived)details+="\n"+getString(R.string.conversion_cross);
  completedText=output+"\n\n"+getString(R.string.conversion_rate,source,rate.value.stripTrailingZeros().toPlainString(),target)+"\n"+details;
  result.setText(completedText);
 }
 @Override protected void onDestroy(){generation++;worker.shutdownNow();super.onDestroy();}
}
