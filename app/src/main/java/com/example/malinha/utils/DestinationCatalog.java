package com.example.malinha.utils;

import com.example.malinha.R;
import com.example.malinha.models.Destino;
import java.util.ArrayList;

/** Offline inspiration, not a ranked API result or an exhaustive world directory. */
public final class DestinationCatalog {
    private DestinationCatalog() {}
    private static final java.util.Map<Integer, Discovery> DISCOVERY = new java.util.HashMap<>();
    static {
        DISCOVERY.put(R.string.destination_rio, new Discovery("BR", "1235"));
        DISCOVERY.put(R.string.destination_paris, new Discovery("FR", "24"));
        DISCOVERY.put(R.string.destination_tokyo, new Discovery("JP", "24"));
        DISCOVERY.put(R.string.destination_santiago, new Discovery("CL", "2345"));
        DISCOVERY.put(R.string.catalog_salvador, new Discovery("BR", "124"));
        DISCOVERY.put(R.string.catalog_recife, new Discovery("BR", "124"));
        DISCOVERY.put(R.string.catalog_florianopolis, new Discovery("BR", "135"));
        DISCOVERY.put(R.string.catalog_foz, new Discovery("BR", "35"));
        DISCOVERY.put(R.string.catalog_buenos_aires, new Discovery("AR", "24"));
        DISCOVERY.put(R.string.catalog_lima, new Discovery("PE", "24"));
        DISCOVERY.put(R.string.catalog_mexico, new Discovery("MX", "24"));
        DISCOVERY.put(R.string.catalog_new_york, new Discovery("US", "24"));
        DISCOVERY.put(R.string.catalog_lisbon, new Discovery("PT", "24"));
        DISCOVERY.put(R.string.catalog_rome, new Discovery("IT", "24"));
        DISCOVERY.put(R.string.catalog_london, new Discovery("GB", "24"));
        DISCOVERY.put(R.string.catalog_barcelona, new Discovery("ES", "124"));
        DISCOVERY.put(R.string.catalog_seoul, new Discovery("KR", "24"));
        DISCOVERY.put(R.string.catalog_bangkok, new Discovery("TH", "24"));
        DISCOVERY.put(R.string.catalog_singapore, new Discovery("SG", "234"));
        DISCOVERY.put(R.string.catalog_istanbul, new Discovery("TR", "24"));
        DISCOVERY.put(R.string.catalog_cape_town, new Discovery("ZA", "135"));
        DISCOVERY.put(R.string.catalog_marrakesh, new Discovery("MA", "245"));
        DISCOVERY.put(R.string.catalog_sydney, new Discovery("AU", "123"));
        DISCOVERY.put(R.string.catalog_auckland, new Discovery("NZ", "235"));
    }

    private static final class Discovery {
        final String country, categories;
        Discovery(String country, String categories) {
            this.country = country;
            this.categories = categories;
        }
    }

    public static boolean matchesCategory(Destino destination, int category) {
        return category == 0 || DISCOVERY.get(destination.name).categories.contains(Integer.toString(category));
    }

    public static String countryCode(Destino destination) {
        return DISCOVERY.get(destination.name).country;
    }

    public static String highlights(Destino destination) {
        return HIGHLIGHTS.get(destination.name);
    }

    private static final java.util.Map<Integer, String> HIGHLIGHTS = new java.util.HashMap<>();
    static {
        HIGHLIGHTS.put(R.string.catalog_recife, "Marco Zero · Parque das Esculturas Francisco Brennand");
        HIGHLIGHTS.put(R.string.catalog_florianopolis, "Lagoa da Conceição · Praia da Joaquina");
        HIGHLIGHTS.put(R.string.catalog_foz, "Cataratas do Iguaçu · Parque das Aves");
        HIGHLIGHTS.put(R.string.catalog_lima, "Plaza Mayor · Museu Larco");
        HIGHLIGHTS.put(R.string.catalog_mexico, "Palácio de Belas Artes · Museu Nacional de Antropologia");
        HIGHLIGHTS.put(R.string.catalog_rome, "Coliseu · Fórum Romano");
        HIGHLIGHTS.put(R.string.catalog_london, "Museu Britânico · Tower Bridge");
        HIGHLIGHTS.put(R.string.catalog_barcelona, "Sagrada Família · Parque Güell");
        HIGHLIGHTS.put(R.string.catalog_seoul, "Palácio Gyeongbokgung · Vila Hanok de Bukchon");
        HIGHLIGHTS.put(R.string.catalog_bangkok, "Grande Palácio · Wat Arun");
        HIGHLIGHTS.put(R.string.catalog_singapore, "Gardens by the Bay · Jardim Botânico de Singapura");
        HIGHLIGHTS.put(R.string.catalog_istanbul, "Santa Sofia · Palácio Topkapı");
        HIGHLIGHTS.put(R.string.catalog_cape_town, "Montanha da Mesa · Jardim Botânico de Kirstenbosch");
        HIGHLIGHTS.put(R.string.catalog_marrakesh, "Praça Jemaa el-Fna · Jardim Majorelle");
        HIGHLIGHTS.put(R.string.catalog_sydney, "Ópera de Sydney · Royal Botanic Garden");
        HIGHLIGHTS.put(R.string.catalog_auckland, "Sky Tower · Auckland Domain");
    }

    public static int season(Destino destination) {
        return destination.region == 3 || destination.name == R.string.destination_tokyo
            || destination.name == R.string.catalog_seoul || destination.name == R.string.catalog_new_york
            ? R.string.season_mild : R.string.season_local;
    }
    public static ArrayList<Destino> all() {
        ArrayList<Destino> values = new ArrayList<>();
        values.add(new Destino(R.string.destination_rio, R.string.rio_description, R.drawable.destination_rio, 1));
        values.add(new Destino(R.string.destination_paris, R.string.paris_description, R.drawable.destination_paris, 3));
        values.add(new Destino(R.string.destination_tokyo, R.string.tokyo_description, R.drawable.destination_tokyo, 4));
        values.add(new Destino(R.string.destination_santiago, R.string.santiago_description, R.drawable.destination_santiago, 1));
        add(values, R.string.catalog_salvador, 1); add(values, R.string.catalog_recife, 1);
        add(values, R.string.catalog_florianopolis, 1); add(values, R.string.catalog_foz, 1);
        add(values, R.string.catalog_buenos_aires, 1); add(values, R.string.catalog_lima, 1);
        add(values, R.string.catalog_mexico, 2); add(values, R.string.catalog_new_york, 2);
        add(values, R.string.catalog_lisbon, 3); add(values, R.string.catalog_rome, 3);
        add(values, R.string.catalog_london, 3); add(values, R.string.catalog_barcelona, 3);
        add(values, R.string.catalog_seoul, 4); add(values, R.string.catalog_bangkok, 4);
        add(values, R.string.catalog_singapore, 4); add(values, R.string.catalog_istanbul, 4);
        add(values, R.string.catalog_cape_town, 5); add(values, R.string.catalog_marrakesh, 5);
        add(values, R.string.catalog_sydney, 6); add(values, R.string.catalog_auckland, 6);
        return values;
    }
    private static void add(ArrayList<Destino> values, int name, int region) {
        values.add(new Destino(name, R.string.catalog_general_description, R.drawable.destination_generic, region));
    }
}
