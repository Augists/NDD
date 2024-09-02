package apkeep.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import org.json.*;

import apkeep.parser.entity.DeviceConfig;
import apkeep.parser.entity.FWRule;
import apkeep.parser.entity.Interface;
import common.ACLRule;
import common.ACLUse;

public class TencentParser extends DeviceParser {

	public TencentParser() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public void ParseConfig(DeviceConfig device, String inpath) throws IOException {
		// TODO Auto-generated method stub
		
		// cess json file
		Scanner OneLine = null;
		try {
			File route_file = new File(inpath);
			OneLine = new Scanner (route_file);
			OneLine.useDelimiter("\n");
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!" + inpath);
			System.exit (0);
		}
		
		String content = "";
		
		/* Read line by line */
		while (OneLine.hasNext()) {
			Scanner TokenInLine = new Scanner(OneLine.next());
			while(TokenInLine.hasNext()) {
				content = content + TokenInLine.next();
			}
		} // end of while (oneline.hasNext())
		
		JSONObject json = new JSONObject(content);

		HandleInterface(device,json);
		HandleACL(device,json);
		HandleLLDP(device,json);
	}

	private void HandleLLDP(DeviceConfig device, JSONObject json) {
		// TODO Auto-generated method stub
		JSONObject lldps = json.getJSONObject("LLDP_ENTRY_TABLE").getJSONObject("value");
		
		for(String key : lldps.keySet()) {
			Interface port1 = device.interfaces.get(key);
			
			if(port1 == null) continue;
			JSONObject lldp_desc = lldps.getJSONObject(key);
			String rem_device_name = lldp_desc.getString("lldp_rem_port_desc");
			rem_device_name = rem_device_name.split("-")[0];
			String rem_port_id = lldp_desc.getString("lldp_rem_port_id");
			
			String rem_topo = rem_device_name+" "+rem_port_id;
			System.out.println(rem_topo);
			port1.rem_topo.add(rem_topo);
		}
	}

	private void HandleInterface(DeviceConfig device, JSONObject json) {
		// TODO Auto-generated method stub
		// the order must hold
		// everything is about interface
		HandlePorts(device,json);
		HandleLAG(device,json);
		HandleVlan(device,json);
	}

	private void HandlePorts(DeviceConfig device, JSONObject json) {
		// TODO Auto-generated method stub
		JSONObject interfaces = json.getJSONObject("INTF_TABLE").getJSONObject("value");
//		
		for(String key : interfaces.keySet()) {
//			System.out.println(key);
			String[] tokens = key.split(":");
			String interface_name = tokens[0];
			String ip_prefix = tokens[1];
			
			Interface router_interface = new Interface();
			// set name
			router_interface.interface_name = interface_name;
			// set ip address
//			System.out.println(ip_prefix);
			int slashInd = ip_prefix.indexOf('/');
//			System.out.println(slashInd);
			int maskNum = Integer.parseInt(ip_prefix.substring(slashInd + 1));
			// ipv6
			if(maskNum > 32) continue;
			String mask = ParseTools.SeperateMask(ip_prefix, slashInd);
			router_interface.ip_prefix = ip_prefix.substring(0, slashInd);
			router_interface.ip_mask = mask;
			// add to router
			device.interfaces.put(router_interface.interface_name, router_interface);
			device.interfaces_with_ip.put(router_interface.ip_prefix, router_interface);
		}
			
	}

	private void HandleLAG(DeviceConfig device, JSONObject json) {
		// TODO Auto-generated method stub
		JSONObject lags = json.getJSONObject("LAG_MEMBER_TABLE").getJSONObject("value");
//		
		for(String key : lags.keySet()) {
//			System.out.println(key);
			String[] tokens = key.split(":");
			String lag_name = tokens[0];
			String interface_name = tokens[1];
			
			Interface router_interface = device.interfaces.get(interface_name);
			if(router_interface == null) {
				/* if the interface does not exist, might be tow things happening
				 * 1. wrong configuration
				 * 2. interface has been filtered because of ipv6 address
				 * 3. interface don't have configuration( directly connect with host)
				*/
//				continue;
				
				router_interface = new Interface();
				router_interface.interface_name = interface_name;
				
			}
			router_interface.eth_trunk = lag_name;
			
			device.interfaces.put(router_interface.interface_name, router_interface);
		}
	}
	
