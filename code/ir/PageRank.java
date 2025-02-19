package ir;

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

	HashMap<String, Integer> docFileNumber = new HashMap<String, Integer>();


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

	private String linksFilename = "./data/linksDavis.txt";

	private String davisTitlesFilename = "./data/davisTitles.txt";

	private String outputFilename = "./index/pagerank";

	double[] a;

	private int noOfDocs = 0;

	/* --------------------------------------------- */

	public PageRank() {
		this.noOfDocs = readDocs();
	}

	public void compute() {
		System.err.println("Computing PageRank...");
		iterate(noOfDocs, 1000);
		writeToFile();
		System.err.println("Done computing PageRank and writing...");
		readDavisTitle();
	}

	void writeToFile() {
		try (FileOutputStream stream = new FileOutputStream(outputFilename); 
				ObjectOutputStream out = new ObjectOutputStream(stream)) {
			out.writeObject(a);
			out.writeObject(docName);
			stream.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	void readPageRank() {
		try (FileInputStream stream = new FileInputStream(outputFilename); 
				ObjectInputStream in = new ObjectInputStream(stream)) {
			System.err.println("Reading PageRank from file...");
			a = (double[]) in.readObject();
			docName = (String[]) in.readObject();
			in.close();
			stream.close();
			// System.err.println("Done reading PageRank from file...");

			// read davisTitles
			readDavisTitle();
		}catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	void readDavisTitle() {
		try {
			System.err.println("Reading davisTitles from file...");
			BufferedReader in2 = new BufferedReader(new FileReader(davisTitlesFilename));
			String line;
			while ((line = in2.readLine()) != null) {
				String[] parts = line.split(";");
				String title = parts[1];
				int index = Integer.parseInt(parts[0]);
				docFileNumber.put(title, index);
			}
			in2.close();
			System.err.println("Done reading davisTitles from file.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* --------------------------------------------- */

	/**
	 * Reads the documents and fills the data structures.
	 *
	 * @return the number of documents read.
	 */
	int readDocs() {
		int fileIndex = 0;
		try {
			System.err.print("Reading file... ");
			BufferedReader in = new BufferedReader(new FileReader(linksFilename));
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
			System.err.println("File " + linksFilename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + linksFilename);
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
		a = new double[numberOfDocs];
		double[] aNext = new double[numberOfDocs];

		double diff = 99;
		int counter = 0;

		// System.out.println(link.get(docNumber.get("5")).get(docNumber.get("1")));

		a[0] = 1; // initial prob
		while (diff > EPSILON && maxIterations > counter) {
			if (counter % 10 == 0) {
				System.out.println("Iteration: " + counter + " Diff: " + diff);
			}

			diff = 0.0;
			a = normalize(a);
			for (int i = 0; i < numberOfDocs; i++) {
				aNext[i] = 0.0;
			}

			for (int i = 0; i < numberOfDocs; i++) { // rows (document from)
				// if document has no outgoing links
				if (out[i] == 0 || link.get(i) == null) {
					for (int j = 0; j < numberOfDocs; j++) {
						aNext[j] += a[i] * 1 / numberOfDocs;
						// System.out.println(aNext[j] + " " + a[i]);
					}
					continue;
				}

				for (int j : link.get(i).keySet()) {
					double Gval = ((1 - BORED) / out[i]) + (BORED / numberOfDocs);
					aNext[j] += a[i] * Gval;
				}

				for (int j = 0; j < numberOfDocs; j++) {
					if (link.get(i).get(j) == null) {
						aNext[j] += a[i] * (BORED / numberOfDocs);
					}
				}
			}

			normalize(aNext);
			diff = man_diff(a, aNext);

			for (int i = 0; i < numberOfDocs; i++) {
				a[i] = aNext[i];
			}

			counter++;
		}

		System.out.println("Iterations: " + counter);
		System.out.println("Diff: " + diff);

		// printTop30();
	}

	private double man_diff(double[] a, double[] aNext) {
		double diff = 0;
		for (int i = 0; i < a.length; i++) {
			diff += Math.abs(aNext[i] - a[i]); // manhattan
		}
		// System.out.println("Diff: " + diff);
		return diff;
	}

	private double euc_diff(double[] a, double[] aNext) {
		double diff = 0;
		for (int i = 0; i < a.length; i++) {
			diff += (aNext[i] - a[i]) * (aNext[i] - a[i]);
		}
		// System.out.println("Diff: " + diff);
		return Math.sqrt(diff);
	}

	private double[] normalize(double[] a) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		// System.out.println("Sum: " + sum);
		for (int i = 0; i < a.length; i++) {
			a[i] = a[i] / sum;
		}

		return a;
	}

	public void printTop30() {
		int[] top30 = new int[30];
		double[] top30Val = new double[30];

		// Initialize with minimum values
		Arrays.fill(top30Val, -1);

		for (int i = 0; i < a.length; i++) {
			// Check if `a[i]` belongs in the top 30
			if (a[i] > top30Val[29]) { // Compare with the smallest value in the top 30
				top30Val[29] = a[i];
				top30[29] = i;

				// Sort top30Val in descending order while maintaining the index mapping
				for (int j = 28; j >= 0; j--) {
					if (top30Val[j] < top30Val[j + 1]) {
						// Swap values
						double tempVal = top30Val[j];
						top30Val[j] = top30Val[j + 1];
						top30Val[j + 1] = tempVal;

						// Swap indices
						int tempIndex = top30[j];
						top30[j] = top30[j + 1];
						top30[j + 1] = tempIndex;
					} else {
						break;
					}
				}
			}
		}

		System.out.println("Top 30 documents:");
		for (int i = 0; i < 30; i++) {
			if (top30Val[i] > -1) { // Ensure valid values are printed
				System.out.println(docName[top30[i]] + ": " + top30Val[i]);
			}
		}
	}

	public double getScore(String docName) {
		int index1 = docFileNumber.get(docName);
		int index2 = docNumber.get(Integer.toString(index1));
		return a[index2];
	}

	/* --------------------------------------------- */

	// public static void main(String[] args) {
	// if (args.length != 1) {
	// System.err.println("Please give the name of the link file");
	// } else {
	// new PageRank(args[0]);
	// }
	// }
}