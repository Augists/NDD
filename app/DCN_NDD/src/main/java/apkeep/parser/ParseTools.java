package apkeep.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Pattern;

import apkeep.parser.entity.DeviceConfig;
import apkeep.parser.entity.Interface;
import apkeep.parser.entity.MCSRule;
import apkeep.parser.entity.NetworkConfig;
import common.ACLUse;
import common.Utility;

public class ParseTools {
	
	public static String SeperateMask(String instr, int ind)
	{
		int maskNum = Integer.parseInt(instr.substring(ind + 1));
		// maskNum = 0,1,2,...32
		int byteNum = maskNum / 8;
		if(byteNum == 4)
		{
			return "255.255.255.255";
		}
		String mask = "";
		for(int i = 0; i < byteNum; i ++)
		{
			mask = mask + "255.";
		}

		int rest = maskNum%8;
		int nextByteNum = (int) Utility.SumPower2(7 - rest + 1, 7);
		mask = mask + Integer.toString(nextByteNum);

		//fill up the rest bytes with "0"
		for(int i = 0; i < 4 - byteNum - 1; i ++)
		{
			mask = mask + ".0";
		}

		return mask;
	}
	public static void GetArgument(Scanner tokeninline, ArrayList<String> argument)
	{
		argument.clear();
		while(tokeninline.hasNext())
		{
			argument.add(tokeninline.next());
		}
	}

	public static void ProcessEthPort(DeviceConfig router) {
		// TODO Auto-generated method stub
		for(Interface one_interface : router.interfaces.values()) {
//			System.out.println(one_huawei_interface.eth_trunk);
			if(one_interface.eth_trunk != null) {
				// locate the eth_trunk interface
				Interface trunk_interface = router.interfaces.get(one_interface.eth_trunk);
				one_interface.copyInfo(trunk_interface);
				LinkedList<String> group_list = router.channel_group.get(one_interface.eth_trunk);
				if(group_list == null) {
					group_list = new LinkedList<String>();
				}
				group_list.add(one_interface.interface_name);
				router.channel_group.put(one_interface.eth_trunk, group_list);
			}
		}
	}
	public static void ProcessPolicyUse(DeviceConfig router) {
		// TODO Auto-generated method stub
		for(Interface routerInterface : router.interfaces.values()) {
			for(String policy_name : routerInterface.in_policies) {
				LinkedList<ACLUse> policyuselist = router.policy_usage.get(policy_name);
				if(policyuselist == null) {
					policyuselist = new LinkedList<ACLUse>();
				}
				ACLUse policyuse = new ACLUse(policy_name,routerInterface.interface_name,"in");
				policyuselist.addLast(policyuse);
				router.policy_usage.put(policy_name, policyuselist);
			}
			for(String policy_name : routerInterface.out_policies) {
				LinkedList<ACLUse> policyuselist = router.policy_usage.get(policy_name);
				if(policyuselist == null) {
					policyuselist = new LinkedList<ACLUse>();
				}
				ACLUse policyuse = new ACLUse(policy_name,routerInterface.interface_name,"out");
				policyuselist.addLast(policyuse);
				router.policy_usage.put(policy_name, policyuselist);
			}
		}
	}
	public static void ProcessACLUse(DeviceConfig router) {
		// TODO Auto-generated method stub
		for(Interface routerInterface : router.interfaces.values()) {
			for(String aclnum : routerInterface.in_acls) {
				LinkedList<ACLUse> acluselist = router.acl_usage.get(aclnum);
				if(acluselist == null) {
					acluselist = new LinkedList<ACLUse>();
				}
				ACLUse acluse = new ACLUse(aclnum,routerInterface.interface_name,"in");
				acluselist.add(acluse);
				router.acl_usage.put(aclnum, acluselist);
			}
			for(String aclnum : routerInterface.out_acls) {
				LinkedList<ACLUse> acluselist = router.acl_usage.get(aclnum);
				if(acluselist == null) {
					acluselist = new LinkedList<ACLUse>();
				}
				ACLUse acluse = new ACLUse(aclnum,routerInterface.interface_name,"out");
				acluselist.add(acluse);
				router.acl_usage.put(aclnum, acluselist);
			}
		}
	}

	public static void ProcessVlanUse(DeviceConfig router) {
		// TODO Auto-generated method stub
		for(Interface one_interface : router.interfaces.values()) {
			if(one_interface.switch_port_mode == null) continue;
			ArrayList<String> vlanlist = one_interface.getPortList();
			for(String vlan : vlanlist) {
				Interface vlan_interface = router.getInterfacebyName(vlan);
				if(vlan_interface != null) {
					if(one_interface.switch_port_mode.equals("trunk")) {
						vlan_interface.trunk_vlans.add(one_interface.interface_name);
					}
					else if(one_interface.switch_port_mode.equals("access")) {
						vlan_interface.access_vlans.add(one_interface.interface_name);
					}
				}
			}
		}
	}
	
