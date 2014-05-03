package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import edu.stanford.cs276.util.QueryWithEdits;
import edu.stanford.cs276.util.Pair;

public class EmpiricalCostModel implements EditCostModel{
	
	// store confusion matrices
	private HashMap<String, Integer> delMatrix = new HashMap<String, Integer>();
	private HashMap<String, Integer> insMatrix = new HashMap<String, Integer>();
	private HashMap<String, Integer> subMatrix = new HashMap<String, Integer>();
	private HashMap<String, Integer> transMatrix = new HashMap<String, Integer>();
	
	// store character unigram and bigram counts
	private HashMap<Character, Integer> charUnigramCount = new HashMap<Character, Integer>();
	private HashMap<String, Integer> charBigramCount = new HashMap<String, Integer>();
	
	/*
	 * In building the confusion matrix (from slide 22):
		del[x,y]: count(xy typed as x)
		ins[x,y]: count(x typed as xy)
		sub[x,y]: count(x typed as y)
		trans[x,y]: count(xy typed as yx)
		Also count the frequency of individual characters (unigrams) and character pair (bigrams). These two thing together with the confusion matrix constitute the empirical noisy channel model.
		 
		Probability of a given error is determined by the unigrams, bigrams, and error counts present in the noisy channel model.
		Probability of deletion or transposition:
		 count of given error / count of "corrected" bigram
		Probability of insertion or substitution:
		 count of given error / count of "corrected" unigram
	 */
	
