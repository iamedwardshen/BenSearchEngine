/*
 * test code of file manipulation
 */
package benjamin.index;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class FileReadWriteHandler {
	private static final int BUFFERSIZE = 20*1000*1000;
	
	//test use
	public static void unGzip(File infile, File outFile) throws IOException {
		  GZIPInputStream gin = new GZIPInputStream(new FileInputStream(infile), BUFFERSIZE);
		  FileOutputStream fos = new FileOutputStream(outFile);
		  byte[] buf = new byte[100000];
		  int len;
		  while ( ( len = gin.read(buf) ) > 0 )
		    fos.write(buf, 0, len);
		  gin.close();
		  fos.close();
	}
	
	//no use
	public static int[] readGZFileToInts(File inFile){
		ArrayList<Integer> tempArray = new ArrayList<>();
		try{
			GZIPInputStream gin = new GZIPInputStream(new FileInputStream(inFile), BUFFERSIZE);
			byte[] buf = new byte[100000];
			int len;
			while ( ( len = gin.read(buf) ) > 0 ){
				for(int i = 0; i < len / 4; i += 4){
					int temp = (buf[i] & 0xFF) << 24 | (buf[i + 1] & 0xFF) << 16 |
							(buf[i + 2] & 0xFF) << 8 | (buf[i + 3] & 0xFF);
					tempArray.add(temp);
				}
			}
			gin.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		int[] result = new int[tempArray.size()];
		for(int i = 0; i < tempArray.size(); i++) result[i] = tempArray.get(i);
		return result;
	}
	
	//log
	public static void log(String line){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter("log", true));
			writer.write(line + "\n");
			writer.close();
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//test use
	public static void testBufferedWriter(){
		try{
			GZIPOutputStream goutFinalIndex = 
					new GZIPOutputStream(new FileOutputStream("testBW"), BUFFERSIZE);
			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(goutFinalIndex, "ASCII"));
			writer.write("adfgt名制");
			writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//test use
	public static String convertByteToBinary(byte b){
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < 8; i++){
			sb.append((b & 0x80) >>> 7);
			b <<= 1;
		}
		return sb.toString();
	}

}

