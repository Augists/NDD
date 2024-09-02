package apkeep.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONObject;
import org.json.JSONTokener;

public class DCNUpdateTools {

	public static void printUpdateFile(String net,String state0,String state1, String post) throws IOException {
		HashSet<String> spst0 = new HashSet<String>();
		HashSet<String> spst1 = new HashSet<String>();
		
//		String net = "DCN_xjtu";
		String input = Parameters.dataset_path+net+"/parsed/";
//		String state0 = "svn-perform-test20-20-200-40";
//		String state0 = "svn-perform-test20-20-200-40_post0";
//		String state0 = "svn-perform-test20-20-200-40_post1";
//		String state0 = "svn-perform-test20-20-200-40_post2";
//		String state1 = "svn-perform-test20-20-200-40_post3";
//		String state1 = "svn-perform-test20-20-200-40_post4";
		
		String infile = input+state0;
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(infile));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + infile); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			spst0.add(linestr);
		}
		
		infile = input+state1;
		
		try {
			fileBufferReader = new BufferedReader(new FileReader(infile));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + infile); // for debugging
			System.exit (0); // Stop program if no file found
		}
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			spst1.add(linestr);
		}
		
		String outfile = input+"update"+post;
		File output_file = new File(outfile);
		FileWriter output_writer = new FileWriter(output_file);
		for(String rule : spst0) {
			if(!spst1.contains(rule)) {
				String nr = "-";
				nr = nr + rule.substring(1);
				output_writer.write(nr+"\n");
			}
		}
		for(String rule : spst1) {
			if(!spst0.contains(rule)) {
				output_writer.write(rule+"\n");
			}
		}
		output_writer.close();
	}
	public static void printReachabilityFileHuawei(String net,String state0,String state1) throws IOException {
		// TODO Auto-generated method stub
		HashSet<String> spst0 = new HashSet<String>();
		HashSet<String> spst1 = new HashSet<String>();
		
		HashMap<String,HashSet<String>> ips0 = new HashMap<String,HashSet<String>>();
		HashMap<String,HashSet<String>> ips1 = new HashMap<String,HashSet<String>>();
		HashMap<String,HashSet<String>> aps0 = new HashMap<String,HashSet<String>>();
		HashMap<String,HashSet<String>> aps1 = new HashMap<String,HashSet<String>>();
		
		String input = Parameters.dataset_path+net+"/reachability/";
		
		String infile = input+net+state0;
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(infile));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + infile); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split("->");
			if(tokens.length != 2) continue;
//			String h1 = tokens[0].substring(17);
			String h1 = tokens[0];
			String h2 = tokens[1];
			spst0.add(h1+"->"+h2);
			// read prefix
			OneLine = fileBufferReader.readLine();
			linestr = OneLine.trim();
			linestr = linestr.substring(1, linestr.length()-1);
			String[] ips = linestr.split(",");
			HashSet<String> rch_ips = new HashSet<String>();
			for(String ip : ips) {
				String ipprefix = ip.trim();
				rch_ips.add(ipprefix);
			}
			ips0.put(h1+"->"+h2, rch_ips);
			// read aps
//			OneLine = fileBufferReader.readLine();
//			linestr = OneLine.trim();
//			linestr = linestr.substring(1, linestr.length()-1);
//			String[] aps = linestr.split(",");
//			HashSet<String> rch_aps = new HashSet<String>();
//			for(String ap : aps) {
//				String apString = ap.trim();
//				rch_aps.add(apString);
//			}
//			aps0.put(h1+"->"+h2, rch_aps);
		}
		
		infile = input+net+state1;
		
		try {
			fileBufferReader = new BufferedReader(new FileReader(infile));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + infile); // for debugging
			System.exit (0); // Stop program if no file found
		}
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split("->");
			if(tokens.length != 2) continue;
//			String h1 = tokens[0].substring(17);
			String h1 = tokens[0];
			String h2 = tokens[1];
			spst1.add(h1+"->"+h2);
			//read prefix
			OneLine = fileBufferReader.readLine();
			linestr = OneLine.trim();
			linestr = linestr.substring(1, linestr.length()-1);
			String[] ips = linestr.split(",");
			HashSet<String> rch_ips = new HashSet<String>();
			for(String ip : ips) {
				String ipprefix = ip.trim();
				rch_ips.add(ipprefix);
			}
			ips1.put(h1+"->"+h2, rch_ips);
			// read aps