	public static void ProcessVBDIFSubnet(NetworkConfig network, DeviceConfig router) {
		// TODO Auto-generated method stub
		String nve_ip = null;
		for(Interface one_interface : router.interfaces_with_ip.values()) {
			if(one_interface.interface_name.startsWith("nve")) {
				nve_ip = one_interface.ip_prefix;
				break;
			}
		}
		if(nve_ip == null) return;
		for(Interface one_interface : router.interfaces_with_ip.values()) {
			if(one_interface.interface_name.startsWith("vbdif")) {
				String prefix = getSlashedPrefix(one_interface.ip_prefix,one_interface.ip_mask);
				HashSet<String> nves = network.subnets.get(prefix);
				if(nves == null) nves = new HashSet<String>();
				nves.add(nve_ip);
				network.subnets.put(prefix, nves);
			}
		}
		
	}
	public static void ProcessEPGSubnets(NetworkConfig network, DeviceConfig device) {
		// TODO Auto-generated method stub
		for(String epg : device.epgs.keySet()) {
			for(MCSRule rule : device.epgs.get(epg)) {
				String subnet = getSlashedPrefix(rule.ipAddress, rule.mask);
				if(network.epg_fields.containsKey(epg)) {
					network.epg_fields.get(epg).add(subnet);
				}
				else {
					HashSet<String> subnets = new HashSet<String>();
					subnets.add(subnet);
					network.epg_fields.put(epg, subnets);
				}
			}
		}
	}
	public static boolean isIpPrefix(String prefix) {
		// TODO Auto-generated method stub
		String patter = "(?:[\\d]{1,3})\\.(?:[\\d]{1,3})\\.(?:[\\d]{1,3})\\.(?:[\\d]{1,3})/(?:[\\d]{1,2})";
		return Pattern.matches(patter, prefix);
	}
	public static String dotted_prefix_to_int(String prefix) {
		// TODO Auto-generated method stub
		long exp = 3;
		long intip = 0;
		int subnet = 32;
		String[] tokens = prefix.split("/");
		
		if(tokens.length > 1) {
			subnet = Integer.valueOf(tokens[1]);
		}
		if(subnet > 32) return null;
		String dotted_ip = tokens[0];
		String[] quad_ip = dotted_ip.split("\\.");
		if(quad_ip.length!=4) {
//			System.out.println(dotted_ip);
			return null;
		}
		for(String quad : quad_ip) {
//			System.out.print(quad + " ");
			intip = (long) (intip + (Long.valueOf(quad) * Math.pow(256L, exp)));
			exp = exp - 1;
		}
//		System.out.println(intip+" "+subnet);
		return intip+" "+subnet;
	}
	public static String get_ethernet_port_name(String port) {
		// TODO Auto-generated method stub
		port = port.split("\\.")[0];
		String result = "";
		String reminder = "";
		if(port.toLowerCase().startsWith("tengigabitethernet")) {
	    	result = "te";
	    	reminder = port.substring("tengigabitethernet".length());
	    }
		else if(port.toLowerCase().startsWith("xgigabitethernet")) {
	    	result = "xge";
	    	reminder = port.substring("xgigabitethernet".length());
	    }
		else if(port.toLowerCase().startsWith("gigabitethernet")) {
	    	result = "gi";
	    	reminder = port.substring("gigabitethernet".length());
	    }
		else if(port.toLowerCase().startsWith("fastethernet")) {
	    	result = "fa";
	    	reminder = port.substring("fastethernet".length());
	    }
		else{
	    	result = port.toLowerCase();
	    }
		return result+reminder;
	}
	public static String getSlashedPrefix(String ip_address, String ip_mask) {
		// TODO Auto-generated method stub
		String prefix = ip_address;
		int prefix_len = 0;
		String[] quad_ip = ip_mask.split("\\.");
		for(String quad : quad_ip) {
			int quad_ip_int = Integer.valueOf(quad);
			while(quad_ip_int > 0) {
				prefix_len = prefix_len + (quad_ip_int & 1);
				quad_ip_int = quad_ip_int >> 1;
			}
		}
		return prefix+"/"+Integer.toString(prefix_len);
	}
	public static Long getPrefixUpper(String prefix, int bits) {
		// TODO Auto-generated method stub
		Long lower = Long.valueOf(prefix.split("/")[0]);
		int len = Integer.valueOf(prefix.split("/")[1]);
		Long mask = 0L;
		Long paste = 0L;
		for(int i=1;i<=len;i++) {
			mask = mask << 1;
			mask = mask | 1;
		}
		for(int i=1;i<=bits-len;i++) {
			mask = mask << 1;
			paste = paste << 1;
			paste = paste | 1;
		}
		Long upper = (lower & mask) | paste;
		return upper;
	}
	public static int[] PrefixLongToBin(long prefix, int prefixlen) 
	{
    	int[] bin = new int[32];
    	for (int i=0; i<32; i++) {
    		if (i >= prefixlen) {
    			bin[i] = 2;
    		}
    		else if((prefix & (1 << (32-i-1))) == 0) {
    			bin[i] = 0;
    		}
    		else {
    			bin[i] = 1;
    		}
    	}    
    	return bin;
	}
}