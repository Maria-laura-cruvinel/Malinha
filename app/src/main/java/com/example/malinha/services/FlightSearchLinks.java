package com.example.malinha.services;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** External flight search URL templates. No provider is contacted by this class. */
public final class FlightSearchLinks {
    public enum Cabin {
        ECONOMY("economic", "economy", "econômica"),
        PREMIUM_ECONOMY("premiumeconomy", "premium", "econômica premium"),
        BUSINESS("business", "business", "executiva"),
        FIRST("first", "first", "primeira classe");

        final String skyscanner, kayak, portuguese;
        Cabin(String skyscanner, String kayak, String portuguese) {
            this.skyscanner = skyscanner;
            this.kayak = kayak;
            this.portuguese = portuguese;
        }
    }

    public enum Provider {
        GOOGLE("https://www.google.com/travel/flights?hl=pt-BR"),
        SKYSCANNER("https://www.skyscanner.com.br/"),
        KAYAK("https://www.kayak.com.br/flights"),
        DECOLAR("https://www.decolar.com/passagens-aereas/"),
        MOMONDO("https://www.momondo.com.br/flights");

        public final String landingPage;
        Provider(String landingPage) { this.landingPage = landingPage; }
    }

    private FlightSearchLinks() {}

    public static URI build(Provider provider, String origin, String destination, String departure,
            String returnDate, int adults, Cabin cabin) {
        String from = origin.trim().toUpperCase(Locale.ROOT);
        String to = destination.trim().toUpperCase(Locale.ROOT);
        if (provider == Provider.GOOGLE) {
            String query = "Voos de " + origin.trim() + " para " + destination.trim() + ", ida "
                    + departure + (returnDate == null ? "" : ", volta " + returnDate)
                    + ", " + adults + " adultos, classe " + cabin.portuguese;
            return URI.create(provider.landingPage + "&q=" + encode(query));
        }
        if (!from.matches("[A-Z]{3}") || !to.matches("[A-Z]{3}")
                || !isoDate(departure) || (returnDate != null && !isoDate(returnDate))
                || adults < 1 || adults > 9) return URI.create(provider.landingPage);

        String route = from + "-" + to + "/" + departure + (returnDate == null ? "" : "/" + returnDate);
        switch (provider) {
            case SKYSCANNER:
                String dates = shortDate(departure) + (returnDate == null ? "" : "/" + shortDate(returnDate));
                return URI.create(provider.landingPage + "transporte/passagens-aereas/"
                        + from.toLowerCase(Locale.ROOT) + "/" + to.toLowerCase(Locale.ROOT) + "/" + dates
                        + "/?adultsv2=" + adults + "&cabinclass=" + cabin.skyscanner);
            case KAYAK:
                return URI.create(provider.landingPage + "/" + route + "/" + cabin.kayak + "/" + adults + "adults");
            case MOMONDO:
                return URI.create("https://www.momondo.com.br/flight-search/" + route + "/"
                        + cabin.kayak + "/" + adults + "adults");
            case DECOLAR:
            default:
                // The public landing page is stable. The booking engine path is not a public contract.
                return URI.create(provider.landingPage);
        }
    }

    private static boolean isoDate(String date) { return date != null && date.matches("\\d{4}-\\d{2}-\\d{2}"); }
    private static String shortDate(String date) { return date.substring(2).replace("-", ""); }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
