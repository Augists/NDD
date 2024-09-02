package apkeep.parser.entity;

import java.util.*;

import common.ACLRule;
import common.ACLUse;
import javafx.util.Pair;

public class DeviceConfig {
	
	public String device_name;
	
	public Hashtable<String, Interface> interfaces;
	public Hashtable<String, Interface> interfaces_with_ip;
	
	public Hashtable<String, LinkedList<ACLRule>> acls;
	// acl_usage<acl_number, acl_use_list>
	public Hashtable<String, LinkedList<ACLUse>> acl_usage;
	
	public LinkedList<FWRule> fw_rules;
	
	// behavior<name,action>
	// action: drop,ip address, interface name
	public HashMap<String,LinkedList<String>> traffic_behavior;
	// classifier<name,if-macth_list>
	// this is some type of operation field need to be considered
	public HashMap<String,ArrayList<String>> traffic_classifier;
//	public HashMap<String,String> traffic_classifier;
	// policy<name, classifier-behavior_list>
	public HashMap<String,ArrayList<Pair<String,String>>> traffic_policy;
	
	public HashMap<String,LinkedList<ACLUse>> policy_usage;
	
	public HashMap<String,LinkedList<String>> channel_group;
	
	//bd/vbdif num 2 l2/l3 vni
	public HashMap<String,String> bds;
	public HashMap<String,String> vni_bd;
	public HashMap<String,String> bd_vrfs;
	public HashMap<String,HashSet<String>> vrf_bds;
	
	public HashMap<String,LinkedList<MCSRule>> epgs;
	public HashMap<String,LinkedList<MCSRule>> segment_classifier;

	public HashMap<String,HashSet<FWRule>> static_routes;
	
	public DeviceConfig() {
		device_name = null;
		interfaces = new Hashtable<String, Interface>();
		interfaces_with_ip = new Hashtable<String, Interface>();
		acls = new Hashtable<String, LinkedList<ACLRule>>();
		acl_usage = new Hashtable<String, LinkedList<ACLUse>>();
		fw_rules = new LinkedList<FWRule>();
		
		traffic_behavior = new HashMap<String,LinkedList<String>>();
		traffic_classifier = new HashMap<String,ArrayList<String>>();
//		traffic_classifier = new HashMap<String,String>();
		traffic_policy = new HashMap<String,ArrayList<Pair<String,String>>>();
		policy_usage = new HashMap<String,LinkedList<ACLUse>>();
		channel_group = new HashMap<String,LinkedList<String>>();
		
		bds = new HashMap<String,String>();
		vni_bd = new HashMap<String,String>();
		bd_vrfs = new HashMap<String,String>();
		vrf_bds = new HashMap<String,HashSet<String>>();
		epgs = new HashMap<String,LinkedList<MCSRule>>();
		segment_classifier = new HashMap<String,LinkedList<MCSRule>>();
		static_routes = new HashMap<String,HashSet<FWRule>>();
	}
	
	public Collection<Interface> getInterfaceWithIP()
	{
		return interfaces_with_ip.values();
	}
	public Collection<Interface> getInterfaces()
	{
		return interfaces.values();
	}
	public Interface getInterfacebyName(String intName)
	{
		return interfaces.get(intName);
	}
	
	@Override
	public String toString() {
		return device_name + " "
				+ interfaces + " "
				+ interfaces_with_ip + " "
				+ acls;
	}
	
	public String getname()
	{
		return device_name;
	}
	
	public Hashtable<String, LinkedList<ACLRule>> getACLs()
	{
		return acls;
	}

	public void addBD(String bd_id, String vni) {
		// TODO Auto-generated method stub
		bds.put(bd_id,vni);
		vni_bd.put(vni, bd_id);
	}
}
