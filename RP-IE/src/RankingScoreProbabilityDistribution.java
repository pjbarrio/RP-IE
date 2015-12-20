import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class RankingScoreProbabilityDistribution implements
		ProbabilityDistribution {
	
	private double[] dist;

	public RankingScoreProbabilityDistribution(Map<String, Float> ranking) {
		Map<String,Float> normalizedScores = normalize(ranking);
		float sum = 0;
		for(Entry<String,Float> entry : normalizedScores.entrySet()){
			sum+=entry.getValue();
		}
		for(Entry<String,Float> entry : normalizedScores.entrySet()){
			entry.setValue(entry.getValue()/sum);
		}
		List<String> sortedCollection = sortCollection(normalizedScores);
		dist = new double[sortedCollection.size()];
		for(int i=0; i<sortedCollection.size(); i++){
			dist[i]=normalizedScores.get(sortedCollection.get(i));
			System.out.println(i + "\t" + ranking.get(sortedCollection.get(i)));
		}
	}
	
	private Map<String,Float> normalize(Map<String,Float> ranking){
		Map<String,Float> result = new HashMap<String,Float>();
		
		float min = Float.MAX_VALUE;
		for(Entry<String,Float> entry : ranking.entrySet()){
			min = Math.min(min, entry.getValue());
		}
		
		float shift = min;
		for(Entry<String,Float> entry : ranking.entrySet()){
			result.put(entry.getKey(), entry.getValue()-shift);
		}
		
		return result;
	}
	
	private List<String> sortCollection(Map<String,Float> scoresCollection){
		final Map<String,Float> scores = scoresCollection;
		List<String> collection = new ArrayList<String>(scoresCollection.keySet());
		Collections.sort(collection, new Comparator<String>() {
			@Override
			public int compare(String doc1, String doc2) {
				Float score2 = scores.get(doc2);
				if(score2==null){
					score2=0.0f;
				}
				Float score1 = scores.get(doc1);
				if(score1==null){
					score1=0.0f;
				}
				return (int) Math.signum(score2-score1);
			}
		});
		return collection;
	}

	@Override
	public double getProbability(int pos) {
		return dist[pos];
	}

}
