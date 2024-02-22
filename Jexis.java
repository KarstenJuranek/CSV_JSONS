import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static lingolava.Nexus.*;
import static lingolava.Nexus.DataNote.*;

public class Jexis
{
    // Hinweis:
    // * Die Bibliothek 'LingoLibry' muss als JAR eingebunden werden
    // * Das JAR enthält sowohl die nutzbaren Packages/Klassen als auch
    //   deren Quelltexte (jeweils 2. Eintrag unter zwei gleichen)

    public static void main(String[] args)
    {
        DataNote DN1, DN2, DN3, NullNote = new DataNote();
        boolean E, E1, E2;

        // ### JEXIS-Datenformat ###
        {
            /*
            JEXIS = "Java/JSON Extended Information Storage" (Java-proprietär):
            * Typen-/Klassen-Hierarchie: Processor -> (CSV/JSON/JEXIS)Processor
            * Funktionalität von Processor und JEXISProcessor identisch
              (aber: Processor analog List/Set/Map generisch verwendbar zum
               einfacheren Austausch des benötigten Prozessors/Datenformats)

            JEXIS-'Philosophie':
            * erweitere Speicherung und Übertragung strukturierter und
              typisierter Daten als leichtgewichtige Alternative zu 'laberigen'
              XML-Dialekten (geringerer Overhead und Ressourcenverbrauch bei JEXIS)
            * eher nicht zur (De-)Serialisierung zu verwenden (ebensowenig
              wie CSV/JSON oder Datenbank-Tabellen hierfür geeignet sind)
            * JEXIS-Datentypen sind eine Obermenge/Erweiterung der JSON-Typen
              (d.h. JSON-Daten können vollständig in JEXIS dargestellt werden):
              + JSON: String/Number/Boole/Void (Simplex) und Array/Table (Complex)
              + JEXIS: JSON plus Char/Label (Simplex) und Option (Complex)
              JEXIS erlaubt zudem beliebige Datentypen als Keys (JSON nur Strings)
            * JEXIS ermöglicht damit beliebige Assoziationen zwischen zwei Daten-
              Einheiten, nicht nur zwischen String (als Variablenname) für einen
              Objekt-Wert (wie in JavaScript bzw. JSON)
            * JEXIS orientiert sich an den wichtigsten (generischen) Standard-
              datentypen von Java: d.h. auch Character/char und Set (zumal eine
              Table bzw. Map aus einem Key-Set und einer Value-Liste besteht)
            */

            System.out.println();
            System.out.println("#############");
            System.out.println("### JEXIS ###");
            System.out.println("#############");
            System.out.println();

            // JEXIS-Standard-Processor (rückwärts-kompatibel zu JSON)
            JEXISProcessor XP = new JEXISProcessor();
            String Jexis =
                """
                {
                    "Simplex":
                    {
                        'S': "Hello World",		# String #
                        'N': 1234.5678E+90,		# Number #
                        'C': '😀',				# Char 😀=U+1F600 #
                        'L': Hello_World,		# Label/Identifier/Tag #
                        'B': true,				# Boole #
                        'V': null				# Void #
                    },
                    "Complex":
                    {					# 👽=U+1F47D, 🌍=U+1F30D, 🐞=U+1F41E #
                        'A': ["HELL👽", "W🌍RLD"],		# List/Array #
                        'O': (null, 1_024),	            # Set/Option #
                        'T': {"🐞": "Ladybug"}	        # Map/Table #
                    }
                }
                """;
            Map<?, ?> M = Map.of("Simplex",
                                 Map.of('S', "Hello World",
                                        'N', 1.2345678E+93,
                                        'C', new DataNote('\uD83D', '\uDE00'),
                                        'L', new DataNote("Hello_World", true),
                                        'B', true,
                                        'V', NullNote   // ('null' nicht in 'Map.of()')
                                       ),
                                 "Complex",
                                 Map.of('A', List.of("HELL👽", "W🌍RLD"),
                                        'O', Set.of(NullNote, 1_024.0),
                                        'T', Map.of("🐞", "Ladybug")
                                       )
                                );

            DN1 = XP.parse(Jexis);          // als Textblock (wie String)
            DN2 = DataNote.byJEXIS(Jexis);  // 'Convenience'-Function (nur für Standardfälle)
            DN3 = new DataNote(M);          // alternativ direkte Erzeugung aus Java-Datenstruktur
            E1 = DN1.equals(DN2);           // inhaltlich gleichwertig
            E2 = DN2.equals(DN3);           // wg. Zahlen ungleichwertig!

            System.out.println("JEXIS-Gleichheit: "+E1+" vs "+E2+"\r\n");

            /*
            Achtung: Zahlen werden vom Processor-Parser nicht normalisiert,
                     da CSV/JSON/JEXIS keine Zahlentypen wie Double etc. kennen;
                     d.h. 1E0 vs. 1.0 vs. 1 sind ungleich, +1 und 1 aber gleich!
                     (vgl. DataNote.byJEXIS("1E0").equals(DataNote.byJEXIS("1.0")); )
            Achtung: Auch Java-Typen 1.0 Double (als Klasse) und 1 Integer (als Klasse)
                     sind ungleich (nicht wg. Werten, sondern wg. Klassen/Typen!)
                     (vgl. auch Double.valueOf(1.0).equals(Integer.valueOf(1)); )
            Hinweis: Die Anzahl der Stellen bei Zahlen in JSON/JEXIS ist unbegrenzt,
                     daher sind keine vorgegebenen/standardisierten Zahlentypen möglich!
            Lösung:  Normalisierung von Zahlen während Parsing/Presenting (s.u.),
                     so dass Zahlen nur normalisiert in CSV/JSON/JEXIS repräsentiert sind
            */
            /* Vergleiche mit BigDecimal (auch keine bestimmte Anzahl Stellen):
            BigDecimal
                BD1 = BigDecimal.ONE,
                BD2 = new BigDecimal("1E0"),
                BD3 = new BigDecimal("1.0");
            System.out.println(BD1.equals(BD2)+", "+BD1.equals(BD3)+", "+BD2.equals(BD3));
            */

            // Zahlennormalisierung (CSV/JSON/JEXIS)
            BiFunction<DataNote, Boolean, DataNote> // Double-Parser auch für Infinity/NaN
                NormTrans = (Nt, Tg) -> Nt.retType() == DataType.Number
                                        ? new DataNote(Double.valueOf(Nt.asString()))
                                        : Nt;
            DN1 = XP.parse(Jexis, NormTrans);   // Transformation nur per Parsing
            E = DN1.equals(DN3);                // DN3 von oben aus Map

            System.out.println("JEXIS-Gleichheit bei Zahlen-Normalisierung: "+E+"\r\n");

            // Umwandlung zu JSON bzgl. nicht-unterstützter JEXIS-Typen (z.B. für Ausgabe)
            BiFunction<DataNote, Boolean, DataNote> // Double-Parser auch für Infinity/NaN
                JSONTrans = (Nt, Tg) ->         // Nt: akt. geparster/erkannter DataNote
            {                                   // Tg: true für Key, false für Value
                if (Nt.isSimplex())             // Nicht-String-Keys und Label/Char-Values
                    return (Tg && Nt.retType() != DataType.String ||    // in Strings wandeln
                            !Tg && Set.of(DataType.Label, DataType.Char).contains(Nt.retType()))
                            ? new DataNote(Nt.asString()) : Nt;
                else
                    if (!Tg)
                        return (Nt.retType() == DataType.Option)
                               ? new DataNote(List.copyOf(Nt.asSet(Function.identity()))) : Nt;
                    else throw new RuntimeException("Incorrect complex key");
            };

            JSONProcessor JP = new JSONProcessor();
            String JSON = JP.present(DN2, JSONTrans);   // DN2 von oben aus JEXIS

            System.out.println("JSON-Text aus JEXIS-Datenformat:\r\n"+JSON+"\r\n");
            /* Hinweis: DN2.asJSON() prüft auf JSON-Konformität und wirft Exception */

            // Todo Fragen:
            // * (Wann) Macht es generell Sinn, in einem JEXIS/JSON-String eine
            //   CSV-Struktur abzulegen und diese z.B. während des JEXIS/JSON-
            //   Parsings in ein 2D-Array umzuwandeln? Was wäre die Alternative?
            // * (Wann) Macht es umgekehrt Sinn, in einem CSV-String JSON/JEXIS
            //   abzulegen und während des CSV-Parsings als JSON/JEXIS-Struktur
            //   zu interpretieren? Gibt es eine Alternative?
        }

        // ### DataNote-Repräsentation ###
        {
            // DataNote repräsentiert alle Datentypen/-strukturen einheitlich:
            // eine DataNote kann beliebig rekursiv verschachtelt werden und
            // enthält selbst andere (nicht) weiter verschachtelnde DataNotes

            // Zugriff auf Daten lesend (nicht-modifizierend) per 'at'
            // (wenn Umwandlung fehlschlägt, dann null oder eigener Default-Wert)
            DataNote Simplex = DN1.at("Simplex");
            String S = Simplex.at('S').asString();  // (analog auch Label)
            Number D = Simplex.at('N').asNumber(true);  // Double
            Number N = Simplex.at('N').asNumber(false); // BigDecimal
            Character C = Simplex.at('C').asChar(); // null, da 😀 supplementär!
            Integer P = Simplex.at('C').asCode();   // Unicode-Punkt
            Boolean B = Simplex.at('B').asBoole();  // auch null möglich
            Boolean V = Simplex.at('V').isNull();   // nur Abfrage, da immer null
            /* Alternativ: String T = DN1.at(List.of("Simplex", 'S')).asString(); */

            E1 = S.equals("Hello World") &&
                 D.doubleValue() == 1.2345678E93 &&
                 N.equals(BigDecimal.valueOf(D.doubleValue())) &&
                 C == null && P.equals(0x1F600) && B && V;

            System.out.println("Daten-Abgriff Simplex: "+E1+"\r\n");

            DataNote Complex = DN1.at("Complex");
            List<String> L = Complex.at('A').asList(DataNote::asString);
            Set<Number> Z = Complex.at('O').asSet(DataNote::asNumber);  // Standard BigDecimal
            Map<String, String> M = Complex.at('T').asMap(DataNote::asString);
            /* Alternativ: für Map auch verschiedene Typen für Key und Value möglich */

            E2 = L.equals(List.of("HELL👽", "W🌍RLD")) &&
                 Z.equals(new HashSet<>(Arrays.asList(null, new BigDecimal("1024.0")))) &&  // Umweg wg. null
                 M.equals(Map.of("🐞", "Ladybug"));

            System.out.println("Daten-Abgriff Complex: "+E2+"\r\n");

            // Todo Fragen:
            // * Finden Sie heraus, was passiert, wenn man z.B. Number als Boole
            //   oder umgekehrt abgreift!
            // * Welche automatischen Typ-Umwandlungen zwischen simplexen und
            //   komplexen Datentypen sind generell sinnvoll/möglich?
            //   - Was ergibt sich, wenn man eine Table als List oder Set abgreift, z.B.:
            //     DataNote.byJEXIS("{A:B}").asList(DataNote::asString) bzw.
            //     DataNote.byJEXIS("{A:B}").asSet(DataNote::asString)
            //   - Was ergibt sich, wenn man ein Array bzw. eine Option als Table abgreift, z.B.:
            //     DataNote.byJEXIS("[A,B]").asMap(DataNote::asString)
            //     DataNote.byJEXIS("(A,B)").asMap(DataNote::asString)

            // Zugriff auf Daten schreibend (modifizierend) per 'at':
            DN3 = DN1.at(List.of("Simplex", 'C'), '☺')  // ☺=U+263A
                     .at(List.of("Complex", 'T', "🐞"), "Lady Beetle");

            System.out.println("Modifiziertes JEXIS:\r\n"+DN3.asJEXIS()+"\r\n");

            // Todo Fragen:
            // * Was passiert, wenn eine Zuweisung/Modifikation auf sich selbst erfolgt, z.B.:
            /*{
                List<Object> AL = new ArrayList<>(List.of(1, 2, 3, 4));
                AL.set(0, AL);          // Liste an Index 0 in sich selbst einfügen
                //System.out.println(AL.hashCode());    // StackOverflow wg. Endlosrekursion!

                DataNote DN = DataNote.byJSON("[1, 2, 3, 4]");
                DN = DN.at(0, DN);  // DataNote-Array/Liste an Index 0 in sich selbst einfügen
                int HC = DN.hashCode();
                String T = DN.asJSON();
                System.out.println(T + " / " + HC+"\r\n");
            }*/
            // * DataNote ist wie String eine unveränderliche (immutable) Datenstruktur:
            //   Wie kann man diese dann trotzdem modifizieren (wie z.B. oben per 'at')?

            // Abfrage der Simplexität/Komplexität bzw. Dimensionalität
            boolean X = DN3.at(List.of("Simplex", 'S')).isComplex(0) &&
                        DN3.at(List.of("Complex", 'A')).isComplex(1) &&
                        DN3.at(List.of("Complex", 'O')).isComplex(1) &&
                        DN3.at(List.of("Complex", 'T')).isComplex(1) &&
                        DataNote.byJSON("[[1,2],[3,4]]").isComplex(2);

            System.out.println("Komplexität: "+X);
            /* Simplex-Werte sind 0-dimensional (inkl. Strings) */
        }

        // Todo: Fragen allgemein
        // * Was ist für folgende Daten besser zur Repräsentation geeignet:
        //   CSV oder JSON (Begründung)?
        //   1) Datenbank/EXCEL-Tabellen
        //   2) 1D-/2D-Arrays/Matrizen
        //   3) Graphen und Bäume mit/ohne Gewichtungen
        //   Wie sähen z.B. Graphen konkret aus?
        // * Lassen sich mit CSV bzw. JSON dieselben Datenstrukturen darstellen
        //   wie mit XML? Welche (Arten von) Daten sind mit CSV bzw. JSON nicht
        //   darstellbar?
        // * Was unterscheidet XML <tag>data</tag> von JSON tag:data?
        //   Was unterscheidet XML name="data" von JSON name:data?
        //   Denken Sie auch an Daten vs. Metadaten und was Keys von Tags/Namen
        //   grundlegend unterscheidet.
        // * Wie könnten allgemein Java-Objekte in JSON/JEXIS modelliert werden?
        //   Von welchen Eigenschaften dieser Datentypen hängt die Wahl der
        //   JSON/JEXIS-Struktur ab?
    }
}