//			OneLine = fileBufferReader.readLine();
//			linestr = OneLine.trim();
//			linestr = linestr.substring(1, linestr.length()-1);
//			String[] aps = linestr.split(",");
//			HashSet<String> rch_aps = new HashSet<String>();
//			for(String ap : aps) {
//				String apString = ap.trim();
//				rch_aps.add(apString);
//			}
//			aps1.put(h1+"->"+h2, rch_aps);
		}
		
		String outfile = input+"update"+state1+state0;
		File output_file = new File(outfile);
		FileWriter output_writer = new FileWriter(output_file);
		// write decrease
		for(String rule : spst0) {
			if(!spst1.contains(rule)) {
				String nr = "-";
				nr = nr + " " + rule;
				output_writer.write(nr+"\n");
				output_writer.write(ips0.get(rule)+"\n");
//				output_writer.write(aps0.get(rule)+"\n");
			}
			else {
				HashSet<String> diff = new HashSet<String>();
				for(String ip : ips0.get(rule)) {
					if(!ips1.get(rule).contains(ip)) {
						diff.add(ip);
					}
				}
				if(!diff.isEmpty()) {
					String nr = "-";
					nr = nr + " " + rule;
					output_writer.write(nr+"\n");
					output_writer.write(diff+"\n");
//					output_writer.write("prefix change\n");
				}
			}
		}
		// write increase
		for(String rule : spst1) {
			if(!spst0.contains(rule)) {
				String nr = "+";
				nr = nr + " " + rule;
				output_writer.write(nr+"\n");
				output_writer.write(ips1.get(rule)+"\n");
//				output_writer.write(aps1.get(rule)+"\n");
			}
			else {
				HashSet<String> diff = new HashSet<String>();
				for(String ip : ips1.get(rule)) {
					if(!ips0.get(rule).contains(ip)) {
						diff.add(ip);
					}
				}
				if(!diff.isEmpty()) {
					String nr = "+";
					nr = nr + " " + rule;
					output_writer.write(nr+"\n");
					output_writer.write(diff+"\n");
//					output_writer.write("prefix change\n");
				}
			}
		}
		output_writer.close();
	}
	public static void printReachabilityFile(String net,String state0,String state1) throws IOException {
		// TODO Auto-generated method stub
		HashSet<String> spst0 = new HashSet<String>();
		HashSet<String> spst1 = new HashSet<String>();
		
		String input = Parameters.dataset_path+"Campus/"+net+"/reachability/";
//		String input = Parameters.dataset_path+net+"/reachability/";
		
		String infile = input+net+state0;
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(infile));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + infile); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split("->");
			if(tokens.length != 2) continue;
			String h1 = tokens[0].substring(17);
			String h2 = tokens[1];
			spst0.add(h1+"->"+h2);
		}
		
		infile = input+net+state1;
		
		try {
			fileBufferReader = new BufferedReader(new FileReader(infile));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + infile); // for debugging
			System.exit (0); // Stop program if no file found
		}
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split("->");
			if(tokens.length != 2) continue;
			String h1 = tokens[0].substring(17);
			String h2 = tokens[1];
			spst1.add(h1+"->"+h2);
		}
		
		String outfile = input+"update"+state1+state0;
		File output_file = new File(outfile);
		FileWriter output_writer = new FileWriter(output_file);
		// write decrease
		for(String rule : spst0) {
			if(!spst1.contains(rule)) {
				String nr = "-";
				nr = nr + " " + rule;
				output_writer.write(nr+"\n");
			}
		}
		// write increase
		for(String rule : spst1) {
			if(!spst0.contains(rule)) {
				String nr = "+";
				nr = nr + " " + rule;
				output_writer.write(nr+"\n");
			}
		}
		output_writer.close();
	}
	private static void parseDCNReachabilityFile(String net, String snapshot, String state0, String state1) throws IOException {
		// TODO Auto-generated method stub
		String input = Parameters.dataset_path+net+"/reachability/";
		
		String infile = input+snapshot+state1+state0+".json";
		JSONTokener jt = new JSONTokener(new FileInputStream(new File(infile)));
		JSONObject json = new JSONObject(jt);
		
		String outfile = input+"huawei"+state1+state0;
		File output_file = new File(outfile);
		FileWriter output_writer = new FileWriter(output_file);
		for(Object detail : json.getJSONArray("detail")) {
			JSONObject content = (JSONObject) detail;
//			output_writer.write(content.getString("oldReachFlag")+"->"+content.getString("newReachFlag")+"\n");
			String old_flag = content.getString("oldReachFlag");
			String new_flag = content.getString("newReachFlag");
			if(old_flag.equals(new_flag)) continue;
			JSONObject bdx = content.getJSONObject("bdx");
			JSONObject bdy = content.getJSONObject("bdy");
			if(old_flag.equals("UNREACHABLE")) {
//				output_writer.write("+ "+bdx.getString("deviceId")+"_bd"+bdx.getInt("bdId")+"->"
//						+bdy.getString("deviceId")+"_bd"+bdy.getInt("bdId")+"\n");
				output_writer.write("+ "+bdx.getString("deviceId")+"_"+bdx.getString("epInfName").toLowerCase()+"->"
						+bdy.getString("deviceId")+"_"+bdy.getString("epInfName").toLowerCase()+"\n");
			}
			else {
//				output_writer.write("- "+bdx.getString("deviceId")+"_bd"+bdx.getInt("bdId")+"->"
//						+bdy.getString("deviceId")+"_bd"+bdy.getInt("bdId")+"\n");
				output_writer.write("- "+bdx.getString("deviceId")+"_"+bdx.getString("epInfName").toLowerCase()+"->"
						+bdy.getString("deviceId")+"_"+bdy.getString("epInfName").toLowerCase()+"\n");
			}
		}
		output_writer.close();
	}
	private static void getDifference(String file1, String file2, String out) throws IOException {
		// TODO Auto-generated method stub
		HashSet<String> spst0 = new HashSet<String>();
		HashSet<String> spst1 = new HashSet<String>();
		
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(file1));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + file1); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			if(linestr.endsWith(":")) {
				linestr = linestr.substring(0,linestr.length()-1);
			}
			spst0.add(linestr);
		}
		
		try {
			fileBufferReader = new BufferedReader(new FileReader(file2));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + file2); // for debugging
			System.exit (0); // Stop program if no file found
		}
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			if(linestr.endsWith(":")) {
				linestr = linestr.substring(0,linestr.length()-1);
			}
			spst1.add(linestr);
		}
		
		File output_file = new File(out);
		FileWriter output_writer = new FileWriter(output_file);
		for(String rule : spst0) {
			if(!spst1.contains(rule)) {
				String nr = "-";
				nr = nr + rule.substring(1);
				output_writer.write(nr+"\n");
			}
		}
		for(String rule : spst1) {
			if(!spst0.contains(rule)) {
				output_writer.write(rule+"\n");
			}
		}
		output_writer.close();
	}
	public static void checkDCN() throws IOException {
		String net = "DCN_xjtu";
		String[] scales = new String[] {""};
//		String[] scales = new String[] {"_20","_50","_100","_200"};
		String[] states = new String[] {"","_post0","_post1","_post2","_post3","_post4"};
		String input = Parameters.dataset_path+net+"/reachability/";
		
		String[] snapshots = new String[] {"svn-perform-test20-20-200-40","svn-perform-test50-50-500-100",
					"svn-perform-test100-100-1000-200","svn-perform-test200-200-2000-400"};
		
//		String state = "svn-perform-test20-20-200-40_post0_base.json";
//		String state = "svn-perform-test20-20-200-40_post1_post0.json";
//		String state = "svn-perform-test20-20-200-40_post2_post1.json";
//		String state = "svn-perform-test20-20-200-40_post3_post2.json";
//		String state = "svn-perform-test20-20-200-40_post4_post3.json";
		
//		String state0 = net+"_base";
//		String state0 = net+"_post0";
//		String state0 = net+"_post1";
//		String state0 = net+"_post2";
//		String state0 = net+"_post3";
//		String state1 = net+"_post4";
		
		/*
		 * build update file
		 */
//		for(int i=0;i<scales.length;i++) {
//			String scale = scales[i];
//			String snapshot = snapshots[i];
//			String name = net+scale;
//			for(int j=0;j<states.length-1;j++) {
//				String state0 = snapshot+states[j];
//				String state1 = snapshot+states[j+1];
//				String post = states[j+1];
//				printUpdateFile(name,state0,state1,post);
//			}
//		}
		/*
		 * build reachability diff
		 */
		for(int i=0;i<scales.length;i++) {
			String scale = scales[i];
			String name = net+scale;
			for(int j=0;j<states.length-1;j++) {
				String state0 = states[j];
				if(j==0) state0 = state0 + "_base";
				String state1 = states[j+1];
//				printReachabilityFileHuawei(name,state0,state1);
				printReachabilityFile(name,state0,state1);
			}
		}
		/*
		 *  parse huawei difference
		 */
//		for(int i=0;i<scales.length;i++) {
//			String scale = scales[i];
//			String snapshot = snapshots[i];
//			String name = net+scale;
//			for(int j=0;j<states.length-1;j++) {
//				String state0 = states[j];
//				if(j==0) state0 = state0 + "_base";
//				String state1 = states[j+1];
////				printReachabilityFile(name,state0,state1);
//				parseDCNReachabilityFile(name,snapshot,state0,state1);
//			}
//		}	
		/*
		 * build difference
		 */
		for(int j=0;j<states.length-1;j++) {
			String state0 = states[j];
			if(j==0) state0 = state0 + "_base";
			String state1 = states[j+1];
			String file1 = input+"huawei"+state1+state0;
			String file2 = input+"update"+state1+state0;
			String out = input+"difference"+state1+state0;
			getDifference(file1,file2,out);
		}
		
//		printReachabilityFile("huawei_blackhole","_base","_post0");
	}
	public static void checkCampus() throws IOException {
//		printUpdateFile("Campus/Campus_500","Campus_500_base","Campus_500_post0","_post0");
//		printUpdateFile("Campus/Campus_500","Campus_500_post0","Campus_500_post1","_post1");
		printReachabilityFile("Campus_500","_base","_post0");
		printReachabilityFile("Campus_500","_base","_post1");
		parseDCNReachabilityFile("Campus/Campus_500","test_per_campus_500","_post0","");
		parseDCNReachabilityFile("Campus/Campus_500","test_per_campus_500","_post1","");
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
//		String file1 = Parameters.log_path+"uscarrier_lf-1_post4";
//		String file2 = Parameters.log_path+"uscarrier_lf-1_reachability_cp";
//		String out = Parameters.log_path+"difference";
//		getDifference(file1,file2,out);
//		checkDCN();
		checkCampus();
	}
}
