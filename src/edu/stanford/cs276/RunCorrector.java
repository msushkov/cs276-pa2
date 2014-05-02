package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import edu.stanford.cs276.util.QueryWithEdits;

public class RunCorrector {

	public static LanguageModel languageModel;
	public static NoisyChannelModel nsm;
	public static CandidateGenerator cg;
	

	public static void main(String[] args) throws Exception {
		
		long startTime = System.currentTimeMillis();
		
		// Parse input arguments
		String uniformOrEmpirical = null;
		String queryFilePath = null;
		String goldFilePath = null;
		String extra = null;
		BufferedReader goldFileReader = null;
		if (args.length == 2) {
			// Run without extra and comparing to gold
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
		}
		else if (args.length == 3) {
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
			if (args[2].equals("extra")) {
				extra = args[2];
			} else {
				goldFilePath = args[2];
			}
		} 
		else if (args.length == 4) {
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
			extra = args[2];
			goldFilePath = args[3];
		}
		else {
			System.err.println(
					"Invalid arguments.  Argument count must be 2, 3 or 4" +
					"./runcorrector <uniform | empirical> <query file> \n" + 
					"./runcorrector <uniform | empirical> <query file> <gold file> \n" +
					"./runcorrector <uniform | empirical> <query file> <extra> \n" +
					"./runcorrector <uniform | empirical> <query file> <extra> <gold file> \n" +
					"SAMPLE: ./runcorrector empirical data/queries.txt \n" +
					"SAMPLE: ./runcorrector empirical data/queries.txt data/gold.txt \n" +
					"SAMPLE: ./runcorrector empirical data/queries.txt extra \n" +
					"SAMPLE: ./runcorrector empirical data/queries.txt extra data/gold.txt \n");
			return;
		}
		
		if (goldFilePath != null ){
			goldFileReader = new BufferedReader(new FileReader(new File(goldFilePath)));
		}
		
		// Load models from disk
		languageModel = LanguageModel.load(); 
		nsm = NoisyChannelModel.load();
		BufferedReader queriesFileReader = new BufferedReader(new FileReader(new File(queryFilePath)));
		nsm.setProbabilityType(uniformOrEmpirical);
		
		cg = CandidateGenerator.get(languageModel, nsm);
		
		int totalCount = 0;
		int yourCorrectCount = 0;
		String query = null;

		/*
		 * Each line in the file represents one query.  We loop over each query and find
		 * the most likely correction
		 */
		while ((query = queriesFileReader.readLine()) != null) {
			
			System.out.println("QUERY: " + query);
			
			String correctedQuery = getBestCandidate(query);
			
			if ("extra".equals(extra)) {
				/*
				 * If you are going to implement something regarding to running the corrector, 
				 * you can add code here. Feel free to move this code block to wherever 
				 * you think is appropriate. But make sure if you add "extra" parameter, 
				 * it will run code for your extra credit and it will run you basic 
				 * implementations without the "extra" parameter.
				 */	
			}
			

			// If a gold file was provided, compare our correction to the gold correction
			// and output the running accuracy
			if (goldFileReader != null) {
				String goldQuery = goldFileReader.readLine();
				if (goldQuery.equals(correctedQuery)) {
					yourCorrectCount++;
				}
				totalCount++;
			}
			
			// System.out.println(correctedQuery);
			System.out.println("ORIGINAL QUERY: " + query + " CORRECTED SUGGESTION: " + correctedQuery);
		}
		queriesFileReader.close();
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("RUNNING TIME: " + totalTime/1000 + " seconds ");
		
		System.out.println("RESULTS: # correct = " + yourCorrectCount + " , # total = " + totalCount + " , rate = " + (double) yourCorrectCount / totalCount);
	}
	
	/*
	 * Ranks the candidates and returns the best one.
	 */
	private static String getBestCandidate(String query) throws Exception {
		// find the highest-score candidate query
		HashSet<QueryWithEdits> candidates = cg.getCandidates(query);
		
		//System.out.println("Query: " + query);
		//CandidateGenerator.debugPrint(candidates);
		
		String bestGuess = "";
		double maxScore = Double.NEGATIVE_INFINITY;
		
		// score each of the candidates and find the best one
		for (QueryWithEdits suggested : candidates) {
			if (suggested.score > maxScore) {
				maxScore = suggested.score;
				bestGuess = suggested.query;
			}
		}
		
		return bestGuess;
	}
}
