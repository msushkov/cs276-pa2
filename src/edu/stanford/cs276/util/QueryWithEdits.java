package edu.stanford.cs276.util;

import java.util.ArrayList;

import edu.stanford.cs276.LanguageModel;
import edu.stanford.cs276.NoisyChannelModel;

// compare by score
public class QueryWithEdits implements Comparable {
	
	public ArrayList<String> editHistory;
	public String query;
	public double score;
	
	public QueryWithEdits(ArrayList<String> editHistory_arg, String query_arg) {
		editHistory = editHistory_arg;
		query = query_arg;
	}

	public double computeScore(String query, LanguageModel languageModel, NoisyChannelModel ncm) {
		double noisyChannelScore = ncm.ecm_.editProbability(query, this, this.editHistory.size());
		double languageModelScore = languageModel.getLanguageModelScore(this.query);
		
		// languageModelScore has already been log-ged
		return Math.log(noisyChannelScore) + languageModel.MU * languageModelScore;
	}

	@Override
	public int compareTo(Object other) {
		return ((Double) this.score).compareTo(((QueryWithEdits) other).score);
	}
	
	// deep copy of edit history
	public ArrayList<String> cloneEditHistory() {
		ArrayList<String> newHistory = new ArrayList<String>();
		
		for (String hist : editHistory) {
			newHistory.add(hist);
		}
		
		return newHistory;
	}
}
