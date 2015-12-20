import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.columbia.cs.utils.Pair;


public class StupidParameterEstimator implements ParameterEstimator{

	@Override
	public Pair<Double, Double> getParameterEstimators(
			Map<String, Float> scoresCollection, Set<String> usefulSample) {
		double sumUseful = 0;
		int numUseful=0;
		double sumUseless = 0;
		int numUseless=0;
		for(Entry<String,Float> entry : scoresCollection.entrySet()){
			String doc = entry.getKey();
			float score = entry.getValue();
			if(usefulSample.contains(doc)){
				sumUseful+=score;
				numUseful++;
			}else{
				sumUseless+=score;
				numUseless++;
			}
		}

		double w1 = sumUseful/numUseful;
		double w2 = sumUseless/numUseless;
		double w = Math.abs(w1/w2);
		System.out.println("w1=" + w1);
		System.out.println("w2=" + w2);
		System.out.println("w=" + w);
		System.out.println(numUseful);
		System.out.println(numUseless);
		double frac = (double)numUseful/(double)(numUseful+numUseless);
		return new Pair<Double, Double>(w, frac);
	}

}
