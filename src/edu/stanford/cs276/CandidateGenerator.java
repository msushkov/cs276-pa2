package edu.stanford.cs276;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import edu.stanford.cs276.util.QueryWithEdits;

public class CandidateGenerator implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static CandidateGenerator cg_;
	private static double percentage = 0.5;

	private static LanguageModel languageModel;
	private static NoisyChannelModel noisyChannelModel;

	// Don't use the constructor since this is a Singleton instance
	private CandidateGenerator(LanguageModel lm, NoisyChannelModel ncm) {
		languageModel = lm;
		noisyChannelModel = ncm;
	}

	public static CandidateGenerator get(LanguageModel lm, NoisyChannelModel ncm) throws Exception{
		if (cg_ == null ){
			cg_ = new CandidateGenerator(lm, ncm);
		}
		return cg_;
	}


	public static final Character[] alphabet = {
		'a','b','c','d','e','f','g','h','i','j','k','l','m','n',
		'o','p','q','r','s','t','u','v','w','x','y','z',
		'0','1','2','3','4','5','6','7','8','9',
		' ',','};

	// Generate all candidates for the target query
	public Set<QueryWithEdits> getCandidates(String query) throws Exception {
		System.out.println("Generating edit1 candidates...");
		
		QueryWithEdits queryObject = new QueryWithEdits(new ArrayList<String>(), query);
		
		TreeSet<QueryWithEdits> edits1 = getEdits1(queryObject, true); // set to false
		
		System.out.println("Done with edits1. Num edits: " + edits1.size());
		
		// take some portion of edits1
		int numElementsToTake = (int) (percentage * edits1.size());

		// get the edits2 for only the # of elements we want to keep (top scores)
		TreeSet<QueryWithEdits> finalCandidates = new TreeSet<QueryWithEdits>();

		Iterator<QueryWithEdits> it = edits1.iterator();
		
		int count = 0;

		// go through the first k best candidates after the edit1 step
		while (it.hasNext() && count < numElementsToTake) {
			QueryWithEdits next = it.next();
			
			// get edits2
			TreeSet<QueryWithEdits> currEdits2 = getEdits2(next);

			// add the edit1's to our results
			finalCandidates.add(next);
			
			for (QueryWithEdits q : currEdits2) {
				finalCandidates.add(q);
			}
		}
		
		System.out.println("Done with edits2.");

		// TODO: add original query into candidate set
		
		return finalCandidates;
	}

	private TreeSet<QueryWithEdits> getEdits2(QueryWithEdits query) {
		TreeSet<QueryWithEdits> edits2 = getEdits1(query, true);
		
		// TODO: prune (in terms of score)
		
		return edits2;
	}

	private TreeSet<QueryWithEdits> getEdits1(QueryWithEdits queryWithEdits, boolean isEdits2) {
		TreeSet<QueryWithEdits> candidates = new TreeSet<QueryWithEdits>();
		String query = queryWithEdits.query;

		// go through each char in the alphabet
		for (char letter : alphabet) {
			// get the insertion before the first character
			String newQuery = "" + letter + query; // insert the character at the beginning

			String currEdit = "INS_<START>_" + letter;
			ArrayList<String> edits = queryWithEdits.cloneEditHistory(); // deep copy of the object
			edits.add(currEdit);

			if (checkIfWordsAreInDict(newQuery, isEdits2)) {
				QueryWithEdits newQ = new QueryWithEdits(edits, newQuery);
				newQ.score = newQ.computeScore(newQuery, languageModel, noisyChannelModel);
				candidates.add(newQ);
			}
		}


		// loop through the chars in the query
		for (int i = 0; i < query.length(); i++) {
			// deletions
			
			if (i < query.length() - 1) {
				String newQuery = query.substring(0, i) + query.substring(i + 1);

				String currEdit = "DEL_" + query.charAt(i) + "_" + query.charAt(i + 1);
				ArrayList<String> edits = queryWithEdits.cloneEditHistory();
				edits.add(currEdit);

				if (checkIfWordsAreInDict(newQuery, isEdits2)) {
					QueryWithEdits newQ = new QueryWithEdits(edits, newQuery);
					newQ.score = newQ.computeScore(newQuery, languageModel, noisyChannelModel);
					candidates.add(newQ);
				}
			} else {
				String newQuery = query.substring(0, i);

				String currEdit = "DEL_" + query.charAt(i) + "_<END>";
				ArrayList<String> edits = queryWithEdits.cloneEditHistory();
				edits.add(currEdit);

				if (checkIfWordsAreInDict(newQuery, isEdits2)) {
					QueryWithEdits newQ = new QueryWithEdits(edits, newQuery);
					newQ.score = newQ.computeScore(newQuery, languageModel, noisyChannelModel);
					candidates.add(newQ);
				}
			}
			
			// transpositions
			if (i < query.length() - 2) {
				String newQuery = query.substring(0, i) + query.charAt(i + 1) + query.charAt(i) + query.substring(i + 2);

				String currEdit = "TRANS_" + query.charAt(i) + "_" + query.charAt(i + 1);
				ArrayList<String> edits = queryWithEdits.cloneEditHistory();
				edits.add(currEdit);

				if (checkIfWordsAreInDict(newQuery, isEdits2)) {
					QueryWithEdits newQ = new QueryWithEdits(edits, newQuery);
					newQ.score = newQ.computeScore(newQuery, languageModel, noisyChannelModel);
					candidates.add(newQ);
				}
			}


			// go through each char in the alphabet
			for (char letter : alphabet) {
				getInsertsAndSubstitutions(candidates, queryWithEdits, i, letter, isEdits2);
			}
		}

		return candidates;
	}

	private void getInsertsAndSubstitutions(TreeSet<QueryWithEdits> candidates, QueryWithEdits queryWithEdits, int i, char letter, boolean isEdits2) {
		String query = queryWithEdits.query;
		
		char c = query.charAt(i);

		// insertions

		String newQuery2 = query.substring(0, i + 1) + letter + query.substring(i + 1);

		String currEdit2 = "INS_" + c + "_" + letter;
		ArrayList<String> edits2 = queryWithEdits.cloneEditHistory();
		edits2.add(currEdit2);

		if (checkIfWordsAreInDict(newQuery2, isEdits2)) {
			QueryWithEdits newQ = new QueryWithEdits(edits2, newQuery2);
			newQ.score = newQ.computeScore(newQuery2, languageModel, noisyChannelModel);
			candidates.add(newQ);
		}


		// substitutions

		String newQuery3 = query.substring(0, i) + letter + query.substring(i + 1);

		String currEdit3 = "SUB_" + c + "_" + letter;
		ArrayList<String> edits3 = queryWithEdits.cloneEditHistory();
		edits3.add(currEdit3);

		if (checkIfWordsAreInDict(newQuery3, isEdits2)) {
			QueryWithEdits newQ = new QueryWithEdits(edits3, newQuery3);
			newQ.score = newQ.computeScore(newQuery3, languageModel, noisyChannelModel);
			candidates.add(newQ);
		}
	}

	/*
	 * If edits2 is true, returns true if all the words are in the dictionary; otherwise returns false.
	 * (used after computing edit distances of 2)
	 * 
	 * If edits2 is false, returns true if at most one of the words in the query is not in the dictionary.
	 * (used after computing edit distances of 1)
	 */
	private boolean checkIfWordsAreInDict(String newQuery, boolean edits2) {
		String[] words = newQuery.split(" ");

		// check each word in the query

		int numNotInDict = 0;

		for (String w : words) {
			if (!languageModel.unigramProb.containsKey(w)) {
				if (edits2) {
					return false;
				}
				
				numNotInDict++;
			}
			
			if (numNotInDict > 1) {
				return false;
			}
		}

		return true;
	}

	private void debugPrint(Set<QueryWithEdits> candidates) {
		for (QueryWithEdits q : candidates) {
			System.out.print("candidate: " + q.query + ", ");
		}
		System.out.println();
	}
}
