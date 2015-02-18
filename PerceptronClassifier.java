import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * 
 * @author Arvind Krishna Parthasarathy
 * 
 */
public class PerceptronClassifier {

	// Step size
	private final static double eta = 0.1;
	private final static int num_iterations = 80;
	private final static String HAM = "ham";
	private final static String SPAM = "spam";
	static Boolean removeStopWords = true;
	private static Map<String, Map<String, Integer>> fileNameHamTokenCountMap;
	private static Map<String, Map<String, Integer>> fileNameSpamTokenCountMap;
	private static Map<String, Double> tokenWeightMap;
	private static ArrayList<String> vocabList;

	/**
	 * 
	 * @param rootDir
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("Select a dataset: \n1. Dataset 1\n2. Enron1 \n3. Enron3\nEnter a number....\n");
		Scanner sc = new Scanner(System.in);
		String dataset ="";
	    int k = sc.nextInt();
	    if(k == 1){
	    	dataset = "dataset1";
	    }
	    else if(k == 2){
	    	dataset = "enron1";
	    }
	    else if(k == 3){
	    	dataset = "enron4";
	    }
	    else{
	    	System.out.println("Not a valid dataset");
	    	System.exit(1);
	    }
		String rootDir="datasets/"+dataset;
		String hamTrainingSetFolderPath = rootDir + File.separator + "train"
				+ File.separator + HAM;
		String spamTrainingSetFolderPath = rootDir + File.separator + "train"
				+ File.separator + SPAM;

		ArrayList<String> hamFiles = FileUtils
				.getListofFiles(hamTrainingSetFolderPath);
		ArrayList<String> spamFiles = FileUtils
				.getListofFiles(spamTrainingSetFolderPath);
		tokenWeightMap = new HashMap<String, Double>();
		fileNameHamTokenCountMap = new HashMap<String, Map<String, Integer>>();
		fileNameSpamTokenCountMap = new HashMap<String, Map<String, Integer>>();

		try {
			System.out.println("Training Phase..");
			buildDataMatrix(rootDir, hamFiles, spamFiles);
			for (int i = 0; i < num_iterations; i++) {
				for (String file : hamFiles) {
					train(file, HAM);
				}
				for (String file : spamFiles) {
					train(file, SPAM);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Testing Phase...");
		String hamTestingSetFolderPath = rootDir + File.separator + "test"
				+ File.separator + HAM;
		String spamTestingSetFolderPath = rootDir + File.separator + "test"
				+ File.separator + SPAM;
		int numDocuments = 0;
		int numSpamDocuments = 0;
		int numHamDocuments = 0;
		int numCorrectClassification = 0;
		int numSpamCorrectClassification = 0;
		int numHamCorrectClassification = 0;
		ArrayList<String> testFiles = FileUtils
				.getListofFiles(hamTestingSetFolderPath);
		numDocuments += testFiles.size();
		numHamDocuments = testFiles.size();
		for (String file : testFiles) {
			boolean result = test(hamTestingSetFolderPath + File.separator
					+ file, HAM);
			if (result == true) {
				numHamCorrectClassification++;
				numCorrectClassification++;
			}
		}

		testFiles = FileUtils.getListofFiles(spamTestingSetFolderPath);
		numDocuments += testFiles.size();
		numSpamDocuments = testFiles.size();
		for (String file : testFiles) {
			boolean result = test(spamTestingSetFolderPath + File.separator
					+ file, SPAM);
			if (result == true) {
				numSpamCorrectClassification++;
				numCorrectClassification++;
			}
		}
		
		
		System.out.println("Perceptron Learning Rule Classification Result");
		System.out.println("Total number of test documents:"+ numDocuments);
		System.out.println("Total number of ham documents:"+numHamDocuments);
		System.out.println("Total number of spam documents:"+numSpamDocuments);
		
		System.out.println("Number of ham documents correctly classified :"
				+ numHamCorrectClassification);
		System.out.println("Number of spam documents correctly classified :"
				+ numSpamCorrectClassification);
		
		System.out.println("Accuracy :" + 100
				* (double) numCorrectClassification / (double) numDocuments);
		System.out.println("Precision :"+ (double) numSpamCorrectClassification 
				/ (double) (numSpamCorrectClassification+(numHamDocuments-numHamCorrectClassification)));
		System.out.println("Recall :"+ (double) numSpamCorrectClassification 
				/ (double) (numSpamCorrectClassification+(numSpamDocuments-numSpamCorrectClassification)));
		
		cleanup();

	}

	private static void cleanup() {
		fileNameHamTokenCountMap =null;
		fileNameSpamTokenCountMap =null;
		tokenWeightMap = null;
		vocabList = null;
	}

	/**
	 * 
	 * Read all training data to a map for easy access.
	 * 
	 * @param rootDir
	 * @param hamFiles
	 * @param spamFiles
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void buildDataMatrix(String rootDir,
			ArrayList<String> hamFiles, ArrayList<String> spamFiles)
			throws FileNotFoundException, IOException {

		String hamTrainingSetFolderPath = rootDir + File.separator + "train"
				+ File.separator + HAM;
		String spamTrainingSetFolderPath = rootDir + File.separator + "train"
				+ File.separator + SPAM;
		ArrayList<String> hamTokens = new ArrayList<String>();
		ArrayList<String> spamTokens = new ArrayList<String>();
		ArrayList<String> tokens = new ArrayList<String>();

		for (String file : hamFiles) {
			hamTokens = FileUtils
					.retrieveTokensFromFile(hamTrainingSetFolderPath
							+ File.separator + file, removeStopWords);
			fileNameHamTokenCountMap.put(file, getTokenCountMap(hamTokens));
			tokens.addAll(hamTokens);
		}

		for (String file : spamFiles) {
			spamTokens = FileUtils
					.retrieveTokensFromFile(spamTrainingSetFolderPath
							+ File.separator + file, removeStopWords);
			fileNameSpamTokenCountMap.put(file, getTokenCountMap(spamTokens));
			tokens.addAll(spamTokens);
		}

		Set<String> vocabSet = getTokensSetFromArray(tokens);
		vocabList = new ArrayList<String>(vocabSet);
		// Weight 0
		vocabList.add(0, "##W0##");

		// Assign random weights
		for (String token : vocabList) {
			tokenWeightMap.put(token, Math.random());
		}
	}

	/**
	 * 
	 * Train the classifier using the stored training data.
	 * 
	 * @param fileName
	 * @param category
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void train(String fileName, String category)
			throws FileNotFoundException, IOException {

		double vectorProduct = 0;
		int output = 1;
		if (category.equals(SPAM)) {
			Map<String, Integer> tokenCntMap = fileNameSpamTokenCountMap
					.get(fileName);
			Set<String> tokens = tokenCntMap.keySet();
			ArrayList<String> tokenList = new ArrayList<String>(tokens);
			for (String token : tokenList) {
				vectorProduct += tokenWeightMap.get(token)
						* tokenCntMap.get(token);
			}
			vectorProduct += tokenWeightMap.get("##W0##");
			if (vectorProduct <= 0)
				output = -1;
			if (output > 0) {
				for (String token : tokenList) {
					tokenWeightMap.put(token, tokenWeightMap.get(token) + eta
							* (0 - output) * tokenCntMap.get(token));
				}
			}
		}
		if (category.equals(HAM)) {

			Map<String, Integer> tokenCntMap = fileNameHamTokenCountMap
					.get(fileName);
			Set<String> tokens = tokenCntMap.keySet();
			ArrayList<String> tokenList = new ArrayList<String>(tokens);
			for (String token : tokenList) {
				vectorProduct += tokenWeightMap.get(token)
						* tokenCntMap.get(token);
			}
			vectorProduct += tokenWeightMap.get("##W0##");
			if (vectorProduct < 0) {
				output = -1;
			}
			if (output < 0) {
				for (String token : tokenList) {
					tokenWeightMap.put(token, tokenWeightMap.get(token) + eta
							* (1 - output) * tokenCntMap.get(token));
				}
			}
		}
	}

	/**
	 * 
	 * Test and report accuracy of the classifier on the test set.
	 * 
	 * @param fileName
	 * @param category
	 * @return
	 * @throws IOException 
	 */
	private static boolean test(String fileName, String category) throws IOException {
		double vectorProduct = 0.0;

		ArrayList<String> testTokens = FileUtils
				.retrieveTokensFromFile(fileName, removeStopWords);
		HashMap<String, Integer> testTokenCountMap = getTokenCountMap(testTokens);
		for (String token : testTokenCountMap.keySet()) {
			if (vocabList.contains(token)) {
				vectorProduct += tokenWeightMap.get(token)
						* testTokenCountMap.get(token);
			}
		}
		vectorProduct += tokenWeightMap.get("##W0##");
		if (vectorProduct > 0.0d && category.equals(HAM)) {
			return true;
		} else if (vectorProduct <= 0.0d && category.equals(SPAM)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get word count map and store it in a hashmap.
	 * 
	 * @param tokens
	 * @return
	 */
	private static HashMap<String, Integer> getTokenCountMap(
			ArrayList<String> tokens) {
		HashMap<String, Integer> tokenCountMap = new HashMap<String, Integer>();
		for (String token : tokens) {
			if (tokenCountMap.containsKey(token)) {
				tokenCountMap.put(token, tokenCountMap.get(token) + 1);
			} else {
				tokenCountMap.put(token, 1);
			}
		}
		return tokenCountMap;
	}

	/**
	 * 
	 * 
	 * @param tokens
	 * @return
	 */
	private static Set<String> getTokensSetFromArray(ArrayList<String> tokens) {
		Set<String> tokenSet = new HashSet<String>();
		for (String s : tokens) {
			tokenSet.add(s);
		}
		return tokenSet;
	}

}