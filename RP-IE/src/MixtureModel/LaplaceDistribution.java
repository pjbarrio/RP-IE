package MixtureModel;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

public class LaplaceDistribution extends AbstractRealDistribution{
	
    private double mu;
    private double scale;

	public LaplaceDistribution(double average, double scale) {
		this.mu=average;
		this.scale=scale;
	}

	@Override
	public double cumulativeProbability(double x) {
		
		if(x<mu){
			return 0.5*Math.exp((x-mu)/scale);
		}else{
			return 1-0.5*Math.exp(-(x-mu)/scale);
		}
	}

	@Override
	public double density(double x) {
		return (0.5/scale)*Math.exp(-Math.abs(x-mu)/scale);
	}

	@Override
	public double getNumericalMean() {
		return mu;
	}

	@Override
	public double getNumericalVariance() {
		return 2*scale*scale;
	}

	@Override
	public double getSupportLowerBound() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double getSupportUpperBound() {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public boolean isSupportConnected() {
		return true;
	}

	@Override
	public boolean isSupportLowerBoundInclusive() {
		return false;
	}

	@Override
	public boolean isSupportUpperBoundInclusive() {
		return false;
	}

}
