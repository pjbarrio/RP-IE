package MixtureModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

public class LaplaceDistributionFactory<N extends Number> implements DistributionFactory<N> {

	@Override
	public AbstractRealDistribution getDistribution(
			Collection<N> observedValues) {		
		List<N> sortedList = new ArrayList<N>(observedValues);
		Collections.sort(sortedList, new NumberComparator());
		
		int num = sortedList.size();
		double median;
		if(num%2==0){
			median = (sortedList.get(num/2).doubleValue()+sortedList.get((num/2)-1).doubleValue())/2.0;
		}else{
			median = sortedList.get(num/2).doubleValue();
		}
		
		double scale = 0.0;
		
		for(N n : observedValues){
			scale+=Math.abs(n.doubleValue()-median);
		}		
		scale/=(observedValues.size());
				
		return new LaplaceDistribution(median,scale);
	}
	
	private class NumberComparator implements Comparator<Number>{

		@Override
		public int compare(Number arg0, Number arg1) {
			return (int) Math.signum(arg0.doubleValue()-arg1.doubleValue());
		}	
	}
	
	@Override
	public String getDistributionName() {
		return "Laplace Distribution";
	}

}
