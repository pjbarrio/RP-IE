import java.util.Map;
import java.util.Set;

import edu.columbia.cs.utils.Pair;


public interface ParameterEstimator {
	public Pair<Double,Double> getParameterEstimators(Map<String,Float> scoresCollection, Set<String> usefulSample);
}
