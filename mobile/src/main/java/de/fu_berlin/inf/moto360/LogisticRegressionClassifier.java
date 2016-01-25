package de.fu_berlin.inf.moto360;

public class LogisticRegressionClassifier {
	// The beta coefficients of the log.reg.
	double [][]betas;
	// The symbolic classes that are returned by the predict function.
	int []classmap;

	LogisticRegressionClassifier(double [][]betas, int []classmap) {
		this.betas = betas;
		this.classmap = classmap;
	}

	// Applies the regression to a feature vector and returns the most likely class (from the classmap).
	int predict(double []X) {
		/*
			Todo:
			There are multiple things that can be changed. The predict function could also return the probability etc.
			For that, a predictProbas method should be introduced that actually applies a sigmoid function to the results
			and then softmax'es them or something along those lines.

			This method here would then just return the maximum value of that function.
			But hey, this works for now!
		 */
		double maximumLikelihood = 0.0;
		int winnerIndex = -1;

		for (int i = 0; i < betas.length; ++i) {
			double likelihood = 0.0;
			for (int beta_i = 0; beta_i < betas[i].length; ++beta_i) {
				likelihood += betas[i][beta_i] * X[beta_i];
			}

			if (winnerIndex == -1 || likelihood > maximumLikelihood) {
				winnerIndex = i;
				maximumLikelihood = likelihood;
			}
		}

		assert (winnerIndex != -1);
		return this.classmap[winnerIndex];
	}
}
