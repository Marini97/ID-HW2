package lucenex;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Questo programma esegue una ricerca su un indice creato con TextFileIndexer
 */

public class SearchFile {
    private static final EnglishAnalyzer analyzer = new EnglishAnalyzer();

    public static void main(String[] args) throws IOException {
        // Cartella dove è stato creato l'indice
        String indexLocation = "tmp/index";

        // Apre l'indice
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexLocation).toPath()));

        // Crea un oggetto per la ricerca
        IndexSearcher searcher = new IndexSearcher(reader);

        // Oggetto per la raccolta dei risultati
        TopScoreDocCollector collector;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String s = "";
        Long startTime, endTime;
        Query q1, q2;

        // Ciclo di ricerca. Termina quando viene inserita la stringa "q"
        while (!s.equalsIgnoreCase("q")) {
            try {
                // collector viene inizializzato con il numero massimo di risultati da restituire
                // si inizializza ogni ciclo per evitare problemi con i risultati della ricerca precedente
                collector = TopScoreDocCollector.create(10, 1000);

                System.out.println("Inserire la query di ricerca: (q per uscire):");
                s = br.readLine();
                if (s.equalsIgnoreCase("q")) {
                    break;
                }

                // Crea un oggetto QueryParser per la ricerca
                // Il primo parametro è il campo su cui cercare
                // Il secondo parametro è l'oggetto Analyzer
                q1 = new QueryParser("contenuto", analyzer).parse(s);
                //q2 = new QueryParser("nome", analyzer).parse(s);

                startTime = System.nanoTime();

                // Esegue la ricerca ed i risultati vengono salvati in collector
                searcher.search(q1, collector);
                //searcher.search(q2, collector);
                ScoreDoc[] hits = collector.topDocs().scoreDocs;

                endTime = System.nanoTime();
                // Stampa i risultati
                System.out.println("Trovati " + collector.getTotalHits() + " hits in "+ (endTime - startTime)/1000000 + " millisecondi.");
                System.out.println("Primi "+ hits.length +" risultati in ordine di score:");
                for (int i = 0; i < hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
                    System.out.println((i + 1) + ". " + d.get("path") + " score=" + hits[i].score);
                }
                System.out.println("");

            } catch (Exception e) {
                System.out.println("Errore nella ricerca di " + s + " : " + e.getMessage());
            }
        }
    }

}

