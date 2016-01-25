package de.fu_berlin.inf.moto360;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import de.fu_berlin.inf.moto360.util.UDPInterface;

/**
 * Class that gets feature input from somewhere, then applies a classifier and smoothes the output over
 * some time window.
 * Finally, if a threshold is reached, an action will be triggered.
 */
public class GestureRecognitionActor {

	// Subclass that does a feature transformation by applying a moving average to the input features.
	// The input can be len N vector and the output will also be len N (and smoothed over the last F frames).
	class MovingAverage implements FeatureUnion.FeatureTransformer {
		int timeWindow = 0;
		double [][]oldValues = null;
		private int rollingIndex = 0;
		private int numberOfFeatures;

		MovingAverage(int timeWindow, int numberOfFeatures) {
			this.timeWindow = timeWindow;
			this.numberOfFeatures = numberOfFeatures;
			// Initialize empty matrix of history values.
			this.oldValues = new double[this.timeWindow][this.numberOfFeatures];
		}

		@Override
		public int getNumberOfOutputFeatures() { return this.numberOfFeatures; }

		@Override
		public double []transform(double[] features) {
			// Put current features into matrix at the oldest position.
			this.oldValues[rollingIndex] = features;
			// Advance rolling buffer index to next oldest position.
			if (++rollingIndex >= timeWindow) rollingIndex = 0;
			// The actual features are just the average over the history.
			// Know your lang. specs:
			// Each class variable, instance variable, or array component is initialized with a default value when it is created (§15.9, §15.10) -> 0.0 for doubles.
			double []transformedFeatures = new double[numberOfFeatures];

			for (int f = 0; f < numberOfFeatures; ++f) {
				for (int i = 0; i < timeWindow; ++i)
					transformedFeatures[f] += oldValues[i][f];
				transformedFeatures[f] /= (double)(timeWindow);
			}
			return transformedFeatures;
		}
	}

	// The actual classifier and post-classifier.
	LogisticRegressionClassifier classifier = null;
	PredictionTimeWindow predictionWindow = null;
	// Holds the transformers that provide the input for the machine learning algorithm.
	FeatureUnion featureUnion = null;

	// This is not an enum (as it should be) because I can't be bothered to f*ck with java.
	public static final int Gesture_None = 0;
	public static final int Gesture_SwipeLeft = 1;
	public static final int Gesture_SwipeRight = 2;
	public static final int Gesture_Whitescreen = 3;

	// Enables a cooldown between gestures.
	long lastReactionTime = 0;

	// Holds the last input vector. This can be used for e.g. logging.
	double []lastInputFeatures = null;

	// If set, this will be used to show an info message about the executed gesture.
	Context infoMessageDisplayContext = null;

	GestureRecognitionActor() {
		// The time windows of the moving averages have carefully been chosen by a hyperparameter-optimization
		// using a (cross-validated) random forest as the classifier.
		this.featureUnion = new FeatureUnion();
		final int numberOfFeaturesFromWearableSensors = 6;
		this.featureUnion.addTransformer(new MovingAverage(2, numberOfFeaturesFromWearableSensors));
		this.featureUnion.addTransformer(new MovingAverage(5, numberOfFeaturesFromWearableSensors));
		this.featureUnion.addTransformer(new MovingAverage(43, numberOfFeaturesFromWearableSensors));

		// Only the best hand-chosen betas are used here.
		// The process for finding them involved long sessions of deep philosophical talk
		// under the olive trees near a wonderful shore in Greece.
		this.classifier = new LogisticRegressionClassifier(
			new double[][] {{5.17318584e-01, -4.54927982e-01, -3.11343929e-03,  1.32599031e+01,
				-8.48803445e+00, -3.78174611e+00, -1.84510599e+00,  1.15099758e+00,
				-3.83664520e-01,  1.33220557e+01, -8.52782008e+00, -3.79947214e+00,
				5.01713372e-02,  2.99640632e-01,  4.19555797e+00,  1.31631909e+01,
				-8.42612627e+00, -3.75416364e+00},
					{-1.00102958e+00,  9.85142481e-02, -2.35191690e-01, -6.15672282e+00,
				3.94109029e+00,  1.75590745e+00,  2.31021368e+00, -6.94967315e-01,
				1.67135423e+00, -6.19047664e+00,  3.96269706e+00,  1.76553409e+00,
				-6.75711095e-01,  2.19850258e-01, -8.20393526e-03, -5.79019020e+00,
				3.70646252e+00,  1.65137174e+00},
					{5.33758278e-01, -3.72117791e-04, -3.29721288e-01,  1.93454639e+01,
				-1.23835719e+01, -5.51735803e+00, -1.96280315e+00,  1.97837400e-02,
				-5.41219489e-01,  1.94768057e+01, -1.24676475e+01, -5.55481693e+00,
				1.84190275e-01, -5.65218655e-01, -1.74870130e+00,  1.95578799e+01,
				-1.25195454e+01, -5.57793942e+00},
					{4.97010137e-01,  2.89662703e-01,  4.27237775e-01,  1.57379459e+01,
				-1.00742989e+01, -4.48848798e+00,  1.03190398e+00,  5.68683073e-02,
				-1.06807524e+00,  1.58488039e+01, -1.01452622e+01, -4.52010487e+00,
				-9.25574619e-02, -4.82904919e-01,  1.18750491e-01,  1.58488640e+01,
				-1.01453007e+01, -4.52012201e+00}},
		new int[]{Gesture_SwipeLeft, Gesture_None, Gesture_SwipeRight, Gesture_Whitescreen});

		// The values here are taken from an analysis that indicates that our predictions might
		// only be as accurate as 25% sometimes. However, we are mainly mixing them up with "no_action"
		// so no harm done.
		predictionWindow = new PredictionTimeWindow(10, 0.25f, Gesture_None);
	}

	public void setInfoDisplayContext(Context context) {
		this.infoMessageDisplayContext = context;
	}

	private void sendMessageAsReaction(String message) {
		// Notify the user about the huge success. First things first.
		if (infoMessageDisplayContext != null) {
			Toast.makeText(infoMessageDisplayContext, message, Toast.LENGTH_SHORT).show();
		}
		// Add a cooldown before the next action can be executed.
		lastReactionTime = System.nanoTime();
		// and finally send the message, too.
		UDPInterface.getInstance().send(message);
	}

	public void input(double []X) {
		// Now construct the actual features that are later passed to the classifier.
		double [] features = this.featureUnion.transform(X);
		lastInputFeatures = features;

		// Evaluate the input.
		int prediction = classifier.predict(features);
		predictionWindow.pushPrediction(prediction);

		// Then check if we have a cooldown in place.
		final long currentSystemTime = System.nanoTime();
		final long timePassed = currentSystemTime - lastReactionTime;
		final long millisecondsPassed = timePassed / 1000 / 1000;
		if (millisecondsPassed < 1000) return;

		// And then react!
		int action = predictionWindow.getPrediction();

		switch (action) {
			case Gesture_None:
				break;
			case Gesture_SwipeLeft:
				sendMessageAsReaction("left");
				break;
			case Gesture_SwipeRight:
				sendMessageAsReaction("right");
				break;
			case Gesture_Whitescreen:
				sendMessageAsReaction("whitescreen");
				break;
		}
	}
}
