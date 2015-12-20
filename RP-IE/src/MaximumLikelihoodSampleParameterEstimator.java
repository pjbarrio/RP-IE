import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.columbia.cs.utils.Pair;


public class MaximumLikelihoodSampleParameterEstimator implements ParameterEstimator {
	
	private List<String> sample;
	private double frac;
	private int N;
	private int iter;

	public MaximumLikelihoodSampleParameterEstimator(List<String> sample,double frac, int N, int iter) {
		this.sample=sample;
		this.frac=frac;
		this.N=N;
		this.iter=iter;
	}

	@Override
	public Pair<Double, Double> getParameterEstimators(
			Map<String, Float> scoresCollection, Set<String> usefulSample) {		
		int[] x = new int[sample.size()];
		int[] m1 = new int[sample.size()];
		int[] m2 = new int[sample.size()];
		
		int initialM1 = (int) Math.round(N*frac);
		int initialM2 = (int) Math.round(N*(1-frac));
		
		int currentUseful = 0;
		int currentUseless = 0;
		for(int i=0; i<sample.size(); i++){
			m1[i]=initialM1-currentUseful;
			m2[i]=initialM2-currentUseless;
			if(usefulSample.contains(sample.get(i))){
				x[i]=1;
				currentUseful++;
			}else{
				x[i]=0;
				currentUseless++;
			}
		}
		
		double w = 1.0;
		double previousW = 0.0;
		for(int i=0; i<iter; i++){
			double sum=0;
			double sumSq=0;
			for(int j=0; j<sample.size(); j++){
				double val = m1[j]/(m1[j]*w+m2[j]);
				sum+=val;
				sumSq+=val*val;
			}
			
			double num = (currentUseful/w)-sum;
			double den = (currentUseful/(w*w))-sumSq;
			
			w=w+(num/den);
			
			if(w==previousW){
				break;
			}
			
			previousW=w;
			System.out.println(i + ":" + w);
		}
		
		System.out.println(w);
		
		return new Pair<Double, Double>(w, frac);
	}

}
