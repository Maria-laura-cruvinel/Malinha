package com.example.malinha.services;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Small shared transport for the two country-information providers. Worker thread only. */
final class CountryHttp {
    private CountryHttp() {}
    static String get(String address, String token) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("Country provider HTTP " + code);
            StringBuilder body = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[2048]; int count;
                while ((count = reader.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
                    body.append(buffer, 0, count);
                    if (body.length() > 1048576) throw new IOException("Response too large");
                }
            }
            return body.toString();
        } finally { connection.disconnect(); }
    }
    static void validateCountry(String code) {
        if (code == null || !code.matches("[A-Z]{2}")) throw new IllegalArgumentException("Invalid country code");
    }
}
