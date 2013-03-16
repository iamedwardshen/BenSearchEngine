/*
 * generate pages using urls indexes info
 */
package benjamin.index;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

public class PageGenerator {
	private static final int BUFFERSIZE = 20*1000*1000;
	private String page;
	private String url;
	private int URLINDEX = 0;
	private int LENGTHINDEX = 3;
	private int OKINDEX = 6;
	private String dataFileString = null;
	private String[] indexLines = null;
	private int begin = 0;
	private int currentLine = 0;
	
	public String lastPage = null;
	
	PageGenerator(File dataFile, File indexFile){
		System.out.println(dataFile.getAbsolutePath());
		System.out.println(indexFile.getAbsolutePath());
		this.dataFileString = readGZFileToString(dataFile);
		String indexFileString = readGZFileToString(indexFile);
		this.indexLines = indexFileString.split("\n");
	}
	
	public String getPage(){
		return this.page;
	}
	public String getUrl(){
		return this.url;
	}
	
	//parse next page according to index file's info
	public boolean parseNext(){
		this.page = null;
		this.url = null;
		String line = null;
		String thisPage = null;
		if(currentLine < indexLines.length) line = indexLines[currentLine++];
		else return false;
		
		String[] splitedLine = line.split(" ");
		int length = Integer.valueOf(splitedLine[LENGTHINDEX]);
		if(splitedLine[OKINDEX].equals("ok")){
			this.url = (splitedLine[URLINDEX]);
			if((begin + length) < this.dataFileString.length()){
				thisPage = this.dataFileString.substring(begin, begin + length);
			}
			else thisPage = this.dataFileString.substring(begin);
			this.page = thisPage;
		}
		this.begin += length;
		
		if(thisPage != null) lastPage = thisPage;
		return true;
	}
	
	//read GZ data file from disk and uncompress it
	private String readGZFileToString(File inFile){
		StringBuffer sb = new StringBuffer();
		try{
			GZIPInputStream gin = new GZIPInputStream(new FileInputStream(inFile), BUFFERSIZE);
			byte[] buf = new byte[100000];
			int len;
			while ( ( len = gin.read(buf) ) > 0 ){
				for(int i = 0; i < len; i++){
					sb.append((char) buf[i]);
				}
			}
			gin.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return sb.toString();
	}

}
