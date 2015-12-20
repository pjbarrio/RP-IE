package MixtureModel;

import java.util.Map;
import java.util.Map.Entry;

public class ScoreBasedOutliersDetector implements RankingOutlierDetector {
	
	private Map<String, Float> scoresCollection;
	private double average;
	private double stdDev;
	private double threshold;

	public ScoreBasedOutliersDetector(Map<String, Float> scoresCollection, double d) {
		this.scoresCollection=scoresCollection;
		this.threshold=d;
		
		double sum = 0.0;
		for(Entry<String,Float> entry : scoresCollection.entrySet()){
			sum+=entry.getValue();
		}
		average = sum/scoresCollection.size();
		
		double sumDiffSq = 0.0;
		for(Entry<String,Float> entry : scoresCollection.entrySet()){
			sumDiffSq+=Math.pow(entry.getValue()-average,2);
		}
		
		stdDev = Math.sqrt(sumDiffSq/(scoresCollection.size()));
	}

	@Override
	public boolean isOutlier(String doc) {
		double v = scoresCollection.get(doc);
		
		if(v<average+stdDev*threshold && v>average-stdDev*threshold){
			return true;
		}
		
		return false;
	}

}
