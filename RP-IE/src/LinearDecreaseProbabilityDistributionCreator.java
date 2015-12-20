import java.util.Map;


public class LinearDecreaseProbabilityDistributionCreator implements
		ProbabilityDistributionCreator {

	@Override
	public ProbabilityDistribution getProbabilityDistribution(Map<String,Float> ranking) {
		return new LinearDecreaseProbabilityDistribution(ranking.size());
	}

}
