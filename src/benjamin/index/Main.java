package benjamin.index;

public class Main {
	
	public static void main(String[] args) {
		System.out.println("maxMemory " + java.lang.Runtime.getRuntime().maxMemory());
		//begin index with data folder
		IndexGenerator index = new IndexGenerator("NZ_data/");
		index.beginIndex();
	}

}
