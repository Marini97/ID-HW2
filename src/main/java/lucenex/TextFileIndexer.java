package lucenex;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.*;

public class TextFileIndexer {
    Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
    //  CharSet per alcune stopwords italiane
    CharArraySet stopWords = new CharArraySet(Arrays.asList("di", "a", "da", "dei", "il", "la"), true);
    private final IndexWriter writer;
    private final List<File> queue = new ArrayList<>();

    /**
     * Constructor
     *
     * @param indexDir the name of the folder in which the index should be created
     * @throws IOException when exception creating index.
     */
    TextFileIndexer(String indexDir) throws IOException {
        // the boolean true parameter means to create a new index everytime,
        // potentially overwriting any existing files there.
        perFieldAnalyzers.put("contenuto", new StandardAnalyzer(stopWords));
        perFieldAnalyzers.put("nome", new WhitespaceAnalyzer());

        Analyzer analyzer = new PerFieldAnalyzerWrapper(new ItalianAnalyzer(), perFieldAnalyzers);

        FSDirectory dir = FSDirectory.open(new File(indexDir).toPath());

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        writer = new IndexWriter(dir, config);
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
        long startTime;
        long endTime;
        String s = "";
        // read input from user until he enters q for quit
        while (!s.equalsIgnoreCase("q")) {
            try {
                System.out.println(
                        "Inserire il percoso della cartella che si vuole indicizzare: (q per uscire)");
                System.out.println("[Formato dei file che verranno indicizzati: .txt, .html]");
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

        // ===================================================
        // closeIndex otherwise the index is not created
        // ===================================================
        indexer.closeIndex();
    }

    /**
     * Indexes a file or directory
     *
     * @param fileName the name of a text file or a folder we wish to add to the
     *                 index
     * @throws IOException when exception
     */
    public void indexFileOrDirectory(String fileName) throws IOException {
        // ===================================================
        // gets the list of files in a folder (if user has submitted
        // the name of a folder) or gets a single file name (if user
        // has submitted only the file name)
        // ===================================================
        long startTime, endTime;
        startTime = System.nanoTime();
        addFiles(new File(fileName));

        int originalNumDocs = writer.getDocStats().numDocs;
        for (File f : queue) {
            FileReader fr = null;
            try {
                Document doc = new Document();

                // ===================================================
                // add contents of file
                // ===================================================
                fr = new FileReader(f);

                doc.add(new TextField("contenuto", fr));
                doc.add(new TextField("nome", f.getName(), Field.Store.YES));
                doc.add(new StringField("path", f.getPath(), Field.Store.YES));

                writer.addDocument(doc);
                System.out.println("Aggiunto: " + f);
            } catch (Exception e) {
                System.out.println("Non può essere aggiunto: " + f);
                System.out.println(e.getMessage());
            } finally {
                fr.close();
            }
        }
        endTime = System.nanoTime();

        int newNumDocs = writer.getDocStats().numDocs;
        System.out.println("************************");
        System.out.println((newNumDocs - originalNumDocs) + " documenti aggiunti.");
        System.out.println("Indicizzazione completata in " + (endTime - startTime)/1000000 + " millisecondi");
        System.out.println("************************");

        queue.clear();
    }

    private void addFiles(File file) {

        if (!file.exists()) {
            System.out.println(file + " non esiste.");
        }
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                addFiles(f);
            }
        } else {
            String filename = file.getName().toLowerCase();
            // ===================================================
            // Indicizza i file che terminano per .html, .txt
            // ===================================================
            if (filename.endsWith(".html") || filename.endsWith(".txt")) {
                queue.add(file);
            } else {
                System.out.println("Saltato " + filename);
            }
        }
    }

    /**
     * Close the index.
     *
     * @throws IOException when exception closing
     */
    public void closeIndex() throws IOException {
        writer.commit();
        writer.close();
    }
}