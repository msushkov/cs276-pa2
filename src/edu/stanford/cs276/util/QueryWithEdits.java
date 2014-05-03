package edu.stanford.cs276.util;

import java.util.ArrayList;

import edu.stanford.cs276.LanguageModel;
import edu.stanford.cs276.NoisyChannelModel;
import edu.stanford.cs276.Config;

// compare by score
public class QueryWithEdits implements Comparable {

	public ArrayList<String> editHistory;
	public String query;
	public double score;
	
	public QueryWithEdits(ArrayList<String> editHistory_arg, String query_arg) {
		editHistory = editHistory_arg;
		query = query_arg;
	}

	// first query is the original
	public double computeScore(String originalQuery, LanguageModel languageModel, NoisyChannelModel ncm) {
		//System.out.println("Computing score for query: " + query);
		
		double noisyChannelScore = ncm.ecm_.editProbability(originalQuery, this, this.editHistory.size());
		
		//System.out.println("Noisy channel score: " + noisyChannelScore);
		
		double languageModelScore = languageModel.getLanguageModelScore(this.query);
		
		//System.out.println("LM score: " + languageModelScore);
		
		// languageModelScore has already been log-ged
		double result = noisyChannelScore + Config.MU * languageModelScore;
		
		//System.out.println("Result: " + result);
		
		return result;
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
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((editHistory == null) ? 0 : editHistory.hashCode());
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryWithEdits other = (QueryWithEdits) obj;
		if (editHistory == null) {
			if (other.editHistory != null)
				return false;
		} else if (!editHistory.equals(other.editHistory))
			return false;
		if (query == null) {
			if (other.query != null)
				return false;
		} else if (!query.equals(other.query))
			return false;
		if (Double.doubleToLongBits(score) != Double
				.doubleToLongBits(other.score))
			return false;
		return true;
	}
}
