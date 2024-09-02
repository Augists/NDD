package apkeep.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import org.json.JSONObject;

import apkeep.parser.entity.*;
import common.ACLParser;
import common.ACLRule;
import common.ACLUse;
import common.PortProtocolParser;
import javafx.util.Pair;

public class HuaweiParser extends DeviceParser {

	@Override
	public void ParseConfig(DeviceConfig router, String inpath) throws IOException {
		// TODO Auto-generated method stub
		
		Scanner OneLine = null;
		try {
			File inputFile = new File(inpath);
			OneLine = new Scanner (inputFile);
			OneLine.useDelimiter("\n");
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!" + inpath);
			System.exit (0);
		}
		
		/* Read line by line */
		while (OneLine.hasNext()) {
			/* Read token by token in each line */
			Scanner TokenInLine = new Scanner(OneLine.next());
			String keyword;
			if (TokenInLine.hasNext()) {
				keyword = TokenInLine.next();
//				System.out.println(keyword);
				if (keyword.startsWith("interface")) {
					/* Create a new interface object */
					Interface routerInterface = new Interface();
					/* Get the interface name */
					routerInterface.interface_name = TokenInLine.next();
					
					HandleInterface(OneLine, routerInterface, router);
				}
				else if (keyword.startsWith("ip")) {
					HandleRoute(OneLine, TokenInLine, router);
				}
				/* handle traffic classifier/policy/behavior commands */
				else if(keyword.startsWith("traffic")){
					HandleTraffic(OneLine, TokenInLine, router);
				}
				/* handle acl rules like acl number  acl-number */
				else if(keyword.startsWith("acl")){
					keyword = TokenInLine.next();
					if(keyword.equals("number")) {
						HandleAccessList(OneLine, TokenInLine, router);
					}
				}
				else continue;
			}
		}
	}

	public void HandleRoute(Scanner oneLine, Scanner tokenInLine, DeviceConfig router) {
		// TODO Auto-generated method stub
		ArrayList<String> argument = new ArrayList<String>();
		ParseTools.GetArgument(tokenInLine, argument);
		
		String ip_address = argument.get(1);
		String ip_mask = argument.get(2);
		String nexthop = argument.get(3);
		String interfaceName = "";
		
		String prefix = ParseTools.getSlashedPrefix(ip_address, ip_mask);
		FWRule fw = new FWRule(prefix,nexthop,interfaceName);
		router.fw_rules.add(fw);
		while(oneLine.hasNext()) {
			tokenInLine = new Scanner(oneLine.next());
			ParseTools.GetArgument(tokenInLine, argument);
			if(argument.size() == 0) continue;
			if(argument.get(0).equals("#")) return;
			
			ip_address = argument.get(2);
			ip_mask = argument.get(3);
			nexthop = argument.get(4);
			interfaceName = "";
			
			prefix = ParseTools.getSlashedPrefix(ip_address, ip_mask);
			fw = new FWRule(prefix,nexthop,interfaceName);
			router.fw_rules.add(fw);
		}
	}

	public void HandleInterface(Scanner OneLine, Interface routerInterface, DeviceConfig router)
			throws IOException {
		// TODO Auto-generated method stub
		
		while(OneLine.hasNext()){

			Scanner TokenInLine = new Scanner(OneLine.next());
			ArrayList<String> argument = new ArrayList<String>();
			ParseTools.GetArgument(TokenInLine, argument);

			/* End of interface section is denoted by # */
			if (argument.get(0).equals("#")) {
				if (routerInterface.interface_name !=null) {
					if (routerInterface.ip_prefix == null) {
						routerInterface.ip_prefix = "no ip address";
					}
					router.interfaces.put(routerInterface.interface_name, routerInterface);
				}
				if (routerInterface.ip_prefix != null &&
						!routerInterface.ip_prefix.equals("no ip address") )
					router.interfaces_with_ip.put(routerInterface.ip_prefix, routerInterface);
				break;
			}

			/* Handle lines "ip address address prefix" */
			if(argument.get(0).equals("ip") && argument.get(1).equals("address") &&
					(! argument.get(2).equals("dhcp")) && (! argument.get(2).equals("negotiated")))
			{
				String ipAddrStr = argument.get(2);
				String ipMaskStr = argument.get(3);
				// ignore the second or sub ip address
				if(argument.size()>4) continue;
				else{
					routerInterface.ip_prefix = ipAddrStr;
					routerInterface.ip_mask = ipMaskStr;
				}
				continue;
			}
			
			/* handle the traffic-policy command */
			if(argument.get(0).equals("traffic-policy")) {
				String policy_name = argument.get(1);
				String direction = argument.get(2);
				if(direction.equals("inbound")) {
					routerInterface.in_policies.add(policy_name);
				}
				else if(direction.equals("outbound")) {
					routerInterface.out_policies.add(policy_name);
				}
				continue;
			}
			
			/* handle the traffic-filter command */
			if(argument.get(0).equals("traffic-filter")) {
				String acl_name = argument.get(2);
				String direction = argument.get(3);
				if(direction.equals("inbound")) {
					routerInterface.in_acls.add(acl_name);
				}
				else if(direction.equals("outbound")) {
					routerInterface.out_acls.add(acl_name);
				}
				continue;
			}
			
			/* handle the eth-trunk command */
			if(argument.get(0).equals("eth-trunk")) {
				String eth_name = argument.get(1);
				routerInterface.eth_trunk = "Eth-Trunk"+eth_name;
				continue;
			}
			
			/* if the interface is shutdown, mark the power status */
			if(argument.get(0).equals("shutdown"))
			{
				routerInterface.power = "shutdown";
				break;
			}
			
			/* handle the port default vlan command */
			if(argument.get(0).equals("port") && argument.get(1).equals("default")){
				String vlan_name = argument.get(3);
				String vlan = "Vlanif" + vlan_name;
				routerInterface.access_vlans.add(vlan);
				routerInterface.switch_port_mode = "access";
				continue;
			}
		}
	}

