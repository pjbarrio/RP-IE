import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.columbia.cs.utils.Pair;


public class ExplicitSampleParameterEstimator implements ParameterEstimator {
	
	private List<String> sample;
	private double frac;

	public ExplicitSampleParameterEstimator(List<String> sample,double frac) {
		this.sample=sample;
		this.frac=frac;
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
		
		
		double val = (double)sampleSize/(double)numUsefulSample;
		val-=1;
		double den = val*frac;
		double num = (1.0-frac);
		double w = num/den;
		
		System.out.println("w=" + w);
		System.out.println("frac=" + frac);
		
		return new Pair<Double, Double>(w, frac);
	}

}
