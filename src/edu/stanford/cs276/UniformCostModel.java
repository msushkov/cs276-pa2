package edu.stanford.cs276;

import edu.stanford.cs276.util.QueryWithEdits;
import edu.stanford.cs276.Config;

public class UniformCostModel implements EditCostModel {
	
	@Override
	public double editProbability(String original, QueryWithEdits R, int distance) {
		double result = -1;
		
		if (distance > 0) {
			result = distance * Math.log(Config.SINGLE_EDIT_PROBABILITY);
		} else if (distance == 0) {
			result = Math.log(Config.ZERO_EDIT_DISTANCE_SCORE);
		} else {
			System.out.println("cannot have negative edit distance!!!");
		}
		
		return result;
	}
}