	public void HandleTraffic(Scanner oneline, Scanner tokeninline, DeviceConfig router) throws IOException {
		ArrayList<String> argument = new ArrayList<String> ();
		ACLParser.GetArgument(tokeninline, argument);
		
		String trafficName = "";
		ArrayList<String> acls = new ArrayList<String>();
		if(argument.get(0).equals("classifier")) {
			argument.remove(0);
			trafficName = argument.get(0);
			
			String aclname = null;
			while(oneline.hasNext()) {
				tokeninline = new Scanner(oneline.next());
				ACLParser.GetArgument(tokeninline, argument);
				
				if(argument.size() == 0) continue;
				if(argument.get(0).equals("#")) {
					//a new ACL
					router.traffic_classifier.put(trafficName, acls);
					return;
				}
				if(argument.get(0).equals("if-match")) {
					argument.remove(0);
					if(argument.get(0).equals("acl")) {
						argument.remove(0);
						aclname = argument.get(0);
						acls.add(aclname);
					}
				}
			}
		}
		else if(argument.get(0).equals("behavior")) {
			argument.remove(0);
			trafficName = argument.get(0);
			
			LinkedList<String> action = new LinkedList<String>();
			while(oneline.hasNext()) {
				tokeninline = new Scanner(oneline.next());
				ACLParser.GetArgument(tokeninline, argument);
				
				if(argument.size() == 0) continue;
				if(argument.get(0).equals("#")) {
					//a new ACL
					router.traffic_behavior.put(trafficName, action);
					return;
				}
				String behavior = "";
				while(argument.isEmpty() == false) {
					behavior = behavior + argument.get(0)+" ";
					argument.remove(0);
				}
				action.add(behavior);
//				if(argument.get(0).equals("statistics")) {
////					continue;
//					action = argument.get(0);
//				}
//				else if(argument.get(0).equals("redirect")) {
//					argument.remove(0);
//					if(argument.get(0).equals("nexthop") || argument.get(0).equals("ip-nexthop")) {
//						argument.remove(0);
//						action = argument.get(0);
//					}
//				}
//				else if(argument.get(0).equals("deny")){
//					action = argument.get(0);
//				}
//				else if(argument.get(0).equals("permit")){
//					action = argument.get(0);
//				}
			}
		}
		else if(argument.get(0).equals("policy")) {
			argument.remove(0);
			trafficName = argument.get(0);
			
			ArrayList<Pair<String,String>> policy = new ArrayList<Pair<String,String>>();
			while(oneline.hasNext()) {
				tokeninline = new Scanner(oneline.next());
				ACLParser.GetArgument(tokeninline, argument);
				
				if(argument.size() == 0) continue;
				if(argument.get(0).equals("#")) {
					//a new ACL
					router.traffic_policy.put(trafficName, policy);
					return;
				}
				String classifier = null;
				String behavior = null;
				String precedence = null;
				while(argument.isEmpty() == false) {
					if(argument.get(0).equals("classifier")) {
						argument.remove(0);
						classifier = argument.get(0);
						argument.remove(0);
					}
					else if(argument.get(0).equals("behavior")) {
						argument.remove(0);
						behavior = argument.get(0);
						argument.remove(0);
					}
					else if(argument.get(0).equals("precedence")) {
						argument.remove(0);
						precedence = argument.get(0);
						argument.remove(0);
					}
				}
				Pair<String,String> onePolicy = new Pair<String,String>(classifier,behavior);
				policy.add(onePolicy);
			}
		}
	}

	public void HandleAccessList(Scanner oneline, Scanner tokeninline, DeviceConfig router) throws IOException {
		// TODO Auto-generated method stub
		ArrayList<String> argument = new ArrayList<String> ();
		ACLParser.GetArgument(tokeninline, argument);
		String thisNumber = argument.get(0);
		
		LinkedList<ACLRule> oneacl = new LinkedList<ACLRule>();
		
		while(oneline.hasNext()){	
			tokeninline = new Scanner(oneline.next());
			ACLParser.GetArgument(tokeninline, argument);
			
			if(argument.size() == 0) continue;
			if(argument.get(0).equals("#")) {
				//a new ACL
				router.acls.put(thisNumber, oneacl);
				return;
			}

			ACLRule onerule = new ACLRule();
			onerule.accessList = "access-list";
			onerule.accessListNumber = thisNumber;
			CheckRuleNumber(onerule, argument);
			ACLParser.CheckPermitDeny(onerule, argument);
			HandleACLRule(onerule, argument);
			oneacl.add(onerule);
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
		
		// maybe need to set any fields
	}
	
	@Override
	public void ParseRoutes(DeviceConfig router, String inpath) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ParseArps(DeviceConfig router, String inpath) throws IOException {
		// TODO Auto-generated method stub
		Scanner OneLine = null;
		try {
			File inputFile = new File(inpath);
			OneLine = new Scanner (inputFile);
			OneLine.useDelimiter("\n");
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!" + inpath);
			System.exit (0);
		}
		
		/* Read line by line */
		while (OneLine.hasNext()) {
			/* Read token by token in each line */
			Scanner TokenInLine = new Scanner(OneLine.next());
			String keyword;
			if (TokenInLine.hasNext()) {
				keyword = TokenInLine.next();
			}
		}
	}

	@Override
	public void ParseMacs(DeviceConfig router, String inpath) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
