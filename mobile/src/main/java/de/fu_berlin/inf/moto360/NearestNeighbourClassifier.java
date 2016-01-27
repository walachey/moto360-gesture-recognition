package de.fu_berlin.inf.moto360;

/**
 * A simple nearest neighbour approach that checks every sample against a set of class-representatives
 * and selects the closest one (euclidian distance).
 */
public class NearestNeighbourClassifier implements Classifier{
	// The beta coefficients of the log.reg.
	double [][]representatives;
	// The symbolic classes that are returned by the predict function.
	int []classAssignments;

	NearestNeighbourClassifier(double [][]representatives, int []classAssignments) {
		this.representatives = representatives;
		this.classAssignments = classAssignments;
	}

	// Returns the class of the closest representative.
	public int predict(double []X) {
		double minDistance = 0.0;
		int bestMatch = -1;

		for (int i = 0; i < classAssignments.length; ++i) {
			double distance = 0.0;
			for (int b_i = 0; b_i < representatives[i].length; ++b_i) {
				double diff = representatives[i][b_i] - X[b_i];
				distance += diff * diff;
			}

			if (bestMatch == -1 || distance < minDistance) {
				bestMatch = classAssignments[i];
				minDistance = distance;
			}
		}

		assert (bestMatch != -1);
		return bestMatch;
	}
}
