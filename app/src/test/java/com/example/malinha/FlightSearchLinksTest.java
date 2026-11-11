package com.example.malinha;

import com.example.malinha.services.FlightSearchLinks;
import com.example.malinha.services.FlightSearchLinks.Cabin;
import com.example.malinha.services.FlightSearchLinks.Provider;
import java.net.URI;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlightSearchLinksTest {
    private static final String DEPARTURE = "2027-04-01";
    private static final String RETURN = "2027-04-10";

    @Test public void googleEncodesAccentsAndQuerySeparators() {
        URI link = FlightSearchLinks.build(Provider.GOOGLE, "São Paulo & Santos", "Lisboa + arredores",
                DEPARTURE, RETURN, 2, Cabin.BUSINESS);
        assertTrue(link.getRawQuery().contains("S%C3%A3o%20Paulo%20%26%20Santos"));
        assertTrue(link.getRawQuery().contains("Lisboa%20%2B%20arredores"));
        assertTrue(link.getQuery().contains("volta " + RETURN));
        assertFalse(oneWay(Provider.GOOGLE).getQuery().contains("volta"));
    }

    @Test public void providersUseTheirOwnPathAndDateFormat() {
        URI skyscanner = roundTrip(Provider.SKYSCANNER);
        assertEquals("/transporte/passagens-aereas/gru/lis/270401/270410/", skyscanner.getPath());
        assertTrue(skyscanner.getQuery().contains("cabinclass=business"));
        assertEquals("/transporte/passagens-aereas/gru/lis/270401/", oneWay(Provider.SKYSCANNER).getPath());
        assertEquals("/flights/GRU-LIS/2027-04-01/2027-04-10/business/2adults", roundTrip(Provider.KAYAK).getPath());
        assertEquals("/flights/GRU-LIS/2027-04-01/business/2adults", oneWay(Provider.KAYAK).getPath());
        assertEquals("/flight-search/GRU-LIS/2027-04-01/2027-04-10/business/2adults", roundTrip(Provider.MOMONDO).getPath());
        assertEquals("/flight-search/GRU-LIS/2027-04-01/business/2adults", oneWay(Provider.MOMONDO).getPath());
    }

    @Test public void fallbackDoesNotInjectInputIntoProviderPaths() {
        assertEquals(Provider.DECOLAR.landingPage, roundTrip(Provider.DECOLAR).toString());
        assertEquals(Provider.DECOLAR.landingPage, oneWay(Provider.DECOLAR).toString());
        for (Provider provider : new Provider[]{Provider.SKYSCANNER, Provider.KAYAK, Provider.DECOLAR, Provider.MOMONDO}) {
            URI link = FlightSearchLinks.build(provider, "SãO/../", "LIS", DEPARTURE, RETURN, 2, Cabin.BUSINESS);
            assertEquals(provider.landingPage, link.toString());
        }
    }

    private static URI roundTrip(Provider provider) {
        return FlightSearchLinks.build(provider, "gru", "lis", DEPARTURE, RETURN, 2, Cabin.BUSINESS);
    }

    private static URI oneWay(Provider provider) {
        return FlightSearchLinks.build(provider, "gru", "lis", DEPARTURE, null, 2, Cabin.BUSINESS);
    }
}
