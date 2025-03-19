/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.nio.charset.*;
import java.io.*;
import java.lang.reflect.Array;

/**
 * A class for representing a query as a list of words, each of which has
 * an associated weight.
 */
public class Query {

    /**
     * Help class to represent one query term, with its associated weight.
     */
    class QueryTerm {
        String term;
        double weight;
        // double idf;

        QueryTerm(String t, double w) {
            term = t;
            weight = w;
        }

        @Override
        public boolean equals(Object o) {
            return term.equals(o);
        }

        @Override
        public String toString() {
            return term;
        }

        // public boolean contains(String t) {
        //     return term.equals(t);
        // }
    }

    /**
     * Representation of the query as a list of terms with associated weights.
     * In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**
     * Relevance feedback constant alpha (= weight of original query terms).
     * Should be between 0 and 1.
     * (only used in assignment 3).
     */
    double alpha = 0.2;

    /**
     * Relevance feedback constant beta (= weight of query terms obtained by
     * feedback from the user).
     * (only used in assignment 3).
     */
    double beta = 1 - alpha;

    /**
     * Creates a new empty Query
     */
    public Query() {
    }

    /**
     * Creates a new Query from a string of words
     */
    public Query(String queryString) {
        StringTokenizer tok = new StringTokenizer(queryString);
        while (tok.hasMoreTokens()) {
            queryterm.add(new QueryTerm(tok.nextToken(), alpha));
        }
    }

    /**
     * Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }

    /**
     * Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for (QueryTerm t : queryterm) {
            len += t.weight;
        }
        return len;
    }

    /**
     * Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for (QueryTerm t : queryterm) {
            queryCopy.queryterm.add(new QueryTerm(t.term, t.weight));
        }
        return queryCopy;
    }

    public void addQueryTerm(String term) {
        queryterm.add(new QueryTerm(term, alpha));
    }

    /**
     * Expands the Query using Relevance Feedback
     *
     * @param results       The results of the previous query.
     * @param docIsRelevant A boolean array representing which query results the
     *                      user deemed relevant.
     * @param engine        The search engine object
     */
    public void relevanceFeedback(PostingsList results, boolean[] docIsRelevant, Engine engine) {
        int numRelevant = countTrue(docIsRelevant);
        int N = Index.docNames.size();
        // a * q_ori + b * (weight_doc / len_rel_docs)
        for (int i = 0; i < queryterm.size(); i++) {
            queryterm.get(i).weight = alpha;
        }
        // System.err.println(docIsRelevant.length + " " + results.size());

        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                ArrayList<String> contents = getDocContent(results.get(i).docID, engine);
                for (int j = 0; j < contents.size(); j++) {
                    int index = find(contents.get(j));
                    if (index != -1) {
                        queryterm.get(index).weight += beta * (1 / numRelevant);
                    } else {
                        queryterm.add(new QueryTerm(contents.get(j), beta * (1 / numRelevant)));
                    }

                }
            }
        }
    }

    private ArrayList<String> getDocContent(int docID, Engine engine) {
        String f = Index.docNames.get(docID);
        // get the content of doc
        ArrayList<String> contents = new ArrayList<String>();
        try {
            Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
            Tokenizer tok = new Tokenizer(reader, true, false, true, engine.patterns_file);
            while (tok.hasMoreTokens()) {
                contents.add(tok.nextToken());
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Error reading file: " + Index.docNames.get(docID));
        }
        System.err.println("DEBUG: content: " + contents.toString());
        return contents;
    }

    private int find(String term) {
        for (int i = 0; i < queryterm.size(); i++) {
            if (queryterm.get(i).term.equals(term)) {
                return i;
            }
        }
        return -1;
    }

    private int countTrue(boolean[] docIsRelevant) {
        int count = 0;
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                count++;
            }
        }
        return count;
    }
}
