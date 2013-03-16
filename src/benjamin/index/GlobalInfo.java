/*
 * global info of words and docs(urls)
 */
package benjamin.index;

import java.util.ArrayList;
import java.util.HashMap;

public class GlobalInfo {
	//word : wordID
	private static HashMap<String, Integer> indexOfWords = new HashMap<>();
	//words with position as wordID
	private static ArrayList<String> wordsList = new ArrayList<>();
	//urls with position as docID
	private static ArrayList<String> globalDocs = new ArrayList<>();
	//number of cleaned words in a HTML page with position as docID
	private static ArrayList<Integer> globalSizes = new ArrayList<>();
	
	public static int getWordsNum(){
		return wordsList.size();
	}
	
	public static String getWordByID(int wordID){
		return wordsList.get(wordID);
	}
	
	public static ArrayList<String> getGlobalDocs(){
		return globalDocs;
	}
	public static ArrayList<Integer> getGlobalSizes(){
		return globalSizes;
	}
	
	//add a word to global dictionary and return the wordID of that word
	public static int putWord(String word){
		if(indexOfWords.containsKey(word)) return indexOfWords.get(word);
		else{
			wordsList.add(word);
			int index = wordsList.size() - 1;
			indexOfWords.put(word, index);
			return index;
		}
	}
	
	//add an url to the doc array and return the docID of that url
	public static int putDoc(String doc, int size){
		globalDocs.add(doc);
		globalSizes.add(size);
		return globalDocs.size() - 1;
	}

}
