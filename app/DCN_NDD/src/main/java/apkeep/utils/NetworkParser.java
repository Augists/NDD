package apkeep.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import common.ACLRule;
import common.ACLUse;
import common.ForwardingRule;
import common.PositionTuple;

public class NetworkParser {

	String network_name;
		
	String fw_path;
	String json_path;
	String config_path;
	String topo_file_name;
	String output_path;
	String[] device_names;
	HashSet<String> acl_device_names;
	HashMap<String, DeviceParser> devices;
	HashMap<PositionTuple, HashSet<PositionTuple>> topology;
	
	int acl_rule_number;
	int fw_rule_number;
	int link_number;
	int port_number;
	int node_number;
	int update_number;
	int acl_number;
	
	public NetworkParser(String name) throws IOException {
		// TODO Auto-generated constructor stub
		network_name = name;
		fw_path = Parameters.dataset_path + network_name + "/fw/";
		json_path = Parameters.dataset_path + network_name + "/json/";
		config_path = Parameters.dataset_path + network_name + "/config/";
		topo_file_name = Parameters.dataset_path + network_name + "/fw/topo.txt";
		output_path = Parameters.dataset_path + network_name + "/parsed/";
		topology = new HashMap<PositionTuple, HashSet<PositionTuple>>();
		acl_device_names = new HashSet<String>();
		
		acl_rule_number = 0;
		fw_rule_number = 0;
		link_number = 0;
		port_number = 0;
		node_number = 0;
		update_number = 0;
		acl_number = 0;
	}
	
	public static void main (String[] args) throws IOException
	{
//		NetworkParser parser = new NetworkParser("purdue-noacl");
//		NetworkParser parser = new NetworkParser("purdue");
//		NetworkParser parser = new NetworkParser("stanford-noacl");
		NetworkParser parser = new NetworkParser("stanford");
//		NetworkParser parser = new NetworkParser("internet2");
		
		parser.ConstructTopology();
		parser.LoadDeviceNames();
		parser.CreateFWDevices();
//		parser.CreateFWDevicesWithOrigin();
		if (parser.network_name.equals("stanford") || parser.network_name.equals("purdue"))
			parser.CreateACLDevices();
//		parser.PrintUpdateACLFirst();
//		parser.PrintUpdateRoundRobin();
//		parser.PrintUpdateRoundRobinReverse();
//		parser.PrintUpdateRoundRobinRandom();
		parser.PrintUpdateRoundRobinACLFirst();
//		parser.PrintUpdateDeviceByDevice();
		//parser.PrintUpdateDeviceByDeviceACLFirst();
//		parser.PrintTopology();
		if (parser.network_name.startsWith("stanford")) {
			parser.DumpVLAN ();
		}
		parser.DumpPorts();
	}
	