	private void HandleVlan(DeviceConfig device, JSONObject json) {
		// TODO Auto-generated method stub
		JSONObject acls = json.getJSONObject("VLAN_MEMBER_TABLE").getJSONObject("value");
		
		for(String key : acls.keySet()) {
			String[] tokens = key.split(":");
			String vlan_name = tokens[0];
			String interface_name = tokens[1];
			
			// this interface maybe a LAG port
			Interface router_interface = device.interfaces.get(interface_name);
			if(router_interface == null) {
				/* same situation as LAG */
//				continue;
				router_interface = new Interface();
				router_interface.interface_name = interface_name;
			}
			
			/* for no keys for weather access or trunk mode 
			 * assuming everything is access */
			router_interface.switch_port_mode = "access";
			router_interface.access_vlans.add(vlan_name);
			
			device.interfaces.put(router_interface.interface_name, router_interface);
		}
	}

	private void HandleACL(DeviceConfig device, JSONObject json) {
		// TODO Auto-generated method stub
		JSONObject acls = json.getJSONObject("ACL_RULE").getJSONObject("value");
		
		for(String key : acls.keySet()) {
			String[] tokens = key.split("\\|");
			String acl_name = tokens[0];
			String acl_num = tokens[1];
			
			LinkedList<ACLRule> oneacl = device.acls.get(acl_name);
			if(oneacl == null) oneacl = new LinkedList<ACLRule>();
			/* we care about this field: 
			 *  IN_PORTS
			 *  OUT_PORTS
			 *  OUT_VLANID
			 *  PACKET_ACTION
			 *  PRIORITY
			 *  L4_DST_PORT(_RANGE)
			 *  L4_SRC_PORT(_RANGE)
			 *  IP_PROTOCOL
			 *  SRC_IP
			 *  DST_IP
			 *  */
			
			if(acls.getJSONObject(key).has("SRC_IPV6") || acls.getJSONObject(key).has("DST_IPV6"))
			{
//				System.out.println(acl_name);
				continue;
			}
			
			ACLRule onerule = new ACLRule();
			onerule.accessList = "access-list";
			onerule.accessListNumber = acl_name;
			onerule.dynamicName = acl_num;
			onerule.permitDeny = acls.getJSONObject(key).getString("PACKET_ACTION").toLowerCase();
			// change the type to cisco
			if(onerule.permitDeny.equals("drop") == false) onerule.permitDeny = "permit";
			else onerule.permitDeny = "deny";
			/* beware of format*/
			if(acls.getJSONObject(key).has("SRC_IP")){
				String ipAddrStr = acls.getJSONObject(key).getString("SRC_IP");
				int slashInd = ipAddrStr.indexOf('/');
				if(slashInd >=0)
				{
					String mask = ParseTools.SeperateMask(ipAddrStr, slashInd);
					onerule.source = ipAddrStr.substring(0, slashInd);
					onerule.sourceWildcard = mask;
				}
			}
			if(acls.getJSONObject(key).has("DST_IP")){
				String ipAddrStr = acls.getJSONObject(key).getString("DST_IP");
				int slashInd = ipAddrStr.indexOf('/');
				if(slashInd >=0)
				{
					String mask = ParseTools.SeperateMask(ipAddrStr, slashInd);
					onerule.destination = ipAddrStr.substring(0, slashInd);
					onerule.destinationWildcard = mask;
				}
			}
			if(acls.getJSONObject(key).has("PRIORITY")){
				onerule.priority = Integer.valueOf(acls.getJSONObject(key).getString("PRIORITY"));
			}
			if(acls.getJSONObject(key).has("IP_PROTOCOL")){
				onerule.protocolLower = acls.getJSONObject(key).getString("IP_PROTOCOL");
				onerule.protocolUpper = onerule.protocolLower;
			}
			else {
				onerule.protocolLower = "0";
				onerule.protocolUpper = "255";
			}
			if(acls.getJSONObject(key).has("L4_SRC_PORT")){
				onerule.sourcePortLower = acls.getJSONObject(key).getString("L4_SRC_PORT");
				onerule.sourcePortUpper = onerule.sourcePortLower;
			}
			else if(acls.getJSONObject(key).has("L4_SRC_PORT_RANGE")){
				String src_port = acls.getJSONObject(key).getString("L4_SRC_PORT_RANGE");
				String[] ports = src_port.split("-");
				onerule.sourcePortLower = ports[0];
				onerule.sourcePortUpper = ports[1];
			}
			if(acls.getJSONObject(key).has("L4_DST_PORT")){
				onerule.destinationPortLower = acls.getJSONObject(key).getString("L4_DST_PORT");
				onerule.destinationPortUpper = onerule.destinationPortLower;
			}
			else if(acls.getJSONObject(key).has("L4_DST_PORT_RANGE")){
				String src_port = acls.getJSONObject(key).getString("L4_DST_PORT_RANGE");
				String[] ports = src_port.split("-");
				onerule.destinationPortLower = ports[0];
				onerule.destinationPortUpper = ports[1];
			}
			oneacl.add(onerule);
			device.acls.put(acl_name, oneacl);
			if(acls.getJSONObject(key).has("IN_PORTS")){
				String port = acls.getJSONObject(key).getString("IN_PORTS");
				if(port.equals("ALL")) {
					for(Interface router_interface : device.interfaces.values()) {
						router_interface.in_acls.add(acl_name);
					}
					continue;
				}
				Interface router_interface = device.interfaces.get(port);
				if(router_interface == null) {
					/* same situation as LAG */
					continue;
				}
				router_interface.in_acls.add(acl_name);
//				LinkedList<ACLUse> acl_use = device.acl_usage.get(acl_name);
//				if(acl_use == null) {
//					acl_use = new LinkedList<ACLUse>();
//				}
//				ACLUse use = new ACLUse(acl_name,port,"in");
//				Boolean flag = false;
//				for(ACLUse exist_use : acl_use) {
//					if(exist_use.getinterface().equals(use.getinterface()) &&
//							exist_use.getdirection().equals(use.getdirection())) {
//						flag = true;
//					}
//				}
//				if(!flag) acl_use.add(use);
//				device.acl_usage.put(acl_name, acl_use);
			}
			if(acls.getJSONObject(key).has("OUT_PORTS")){
				String port = acls.getJSONObject(key).getString("OUT_PORTS");
				if(port.equals("ALL")) {
					for(Interface router_interface : device.interfaces.values()) {
						router_interface.out_acls.add(acl_name);
					}
					continue;
				}
				Interface router_interface = device.interfaces.get(port);
				if(router_interface == null) {
					/* same situation as LAG */
					continue;
				}
				router_interface.out_acls.add(acl_name);
			}
			if(acls.getJSONObject(key).has("OUT_VLANID")) {
				String vlan_id = acls.getJSONObject(key).getString("OUT_VLANID");
				if(vlan_id.equals("ALL")) {
					for(Interface router_interface : device.interfaces.values()) {
						router_interface.out_acls.add(acl_name);
					}
					continue;
				}
				Interface router_interface = device.interfaces.get(vlan_id);
				if(router_interface == null) {
					/* same situation as LAG */
					continue;
				}
				ArrayList<String> out_acls = router_interface.out_acls;
				Boolean flag = false;
				for(String out_acl : out_acls) {
					if(out_acl.equals(acl_name)) {
						flag = true;
						break;
					}
				}
				if(!flag) router_interface.out_acls.add(acl_name);
			}
		}
	}

