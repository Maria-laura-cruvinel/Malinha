package com.example.malinha.services;

import org.json.JSONObject;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** AwesomeAPI public quotes. Call only from a worker thread. No API key required. */
public class CurrencyService {
    public static class Rate {
        public final BigDecimal value;
        public final String date;
        public final boolean derived;
        public Rate(BigDecimal value, String date) { this(value, date, false); }
        public Rate(BigDecimal value, String date, boolean derived) {
            this.value = value; this.date = date; this.derived = derived;
        }
    }
    public Rate fetch(String from, String to) throws Exception {
        validate(from, to);
        if (from.equals(to)) return new Rate(BigDecimal.ONE, "");
        // Fetch only the BRL pairs needed for this conversion, in one request.
        String pairs = from.equals("BRL") ? to + "-BRL" : from + "-BRL";
        if (!from.equals("BRL") && !to.equals("BRL")) pairs += "," + to + "-BRL";
        HttpURLConnection connection = (HttpURLConnection)new URL("https://economia.awesomeapi.com.br/json/last/" + pairs).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("HTTP " + connection.getResponseCode());
            StringBuilder response = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[2048]; int count;
                while ((count = reader.read(buffer)) != -1) {
                    response.append(buffer, 0, count);
                    if (response.length() > 65536) throw new IOException("Unexpected response size");
                }
            }
            return parse(response.toString(), from, to);
        } finally { connection.disconnect(); }
    }
    public static Rate parse(String json, String from, String to) throws Exception {
        validate(from, to);
        if (from.equals(to)) return new Rate(BigDecimal.ONE, "");
        JSONObject object = new JSONObject(json);
        Quote source = from.equals("BRL") ? null : quote(object, from);
        Quote target = to.equals("BRL") ? null : quote(object, to);
        BigDecimal sourceBrl = source == null ? BigDecimal.ONE : source.bid;
        BigDecimal targetBrl = target == null ? BigDecimal.ONE : target.bid;
        BigDecimal rate = sourceBrl.divide(targetBrl, MathContext.DECIMAL128);
        long timestamp = source == null ? target.timestamp : target == null ? source.timestamp
            : Math.min(source.timestamp, target.timestamp);
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss 'UTC'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new Rate(rate, format.format(new Date(timestamp)), source != null && target != null);
    }
    private static void validate(String from, String to) {
        if (from == null || to == null || !from.matches("[A-Z]{3}") || !to.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("Invalid currency");
    }
    private static Quote quote(JSONObject object, String currency) throws Exception {
        JSONObject item = object.getJSONObject(currency + "BRL");
        if (!currency.equals(item.getString("code")) || !"BRL".equals(item.getString("codein")))
            throw new IOException("Unexpected currency pair");
        BigDecimal bid = new BigDecimal(item.getString("bid"));
        long seconds = Long.parseLong(item.getString("timestamp"));
        if (bid.signum() <= 0 || seconds <= 0) throw new IOException("Invalid quote");
        return new Quote(bid, Math.multiplyExact(seconds, 1000L));
    }
    private static final class Quote {
        final BigDecimal bid;
        final long timestamp;
        Quote(BigDecimal bid, long timestamp) { this.bid = bid; this.timestamp = timestamp; }
    }
}
