package MixtureModel;
import edu.columbia.cs.utils.Pair;


public class ExplicitCurve implements RankingMethodCurve {
	
	private double[] x;
	private double[] y;
	
	public ExplicitCurve(double[] x, double[] y){
		this.x=x;
		this.y=y;
	}

	@Override
	public Pair<double[], double[]> getCurve() {
		return new Pair<double[], double[]>(x, y);
	}

}
