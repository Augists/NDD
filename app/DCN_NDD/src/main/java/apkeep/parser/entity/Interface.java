package apkeep.parser.entity;

import java.util.*;

import javafx.util.Pair;

public class Interface {
	
	public String interface_name;
	
	public String switch_port_mode;
	public ArrayList<String> trunk_vlans;
	public ArrayList<String> access_vlans;
	
//	public ArrayList<Pair<String,String>> ip_prefix_list;
	public String ip_prefix;
	public String ip_mask;
	
	public ArrayList<String> in_acls; 
	public ArrayList<String> out_acls;

	public String power;
	
	public ArrayList<String> in_policies;
	public ArrayList<String> out_policies;
	
	public ArrayList<String> rem_topo;
	
	public String eth_trunk;
	
	public Interface() {
		interface_name = null;
		
//		ip_prefix_list = new ArrayList<Pair<String,String>>();
		ip_prefix = null;
		ip_mask = null;
		
		switch_port_mode = null;
		trunk_vlans = new ArrayList<String>();
		access_vlans = new ArrayList<String>();
		
		in_acls = new ArrayList<String>();
		out_acls = new ArrayList<String>();
		
		power = "no shutdown";
		
		eth_trunk = null;
//		switch_port_mode = "access";
		in_policies = new ArrayList<String>();
		out_policies = new ArrayList<String>();
		
		rem_topo = new ArrayList<String>();
	}
	
	@SuppressWarnings("unchecked")
	public void copyInfo(Interface trunk_interface) {
		// TODO Auto-generated method stub
		
		switch_port_mode = trunk_interface.switch_port_mode;
		
		trunk_vlans = (ArrayList<String>) trunk_interface.trunk_vlans.clone();
		access_vlans = (ArrayList<String>) trunk_interface.access_vlans.clone();
		
		in_acls = (ArrayList<String>) trunk_interface.in_acls.clone();
		out_acls = (ArrayList<String>) trunk_interface.out_acls.clone();
		
		in_policies = (ArrayList<String>) trunk_interface.in_policies.clone();
		out_policies = (ArrayList<String>) trunk_interface.out_policies.clone();
		
		rem_topo = (ArrayList<String>) trunk_interface.rem_topo.clone();
//		System.out.println(toString());
	}
	
	@Override
	public String toString() {
		return interface_name + " " + ip_prefix + " " + ip_mask + " "
	    + switch_port_mode + " " + trunk_vlans + " " + access_vlans + " "
		+ in_acls + " " + out_acls;
	}

	public ArrayList<String> getPortList() {
		// TODO Auto-generated method stub
		if(switch_port_mode == null) return null;
		if(switch_port_mode.equals("trunk")) {
			return trunk_vlans;
		}
		else if(switch_port_mode.equals("access")){
			return access_vlans;
		}
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return interface_name;
	}
}