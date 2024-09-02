package apkeep.FDN;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import NDD.NDD;
import apkeep.FDN.CheckerV2;
import apkeep.FDN.FDNNetwork;
import apkeep.FDN.FieldNode;
import apkeep.utils.ExperimentTools;
import common.ACLRule;
import jdd.bdd.BDD;
import jdd.bdd.NodeTable;

public class FDNNetworkV2 extends FDNNetwork {
    static int orderPlan = 1;
    protected CheckerV2 checkerV2;
    // protected Checker checkerV2;

    public FDNNetworkV2(String name, String mode, String postid, String dataset, boolean extend) throws Exception {
        super(name, mode, postid, dataset, true);

        NDDOrder(orderPlan);

        FieldNode.network = this;
        FieldNode.bdd = bdd_engine;
        NDD.bdd = bdd_engine.getBDD();
    }

    public FDNNetworkV2(String name, String mode, String postid, String dataset) throws Exception {
        super(name, mode, postid, dataset, true);

        NDDOrder(orderPlan);

        FieldNode.network = this;
        FieldNode.bdd = bdd_engine;
        NDD.bdd = bdd_engine.getBDD();

        double t1 = System.nanoTime();

        constructPPMFromFile(postid);

        double t2 = System.nanoTime();

        isBatch = true;
        updatePPMSnapshot_Increasement();
        affected_aps = NDD.NDDTrue;

        double t3 = System.nanoTime();
        // System.out.println("update model:" + (t3 - t2) / 1000000000);
        pw.println("update model:" + (t3 - t2) / 1000000000);
        pw.flush();

        // bdd_engine.getBDD().showStats();
        NDD.useCache = true;
        checkerV2 = new CheckerV2(this, new HashSet<String>(), new HashMap<>());
        // checkerV2 = new Checker(this, new HashSet<String>(), new HashMap<>());
        checkerV2.PropertyCheck();
        NDD.useCache = false;
        // System.out.println();
        // bdd_engine.getBDD().showStats();
        // for(String str : checkerV2.reach)
        // {
        //     if(str.split("->")[0].equals("core-0-0_vpn0"))
        //     System.out.println(str);
        // }
        pw.println("reach size:" + checkerV2.reach.size());
        pw.flush();

        double t4 = System.nanoTime();

        pw.println("property check:" + (t4 - t3) / 1000000000);
        pw.flush();
        pw.println("total:" + (t4 - t2) / 1000000000);
        pw.flush();

        FileWriter f = new FileWriter("/home/zcli/lzc/Field-Decision-Network/DCN_NDD/output/NDD/no_cache/allOrder",true);
        PrintWriter p = new PrintWriter(f);
        p.println(orderPlan+" "+ NDD.Time / 1000000000.0 + " " + NDD.getMKCount() + " " +NodeTable.mkCount+" "+checkerV2.reach.size());
        p.flush();
    }

    void NDDOrder(int orderPlan) throws IOException
    {
		ArrayList<String> orders = ExperimentTools.readFromFile("/home/zcli/lzc/Field-Decision-Network/apkatra-main/src/main/java/dataset/fieldOrder");
		String chosen = orders.get(orderPlan-1);
		String [] tokens = chosen.split(" ");
        int [] upperBound = new int [field_num];
        int upper = -1;
        for(int i=0;i<=5;i++)
        {
            if(tokens[i].equals("0"))
            {
                FDNNetwork.dst_ip_field = i;
                upper += 32;
                upperBound[i] = upper;
            }
            else if(tokens[i].equals("1"))
            {
                FDNNetwork.src_ip_field = i;
                upper += 32;
                upperBound[i] = upper;
            }
            else if(tokens[i].equals("2"))
            {
                FDNNetwork.vni_field = i;
                upper += 24;
                upperBound[i] = upper;
            }
            else if(tokens[i].equals("3"))
            {
                FDNNetwork.dst_epg_field = i;
                upper += 16;
                upperBound[i] = upper;
            }
            else if(tokens[i].equals("4"))
            {
                FDNNetwork.src_epg_field = i;
                upper += 16;
                upperBound[i] = upper;
            }
            else if(tokens[i].equals("5"))
            {
                FDNNetwork.outter_ip_field = i;
                upper += 32;
                upperBound[i] = upper;
            }
        }
        bdd_engine = new BDDACLWrapper(orderPlan);
        NDD.SetFieldNum(field_num);
        NDD.SetUpperBound(upperBound);
    }

    void addACLNode(String eName) {
        if (!FieldNodes.containsKey(eName)) {
            ae++;
            FieldNode e = new FieldNode(eName, this, -1, false, multi_filed, no_field);
            FieldNodes.put(e.name, e);
        }
    }

    void addPBRNode(String eName) {
        if (!FieldNodes.containsKey(eName)) {
            ae++;
            FieldNode e = new FieldNode(eName, this, 1, false, multi_filed, no_field);
            FieldNodes.put(e.name, e);
            addEndHostPBR(eName, "redirect");
        }
    }

    void addEndHostPBR(String device, String port) {
        String [] tokens = device.split("_");
        String vpn = tokens[1].substring(2);
        String fireWall = tokens[0]+"_"+vpn+"_"+"FireWall";
        endEndPoint.add(fireWall);
        AddOneWayLink(device, port, fireWall, "inport");
    }

    public void processPBRFile(String file) throws Exception {
        acl_rules.clear();
        ArrayList<String> rules = ExperimentTools.readFromFile(file);
        for (String rule : rules) {
            String[] tokens = rule.split(" ");
            String type = tokens[1];
            String policy_name = tokens[2];
            if (type.equals("policy")) {
                String device_name = policy_name.split("_")[0];
                String vpn = tokens[4];
                String vpn_name = device_name + "_" + vpn;
                addPBRNode(policy_name);
                insertOneWayLink(vpn_name + "_decapepg", "default", policy_name, "inport", "permit");
            } else if (type.equals("pbr")) {
                ACLRule r = new ACLRule(rule.substring(tokens[0].length() + tokens[1].length() + tokens[2].length() + 3,
                        rule.length() - tokens[tokens.length - 1].length() - 1), "redirect");
                addACLRule(policy_name, r);
            }
        }
    }

    public void updatePBRRuleBatch_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        for (String element : acl_rules.keySet()) {
            for (ACLRule rule : acl_rules.get(element)) {
                PBR_num++;
                FieldNode e = FieldNodes.get(element);
                int rule_bdd = bdd_engine.ConvertACLRule(rule);
                rule.insert_vals(rule_bdd, 0, 0);
                change_set = e.InsertACLRuleBDD(rule);
                // System.out.println(element+" "+change_set.toString());
                e.update_ACL(change_set);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(args[0]+" "+args[1]);
        orderPlan = Integer.parseInt(args[1]);

        fw = new FileWriter("/home/zcli/lzc/Field-Decision-Network/DCN_NDD/output/NDD/no_cache/" + args[0] + "_order" + orderPlan + ".txt",
                false);
        pw = new PrintWriter(fw);

        String mode = "normal";

        Runtime r = Runtime.getRuntime();
        r.gc();
        r.gc();
        long m1 = r.totalMemory() - r.freeMemory();

        FDNNetworkV2 n = new FDNNetworkV2(args[0], mode, "base", args[0]);

        Runtime r1 = Runtime.getRuntime();
        r1.gc();
        r1.gc();
        long m2 = r1.totalMemory() - r1.freeMemory();

        pw.println("memory:" + (m2 - m1) / (1024 * 1024) + "MB");
        pw.flush();
    }
}