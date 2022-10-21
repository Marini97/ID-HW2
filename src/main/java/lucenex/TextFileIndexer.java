package lucenex;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LetterTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.*;

/**
 * Questo programma crea un indice a partire da un insieme di file di testo
 */
public class TextFileIndexer {
    private final IndexWriter writer;
    private final List<File> queue = new ArrayList<>();

    /**
     * Costruttore
     *
     * @param indexDir la cartella dove creare l'indice.
     * @throws IOException se si verifica un errore di I/O.
     */
    TextFileIndexer(String indexDir) throws IOException {
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();

        // Analyzer per il contenuto dei file di testo in lingua inglese
        perFieldAnalyzers.put("contenuto", new EnglishAnalyzer());

        // Analyzer per il nome dei file con un filtro per rimuovere le estensioni
        perFieldAnalyzers.put("nome", CustomAnalyzer.builder()
                .withTokenizer(LetterTokenizerFactory.class)
                .addCharFilter(PatternReplaceCharFilterFactory.class, "pattern", "\\.[^.]*$", "replacement", "")
                .build());


        Analyzer analyzer = new PerFieldAnalyzerWrapper(new EnglishAnalyzer(), perFieldAnalyzers);

        FSDirectory dir = FSDirectory.open(new File(indexDir).toPath());

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setCodec(new SimpleTextCodec());
        writer = new IndexWriter(dir, config);

        // cancella l'indice già presente in indexDir
        writer.deleteAll();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("L'indice viene creato nella cartella /tmp/index");

        String indexLocation = "tmp/index";
        TextFileIndexer indexer = null;
        try {
            indexer = new TextFileIndexer(indexLocation);
        } catch (Exception ex) {
            System.out.println("Errore nella creazione dell'indice: " + ex.getMessage());
            System.exit(-1);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String s = "";
        // read input from user until he enters q for quit
        while (!s.equalsIgnoreCase("q")) {
            try {
                System.out.println(
                        "Inserire il percoso della cartella che si vuole indicizzare: (q per uscire)");
                //System.out.println("[Formato dei file che verranno indicizzati: .txt]");
                s = br.readLine();
                if (s.equalsIgnoreCase("q")) {
                    break;
                }

                // try to add file into the index
                indexer.indexFileOrDirectory(s);

            } catch (Exception e) {
                System.out.println("Errore nell'indicizzazione di " + s + " : " + e.getMessage());
            }
        }

        // chiusura dell'indice altrimenti non viene creato
        indexer.closeIndex();
    }

    /**
     * Indicizza un file o una cartella
     *
     * @param fileName il nome del file o della cartella che si vuole indicizzare
     * @throws IOException when exception
     */
    public void indexFileOrDirectory(String fileName) throws IOException {
        long startTime, endTime;
        startTime = System.nanoTime();
        addFiles(new File(fileName));
        int originalNumDocs = writer.getDocStats().numDocs;

        // ciclo per scorrere tutti i file presenti in queue
        for (File f : queue) {
            FileReader fr = null;
            try {
                Document doc = new Document();

                fr = new FileReader(f);
                // Aggiunge il contenuto, il nome ed il percorso del file
                doc.add(new TextField("contenuto", fr));
                doc.add(new TextField("nome", f.getName(), Field.Store.YES));
                doc.add(new StringField("path", f.getPath(), Field.Store.YES));

                writer.addDocument(doc);
                //System.out.println("Aggiunto: " + f);
            } catch (Exception e) {
                System.out.println("Non può essere aggiunto: " + f);
                System.out.println(e.getMessage());
            } finally {
                if(fr != null) {
                    fr.close();
                }
            }
        }
        endTime = System.nanoTime();

        int newNumDocs = writer.getDocStats().numDocs;
        System.out.println("************************");
        System.out.println((newNumDocs - originalNumDocs) + " documenti aggiunti.");
        System.out.println("Indicizzazione completata in " + (endTime - startTime)/1000000 + " millisecondi.");
        System.out.println("************************");

        writer.commit();
        queue.clear();
    }

    private void addFiles(File file) {

        if (!file.exists()) {
            System.out.println(file + " non esiste.");
        }
        if (file.isDirectory()) {
            for (File f : Objects.requireNonNull(file.listFiles())) {
                addFiles(f);
            }
        } else {
            String filename = file.getName().toLowerCase();
            // ===================================================
            // Indicizza i file che terminano per .txt
            // ===================================================
            //if (filename.endsWith(".html") || filename.endsWith(".txt")) {
            if (filename.endsWith(".txt")) {
                queue.add(file);
            }
        }
    }

    /**
     * Chiude l'indice.
     *
     * @throws IOException when exception closing
     */
    public void closeIndex() throws IOException {
        writer.close();
    }
}