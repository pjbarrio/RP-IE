import java.util.Map;


public class UniformProbabilityDistributionCreator implements
		ProbabilityDistributionCreator {

	@Override
	public ProbabilityDistribution getProbabilityDistribution(Map<String,Float> ranking) {
		return new UniformProbabilityDistribution(ranking.size());
	}

}
