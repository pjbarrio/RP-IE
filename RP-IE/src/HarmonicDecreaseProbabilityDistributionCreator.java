import java.util.Map;


public class HarmonicDecreaseProbabilityDistributionCreator implements
		ProbabilityDistributionCreator {
	
	private double coef;
	
	public HarmonicDecreaseProbabilityDistributionCreator(double coef){
		this.coef=coef;
	}
	
	@Override
	public ProbabilityDistribution getProbabilityDistribution(Map<String,Float> ranking) {
		return new HarmonicDecreaseProbabilityDistribution(coef, ranking.size());
	}

}
