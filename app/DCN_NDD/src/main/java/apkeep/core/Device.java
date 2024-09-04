package apkeep.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javafx.util.Pair;

public class Device {
	private String name; //device name
	private LinkedList<String> listofInterface;
	
	private HashMap<String,HashSet<String>> vlan_ports;
	private HashMap<String,HashSet<String>> in_acl;
	private HashMap<String,HashSet<String>> out_acl;
	HashSet<String> tableofACLs;
	
	public HashMap<String,LinkedList<String>> acl_rules;
	public HashMap<String,LinkedList<String>> traffic_behavior;
	public HashMap<String,String> traffic_classifier;
	public HashMap<String,ArrayList<Pair<String,String>>> traffic_policy;
	public HashMap<String,HashSet<String>> in_policy; 
	
	public HashMap<String,String> arp_table;

	public Device(String name) {
		super();
		this.setName(name);
		setListofInterface(new LinkedList<String>());
		
		setVlan_ports(new HashMap<String,HashSet<String>>());
		setIn_acl(new HashMap<String,HashSet<String>>());
		setOut_acl(new HashMap<String,HashSet<String>>());
		tableofACLs = new HashSet<String>();
		
		acl_rules = new HashMap<String,LinkedList<String>>();
		traffic_behavior = new HashMap<String,LinkedList<String>>();
		traffic_classifier = new HashMap<String,String>();
		traffic_policy = new HashMap<String,ArrayList<Pair<String,String>>>();
		in_policy = new HashMap<String,HashSet<String>>();
		
		arp_table = new HashMap<String,String>();
	}
	public void addInterface(String name)
	{
		getListofInterface().add(name);
	}
	public void addArp(String ip_address,String interface_name) {
		arp_table.put(ip_address, interface_name);
	}
	public void addVlan(String vlan_name, String port) {
		// TODO Auto-generated method stub
		HashSet<String> portset = getVlan_ports().get(vlan_name);
		if(portset == null) {
			portset = new HashSet<String>();
		}
		portset.add(port);
		getVlan_ports().put(vlan_name, portset);
	}
	public void addBehavior(String behavior_name, String behavior) {
		// TODO Auto-generated method stub
		LinkedList<String> behaviors = traffic_behavior.get(behavior_name);
		if(behaviors == null) {
			behaviors = new LinkedList<String>();
		}
		behaviors.add(behavior);
		traffic_behavior.put(behavior_name, behaviors);
	}
	public void addClassifier(String classifier_name, String classifier) {
		// TODO Auto-generated method stub
		traffic_classifier.put(classifier_name, classifier);
	}
	public void addPolicy(String policy_name, Pair<String,String> policy) {
		// TODO Auto-generated method stub
		ArrayList<Pair<String,String>> policies = traffic_policy.get(policy_name);
		if(policies == null) {
			policies = new ArrayList<Pair<String,String>>();
		}
		policies.add(policy);
		traffic_policy.put(policy_name, policies);
	}
	public boolean hasInPolicy(String interfaceName) {
		// TODO Auto-generated method stub
		return in_policy.containsKey(interfaceName);
	}
	public void addInPolicy(String interface_name, String policy) {
		// TODO Auto-generated method stub
		HashSet<String> policy_set = in_policy.get(interface_name);
		if(policy_set == null) {
			policy_set = new HashSet<String>();
		}
		policy_set.add(policy);
		in_policy.put(interface_name, policy_set);
	}
	public void addInACL(String interface_name, String acl) {
		// TODO Auto-generated method stub
		HashSet<String> aclset = getIn_acl().get(interface_name);
		if(aclset == null) {
			aclset = new HashSet<String>();
		}
		aclset.add(acl);
		getIn_acl().put(interface_name, aclset);
	}

	public void addInACL(Collection<String> vlanInterface, String acl) {
		// TODO Auto-generated method stub
		if(vlanInterface == null) return;
		for(String interfaceName : vlanInterface) {
			addInACL(interfaceName,acl);
		}
	}
	public void addOutACL(String interface_name, String acl) {
		// TODO Auto-generated method stub
		HashSet<String> aclset = getOut_acl().get(interface_name);
		if(aclset == null) {
			aclset = new HashSet<String>();
		}
		aclset.add(acl);
		getOut_acl().put(interface_name, aclset);
	}
	public void addOutACL(Collection<String> vlanInterface, String acl) {
		// TODO Auto-generated method stub
		if(vlanInterface == null) return;
		for(String interfaceName : vlanInterface) {
			addOutACL(interfaceName,acl);
		}
	}
	public boolean hasInACL(String interfaceName) {
		// TODO Auto-generated method stub
		return getIn_acl().containsKey(interfaceName);
	}
	public boolean hasOutACL(String interfaceName) {
		// TODO Auto-generated method stub
		return getOut_acl().containsKey(interfaceName);
	}
	public Collection<String> getVlanInterface(String vlan_name) {
		// TODO Auto-generated method stub
		if(getVlan_ports().containsKey(vlan_name)) return getVlan_ports().get(vlan_name);
		return null;
	}
	public void putACL(String acl_name) {
		// TODO Auto-generated method stub
		tableofACLs.add(acl_name);
	}
	public boolean containsACL(String acl_name) {
		// TODO Auto-generated method stub
		return tableofACLs.contains(acl_name);
	}
	public String getArpInterface(String ip_address) {
		// TODO Auto-generated method stub
		return arp_table.get(ip_address);
	}
	public boolean hasInterface(String interfaceName) {
		// TODO Auto-generated method stub
		return getListofInterface().contains(interfaceName);
	}
	public void addACLrule(String acl_name, String acl_rule) {
		// TODO Auto-generated method stub
		if(acl_rules.containsKey(acl_name) == false) {
			LinkedList<String> acl_list = new LinkedList<String>();
			acl_rules.put(acl_name, acl_list);
		}
		acl_rules.get(acl_name).addLast(acl_rule);
	}
	public LinkedList<String> getListofInterface() {
		return listofInterface;
	}
	public void setListofInterface(LinkedList<String> listofInterface) {
		this.listofInterface = listofInterface;
	}
	public HashMap<String,HashSet<String>> getIn_acl() {
		return in_acl;
	}
	public void setIn_acl(HashMap<String,HashSet<String>> in_acl) {
		this.in_acl = in_acl;
	}
	public HashMap<String,HashSet<String>> getOut_acl() {
		return out_acl;
	}
	public void setOut_acl(HashMap<String,HashSet<String>> out_acl) {
		this.out_acl = out_acl;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public HashMap<String,HashSet<String>> getVlan_ports() {
		return vlan_ports;
	}
	public void setVlan_ports(HashMap<String,HashSet<String>> vlan_ports) {
		this.vlan_ports = vlan_ports;
	}
}
