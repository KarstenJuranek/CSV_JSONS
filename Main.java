import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static lingolava.Nexus.*;

public class Main
{
    /*
	Konzept der Utility-Klasse Nexus:

	* Nexus ermöglicht Transformationen zwischen CSV/JSON/JEXIS-Strings und
	  Java-Objekten bzw. Nexus-DataNotes (via 'Parsing' und 'Presenting'):
	  + Input ist keine Datei, sondern (Daten-)Text als CharSequence/String;
	    Datenformate sind unabhängig von ihrer Verwaltung/Verarbeitung und ihren
	    Dateiformaten (z.B. kann CSV/JSON/JEXIS in jedem Texteditor erzeugt werden)
	  + Daten-Text/String kann nicht nur aus Datei, sondern auch von Konsole/Editor
	    oder aus Netzwerk usw. stammen (d.h. quellen-/speicher-neutrale Verarbeitung)
      + CSV/JSON/JEXIS-Daten via Dateien (s. Bsp. ganz unten):
        - Lesen von Daten-Strings am einfachsten per Files.readString
        - Schreiben von Daten-Strings am einfachsten per Files.writeString

	* Überblick über Parsing und Presenting (Umkehrfunktionen):
	  + Datenstring =>  Processor-Parsing     => DataNote
	    DataNote =>     Processor-Presenting  => Datenstring
	  + Java-Daten =>   DataNote-Construction => DataNote
	    DataNote =>     DataNote-Conversion   => Java-Daten

	* Processor (CSV/JSON/JEXIS):
	  + Übersetzung zwischen seriellem/'flachem' String und hierarchischer
	    interner Datenstruktur => Serialisierung/Deserialisierung von Nutzdaten
	    (für Java-Objekte aber generell integrierte Objekt-Serialisierung verwenden!)
	  + generischer Daten-Processor mit Unterklassen CSVProcessor/JSONProcessor/
	    JEXISProcessor (JSON-Struktur/Definition ist Teilmenge der allgemeinen
	    JEXIS-Definition mit Bereitstellung zusätzlicher Datentypen)
	  + einheitliches Parsing-Ergebnis als DataNote-Objekt mit einheitlicher
	    Zugriffsschnittstelle unabhängig von der ursprünglichen Datenquelle
	    (Parsing von CSV/JSON/JEXIS-Datenstring oder per DataNote-Konstruktion)

	* DataNote:
	  + Ergebnis für (CSV/JSON/JEXIS)Processor-Parsing und DataNote-Konstruktion:
	    Datenstruktur, die elementare/terminale und komplexe/nonterminale
	    Datenstrukturen zugleich verwalten kann (z.B. Number/Boole vs. Array/Table)
	  + Datenstruktur für neutrale Verwaltung/Verarbeitung und einheitliche
	    Repräsentation/Sicht auf unterschiedliche Datenformate (theoret. weitere)
	  + folgt dem Prinzip der Immutabilität (Unveränderlichkeit), d.h. Änderungen
	    erzeugen neue veränderte DataNote-Intanzen (analog zur String-Verarbeitung)
	*/

