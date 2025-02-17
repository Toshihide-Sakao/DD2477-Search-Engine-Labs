/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {
        if (query.queryterm == null) {
            return null;
        }
        PrintSearchedTerms(query);

        switch (queryType) {
            case PHRASE_QUERY:

                return ContiguousAll(query);
            case INTERSECTION_QUERY:
                return IntersectAll(query);
            case RANKED_QUERY:
                return Ranked(query);
            default:
                break;
        }

        return null;
    }

    private PostingsList Ranked(Query query) {
        PostingsList answer = index.getPostings(query.queryterm.get(0).term);
        int N = index.docNames.size();
        int df_t = answer.size();
        double idf_t = Math.log((double)N / (double)df_t);

        System.out.println("idf_t: " + idf_t + " df_t: " + df_t + " N: " + N);

        int tf_dt = 0;
        int len_d = 0;
        for (int i = 0; i < df_t; i++) {
            tf_dt = answer.get(i).getOffsets().size();
            len_d = index.docLengths.get(answer.get(i).docID);

            double tf_idf_dt = tf_dt * (double)idf_t / len_d;
            answer.get(i).setScore(tf_idf_dt);
        }
        answer.sort();
        return answer;
    }

    // 4 elements
    // e.g. 0 to 1, 1 to 2, 2 to 3
    private PostingsList IntersectAll(Query query) {
        PostingsList answer = index.getPostings(query.queryterm.get(0).term);
        for (int i = 1; i < query.queryterm.size(); i++) {
            PostingsList next = index.getPostings(query.queryterm.get(i).term);
            if (next == null) { // TEST:
                continue;
            }
            answer = Intersect(answer, next);
        }

        return answer;
    }

    // TIme complexity O(n + m)
    private PostingsList Intersect(PostingsList p1, PostingsList p2) {
        PostingsList answer = new PostingsList();
        int i = 0;
        int j = 0;
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i).docID == p2.get(j).docID) {
                answer.add(p1.get(i).docID, 0, 0);
                i++;
                j++;
            } else if (p1.get(i).docID < p2.get(j).docID) {
                i++;
            } else {
                j++;
            }
        }

        return answer;
    }

    private PostingsList ContiguousAll(Query query) {
        PostingsList answer = index.getPostings(query.queryterm.get(0).term);
        for (int i = 1; i < query.queryterm.size(); i++) {
            PostingsList next = index.getPostings(query.queryterm.get(i).term);
            if (next == null) {
                continue;
            }
            answer = Contiguous(answer, next);
        }

        return answer;
    }

    private PostingsList Contiguous(PostingsList p1, PostingsList p2) {
        PostingsList answer = new PostingsList();
        int i = 0;
        int j = 0;
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i).docID == p2.get(j).docID) {
                ArrayList<Integer> offsets1 = p1.get(i).getOffsets();
                ArrayList<Integer> offsets2 = p2.get(j).getOffsets();
                int k = 0;
                int l = 0;
                while (k < offsets1.size() && l < offsets2.size()) {
                    if (offsets1.get(k) + 1 == offsets2.get(l)) {
                        answer.add(p1.get(i).docID, offsets2.get(l), 0);
                        break;
                    } else if (offsets1.get(k) < offsets2.get(l)) {
                        k++;
                    } else {
                        l++;
                    }
                }
                i++;
                j++;
            } else if (p1.get(i).docID < p2.get(j).docID) {
                i++;
            } else {
                j++;
            }
        }

        return answer;
    }

    private void PrintSearchedTerms(Query query) {
        System.out.printf("DEBUG: the term searched: ");
        for (int i = 0; i < query.queryterm.size(); i++) {
            System.out.printf("%s ", query.queryterm.get(i).term);
        }
        System.out.println();
    }
}