	@Override
	public void ParseRoutes(DeviceConfig device, String inpath) throws IOException {
		// TODO Auto-generated method stub
//		System.out.println("in route parser!");
		Scanner OneLine = null;
		try {
			File route_file = new File(inpath);
			OneLine = new Scanner (route_file);
			OneLine.useDelimiter("\n");
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!" + inpath);
			System.exit (0);
		}
		
		String content = "";
		
		/* Read line by line */
		int line_num = 0;
		while (OneLine.hasNext()) {
			line_num++;
			if(line_num%10000 == 0) System.out.println(line_num);
			Scanner TokenInLine = new Scanner(OneLine.next());
			while(TokenInLine.hasNext()) {
				String temp = TokenInLine.next();
//				System.out.println(temp);
				content = content + temp;
			}
		} // end of while (oneline.hasNext())
		
		System.out.println("finish reading");
		JSONObject json = new JSONObject(content);
		JSONObject routes = json.getJSONObject("ROUTE_TABLE*").getJSONObject("value");
		
		
		LinkedList<Interface> vlanforECMP = new LinkedList<Interface>();
		
		for(String ip_prefix : routes.keySet()) {
			String out_interface = routes.getJSONObject(ip_prefix).getString("ifname");
			String next_hop = routes.getJSONObject(ip_prefix).getString("nexthop");
			
			// handle the ECMP route
			String[] ifnames = out_interface.split(",");
			if(ifnames.length > 1) {
				// create new VLAN if there is none
				// notice that vlan's use is already handled
				Interface newvlan = null;
				for(Interface oldvlan : vlanforECMP) {
					if(oldvlan.access_vlans.size() != ifnames.length) continue;
					int i;
					for(i=0;i<ifnames.length;i++) {
						if(oldvlan.access_vlans.contains(ifnames[i]) == false) break;
					}
					if(i == ifnames.length) {
						newvlan = oldvlan;
						break;
					}
				}
				if(newvlan == null) {
					newvlan = new Interface();
					newvlan.interface_name = "VlanECMP" + vlanforECMP.size();
					for(int i=0;i<ifnames.length;i++) {
						newvlan.access_vlans.add(ifnames[i]);
					}
					vlanforECMP.add(newvlan);
				}
				
				out_interface = newvlan.interface_name;
				next_hop = "";
			}
			
			// handle the vlan interface
			if(out_interface.startsWith("Vlan")) {
				if(next_hop.equals("") == false) {
					Interface true_interface = device.interfaces_with_ip.get(next_hop);
					if(true_interface != null) {
						out_interface = true_interface.interface_name;
						System.out.println(out_interface+" "+next_hop);
					}
				}
			}
			FWRule fw = new FWRule(ip_prefix,next_hop,out_interface);
			
			device.fw_rules.add(fw);
		}
		
		// add the new List to the old interface list
		
//		for(Interface newvlan : vlanforECMP) {
//			for(Interface oldinterface : device.interfaces.values()) {
//				if(oldinterface.access_vlans.size() != newvlan.access_vlans.size()) continue;
//				int i;
//				for(i=0;i<newvlan.access_vlans.size();i++) {
//					String port_name = newvlan.access_vlans.get(i);
//					if(oldinterface.access_vlans.contains(port_name) == false) break;
//				}
//				if(i == newvlan.access_vlans.size()) break;
//				
//			}
//		}
		
		for(Interface newvlan : vlanforECMP) {
			for(String port_name : newvlan.access_vlans) {
				Interface mem_interface = device.interfaces.get(port_name);
				if(mem_interface == null) {
					System.err.println("no interface named "+port_name);
				}
				else {
					if(mem_interface.switch_port_mode != null) {
						System.err.println("interface "+mem_interface.interface_name+" had a switch mode " + 
								mem_interface.switch_port_mode+"!");
					}
					else{
						mem_interface.switch_port_mode = "access";
					}
					mem_interface.access_vlans.add(newvlan.interface_name);
					System.out.println(mem_interface.interface_name+":\n"+
							mem_interface.access_vlans.toString());
//					device.interfaces.put(mem_interface.interface_name, mem_interface);
				}
			}
			device.interfaces.put(newvlan.interface_name, newvlan);
		}
	}

	@Override
	public void ParseArps(DeviceConfig router, String inpath) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ParseMacs(DeviceConfig router, String inpath) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
