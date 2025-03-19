/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

public class PostingsList {

    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();
    private int lastDocID = -1;

    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    public void add(int docID, int offset, double score) {
        // int contains = cointains(docID);
        if (docID == lastDocID) {
            list.get(list.size() - 1).addOffset(offset);
            return;
        }
        PostingsEntry entry = new PostingsEntry(docID, score, offset);
        list.add(entry);
        lastDocID = docID;
    }

    public void add(int docID, ArrayList<Integer> offsets, double score) {
        if (docID == lastDocID) {
            list.get(list.size() - 1).addOffsets(offsets);
            return;
        }
        PostingsEntry entry = new PostingsEntry(docID, score, offsets.get(0));
        list.add(entry);
        list.get(list.size() - 1).addOffsets(offsets);
        lastDocID = docID;
    }

    public void add(PostingsEntry entry) {
        // list.add(entry);
        list.add(new PostingsEntry(entry.docID, entry.score, entry.getOffsets()));
    }

    public void addOffsetToLast(int offset) {
        list.get(list.size() - 1).addOffset(offset);
    }

    // N: number of DocID
    // ; end of DocID
    // 2:D1:O1:O2:O3;D2:O1:O2;
    @Override
    public String toString() {
        String s = "";
        s += list.size() + ";";
        for (int i = 0; i < list.size(); i++) {
            s += list.get(i).docID;
            for (int j = 0; j < list.get(i).getOffsets().size(); j++) {
                s += ":" + list.get(i).getOffsets().get(j);
            }
            s += ";";
        }
        return s;
    }

    public void sort() {
        list.sort((a, b) -> {
            return Double.compare(b.score, a.score);
        });
    }

    public void merge(PostingsList other, int TYPE) {
        if (other == null) {
            return;
        }

        int i = 0;
        int j = 0;
        while (i < list.size() && j < other.size()) {
            if (list.get(i).docID == other.get(j).docID) {
                if (TYPE == 0) { // IF_IDF
                    list.get(i).getOffsets().addAll(other.get(j).getOffsets());
                    list.get(i).score += other.get(j).score;
                }
                i++;
                j++;
            } else if (list.get(i).docID < other.get(j).docID) {
                i++;
            } else {
                list.add(other.get(j));
                j++;
            }
        }
        while (j < other.size()) {
            list.add(other.get(j));
            j++;
        }
    }

    public void printList() {
        for (int i = 0; i < list.size(); i++) {
            System.out.printf("DocID: %d\n", list.get(i).docID);
        }
    }
}
