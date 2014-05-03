package edu.stanford.cs276;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.stanford.cs276.util.QueryWithEdits;

public class CandidateGenerator implements Serializable {

	private static final long serialVersionUID = 1L;

	private static CandidateGenerator cg_;
	private static double percentage = 0.1;

	private static LanguageModel languageModel;
	private static NoisyChannelModel noisyChannelModel;
	
	private double bestScoreSoFar;
	private QueryWithEdits bestCandidate;
	
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
	
	/*
	 * NOTE (msushkov): since we only care about the top result, if we are at the edits2 phase,
	 * this means that all the tokens this phase produces must be in the dictionary, so we can compute
	 * their scores and simply keep track of only the top one.
	 * 
	 * For the edits1 phase we must still keep track of the candidate list since this will be used
	 * to generate edits2 (and some of these queries may have a single word not in the dict).
	 */
	

	// Generate the best candidate for the target query
	public QueryWithEdits getCandidate(String query) throws Exception {
		bestScoreSoFar = Double.NEGATIVE_INFINITY;
		
		QueryWithEdits queryObject = new QueryWithEdits(new ArrayList<String>(), query);

		// get the edits1
		HashSet<QueryWithEdits> edits1 = getEdits1(queryObject, false, query);

		// take some portion of edits1
		int numElementsToTake = (int) (percentage * edits1.size());

		Iterator<QueryWithEdits> it = edits1.iterator();
		int count = 0;

		QueryWithEdits toAdd;
		
		// go through the first k candidates after the edit1 step
		while (it.hasNext() && count < numElementsToTake) {
			QueryWithEdits next = it.next();
			
			// the edit1's with all words in dict have already been "added" to the result 
//
//			// add the edit1's to our results but only if all the words are in the dictionary
//			if (checkIfWordsAreInDict(next.query, true)) {
//				toAdd = new QueryWithEdits(next.cloneEditHistory(), next.query);
//				toAdd.score = toAdd.computeScore(toAdd.query, languageModel, noisyChannelModel);
//				
//				if (toAdd.score > bestScoreSoFar) {
//					bestScoreSoFar = toAdd.score;
//					bestCandidate = toAdd;
//				}
//			}

			// get edits2
			HashSet<QueryWithEdits> currEdits2 = getEdits2(next, query);
			
			// these will be "added" to the result set automatically
			
//			for (QueryWithEdits q : currEdits2) {
//				if (checkIfWordsAreInDict(q.query, true)) {
//					toAdd = new QueryWithEdits(q.cloneEditHistory(), q.query);
//					toAdd.score = toAdd.computeScore(toAdd.query, languageModel, noisyChannelModel);
//
//					if (toAdd.score > bestScoreSoFar) {
//						bestScoreSoFar = toAdd.score;
//						bestCandidate = toAdd;
//					}
//				}
//			}
			
			currEdits2.clear();
			currEdits2 = null;
			
			count++;
		}

		// add original query into candidate set, but only if all the words are in the dict
		if (checkIfWordsAreInDict(query, true)) {
			toAdd = new QueryWithEdits(new ArrayList<String>(), query);
			toAdd.score = toAdd.computeScore(query, languageModel, noisyChannelModel);

			if (toAdd.score > bestScoreSoFar) {
				bestScoreSoFar = toAdd.score;
				bestCandidate = toAdd;
			}
		}
		
		edits1.clear();
		edits1 = null;

		return bestCandidate;
	}

	private HashSet<QueryWithEdits> getEdits2(QueryWithEdits query, String originalQuery) {
		return getEdits1(query, true, originalQuery);
	}

