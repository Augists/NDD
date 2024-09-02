package apkeep.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import org.json.*;

import apkeep.parser.entity.DeviceConfig;
import apkeep.parser.entity.FWRule;
import apkeep.parser.entity.Interface;
import common.ACLParser;
import common.ACLParser.ACLType;
import common.ACLRule;
import common.ACLUse;

public class CiscoParser extends DeviceParser {

	public CiscoParser() {
		// TODO Auto-generated constructor stub
		super();
	}

	@Override
	public void ParseConfig (DeviceConfig device, String config_file_name) throws IOException
	{
		Scanner OneLine = null;
		try {
			File route_file = new File(config_file_name);
			OneLine = new Scanner (route_file);
			OneLine.useDelimiter("\n");
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!" + config_file_name);
			System.exit (0);
		}
		
		/* Read line by line */
		while (OneLine.hasNext()) {
			/* Read token by token in each line */
			Scanner TokenInLine = new Scanner(OneLine.next());
			String keyword;
			if (TokenInLine.hasNext()) {
				keyword = TokenInLine.next();
				/******************************************************************
				 * This section handles the interface table creation
				 ******************************************************************/
				if (keyword.equals("interface")) {
					/* Create a new interface object */
					Interface routerInterface = new Interface();
					/* Get the interface name */
					routerInterface.interface_name = TokenInLine.next();
					if(TokenInLine.hasNext()) {
						routerInterface.interface_name = routerInterface.interface_name + "-" +
								TokenInLine.next();
					}

					HandleInterface(OneLine, routerInterface, device);
					continue;
				}
				/**************************************************************
				 * This section handles ACL rules that start with "access-list"
				 ***************************************************************/
				if (keyword.equals("access-list")) {
					HandleAccessList(OneLine, TokenInLine,device);
				}
				/* handles acl rules that start with ip access-list extend/standard*/
				else if(keyword.equals("ip")) {
					keyword = TokenInLine.next();
					if(keyword.equals("access-list")) {
						HandleAccessListGrouped(OneLine, TokenInLine,device);
					}
				}
			}
		} // end of while (oneline.hasNext())
	}
	
