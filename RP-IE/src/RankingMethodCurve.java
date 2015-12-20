

import java.io.Serializable;

import edu.columbia.cs.utils.Pair;

public interface RankingMethodCurve extends Serializable{
	public Pair<double[],double[]> getCurve();
}
