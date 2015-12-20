
public class ExponentialDecreaseProbabilityDistribution implements
		ProbabilityDistribution {
	
	private double[] dist;
	private double coef;

	public ExponentialDecreaseProbabilityDistribution(double coef, int size) {
		this.coef=coef;
		dist = new double[size];
		double sum = 0;
		for(int i=0; i<size; i++){
			double val = Math.pow(coef, -(i+1));
			sum+=val;
			dist[i]=val;
		}
		for(int i=0; i<size; i++){
			dist[i]/=sum;
		}
	}

	@Override
	public double getProbability(int pos) {
		return dist[pos];
	}

}
