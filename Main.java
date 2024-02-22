import lingolava.Nexus.*;
import lingolava.Tuple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class Main
{
	// Todo: Achtung evtl. Preview-Features für Java 18 einschalten:
	//	     [File] => [Project Structure]:	Project-Settings => Project =>
	//	     								Language Level: 18 (Preview)
	/*
	Konzept der Utility-Klasse Nexus:
	* Klasse ermöglicht Transformationen zwischen CharSequence/String und
	  Java-/Nexus-Datenstrukturen ('Parsing' und 'Presenting'):
	  + Input ist keine Datei/Dateiname, sondern Text als CharSequence;
	    Datenformate sind unabhängig von Verwaltung/Verarbeitung und
	    Dateiformaten (JSON/CSV kann in jedem Texteditor erzeugt werden)
	  + Text/String kann aus Datei, Konsole/Editor, Netzwerk etc. stammen
	    (quellen-neutrale Verarbeitung)

	* Überblick:
	  + String =>     Processor-Parsing     => DataNote (oder <R>)
	    DataNote =>   Processor-Presenting  => String
	  + Java-Daten => DataNote-Construction => DataNote
	    DataNote =>   DataNote-Conversion   => Java-Daten(struktur)
	
	* Processor:
	  + Übersetzung zwischen seriellem/'flachem' String und hierarchischer
	    Datenstruktur => Serialisierung/Deserialisierung (dafür aber
	    generell Java-intgrierten Mechanismus verwenden!)
	  + generischer Processor mit Unterklassen JSONProcessor/CSVProcessor
	    (JSON-Struktur/Definition ist Teilmenge der allg. Processor-Def.)
	  + einheitliches Ergebnis als DataNote (oder nutzerdef. <R>) mit
	    einheitlicher Zugriffsschnittstelle auf Ergebnisse unabhängig
	    von Datenquelle)
	
	* DataNote:
	  + Standard-Ergebnis für Processor-Parsing und -Construction:
	    Datenstruktur, die elementare/terminale und komplexe/nonterminale
	    Datenstrukturen zugleich beinhalten kann (z.B. Number vs. Array)
	  + Datenstruktur für neutrale Repräsentation und einheitliche Sicht
	    auf unterschiedliche Datenformate
	  + folgt dem Prinzip der Immutabilität (Unveränderlichkeit), d.h.
	    Änderungen erzeugen neue veränderte DataNote (analog String)
	*/
	
	// ### JSON ###
	static public void JSON()
	{
		/*
		JSON-Standardisierung und -Aufbau (s. GRIPS):
		* https://www.json.org/json-en.html
		  https://www.iso.org/standard/71616.html
		* verschiedene existierende JSON-Konzepte/Klassen zur Verarbeitung
		  mit unterschiedlichen Ansätzen (Vergleich):
		  https://www.innoq.com/en/articles/2022/02/java-json/
		
		JSON-'Philosophie':
		* systemneutrale Speicherung und Übertragung strukturierter Daten
		  als leichtgewichtige Alternative zu 'laberigen' XML-Dialekten
		  (geringerer Overhead und Ressourcenverbrauch bei JSON)
		* eher nicht nur (De-)Serialisierung zu verwenden (ebensowenig
		  wie CSV oder Datenbank-Tabellen hierfür geeignet sind)
		* JSON-Datentypen:
		  https://www.tutorialspoint.com/json/json_data_types.htm
		*/
		
		System.out.println("### JSON ###");
		
		// ## Beispiele mit JSON-Processor in Nexus-Klasse aus LingoLibry ##
		JSONProcessor JP = new JSONProcessor();
		DataNote Note; DataNote.DataType Type;
		String S; Number N; double D; boolean B;
		List<DataNote> L; List<Number> LN;
		DataNote[] A; Number[] AN;
		Map<DataNote, DataNote> M;
		Map<String, Number> MN;
		
		// # Simplex-Werte (terminale, nicht rekursiv verschachtelbare Werte) #
		System.out.println("Zahlen:");
		Note = JP.parse("123.456E-789");	// Zahl (beliebig lang)
		Type = Note.retType();					// aktuell erkannter Datentyp
		N = Note.asNumber();					// aus DataNote nach Java-Zahl (BigDecimal)
		D = Note.asNumber().doubleValue();		// (Ergebnis korrekt!)
		S = Note.asString();
		System.out.println("Datentyp: "+Type);
		System.out.println("N="+N+"\t\t"+"D="+D+"\t\t"+"S="+S);
		System.out.println();
		// Frage: Warum kommt für D=0.0 raus, für N aber nicht?
		
		System.out.println("Zeichenketten:");
		Note = JP.parse("\"Hello World\"");	// Quotierung notwendig!
		Type = Note.retType();					// aktuell erkannter Datentyp
		S = Note.asString();
		N = Note.asNumber();					// nicht konvertierbar
		System.out.println("Datentyp: "+Type);
		System.out.println("S="+S+"\t\t"+"N="+N);
		System.out.println();
		// Frage: Wie bekommt man Anführungszeichen innerhalb des JSON-Strings?
		// Frage: Wie kann man Escaping von JSON-Strings in Java-Strings vermeiden?
		
		System.out.println("Wahrheitswerte:");
		Note = JP.parse("false");
		Type = Note.retType();					// aktuell erkannter Datentyp
		B = Note.asBoole();
		N = Note.asNumber();					// konvertierbar
		System.out.println("Datentyp: "+Type);
		System.out.println("B="+B+"\t\t"+"N="+N);
		System.out.println();
		
		System.out.println("Nullwerte:");
		Note = JP.parse("null");
		Type = Note.retType();					// aktuell erkannter Datentyp
		B = Note.isNull();						// 'asNull' nicht sinnvoll, da immer null
		S = Note.asString();					// konvertierbar
		System.out.println("Datentyp: "+Type);
		System.out.println("B="+B+"\t\t"+"S="+S);
		System.out.println();
		
		// # Complex-Werte (nonterminale, rekursiv verschachtelbare Werte) #
		System.out.println("Arrays/Listen 1:");
		Note = JP.parse("[1, 22, 333]");
		Type = Note.retType();					// aktuell erkannter Datentyp
		A = Note.asArray();
		AN = Note.asArray(Number.class, DataNote::asNumber);
		L = Note.asList();
		LN = Note.asList(DataNote::asNumber);
		N = L.get(0).asNumber();
		S = A[1].asString();
		System.out.println("Datentyp: "+Type);
		System.out.println("A="+Arrays.toString(A)+"\t\t"+"L="+L);
		System.out.println("AN="+Arrays.toString(AN)+"\t\t"+"LN="+LN);
		System.out.println("N="+N+"\t\t"+"S="+S);
		System.out.println();
		
		System.out.println("Arrays/Listen 2:");
		Note = JP.parse("[[1, 22], [333, 4444]]");
		Type = Note.retType();					// aktuell erkannter Datentyp
		//Object[] AO = Note.asArray(DataNote[].class, DataNote::asArray);
		Number[][] AM = Note.asArray(Number[].class, DN -> DN.asArray(Number.class, DataNote::asNumber));
		DataNote[][] AD = Note.asArray(DataNote[].class, DataNote::asArray);
		//List<Object> LO = Note.asList(DataNote::asList);
		List<List<Number>> LL = Note.asList(DN -> DN.asList(DataNote::asNumber));
		List<List<DataNote>> LD = Note.asList(DataNote::asList);
		System.out.println("Datentyp: "+Type);
		//System.out.println("AO="+Arrays.deepToString(AO)+"\t\t"+"LO="+LO);
		System.out.println("AM="+Arrays.deepToString(AM)+"\t\t"+"LL="+LL);
		System.out.println("AD="+Arrays.deepToString(AD)+"\t\t"+"LD="+LD);
		System.out.println();
		
		System.out.println("Tables/Maps 1:");
		Note = JP.parse("{\"A\":1, \"BB\":22, \"CCC\":333}");
		Type = Note.retType();					// aktuell erkannter Datentyp
		M = Note.asMap();
		MN = Note.asMap(DataNote::asString, DataNote::asNumber);
		System.out.println("Datentyp: "+Type);
		System.out.println("M="+M+"\n\r"+"MN="+MN);
		System.out.println();
		
		System.out.println("Tables/Maps 2:");
		Note = JP.parse("{\"A\":[null], \"BB\":[false, true], \"CCC\":[1, 22, 333]}");
		Type = Note.retType();					// aktuell erkannter Datentyp
		Map<String, List<String>>
			ML = Note.asMap(DataNote::asString,	// Keys als String, Values ebenfalls alle simplex
							DN -> DN.asList(DataNote::asString));	// und zu String konvertierbar
		System.out.println("Datentyp: "+Type);
		System.out.println("ML="+ML);
		System.out.println();
		
		System.out.println("Array als Table:");
		Note = JP.parse("[\"A\", \"BB\", \"CCC\"]");
		Type = Note.retType();					// aktuell erkannter Datentyp
		Map<Number, String> MA = Note.asMap(DataNote::asNumber, DataNote::asString);
		System.out.println("Datentyp: "+Type);
		System.out.println("MA="+MA);
		System.out.println("Table als Array:");
		Note = JP.parse("{\"1\":\"A\", \"2\":\"BB\", \"3\":\"CCC\"}");
		Type = Note.retType();					// aktuell erkannter Datentyp
		List<String> LA = Note.asList(DataNote::asString);
		System.out.println("Datentyp: "+Type);
		System.out.println("LA="+LA);
		System.out.println();
		
		// ## Beispiel zu Parsing und Transformation in andere Datenstruktur statt DataNote (optional) ##
		class Triad extends Tuple.Triple<DataNote.DataType, List<Integer>, Map<Triad, Triad>>
		{
			public Triad(DataNote.DataType Type, List<Integer> Text, Map<Triad, Triad> Tabl)
			{ super(Type, Text, Tabl); }	// oder statt Text immer null (*)
		}
		System.out.println("Transformation zu Triade/Triple:");
		Triad Data = JP.parse("{\"A\":1, [null, true]}", Triad::new);
		System.out.println(Data);
		System.out.println();
		// Anwendungsbeispiel: Man speichert nur die Datentypen mit
		// neutralen Inhalten in einer Datenstruktur (z.B. Dyade ohne
		// Text oder Triade mit Text=null) und vergleicht dann nur die
		// beiden inhaltslosen Strukturbäume auf Typgleichheiten (s.o. *)
		
		// ## JSON-Aufgaben ##
		// * Erzeugen Sie eine JSON-Struktur (zunächst als String), die ein
		//   3D-Array (bzw. eine entsprechend verschachtelte Liste) der
		//   Zahlen von 1 bis 8 darstellt, wobei jede Dimension 2 Elemente
		//   enthält (2³ = 8). Prüfen Sie den String per Parsing zu einer
		//   DataNote.
		// * Erzeugen Sie eine JSON-Struktur (zunächst als String), die
		//   obige 8 Daten jeweils mit einem benennendem Key nach dem
		//   Schema <0, 0, 0> bis <1, 1, 1> versieht, die die Indexierung
		//   repräsentiert. Prüfen Sie den String wieder per Parsing.
		//
		// ## JSON-Fragen ##
		// * Wie würden Sie Characters, Datentupel oder Mengen in JSON
		//   modellieren? Was sind die Unterschiede zu JSON-Datentypen?
		// * Wie sollen allgemein Java-Objekte in JSON modelliert werden?
		//   Von welchen Eigenschaften dieser Datentypen hängt die Wahl
		//   der JSON-Struktur ab?
		// * Wie übersetzen Sie XML in JSON? Was ist analog wie zu modellieren?
	}
	
	// ### CSV ###
	static public void CSV()
	{
		/*
		CSV-'Recommendation'/RFC und -Aufbau (s. GRIPS):
		* https://www.rfc-editor.org/rfc/rfc4180
		* verschiedene existierende CSV-Konzepte/Klassen zur Verarbeitung:
		  https://www.baeldung.com/java-csv
		
		CSV-'Philosophie':
		* systemneutrale Speicherung und Übertragung tabellierter Daten
		  als leichtgewichtige Alternative zu 'laberigen' XML-Dialekten
		  (geringerer Overhead und Ressourcenverbrauch bei CSV)
		* Tabellen weisen (in der Regel) gleiche Anzahl Spalten pro Zeile
		  auf und verfügen optional über Header-Zeile mit Spaltennamen
		* nicht nur (De-)Serialisierung zu verwenden (ebensowenig
		  Datenbank-Tabellen hierfür geeignet sind)
		CSV-Datentypen:
		* letztlich nur wörtliche String-Daten, die jeweils individuell
		  pro Anwendung zu interpretieren sind (kein Standard)
		* CSVProcessor aus LingoLibry kann Daten auf Wunsch auch als
		  JSON-Daten(typen) interpretieren
		*/
		
		System.out.println("### CSV ###");
		
		// ## Beispiele mit CSV-Processor in Nexus-Klasse aus LingoLibry ##
		CSVProcessor
			CP0 = new CSVProcessor(),
			CP1 = new CSVProcessor('|', '#', '€');
		JSONProcessor
			JP = new JSONProcessor();			// zum Gegencheck
		DataNote Note;
		DataNote.DataType Type;
		String S, T;
		List<String> L;
		Function<DataNote, List<String>>		// 1 Zeilen-DataNote zu Stringliste
			F = LineDN -> LineDN.asList(DataNote::asString);	// umwandeln
		
		// # CSV-Zeilen ohne Header-Zeile #
		System.out.println("1 Textzeile, 2 Datenzeilen:");
		S = "A,BB,CCC\r\n333,22,1";				// standardmäßig ohne Whitespace,
		Note = CP0.parse(S);					// da alle Zeichen zu Daten gehören!
		Type = Note.retType();					// Array/List in Array/List
		System.out.println("Datentyp: "+Type);	// (2D-Array aus Zeilen von Spalten)
		System.out.println(Arrays.deepToString(Note.asArrays(String.class, DataNote::asString)));
		System.out.println();					// (Möglichkeit 1 für Konversion)
		
		System.out.println("3 Text- und Datenzeilen:");			// per Default alles
		L = List.of("A,BB,CCC", "333,22,1", "null,false,true");	// Strings/Textdaten
		Note = CP0.parse(L);					// meist gleiche # Spalten pro Zeile (nicht zwingend)
		System.out.println(Note.asList(F));		// (Möglichkeit 2 für Konversion)
		T = "[[\"A\", \"BB\", \"CCC\"], [\"333\", \"22\", \"1\"]," +
			"[\"null\", \"false\", \"true\"]]";	// analoge JSON-Struktur als
		System.out.println(JP.parse(T).equals(Note));	// 2D-Stringlisten
		System.out.println();
		
		System.out.println("2 Text- und 3 Datenzeilen (beliebig kombinierbar):");
		L = List.of("A,BB,CCC", "333,22,1\r\nnull,false,true");	// nur 1 CSV
		Note = CP0.parse(L);
		System.out.println(Note.asList(F));
		T = "[[\"A\", \"BB\", \"CCC\"], [\"333\", \"22\", \"1\"]," +
			"[\"null\", \"false\", \"true\"]]";	// analoge JSON-Struktur als
		System.out.println(JP.parse(T).equals(Note));	// 2D-Stringlisten
		System.out.println();
		
		System.out.println("Datenzeilen mit JSON-Interpretation für Datenfelder:");
		S = "A,BB,CCC\r\n333,22,1\r\nnull,false,true";
		Note = CP0.parse(S, true);		// Parsing mit JSON-Typen
		System.out.println(Note.asList(F));		// (Unterschied in Ausgabe
		T = "[[\"A\", \"BB\", \"CCC\"], [333, 22, 1]," +	// nicht sichtbar)
			"[null, false, true]]";				// analoge JSON-Struktur
		System.out.println(JP.parse(T).equals(Note));
		System.out.println();
		
		System.out.println("CSV-Zeilen mit Quotierung/Escaping:");
		S = "\"A\",\"\\\\BB\\\\\",\"\"\"CCC\"\"\"";		// Verdopplung von "
		Note = CP0.parse(S);
		System.out.println(Note.asList(F));
		T = "[[\"A\", \"\\\\BB\\\\\", \"\\\"CCC\\\"\"]]";
		System.out.println(JP.parse(T).equals(Note));
		System.out.println();
		// Hinweis: CSV-Quotierung ist nicht Teil der Daten => falls JSON-
		//			interpretiert, fehlen die JSON-Anführungszeichen hierfür
		//			(Default-Verhalten: nicht JSON-Interpretierbares bleibt String!)
		
		System.out.println("CSV-Zeilen mit Alternativ-Quotierung/Escaping:");
		S = "#A#|#€€BB€€#|#€#CCC€##";			// auch ###CCC### möglich
		Note = CP1.parse(S);					// alternative Konfiguration
		System.out.println(Note.asList(F));
		T = "[[\"A\", \"€BB€\", \"#CCC#\"]]";
		System.out.println(JP.parse(T).equals(Note));
		System.out.println();
		
		// # CSV-Zeilen mit impliziter Header-Zeile aus CSV-Text (0. CSV-Zeile) #
		Function<DataNote, Map<String, Number>>	// jede Note-Zeile ist selbst Map aus Strings:Numbers
			FF = LineDN -> LineDN.asMap(DataNote::asString, DataNote::asNumber);
		List<Map<String, Number>> LL;

		System.out.println("Header-Keys/Spalten auf Zeilen verteilt:");
		S = "A,BB,CCC\r\n1,22,333\r\n4444,55555,666666";	// 0. Zeile als Header
		Note = CP0.parse(S, true, true);		// Array/List of Tables
		LL = Note.asList(FF);
		System.out.println(LL);
		T = "[{\"A\": 1, \"BB\": 22, \"CCC\": 333}," +
			" {\"A\": 4444, \"BB\": 55555, \"CCC\": 666666}]";
		System.out.println(JP.parse(T).equals(Note));
		System.out.println();
		// Hinweis: Wenn Header = false, wird Headerzeile übersprungen
		//			und keine Keys auf die CSV-Zeilen verteilt
		
		System.out.println("Header bzw. Daten mit zu wenigen/vielen Spalten:");
		S = "A,BB\r\n1\r\n4444,55555,666666";
		Note = CP0.parse(S, true, true);
		LL = Note.asList(FF);
		System.out.println(LL);
		T = "[{\"A\": 1, \"BB\": null}," +
			" {\"A\": 4444, \"BB\": 55555, \"-2\": 666666}]";
		System.out.println(JP.parse(T).equals(Note));
		System.out.println();
		
		// # CSV-Zeilen mit expliziter Header-Zeile (nicht aus CSV-Text) #
		System.out.println("Header-Keys/Spalten von extern:");
		S = "1,22,333\r\n4444,55555,666666";
		Note = CP0.parse(S, "A,BB,CCC", true);
		LL = Note.asList(FF);
		System.out.println(LL);
		T = "[{\"A\": 1, \"BB\": 22, \"CCC\": 333}," +
			" {\"A\": 4444, \"BB\": 55555, \"CCC\": 666666}]";
		System.out.println(JP.parse(T).equals(Note));
		System.out.println();
		
		// # CSV-Daten mit komplexen eingebetteten JSON-Werten #
		System.out.println("CSV mit komplexen JSON-Werten:");
		S = "\"[1, 22, 333]\"," +	// CSV-Felder quotiert wg. Separator ','
			"\"{\"\"A\"\": 1, \"\"BB\"\": 22, \"\"CCC\"\": 333}\"";
		Note = CP0.parse(S, true);
		DataNote[][] AD = Note.asArrays();	// 1 CSV-Zeile mit 2 DataNotes
		System.out.println(AD[0][0].asList(DataNote::asNumber));
		System.out.println(AD[0][1].asMap(DataNote::asString, DataNote::asNumber));
		T = "[[[1, 22, 333], {\"A\": 1, \"BB\": 22, \"CCC\": 333}]]";	// analoges JSON
		System.out.println(JP.parse(T).equals(Note));
		System.out.println();
		// Frage: Können CSV-Datenfelder selbst CSV-Daten (als String) enthalten?
		// 		  Können JSON-Strings auch CSV-Daten enthalten?
		// Frage: Wann würde eine JSON-Datenstruktur (ungeparst) in einem
		// 		  JSON-String Sinn machen? (vgl. {"A": "[1, 2, 3]", ...})
		
		// # Reale CSV-Daten einladen und verarbeiten #
		try
		{	// Achtung: eigenen Pfad angeben (Beispieldatei s. GRIPS)!
			Path P = Path.of("C:", "Users", "jr", "Desktop", "Surnames.txt");
			String R = Files.readString(P);
			System.out.println(R.length());
			
			/*long X1 = System.currentTimeMillis();
			Note = CP0.parse(R, false, false);
			long X2 = System.currentTimeMillis();
			System.out.println((double)(X2-X1)/1000+" sec");
			System.out.println(Note.asList().size());*/
			
			// 12 * 162253 ≈ 1.95 Mio DataNotes im Speicher nach ca. 3 sec.
			// (11 Spaltenknoten pro Zeile + 162253 Zeilenknoten)
			// Mit zusätzlichem JSON-Parsing jedes DataNotes: ca. 5 sec.
			// (Name der 1. Spalte ist String, alle anderen Daten sind Number)
			// (Hinweis: Performance-Daten im Netzbetrieb, Core i7 Gen 7)
		}
		catch ( Exception ignored)
		{ System.out.println("Fehler!"); }
		
		// ## CSV-Aufgaben ##
		// * Erzeugen Sie eine CSV-Struktur (zunächst als String), die ein
		//   (nicht-)rechteckiges 2D-Array darstellt. Parsen Sie den String
		//   anschließend.
		//   Rechteckig: 	{{1, 2, 3}, {4, 5, 6}}
		//   Ausgefranst:	{{1, 2, 3}, {4, 5}, {6}}
		// * Erzeugen Sie eine CSV-Struktur für eine fiktive Zitatensammlung,
		//   die bekannte Sätze mit Urheber darstellt.
		//   Michail Gorbatschow: "Wer zu spät kommt, den bestraft das Leben."
		//	 Götz von Berlichingen: "Leck mich am Arsch!" (eigtl. "im Arsch")
		//	 Was ist evtl. bei der Darstellung zu beachten?
		
		// ## CSV-Fragen ##
		// * (Wie) Lässt sich XML auf CSV abbilden?
		// * Wie lassen sich Graphen mit CSV darstellen?
		// * Ist eine CSV-Zeile ein Datentupel? Was hängt dies mit Strukturen relationaler
		//   Datenbanken zusammen? Wie wird eine ganze Datenbank gespeichert?
	}

	// ### DataNote ###
	static public void DataNote()
	{
		DataNote Note1, Note2;
		Processor PC = new Processor();			// generischer Processor
		
		System.out.println("### DataNote ###");
		
		// Simplex
		Note1 = new DataNote(1234567890);		// problematischer: nicht-
		Note2 = PC.parse("1234567890");		// normalisierte Float/Double
		System.out.println(Note1.equals(Note2));
		
		Note1 = new DataNote("Hello World");	// auch StringBuilder
		Note2 = PC.parse("\"Hello World\"");
		System.out.println(Note1.equals(Note2));
		
		Note1 = new DataNote(true);
		Note2 = PC.parse("true");
		System.out.println(Note1.equals(Note2));
		
		Note1 = new DataNote((Object)null);
		Note2 = PC.parse("null");
		System.out.println(Note1.equals(Note2));
		
		// Complex
		Note1 = new DataNote(new char[] {'A', 'B', 'C'});
		Note2 = PC.parse("['A', 'B', 'C']");		// nicht in JSON
		System.out.println(Note1.equals(Note2));

		Note1 = new DataNote(List.of(1, 22, 333));
		Note2 = PC.parse("[1, 22, 333]");
		System.out.println(Note1.equals(Note2));
		
		Note1 = new DataNote(Set.of('A', 'B', 'C'));
		Note2 = PC.parse("{'A', 'B', 'C'}");		// intern Option
		System.out.println(Note1.equals(Note2));
		
		Note1 = new DataNote(Map.of(1, "A", 22, "BB"));
		Note2 = PC.parse("{1:\"A\", 22:\"BB\"}");	// nicht in JSON
		System.out.println(Note1.equals(Note2));
		
		Note1 = new DataNote(Map.of(false, List.of('A', 'B'), Map.of(), true));
		Note2 = PC.parse("{false: ['\uD83D\uDE00', '\uD83D\uDE00'], {}: true}");
		System.out.println(Note1.equals(Note2));	// ungleich, da 'A' != '😀'
		System.out.println(Note2.asMap());
		//System.out.println(PC.present(Note2));
	}
		
	// ### Fragen CSV/JSON allgemein ###
	// * Was ist für folgende Daten besser zur Repräsentation geeignet:
	//   CSV oder JSON (Begründung)?
	//   1) Datenbank-Tabellen, EXCEL-Tabellen
	//   2) 1D-/2D-Arrays/Matrizen, Maps
	//   3) Graphen und Bäume mit/ohne Gewichtungen
	//   4) Formale Sprachen (Regex, XML)
	// * Lassen sich mit CSV bzw. JSON dieselben Datenstrukturen darstellen
	//   wie mit XML? Welche (Arten von) Daten sind mit CSV bzw. JSON nicht
	//   darstellbar?
	
	public static void main(String[] args)
	{
		JSON();
		CSV();
		DataNote();
	}
}
