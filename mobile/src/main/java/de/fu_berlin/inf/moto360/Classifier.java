package de.fu_berlin.inf.moto360;

/**
 * A simple classifier interface, loosely following the API of scikit-learn.
 */
public interface Classifier {
	/*
		Takes ONE sample with X.length features and predicts the class.
	 */
	int predict(double []X);
	/*
		As no implementation currently implements the "fit" method, it's left out for now.
		void fit(double [][]X, int []y);
	 */
}