	public void LoadDeviceNames()
	{
		if (network_name.startsWith("stanford")) {
			String[] stanford_names = {"bbra_rtr", "bbrb_rtr", "boza_rtr", "bozb_rtr", "coza_rtr", "cozb_rtr", "goza_rtr",
					"gozb_rtr", "poza_rtr", "pozb_rtr", "roza_rtr", "rozb_rtr", "soza_rtr", "sozb_rtr", "yoza_rtr", "yozb_rtr"};
			device_names = stanford_names;
		}
		else if (network_name.equals("internet2")) {
			String[] internet2_names = {"atla","chic","hous","kans","losa","newy32aoa","salt","seat","wash"};
			device_names = internet2_names;
		}
		else if (network_name.startsWith("purdue")) {
			// get device names
			File[] list_configs = new File(config_path).listFiles();
			device_names = new String [list_configs.length];
			for(int i = 0; i < device_names.length; i ++){
				device_names[i] = list_configs[i].getName();
			}
		}
		
		System.out.println("Number of devices: " + device_names.length);
	}
	
	
	public void CreateFWDevices()  throws IOException
	{
		devices = new HashMap<String, DeviceParser>();
		
		// create devices
		for(int i = 0; i < device_names.length; i ++) {
			//System.out.println(device_names[i]);
			DeviceParser d = null;
			if (network_name.startsWith("stanford")) {			
				d  = new DeviceParser(device_names[i], "fwd", config_path + device_names[i] + "_config.txt", 
					fw_path + device_names[i] + "ap", output_path);
			}
			if (network_name.equals("internet2")) {			
				d  = new DeviceParser(device_names[i], "fwd", 
					fw_path + device_names[i] + "apnotcomp", output_path);
			}
			
			else if (network_name.startsWith("purdue")) {			
				d  = new DeviceParser(device_names[i], "fwd", config_path + device_names[i], 
					fw_path + device_names[i], output_path);
			}
			devices.put(d.GetDeviceName(), d);
			if (!network_name.equals("internet2")){
				d.readConfig();
			}
			d.readParsedFW();
			fw_rule_number += d.fw_rules.size();
		}
		System.out.println("Number of forwarding rules: " + fw_rule_number);
	}
	public void CreateFWDevicesWithOrigin()  throws IOException
	{
		devices = new HashMap<String, DeviceParser>();
		
		// create devices
		for(int i = 0; i < device_names.length; i ++) {
			//System.out.println(device_names[i]);
			DeviceParser d = null;
			if (network_name.startsWith("stanford")) {			
				d  = new DeviceParser(device_names[i], "fwd", config_path + device_names[i] + "_config.txt", 
					fw_path + device_names[i] + "ap",json_path + device_names[i] + "_route.txt", output_path);
			}
			if (network_name.equals("internet2")) {			
				d  = new DeviceParser(device_names[i], "fwd", 
					fw_path + device_names[i] + "apnotcomp", output_path);
			}
			
			else if (network_name.startsWith("purdue")) {			
				d  = new DeviceParser(device_names[i], "fwd", config_path + device_names[i], 
					fw_path + device_names[i], output_path);
			}
			devices.put(d.GetDeviceName(), d);
			if (!network_name.equals("internet2")){
				d.readConfig();
			}
//			d.readParsedFW();
			d.readParsedFWOrigin();
			fw_rule_number += d.fw_rules.size();
		}
		System.out.println("Number of forwarding rules: " + fw_rule_number);
	}
	public void CreateACLDevices() throws IOException
	{
		HashMap<String, DeviceParser> acl_devices = new HashMap<String, DeviceParser>();

		for (DeviceParser fw_device : devices.values()) {
			//System.out.println("Add acl devices for " + d.getDeviceName());
			for(ACLUse oneacluse : fw_device.acluses) {
				String aclname = oneacluse.getnumber();
				LinkedList<ACLRule> aclrules = fw_device.getACLRules(aclname);
				
				// cannot find the acl
				if(aclrules == null) {
					System.err.println("Cannot find the acl " + aclname + " for " + fw_device.GetDeviceName());
					continue;
				}
				
				// create ACL devices (one for each acl)
				if (fw_device.addEffectiveACL(oneacluse.getnumber())) {
					String acl_device_name = fw_device.GetDeviceName() + "_" + aclname;
					DeviceParser new_acl_device = new DeviceParser(acl_device_name, "acl");
					//System.out.println("Creating ACL Device " + acl_device_name);
					new_acl_device.addACLRules(aclrules);
					acl_devices.put(new_acl_device.GetDeviceName(), new_acl_device);
					acl_rule_number += aclrules.size();
					acl_number ++;
				}
				
				// create ACL nodes (one for each port)
				HashSet<String> ports;
				if(fw_device.hasVlanPort(oneacluse.getinterface()))
				{
					ports = fw_device.vlanToPhy(oneacluse.getinterface());
				}else
				{
					ports = new HashSet<String> ();
					ports.add(oneacluse.getinterface());
				}
				
				for(String pport : ports){
					//System.out.println(fw_device.getDeviceName() + " " + pport + " " + oneacluse.getdirection() +  " " + aclname);
					AttachACLNodeInTopology(fw_device.GetDeviceName(), pport, oneacluse.getdirection(), aclname);
				}
			}
		}
		
		System.out.println("Number of ACLs: " + acl_number);
		System.out.println("Number of ACL rules: " + acl_rule_number);
		System.out.println("Number of ACL nodes: " + acl_device_names.size());
		System.out.println("Number of FW+ACL nodes: " + (devices.size() + acl_device_names.size()));
		
		devices.putAll(acl_devices);
	}
	
