import java.util.Map;


public class RankingScoreProbabilityDistributionCreator implements
		ProbabilityDistributionCreator {

	@Override
	public ProbabilityDistribution getProbabilityDistribution(Map<String,Float> ranking) {
		return new RankingScoreProbabilityDistribution(ranking);
	}

}
