import java.util.Map;


public interface ProbabilityDistributionCreator {
	public ProbabilityDistribution getProbabilityDistribution(Map<String,Float> ranking);
}
