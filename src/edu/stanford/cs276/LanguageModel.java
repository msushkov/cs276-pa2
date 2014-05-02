package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;


public class LanguageModel implements Serializable {

	private double LAMBDA = 0.1;
	public double MU = 1;

	private static LanguageModel lm_;

	// store the probabilities
	public HashMap<String, Double> unigramProb = new HashMap<String, Double>();
	public HashMap<String, Double> bigramProb = new HashMap<String, Double>();


	// Do not call constructor directly since this is a Singleton
	private LanguageModel(String corpusFilePath) throws Exception {
		constructDictionaries(corpusFilePath);
	}


	public void constructDictionaries(String corpusFilePath)
			throws Exception {

		int totalUnigramCount = 0;

		HashMap<String, Integer> unigramCounts = new HashMap<String, Integer>();
		HashMap<String, Integer> bigramCounts = new HashMap<String, Integer>();

		System.out.println("Constructing dictionaries...");
		File dir = new File(corpusFilePath);
		for (File file : dir.listFiles()) {
			if (".".equals(file.getName()) || "..".equals(file.getName())) {
				continue; // Ignore the self and parent aliases.
			}
			System.out.printf("Reading data file %s ...\n", file.getName());
			BufferedReader input = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = input.readLine()) != null) {

				// split the line by space
				String[] words = line.trim().split("\\s+");
				String prevWord = null;

				for (String w : words) {
					totalUnigramCount++;

					if (unigramCounts.containsKey(w)) {
						unigramCounts.put(w, unigramCounts.get(w) + 1);
					} else {
						unigramCounts.put(w, 1);
					}

					String newBigram = prevWord + " " + w;

					if (prevWord != null) {
						if (bigramCounts.containsKey(newBigram)) {
							bigramCounts.put(newBigram, bigramCounts.get(newBigram) + 1);
						} else {
							bigramCounts.put(newBigram, 1);
						}
					}

					prevWord = w;
				}
			}
			input.close();
		}

		// compute the probabilities
		for (String unigram : unigramCounts.keySet()) {
			double prob = Math.log(unigramCounts.get(unigram)) - Math.log((double) totalUnigramCount);
			unigramProb.put(unigram, prob);
		}

		for (String bigram : bigramCounts.keySet()) {
			String firstToken = bigram.split(" ")[0];
			String secondToken = bigram.split(" ")[1];

			// if we dont have this bigram in our dictionary, its prob is 0
			double p_mle = 0;
			if (bigramCounts.containsKey(bigram)) {
				p_mle = Math.log(1 - LAMBDA) + Math.log(bigramCounts.get(bigram)) - Math.log((double) unigramCounts.get(firstToken));
			}

			double prob = logAdd(Math.log(LAMBDA) + unigramProb.get(secondToken), p_mle);
			bigramProb.put(bigram, prob);
		}

		System.out.println("Done.");
	}

	// Loads the object (and all associated data) from disk
	public static LanguageModel load() throws Exception {
		try {
			if (lm_==null){
				FileInputStream fiA = new FileInputStream(Config.languageModelFile);
				ObjectInputStream oisA = new ObjectInputStream(fiA);
				lm_ = (LanguageModel) oisA.readObject();
			}
		} catch (Exception e){
			throw new Exception("Unable to load language model.  You may have not run build corrector");
		}
		return lm_;
	}

	// Saves the object (and all associated data) to disk
	public void save() throws Exception{
		FileOutputStream saveFile = new FileOutputStream(Config.languageModelFile);
		ObjectOutputStream save = new ObjectOutputStream(saveFile);
		save.writeObject(this);
		save.close();
	}

	// Creates a new lm object from a corpus
	public static LanguageModel create(String corpusFilePath) throws Exception {
		if(lm_ == null ){
			lm_ = new LanguageModel(corpusFilePath);
		}
		return lm_;
	}

	// without the mu
	public double getLanguageModelScore(String candidateQuery) {
		String[] words = candidateQuery.split(" ");
		
		String prevWord = words[0];

		// first word is a unigram
		double sum = unigramProb.get(prevWord);

		// start with the second word
		for (int i = 1; i < words.length; i++) {
			String currBigram = prevWord + " " + words[i];
			
			if (bigramProb.containsKey(currBigram)) {
				sum += bigramProb.get(currBigram);
			} else {				
				sum += Math.log(LAMBDA) + unigramProb.get(words[i]);
			}

			prevWord = words[i];
		}

		return sum;
	}

	
	// Taken from: https://facwiki.cs.byu.edu/nlp/index.php/Log_Domain_Computations
	public static double logAdd(double logX, double logY) {
		// 1. make X the max
		if (logY > logX) {
			double temp = logX;
			logX = logY;
			logY = temp;
		}
		// 2. now X is bigger
		if (logX == Double.NEGATIVE_INFINITY) {
			return logX;
		}
		// 3. how far "down" (think decibels) is logY from logX?
		//    if it's really small (20 orders of magnitude smaller), then ignore
		double negDiff = logY - logX;
		if (negDiff < -20) {
			return logX;
		}
		// 4. otherwise use some nice algebra to stay in the log domain
		//    (except for negDiff)
		return logX + java.lang.Math.log(1.0 + java.lang.Math.exp(negDiff));
	}
}
