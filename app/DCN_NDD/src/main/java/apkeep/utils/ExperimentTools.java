package apkeep.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import common.Fields;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

// import apkeep.elements.ForwardElement;
import common.PositionTuple;
import javafx.util.Pair;

class sortByNumPlus implements Comparator<Object> {
	@Override
    public int compare(Object o1, Object o2) {
		String s1 = (String) o1;
		String s2 = (String) o2;
    	String s_1 = s1.split("-")[1];
    	String s_2 = s2.split("-")[1];
//    	System.out.println(s_1 + " " + s_2);
//    	return 1;
    	int num1 = Integer.valueOf(s_1.split("\\(")[0]);
    	int num2 = Integer.valueOf(s_2.split("\\(")[0]);
    	return num1>num2 ? 1 : -1;
    }
}

public class ExperimentTools {

	static String[] bgp_types = new String[] {"lf-restore","lf","net-restore","net","bn-restore","bn","lp","mp","agg","sr","link","link-restore"};
	static String[] ospf_types = new String[] {"lf-restore","lf","lc","mp"};
	
	public ExperimentTools() {
		// TODO Auto-generated constructor stub
	}
	public static ArrayList<String> readFromFile(String inputFile) throws IOException{
		BufferedReader br = null;
		try {
//			System.out.println(inputFile);
			File file=new File(inputFile);

			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<String> contents = new ArrayList<String>();
		
		String OneLine;
		while((OneLine = br.readLine()) != null) {
			String linestr = OneLine.trim();
			contents.add(linestr);
		}
		
		return contents;
	}

	public static HashMap<String,HashMap<String,Double>> getAvgTime(String in_folder, String out_folder,String verify_type) throws IOException {
		File[] list = new File(in_folder).listFiles();
		HashMap<String,HashMap<String,Double>> prm_results = new HashMap<String,HashMap<String,Double>>();
		for(File in_file : list) {
//			if(!in_file.getName().startsWith("ospf") && !in_file.getName().startsWith("bgp")) continue;
			String file_name = in_file.getName();
			String type = file_name.split("_")[2];
			String network = file_name.split("_")[0]+"_"+file_name.split("_")[1];
			boolean isdouble = false;
			if(file_name.endsWith("_lf_normal") || file_name.endsWith("_bn_normal")) isdouble = true;
			if(file_name.endsWith("_lf_nomerge") || file_name.endsWith("_bn_nomerge") || file_name.endsWith("net_nomerge")) isdouble = true;
			if(file_name.endsWith("_link_nomerge")) isdouble = true;
			BufferedReader fileBufferReader = null;
			try {
				fileBufferReader = new BufferedReader(new FileReader(in_folder+file_name));
			} catch (FileNotFoundException e) {
				System.err.println ("File not found! " + in_folder+file_name); // for debugging
				System.exit (0); // Stop program if no file found
			}
			String OneLine;
			long ppm_sum = 0;
			long prm_sum = 0;
			int line = 0;
			int change = 0;
			ArrayList<String> results = new ArrayList<String>();
			File output_file = new File(out_folder+file_name);
			FileWriter output_writer = new FileWriter(output_file);
			while((OneLine = fileBufferReader.readLine()) != null)
			{
				line ++;
				String linestr = OneLine.trim();
				String[] tokens = linestr.split(" ");
				long ppm = Long.parseLong(tokens[8]);
				long prm = Long.parseLong(tokens[9]);
				if(isdouble) {
					if(line%2 == 0) {
						results.add(ppm+" "+prm);
						continue;
					}
				}
				output_writer.write(ppm*1.0/1000+"\t"+prm*1.0/1000+"\n");
				ppm_sum += ppm;
				prm_sum += prm;
				change++;
			}
			output_writer.close();
//			System.out.println(file_name + " "+ppm_sum*1.0/change/1000+" "+prm_sum*1.0/change/1000);
//			System.out.println(file_name +" "+prm_sum*1.0/change/1000);
			HashMap<String,Double> prm_result = prm_results.get(type);
			if(prm_result == null) {
				prm_result = new HashMap<String,Double>();
				prm_results.put(type, prm_result);
			}
			if(verify_type.equals("ppm")) {
				prm_result.put(network, ppm_sum*1.0/change/1000);
			}
			else {
				prm_result.put(network, prm_sum*1.0/change/1000);
			}
			if(!isdouble) continue;
			ppm_sum = 0;
			prm_sum = 0;
			change = 0;
			file_name = file_name.substring(0,file_name.length()-8) + "-restore_normal";
			type = file_name.split("_")[2];
			output_file = new File(out_folder+file_name);
			output_writer = new FileWriter(output_file);
			for(int i = 0;i<results.size();i++) {
				long ppm = Long.parseLong(results.get(i).split(" ")[0]);
				long prm = Long.parseLong(results.get(i).split(" ")[1]);
				output_writer.write(ppm+" "+prm+"\n");
				ppm_sum += ppm;
				prm_sum += prm;
				change++;
			}
			output_writer.close();
//			System.out.println(file_name + " "+ppm_sum*1.0/change/1000+" "+prm_sum*1.0/change/1000);
//			System.out.println(file_name +" "+prm_sum*1.0/change/1000);
			prm_result = prm_results.get(type);
			if(prm_result == null) {
				prm_result = new HashMap<String,Double>();
				prm_results.put(type, prm_result);
			}
			if(verify_type.equals("ppm")) {
				prm_result.put(network, ppm_sum*1.0/change/1000);
			}
			else {
				prm_result.put(network, prm_sum*1.0/change/1000);
			}
		}
		for(int i = 0; i<bgp_types.length;i++) {
			if(!prm_results.containsKey(bgp_types[i])) continue;
			System.out.println("\""+(i+1)+"\"\t"+String.format("%.2f", prm_results.get(bgp_types[i]).get("bgp_fattree12"))+
					"\t"+String.format("%.2f", prm_results.get(bgp_types[i]).get("bgp_fattree16"))+
					"\t"+String.format("%.2f", prm_results.get(bgp_types[i]).get("bgp_fattree20")));
		}
		for(int i = 0; i<ospf_types.length;i++) {
			if(!prm_results.containsKey(ospf_types[i])) continue;
			System.out.println("\""+(i+10)+"\"\t"+String.format("%.2f", prm_results.get(ospf_types[i]).get("ospf_fattree12"))+
					"\t"+String.format("%.2f", prm_results.get(ospf_types[i]).get("ospf_fattree16"))+
					"\t"+String.format("%.2f", prm_results.get(ospf_types[i]).get("ospf_fattree20")));
		}
		return prm_results;
	}

	private static void getFattreeResults(String in_folder, String out_folder, String type) throws IOException {
		// TODO Auto-generated method stub
		HashMap<String,HashMap<String,Double>> dna_results = getAvgTime(in_folder+"-dna/",out_folder,type);
		HashMap<String,HashMap<String,Double>> naive_inc_results = getAvgTime(in_folder+"-naive-inc/",out_folder,type);
//		HashMap<String,HashMap<String,Double>> naive_results = getAvgTime(in_folder+"-naive/",out_folder,type);
		HashMap<String,HashMap<String,Double>> apkeep_results = getAvgTime(in_folder+"-apkeep/",out_folder,type);
		// print update time
		if(type.equals("ppm")) {
			System.out.println("# update time");
			for(int i = 0; i<bgp_types.length;i++) {
				if(!apkeep_results.containsKey(bgp_types[i])) continue;
				System.out.println("\""+(i+1)+"\""+
						"\t"+String.format("%.2f", apkeep_results.get(bgp_types[i]).get("bgp_fattree12"))+
						"\t"+String.format("%.2f", dna_results.get(bgp_types[i]).get("bgp_fattree12"))+
						"\t"+String.format("%.2f", apkeep_results.get(bgp_types[i]).get("bgp_fattree16"))+
						"\t"+String.format("%.2f", dna_results.get(bgp_types[i]).get("bgp_fattree16"))+
						"\t"+String.format("%.2f", apkeep_results.get(bgp_types[i]).get("bgp_fattree20"))+
						"\t"+String.format("%.2f", dna_results.get(bgp_types[i]).get("bgp_fattree20")));
			}
		}
		// print verify time
		else {
			System.out.println("# verify time");
			for(int i = 0; i<bgp_types.length;i++) {
				if(!naive_inc_results.containsKey(bgp_types[i])) continue;
				System.out.println("\""+(i+1)+"\""+
//						"\t"+String.format("%.2f", naive_results.get(bgp_types[i]).get("bgp_fattree12"))+
						"\t"+String.format("%.2f", naive_inc_results.get(bgp_types[i]).get("bgp_fattree12"))+
						"\t"+String.format("%.2f", dna_results.get(bgp_types[i]).get("bgp_fattree12"))+
//						"\t"+String.format("%.2f", naive_results.get(bgp_types[i]).get("bgp_fattree16"))+
						"\t"+String.format("%.2f", naive_inc_results.get(bgp_types[i]).get("bgp_fattree16"))+
						"\t"+String.format("%.2f", dna_results.get(bgp_types[i]).get("bgp_fattree16"))+
//						"\t"+String.format("%.2f", naive_results.get(bgp_types[i]).get("bgp_fattree20"))+
						"\t"+String.format("%.2f", naive_inc_results.get(bgp_types[i]).get("bgp_fattree20"))+
						"\t"+String.format("%.2f", dna_results.get(bgp_types[i]).get("bgp_fattree20")));
			}
		}
	}
	public static void getCampusAvgTime(String in_folder, String out_folder) throws IOException {
		File[] list = new File(in_folder).listFiles();
		for(File in_file : list) {
//			if(!in_file.getName().startsWith("ospf") && !in_file.getName().startsWith("bgp")) continue;
			String file_name = in_file.getName();
			String network = file_name.split("_")[0]+"_"+file_name.split("_")[1];
			boolean isdouble = false;
			if(file_name.endsWith("_lf_normal") || file_name.endsWith("_bn_normal")) isdouble = true;
			BufferedReader fileBufferReader = null;
			try {
				fileBufferReader = new BufferedReader(new FileReader(in_folder+file_name));
			} catch (FileNotFoundException e) {
				System.err.println ("File not found! " + in_folder+file_name); // for debugging
				System.exit (0); // Stop program if no file found
			}
			String OneLine;
			long ppm_sum = 0;
			long prm_sum = 0;
			int line = 0;
			int change = 0;
			ArrayList<String> results = new ArrayList<String>();
			File output_file = new File(out_folder+file_name);
			FileWriter output_writer = new FileWriter(output_file);
			while((OneLine = fileBufferReader.readLine()) != null)
			{
				line ++;
				String linestr = OneLine.trim();
				String[] tokens = linestr.split(" ");
				long ppm = Long.parseLong(tokens[8]);
				long prm = Long.parseLong(tokens[9]);
				if(isdouble) {
					if(line%2 == 0) {
						results.add(ppm+" "+prm);
						continue;
					}
				}
				output_writer.write(ppm*1.0/1000+"\t"+prm*1.0/1000+"\n");
				ppm_sum += ppm;
				prm_sum += prm;
				change++;
			}
			output_writer.close();
			System.out.println(file_name + " "+ppm_sum*1.0/change/1000+" "+prm_sum*1.0/change/1000);
//			System.out.println(file_name +" "+prm_sum*1.0/change/1000);
			if(!isdouble) continue;
			ppm_sum = 0;
			prm_sum = 0;
			change = 0;
			file_name = file_name.substring(0,file_name.length()-7) + "-restore_normal";
			output_file = new File(out_folder+file_name);
			output_writer = new FileWriter(output_file);
			for(int i = 0;i<results.size();i++) {
				long ppm = Long.parseLong(results.get(i).split(" ")[0]);
				long prm = Long.parseLong(results.get(i).split(" ")[1]);
				output_writer.write(ppm+" "+prm+"\n");
				ppm_sum += ppm;
				prm_sum += prm;
				change++;
			}
			output_writer.close();
			System.out.println(file_name + " "+ppm_sum*1.0/change/1000+" "+prm_sum*1.0/change/1000);
//			System.out.println(file_name +" "+prm_sum*1.0/change/1000);
		}
	}
	public static void parseEdgePorts() throws IOException {
		String [] nets = new String[] {"fattree12","fattree04","fattree08"};
		String [] protos = new String[] {"bgp","ospf"};
//		String [] nets = new String[] {"fattree20"};
//		String [] protos = new String[] {"ospf"};
		for(String net : nets) {
			for(String proto : protos) {
				String net_name  = proto + "_" + net;
				String root_folder = Parameters.dataset_path+net_name+"/";
				
				File[] change_folders = new File(root_folder).listFiles();
				
				ArrayList<String> folder_names = new ArrayList<String>();
				for(File change_folder : change_folders){
					if(!change_folder.isDirectory()) continue;
					folder_names.add(change_folder.getName());
				}
				HashSet<PositionTuple> edge_ports = new HashSet<PositionTuple>();
				for(String change_name : folder_names) {
					String update_folder = root_folder + change_name + "/";
					File[] update_files = new File(update_folder).listFiles();
					for(File update_file : update_files) {
						if(update_file.isDirectory()) continue;
						if(update_file.getName().startsWith("change_restore")) continue;

						BufferedReader fileBufferReader = null;
						try {
							fileBufferReader = new BufferedReader(new FileReader(update_folder+update_file.getName()));
						} catch (FileNotFoundException e) {
							System.err.println ("File not found! " + update_folder+update_file.getName()); // for debugging
							System.exit (0); // Stop program if no file found
						}
						String OneLine;
						while((OneLine = fileBufferReader.readLine()) != null)
						{
							String linestr = OneLine.trim();
							String[] tokens = linestr.split(" ");
							edge_ports.add(new PositionTuple(tokens[2],tokens[5]));
						}
					}
				}
				HashSet<PositionTuple> net_ports = new HashSet<PositionTuple>();
				String topo_file = "";
				for(File change_folder : change_folders){
					if(change_folder.isDirectory()) continue;
					if(change_folder.getName().endsWith("topo.txt")) {
						topo_file = change_folder.getName();
						break;
					}
				}
				BufferedReader fileBufferReader = null;
				try {
					fileBufferReader = new BufferedReader(new FileReader(root_folder+topo_file));
				} catch (FileNotFoundException e) {
					System.err.println ("File not found! " + root_folder+topo_file); // for debugging
					System.exit (0); // Stop program if no file found
				}
				String OneLine;
				while((OneLine = fileBufferReader.readLine()) != null)
				{
					String linestr = OneLine.trim();
					String[] tokens = linestr.split(" ");
					net_ports.add(new PositionTuple(tokens[0],tokens[1]));
					net_ports.add(new PositionTuple(tokens[2],tokens[3]));
				}
				
				edge_ports.removeAll(net_ports);
				
				String outfile = root_folder+"edge_ports";
				File output_file = new File(outfile);
				FileWriter output_writer = new FileWriter(output_file);
				for(PositionTuple pt : edge_ports) {
					output_writer.write(pt.getDeviceName() + " " + pt.getPortName()+"\n");
				}
				output_writer.close();
				outfile = root_folder+"end_hosts";
				output_file = new File(outfile);
				output_writer = new FileWriter(output_file);
				HashSet<String> edge_host = new HashSet<String>();
				for(PositionTuple pt : edge_ports) {
					output_writer.write(pt.getDeviceName() + "," + pt.getPortName()+"\n");
					if(edge_host.contains(pt.getDeviceName())) continue;
					edge_host.add(pt.getDeviceName());
					output_writer.write(pt.getDeviceName() + "\n");
//					output_writer.write(pt.getDeviceName() + "_" + pt.getPortName()+"_start\n");
				}
				output_writer.close();
			}
		}
		
	}
	public static void constructEndHost() throws IOException {
		// TODO Auto-generated method stub
//		String [] nets = new String[] {"bics","columbus","uscarrier"};
//		String [] protos = new String[] {""};
//		String [] nets = new String[] {"fattree04","fattree08","fattree12"};
//		String [] protos = new String[] {"bgp","ospf"};
//		String [] nets = new String[] {"fattree12","fattree16","fattree20"};
//		String [] protos = new String[] {"bgp"};
		String [] nets = new String[] {"blackhole_new"};
		String [] protos = new String[] {"huawei"};
		for(String net : nets) {
			for(String proto : protos) {
				String net_name  = proto + "_" + net;
				if(proto.equals("")) net_name = net;
//				String net_name  = "campus";
				String root_folder = Parameters.dataset_path+net_name+"/";
				
				String in_file = root_folder+"edgePorts"; 
				BufferedReader csvReader = new BufferedReader(new FileReader(in_file));
				String row = "";
				String out_file = root_folder+"end_hosts";
				File output_file = new File(out_file);
				FileWriter output_writer = new FileWriter(output_file);
				
				while ((row = csvReader.readLine()) != null) {
				    String[] data = row.split(" ");
				    for(int i=0;i<data.length;i++) {
				    	data[i] = data[i].trim();
				    }
				    output_writer.write(data[0]+"\n");
				    
				    output_writer.write(data[0]);
				    for(int i = 1; i < data.length ; i++) {
					    output_writer.write(","+data[i]);
				    }
				    output_writer.write("\n");
				}
				csvReader.close();
				output_writer.close();
			}
		}
	}
	public static void simplifybase() throws IOException {
		String infile = "F:/dataset/ospf_fattree12/ospf_fattree12_lf-2/change_base";
		String outfile = "F:/dataset/ospf_fattree12/ospf_fattree12_lf/change_base";
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(infile));
		} catch (FileNotFoundException e) {
			System.err.println ("File not found! " + infile); // for debugging
			System.exit (0); // Stop program if no file found
		}
		File output_file = new File(outfile);
		FileWriter output_writer = new FileWriter(output_file);
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null){
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			if(tokens[3].equals("504038416")) {
				output_writer.write(linestr+"\n");
			}
		}
		output_writer.close();
	}
	public static void dataStatistics() throws IOException {
		LOG.ClearLogFile();
//		String [] nets = new String[] {"fattree12","fattree16","fattree20"};
//		String [] protos = new String[] {"bgp","ospf"};
		String [] nets = new String[] {""};
		String [] protos = new String[] {""};
		for(String net : nets) {
			for(String proto : protos) {
				String net_name  = proto + "_" + net;
				if(net_name.equals("_")) {
					net_name  = "campus";
				}
				String root_folder = Parameters.dataset_path+net_name+"/";
				
				File[] change_folders = new File(root_folder).listFiles();
				
				ArrayList<String> folder_names = new ArrayList<String>();
				if(!net_name.equals("campus")) {
					for(File change_folder : change_folders){
						if(!change_folder.isDirectory()) continue;
						folder_names.add(change_folder.getName());
					}
				}
				else {
					folder_names.add("parsed");
				}
				for(String change_name : folder_names) {
					double class1 = 0;
					double class2 = 0;
					double class3 = 0;
					int change_num = 0;
					String update_folder = root_folder + change_name + "/";
					File[] update_files = new File(update_folder).listFiles();
					for(File update_file : update_files) {
						if(update_file.isDirectory()) continue;
						if(update_file.getName().startsWith("change_restore")) continue;
						if(update_file.getName().startsWith("change_base")) continue;
						change_num++;
						int total_update = 0;
						int port_change = 0;
						int same_move = 0;
						int same_prefix = 0;
						HashMap<String, HashMap<String, Pair<HashSet<String>,HashSet<String>>>>
								fw_rules = new HashMap<String, HashMap<String, Pair<HashSet<String>,HashSet<String>>>>();

						BufferedReader fileBufferReader = null;
						try {
							fileBufferReader = new BufferedReader(new FileReader(update_folder+update_file.getName()));
						} catch (FileNotFoundException e) {
							System.err.println ("File not found! " + update_folder+update_file.getName()); // for debugging
							System.exit (0); // Stop program if no file found
						}
						String OneLine;
						while((OneLine = fileBufferReader.readLine()) != null)
						{
							String token = " ";
							if(net_name.equals("campus")) token = ",";
							total_update++;
							String linestr = OneLine.trim();
							String[] tokens = linestr.split(token);
							
							if(!tokens[1].equals("fwd")) continue;
							String op = tokens[0];
							String element_name = tokens[2];
							String ipInt = tokens[3];
							String prio = tokens[4];
							String outport = tokens[5];
							
							String ip = ipInt + "/" + prio;
							HashMap<String, Pair<HashSet<String>,HashSet<String>>> rules = fw_rules.get(ip);
							if(rules == null) {
								rules = new HashMap<String, Pair<HashSet<String>,HashSet<String>>>();
							}
							Pair<HashSet<String>,HashSet<String>> actions = rules.get(element_name);
							if(actions == null) {
								actions = new Pair<HashSet<String>,HashSet<String>>(new HashSet<String>(),new HashSet<String>());
							}
							if(op.equals("-")) actions.getKey().add(outport);
							else actions.getValue().add(outport);
							rules.put(element_name, actions);
							fw_rules.put(ip, rules);
						}
						HashMap<Pair<HashSet<PositionTuple>, HashSet<PositionTuple>>, HashSet<String>> move_aps
							= new HashMap<Pair<HashSet<PositionTuple>, HashSet<PositionTuple>>, HashSet<String>>();
						for(String ipprefix : fw_rules.keySet()) {
							same_prefix = same_prefix + fw_rules.get(ipprefix).keySet().size();
							for(String element : fw_rules.get(ipprefix).keySet()) {
								if(!fw_rules.get(ipprefix).get(element).getKey().isEmpty() 
										&& !fw_rules.get(ipprefix).get(element).getValue().isEmpty()) {
									port_change = port_change + fw_rules.get(ipprefix).get(element).getKey().size();
									port_change = port_change + fw_rules.get(ipprefix).get(element).getValue().size();
								}
								HashSet<PositionTuple> remove_tuples = new HashSet<PositionTuple>();
								HashSet<PositionTuple> add_tuples = new HashSet<PositionTuple>();
								for(String port : fw_rules.get(ipprefix).get(element).getKey()) {
									remove_tuples.add(new PositionTuple(element, port));
								}
								for(String port : fw_rules.get(ipprefix).get(element).getValue()) {
									add_tuples.add(new PositionTuple(element, port));
								}
								Pair<HashSet<PositionTuple>, HashSet<PositionTuple>> pair 
									= new Pair<HashSet<PositionTuple>, HashSet<PositionTuple>>(remove_tuples,add_tuples);
								if(move_aps.containsKey(pair)) {
									move_aps.get(pair).add(ipprefix);
								}
								else {
									HashSet<String> aps = new HashSet<String>();
									aps.add(ipprefix);
									move_aps.put(pair, aps);
								}
							}
						}
						for(Pair<HashSet<PositionTuple>, HashSet<PositionTuple>> pair : move_aps.keySet()) {
							if(move_aps.get(pair).size()>1)
							same_move = same_move + (pair.getKey().size() + pair.getValue().size())*move_aps.get(pair).size();
						}
//						LOG.LOGFILE(total_update +" "+ port_change +" "+same_move +" "+same_prefix);
						if(total_update!=0) {
							class1 += port_change*100.0/total_update;
							class2 += same_move*100.0/total_update;
							class3 += same_prefix*100.0/total_update;
						}
						else {
							change_num --;
						}
					}
					LOG.LOGFILE("| "+change_name+" | "+class1/change_num +"% | "+ class2/change_num +"% | "+class3/change_num+"% |");
				}
			}
		}
	}
	public static void parseUpdateFile() throws IOException {
		String net_name = "campus";
//		String update_folder = Parameters.dataset_path+net_name+"/"+net_name+"/";
//		String rule_folder = Parameters.dataset_path+net_name+"/rule/";
		String update_folder = Parameters.dataset_path+net_name+"/vlan/";
		String rule_folder = Parameters.dataset_path+net_name+"/vlanRule/";
		
		File[] file_list = new File(rule_folder).listFiles();
		int total_updates = 0;
		String last_name = "base";
		while(total_updates < file_list.length) {
			for(File file : file_list) {
				if(file.getName().startsWith(last_name)) {
//					break;
					String file_name = "";
					if(total_updates == 0) file_name = "change_base";
					else file_name = "change_update-"+total_updates;
					
					File update_file = new File(update_folder+file_name);
					FileWriter output_writer = new FileWriter(update_file);
					
					BufferedReader fileBufferReader = null;
					try {
						fileBufferReader = new BufferedReader(new FileReader(rule_folder+file.getName()));
					} catch (FileNotFoundException e) {
						System.err.println ("File not found! " + rule_folder+file.getName()); // for debugging
						System.exit (0); // Stop program if no file found
					}
					String OneLine;
					while((OneLine = fileBufferReader.readLine()) != null)
					{
						String linestr = OneLine.trim();
						output_writer.write(linestr+"\n");
					}
					output_writer.close();
					total_updates++;
					last_name = file.getName().split("_")[1];
				}
			}
		}
	}
	private static void parseBatfishResults(String in_folder, String out_folder) throws IOException {
		// TODO Auto-generated method stub
		HashMap<String,HashMap<String,HashSet<String>>> reachMatrix = new HashMap<String,HashMap<String,HashSet<String>>>();
		File[] list = new File(in_folder).listFiles();
		for(File in_file : list) {
			HashMap<String,HashMap<String,HashSet<String>>> new_reachMatrix = new HashMap<String,HashMap<String,HashSet<String>>>();
			String file_name = in_file.getName();
			BufferedReader fileBufferReader = null;
			try {
				fileBufferReader = new BufferedReader(new FileReader(in_folder+file_name));
			} catch (FileNotFoundException e) {
				System.err.println ("File not found! " + in_folder+file_name); // for debugging
				System.exit (0); // Stop program if no file found
			}
			String OneLine;
			File output_file = new File(out_folder+file_name);
			FileWriter output_writer = new FileWriter(output_file);
			while((OneLine = fileBufferReader.readLine()) != null)
			{
				String linestr = OneLine.trim();
				String[] tokens = linestr.split(",");
				String flag = tokens[3];
				String IPs = tokens[2];
				String dstNode = tokens[1];
				
			}
			output_writer.close();
		}
	}
	public static void compareFile(String file1, String file2, String target) throws IOException {
//		System.out.println("Comparing "+file1+", "+file2+" -> "+target);
		ArrayList<String> content1 = readFromFile(file1);
		ArrayList<String> content2 = readFromFile(file2);
		
		FileWriter output_writer = new FileWriter(new File(target));
		for(String rule : content1) {
			if(!content2.contains(rule)) {
				String nr = "-";
				if(rule.startsWith("+")) nr = nr + rule.substring(1);
				else nr = nr + " " + rule;
				output_writer.write(nr+"\n");
			}
		}
		for(String rule : content2) {
			if(!content1.contains(rule)) {
				String nr = "+";
				if(rule.startsWith("+")) nr = rule;
				else nr = nr + " " + rule;
				output_writer.write(rule+"\n");
			}
		}
		output_writer.close();
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
//		String path = "C:/Users/reason/Desktop/results-last";
//		getFattreeResults(path,"C:/Users/reason/Desktop/avg/","ppm");
//		getFattreeResults(path,"C:/Users/reason/Desktop/avg/","prm");
		getAvgTime("C:/Users/reason/Desktop/results-last-naive/","C:/Users/reason/Desktop/avg/","prm");
//		getCampusAvgTime("C:/Users/reason/Desktop/results-campus/","C:/Users/reason/Desktop/data-campus/");
//		parseEdgePorts();
//		constructEndHost();
//		simplifybase();
//		dataStatistics();
//		parseUpdateFile();
//		parseVlan();
//		parseBatfishResults("C:/Users/reason/Desktop/campus_batfish/","C:/Users/reason/Desktop/diffReach/");
	}
}
