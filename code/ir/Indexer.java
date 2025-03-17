/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.io.*;
import java.nio.charset.*;
import java.util.HashMap;

/**
 * Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer {

    /** The index to be built up by this Indexer. */
    Index index;

    /** K-gram index to be built up by this Indexer */
    KGramIndex kgIndex;

    /** The next docID to be generated. */
    private int lastDocID = 0;

    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file;

    int totalDocs = 0;

    /* ----------------------------------------------- */

    /** Constructor */
    public Indexer(Index index, KGramIndex kgIndex, String patterns_file) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.patterns_file = patterns_file;
    }

    /** Generates a new document identifier as an integer. */
    private int generateDocID() {
        return lastDocID++;
    }

    /**
     * Tokenizes and indexes the file @code{f}. If <code>f</code> is a directory,
     * all its files and subdirectories are recursively processed.
     */
    public void processFiles(File f, boolean is_indexing) {
        // do not try to index fs that cannot be read
        if (is_indexing) {
            if (f.canRead()) {
                if (f.isDirectory()) {
                    String[] fs = f.list();
                    // an IO error could occur
                    if (fs != null) {
                        for (int i = 0; i < fs.length; i++) {
                            processFiles(new File(f, fs[i]), is_indexing);
                        }
                    }
                } else {
                    // First register the document and get a docID
                    int docID = generateDocID();
                    if (docID % 1000 == 0)
                        System.err.println("Indexed " + docID + " files");
                    try {
                        Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
                        Tokenizer tok = new Tokenizer(reader, true, false, true, patterns_file);

                        // HashMap<String,Integer> uniqueTokens = new HashMap<String,Integer>();

                        int offset = 0;
                        while (tok.hasMoreTokens()) {
                            String token = tok.nextToken();
                            insertIntoIndex(docID, token, offset++);

                            // uniqueTokens.put(token, uniqueTokens.getOrDefault(token, 0) + 1);
                        }
                        index.docNames.put(docID, f.getPath());
                        index.docLengths.put(docID, offset);

                        // Hasekll magic :)
                        // index.docEucLengths.put(docID,
                        // Math.sqrt(uniqueTokens.values().stream().mapToDouble(i -> (totalDocs /
                        // i)*(totalDocs / i)).sum()));
                        reader.close();
                    } catch (IOException e) {
                        System.err.println("Warning: IOException during indexing.");
                    }
                }
            }
        }
    }

    public void calcEucLengths() {
        int N = index.docNames.size();
        HashMap<String, PostingsList> lindex = index.getLoadedIndex();
        int sizesub = "./../davisWiki/".length();
        String fileName = "";
        try {
            for (int docID = 0; docID < N; docID++) {
                fileName = index.docNames.get(docID);
                // String docFile = fileName.substring(sizesub);
                Reader reader = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8);
                Tokenizer tok = new Tokenizer(reader, true, false, true, patterns_file);

                HashMap<String, Integer> uniqueTokens = new HashMap<String, Integer>();
                HashMap<String, Integer> df_ts = new HashMap<String, Integer>();

                while (tok.hasMoreTokens()) {
                    String token = tok.nextToken();
                    uniqueTokens.put(token, uniqueTokens.getOrDefault(token, 0) + 1);
                    df_ts.put(token, lindex.get(token).size());
                }

                // Hasekll magic EDIT: :( gave up on streams
                double sum = 0;
                for (String t : uniqueTokens.keySet()) {
                    sum += Math.pow(uniqueTokens.get(t) * Math.log((double) N / (double)df_ts.get(t)), 2);
                }
                Index.docEucLengths.put(docID, Math.sqrt(sum));
                // index.docEucLengths.put(docID,
                // Math.sqrt(uniqueTokens.keySet().stream().map((t, i) -> Math.pow(i * (N /
                // df_ts.get(t)), 2)).sum()));
                reader.close();
            }
        } catch (Exception e) {
            System.out.println("Error in calcEucLengths: " + fileName);
            e.printStackTrace();
        }
    }

    // public void calcEucLengths(File f, boolean is_indexing) {
    //     // do not try to index fs that cannot be read
    //     if (is_indexing) {
    //         if (f.canRead()) {
    //             if (f.isDirectory()) {
    //                 String[] fs = f.list();
    //                 // an IO error could occur
    //                 if (fs != null) {
    //                     for (int i = 0; i < fs.length; i++) {
    //                         calcEucLengths(new File(f, fs[i]), is_indexing);
    //                     }
    //                 }
    //             } else {
    //                 // First register the document and get a docID
    //                 int docID = generateDocID();
    //                 if (docID % 5000 == 0)
    //                     System.err.println("Calced " + docID + " euclidean lengths");
    //                 try {
    //                     Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
    //                     Tokenizer tok = new Tokenizer(reader, true, false, true, patterns_file);
    //                     HashMap<String, Integer> uniqueTokens = new HashMap<String, Integer>();

    //                     while (tok.hasMoreTokens()) {
    //                         String token = tok.nextToken();
    //                         uniqueTokens.put(token, uniqueTokens.getOrDefault(token, 0) + 1);
    //                     }
    //                     // Hasekll magic :)
    //                     index.docEucLengths.put(docID, Math.sqrt(uniqueTokens.values().stream()
    //                             .mapToDouble(i -> Math.pow(Math.log((double) index.docNames.size() / i), 2)).sum()));
    //                     reader.close();
    //                 } catch (IOException e) {
    //                     System.err.println("Warning: IOException during indexing.");
    //                 }
    //             }
    //         }
    //     }
    // }

    void printFUCKINGARRAY(int[] a) {
        for (int i = 0; i < a.length; i++) {
            System.out.print(a[i] + " ");
        }
        System.out.println();
    }

    /* ----------------------------------------------- */

    /**
     * Indexes one token.
     */
    public void insertIntoIndex(int docID, String token, int offset) {
        index.insert(token, docID, offset);
        if (kgIndex != null)
            kgIndex.insert(token);
    }
}
