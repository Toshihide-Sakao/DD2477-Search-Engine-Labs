/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;

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
            list.get(list.size()-1).addOffset(offset);
            return;
        }
        PostingsEntry entry = new PostingsEntry(docID, score, offset);
        list.add(entry);
        lastDocID = docID;
    }

    public void addOffsetToLast(int offset) {
        list.get(list.size()-1).addOffset(offset);
    }

    // private int cointains(int docID) {
    //     for (int i = 0; i < list.size(); i++) {
    //         if (list.get(i).docID == docID) {
    //             return i;
    //         }
    //     }
    //     return -1;
    // }

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

    public void printList() {
        for (int i = 0; i < list.size(); i++) {
            System.out.printf("DocID: %d\n", list.get(i).docID);
        }
    }
}
