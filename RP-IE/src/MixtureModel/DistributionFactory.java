package MixtureModel;

import java.util.Collection;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

public interface DistributionFactory<N extends Number> {
	public AbstractRealDistribution getDistribution(Collection<N> observedValues);
	public String getDistributionName();
}