	public EmpiricalCostModel(String editsFile) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(editsFile));
		System.out.println("Constructing edit distance map...");
		String line = null;
		while ((line = input.readLine()) != null) {
			Scanner lineSc = new Scanner(line);
			lineSc.useDelimiter("\t");
			String noisy = lineSc.next();
			String clean = lineSc.next();
			
			// Determine type of error and record probability
			String typeOfEdit = getEdit(clean, noisy);
			
			if (typeOfEdit.startsWith("INS")) {
				incrementCount(delMatrix, typeOfEdit);
			} else if (typeOfEdit.startsWith("DEL")) {
				incrementCount(insMatrix, typeOfEdit);
			} else if (typeOfEdit.startsWith("SUB")) {
				incrementCount(subMatrix, typeOfEdit);
			} else {
				incrementCount(transMatrix, typeOfEdit);
			}
			
			// count character unigrams and bigrams
			
			char prevChar = '$';
			for (int i = 0; i < clean.length(); i++) {
				incrementCount(charUnigramCount, clean.charAt(i));
				
				StringBuilder bigram = new StringBuilder();
				bigram.append(prevChar);
				bigram.append(clean.charAt(i));				
				incrementCount(charBigramCount, bigram.toString());
				
				prevChar = clean.charAt(i);
			}
			
			StringBuilder bigram = new StringBuilder();
			bigram.append(prevChar);
			bigram.append('$');				
			incrementCount(charBigramCount, bigram.toString());
			
			
			char prevChar2 = '$';
			for (int i = 0; i < noisy.length(); i++) {
				incrementCount(charUnigramCount, noisy.charAt(i));
				
				StringBuilder bigram2 = new StringBuilder();
				bigram2.append(prevChar2);
				bigram2.append(noisy.charAt(i));
				incrementCount(charBigramCount, bigram2.toString());
				
				prevChar2 = noisy.charAt(i);
			}
			
			StringBuilder bigram2 = new StringBuilder();
			bigram2.append(prevChar2);
			bigram2.append('$');				
			incrementCount(charBigramCount, bigram2.toString());
		}

		input.close();
		System.out.println("Done.");
	}
	
	// You need to update this to calculate the proper empirical cost
	@Override
	public double editProbability(String original, QueryWithEdits R, int distance) {
		// only use QueryWithEdits
		
		double prob = 0;
		
		for (String edit : R.editHistory) {
			String[] letters = edit.split("_");
			
			int count = 0;
			double denom = 0;
			
			if (edit.startsWith("INS")) {
				count = insMatrix.get(edit);
				denom = charUnigramCount.get(letters[1].charAt(0));
			} else if (edit.startsWith("DEL")) {
				count = delMatrix.get(edit);
				denom = charBigramCount.get(letters[1] + letters[2]);
			} else if (edit.startsWith("SUB")) {
				count = subMatrix.get(edit);
				denom = charUnigramCount.get(letters[1].charAt(0));
			} else {
				count = transMatrix.get(edit);
				denom = charBigramCount.get(letters[1] + letters[2]);
			}
			
			prob += (Math.log(count) - Math.log(denom));
		}
		
		return prob;
	}
	
	private String getEdit(String original, String candidate) {
		int origLen = original.length();
		int candLen = candidate.length();
		
		String edit = "";
		
		// substitution or transposition
		if (origLen == candLen) {
			// find the mismatching characters
			ArrayList<Pair<Integer, Pair<Character, Character>>> mismatchedChars = 
					new ArrayList<Pair<Integer, Pair<Character, Character>>>();
			
			for (int i = 0; i < origLen; i++) {
				if (original.charAt(i) != candidate.charAt(i)) {
					mismatchedChars.add(new Pair(i, new Pair(original.charAt(i), candidate.charAt(i))));
				}
			}
			
			// substitution
			if (mismatchedChars.size() == 1) {
				char x = mismatchedChars.get(0).getSecond().getFirst();
				char y = mismatchedChars.get(0).getSecond().getSecond();
				
				StringBuffer b = new StringBuffer();
				b.append("SUB_");
				b.append(x);
				b.append("_");
				b.append(y);
				
				edit = b.toString();  
			} else {
				// transposition (2 mismatches)
				char x = mismatchedChars.get(0).getSecond().getFirst();
				char y = mismatchedChars.get(0).getSecond().getSecond();
				
				StringBuffer b = new StringBuffer();
				b.append("TRANS_");
				b.append(x);
				b.append("_");
				b.append(y);
				
				edit = b.toString(); 
			}
			
		} else {
			// insertion
			if (candLen > origLen) {
				char x = '$';
				char y = '\u0000';
				
				int misMatchIndex = -1;
				for (int i = 0; i < origLen; i++) {
					if (original.charAt(i) != candidate.charAt(i)) {
						misMatchIndex = i;
						y = candidate.charAt(i);
						
						if (i > 0) {
							x = original.charAt(i - 1);
						}
						
						break;
					}
				}
				
				// reached the end of the shorter string
				if (misMatchIndex == -1) {
					// candidate = original + char
					x = original.charAt(original.length() - 1);
					y = candidate.charAt(candidate.length() - 1); 
				}
				
				StringBuffer b = new StringBuffer();
				b.append("INS_");
				b.append(x);
				b.append("_");
				b.append(y);
				edit = b.toString(); 
			} else {
				// deletion (candidate is shorter)
				
				char x = '$';
				char y = '\u0000';
				
				int misMatchIndex = -1;
				for (int i = 0; i < candLen; i++) {
					if (original.charAt(i) != candidate.charAt(i)) {
						misMatchIndex = i;
						
						if (i > 0) {
							x = original.charAt(i - 1);
						}
						
						y = original.charAt(i);
						
						break;
					}
				}
				
				// reached the end of the shorter string
				if (misMatchIndex == -1) {
					// candidate = original + char
					x = original.charAt(original.length() - 1);
					y = candidate.charAt(candidate.length() - 1); 
				}
				
				StringBuffer b = new StringBuffer();
				b.append("INS_");
				b.append(x);
				b.append("_");
				b.append(y);
				edit = b.toString(); 
			}
		}
		
		return edit;
	}
	
	private <T> void incrementCount(HashMap<T, Integer> map, T elem) {
		if (map.containsKey(elem)) {
			map.put(elem, map.get(elem) + 1);
		} else {
			map.put(elem, 1);
		}
	}
}
