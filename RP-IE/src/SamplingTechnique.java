import java.util.List;
import java.util.Map;


public interface SamplingTechnique {
	public String sampleDocument(Map<String, Float> scoresCollection);
	public List<String> sampleDocuments(Map<String, Float> scoresCollection,int numDocs);
}
