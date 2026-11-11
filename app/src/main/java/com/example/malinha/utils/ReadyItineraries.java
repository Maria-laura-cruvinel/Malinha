package com.example.malinha.utils;

import com.example.malinha.R;
import com.example.malinha.models.Atividade;
import java.util.ArrayList;

/** Editable offline templates. Each call creates independent activities. */
public final class ReadyItineraries {
    private ReadyItineraries() {}

    public static boolean hasTemplate(int destination) {
        return destination == R.string.destination_rio || destination == R.string.destination_paris
            || destination == R.string.destination_tokyo || destination == R.string.catalog_lisbon
            || destination == R.string.destination_santiago || destination == R.string.catalog_salvador
            || destination == R.string.catalog_buenos_aires || destination == R.string.catalog_new_york;
    }

    public static ArrayList<Atividade> create(int destination) {
        ArrayList<Atividade> activities = new ArrayList<>();
        int day = 0;
        if (destination == R.string.destination_rio) {
            addDay(activities, day++, "Pão de Açúcar", "Praia Vermelha", "Pista Cláudio Coutinho", "Mureta da Urca");
            addDay(activities, day++, "Jardim Botânico do Rio de Janeiro", "Parque Lage", "Lagoa Rodrigo de Freitas", "Parque dos Patins");
            addDay(activities, day++, "Cristo Redentor", "Museu Histórico da Cidade do Rio de Janeiro", "Praia do Leblon", "Mirante do Leblon");
            addDay(activities, day++, "Museu do Amanhã", "Museu de Arte do Rio", "Boulevard Olímpico", "AquaRio");
            addDay(activities, day++, "Escadaria Selarón", "Arcos da Lapa", "Catedral Metropolitana do Rio de Janeiro", "Parque das Ruínas");
        } else if (destination == R.string.destination_paris) {
            addDay(activities, day++, "Museu do Louvre", "Jardim das Tulherias", "Palais-Royal", "Place Vendôme");
            addDay(activities, day++, "Trocadéro", "Torre Eiffel", "Champ de Mars", "Pont de Bir-Hakeim");
            addDay(activities, day++, "Museu d’Orsay", "Museu Rodin", "Esplanade des Invalides", "Pont Alexandre III");
            addDay(activities, day++, "Basílica de Sacré-Cœur", "Place du Tertre", "Museu de Montmartre", "Muro dos Eu Te Amo");
            addDay(activities, day++, "Panteão de Paris", "Jardim de Luxemburgo", "Sainte-Chapelle", "Catedral de Notre-Dame de Paris");
            addDay(activities, day++, "Palácio de Versalhes", "Jardins de Versalhes", "Grand Trianon", "Petit Trianon");
        } else if (destination == R.string.destination_tokyo) {
            addDay(activities, day++, "Templo Sensō-ji", "Rua Nakamise", "Portão Kaminarimon", "Parque Sumida");
            addDay(activities, day++, "Santuário Meiji Jingū", "Parque Yoyogi", "Rua Takeshita", "Cruzamento de Shibuya");
            addDay(activities, day++, "Museu Nacional de Tóquio", "Parque Ueno", "Lago Shinobazu", "Mercado Ameyoko");
            addDay(activities, day++, "Mercado Externo de Tsukiji", "Templo Tsukiji Hongan-ji", "Teatro Kabukiza", "Ginza");
            addDay(activities, day++, "Jardim Hamarikyū", "Santuário Shiba Daijingu", "Templo Zōjō-ji", "Torre de Tóquio");
            addDay(activities, day++, "Parque Shinjuku Gyoen", "Santuário Hanazono", "Omoide Yokocho", "Governo Metropolitano de Tóquio");
            addDay(activities, day++, "Museu Edo-Tokyo a Céu Aberto", "Parque Koganei", "Parque Inokashira", "Rua Sunroad de Kichijōji");
        } else if (destination == R.string.catalog_lisbon) {
            addDay(activities, day++, "Praça do Comércio", "Rua Augusta", "Praça do Rossio", "Convento do Carmo");
            addDay(activities, day++, "Mosteiro dos Jerónimos", "Pastéis de Belém", "Padrão dos Descobrimentos", "Torre de Belém");
            addDay(activities, day++, "Castelo de São Jorge", "Miradouro de Santa Luzia", "Sé de Lisboa", "Museu do Fado");
            addDay(activities, day++, "Museu Nacional do Azulejo", "Panteão Nacional", "Feira da Ladra", "Mosteiro de São Vicente de Fora");
            addDay(activities, day++, "Oceanário de Lisboa", "Jardins da Água", "Passeio das Tágides", "Parque do Tejo");
        } else if (destination == R.string.destination_santiago) {
            addDay(activities, day++, "Plaza de Armas", "Catedral Metropolitana de Santiago", "Museu Histórico Nacional do Chile", "Mercado Central de Santiago");
            addDay(activities, day++, "Cerro San Cristóbal", "Jardim Japonês do Parque Metropolitano", "Pátio Bellavista", "La Chascona");
            addDay(activities, day++, "Museu da Memória e dos Direitos Humanos", "Parque Quinta Normal", "Museu Nacional de História Natural do Chile", "Museu Ferroviário de Santiago");
            addDay(activities, day++, "Bairro Lastarria", "Centro Cultural Gabriela Mistral", "Museu Nacional de Belas Artes do Chile", "Parque Forestal");
            addDay(activities, day++, "Cerro Santa Lucía", "Igreja de San Francisco", "Bairro Paris-Londres", "Centro Cultural La Moneda");
        } else if (destination == R.string.catalog_salvador) {
            addDay(activities, day++, "Pelourinho", "Igreja e Convento de São Francisco", "Fundação Casa de Jorge Amado", "Largo do Terreiro de Jesus");
            addDay(activities, day++, "Mercado Modelo", "Elevador Lacerda", "Praça Tomé de Sousa", "Museu da Misericórdia");
            addDay(activities, day++, "Praia do Porto da Barra", "Forte de Santa Maria", "Forte de São Diogo", "Farol da Barra");
            addDay(activities, day++, "Igreja do Bonfim", "Memorial Irmã Dulce", "Forte de Monte Serrat", "Ponta de Humaitá");
            addDay(activities, day++, "Dique do Tororó", "Museu de Arte Moderna da Bahia", "Solar do Unhão", "Praça Dois de Julho");
        } else if (destination == R.string.catalog_buenos_aires) {
            addDay(activities, day++, "Teatro Colón", "Obelisco de Buenos Aires", "Avenida Corrientes", "Palácio Barolo");
            addDay(activities, day++, "Mercado de San Telmo", "Plaza Dorrego", "Museu Histórico Nacional da Argentina", "Parque Lezama");
            addDay(activities, day++, "Caminito", "Fundação Proa", "Museu Benito Quinquela Martín", "La Bombonera");
            addDay(activities, day++, "Cemitério da Recoleta", "Basílica Nuestra Señora del Pilar", "Museu Nacional de Belas Artes da Argentina", "Floralis Genérica");
            addDay(activities, day++, "Rosedal de Palermo", "Jardim Japonês de Buenos Aires", "Jardim Botânico Carlos Thays", "Museu Evita");
        } else if (destination == R.string.catalog_new_york) {
            addDay(activities, day++, "Central Park", "Bethesda Terrace", "Strawberry Fields", "Columbus Circle");
            addDay(activities, day++, "Metropolitan Museum of Art", "Museu Solomon R. Guggenheim", "Neue Galerie New York", "Carl Schurz Park");
            addDay(activities, day++, "Ponte do Brooklyn", "DUMBO", "Brooklyn Bridge Park", "Brooklyn Heights Promenade");
            addDay(activities, day++, "High Line", "Chelsea Market", "Whitney Museum of American Art", "Little Island");
            addDay(activities, day++, "Balsa de Staten Island", "Battery Park", "Museu Nacional do Índio Americano em Nova York", "Memorial do 11 de Setembro");
        }
        return activities;
    }

    private static void addDay(ArrayList<Atividade> activities, int day, String... stops) {
        int[] startHours = {9, 11, 14, 16};
        for (int index = 0; index < stops.length; index++) {
            addStop(activities, day, startHours[index], stops[index]);
        }
    }

    private static void addStop(ArrayList<Atividade> activities, int day, int start, String name) {
        Atividade activity = new Atividade();
        activity.day = day;
        activity.startMinutes = start * 60;
        activity.endMinutes = (start + 1) * 60;
        activity.name = name;
        activities.add(activity);
    }
}