    public static void main(String[] args)
    {
        DataNote DN1, DN2, DN3, DN4, DN5, DN6;
        boolean E;

        // ### CSV-Datenformat ###
        {
            /*
            CSV-'Philosophie':
            * systemneutrale Speicherung und Übertragung tabellierter Daten
              als leichtgewichtige Alternative zu 'laberigen' XML-Dialekten
              (geringerer Overhead und Ressourcenverbrauch bei CSV)
            * Tabellen weisen (in der Regel) gleiche Anzahl Spalten pro Zeile
              auf und verfügen optional über Header-Zeile mit Spaltennamen
            * nicht zur (De-)Serialisierung zu verwenden (ebensowenig wie
              Datenbank-Tabellen hierfür geeignet sind)

            CSV-Datentypen:
            * letztlich nur wörtliche String-Daten, die jeweils individuell
              pro Anwendung zu interpretieren sind (CSV ist kein Standard)
            * Achtung: Whitespace (Leerzeichen etc.) gehören zu den Daten, sofern
              Datenfelder nicht quotiert sind und/oder Whitespace entfernt wird
            * CSVProcessor aus LingoLibry kann Daten per nutzer-definierter
              Transformation auch z.B. als JSON-Daten(typen) interpretieren
            */

            System.out.println();
            System.out.println("###########");
            System.out.println("### CSV ###");
            System.out.println("###########");
            System.out.println();

            // CSV-Standard-Processor (Comma/Character-Separated Values)
            CSVProcessor CP = new CSVProcessor();  // 😀 (U+1F600) / 🤡 (U+1F921)
            String CSV = "Hello,World,\uD83D\uDE00\r\n" +       // 2D-Datenstruktur
                         "Hallo,Welt,\uD83E\uDD21";
            List<List<String>>
                L = List.of(List.of("Hello", "World", "\uD83D\uDE00"), // gleiche Daten
                            List.of("Hallo", "Welt", "\uD83E\uDD21")); // als 2D-Stringliste

            DN1 = CP.parse(CSV);        // übliche Vorgehensweise (für Parametrisierungen)
            DN2 = DataNote.byCSV(CSV);  // 'Convenience'-Function (nur für Standardfälle)
            DN3 = new DataNote(L);      // direkte Erzeugung aus Java-Datenstruktur
            E = DN1.equals(DN2) && DN2.equals(DN3);     // true
            System.out.println("CSV-Gleichheit: "+E+"\r\n");

            String
                CSVF = DN1.asCSV(true), // formatiertes CSV (mit Quotes und Whitespace)
                JSON = DN1.asJSON();            // gleiche 2D-Daten im JSON-Format
            List<List<String>> L2 = DN1.asList(Nt -> Nt.asList(DataNote::asString));    // als 2D-Stringliste (alles)
            List<String> L1 = DN1.at(0).asList(DataNote::asString);     // als 1D-Stringliste (nur 0. Zeile)
            String L0 = DN1.at(0).at(0).asString();     // als 0D-String (Einzelwert aus 0. Zeile)

            System.out.println("Unformatierte CSV-Daten:\r\n"+DN1.asCSV());
            System.out.println("Formatierte CSV-Daten:\r\n"+CSVF);  // vertikal
            System.out.println("CSV als JSON:\t"+JSON);             // horizontal
            System.out.println("2D-Stringliste:\t"+L2);
            System.out.println("1D-Stringliste:\t"+L1);
            System.out.println("0D-String:\t\t"+L0);
            System.out.println();

            // Selbstdefinierter TSVProcessor (Tab[ulator]-Separated Values)
            class TSVProcessor extends CSVProcessor
            { public TSVProcessor() { super('\t', '\'', '\\'); } }

            TSVProcessor TP = new TSVProcessor();
            String TSV = "'Hello'\t'World'\t'\uD83D\uDE00'\r\n" + // 2D-Datenstruktur
                         "'Hallo'\t'Welt'\t'\uD83E\uDD21'";       // Quotes nur als Beispiel (hier weglassbar)
            DN4 = TP.parse(TSV);            // übliche Vorgehensweise
            DN5 = DataNote.byCSV(TP, TSV);  // 'Convenience'-Function
            E = DN4.equals(DN5) && DN1.equals(DN4); // true (gleiche Daten wie CSV oben!)
            System.out.println("TSV/CSV-Gleichheit (TAB vs. COMMA): "+E);

            CSVProcessor CPT = new CSVProcessor('\t', '\'', '\\');
            DN6 = CPT.parse(TSV);   // Alternative: direkt konfigurierter CSV-Processor ohne eigene Klasse
            E = DN4.equals(DN6);
            System.out.println("TSV/CSV-Gleichheit (mit/ohne Klasse): "+E);

            // Todo Fragen:
            // * Warum sind die CSV- und TSV-Daten gleich?
            // * Lässt sich XML auf CSV abbilden und wenn ja, wie?
            // * Wie lassen sich allgemein Graphen mit CSV darstellen?
            // * Ist eine CSV-Zeile ein Datentupel? Was hängt dies mit Strukturen relationaler
            //   Datenbanken zusammen? Wie wird eine ganze Datenbank gespeichert?

            // Todo Aufgaben:
            // * Erzeugen Sie eine CSV-Struktur (als CSV-String), die ein (nicht-)
            //   rechteckiges 2D-Array darstellt. Parsen Sie den String anschließend
            //   zu einer weiterverarbeitbaren Datenstruktur. Daten:
            //   - Rechteckig: 	{{1, 2, 3}, {4, 5, 6}}
            //   - Ausgefranst:	{{1, 2, 3}, {4, 5}, {6}}
            //   Greifen Sie die Daten jeweils als 2D-Java-Stringliste ab.
            // * Erzeugen Sie eine CSV-Struktur für eine fiktive Zitatensammlung,
            //   die bekannte Sätze mit ihren Urhebern darstellt.
            //   - Michail Gorbatschow: "Wer zu spät kommt, den bestraft das Leben."
            //	 - Götz von Berlichingen: "Leck mich am Arsch!" (eigtl. "im Arsch")
            //	 Was ist evtl. bei der Darstellung zu beachten?
        }

        // ### JSON-Datenformat ###
        {
            /*
            JSON-'Philosophie':
            * systemneutrale Speicherung und Übertragung strukturierter und
              typisierter Daten als leichtgewichtige Alternative zu 'laberigen'
              XML-Dialekten (geringerer Overhead und Ressourcenverbrauch bei JSON)
            * eher nicht zur (De-)Serialisierung zu verwenden (ebensowenig
              wie CSV oder Datenbank-Tabellen hierfür geeignet sind)
            * JSON-Datentypen sind standardisiert:
              https://www.tutorialspoint.com/json/json_data_types.htm
            */

            System.out.println();
            System.out.println("############");
            System.out.println("### JSON ###");
            System.out.println("############");
            System.out.println();

            // JSON-Standard-Processor (kompatibel zum Standard)
            JSONProcessor JP = new JSONProcessor();  // 😀 (U+1F600) / 🤡 (U+1F921)
            String JSONS = "{\"\uD83D\uDE00\": [128512, \"GRINNING FACE\"]," +  // 2D-Datenstruktur
                           " \"\uD83E\uDD21\": [129313, \"CLOWN FACE\"]}";
            String JSONT =  // alternativ als gleichwertiger Textblock ohne zu escapende Quotes
                """
                {
                    "\uD83D\uDE00": [128512, "GRINNING FACE"],
                    "\uD83E\uDD21": [129313, "CLOWN FACE"]
                }
                """;
            Map<?, ?> M = Map.of("\uD83D\uDE00", List.of(128512, "GRINNING FACE"),  // analoge Java-Map
                                 "\uD83E\uDD21", List.of(129313, "CLOWN FACE"));

            DN1 = JP.parse(JSONS);          // als String
            DN2 = DataNote.byJSON(JSONS);   // 'Convenience'-Function (nur für Standardfälle)
            DN3 = JP.parse(JSONT);          // als Textblock (analog zu String)
            DN4 = DataNote.byJSON(JSONT);   // 'Convenience'-Function (nur für Standardfälle)
            DN5 = new DataNote(M);          // alternativ direkte Erzeugung aus Java-Datenstruktur
            E = DN1.equals(DN2) && DN2.equals(DN3) &&   // inhaltlich alle gleichwertig
                DN3.equals(DN4) && DN4.equals(DN5);
            System.out.println("JSON-Gleichheit: "+E+"\r\n");

            // Selbstdefinierter XJSONProcessor mit erweiterten Zahlen
            // (kein JSON-Standard und nicht alles parsebar von Double, daher optional zuschaltbar):
            // [±]Infinity/[±]NaN, 1_234 (segmentierende Unterstriche), 0b/0q/0o/0x (Zahlensystempräfixe)
            class XJSONProcessor extends JSONProcessor  // eigene Klasse für mehrmalige Verwendung
            { public XJSONProcessor() { super(true); } }    // Extra-Zahlen einschalten

            XJSONProcessor XP = new XJSONProcessor();
            String XJSON = "[1_024, 0x400P0, NaN, Infinity," +  // P: binärer Exponent für Hex
                           "false, true]";                      // Booleans auch als Zahlen nutzbar

            DN1 = XP.parse(XJSON);
            DN2 = DataNote.byJSON(XP, XJSON);
            E = DN1.equals(DN2); // true
            System.out.println("XJSON-Gleichheit: "+E);

            List<Double> L = DN1.asList(Nt -> Nt.asNumber(true).doubleValue());  // Double mit Hex
            E = L.equals(List.of(1024.0, 1024.0, Double.NaN, Double.POSITIVE_INFINITY,
                                 0.0, 1.0));
            System.out.println("Zahlen-Gleichheit: "+E);

            // Todo Fragen:
            // * Wie würden Sie Characters, Datentupel oder Mengen in JSON
            //   modellieren? Was sind die Unterschiede zu JSON-Datentypen?
            // * Wie sollen allgemein Java-Objekte in JSON modelliert werden?
            //   Von welchen Eigenschaften dieser Datentypen hängt die Wahl
            //   der JSON-Struktur ab?
            // * Finden Sie heraus, wie eine formatierte JSON-Ausgabe funktioniert
            //   und was dies bedeutet!
            // * Wie übersetzen Sie XML in JSON? Was ist analog wie zu modellieren?

            // Todo Aufgaben:
            // * Erzeugen Sie eine JSON-Struktur (zunächst als String), die ein
            //   3D-Array (bzw. eine entsprechend verschachtelte Liste) der
            //   Zahlen von 1 bis 8 darstellt, wobei jede Dimension 2 Elemente
            //   enthält (2³ = 8). Parsen Sie den String und prüfen Sie die
            //   entstehende DataNote auf ihre Struktur.
            // * Erzeugen Sie eine JSON-Struktur (zunächst als String), die
            //   obige 8 Daten jeweils mit einem benennendem Key nach dem
            //   Schema <0, 0, 0> bis <1, 1, 1> versieht, die die Indexierung
            //   repräsentiert. Prüfen Sie den String wieder per Parsing.
        }

        // ### CSV und/versus JSON
        {
            System.out.println();
            System.out.println("################");
            System.out.println("### CSV/JSON ###");
            System.out.println("################");
            System.out.println();

            // CSV mit Header (Spaltennamen) und analoges JSON (Keys)
            CSVProcessor CP = new CSVProcessor();
            JSONProcessor JP = new JSONProcessor(true); // (Variable XP von oben hier nicht mehr verfügbar)
            String
                CSV = "U+1F600,\uD83D\uDE00,GRINNING FACE\r\n" +
                      "U+1F921,\uD83E\uDD21,CLOWN FACE",
                JSON = "[{\"Code\": \"U+1F600\", \"Char\": \"\uD83D\uDE00\", \"Name\": \"GRINNING FACE\"}," +
                       " {\"Code\": \"U+1F921\", \"Char\": \"\uD83E\uDD21\", \"Name\": \"CLOWN FACE\"}]";

            DN1 = CP.parse(CSV, "Code,Char,Name"); // Keys pro Spalte hinzugefügt
            DN2 = JP.parse(JSON);
            E = DN1.equals(DN2); // true
            System.out.println("CSV/JSON-Gleichheit: "+E);

            // Typisierung von CSV-Daten analog JSON-Datentypen
            EnumSet<DataNote.DataType> Z = EnumSet.of(DataNote.DataType.Number, DataNote.DataType.Boole, DataNote.DataType.Void);
            BiFunction<DataNote, Boolean, DataNote> // Transformation zur Datentypisierung
                TypeTrans = (Nt, Tg) ->	    // Tg hier unbenutzt (true wenn Key, false wenn Value)
            {
                DataNote Note;
                try						    // in Number/Boole/Void konvertieren,
                {						    // wenn als JSON-Inhalt interpretierbar
                    Note = JP.parse(Nt.asString());     // CSV-Datenfeld als JSON
                    Note = Z.contains(Note.retType()) ? Note : Nt;  // Strings lassen
                }
                catch (Exception ignored) { Note = Nt; }
                return Note;
            };

            String
                CSVT = "null,false,true\r\n12.34E56,Infinity,Hello World",
                JSONT = "[[null,false,true],[12.34E56,Infinity,\"Hello World\"]]";

            DN1 = CP.parse(CSVT, TypeTrans);
            DN2 = JP.parse(JSONT);
            E = DN1.equals(DN2);
            System.out.println("CSV/JSON-Gleichheit: "+E);
        }

        // ### Lesen/Schreiben aus/in Datei (s. Datei mit engl. Namen auf GRIPS) ###
        {
            /*
            System.out.println();
            System.out.println("#################");
            System.out.println("### CSV-Datei ###");
            System.out.println("#################");
            System.out.println();

            try
            {   // Speicherung und Verarbeitung von CSV/JSON komplett vom Dateisystem entkoppelt
                String T = Files.readString(Path.of("C:\\Users\\jr\\Desktop\\PreSurNames\\Surnames.txt"));
                DataNote DN = DataNote.byCSV(T);
                System.out.println("Anzahl Zeilen: "+DN.extent());
            }
            catch (Exception Exce)
            { System.out.println(Exce.getMessage()); }
            */
        }
    }
}
