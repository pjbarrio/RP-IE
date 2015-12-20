package MixtureModel;

import java.util.Collection;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

public class GaussianDistributionFactory<N extends Number> implements DistributionFactory<N> {

	@Override
	public AbstractRealDistribution getDistribution(
			Collection<N> observedValues) {
		double sum = 0.0;
		for(N n : observedValues){
			sum+=n.doubleValue();
		}
		double average = sum/observedValues.size();
		double stddev = 0.0;
		
		for(N n : observedValues){
			stddev+=Math.pow(n.doubleValue()-average, 2);
		}
		
		stddev/=(observedValues.size());
		stddev=Math.sqrt(stddev);
				
		return new NormalDistribution(average,stddev);
	}

	@Override
	public String getDistributionName() {
		return "Gaussian Distribution";
	}

}
