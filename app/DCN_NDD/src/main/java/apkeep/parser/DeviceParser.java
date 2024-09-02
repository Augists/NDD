package apkeep.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import org.json.*;

import apkeep.parser.entity.DeviceConfig;
import apkeep.parser.entity.FWRule;
import apkeep.parser.entity.Interface;
import common.ACLRule;
import common.ACLUse;
import javafx.util.Pair;

public abstract class DeviceParser {
	
	public DeviceParser() {
		super();
	}
	
	public abstract void ParseConfig(DeviceConfig router,String inpath) throws IOException;
	public abstract void ParseRoutes(DeviceConfig router,String inpath) throws IOException;
	public abstract void ParseArps(DeviceConfig router, String inpath) throws IOException;
	public abstract void ParseMacs(DeviceConfig router, String inpath) throws IOException;
	
	public static void WriteConfig(DeviceConfig router,String outpath) throws IOException
	{
		File output_file = new File(outpath);
		FileWriter output_writer = new FileWriter(output_file);
		
		for(String key : router.acls.keySet()) {
			output_writer.write(key+"\n");
			LinkedList<ACLRule> acls = router.acls.get(key);
			for(ACLRule acl : acls) {
				output_writer.write(acl.toString()+"\n");
			}
		}
		
		output_writer.write(router.device_name+"\n");
		for(Interface inter : router.getInterfaces()) {
			output_writer.write(inter.toString()+"\n");
		}
		output_writer.close();
	}
	public static void WriteConfigJSON (DeviceConfig router,String outpath) throws IOException
	{
		JSONObject json = new JSONObject();
		
		File output_file = new File(outpath);
		FileWriter output_writer = new FileWriter(output_file);
		
		/**
		 *  add ACL module
		 *  for every ACL-group, use acl-num(key), app-list, rule-list
		 *  
		 *  for every app-list, use interfaceName, direction
		 */
		JSONObject ACLs = new JSONObject();
		for(String key : router.acls.keySet()) {
			JSONObject aclgroup = new JSONObject();
			/* set aclgroup "acl-rules" */
			LinkedList<ACLRule> acls = router.acls.get(key);
			for(ACLRule acl : acls) {
				aclgroup.append("rules", acl.toString());
			}
			/* set aclgroup "app-list" */
			LinkedList<ACLUse> acluses = router.acl_usage.get(key);
			if(acluses != null){
				for(ACLUse acluse : acluses) {
					JSONObject aclusejson = new JSONObject();
					aclusejson.put("interface", acluse.getinterface());
					aclusejson.put("direction", acluse.getdirection());
					aclgroup.append("applications", aclusejson);
				}
			}
			ACLs.put(key, aclgroup);
		}
		json.append("acl", ACLs);
		
		/**
		 *  add vlan and port module
		 *  for every vlan, use vlan-num(key), IP, mode, port-list, acl-app-list
		 *  for every port, use interfaceName(key), IP, mode, vlan-list, acl-app-list
		 *  
		 *  
		 *  for every port-list, use interfaceName(key), mode
		 *  for every acl-app-list, use acl-num(key), direction
		 */
		JSONObject Vlans = new JSONObject();
		JSONObject Ports = new JSONObject();
		
		for(Interface intf : router.getInterfaces()) {
//			System.out.println(intf.toString());
			String interfaceName = intf.getName();
			JSONObject vlan = new JSONObject();
			/* set vlan "IP" */
			JSONObject ipjson = new JSONObject();
			ipjson.put("prefix", intf.ip_prefix);
			ipjson.put("mask", intf.ip_mask);
			vlan.put("ip", ipjson);
			
			/*set vlan "mode" */
			vlan.put("mode", intf.switch_port_mode);
			
			/* set vlan "acl-app-list" */
			JSONObject acl_app_list = new JSONObject();
			for(String aclnum : intf.in_acls) {
				acl_app_list.append("in", aclnum);
			}
			for(String aclnum : intf.out_acls) {
				acl_app_list.append("out", aclnum);
			}
			vlan.append("acls", acl_app_list);
			
			JSONObject policy_list = new JSONObject();
			for(String policyName : intf.in_policies) {
				policy_list.append("in", policyName);
			}
			for(String policyName : intf.out_policies) {
				policy_list.append("out", policyName);
			}
			vlan.append("policy", policy_list);
			
			JSONObject topo_list = new JSONObject();
			for(String rem_topo : intf.rem_topo) {
				String[] tokens = rem_topo.split(" ");
				String rem_device_name = tokens[0];
				String rem_port_id = tokens[1];
				topo_list.append(rem_device_name, rem_port_id);
			}
			vlan.append("lldp", topo_list);
			
			if(interfaceName.startsWith("Vlan")) {
				/* for vlan set vlan "port-list" */
				vlan.put("trunk ports", intf.trunk_vlans);
				vlan.put("access ports", intf.access_vlans);
				Vlans.put(interfaceName, vlan);
			}
			else {
				/* for port set port "vlan-list" */
				vlan.put("vlan", intf.getPortList());
				Ports.put(interfaceName, vlan);
			}
			
			
		}
		json.append("vlan", Vlans);
		json.append("interface", Ports);
		
		/*
		 * add policy module
		 * for every policy, use name, classifier, behavior and direction
		 */
		JSONObject Policies = new JSONObject();
//		System.out.println("*************************");
//		System.out.println(router.traffic_policy.size());
//		System.out.println("*************************");
		for(String key : router.traffic_policy.keySet()) {
//			System.out.println(key);
			JSONObject one_policy = new JSONObject();
			JSONArray traffic_pair_list = new JSONArray();
			/* set aclgroup "acl-rules" */
			ArrayList<Pair<String, String>> policies = router.traffic_policy.get(key);
			for(Pair<String, String> policy : policies) {
				JSONObject traffic_policy = new JSONObject();
				traffic_policy.put("classifier", policy.getKey());
				traffic_policy.put("behavior", policy.getValue());
				traffic_pair_list.put(traffic_policy);
			}
			one_policy.put("rules", traffic_pair_list);
			/* set aclgroup "app-list" */
			LinkedList<ACLUse> policyuses = router.policy_usage.get(key);
//			System.out.println(key+" "+policyuses);
			if(policyuses != null){
				for(ACLUse policyuse : policyuses) {
					JSONObject policyusejson = new JSONObject();
					policyusejson.put("interface", policyuse.getinterface());
					policyusejson.put("direction", policyuse.getdirection());
					one_policy.append("applications", policyusejson);
				}
			}
			Policies.put(key, one_policy);
		}
		json.append("policy",Policies);
		
		JSONObject Classifiers = new JSONObject();
		for(String key : router.traffic_classifier.keySet()) {
//			System.out.println(key);
//			JSONObject traffic_classifier = new JSONObject();
			/* set aclgroup "acl-rules" */
			String classifier = router.traffic_classifier.get(key).get(0);
			/* set aclgroup "app-list" */
			Classifiers.put(key, classifier);
		}
		json.append("classifier",Classifiers);
		
		JSONObject Behaviors = new JSONObject();
		for(String key : router.traffic_behavior.keySet()) {
//			JSONObject traffic_behavior = new JSONObject();
			/* set aclgroup "acl-rules" */
			LinkedList<String> behaviors = router.traffic_behavior.get(key);
			for(String behavior : behaviors) {
//				System.out.println(key+" "+behavior);
				/* set aclgroup "app-list" */
				Behaviors.append(key, behavior);
			}
		}
		json.append("behavior",Behaviors);
		
		JSONObject Group = new JSONObject();
		for(String key : router.channel_group.keySet()) {
//			JSONObject traffic_behavior = new JSONObject();
			/* set aclgroup "acl-rules" */
			LinkedList<String> groups = router.channel_group.get(key);
			for(String group : groups) {
//				System.out.println(key+" "+behavior);
				/* set aclgroup "app-list" */
				Group.append(key, group);
			}
		}
		json.append("port-channel",Group);
		
		json.write(output_writer);
		output_writer.close();
	}
	public final static void WriteRoutes (DeviceConfig device,String out_path) throws IOException
	{
		
		File output_file = new File(out_path);
		FileWriter output_writer = new FileWriter(output_file);
		
		/* add fw-list */
		for(FWRule rule : device.fw_rules) {
			output_writer.write(rule.toString()+"\n");
		}
		
		output_writer.close();
	}
	public final static void WriteRoutesJSON (DeviceConfig device,String out_path) throws IOException
	{
		JSONObject json = new JSONObject();
		
		File output_file = new File(out_path);
		FileWriter output_writer = new FileWriter(output_file);
		
		/* add fw-list */
		for(FWRule rule : device.fw_rules) {
			JSONObject fwrule = new JSONObject();
			fwrule.put("prefix", rule.prefix);
			fwrule.put("nexthop", rule.nexthop);
			fwrule.put("outinterface", rule.outinterface);
			
			json.append("FW", fwrule);
		}
		
		json.write(output_writer);
		output_writer.close();
	}
}
