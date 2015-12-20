import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.utils.Pair;


public class PredictRankingRecall {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		//Load a ranked list of documents and their results
		String pathToResults = "results/IAT/adaptiveRankSVM.data";
		SortedCurve curve = (SortedCurve) SerializationHelper.read(pathToResults);
		List<String> sortedResults = curve.getSortedDocuments();
		Map<String,Integer> relevanceResults = curve.getRelevantDocuments();
		
		//TODO: Obtain a sample of the documents (I should try different approaches here... top-K, several parts of the ranking?)
		Set<String> sample = getSample(sortedResults);
		
		
		//TODO: Find a distribution that explains the ranking (I should also try different methods to do this)
		//Here I have an opportunity for a good experimentation:
		// - What types of estimation fit better in our data?
		// - Evaluate time (this estimation should be fast), and quality of the estimation
		double sParameter = getSParameter(sample);
		
		//Estimate the recall at all points (if I keep using a Zipf Law, I just need to compute the cumulative function like I am doing now)
		Pair<double[],double[]> realRecallCurve = curve.getCurveRetrieval();
		double[] estimatedRecall = getEstimatedRecall(sParameter,sortedResults.size());
		Pair<double[],double[]> estimatedRecallCurve = new Pair<double[], double[]>(realRecallCurve.first(), estimatedRecall);
		
		//TODO: Compare the recall at all points with the real recall (for this, we will always assume that the IE program is perfect...
		//alleviating this assumption is the challenge that we will have to face later)
		RankingMethodCurve realCurve = new ExplicitCurve(realRecallCurve.first(), realRecallCurve.second());
		RankingMethodCurve predictedCurve = new ExplicitCurve(estimatedRecallCurve.first(), estimatedRecallCurve.second());
		ExcelGenerator gen = new ExcelGenerator();
		gen.addRankingCurve("Real", realCurve);
		gen.addRankingCurve("Predicted", predictedCurve);
		gen.generateR("test.png", 50, true);
	}

	private static double[] getEstimatedRecall(double sParameter, int size) {
		double[] result = new double[size+1];
		double sum=0;
		for(int i=0; i<size+1; i++){
			if(i==0){
				result[i]=0;
			}else{
				sum+=Math.pow(i, -sParameter);
				result[i]=sum;
			}
			
		}
		for(int i=0; i<size+1; i++){
			result[i]/=sum;
		}
		return result;
	}

	private static double getSParameter(Set<String> sample) {
		return 0.85;
	}

	private static Set<String> getSample(List<String> sortedResults) {
		// TODO Auto-generated method stub
		return null;
	}
}
