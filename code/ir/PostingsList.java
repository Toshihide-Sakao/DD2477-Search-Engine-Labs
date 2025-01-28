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

    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    public void add(int docID, double offset, double score) {
        if (cointains(docID)) {
            return;
        }
        PostingsEntry entry = new PostingsEntry();
        entry.docID = docID;
        entry.score = score;
        list.add(entry);
    }

    private Boolean cointains(int docID) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).docID == docID) {
                return true;
            }
        }
        return false;
    }

    public void printList() {
        for (int i = 0; i < list.size(); i++) {
            System.out.printf("DocID: %d\n", list.get(i).docID);
        }
    }
}
