package MixtureModel;
import java.util.Map;
import java.util.Set;

import edu.columbia.cs.utils.Pair;


public interface ParameterEstimator {
	public double[] getParameterEstimators(Map<String,Float> scoresCollection, Set<String> usefulSample, Set<String> wellModeledSample);
}
