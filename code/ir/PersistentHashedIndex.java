/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */

package ir;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

    // ===================================================================

    // public PageRank pagerank;

    /**
     * A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {
        String token;
        long ptr;
        String data;
        long byteSize;

        public Entry(String token, long ptr, String data) {
            this.token = token;
            this.ptr = ptr;
            this.data = data;
            this.byteSize = data.getBytes().length; // +1 for newline
        }

        public PostingsList getPostingsList() {
            String[] list = data.split("[;]"); // has [DocID, O1, ..., O2] , last becomes newline

            // System.out.printf("Token: %s, length: %d\n", token, list.length - 2);
            // System.out.printf("List: ");
            // for (int i = 0; i < list.length - 1; i++) {
            // System.out.printf("[%s] ", list[i]);
            // }
            // System.out.println();

            PostingsList postings = new PostingsList();

            if (list.length == 0 || ptr == -1 || list[0].equals("")) {
                return postings;
            }
            if (list[0].equals("rs")) {
                System.err.println("List: " + list[0] + " token: " + token);
            }

            int size = Integer.parseInt(list[0]); // number of postings
            for (int i = 1; i <= size; i++) {
                String[] doc = list[i].split("[:]");
                int docID = Integer.parseInt(doc[0]); // docID
                int offset = Integer.parseInt(doc[1]);
                postings.add(docID, offset, 0);

                for (int j = 2; j < doc.length; j++) {
                    offset = Integer.parseInt(doc[j]);
                    postings.addOffsetToLast(offset);
                }
            }
            return postings;
        }
    }

    // ==================================================================

    /**
     * Constructor. Opens the dictionary file and the data file.
     * If these files don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }

        // try {
        //     pagerank = new PageRank();
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
    }

    /**
     * Writes data to the data file at a specified place.
     *
     * @return The number of bytes written.
     */
    int writeData(String dataString, long ptr) {
        try {
            dataFile.seek(ptr);
            byte[] data = dataString.getBytes();
            dataFile.write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Reads data from the data file
     */
    String readData(long ptr, int size) {
        try {
            dataFile.seek(ptr);
            byte[] data = new byte[size];
            dataFile.readFully(data);
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==================================================================
    //
    // Reading and writing to the dictionary file.

    /*
     * Writes an entry to the dictionary hash table file.
     *
     * @param entry The key of this entry is assumed to have a fixed length
     * 
     * @param ptr The place in the dictionary file to store the entry
     */
    void writeEntry(Entry entry, long ptr) {
        try {
            dictionaryFile.seek(ptr);
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2); // every byte 8 bytes
            buffer.putLong(entry.ptr);
            buffer.putLong(entry.byteSize);
            dictionaryFile.write(buffer.array());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isEntryCollision(long ptr) {
        try {
            dictionaryFile.seek(ptr);
            byte[] dataptr = new byte[Long.BYTES];
            int numberRead = dictionaryFile.read(dataptr);
            if (numberRead == -1) {
                return false;
            }
            // System.out.println("Collision at: " + ptr + " with value: " + dataptr[0]);
            for (int i = 0; i < numberRead; i++) {
                if (dataptr[i] != 0) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Reads an entry from the dictionary file.
     *
     * @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(String token, long ptr) {
        try {
            String data = "";
            long entryptr = 0;
            int counter = 1;
            while (!data.startsWith(token + ";")) {
                // System.out.println("Reading at: " + ptr);
                dictionaryFile.seek(ptr);
                byte[] dataptr = new byte[Long.BYTES];
                byte[] datasize = new byte[Long.BYTES];
                dictionaryFile.readFully(dataptr);
                dictionaryFile.readFully(datasize);

                entryptr = ByteBuffer.wrap(dataptr).getLong();
                long size = ByteBuffer.wrap(datasize).getLong();

                if (dataptr == null || entryptr < 0 || datasize == null || size <= 0) {
                    // System.out.println("Entry not found: " + token);
                    return new Entry(token, -1, "");
                }

                data = readData(entryptr, (int) size);

                // System.out.println("Read: " + data + " at: " + ptr + " with size: " + size);

                // was a collision
                ptr = fixHash(ptr, counter);
                counter++;
            }

            data = data.substring(token.length() + 1); // remove token from data
            return new Entry(token, entryptr, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private long fixHash(long ptr, int counter) {
        return (((ptr + (long) counter * (long) counter)) % TABLESIZE) * (Long.BYTES * 2);
    }

    // ==================================================================

    /**
     * Writes the document names and document lengths to file.
     *
     * @throws IOException { exception_description }
     */
    public void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/docInfo");
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + ";" + docEucLengths.getOrDefault(key, 0.0) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }

    /**
     * Reads the document names and document lengths from file, and
     * put them in the appropriate data structures.
     *
     * @throws IOException { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File(INDEXDIR + "/docInfo");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
                docEucLengths.put(new Integer(data[0]), new Double(data[3]));
            }
        }
        freader.close();
    }

    /**
     * Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            long entryptr = 0;

            int counter = 0;
            for (Map.Entry<String, PostingsList> value : index.entrySet()) {
                if (counter % 5000 == 0)
                    System.err.println("Wrote " + counter + " entries" + " with " + collisions + " collisions");

                String key = value.getKey();
                PostingsList list = value.getValue();
                String listString = key + ";" + list.toString() + "\n";
                long hashed = (hash(key) % TABLESIZE) * (Long.BYTES * 2); // 2 longs for ptr and size
                Entry entry = new Entry(key, entryptr, listString);

                // Check for collisions
                int probCounter = 1;
                while (isEntryCollision(hashed)) { // if there is a collision get new hash
                    // System.out.println("Collision at: " + hashed / (Long.BYTES * 2) + " with key:
                    // " + key);
                    hashed = fixHash(hashed, probCounter);

                    collisions++;
                    probCounter++;
                }

                writeEntry(entry, hashed);
                writeData(listString, entryptr);

                // System.out.println("Key: " + key + " Hashed: " + hashed + " Entryptr: " +
                // entryptr + "listStr: "
                // + list.toString());
                entryptr += entry.byteSize;

                counter++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(collisions + " collisions.");
    }

    // make better hash function
    private long hash(String key) {
        long hash = 5381;

        for (int i = 0; i < key.length(); i++) {
            hash = hash * 33 + key.charAt(i);
        }
        return Math.abs(hash);
    }

    // ==================================================================

    /**
     * Returns the postings for a specific term, or null
     * if the term is not in the index.
     */
    public PostingsList getPostings(String token) {
        Entry entry = readEntry(token, (hash(token) % TABLESIZE) * (Long.BYTES * 2));
        return entry.getPostingsList();

        // return null;
    }

    public HashMap<String, PostingsList> getLoadedIndex() {
        return index;
    }

    /**
     * Inserts this token in the main-memory hashtable.
     */
    public void insert(String token, int docID, int offset) {
        PostingsList list = index.get(token);
        if (list == null) {
            list = new PostingsList();
            index.put(token, list);
        }
        list.add(docID, offset, 0);
    }

    /**
     * Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println(index.keySet().size() + " unique words");

        // TODO: uncomment these later
        System.err.print("Writing index to disk...");
        writeIndex();

        System.err.println("done!");
    }
}
