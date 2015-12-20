import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.columbia.cs.utils.Pair;


public class SampleBasedParameterEstimator implements ParameterEstimator{
	
	private SamplingTechnique sampler;
	
	public SampleBasedParameterEstimator(SamplingTechnique sampler){
		this.sampler=sampler;
	}

	@Override
	public Pair<Double, Double> getParameterEstimators(Map<String, Float> scoresCollection, Set<String> usefulSample) {
		int numUseful=0;
		int numUseless=0;
		for(Entry<String,Float> entry : scoresCollection.entrySet()){
			String doc = entry.getKey();
			float score = entry.getValue();
			if(usefulSample.contains(doc)){
				numUseful++;
			}else{
				numUseless++;
			}
		}
		double frac = (double)numUseful/(double)(numUseful+numUseless);
		
		List<String> sample = sampler.sampleDocuments(scoresCollection,scoresCollection.size());
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