	@Override
	public void ParseRoutes (DeviceConfig router,String route_file_name) {
		Scanner OneLine = null;
		try {
			File route_file = new File(route_file_name);
			OneLine = new Scanner (route_file);
			OneLine.useDelimiter("\n");
			//scanner.useDelimiter(System.getProperty("line.separator"));
			// doesn't work for .conf files
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!" + route_file_name); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		/* Read line by line */
		while (OneLine.hasNext()) {
			Scanner TokenInLine = new Scanner(OneLine.next());
			if (TokenInLine.hasNext()) {
				String prefix = TokenInLine.next();
				if(ParseTools.isIpPrefix(prefix) == false) continue;
				
//				prefix = ParseTools.dotted_prefix_to_int(prefix);
				
				String nexthop = TokenInLine.next();
				String interfaceName = "";
				
				if (TokenInLine.hasNext()) {
					interfaceName = TokenInLine.next();
				}
//				else {
//					interfaceName = "self";
//				}
//				
//				if(interfaceName.toLowerCase().startsWith("loopback") || 
//						interfaceName.toLowerCase().startsWith("null") ||
//						nexthop.toLowerCase().startsWith("drop")) {
//					interfaceName = "self";
//				}
				
				FWRule fw = new FWRule(prefix,nexthop,interfaceName);
				
				router.fw_rules.add(fw);
			}
		}
	}
	
	public void HandleInterface(Scanner OneLine, Interface router_interface, DeviceConfig router) throws IOException
	{
		while(OneLine.hasNext()){

			Scanner TokenInLine = new Scanner(OneLine.next());
			ArrayList<String> argument = new ArrayList<String>();
			ParseTools.GetArgument(TokenInLine, argument);

			/* End of interface section is denoted by ! */
			if (argument.get(0).equals("!")) {
				/****************************************************
				 * Store interface objects in Router Config
				 ***************************************************/
				/* Store object in table with name as the key */
				if (router_interface.interface_name !=null) {
					/* there may be interface that didn't specify ip address
					 * specify here "no ip address"
					 * to prevent a null pointer error ***/
					if (router_interface.ip_prefix == null) {
						router_interface.ip_prefix = "no ip address";
					}
					router.interfaces.put(router_interface.interface_name, router_interface);
				}
				/* Store object in table with ip as the key
				 * If no ip, object will not be stored */
				if (router_interface.ip_prefix != null &&
						!router_interface.ip_prefix.equals("no ip address") )
					router.interfaces_with_ip.put(router_interface.ip_prefix, router_interface);
				//finish parsing the interface section
				break;
			}

			/* Handle lines "ip address address prefix" */
			if(argument.get(0).equals("ip") && argument.get(1).equals("address") &&
					(! argument.get(2).equals("dhcp")) && (! argument.get(2).equals("negotiated")))
			{
				String ipAddrStr = argument.get(2);
				int slashInd = ipAddrStr.indexOf('/');
				if(slashInd >=0)
				{
					String mask = ParseTools.SeperateMask(ipAddrStr, slashInd);
					router_interface.ip_prefix = ipAddrStr.substring(0, slashInd);
					router_interface.ip_mask = mask;
				}else{
					router_interface.ip_prefix = argument.get(2);
					router_interface.ip_mask = argument.get(3);
				}
				continue;
			}
			
			/*find layer2 neighbors. consider 'description'  */		
			if(argument.get(0).equalsIgnoreCase("description"))
			{
				continue;
			}
			
			/* Handle lines "ip access-group aclnum in/out" */
			if(argument.get(0).equals("ip") && argument.get(1).equals("access-group"))
			{
				if(argument.get(3).equals("in"))
				{
					router_interface.in_acls.add(argument.get(2));
				}else 
					// assume the other case is 'out'
				{
					router_interface.out_acls.add(argument.get(2));
				}
				continue;
			}

			/* Handles interfaces with "no ip address" */
			if (argument.get(0).equals("no") && argument.get(1).equals("ip") && 
					argument.get(2).equals("address")) {
				router_interface.ip_prefix = "no ip address";
				continue;
			} 
			/* Handles lines "switchport mode mode-type" */
			if(argument.get(0).equals("switchport") && argument.size() == 3 && argument.get(1).equals("mode"))
			{
				router_interface.switch_port_mode = argument.get(2);
				continue;
			}
			
			/* Handles lines "switchport access vlan vlannum" */
			if(argument.get(0).equals("switchport") && argument.size() == 4 && argument.get(1).equals("access") && argument.get(2).equals("vlan"))
			{
				String vlan = "Vlan"+argument.get(3);
				router_interface.access_vlans.add(vlan);
				continue;
			}
			/* Handles lines "switchport trunk allowed vlan vlan-list" */
			if(argument.get(0).equals("switchport") && argument.size() > 4 && argument.get(1).equals("trunk") && argument.get(2).equals("allowed") && argument.get(3).equals("vlan"))
			{
				int pos = 4;
				if(argument.get(4).equals("add")) pos++;
				String[] vlanlist = argument.get(pos).split(",");
				for(int i = 0; i<vlanlist.length;i++) {
					String vlanrange = vlanlist[i];
					String[] vlans = vlanrange.split("-");
					if(vlans.length == 1) {
						String vlan = "Vlan"+vlans[0];
						router_interface.trunk_vlans.add(vlan);
					}
					else if(vlans.length == 2) {
						int lower = Integer.parseInt(vlans[0]);
						int upper = Integer.parseInt(vlans[1]);
						for(int j = lower;j<= upper;j++) {
							String vlan = "Vlan"+String.valueOf(j);
							router_interface.trunk_vlans.add(vlan);
						}
					}
				}
				continue;
			}

			/* if the interface is shutdown, quit parsing */
			
			/* if the interface is shutdown, mark the power status */
			if(argument.get(0).equals("shutdown"))
			{
				router_interface.power = "shutdown";
//				break;
			}
		}

	}
	
	public void HandleAccessList(Scanner oneline, Scanner tokeninline, DeviceConfig router) throws IOException
	{
		ArrayList<String> argument = new ArrayList<String>();
		/**set up argument, 'access-list' has already been parsed*/
		ACLParser.GetArgument(tokeninline, argument);

		int currentACLNum = -1;
		int preACLNum = -1;
		int[] aclNumbers = {preACLNum, currentACLNum};// pay attention to the order

		ACLRule onerule = null;
		LinkedList<ACLRule> oneacl = new LinkedList<ACLRule>();

		if(ACLParser.CheckValidACL(argument, aclNumbers))
		{
			onerule = new ACLRule();
			onerule.accessList = "access-list";

			preACLNum = aclNumbers[0];
			currentACLNum = aclNumbers[1];
			onerule.accessListNumber = Integer.toString(currentACLNum);
			//
			ACLParser.AddDynamic(onerule, argument);
			ACLParser.CheckPermitDeny(onerule, argument);

			if(ACLParser.CheckACLType(currentACLNum) == ACLType.standard)
			{
				ACLParser.HandleACLRuleStandard(onerule, argument);
			}else
			{
				ACLParser.HandleACLRuleExtend(onerule, argument);
			}
			oneacl.add(onerule);
		}


		while(oneline.hasNext())
		{
			String keyword = "";
			tokeninline = new Scanner(oneline.next());
			if(tokeninline.hasNext()){
				keyword = tokeninline.next();
			}else{
				// seems reach the end of file, finish parsing
				break;
			}
			if(keyword.equals("access-list"))
			{
				aclNumbers[0] = preACLNum; 
				aclNumbers[1] = currentACLNum;
				ACLParser.GetArgument(tokeninline, argument);

				if(ACLParser.CheckValidACL(argument, aclNumbers))
				{
					preACLNum = aclNumbers[0];
					currentACLNum = aclNumbers[1];
					if(preACLNum != currentACLNum)
					{
						// finish parsing an acl, add to the router
						router.acls.put(Integer.toString(preACLNum), oneacl);

						//debug
						ArrayList<String> oneaclInfo = new ArrayList<String>();
						oneaclInfo.add(oneacl.get(0).accessListNumber);
						oneaclInfo.add(Integer.toString(oneacl.size()));
						// get a new one to store
						oneacl = new LinkedList<ACLRule>();
						preACLNum = currentACLNum;
					}

					onerule = new ACLRule();
					onerule.accessList = "access-list";

					onerule.accessListNumber = Integer.toString(currentACLNum);
					//
					ACLParser.AddDynamic(onerule, argument);
					ACLParser.CheckPermitDeny(onerule, argument);

					if(ACLParser.CheckACLType(currentACLNum) == ACLType.standard)
					{
						ACLParser.HandleACLRuleStandard(onerule, argument);
					}else
					{
						ACLParser.HandleACLRuleExtend(onerule, argument);
					}
					//debug
					//DebugTools.IntermediateACLRuleCheck(onerule, System.out);
					oneacl.add(onerule);
				}

			}else{
				// the acl part ends
				break;
			}
		}

		// need to add the last acl
		if(currentACLNum != -1){
			//debug
			ArrayList<String> oneaclInfo = new ArrayList<String>();
			oneaclInfo.add(oneacl.get(0).accessListNumber);
			oneaclInfo.add(Integer.toString(oneacl.size()));
			router.acls.put(Integer.toString(currentACLNum), oneacl);
		}
	}
	
	public void HandleAccessListGrouped(Scanner oneline, Scanner tokeninline, DeviceConfig router)throws IOException
	{
		ArrayList<String> argument = new ArrayList<String> ();
		ACLParser.GetArgument(tokeninline, argument);
		while(true){
			//"access-list" already removed		
			LinkedList<ACLRule> oneacl = new LinkedList<ACLRule>();
			
			ACLType thisType;
			if(argument.get(0).equals("extended")){
				thisType = ACLType.extend;
			}else{
				thisType = ACLType.standard;
			}
			String thisNumber = argument.get(1);

			while(true){
				tokeninline = new Scanner(oneline.next());
				ACLParser.GetArgument(tokeninline, argument);
				if(argument.get(0).equals("ip")){
					//a new ACL, need to add the old one 
					router.acls.put(thisNumber, oneacl);
					argument.remove(0);// remove ip
					argument.remove(0);// remove access-list, so that it can restart
					
					break;
				}
				if((!argument.get(0).equals("permit")) && (!argument.get(0).equals("deny"))){
					//this means the end of the ACL definition
					router.acls.put(thisNumber, oneacl);
					return;
				}
				ACLRule onerule = new ACLRule();
				onerule.accessList = "access-list";
				onerule.accessListNumber = thisNumber;
				ACLParser.CheckPermitDeny(onerule, argument);
				if(thisType == ACLType.extend){
					ACLParser.HandleACLRuleExtend(onerule, argument);
				}else{
					ACLParser.HandleACLRuleStandard(onerule, argument);
				}
				oneacl.add(onerule);
			}
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