	private HashSet<QueryWithEdits> getEdits1(QueryWithEdits queryWithEdits, boolean isEdits2, String originalQuery) {
		HashSet<QueryWithEdits> candidates = new HashSet<QueryWithEdits>();
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
				
				// if we are doing edits2, check if the score is higher than the highest so far and
				// only add this candidate in that case
				if (isEdits2) {
					newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
					if (newQ.score > bestScoreSoFar) {
						bestScoreSoFar = newQ.score;
						bestCandidate = newQ;
					}
				} else {
					// if we are doing edits1 but all the words happen to be in the dict
					if (checkIfWordsAreInDict(newQuery, true)) {
						newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
						if (newQ.score > bestScoreSoFar) {
							bestScoreSoFar = newQ.score;
							bestCandidate = newQ;
						}
					}
					
					// need this to generate edit2s
					candidates.add(newQ);
				}
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
					
					// if we are doing edits2, check if the score is higher than the highest so far and
					// only add this candidate in that case
					if (isEdits2) {
						newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
						if (newQ.score > bestScoreSoFar) {
							bestScoreSoFar = newQ.score;
							bestCandidate = newQ;
						}
					} else {
						// if we are doing edits1 but all the words happen to be in the dict
						if (checkIfWordsAreInDict(newQuery, true)) {
							newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
							if (newQ.score > bestScoreSoFar) {
								bestScoreSoFar = newQ.score;
								bestCandidate = newQ;
							}
						}
						
						// need this to generate edit2s
						candidates.add(newQ);
					}
				}
			} else {
				String newQuery = query.substring(0, i);

				String currEdit = "DEL_" + query.charAt(i) + "_<END>";
				ArrayList<String> edits = queryWithEdits.cloneEditHistory();
				edits.add(currEdit);

				if (checkIfWordsAreInDict(newQuery, isEdits2)) {
					QueryWithEdits newQ = new QueryWithEdits(edits, newQuery);
					
					// if we are doing edits2, check if the score is higher than the highest so far and
					// only add this candidate in that case
					if (isEdits2) {
						newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
						if (newQ.score > bestScoreSoFar) {
							bestScoreSoFar = newQ.score;
							bestCandidate = newQ;
						}
					} else {
						// if we are doing edits1 but all the words happen to be in the dict
						if (checkIfWordsAreInDict(newQuery, true)) {
							newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
							if (newQ.score > bestScoreSoFar) {
								bestScoreSoFar = newQ.score;
								bestCandidate = newQ;
							}
						}
						
						// need this to generate edit2s
						candidates.add(newQ);
					}
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
					
					// if we are doing edits2, check if the score is higher than the highest so far and
					// only add this candidate in that case
					if (isEdits2) {
						newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
						if (newQ.score > bestScoreSoFar) {
							bestScoreSoFar = newQ.score;
							bestCandidate = newQ;
						}
					} else {
						// if we are doing edits1 but all the words happen to be in the dict
						if (checkIfWordsAreInDict(newQuery, true)) {
							newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
							if (newQ.score > bestScoreSoFar) {
								bestScoreSoFar = newQ.score;
								bestCandidate = newQ;
							}
						}
						
						// need this to generate edit2s
						candidates.add(newQ);
					}
				}
			}

			// go through each char in the alphabet
			for (char letter : alphabet) {
				getInsertsAndSubstitutions(candidates, queryWithEdits, i, letter, isEdits2, originalQuery);
			}
		}

		return candidates;
	}

	private void getInsertsAndSubstitutions(Set<QueryWithEdits> candidates, QueryWithEdits queryWithEdits, int i, char letter, boolean isEdits2, String originalQuery) {
		String query = queryWithEdits.query;

		char c = query.charAt(i);

		// insertions

		String newQuery2 = query.substring(0, i + 1) + letter + query.substring(i + 1);

		String currEdit2 = "INS_" + c + "_" + letter;
		ArrayList<String> edits2 = queryWithEdits.cloneEditHistory();
		edits2.add(currEdit2);

		if (checkIfWordsAreInDict(newQuery2, isEdits2)) {
			QueryWithEdits newQ = new QueryWithEdits(edits2, newQuery2);
			
			// if we are doing edits2, check if the score is higher than the highest so far and
			// only add this candidate in that case
			if (isEdits2) {
				newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
				if (newQ.score > bestScoreSoFar) {
					bestScoreSoFar = newQ.score;
					bestCandidate = newQ;
				}
			} else {
				// if we are doing edits1 but all the words happen to be in the dict
				if (checkIfWordsAreInDict(newQuery2, true)) {
					newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
					if (newQ.score > bestScoreSoFar) {
						bestScoreSoFar = newQ.score;
						bestCandidate = newQ;
					}
				}
				
				// need this to generate edit2s
				candidates.add(newQ);
			}
		}


		// substitutions

		String newQuery3 = query.substring(0, i) + letter + query.substring(i + 1);

		String currEdit3 = "SUB_" + c + "_" + letter;
		ArrayList<String> edits3 = queryWithEdits.cloneEditHistory();
		edits3.add(currEdit3);

		if (checkIfWordsAreInDict(newQuery3, isEdits2)) {
			QueryWithEdits newQ = new QueryWithEdits(edits3, newQuery3);
			
			// if we are doing edits2, check if the score is higher than the highest so far and
			// only add this candidate in that case
			if (isEdits2) {
				newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
				if (newQ.score > bestScoreSoFar) {
					bestScoreSoFar = newQ.score;
					bestCandidate = newQ;
				}
			} else {
				// if we are doing edits1 but all the words happen to be in the dict
				if (checkIfWordsAreInDict(newQuery3, true)) {
					newQ.score = newQ.computeScore(originalQuery, languageModel, noisyChannelModel);
					if (newQ.score > bestScoreSoFar) {
						bestScoreSoFar = newQ.score;
						bestCandidate = newQ;
					}
				}
				
				// need this to generate edit2s
				candidates.add(newQ);
			}
		}
	}

	/*
	 * If edits2 is true, returns true if all the words are in the dictionary; otherwise returns false.
	 * (used after computing edit distances of 2)
	 * 
	 * If edits2 is false, returns true if at most one of the words in the query is not in the dictionary.
	 * (used after computing edit distances of 1)
	 */
	public static boolean checkIfWordsAreInDict(String newQuery, boolean edits2) {
		String[] words = newQuery.split(" ");
		
		if (words.length == 0) {
			return false;
		}

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

	public static void debugPrint(Collection<QueryWithEdits> candidates) {
		for (QueryWithEdits q : candidates) {
			System.out.print("candidate: " + q.query + ", score: " + q.score);
		}
		System.out.println();
	}
}
