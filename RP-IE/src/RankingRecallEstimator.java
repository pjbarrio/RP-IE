import java.util.Map;
import java.util.Set;

import edu.columbia.cs.utils.Pair;


public class RankingRecallEstimator {
	
	private double w;
	private double fraction;
	private ParameterEstimator est;
	
	public RankingRecallEstimator(Map<String,Float> scoresCollection, Set<String> usefulSample,
			ParameterEstimator est){
		this.est=est;
		Pair<Double,Double> parameters = est.getParameterEstimators(scoresCollection, usefulSample);
		this.w=parameters.first();
		this.fraction=parameters.second();
	}
	
	public double getNumNecessaryDocuments(double R, int N){
		int m1 = (int) Math.round(fraction*N);
		int m2= N-m1;
		double exp = Math.pow(1-R, 1.0/w);
		double m2Part = m2*(1-exp);
		double m1Part = m1*R;
		return m1Part+m2Part;
	}
	
	public double getRecallNewtonMethod(int n, int N, int iter){
		int m1 = (int) Math.round(fraction*N);
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
	
	
}
