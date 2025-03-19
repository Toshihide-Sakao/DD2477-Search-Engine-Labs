/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    PageRank pagerank;

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.pagerank = new PageRank();
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {
        if (query.queryterm == null || query.queryterm.size() == 0) {
            return null;
        }
        PrintSearchedTerms(query);

        // testing
        // pagerank.printTop30();

        switch (queryType) {
            case PHRASE_QUERY:
                return ContiguousAll(query);
            case INTERSECTION_QUERY:
                return IntersectAll(query);
            case RANKED_QUERY:
                return RankedAll(query, rankingType, normType);
            // return Ranked(query, 0);
            default:
                break;
        }

        return null;
    }

    private PostingsList RankedAll(Query query, RankingType rankingType, NormalizationType normType) {
        switch (rankingType) {
            case TF_IDF:
                return RankedAllTF_IDF(query, normType);
            case PAGERANK:
                return RankedAllPageRank(query);
            // return RankedPageRank(query, 0);
            case COMBINATION:
                return RankedAllComb(query, normType, 1000, 1);
            default:
                break;
        }

        return new PostingsList();
    }

    private PostingsList RankedAllComb(Query query, NormalizationType normType, double prMult, double tfidfMult) {
        PostingsList answerPR = RankedAllPageRank(query);
        PostingsList answerTF_IDF = RankedAllTF_IDF(query, normType);
        PostingsList answer = Combine(answerPR, answerTF_IDF, prMult, tfidfMult);

        answer.sort();
        return answer;
    }

    private PostingsList Combine(PostingsList a, PostingsList b, double aMult, double bMult) {
        PostingsList answer = new PostingsList();
        for (int i = 0; i < a.size(); i++) {
            for (int j = 0; j < b.size(); j++) {
                if (a.get(i).docID == b.get(j).docID) {
                    double score = aMult * a.get(i).score + bMult * b.get(j).score;
                    answer.add(a.get(i).docID, 0, score);
                }
            }
        }

        return answer;
    }

    private PostingsList RankedAllPageRank(Query query) {
        PostingsList answer = RankedPageRank(query, 0);
        if (answer == null) {
            answer = new PostingsList();
        }
        for (int i = 1; i < query.queryterm.size(); i++) {
            PostingsList next = RankedPageRank(query, i);
            answer.merge(next, 1);
        }

        answer.sort();
        return answer;
    }

    private PostingsList RankedPageRank(Query query, int j) {
        PostingsList answer = getWildPostings(query.queryterm.get(j).term);
        for (int i = 0; i < answer.size(); i++) {
            String docFile = index.docNames.get(answer.get(i).docID).substring("./../davisWiki/".length()); // FIX: now
                                                                                                            // it is
                                                                                                            // hardcod
            // System.out.println("DEBUG: docFile: " + docFile);
            double score = pagerank.getScore(docFile);
            // answer.get(i).setScore(score);
            answer.get(i).setScore(score * query.queryterm.get(j).weight);
        }

        return answer;
    }

    private PostingsList RankedAllTF_IDF(Query query, NormalizationType normType) {
        // System.err.println("DEBUG: query: " + query.queryterm.toString());
        PostingsList answer = RankedTF_IDF(query, 0, normType);
        // PostingsList answer = RankedTF_IDFWild(query, 0, normType);
        if (answer == null) {
            answer = new PostingsList();
        }
        for (int i = 1; i < query.queryterm.size(); i++) {
            PostingsList next = RankedTF_IDF(query, i, normType);
            // PostingsList next = RankedTF_IDFWild(query, i, normType);
            if (next == null) {
                continue;
            }
            // System.err.println("DEBUG: next size: " + next.size());
            answer.merge(next, 0);
        }

        // System.err.println("DEBUG: answer size: " + answer.size());

        answer.sort();
        return answer;
    }

    private PostingsList RankedTF_IDFWild(Query query, int j, NormalizationType normType) {
        int starIndex = query.queryterm.get(j).term.indexOf("*");
        if (starIndex == -1) {
            return RankedTF_IDF(query, j, normType);
        }

        Query expanded = expandWild(query.queryterm.get(j).term, starIndex);
        System.err.println("DEBUG: expanded size: " + expanded.queryterm.size());
        return RankedAllTF_IDF(expanded, normType);
    }

    private PostingsList RankedTF_IDF(Query query, int j, NormalizationType normType) {
        int N = Index.docNames.size();
        // PostingsList answer = index.getPostings(query.queryterm.get(j).term);
        PostingsList answer = getWildPostings(query.queryterm.get(j).term);
        int df_t = answer.size();
        double idf_t = Math.log((double) N / (double) df_t);
        // System.out.println("DEBUG: idf_t: " + idf_t);

        for (int i = 0; i < df_t; i++) {
            int tf_dt = answer.get(i).getOffsets().size();

            double len_d = 0;
            if (normType == NormalizationType.NUMBER_OF_WORDS) {
                len_d = Index.docLengths.get(answer.get(i).docID);
            } else {
                len_d = Index.docEucLengths.get(answer.get(i).docID);
            }

            double tf_idf_dt = tf_dt * idf_t * query.queryterm.get(j).weight / (double) len_d;
            answer.get(i).setScore(tf_idf_dt);
        }
        return answer;
    }

    // TODO: some abstraction so I can idk am tired
    // private double get_tf_idf_dt(String term) {
    //     PostingsList answer = index.getPostings(term);

    //     int tf_dt = answer.get(i).getOffsets().size();

    //     double len_d = 0;
    //     if (normType == NormalizationType.NUMBER_OF_WORDS) {
    //         len_d = Index.docLengths.get(answer.get(i).docID);
    //     } else {
    //         len_d = Index.docEucLengths.get(answer.get(i).docID);
    //     }

    //     double tf_idf_dt = tf_dt * idf_t * query.queryterm.get(j).weight / (double) len_d;
    //     answer.get(i).setScore(tf_idf_dt);

    //     return tf_idf_dt;
    // }

    // 4 elements
    // e.g. 0 to 1, 1 to 2, 2 to 3
    private PostingsList IntersectAll(Query query) {
        PostingsList answer = getWildPostings(query.queryterm.get(0).term);
        if (answer == null) {
            answer = new PostingsList();
        }
        for (int i = 1; i < query.queryterm.size(); i++) {
            PostingsList next = getWildPostings(query.queryterm.get(i).term);
            if (next == null) {
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
        PostingsList answer = getWildPostings(query.queryterm.get(0).term);
        // PostingsList answer = index.getPostings(query.queryterm.get(0).term);
        if (answer == null) {
            answer = new PostingsList();
        }
        for (int i = 1; i < query.queryterm.size(); i++) {
            PostingsList next = getWildPostings(query.queryterm.get(i).term);
            // PostingsList next = index.getPostings(query.queryterm.get(i).term);

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

                if (p1.get(i).docID == 3793) {
                    System.out.println(
                            "DEBUG: 11 in offsets2: real: " + offsets2.size() + " offsets1: " + offsets1.size());
                }

                int k = 0;
                int l = 0;
                while (k < offsets1.size() && l < offsets2.size()) {
                    if (offsets1.get(k) + 1 == offsets2.get(l)) {
                        answer.add(p1.get(i).docID, offsets2.get(l), 0);
                        k++;
                        l++;
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

    private PostingsList UnionAll(Query query) {
        PostingsList answer = index.getPostings(query.queryterm.get(0).term);
        if (answer == null) {
            answer = new PostingsList();
        }
        for (int i = 1; i < query.queryterm.size(); i++) {
            PostingsList next = index.getPostings(query.queryterm.get(i).term);
            if (next == null) {
                continue;
            }
            answer = Union(answer, next);
        }

        return answer;
    }

    private PostingsList Union(PostingsList p1, PostingsList p2) {
        PostingsList answer = new PostingsList();
        int i = 0;
        int j = 0;
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i).docID == p2.get(j).docID) {
                answer.add(p1.get(i));
                answer.get(answer.size() - 1).getOffsets().addAll(p2.get(j).getOffsets());
                // answer.get(answer.size() - 1).score += p2.get(j).score;

                answer.get(answer.size() - 1).getOffsets().sort((a, b) -> {
                    return Integer.compare(a, b);
                });

                i++;
                j++;
            } else if (p1.get(i).docID < p2.get(j).docID) {
                answer.add(p1.get(i));
                i++;
            } else {
                answer.add(p2.get(j));
                j++;
            }
        }
        while (i < p1.size()) {
            answer.add(p1.get(i));
            i++;
        }
        while (j < p2.size()) {
            answer.add(p2.get(j));
            j++;
        }

        return answer;
    }

    private PostingsList getWildPostings(String token) {
        int starIndex = token.indexOf("*");
        if (starIndex == -1) {
            return index.getPostings(token);
        }
        Query expanded = expandWild(token, starIndex);

        // PrintSearchedTerms(expanded);
        PostingsList answer = UnionAll(expanded);
        System.err.println("DEBUG: expanded size: " + expanded.queryterm.size());
        System.err.println("DEBUG: expanded answer size: " + answer.size());
        // System.err.println("DEBUG: expanded answer: " + answer.toString());

        return answer;
    }

    private Query expandWild(String token, int starIndex) {
        Query expanded = new Query();
        ArrayList<String> kStrings = new ArrayList<String>();
        token = "^" + token + "$";
        // before *
        for (int j = kgIndex.K; j < starIndex + 1 + 1; j++) {
            String kgram = token.substring(j - kgIndex.K, j);
            kStrings.add(kgram);

            System.err.println("Inserting kgram: " + kgram);
        }

        for (int j = starIndex + 1 + 1 + 2; j < token.length() + 1; j++) {
            String kgram = token.substring(j - kgIndex.K, j);
            kStrings.add(kgram);

            System.err.println("Inserting kgram: " + kgram);
        }
        token = token.substring(0, starIndex + 1) + "." + token.substring(starIndex + 1);
        addKGramsToQuery(expanded, kStrings, token);

        return expanded;
    }

    public void addKGramsToQuery(Query query, ArrayList<String> kStrings, String oriToken) {
        List<KGramPostingsEntry> kgPostings = kgIndex.getIntersectAll(kStrings);
        if (kgPostings == null) {
            return;
        }
        for (int i = 0; i < kgPostings.size(); i++) {
            String tok = kgIndex.id2term.get(kgPostings.get(i).tokenID);
            if (tok.matches(oriToken)) {
                query.addQueryTerm(tok);
            }
        }
    }

    private void PrintSearchedTerms(Query query) {
        System.out.printf("DEBUG: the term searched: ");
        for (int i = 0; i < query.queryterm.size(); i++) {
            System.out.printf("%s ", query.queryterm.get(i).term);
        }
        System.out.println();
    }

    public PageRank getPageRank() {
        return this.pagerank;
    }
}