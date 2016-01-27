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
	Classifier classifier = null;
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
		this.featureUnion.addTransformer(new MovingAverage(3, numberOfFeaturesFromWearableSensors));
		this.featureUnion.addTransformer(new MovingAverage(23, numberOfFeaturesFromWearableSensors));

		// Only the best hand-chosen points are used here.
		// The process for finding them involved long sessions of deep philosophical talk
		// under the olive trees near a wonderful shore in Greece.
		this.classifier = new NearestNeighbourClassifier(
				new double[][] {
						{0.0845565795898, -0.290397644043, -0.443580627441, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.0832926432292, -0.301233927409, -0.456075032552, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.00474017599355, -0.312764706819, -0.686542676843, -0.023585167349, 0.0150975245658, 0.00672652840655},
						{0.0204086303711, 0.0143356323242, -0.0205307006836, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.0219370524089, 0.0137176513672, -0.0174153645833, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.0364751401155, 0.0524159307065, -0.0309083358101, -0.023585167349, 0.0150975245658, 0.00672652840655},
						{0.377799987793, 0.0626068115234, -0.662734985352, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.386525472005, 0.0316365559896, -0.680516560872, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.227367235267, -0.159856381624, -0.557479858398, -0.023585167349, 0.0150975245658, 0.00672652840655},
						{-0.139762878418, 0.567184448242, 0.0522003173828, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.149612426758, 0.577814737956, 0.0544281005859, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.20279992145, 0.522180971892, 0.0167216425357, -0.0230833552778, 0.0147763006389, 0.00658341078088},
						{0.0490570068359, -0.11637878418, -0.384452819824, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.0233459472656, -0.156255086263, -0.364491780599, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.161559395168, -0.000312473462976, -0.306831691576, -0.0250906035628, 0.0160611963466, 0.00715588128356},
						{0.115409851074, 128.039268494, 0.130081176758, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.121724446615, 85.3594258626, 0.144358317057, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.249261607294, 11.2421311088, -0.401709515115, -0.0225815432065, 0.014455076712, 0.00644029315521},
						{0.0952987670898, -0.712699890137, 128.059417725, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.103312174479, -0.551864624023, 85.3056488037, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.192626953125, -0.506698608398, 10.2715162194, -0.0245887914915, 0.0157399724197, 0.00701276365789},
						{0.0742950439453, 0.0889282226563, 0.0626449584961, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.101704915365, 85.38915507, 0.0989074707031, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.256033855936, 11.2598379384, -0.313809602157, -0.0225815432065, 0.014455076712, 0.00644029315521},
						{0.264343261719, -0.664421081543, -1.35547637939, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.237497965495, -0.714238484701, -1.31645202637, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.0728627080503, -0.2135149085, -0.308481631072, -0.023585167349, 0.0150975245658, 0.00672652840655},
						{-0.061637878418, 0.0854873657227, 0.107391357422, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.0140329996745, 0.0637461344401, 0.132878621419, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.108546381411, 0.107560530953, -0.337525740914, -0.0230833552778, 0.0147763006389, 0.00658341078088},
						{0.223114013672, -0.625984191895, 0.13240814209, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.149032592773, -0.65131632487, 85.4100799561, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.2022028384, -0.487231047257, 10.3245557702, -0.0245887914915, 0.0157399724197, 0.00701276365789},
						{-0.0216827392578, -0.0935211181641, -0.0935516357422, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.0356089274089, -0.0413258870443, -85.3849843343, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.319935010827, 0.39295296047, -11.0414700715, -0.023585167349, 0.0150975245658, 0.00672652840655},
						{0.0442123413086, 0.138984680176, 0.118873596191, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.0570882161458, 0.126780192057, 0.141860961914, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.0999735956607, 0.177134638247, -0.289915001911, -0.0250906035628, 0.0160611963466, 0.00715588128356},
						{0.0402069091797, 0.0645980834961, 0.00829315185547, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.0358378092448, 0.0573323567708, 0.0114440917969, -0.0230833552778, 0.0147763006389, 0.00658341078088, 0.195891007133, 11.0984132186, 0.0475145422894, -0.0245887914915, 0.0157399724197, 0.00701276365789},
						{-0.0414505004883, -0.109680175781, -0.111907958984, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.0397135416667, -0.109736124674, -0.120346069336, -0.0230833552778, 0.0147763006389, 0.00658341078088, -0.0410388448964, 0.034644085428, -11.0982122007, -0.0230833552778, 0.0147763006389, 0.00658341078088},
						{0.328506469727, -0.861038208008, -1.31101989746, -0.0288541940972, 0.0184703757986, 0.0082292634761, 0.423233032227, -0.926717122396, -1.1057027181, -0.0269305811574, 0.0172390174121, 0.00768064591102, 0.124422156292, -0.256213644276, -0.18548185929, -0.0245887914915, 0.0157399724197, 0.00701276365789},
				},
				new int [] {Gesture_SwipeRight, Gesture_None, Gesture_Whitescreen, Gesture_SwipeLeft, Gesture_None, Gesture_None, Gesture_SwipeRight, Gesture_None, Gesture_SwipeRight, Gesture_Whitescreen, Gesture_None, Gesture_None, Gesture_None, Gesture_None, Gesture_None, Gesture_Whitescreen});

		// If more than 60% of predictions in the last 10 steps are of the same kind, we execute.
		// This was carefully selected after having analyzed the influence of the window size
		// on the recognition rate.
		predictionWindow = new PredictionTimeWindow(10, 0.6f, Gesture_None);
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
