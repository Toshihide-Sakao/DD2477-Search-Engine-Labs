/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer, String> id2term = new HashMap<Integer, String>();

    /** Mapping from term strings to term ids */
    HashMap<String, Integer> term2id = new HashMap<String, Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String, List<KGramPostingsEntry>> index = new HashMap<String, List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }

    /**
     * Get intersection of two postings lists
     */
    private List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        ArrayList<KGramPostingsEntry> answer = new ArrayList<KGramPostingsEntry>();
        int i = 0;
        int j = 0;
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i).tokenID == p2.get(j).tokenID) {
                answer.add(p1.get(i));
                i++;
                j++;
            } else if (p1.get(i).tokenID < p2.get(j).tokenID) {
                i++;
            } else {
                j++;
            }
        }

        return answer;
    }

    /** Inserts all k-grams from a token into the index. */
    public void insert(String token) {
        if (token.equals("http://www.pinterest.com/pin/create/button/?description=abba%e2%80%99s%20bj%c3%b6rn%20ulvaeus%20on%20stockholm%27s%20travel%20highlights&url=https%3a%2f%2fwww.theguardian.com%2ftravel%2f2015%2fmay%2f22%2fbjorn-ulvaeus-abba-stockholm-sweden-travel%3fpage%3dwith%3aimg-3%23img-3&media=https%3a%2f%2fstatic.guim.co.uk%2fsys-images%2fguardian%2fpix%2fpictures%2f2015%2f5%2f21%2f1432216611190%2f20e6492d-934b-4ea8-b061-1b9735c6ebc5-2060x1539.jpeg")) {
            System.err.println("ok, term2id contains token: " + term2id.containsKey(token));
        }
        if (term2id.containsKey(token)) {
            // System.err.println("Token already exists in the index: " + token);
            return;
        }

        List<KGramPostingsEntry> entries = new ArrayList<KGramPostingsEntry>();

        
        int id = generateTermID();
        term2id.put(token, id);
        id2term.put(id, token);

        token = " " + token + " ";
        // (n - 2) + 3 - k + (k)
        for (int i = K; i < token.length() + 1; i++) {
            String kgram = token.substring(i - K, i).trim();
            
            // System.err.println("Inserting kgram: " + kgram);

            

            if (index.containsKey(kgram)) {
                entries = index.get(kgram);
            } else {
                entries = new ArrayList<KGramPostingsEntry>();
            }

            if (entries.contains(new KGramPostingsEntry(id))) {
                System.err.println("DEBUG: Found token in the index: " + token);
            }

            entries.add(new KGramPostingsEntry(id));
            index.put(kgram, entries);
        }
    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        

        List<KGramPostingsEntry> postings = new ArrayList<KGramPostingsEntry>();
        postings = index.get(kgram);
        if (postings == null) {
            return new ArrayList<KGramPostingsEntry>();
        }

        System.err.println("DEBUG: Found " + postings.size() + " posting(s) for kgram: " + kgram);
        System.err.printf("DEBUG: postings: \n");
        for (int i = 0; i < postings.size(); i++) {
            System.err.printf("%d. %s \n", i, id2term.get(postings.get(i).tokenID));
        }
        System.err.println();

        return postings;
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    private static HashMap<String, String> decodeArgs(String[] args) {
        HashMap<String, String> decodedArgs = new HashMap<String, String>();
        int i = 0, j = 0;
        while (i < args.length) {
            if ("-p".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ("-f".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ("-k".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ("-kg".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println("Unknown option: " + args[i]);
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String, String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
        Tokenizer tok = new Tokenizer(reader, true, false, true, args.get("patterns_file"));
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println(
                        "Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}
