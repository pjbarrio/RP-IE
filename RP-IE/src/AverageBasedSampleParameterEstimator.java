import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.columbia.cs.utils.Pair;


public class AverageBasedSampleParameterEstimator implements ParameterEstimator {
	
	private List<String> sample;
	private double frac;
	private int N;

	public AverageBasedSampleParameterEstimator(List<String> sample,double frac, int N) {
		this.sample=sample;
		this.frac=frac;
		this.N=N;
	}

	@Override
	public Pair<Double, Double> getParameterEstimators(
			Map<String, Float> scoresCollection, Set<String> usefulSample) {
		int numUsefulSample = 0;
		for(String document : sample){
			if(usefulSample.contains(document)){
				numUsefulSample++;
			}
		}
		int sampleSize = sample.size();
		double m1 = N*frac;
		double m2 = N*(1.0-frac);
		
		double num = Math.log(1.0-(numUsefulSample/m1));
		double den = Math.log(1.0-((sampleSize-numUsefulSample)/m2));
		double w = num/den;
		
		System.out.println("w=" + w);
		System.out.println("frac=" + frac);
		
		return new Pair<Double, Double>(w, frac);
	}

}
