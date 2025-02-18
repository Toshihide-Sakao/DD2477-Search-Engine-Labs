import java.util.*;
import java.io.*;

public class PageRank {

	/**
	 * Maximal number of documents. We're assuming here that we
	 * don't have more docs than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	/**
	 * Mapping from document names to document numbers.
	 */
	HashMap<String, Integer> docNumber = new HashMap<String, Integer>();

	/**
	 * Mapping from document numbers to document names
	 */
	String[] docName = new String[MAX_NUMBER_OF_DOCS];

	/**
	 * A memory-efficient representation of the transition matrix.
	 * The outlinks are represented as a HashMap, whose keys are
	 * the numbers of the documents linked from.
	 * <p>
	 *
	 * The value corresponding to key i is a HashMap whose keys are
	 * all the numbers of documents j that i links to.
	 * <p>
	 *
	 * If there are no outlinks from i, then the value corresponding
	 * key i is null.
	 */
	HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<Integer, HashMap<Integer, Boolean>>();

	/**
	 * The number of outlinks from each node.
	 */
	int[] out = new int[MAX_NUMBER_OF_DOCS];

	/**
	 * The probability that the surfer will be bored, stop
	 * following links, and take a random jump somewhere.
	 */
	final static double BORED = 0.15;

	/**
	 * Convergence criterion: Transition probabilities do not
	 * change more that EPSILON from one iteration to another.
	 */
	final static double EPSILON = 0.0001;

	/* --------------------------------------------- */

	public PageRank(String filename) {
		int noOfDocs = readDocs(filename);
		iterate(noOfDocs, 1000);
	}

	/* --------------------------------------------- */

	/**
	 * Reads the documents and fills the data structures.
	 *
	 * @return the number of documents read.
	 */
	int readDocs(String filename) {
		int fileIndex = 0;
		try {
			System.err.print("Reading file... ");
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
				int index = line.indexOf(";");
				String title = line.substring(0, index);
				Integer fromdoc = docNumber.get(title);

				// Have we seen this document before?
				if (fromdoc == null) {
					// This is a previously unseen doc, so add it to the table.
					fromdoc = fileIndex++;
					docNumber.put(title, fromdoc);
					docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
				while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
					String otherTitle = tok.nextToken();
					Integer otherDoc = docNumber.get(otherTitle);
					if (otherDoc == null) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = fileIndex++;
						docNumber.put(otherTitle, otherDoc);
						docName[otherDoc] = otherTitle;
					}
					// Set the probability to 0 for now, to indicate that there is
					// a link from fromdoc to otherDoc.
					if (link.get(fromdoc) == null) {
						link.put(fromdoc, new HashMap<Integer, Boolean>());
					}
					if (link.get(fromdoc).get(otherDoc) == null) {
						link.get(fromdoc).put(otherDoc, true);
						out[fromdoc]++;
					}
				}
			}
			if (fileIndex >= MAX_NUMBER_OF_DOCS) {
				System.err.print("stopped reading since documents table is full. ");
			} else {
				System.err.print("done. ");
			}
		} catch (FileNotFoundException e) {
			System.err.println("File " + filename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}
		System.err.println("Read " + fileIndex + " number of documents");
		return fileIndex;
	}

	/* --------------------------------------------- */

	/*
	 * Chooses a probability vector a, and repeatedly computes
	 * aP, aP^2, aP^3... until aP^i = aP^(i+1).
	 */
	void iterate(int numberOfDocs, int maxIterations) {

		// YOUR CODE HERE
		double[] a = new double[numberOfDocs];
		double[] aNext = new double[numberOfDocs];

		double diff = 99;
		int counter = 0;

		// System.out.println(link.get(docNumber.get("5")).get(docNumber.get("1")));

		a[0] = 1; // initial prob
		// double BoJprob = BORED * 1 / numberOfDocs;
		while (diff > EPSILON && maxIterations > counter) {
			for (int i = 0; i < numberOfDocs; i++) { // rows (document from)
				// if document has no outgoing links
				if (out[i] == 0 /* || link.get(i) == null */) {
					for (int j = 0; j < numberOfDocs; j++) {
						aNext[i] = 1 / numberOfDocs;
					}
					continue;
				}
				
				// double probbb = 1 / out[i];
				int jCount = 0;
				for (int j = 0; j < numberOfDocs; j++) { // columns (document to)
					if (jCount == out[i]) {
						break; // break when done
					}
					if (link.get(i).get(j) != null) {
						double Gval = ((1 - BORED) * (1 / out[i])) + (BORED * 1 / numberOfDocs);
						aNext[i] += a[j] * Gval;
						jCount++;
					}
				}
				
				// diff = TODO:
			}
			a = aNext;
			counter++;
		}

		// for (int i = 0; i < numberOfDocs; i++) {
		// System.out.printf("%f ", a[i]);
		// }
		// System.out.println();

		// for (int i = 0; i < numberOfDocs; i++) {
		// System.out.printf("%f ", aNext[i]);
		// }
		System.out.println();

	}

	/* --------------------------------------------- */

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Please give the name of the link file");
		} else {
			new PageRank(args[0]);
		}
	}
}