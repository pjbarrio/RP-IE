package Clustering;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class ThetaRandomGenerator extends JDKRandomGenerator {

	private boolean thetaTaken;
	
	public ThetaRandomGenerator() {
		super();
		thetaTaken = false;
	}
	
	@Override
	public int nextInt(int n) {
		if (!thetaTaken){
			thetaTaken = true;
			return 0;
		}
			
		return super.nextInt(n);
	}
	
}
