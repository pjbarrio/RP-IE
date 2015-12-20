package MixtureModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;

import edu.columbia.cs.utils.Pair;

public class ChiSquareTestExecutor<E,N extends Number> {
	
	private int numBuckets;
	private int minBuckets;
	private double significance;
	private DistributionFactory<N> factory;
	
	public ChiSquareTestExecutor(int numBuckets, int minBuckets, double significance,
			DistributionFactory<N> factory){
		this.numBuckets=numBuckets;
		this.minBuckets=minBuckets;
		this.significance=significance;
		this.factory=factory;
	}
	
	public boolean performFitTest(Map<E, N> scoresCollection){
		double min = Float.MAX_VALUE;
		double max = Float.MIN_NORMAL;
		
		for(Entry<E, N> entry : scoresCollection.entrySet()){
			double current = entry.getValue().doubleValue();
			min = Math.min(min, current);
			max = Math.max(max, current);
		}
		
		double splitter = (max-min)/numBuckets;
		int[] freqs = new int[numBuckets];
		double[] labels = new double[numBuckets];
		for(int i=0; i<numBuckets; i++){
			double maxBucket = min+((i+1)*splitter);
			labels[i]=maxBucket;
		}
		for(Entry<E, N> entry : scoresCollection.entrySet()){
			double floatIndex = (entry.getValue().doubleValue()-min)/splitter;
			if(entry.getValue().doubleValue()==max){
				floatIndex--;
			}
			int index = (int) Math.floor(floatIndex);
			freqs[index]++;
		}
		
		
		double[] expectedFreq = new double[numBuckets];
		
		AbstractRealDistribution dist = factory.getDistribution(scoresCollection.values());
		
		int N = scoresCollection.size();
		for(int i=0; i<numBuckets; i++){
			if(i==0){
				expectedFreq[i]=N*dist.cumulativeProbability(labels[i]);
			}else if(i==numBuckets-1){
				expectedFreq[i]=N*(1-dist.cumulativeProbability(labels[i-1]));
			}else{
				expectedFreq[i]=N*dist.probability(labels[i-1], labels[i]);
			}
		}
		
		Pair<long[],double[]> regrouped = regroup(freqs,expectedFreq, minBuckets);
		long[] observed=regrouped.first();
		expectedFreq=regrouped.second();
		
		ChiSquareTest test = new ChiSquareTest();
		System.out.println(test.chiSquareTest(expectedFreq, observed));
		return test.chiSquareTest(expectedFreq, observed, significance);
	}

	private Pair<long[],double[]> regroup(int[] freqs, double[] expectedFreq,int minimum) {
		int currentFreqAcc=0;
		double currentExpeAcc=0;
		List<Integer> newFrequencies = new ArrayList<Integer>();
		List<Double> newExpected = new ArrayList<Double>();
		for(int i=0; i<freqs.length; i++){
			currentFreqAcc+=freqs[i];
			currentExpeAcc+=expectedFreq[i];
			if(currentExpeAcc<minimum){
				if(newExpected.size()==0 || (i+1<freqs.length && newExpected.get(newExpected.size()-1)>freqs[i+1])){
				}else{
					newFrequencies.add(newFrequencies.remove(newExpected.size()-1)+currentFreqAcc);
					newExpected.add(newExpected.remove(newExpected.size()-1)+currentExpeAcc);
					currentExpeAcc=0;
					currentFreqAcc=0;
				}
			}else{
				newFrequencies.add(currentFreqAcc);
				newExpected.add(currentExpeAcc);
				currentExpeAcc=0;
				currentFreqAcc=0;
			}
		}
		
		if(currentExpeAcc!=0 || currentFreqAcc!=0){
			newFrequencies.add(newFrequencies.remove(newExpected.size()-1)+currentFreqAcc);
			newExpected.add(newExpected.remove(newExpected.size()-1)+currentExpeAcc);
			currentExpeAcc=0;
			currentFreqAcc=0;
		}
		
		long[] resultF = new long[newFrequencies.size()];
		double[] resultE = new double[newExpected.size()];
		for(int i=0; i<newFrequencies.size(); i++){
			resultF[i]=newFrequencies.get(i);
			resultE[i]=newExpected.get(i);
		}
		
		return new Pair<long[], double[]>(resultF, resultE);
	}
}
