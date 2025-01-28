/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.io.Serializable;
import java.util.ArrayList;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;

    private ArrayList<Integer> offsets = new ArrayList<Integer>();

    public PostingsEntry(int docID, double score, int offset) {
        this.docID = docID;
        this.score = score;
        this.offsets.add(offset);
    }

    /**
     * PostingsEntries are compared by their score (only relevant
     * in ranked retrieval).
     *
     * The comparison is defined so that entries will be put in
     * descending order.
     */
    public int compareTo(PostingsEntry other) {
        return Double.compare(other.score, score);
    }

    // mine
    public void addOffset(int offset) {
        offsets.add(offset);
    }

    public int getOffset(int i) {
        return offsets.get(i);
    }

    public ArrayList<Integer> getOffsets() {
        return offsets;
    }
}