	public void PrintUpdateACLFirst() throws IOException
	{
		File updatefile = new File(output_path + "updates");
		FileWriter updatewriter = new FileWriter(updatefile);
		
		
		HashMap<String, Integer> indice = new HashMap<String, Integer>();
		
		for(String dname : devices.keySet()) {
			indice.put(dname, 0);
		}
		while(true){
			boolean HasRule = false;
			for(DeviceParser d : devices.values()) {
				int index = indice.get(d.GetDeviceName());
				if (d.device_type.equals("acl")) {
					if (index >= d.acl_rules.size())
						continue;
					ACLRule r = d.acl_rules.get(index);
					updatewriter.write("+ " +d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
					indice.put(d.GetDeviceName(), index+1);
					update_number ++;
					HasRule = true;
				}
			}
			if (!HasRule) {
				break;
			}
		}
		
		for(String dname : devices.keySet()) {
			indice.put(dname, 0);
		}
		while(true){
			boolean HasRule = false;
			for(DeviceParser d : devices.values()) {
				int index = indice.get(d.GetDeviceName());
				if (d.getDeviceType().equals("fwd")) {
					if (index >= d.fw_rules.size())
						continue;
					ForwardingRule r = d.fw_rules.get(index);
					updatewriter.write("+ " + d.getDeviceType() + " " + d.GetDeviceName() + " " + r +"\n");
					indice.put(d.GetDeviceName(), index+1);
					update_number ++;
					HasRule = true;
				}
			}
			if (!HasRule) {
				break;
			}
		}
		

		for(String dname : devices.keySet()) {
			indice.put(dname, 0);
		}
		while(true){
			boolean HasRule = false;
			for(DeviceParser d : devices.values()) {
				int index = indice.get(d.GetDeviceName());
				if (d.getDeviceType().equals("fwd")) {
					if (index >= d.fw_rules.size())
						continue;
					ForwardingRule r = d.fw_rules.get(index);
					updatewriter.write("- " + d.getDeviceType() + " " + d.GetDeviceName() + " " + r +"\n");
					indice.put(d.GetDeviceName(), index+1);
					update_number ++;
					HasRule = true;
				}
			}
			if (!HasRule) {
				break;
			}
		}
		
		
		for(String dname : devices.keySet()) {
			indice.put(dname, 0);
		}
		while(true){
			boolean HasRule = false;
			for(DeviceParser d : devices.values()) {
				int index = indice.get(d.GetDeviceName());
				if (d.getDeviceType().equals("acl")) {
					if (index >= d.acl_rules.size())
						continue;
					ACLRule r = d.acl_rules.get(index);
					updatewriter.write("- " + d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
					indice.put(d.GetDeviceName(), index+1);
					update_number ++;
					HasRule = true;
				}
			}
			if (!HasRule) {
				break;
			}
		}
		

		updatewriter.close();
		
		System.out.println("Number of updates: " + update_number);
	}
	
	public void PrintUpdateRoundRobinACLFirst() throws IOException
	{
		File updatefile = new File(output_path + "updates");
		FileWriter updatewriter = new FileWriter(updatefile);
		
		
		HashMap<String, Integer> indice = new HashMap<String, Integer>();
		ArrayList<String> updates = new ArrayList<String>();

		for(String dname : devices.keySet()) {
			indice.put(dname, 0);
		}
		while(true){
			boolean HasRule = false;
			for(DeviceParser d : devices.values()) {
				int index = indice.get(d.GetDeviceName());
				if (d.device_type.equals("acl")) {
					if (index >= d.acl_rules.size())
						continue;
					ACLRule r = d.acl_rules.get(index);
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
					indice.put(d.GetDeviceName(), index+1);
					update_number ++;
					HasRule = true;
				}
			}
			if (!HasRule) {
				break;
			}
		}

		for(String dname : devices.keySet()) {
			indice.put(dname, 0);
		}
		while(true){
			boolean HasRule = false;
			for(DeviceParser d : devices.values()) {
				int index = indice.get(d.GetDeviceName());
				if (d.getDeviceType().equals("fwd")) {
					if (index >= d.fw_rules.size())
						continue;
					ForwardingRule r = d.fw_rules.get(index);
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
					indice.put(d.GetDeviceName(), index+1);
					update_number ++;
					HasRule = true;
				}
			}
			if (!HasRule) {
				break;
			}
		}
		
		for (String update : updates) {
			updatewriter.write("+ " + update);
		}
		
		Collections.reverse(updates);
		
		for (String update : updates) {
			updatewriter.write("- " + update);
		}	
		updatewriter.close();
		
		System.out.println("Number of updates: " + update_number);
	}

	public void PrintUpdateRoundRobinRandom() throws IOException
	{
		File updatefile = new File(output_path + "updates");
		FileWriter updatewriter = new FileWriter(updatefile);
		
		ArrayList<String> updates = new ArrayList<String>();

		Random rand = new Random();
		
		while(true){
			boolean HasRule = false;
			for(DeviceParser d : devices.values()) {
				if (d.device_type.equals("acl")) {
					if (d.acl_rules.size() == 0) 
						continue;
					int index = rand.nextInt(d.acl_rules.size());
					ACLRule r = d.acl_rules.remove(index);
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
				}
				else if (d.getDeviceType().equals("fwd")) {
					if (d.fw_rules.size() == 0) 				
						continue;
					int index = rand.nextInt(d.fw_rules.size());
					ForwardingRule r = d.fw_rules.remove(index);
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
				}
				HasRule = true;
				update_number ++;
			}
			if (!HasRule) {
				break;
			}
		}
		
		for (String update : updates) {
			updatewriter.write("+ " + update);
		}
		
		Collections.reverse(updates);
		
		for (String update : updates) {
			updatewriter.write("- " + update);
		}	
		updatewriter.close();
		
		System.out.println("Number of updates: " + update_number);
	}
	
	public void PrintUpdateRoundRobinReverse() throws IOException
	{
		File updatefile = new File(output_path + "updates");
		FileWriter updatewriter = new FileWriter(updatefile);
		
		
		HashMap<String, Integer> indice = new HashMap<String, Integer>();
		ArrayList<String> updates = new ArrayList<String>();

		for(String dname : devices.keySet()) {
			indice.put(dname, 0);
		}
		while(true){
			boolean HasRule = false;
			for(DeviceParser d : devices.values()) {
				int index = indice.get(d.GetDeviceName());
				if (d.device_type.equals("acl")) {
					if (index >= d.acl_rules.size())
						continue;
					ACLRule r = d.acl_rules.get(d.acl_rules.size() - index -1);
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
				}
				else if (d.getDeviceType().equals("fwd")) {
					if (index >= d.fw_rules.size())
						continue;
					ForwardingRule r = d.fw_rules.get(index);
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
				}
				
				indice.put(d.GetDeviceName(), index+1);
				update_number ++;
				HasRule = true;
			}
			if (!HasRule) {
				break;
			}
		}
		
		for (String update : updates) {
			updatewriter.write("+ " + update);
		}
		
		Collections.reverse(updates);
		
		for (String update : updates) {
			updatewriter.write("- " + update);
		}	
		updatewriter.close();
		
		System.out.println("Number of updates: " + update_number);
	}
	
	public void PrintUpdateRoundRobin() throws IOException
	{
		File updatefile = new File(output_path + "updates");
		FileWriter updatewriter = new FileWriter(updatefile);
		
		
		HashMap<String, Integer> indice = new HashMap<String, Integer>();
		ArrayList<String> updates = new ArrayList<String>();

		for(String dname : devices.keySet()) {
			indice.put(dname, 0);
		}
		while(true){
			boolean HasRule = false;
			for(DeviceParser d : devices.values()) {
				int index = indice.get(d.GetDeviceName());
				if (d.device_type.equals("acl")) {
					if (index >= d.acl_rules.size())
						continue;
					ACLRule r = d.acl_rules.get(index);
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
				}
				else if (d.getDeviceType().equals("fwd")) {
					if (index >= d.fw_rules.size())
						continue;
					ForwardingRule r = d.fw_rules.get(index);
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
				}
				
				indice.put(d.GetDeviceName(), index+1);
				update_number ++;
				HasRule = true;
			}
			if (!HasRule) {
				break;
			}
		}
		
		for (String update : updates) {
			updatewriter.write("+ " + update);
		}
		
		Collections.reverse(updates);
		
		for (String update : updates) {
			updatewriter.write("- " + update);
		}	
		updatewriter.close();
		
		System.out.println("Number of updates: " + update_number);
	}
	
	public void PrintUpdateDeviceByDevice() throws IOException
	{
		File updatefile = new File(output_path + "updates");
		FileWriter updatewriter = new FileWriter(updatefile);
		
		ArrayList<String> updates = new ArrayList<String>();
		
		for(DeviceParser d : devices.values()) {
			if (d.getDeviceType().equals("acl")) {
				for (ACLRule r : d.acl_rules) {
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r.toString()+"\n");
				}
			}
			else if (d.getDeviceType().equals("fwd")) {
				for (ForwardingRule r : d.fw_rules) {
					updates.add(d.getDeviceType() + " " + d.GetDeviceName() + " " + r +"\n");
				}
			}
		}
		
		//Collections.shuffle(updates);

		for (String update : updates) {
			updatewriter.write("+ " + update);
		}
	
		//Collections.shuffle(updates);
		Collections.reverse(updates);
		
		for (String update : updates) {
			updatewriter.write("- " + update);
		}	
		
		updatewriter.close();
	}
	
	public void PrintUpdateDeviceByDeviceACLFirst() throws IOException
	{
		File updatefile = new File(output_path + "update");
		FileWriter updatewriter = new FileWriter(updatefile);
		
		ArrayList<String> updates = new ArrayList<String>();
		
		for(DeviceParser d : devices.values()) {
			if (d.getDeviceType().equals("acl")) {
				for (ACLRule r : d.acl_rules) {
					updates.add(d.GetDeviceName() + " " + r.toString()+"\n");
				}
			}
		}
		for(DeviceParser d : devices.values()) {		
			if (d.getDeviceType().equals("fwd")) {
				for (ForwardingRule r : d.fw_rules) {
					updates.add(d.GetDeviceName() + " " + r +"\n");
				}
			}
		}
		
		//Collections.shuffle(updates);

		for (String update : updates) {
			updatewriter.write("+ " + update);
		}
	
		//Collections.shuffle(updates);
		Collections.reverse(updates);
		
		for (String update : updates) {
			updatewriter.write("- " + update);
		}	
		
		updatewriter.close();
	}
	
	public void AttachACLNodeInTopology (String devicename, String port, String direction, String aclname) throws IOException
	{
		String acldevicename = devicename + "_" + aclname + "_" + port + "_" + direction;
		
		// the acl device already exists
		if (!acl_device_names.add(acldevicename)) 
			return;
				
		// the port that the acl takes effect
		PositionTuple fwpt = new PositionTuple(devicename, port);

		if(direction.equals("in")) {
			// the order of step 1 and 2 matters
			// step 1: update existing links
			PositionTuple aclpt = new PositionTuple(acldevicename, "inport");			
			for (PositionTuple pt : topology.keySet()) {
				if (topology.get(pt).contains(fwpt)) {
					topology.get(pt).remove(fwpt);
					topology.get(pt).add(aclpt);
				}
			}
			
			// step 2: add the ACL device to the topology
			addTopologyOneWay(acldevicename, "permit", devicename, port);
		}
		else if(direction.equals("out")) {
			// the order of step 1 and 2 matters
			// step 1: update existing links
			if (topology.containsKey(fwpt)) { 
				for (PositionTuple pt : topology.get(fwpt)){
					addTopologyOneWay(acldevicename, "permit", pt.getDeviceName(), pt.getPortName());
				}
				// 
				topology.get(fwpt).clear();
			}
			
			// step 2: add the ACL device to the topology
			addTopologyOneWay(devicename, port, acldevicename, "inport");
		}
	}
	
	public void ConstructTopology() 
	{
		Scanner OneLine = null;
		try {
			File topofile = new File(topo_file_name);
			OneLine = new Scanner (topofile);
			OneLine.useDelimiter("\n");	
		} catch (FileNotFoundException e) {
			System.err.println ("File not found! " + topo_file_name); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		while(OneLine.hasNext())
		{
			String linestr = OneLine.next().trim();
			String[] tokens = linestr.split(" ");
			addTopology(tokens[0], tokens[1], tokens[2], tokens[3]);
			link_number += 2;
		}
		
		System.out.println("Number of links: " + link_number);
	}
	
	public void addTopology(String d1, String p1, String d2, String p2)
	{
		//System.out.println(d1 + " " + p1 + " <-> " + d2 + " " + p2);
		PositionTuple pt1 = new PositionTuple(d1, p1);
		PositionTuple pt2 = new PositionTuple(d2, p2);
		// links are one way
		if(topology.containsKey(pt1))
		{
			topology.get(pt1).add(pt2);
		}else
		{
			HashSet<PositionTuple> newset = new HashSet<PositionTuple>();
			newset.add(pt2);
			topology.put(pt1, newset);
		}
		
		if(topology.containsKey(pt2))
		{
			topology.get(pt2).add(pt1);
		}else
		{
			HashSet<PositionTuple> newset = new HashSet<PositionTuple>();
			newset.add(pt1);
			topology.put(pt2, newset);
		}
	}
	
	public void addTopologyOneWay(String d1, String p1, String d2, String p2)
	{
		//System.out.println(d1 + " " + p1 + " -> " + d2 + " " + p2);
		PositionTuple pt1 = new PositionTuple(d1, p1);
		PositionTuple pt2 = new PositionTuple(d2, p2);
		// links are one way
		if(topology.containsKey(pt1))
		{
			topology.get(pt1).add(pt2);
		}else
		{
			HashSet<PositionTuple> newset = new HashSet<PositionTuple>();
			newset.add(pt2);
			topology.put(pt1, newset);
		}
	}
	
	public void DumpPorts () throws IOException
	{
		File vlanfile = new File(output_path + "port.txt");
		FileWriter portwriter = new FileWriter(vlanfile);
		
		for(Map.Entry<String, DeviceParser> entry: devices.entrySet()) {
			String dname = entry.getKey();
			DeviceParser d = entry.getValue();
			if (d.getDeviceType().equals("acl"))
				continue;
			port_number += d.fw_ports.size();
			portwriter.write(dname);
			for (String port : d.fw_ports) {
				portwriter.write(" " + port);
			}
			portwriter.write("\n");
		}
		
		portwriter.close();
		
		System.out.println("Number of forwarding ports: " + port_number);
	}
	
	public void DumpVLAN () throws IOException
	{
		File vlanfile = new File(output_path + "vlan.txt");
		FileWriter vlanwriter = new FileWriter(vlanfile);
		
		for(Map.Entry<String, DeviceParser> entry: devices.entrySet()) {
			String dname = entry.getKey();
			DeviceParser d = entry.getValue();
			if (d.getDeviceType().equals("acl"))
				continue;
			for (String vlan : d.vlan_ports.keySet()) {
				vlanwriter.write(dname + " " + vlan);
				HashSet<String> ports = d.vlan_ports.get(vlan);
				for (String port : ports)
					vlanwriter.write(" " + port);
				vlanwriter.write("\n");
			}
		}
		
		vlanwriter.close();
	}
	
	public void PrintTopology() throws IOException
	{
		int links_with_acls = 0;
		ArrayList<String> topology_strings =  new ArrayList<String>();	
		for (PositionTuple pt1 : topology.keySet()) {
			for (PositionTuple pt2 : topology.get(pt1)) {
				topology_strings.add(pt1.getDeviceName() + " " + pt1.getPortName() + " " 
						+ pt2.getDeviceName() + " " + pt2.getPortName() + "\n");
				links_with_acls ++;
			}
		}
		
		System.out.println("Number of links after adding ACL nodes: " + links_with_acls);
		
		Collections.sort(topology_strings);
		File topofile = new File(output_path + "topo.txt");
		FileWriter topowriter = new FileWriter(topofile);
		for (String s : topology_strings) {
			topowriter.write(s);
		}
		topowriter.close();
	}
}
