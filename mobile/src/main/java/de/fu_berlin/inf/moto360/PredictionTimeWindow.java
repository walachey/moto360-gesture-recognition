package de.fu_berlin.inf.moto360;
import java.util.HashMap;
import java.util.Map;

/*
	This class takes predictions and stores T of them.
	You can then check whether e.g. more than 30% of the last T predictions were of class X.

	This smoothes the prediction a bit and shoul reduce outliers (assuming they are uncorrelated).
 */
public class PredictionTimeWindow {
	int []predictionBuffer;
	int rollingIndex = 0;
	float threshold = 0.75f;
	// Prediction that is returned when getPrediction is called and the threshold is not reached
	// by the best other prediction.
	int defaultPrediction;

	PredictionTimeWindow(int bufferSize, float threshold, int defaultPrediction) {
		this.predictionBuffer = new int[bufferSize];
		this.defaultPrediction = defaultPrediction;
	}

	public void pushPrediction(int prediction) {
		// Store in the oldest position.
		this.predictionBuffer[rollingIndex] = prediction;
		// And roll!
		if (++rollingIndex >= this.predictionBuffer.length) {
			rollingIndex = 0;
		}
	}

	public int getPrediction() {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (int i = 0; i < predictionBuffer.length; ++i) {
			Integer prediction = new Integer(predictionBuffer[i]);
			int current = 0;
			if (map.containsKey(prediction)) {
				current = map.get(prediction);
			}
			map.put(prediction, current + 1);
		}

		int max = 0;
		int maxPrediction = -1;
		int sum = 0;
		for (Map.Entry<Integer, Integer> prediction : map.entrySet()) {
			final int value = prediction.getValue();
			sum += value;

			if (maxPrediction == -1 || value > max) {
				maxPrediction = prediction.getKey();
				max = value;
			}
		}

		// What.
		if (sum == 0) return this.defaultPrediction;

		final float ratio = (float)(max) / (float)(sum);
		if (ratio < this.threshold) return this.defaultPrediction;
		return maxPrediction;
	}
}
