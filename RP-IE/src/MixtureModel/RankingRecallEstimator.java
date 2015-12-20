package MixtureModel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.columbia.cs.utils.Pair;


public class RankingRecallEstimator {
	
	private double w;
	private double fractionDistribution;
	private double fractionUseful;
	
	public RankingRecallEstimator(Map<String,Float> scoresCollection, Set<String> usefulSample,
			RankingOutlierDetector outlierDetector, ParameterEstimator est){
		Set<String> wellModeledSample = new HashSet<String>();
		for(String doc : scoresCollection.keySet()){
			if(!outlierDetector.isOutlier(doc)){
				wellModeledSample.add(doc);
			}
		}
		double[] parameters = est.getParameterEstimators(scoresCollection, usefulSample, wellModeledSample);
		this.w=parameters[0];
		this.fractionDistribution=parameters[1];
		this.fractionUseful=parameters[2];
	}
	
	public double getNumNecessaryDocuments(double R, int N){
		throw new UnsupportedOperationException();
	}
	
	public double getRecallNewtonMethod(int n, int N, int iter){
		return fractionDistribution*getRecallWallenius(n*fractionDistribution, N*fractionDistribution, iter)
				+ (1-fractionDistribution)*getRecallHypergeometric(n*(1-fractionDistribution), N*(1-fractionDistribution));
	}
	
	private double getRecallWallenius(double n, double N, int iter){
		if(N==0){
			return 0;
		}
		double m1 = (int) Math.round(fractionUseful*N);
		double minResult = Math.max(0, n-(N-m1));
		double maxResult = Math.min(n, m1);
		double result = (maxResult+minResult)/2.0;
				
		double k1 = w/(N-m1);
		double k2 = 1.0/m1;
		for(int i=0; i<iter; i++){
			double coefExp = 1.0-(n-result)/(N-m1);
			double num = Math.pow(coefExp, w)-1+(result/m1);
			double den = k1*Math.pow(coefExp, w-1)+k2;
			double newResult = result - num/den;
			if(result==newResult){
				//If the result did not change it means that we will not make more progress.
				break;
			}
			result = newResult;
		}
				
		return result/m1;
	}
	
	private double getRecallHypergeometric(double n, double N){
		if(N!=0){
			return n/N;
		}else{
			return 0;
		}
	}
	
	
}
