package edu.stanford.cs276;

import edu.stanford.cs276.util.QueryWithEdits;

public class UniformCostModel implements EditCostModel {
	
	// the probability of making a single edit (equal for all edits in ths model)
	private double SINGLE_EDIT_PROBABILITY = 0.05;
	
	private double ZERO_EDIT_DISTANCE_SCORE = 0.95;
	
	@Override
	public double editProbability(String original, QueryWithEdits R, int distance) {
		double result = -1;
		
		if (distance > 0) {
			result = distance * Math.log(SINGLE_EDIT_PROBABILITY);
		} else if (distance == 0) {
			result = Math.log(ZERO_EDIT_DISTANCE_SCORE);
		} else {
			System.out.println("cannot have negative edit distance!!!");
		}
		
		return result;
	}
}
