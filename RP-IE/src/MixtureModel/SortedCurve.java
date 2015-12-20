package MixtureModel;


import java.util.List;
import java.util.Map;

import edu.columbia.cs.utils.Pair;

public class SortedCurve implements RankingMethodCurve {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8945704237353161950L;
	private int numDocuments;
	private int numTuples;
	private List<String> sortedDocuments;
	private Map<String,Integer> relevantDocuments;
	private String name;
	
	public SortedCurve(int numDocuments, List<String> sortedDocuments, Map<String,Integer> relevantDocuments){
		this.sortedDocuments=sortedDocuments;
		this.relevantDocuments=relevantDocuments;
		this.numDocuments=numDocuments;
		numTuples=0;
		for(Integer numT : relevantDocuments.values()){
			numTuples+=numT;
		}
	}
	
	protected void setRelevantDocuments(Map<String,Integer> relevantDocuments){
		this.relevantDocuments=relevantDocuments;
		numTuples=0;
		for(Integer numT : relevantDocuments.values()){
			numTuples+=numT;
		}
	}
	
	protected void setNumDocuments(int numDocuments){
		this.numDocuments=numDocuments;
	}
	
	protected void setSortedDocuments(List<String> sortedDocuments){
		this.sortedDocuments=sortedDocuments;
	}

	@Override
	public Pair<double[], double[]> getCurve() {
		int numSortedDocuments = sortedDocuments.size();
		int numRelevantDocuments = relevantDocuments.size();
		double[] x = new double[numDocuments+1];
		double[] y = new double[numDocuments+1];
		
		int currentNumRelDocs=0;
		double accPrecision=0;
		double maximumRecall=0;
		for(int i=0; i<=numDocuments; i++){
			x[i] = ((double)i)/((double)numDocuments);
			double rel=0;
			if(i==0){
				y[i] = 0.0;
			}else if(i<=numSortedDocuments){
				String currentDocument = sortedDocuments.get(i-1);
				if(relevantDocuments.containsKey(currentDocument)){
					rel=1.0;
					currentNumRelDocs++;
				}
				y[i] = ((double)currentNumRelDocs)/((double)numRelevantDocuments);
				maximumRecall=Math.max(maximumRecall, y[i]);
			}else{
				y[i] = maximumRecall;
			}
			
			if(i!=0){
				double precisionAtI = (double)currentNumRelDocs/(double)i;
				accPrecision+=precisionAtI*rel;
			}
		}
		
		
		
		return new Pair<double[], double[]>(x, y);
	}
	
	public void setName(String name){
		this.name=name;
	}
	
	public List<String> getSortedDocuments(){
		return sortedDocuments;
	}
	
	public Map<String,Integer> getRelevantDocuments(){
		return relevantDocuments;
	}

}
