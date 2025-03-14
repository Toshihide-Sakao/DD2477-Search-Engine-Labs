import java.util.*;
import java.io.*;
import java.sql.Array;

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

	// Monte Carlo c:
	final static double C = 0.85;

	/* --------------------------------------------- */

	public PageRank(String filename) {
		int noOfDocs = readDocs(filename);
		// int[] davisTop30real = readDavisTop30("davis_top_30.txt");

		// iterate(noOfDocs, 1000);
		// int[] davisMC1 = iterateMC1(noOfDocs, noOfDocs * 10);
		// int[] davisMC2 = iterateMC2(noOfDocs, 10);
		int[] davisMC3 = iterateMC4(noOfDocs, 10);
		// int[] davisMC4 = iterateMC5(noOfDocs, noOfDocs * 24);

		// System.out.println("Goodness MC1: " + calcGoodness(davisMC1, davisTop30real));
		// System.out.println("Goodness MC2: " + calcGoodness(davisMC2, davisTop30real));
		// System.out.println("Goodness MC4: " + calcGoodness(davisMC3, davisTop30real));
		// System.out.println("Goodness MC5: " + calcGoodness(davisMC4, davisTop30real));

		// Iteration
		// int N = 1;
		// int[] lastTop = iterateMC5(noOfDocs, noOfDocs * N);
		// while (true) {
		// 	N++;
		// 	int[] top = iterateMC5(noOfDocs, noOfDocs * N);
		// 	if (Arrays.equals(lastTop, top)) {
		// 		break;
		// 	}
		// 	lastTop = top;
		// 	System.err.println("N: " + N);
		// }

		// System.err.println("N: " + N);
		// System.err.println("noOfDocs * N: " + (noOfDocs * N));

		// int topN = 100;
		// double[] goodnessMC1 = allGoodness(1, noOfDocs, davisTop30real, topN);
		// double[] goodnessMC2 = allGoodness(2, noOfDocs, davisTop30real, topN);
		// double[] goodnessMC4 = allGoodness(3, noOfDocs, davisTop30real, topN);
		// double[] goodnessMC5 = allGoodness(4, noOfDocs, davisTop30real, topN);

		// for (int i = 0; i < goodnessMC1.length; i++) {
		// 	System.out.printf(goodnessMC1[i] + "\t" + goodnessMC2[i] + "\t" + goodnessMC4[i] + "\t" + goodnessMC5[i] + "\n");
		// }
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
	int[] iterate(int numberOfDocs, int maxIterations) {
		double[] a = new double[numberOfDocs];
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
			Arrays.fill(aNext, 0.0);

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

		return print_top30(a);
	}

	// Monte Carlo 1
	int[] iterateMC1(int numberOfDocs, int N) {
		double[] rank = new double[numberOfDocs];
		Arrays.fill(rank, 0.0);

		Random rand = new Random();
		for (int i = 0; i < N; i++) {
			int initPos = rand.nextInt(numberOfDocs);
			rank = randomWalk(numberOfDocs, initPos, rank, false, new int[] { 0 });
		}

		for (int i = 0; i < numberOfDocs; i++) {
			rank[i] = rank[i] / N;
		}

		return print_top30(rank);
	}

	int[] iterateMC2(int numberOfDocs, int N) {
		double[] rank = new double[numberOfDocs];
		Arrays.fill(rank, 0.0);

		for (int i = 0; i < N; i++) {
			for (int m = 0; m < numberOfDocs; m++) {
				rank = randomWalk(numberOfDocs, m, rank, false, new int[] { 0 });
			}
		}

		for (int i = 0; i < numberOfDocs; i++) {
			rank[i] = rank[i] / N;
		}

		return print_top30(rank);
	}

	int[] iterateMC4(int numberOfDocs, int N) {
		double[] rank = new double[numberOfDocs];
		Arrays.fill(rank, 0.0);
		int[] totalVisits = { 1 };

		for (int i = 0; i < N; i++) {
			for (int m = 0; m < numberOfDocs; m++) {
				rank = randomWalk(numberOfDocs, m, rank, true, totalVisits);
			}
		}

		for (int i = 0; i < numberOfDocs; i++) {
			rank[i] = rank[i] / totalVisits[0];
		}

		// System.out.println("Total visits: " + totalVisits);

		return print_top30(rank);
	}

	int[] iterateMC5(int numberOfDocs, int N) {
		double[] rank = new double[numberOfDocs];
		Arrays.fill(rank, 0.0);
		int[] totalVisits = { 1 };

		Random rand = new Random();
		for (int i = 0; i < N; i++) {
			int initPos = rand.nextInt(numberOfDocs);
			rank = randomWalk(numberOfDocs, initPos, rank, true, totalVisits);
		}

		for (int i = 0; i < numberOfDocs; i++) {
			rank[i] = rank[i] / totalVisits[0];
		}

		return print_top30(rank);
	}

	double[] randomWalk(int numberOfDocs, int start, double[] rank, boolean stopWhenDangle, int[] totalVisits) {
		Random rand = new Random();
		rank[start]++;
		int next = start;
		while (rand.nextInt(Integer.MAX_VALUE) <= C * Integer.MAX_VALUE) {
			if ((link.get(next) == null || link.get(next).isEmpty())) { // if no outgoings just jump
				if (!stopWhenDangle) {
					next = rand.nextInt(numberOfDocs);
				} else {
					break;
				}
			} else {
				next = (int) link.get(next).keySet().toArray()[rand.nextInt(link.get(next).size())];
			}

			if (stopWhenDangle) {
				totalVisits[0]++;
				rank[next]++;
			}
		}

		if (!stopWhenDangle) {
			rank[next]++;
		}
		return rank;
	}

	int[] readDavisTop30(String filename) {
		int[] top30titles = new int[30];

		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
			for (int i = 0; i < 30; i++) {
				line = in.readLine();
				int index = line.indexOf(":");
				String title = line.substring(0, index);
				top30titles[i] = docNumber.get(title);
			}
		} catch (FileNotFoundException e) {
			System.err.println("File " + filename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}

		return top30titles;
	}

	double calcGoodness(int[] top30, int[] top30real) {
		double sum = 0;
		boolean exists = false;
		for (int i = 0; i < 30; i++) {
			for (int j = 0; j < 30; j++) {
				if (top30[i] == top30real[j]) {
					sum += (i - j) * (i - j);
					exists = true;
					break;
				}
			}
			if (!exists) {
				sum += 30 * 30;
			}
		}

		return sum;
	}

	double[] allGoodness(int choice, int noOfDocs, int[] davisTop30real, int topN) {
		int[] top30 = new int[30];
		double[] goodness = new double[topN];
		for (int i = 0; i < topN; i++) {
			switch (choice) {
				case 1:
					top30 = iterateMC1(noOfDocs, noOfDocs * i);
					break;
				case 2:
					top30 = iterateMC2(noOfDocs, i);
					break;
				case 3:
					top30 = iterateMC4(noOfDocs, i);
					break;
				case 4:
					top30 = iterateMC5(noOfDocs, noOfDocs * 10);
					break;
			}
			goodness[i] = calcGoodness(top30, davisTop30real);
		}

		return goodness;
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

	private int[] print_top30(double[] a) {
		int[] top30 = new int[30];
		double[] top30Val = new double[30];

		Arrays.fill(top30Val, -1);

		for (int i = 0; i < a.length; i++) {
			if (a[i] > top30Val[29]) {
				top30Val[29] = a[i];
				top30[29] = i;

				for (int j = 28; j >= 0; j--) {
					if (top30Val[j] < top30Val[j + 1]) {
						double tmpVal = top30Val[j];
						top30Val[j] = top30Val[j + 1];
						top30Val[j + 1] = tmpVal;

						int tmpIndex = top30[j];
						top30[j] = top30[j + 1];
						top30[j + 1] = tmpIndex;
					} else {
						break;
					}
				}
			}
		}

		// System.out.println("Top 30 documents:");
		for (int i = 0; i < 30; i++) {
			System.out.println(docName[top30[i]] + ": " + top30Val[i]);
		}

		return top30;
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