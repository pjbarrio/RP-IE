
public class LinearDecreaseProbabilityDistribution implements
		ProbabilityDistribution {
	
	private double[] dist;
	
	public LinearDecreaseProbabilityDistribution(int size){
		dist = new double[size];
		int sum = ((size-1)*size)/2;
		for(int i=0; i<size; i++){
			dist[size-1-i]=(double)i/(double)sum;
		}
	}

	@Override
	public double getProbability(int pos) {
		return dist[pos];
	}

}
