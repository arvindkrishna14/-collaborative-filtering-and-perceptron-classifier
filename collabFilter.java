import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * 
 * @author Arvind Krishna Parthasarathy
 * 
 */
public class collabFilter {
	TreeMap<String, HashMap<String, Double>>trainingMap = new TreeMap<String, HashMap<String, Double>>();
	TreeMap<String, HashMap<String, Double>>testMap = new TreeMap<String, HashMap<String, Double>>();
	HashMap<String, Double> avgUserRating = new HashMap<String, Double>();
	HashMap<String, Double> correlation = new HashMap<String, Double>();
	HashSet<String> users = new HashSet<String>();
	HashMap<String, HashMap<String,Double>> predictedVotes = new HashMap<String, HashMap<String, Double>>();
	Set<String> movies = new HashSet<String>();
	public void readValuesToMap(String path, boolean train) throws IOException{
		InputStreamReader reader = new InputStreamReader(new FileInputStream(path));
		BufferedReader br = new BufferedReader(reader);
		String line;
		while ((line = br.readLine()) != null) {
			String[] words = line.split(",");
			if(train){
				if(trainingMap.containsKey(words[1])){
					trainingMap.get(words[1]).put(words[0], Double.parseDouble(words[2]));
				}
				else{
					HashMap<String, Double> hm = new HashMap<String, Double>();
					hm.put(words[0], Double.parseDouble(words[2]));
					trainingMap.put(words[1], hm);
				}
				users.add(words[1]);
			}
			else{
				if(testMap.containsKey(words[1])){
					testMap.get(words[1]).put(words[0], Double.parseDouble(words[2]));
				}
				else{
					HashMap<String, Double> hm = new HashMap<String, Double>();
					hm.put(words[0], Double.parseDouble(words[2]));
					testMap.put(words[1], hm);
				}
			}
		}
		br.close();
		reader.close();
	}
	public void getUserAverageRating(){
		for(String userID: trainingMap.keySet()){
			HashMap<String, Double> userRatings = trainingMap.get(userID);
			double total = 0.0;
			double movie = 0;
			double avg = 0.0;
			for(String movieID: userRatings.keySet()){
				movies.add(movieID);
				total = total + userRatings.get(movieID);
				movie++;
			}
			avg = total/movie;
			avgUserRating.put(userID, avg);
		}
	}


	public void calculateWeights(){
		double MAerror = 0.0;
		double RMSerror = 0.0;
		int k = 0;
		int j = 0;
		Iterator<Entry<String, HashMap<String, Double>>> user1 = trainingMap.entrySet().iterator();
		while(user1.hasNext()){
			HashMap<String, Double> weights = new HashMap<String, Double>();
			Entry<String, HashMap<String, Double>> user1Ratings = user1.next();
			Set<String> toVote = new HashSet<String>(movies);
			double normalisingFactor = 0;
			String userID1 = user1Ratings.getKey();
			if(testMap.containsKey(userID1)){
				Set<String> votedMovies = new HashSet<String>();
				votedMovies = trainingMap.get(userID1).keySet();
				toVote.removeAll(votedMovies);
				Iterator<Entry<String, HashMap<String, Double>>> user2 = trainingMap.entrySet().iterator();
				while(user2.hasNext()){
					Entry<String, HashMap<String, Double>> user2Ratings = user2.next();
					String userID2 = user2Ratings.getKey();
					double weight = getSummation(user1Ratings, user2Ratings);
					normalisingFactor += weight;
					weights.put(userID2, weight);
				}

				for(String predictVote: testMap.get(userID1).keySet()){
					double sum = 0;
					double term1, term2, vij;
					for(String otherUser: users){
						term1= weights.get(otherUser);
						if(trainingMap.get(otherUser).containsKey(predictVote)){
							vij=trainingMap.get(otherUser).get(predictVote);
							term2= vij - avgUserRating.get(otherUser);
						}
						else{
							term2 = 0;
						}
						sum = sum+(term1*term2);
					}
					sum = sum*normalisingFactor;
					sum = sum+avgUserRating.get(userID1);
					double errorTerm = sum-testMap.get(userID1).get(predictVote);
					MAerror = MAerror+Math.abs(errorTerm);
					RMSerror = RMSerror +Math.pow(errorTerm,2);
					k++;
					//System.out.println(sum+" "+testMap.get(userID1).get(predictVote));
				}
				j++;
			}
		}
		MAerror = MAerror / k;
		RMSerror = RMSerror / k;
		RMSerror = Math.sqrt(RMSerror);
		System.out.println("Mean absolute error: "+MAerror);
		System.out.println("Root mean square error: "+RMSerror);
	}


	public double getSummation(Entry<String, HashMap<String, Double>> user1, Entry<String, HashMap<String, Double>> user2){
		Set<String> movies2 = new HashSet<String>();
		movies2 = user2.getValue().keySet();
		Set<String> movies1 = new HashSet<String>();
		movies1 = user1.getValue().keySet();
		movies2.retainAll(movies1);
		if(movies2.size()==0){
			return 0;
		}
		double weight;
		double denominator;
		double numerator = 0;
		double mult1 =0.0;
		double mult2=0.0;
		double term1, term2, bd, vaj,vij;
		double avgUser1 = avgUserRating.get(user1.getKey());
		double avgUser2 = avgUserRating.get(user2.getKey());
		for(String movie: movies2){
			vaj = user1.getValue().get(movie);
			term1 = vaj-avgUser1;
			vij = user2.getValue().get(movie);
			term2 = vij-avgUser2;
			bd = (term1*term2);
			numerator = numerator+bd;
			mult1 =mult1+(term1*term1);
			mult2 = mult2+(term2*term2);
		}
		double denominatorsq = mult1*mult2;
		denominator = Math.sqrt(denominatorsq);
		if(numerator==0&&denominator==0){
			return 0;
		}
		weight = numerator/denominator;
		return weight;
	}
	public static void main(String[] args) throws IOException{
		collabFilter cF = new collabFilter();
		String trainingPath= "CollabFiltering/TrainingRatings.txt";
		String testPath = "CollabFiltering/TestingRatings.txt";
		System.out.println("Reading training data.");
		cF.readValuesToMap(trainingPath, true);
		System.out.println("Reading test data.");
		cF.readValuesToMap(testPath, false);
		System.out.println("Calculating average ratings for each user.");
		cF.getUserAverageRating();
		System.out.println("Finding weights and predicting rating. This is going to take some time...");
		cF.calculateWeights();
	}
}
