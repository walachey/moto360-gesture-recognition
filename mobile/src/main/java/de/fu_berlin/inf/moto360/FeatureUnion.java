package de.fu_berlin.inf.moto360;

import java.util.ArrayList;

// Class that plays a similar role as scikit-learn's FeatureUnion class.
// It holds several FeatureTransfomers and applies them to some input, returning the transformed, concatenated output.
public class FeatureUnion {
	public interface FeatureTransformer {
		double []transform(double[] features);
		int getNumberOfOutputFeatures();
	}
	private int numberOfOutputFeatures = 0;
	ArrayList<FeatureTransformer> transformers = null;

	public int getNumberOfOutputFeatures() { return numberOfOutputFeatures; }

	void addTransformer(FeatureTransformer t) {
		if (this.transformers == null) this.transformers = new ArrayList<FeatureTransformer>();
		this.transformers.add(t);
		this.numberOfOutputFeatures += t.getNumberOfOutputFeatures();
	}

	public double []transform(double[] features) {
		double []output = new double[this.numberOfOutputFeatures];
		int index = 0;

		for (FeatureTransformer t : this.transformers) {
			double[] localOutput = t.transform(features);
			for (int i = 0; i < localOutput.length; ++i) {
				output[index++] = localOutput[i];
			}
		}

		return output;
	}
}