package apkeep.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import apkeep.parser.entity.DeviceConfig;
import apkeep.parser.entity.FWRule;
import apkeep.parser.entity.MCSRule;
import common.ACLParser;
import common.ACLRule;
import common.ACLUse;
import common.PortProtocolParser;
import javafx.util.Pair;
import apkeep.parser.entity.Interface;

public class DCNParser extends DeviceParser {

	@Override
	public void ParseConfig(DeviceConfig router, String inpath) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(inpath));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + inpath); // for debugging
			System.exit (0); // Stop program if no file found
		}
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			
			String keyword = tokens[0];
			switch(keyword){
			case "#":
				// do nothing
				break;
			case "interface":
				/* Create a new interface object */
				Interface routerInterface = new Interface();
				/* Get the interface name */
				routerInterface.interface_name = tokens[1];
				if(routerInterface.interface_name.equals("Eth-Trunk")) {
					routerInterface.interface_name = routerInterface.interface_name + tokens[2];
				}
				HandleInterface(fileBufferReader, routerInterface, router);
				break;
			case "traffic-segment":
				if(tokens[1].equals("segment-id")) {
					String epgid = tokens[2];
					HandleEPGRules(epgid, fileBufferReader, router);
				}
				break;
			case "segment":
				String name = tokens[2];
				if(tokens[1].equals("classifier")) {
					HandleSegmentClassifier(name, fileBufferReader, router);
				}
				else if(tokens[1].equals("behavior")) {
					HandleSegmentBehavior(name, fileBufferReader, router);
				}
				else if(tokens[1].equals("policy")) {
					HandleTrafficPolicy(name, fileBufferReader, router);
				}
				break;
			case "ip":
				if(tokens[1].equals("vpn-instance")) {
					String vpn_name = tokens[2];
					HandleVPN(vpn_name, fileBufferReader, router);
				}
				else if(tokens[1].equals("route-static")) {
//					HandleStaticRoute(tokens,fileBufferReader, router);
				}
				break;
			case "bridge-domain":
				String bd_name = tokens[1];
				HandleBD(bd_name,fileBufferReader, router);
				break;
			case "acl":
				String acl_name = tokens[2];
				HandleAccessList(acl_name,fileBufferReader,router);
				break;
			case "traffic":
				String traffic_name = tokens[2];
				if(tokens[1].equals("classifier")) {
					HandleTrafficClassifier(traffic_name, fileBufferReader, router);
				}
				else if(tokens[1].equals("behavior")) {
					HandleTrafficBehavior(traffic_name, fileBufferReader, router);
				}
				else if(tokens[1].equals("policy")) {
					HandleTrafficPolicy(traffic_name, fileBufferReader, router);
				}
				break;
			case "traffic-policy":
				HandleTrafficUsage(tokens,fileBufferReader, router);
				break;
			default:
				// do nothing
				break;
			}
		}
	}

	private void HandleTrafficUsage(String[] firstLine, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		String tp_name = firstLine[1];
		String direction = firstLine[3];
		String location = firstLine[2];
		if(direction.equals("inbound")) direction = "in";
		else direction = "out";
		ACLUse aclu = new ACLUse(tp_name,location,direction);
		if(!router.policy_usage.containsKey(tp_name)) {
			LinkedList<ACLUse> ll = new LinkedList<ACLUse>();
			ll.add(aclu);
			router.policy_usage.put(tp_name, ll);
		}
		else {
			router.policy_usage.get(tp_name).add(aclu);
		}
		
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			if(linestr.length() == 0) continue;
			String[] tokens = linestr.split(" ");
			if(tokens[0].equals("#")) break;
			
			tp_name = firstLine[1];
			direction = firstLine[3];
			location = firstLine[2];
			if(direction.equals("inbound")) direction = "in";
			else direction = "out";
			aclu = new ACLUse(tp_name,location,direction);
			if(!router.policy_usage.containsKey(tp_name)) {
				LinkedList<ACLUse> ll = new LinkedList<ACLUse>();
				ll.add(aclu);
				router.policy_usage.put(tp_name, ll);
			}
			else {
				router.policy_usage.get(tp_name).add(aclu);
			}
		}
	}

	private void HandleAccessList(String acl_name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		LinkedList<ACLRule> oneacl = new LinkedList<ACLRule>();
		ArrayList<String> argument = new ArrayList<String> ();
		
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			getArgument(linestr,argument);
			if(argument.size() == 0) continue;
			if(argument.get(0).equals("#")) break;

			ACLRule onerule = new ACLRule();
			onerule.accessList = "access-list";
			onerule.accessListNumber = acl_name;
			CheckRuleNumber(onerule, argument);
			ACLParser.CheckPermitDeny(onerule, argument);
			HandleACLRule(onerule, argument);
//			System.out.println(onerule);
			onerule.SetPriority(65535);
			oneacl.add(onerule);
		}
		router.acls.put(acl_name, oneacl);
	}

	private void getArgument(String linestr, ArrayList<String> argument) {
		// TODO Auto-generated method stub
		argument.clear();
		String[] tokens = linestr.split(" ");
		for(int i = 0; i<tokens.length;i++)
		{
			argument.add(tokens[i]);
		}
	}

	private void HandleACLRule(ACLRule onerule, ArrayList<String> argument) {
		// TODO Auto-generated method stub
		// then is the match field
		while(argument.isEmpty() == false) {
//			System.out.println(argument.toString());
			if(argument.get(0).equals("destination")) {
				argument.remove(0);
				onerule.destination = argument.get(0);
				argument.remove(0);
				onerule.destinationWildcard = argument.get(0);
				argument.remove(0);
			}
			else if(argument.get(0).equals("destination-port")) {
				argument.remove(0);
				PortProtocolParser.ParsePort (onerule, argument, "destination");
				argument.remove(0);
			}
			else if(argument.get(0).equals("source")) {
				argument.remove(0);
				onerule.source = argument.get(0);
				argument.remove(0);
				onerule.sourceWildcard = argument.get(0);
				argument.remove(0);
			}
			else if(argument.get(0).equals("source-port")) {
				argument.remove(0);
				PortProtocolParser.ParsePort (onerule, argument, "source");
			}
			else {
				// first is the protocol
				onerule.protocolLower = PortProtocolParser.GetProtocolNumber(argument.get(0));
				if (onerule.protocolLower.equals("256")) {
					onerule.protocolLower = "0";
					onerule.protocolUpper = "255";
				}else {
					onerule.protocolUpper = onerule.protocolLower;
				}
				argument.remove(0);
			}
		}
		
	}

	private void CheckRuleNumber(ACLRule onerule, ArrayList<String> argument) {
		// TODO Auto-generated method stub
		if(argument.get(0).equals("rule"))
		{
			argument.remove(0); 
			// use dynamic name for sequence number as Huawei case
			onerule.dynamicName = argument.get(0);
		}
		argument.remove(0); 
	}

	private void HandleTrafficClassifier(String traffic_name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		ArrayList<String> acls = new ArrayList<String>();
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			if(tokens[0].equals("#")) break;
			
			int index = 0;
			switch(tokens[index]) {
			case "if-match":
				index++;
				if(tokens[index].equals("acl")) {
					String rule = "acl "+tokens[index+1];
					acls.add(rule);
				}
				else if(tokens[index].equals("vxlan") && tokens[index+1].equals("vni")) {
					String rule = "vni "+tokens[index+2];
					acls.add(rule);
				}
				else if(tokens[index].equals("vxlan") && tokens[index+1].equals("acl")) {
					String rule = "acl "+tokens[index+2];
					acls.add(rule);
				}
				break;
			default:
				break;
			}
		}
		router.traffic_classifier.put(traffic_name, acls);
	}

	private void HandleTrafficBehavior(String traffic_name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		LinkedList<String> actions = new LinkedList<String>();
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");

			if(tokens[0].equals("#")) break;
			String action = linestr;
			actions.add(action);
		}
		router.traffic_behavior.put(traffic_name, actions);
	}

	private void HandleEPGRules(String epgid, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		LinkedList<MCSRule> rules = new LinkedList<MCSRule>();
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			if(tokens[0].equals("#")) break;
			
			int index = 0;
			switch(tokens[index]) {
			// one epg rule
			case "segment-member":
				MCSRule rule = new MCSRule();
				String ipstring = null;
				String mask = null;
				String vpn = null;
				index++;
				while(index < tokens.length) {
					switch(tokens[index]) {
					case "ip":
						ipstring = tokens[++index];
						mask = tokens[++index];
						index++;
						break;
					case "vpn-instance":
						vpn = tokens[++index];
						index++;
						break;
					default:
						index++;
						break;
					}
				}
				if(ipstring != null && mask != null && vpn !=null) {
					rule.setEPGRule(ipstring, mask, vpn);
					rules.add(rule);
				}
				break;
			//do nothing
			default:
				break;
			}
		}
		router.epgs.put(epgid, rules);
	}
	private void HandleSegmentClassifier(String name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		LinkedList<MCSRule> rules = new LinkedList<MCSRule>();
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			if(tokens[0].equals("#")) break;
			
			int index = 0;
			switch(tokens[index]) {
			// one epg rule
			case "rule":
				index++;
				MCSRule rule = new MCSRule();
				String action = tokens[index++];
				String src_epg = null;
				String dst_epg = null;
				while(index < tokens.length) {
					switch(tokens[index]) {
					case "source-segment":
						src_epg = tokens[++index];
						index++;
						break;
					case "destination-segment":
						dst_epg = tokens[++index];
						index++;
						break;
					default:
						index++;
						break;
					}
				}
				if(src_epg != null && dst_epg !=null) {
					rule.setClassifierRule(action,src_epg,dst_epg);
					rules.add(rule);
				}
				break;
			//do nothing
			default:
				break;
			}
		}
		router.segment_classifier.put(name, rules);
	}

	private void HandleSegmentBehavior(String name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		LinkedList<String> actions = new LinkedList<String>();
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");

			if(tokens[0].equals("#")) break;
			String action = tokens[0];
			actions.add(action);
		}
		router.traffic_behavior.put(name, actions);
	}

	private void HandleTrafficPolicy(String name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		ArrayList<Pair<String,String>> policys = new ArrayList<Pair<String,String>>();
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");

			if(tokens[0].equals("#")) break;
			
			int index = 0;
			switch(tokens[index]) {
			// one epg rule
			case "classifier":
				index++;
				String classifier_name = tokens[index++];
				String behavior_name = null;
				while(index < tokens.length) {
					switch(tokens[index]) {
					case "behavior":
						behavior_name = tokens[++index];
						index++;
						break;
					default:
						index++;
						break;
					}
				}
				if(classifier_name != null && behavior_name != null) {
					Pair<String,String> policy = new Pair<String,String>(classifier_name,behavior_name);
					policys.add(policy);	
				}
				break;
			//do nothing
			default:
				break;
			}			
		}
		router.traffic_policy.put(name, policys);
	}

	private void HandleVPN(String vpn_name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			
			String keyword = tokens[0];
			if(keyword.equals("#")) break;
			if(tokens[0].equals("vxlan") && tokens[1].equals("vni")) {
				router.addBD(vpn_name,tokens[2]);
			}
		}
	}

	private void HandleBD(String name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		name = "bd"+name;
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			
			String keyword = tokens[0];
			if(keyword.equals("#")) break;
			if(tokens[0].equals("vxlan") && tokens[1].equals("vni")) {
				router.addBD(name,tokens[2]);
			}
		}
	}
	
	private void HandleStaticRoute(String[] firstLine, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		String location = firstLine[3];
		String ipAddress = firstLine[4];
		String mask = firstLine[5];
		String outInterface = firstLine[7];
		FWRule firstRule = new FWRule(ipAddress,mask,outInterface);
		HashSet<FWRule> rules = router.static_routes.get(location);
		if(rules == null) rules = new HashSet<FWRule>();
		rules.add(firstRule);
		router.static_routes.put(location,rules);
		
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split(" ");
			if(tokens[0].equals("#")) break;
			
			location = tokens[3];
			ipAddress = tokens[4];
			mask = tokens[5];
			outInterface = tokens[7];
			FWRule one_rule = new FWRule(ipAddress,mask,outInterface);
			rules = router.static_routes.get(location);
			if(rules == null) rules = new HashSet<FWRule>();
			rules.add(one_rule);
			router.static_routes.put(location,rules);
		}
	}

	private void HandleInterface(BufferedReader fileBufferReader, Interface routerInterface, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		//different action due to interface name
		routerInterface.interface_name = ParseTools.get_ethernet_port_name(routerInterface.interface_name);
		String name = routerInterface.interface_name;
		String OneLine;
		if(name.startsWith("vbdif")) {
			String bd_id = "bd" + name.substring(5);
			while((OneLine = fileBufferReader.readLine()) != null) {
				String linestr = OneLine.trim();
				String[] tokens = linestr.split(" ");
				
				if(tokens[0].equals("#")) break;
				if(tokens[0].equals("ip") && tokens[1].equals("binding")) {
					String vpn_id = tokens[3];
					router.bd_vrfs.put(bd_id,vpn_id);
					HashSet<String> bds = router.vrf_bds.get(vpn_id);
					if(bds == null) bds = new HashSet<String>();
					bds.add(bd_id);
					router.vrf_bds.put(vpn_id,bds);
				}
				else if(tokens[0].equals("ip") && tokens[1].equals("address")) {
					routerInterface.ip_prefix = tokens[2];
					routerInterface.ip_mask = tokens[3];
				}
				else if(tokens[0].equals("traffic-policy")) {
					String policy_name = tokens[1];
					String direction = tokens[2];
					if(direction.equals("inbound")) {
						routerInterface.in_policies.add(policy_name);
					}
					else {
						routerInterface.out_policies.add(policy_name);
					}
				}
			}
		}
		else if(name.startsWith("eth-trunk")) {
			//do nothing
		}
		else if(name.startsWith("nve")) {
			while((OneLine = fileBufferReader.readLine()) != null) {
				String linestr = OneLine.trim();
				String[] tokens = linestr.split(" ");
				
				String keyword = tokens[0];
				if(keyword.equals("#")) break;
				if(tokens[0].equals("source")) {
					routerInterface.ip_prefix = tokens[1];
					routerInterface.ip_mask = "255.255.255.255";
				}
			}
		}
		else {
			while((OneLine = fileBufferReader.readLine()) != null) {
				String linestr = OneLine.trim();
				String[] tokens = linestr.split(" ");
				
				if(tokens[0].equals("#")) break;
				else if(tokens[0].equals("ip") && tokens[1].equals("address")) {
					routerInterface.ip_prefix = tokens[2];
					routerInterface.ip_mask = tokens[3];
				}
				else if(tokens[0].equals("eth-trunk")) {
					routerInterface.eth_trunk = "eth-trunk"+tokens[1];
				}
				else if(tokens[0].equals("traffic-policy")) {
					String policy_name = tokens[1];
					String direction = tokens[2];
					if(direction.equals("inbound")) {
						routerInterface.in_policies.add(policy_name);
					}
					else {
						routerInterface.out_policies.add(policy_name);
					}
				}
			}
		}
		if (routerInterface.interface_name !=null) {
			if (routerInterface.ip_prefix == null) {
				routerInterface.ip_prefix = "no ip address";
			}
			router.interfaces.put(routerInterface.interface_name, routerInterface);
		}
		if (routerInterface.ip_prefix != null &&
				!routerInterface.ip_prefix.equals("no ip address") )
			router.interfaces_with_ip.put(routerInterface.ip_prefix, routerInterface);
	}
	

	@Override
	public void ParseRoutes(DeviceConfig router, String route_file_name) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(route_file_name));
		} catch (FileNotFoundException e) {
			System.out.println ("File not found! " + route_file_name); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split("\\s");
			
			if(tokens.length == 0) continue;
			
			String keyword = tokens[0];
			if(keyword.startsWith("-")) {
				continue;
			}
			if(keyword.equals("Routing")) {
				String table_name = tokens[3];
				HandleRoutes(table_name,fileBufferReader,router);
			}
		}
	}
	public void ParseRoutesJson(DeviceConfig router, JSONObject json) {
		// TODO Auto-generated method stub
		JSONArray public_routes = json.getJSONArray("_public_");
		for(int i=0;i<public_routes.length();i++){
			JSONObject oneRoute = public_routes.getJSONObject(i);
			String ipPrefix = oneRoute.getString("Destination/Mask");
			String nexthop = oneRoute.getString("NextHopIp");
			String outinterface = oneRoute.getString("Interface");
			outinterface = ParseTools.get_ethernet_port_name(outinterface);
			if(oneRoute.isNull("NextNode") && !outinterface.startsWith("vlanif")) {
				continue;
			}
			FWRule r = new FWRule(router.device_name,ipPrefix,nexthop,outinterface);
//			System.out.println(tokens.toString());
//			System.out.println(r.toString());
			router.fw_rules.add(r);
		}
	}

	private void HandleRoutes(String table_name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split("\\s");
			
			if(tokens.length == 0) continue;
			
			String keyword = tokens[0];
			if(keyword.startsWith("-")) {
				break;
			}
			if(keyword.equals("Destination/Mask")) {
				HandleOneRouteTable(table_name, fileBufferReader,router);
				break;
			}
		}
		
	}

	private void HandleOneRouteTable(String table_name, BufferedReader fileBufferReader, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		String device_name;
		if(table_name.equals("_public_")) {
			device_name = router.device_name;
		}
		else {
			device_name = router.device_name+"_"+table_name;
		}
		String OneLine;
		String prefix = "";
		while((OneLine = fileBufferReader.readLine()) != null) {
			String linestr = OneLine.trim();
			String[] tokens = linestr.split("\\s+");
			
			String first = tokens[0];
			if(first.startsWith("-")) {
				return;
			}
			int index = 5;
			if(ParseTools.isIpPrefix(first)) {
				prefix = first;
			}
			else {
				index--;
			}
//			System.out.println(linestr);
			String nexthop = tokens[index];
			String outinterface = tokens[index+1];
			outinterface = ParseTools.get_ethernet_port_name(outinterface);
			FWRule r = new FWRule(device_name,prefix,nexthop,outinterface);
//			System.out.println(tokens.toString());
//			System.out.println(r.toString());
			router.fw_rules.add(r);
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
