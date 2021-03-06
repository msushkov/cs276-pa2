package edu.stanford.cs276;

import java.io.Serializable;

import edu.stanford.cs276.util.QueryWithEdits;

public interface EditCostModel extends Serializable {

	public double editProbability(String original, QueryWithEdits R, int distance);
}
