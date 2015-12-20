import java.util.Map;


public class ExponentialDecreaseProbabilityDistributionCreator implements
		ProbabilityDistributionCreator {
	
	private double coef;
	
	public ExponentialDecreaseProbabilityDistributionCreator(double coef){
		this.coef=coef;
	}

	@Override
	public ProbabilityDistribution getProbabilityDistribution(Map<String,Float> ranking) {
		return new ExponentialDecreaseProbabilityDistribution(coef, ranking.size());
	}

}
