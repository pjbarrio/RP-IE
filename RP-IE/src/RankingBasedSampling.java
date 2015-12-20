import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


public class RankingBasedSampling implements SamplingTechnique {
	
	private ProbabilityDistributionCreator creator;
	
	public RankingBasedSampling(ProbabilityDistributionCreator creator){
		this.creator=creator;
	}

	@Override
	public String sampleDocument(Map<String, Float> scoresCollection) {
		List<String> collection = sortCollection(scoresCollection);
		ProbabilityDistribution d = creator.getProbabilityDistribution(scoresCollection);
		return getRandomDocument(collection, d);
	}

	@Override
	public List<String> sampleDocuments(Map<String, Float> scoresCollection,
			int numDocs) {
		List<String> collection = sortCollection(scoresCollection);
		ProbabilityDistribution d = creator.getProbabilityDistribution(scoresCollection);
		List<String> result = new ArrayList<String>();

		for(int i=0; i<numDocs; i++){
			result.add(getRandomDocument(collection, d));
		}

		return result;
	}
	
	private String getRandomDocument(List<String> collection,ProbabilityDistribution d){
		double randomVal = Math.random();
		double currentAccumulatedDist=0;
		int currentVal = 0;
		while(currentAccumulatedDist<=randomVal){
			double posDist = d.getProbability(currentVal);
			currentAccumulatedDist+=posDist;
			currentVal++;
		}

		currentVal--;

		return collection.get(currentVal);
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
}
