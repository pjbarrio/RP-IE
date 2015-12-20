package MixtureModel;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.columbia.cs.utils.Pair;


public class ExplicitSampleParameterEstimator implements ParameterEstimator {
	
	private List<String> sample;
	private double fracUseful;
	private double fracDist;

	public ExplicitSampleParameterEstimator(List<String> sample,double fracUseful, double fracDist) {
		this.sample=sample;
		this.fracUseful=fracUseful;
		this.fracDist=fracDist;
	}

	@Override
	public double[] getParameterEstimators(Map<String, Float> scoresCollection,
			Set<String> usefulSample, Set<String> wellModeledSample) {
		int numUsefulSample = 0;
		for(String document : sample){
			if(usefulSample.contains(document)){
				numUsefulSample++;
			}
		}
		int sampleSize = sample.size();
		
		
		double val = (double)sampleSize/(double)numUsefulSample;
		val-=1;
		double den = val*fracUseful;
		double num = (1.0-fracUseful);
		double w = num/den;
		
		System.out.println("w=" + w);
		System.out.println("frac=" + fracUseful);
		
		return new double[]{w, fracDist, fracUseful};
	}
}
