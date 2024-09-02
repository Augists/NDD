package apkeep.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class DeltaParser {
	static String network_name;
	static String update_file;
	static String new_update_file;
	
	public DeltaParser()
	{
		
	}
	public static void updateRules(String name) throws IOException
	{
		network_name = name;
		update_file = apkeep.utils.Parameters.dataset_path + network_name+"/"
				+ network_name + "_rules";
		new_update_file = update_file + "_new";
		
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(update_file));
		} catch (FileNotFoundException e) {
			System.err.println ("File not found! " + update_file); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		File output_file = new File(new_update_file);
		FileWriter output_writer = new FileWriter(output_file);

		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null)
		{
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			
			String line = tokens[0] + " fwd";
			for(int i=1;i<tokens.length;i++) {
				line = line + " " + tokens[i];
			}
			output_writer.write(line+"\n");
		}
		output_writer.close();
	}
	public static void main(String[] args) throws IOException
	{
//		updateRules("4switch");
//		updateRules("airtel1");
		updateRules("airtel2");
	}
}
