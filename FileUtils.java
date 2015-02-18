
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * 
 * @author Arvind Krishna Parthasarathy
 *
 */
public class FileUtils {
	public static ArrayList<String> stopWords = new ArrayList<String>();
	public static ArrayList<String> getListofFiles(String path) {
		System.out.println(path);
		ArrayList<String> files = new ArrayList<String>();
		File file = new File(path);
		String[] fileNames = file.list();
		for (String s : fileNames) {
			files.add(s);
		}
		return files;
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws IOException 
	 */
	public static ArrayList<String> retrieveTokensFromFile(String path, boolean removeStopWords)throws IOException {
		File file = new File(path);
		BufferedReader bufferedReader = null;
		ArrayList<String> tokens = new ArrayList<>();
		bufferedReader = new BufferedReader(new FileReader(file));
		String line;

		while ((line = bufferedReader.readLine()) != null) {
			String[] split = line.split("\\s+");
			if(removeStopWords==true){
				for (String token : split) {
					token = token.replaceAll("[^a-zA-Z]", "");
					token = token.toLowerCase();
					if(!token.isEmpty() && !stopWords.contains(token)){
						tokens.add(token);
					}
				}
			}
			else{
				for (String token : split) {
					token = token.replaceAll("[^a-zA-Z]", "");
					token = token.toLowerCase();
					if(!token.isEmpty()){
						tokens.add(token);
					}
				}
				}
			}

		
		bufferedReader.close();
		return tokens;
	}

	public static void putStopWords(String rootDir) throws IOException{
		rootDir = rootDir+File.separator+"stopwords.txt";
		BufferedReader bufferedReader = null;
		bufferedReader = new BufferedReader(new FileReader(rootDir));
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			stopWords.add(line);
		}
		bufferedReader.close();
	}
	
	

}
