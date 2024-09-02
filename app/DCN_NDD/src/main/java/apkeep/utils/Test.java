package apkeep.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import common.PositionTuple;
import javafx.util.Pair;

public class Test {

	public Test() {
		// TODO Auto-generated constructor stub
	}
	public static void printLoopSimplified() throws IOException
	{
		String name = "huawei_loop";
		String dropbox = "C:/Users/reaso/Dropbox/"+name+"/reachability/";
		String file = "reachange_"+name+"_post0";
		String input = dropbox + file;

		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(input));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + input); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		HashMap<String, HashSet<Pair<String,String>>> ap_reach = new HashMap<String, HashSet<Pair<String,String>>>();
		HashMap<String,HashMap<String,String>> old_pair_path = new HashMap<String,HashMap<String,String>>();
		HashMap<String,HashMap<String,String>> new_pair_path = new HashMap<String,HashMap<String,String>>();
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			// get ap and prefix
			String linestr = OneLine.trim();
			String ip = linestr;
			// get old path
			OneLine = fileBufferReader.readLine();
			OneLine = fileBufferReader.readLine();
			String old_pair = OneLine.trim();
			String[] tokens = old_pair.split("->");
			String h1 = tokens[0].substring(0,6);
			if(h1.startsWith("null")) h1 = tokens[0];
			String h2 = tokens[1];
			old_pair = h1+"->"+h2;
			OneLine = fileBufferReader.readLine();
			String old_path = OneLine.trim();
			tokens = old_path.split(",");
			old_path= "";
			for(int i = 4;i<tokens.length-1;i++) {
				old_path = old_path+tokens[i]+",";
			}
			old_path = old_path+tokens[tokens.length-1].substring(0,tokens[tokens.length-1].length()-1);
			//get new path
			OneLine = fileBufferReader.readLine();
			OneLine = fileBufferReader.readLine();
			String new_pair = OneLine.trim();
			tokens = new_pair.split("->");
			h1 = tokens[0].substring(0,6);
			if(h1.startsWith("null")) h1 = tokens[0];
			h2 = tokens[1];
			new_pair = h1+"->"+h2;
			OneLine = fileBufferReader.readLine();
			String new_path = OneLine.trim();
			tokens = new_path.split(",");
			new_path= "";
			for(int i = 4;i<tokens.length-1;i++) {
				new_path = new_path+tokens[i]+",";
			}
			new_path = new_path+tokens[tokens.length-1].substring(0,tokens[tokens.length-1].length()-1);
			// store
			HashSet<Pair<String,String>> pair_change = ap_reach.get(ip);
			if(pair_change == null) {
				pair_change = new HashSet<Pair<String,String>>();
			}
			pair_change.add(new Pair<String,String>(old_pair,new_pair));
			ap_reach.put(ip, pair_change);
			HashMap<String,String> pair_path = old_pair_path.get(ip);
			if(pair_path == null) {
				pair_path = new HashMap<String,String>();
			}
			pair_path.put(old_pair, old_path);
			old_pair_path.put(ip, pair_path);
			pair_path = new_pair_path.get(ip);
			if(pair_path == null) {
				pair_path = new HashMap<String,String>();
			}
			pair_path.put(new_pair, new_path);
			new_pair_path.put(ip, pair_path);
		}
		String outfile = dropbox+"update_simplified";
		File output_file = new File(outfile);
		FileWriter output_writer = new FileWriter(output_file);
		for(String ip:ap_reach.keySet()) {
			output_writer.write(ip+"\n");
			for(Pair<String,String> pairs:ap_reach.get(ip)) {
				output_writer.write(pairs.getKey()+" ====> "+pairs.getValue()+"\n");
				output_writer.write("old path: "+old_pair_path.get(ip).get(pairs.getKey())+"\n");
				output_writer.write("new path: "+new_pair_path.get(ip).get(pairs.getValue())+"\n");
			}
		}
		output_writer.close();
	}
	public static void printBlackholeSimplified() throws IOException{
		String name = "huawei_blackhole";
		String dropbox = "C:/Users/reason/Dropbox/"+name+"/"+name+"/reachability/";
		String file = "update_post0_base";
		String input = dropbox + file;

		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(input));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + input); // for debugging
			System.exit (0); // Stop program if no file found
		}
		HashMap<String,HashSet<String>> pairs_ips = new HashMap<String,HashSet<String>>();
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			// get ap and prefix
			String linestr = OneLine.trim();
			String[] tokens = linestr.split("->");
			if(tokens.length != 2) continue;
			String h1 = tokens[0].substring(0,8);
			String h2 = tokens[1];
			String pair = h1+"->"+h2;
			OneLine = fileBufferReader.readLine();
			linestr = OneLine.trim();
			linestr = linestr.substring(1,linestr.length()-1);
			String[] ips = linestr.split(",");
			
			HashSet<String> pair_ips = pairs_ips.get(pair);
			if(pair_ips == null) pair_ips = new HashSet<String>();
			for(String ip : ips) {
				ip = ip.trim();
				pair_ips.add(ip);
			}
			pairs_ips.put(pair, pair_ips);
		}
		String outfile = dropbox+"results_simplified";
		File output_file = new File(outfile);
		FileWriter output_writer = new FileWriter(output_file);
		for(String pair:pairs_ips.keySet()) {
			output_writer.write(pair+"\n");
			output_writer.write(pairs_ips.get(pair)+"\n");
		}
		output_writer.close();
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String name = "fattree12_ospf_cost";
		String base = Parameters.dataset_path+name+"/parsed/base";
		String base1 = Parameters.dataset_path+name+"/test/base-1";
		
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(base));
		} catch (FileNotFoundException e) {
			System.err.println ("File not found! " + base); // for debugging
			System.exit (0); // Stop program if no file found
		}
		File output_file = new File(base1);
		FileWriter output_writer = new FileWriter(output_file);
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null)
		{
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			if(tokens[2].equals("c30") || tokens[2].equals("c31")) {
				output_writer.write(linestr+"\n");
			}
		}
		output_writer.close();
	}

}
