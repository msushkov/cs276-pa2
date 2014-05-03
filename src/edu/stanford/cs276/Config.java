package edu.stanford.cs276;

public class Config {
	public static final String noisyChannelFile = "noisyChannel";
	public static final String languageModelFile = "languageModel";
	public static final String candidateGenFile = "candidateGenerator";

	// the probability of making a single edit (equal for all edits in ths model)
	public static final double SINGLE_EDIT_PROBABILITY = 0.05;

	public static final double ZERO_EDIT_DISTANCE_SCORE = 0.95;
	
	public static final double LAMBDA = 0.1;
	public static final double MU = 1; 
}
