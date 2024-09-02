package apkeep.FDN;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

import NDD.NDD;
// import apkeep.core.APKeeperNew;
import apkeep.core.ChangeItem;
import apkeep.utils.*;

import common.*;
import javafx.util.Pair;
import jdd.bdd.BDD;
import jdd.bdd.Permutation;

public class FDNNetworkGate extends FDNNetwork {

    protected CheckerGate checkerGate;
    static NDD GateIPNDD = NDD.NDDFalse;

    public FDNNetworkGate(String name, String mode, String postid, String dataset) throws Exception {
        super(name, mode, postid, dataset, true);

        int [] upperBound = new int [field_num];
        upperBound[0] = 31;
        upperBound[1] = 63;
        upperBound[2] = 87;
        upperBound[3] = 103;
        upperBound[4] = 119;
        upperBound[5] = 151;
        bdd_engine = new BDDACLWrapper();
        NDD.SetFieldNum(field_num);
        NDD.SetUpperBound(upperBound);

        FieldNode.network = this;
        FieldNode.bdd = bdd_engine;
        NDD.bdd = bdd_engine.getBDD();

        double t1 = System.nanoTime();

        constructPPMFromFile(postid);

        double t2 = System.nanoTime();

        isBatch = true;
        updatePPMSnapshot_Increasement();
        updateGate();
        affected_aps = NDD.NDDTrue;

        double t3 = System.nanoTime();
        // System.out.println("update model:" + (t3 - t2) / 1000000000);
        pw.println("update model:" + (t3 - t2) / 1000000000);
        pw.flush();

        // bdd_engine.getBDD().showStats();
        checkerGate = new CheckerGate(this, new HashSet<String>(), new HashMap<>());
        checkerGate.PropertyCheck();
        // System.out.println();
        // bdd_engine.getBDD().showStats();
        pw.println("reach size:" + checkerGate.reach.size());
        pw.flush();

        double t4 = System.nanoTime();

        pw.println("property check:" + (t4 - t3) / 1000000000);
        pw.flush();
        pw.println("total:" + (t4 - t2) / 1000000000);
        pw.flush();
    }

    public void constructPPMFromFile(String postid) throws Exception {
        String topoFile = parsedRoot + postid + "/topology";
        String fibFile = parsedRoot + postid + "/fibs";
        String vxlanFile = parsedRoot + postid + "/vxlan";
        String epgFile = parsedRoot + postid + "/epg";
        String gbpFile = parsedRoot + postid + "/gbp";
        String pbrFile = parsedRoot + postid + "/pbr";
        String parFile = parsedRoot + postid + "/parameter";

        processTopoFile(topoFile);
        processFibFile(fibFile);
        processVxlanFile(vxlanFile);
        processEpgFile(epgFile);
        processGbpFile(gbpFile);
        processPBRFile(pbrFile);
        processGateFile(parFile);
    }

    int VPCNum;
    public void processGateFile(String file) throws IOException {
        ArrayList<String> parameters = ExperimentTools.readFromFile(file);
        Long GateIP = Long.parseLong(parameters.get(0).split(" ")[1]);
        VPCNum = Integer.parseInt(parameters.get(1).split(" ")[1]);

        String device = "gate-0-0";
        for (int vpc = 0; vpc < VPCNum; vpc++) {
            String vpnid = "vpn"+Integer.toString(vpc);
            String vpn = device+"_"+vpnid;
            addForwardNode(vpn, dst_ip_field);
            startEndPoint.add(vpn);
            String port = "internet";
            String host = "out";
            endEndPoint.add(host);
            AddOneWayLink(vpn, port, host, "inport");

            addNATNode(vpn + "_encapvni", dst_ip_field, vni_field);
            addNATNode(vpn + "_decapvni", no_field, vni_field);

            addNATNode(vpn + "_encapsrcepg", src_ip_field, src_epg_field);
            addNATNode(vpn + "_encapdstepg", dst_ip_field, dst_epg_field);
            addNATNode(vpn + "_decapepg", no_field, multi_filed);
            AddOneWayLink(device + "_vc", vpnid, vpn + "_decapvni", "inport");
            AddOneWayLink(vpn + "_decapvni", "default", vpn + "_encapdstepg", "inport");
            AddOneWayLink(vpn + "_encapdstepg", "default", vpn + "_decapepg", "inport");
            AddOneWayLink(vpn + "_decapepg", "default", vpn, "inport");

            AddOneWayLink(vpn, "vxlan", vpn + "_encapsrcepg", "inport");
            AddOneWayLink(vpn + "_encapsrcepg", "default", vpn + "_encapvni", "inport");

            AddOneWayLink(vpn + "_encapvni", "default", device + "_encapip", "inport");
        }

        for(String str : FieldNodes.keySet())
        {
            if(str.split("_").length != 2 || str.contains("vc") || str.contains("gate") || str.contains("encapip") || str.contains("decapip"))continue;
            AddOneWayLink(str, "default", str + "_encapsrcepg", "inport");
        }

        int GateIPBDD = bdd_engine.encodeDstIPPrefix_outter(GateIP, 32);
        GateIPNDD = NDD.table.ref(NDD.toNDD(GateIPBDD));
    }

    public void updateGate()
    {
        FieldNode Gate = FieldNodes.get("gate-0-0");
        Gate.ports.add("vxlan");
        Gate.ports_pred.put("vxlan", NDD.table.ref(GateIPNDD));
        NDD oldDefault = Gate.ports_pred.get("default");
        Gate.ports_pred.put("default", NDD.table.ref(NDD.Diff(oldDefault, GateIPNDD)));
        NDD.table.deref(oldDefault);

        FieldNode GateVC = FieldNodes.get("gate-0-0_vc");
        NDD sum = NDD.NDDFalse;
        for (int vpc = 0; vpc < VPCNum; vpc++) {
            String vpnid = "vpn"+Integer.toString(vpc);
            int vni = 50000 + vpc;
            int vnibdd = bdd_engine.encodeVNI(String.valueOf(vni));
            NDD vnindd = NDD.table.ref(NDD.toNDD(vnibdd));
            GateVC.ports.add(vpnid);
            GateVC.ports_pred.put(vpnid, vnindd);
            NDD oldSum = sum;
            sum = NDD.table.ref(NDD.OR(sum, vnindd));
            NDD.table.deref(oldSum);
        }
        oldDefault = GateVC.ports_pred.get("default");
        GateVC.ports_pred.put("default", NDD.table.ref(NDD.Diff(oldDefault, sum)));
        NDD.table.deref(oldDefault);
        NDD.table.deref(sum);

        String device = "gate-0-0";
        for (int vpc = 0; vpc < VPCNum; vpc++) {
            String vpnid = "vpn"+Integer.toString(vpc);
            String vpn = device+"_"+vpnid;
            FieldNode gateVPC = FieldNodes.get(vpn);
            for(String dName : FieldNodes.keySet())
            {
                if(dName.split("_").length == 2 && dName.contains("leaf") && dName.contains(vpnid))
                {
                    FieldNode leafVPC = FieldNodes.get(dName);
                    NDD pred = NDD.table.ref(leafVPC.ports_pred.get("default"));
                    gateVPC.ports.add("internet");
                    gateVPC.ports.add("default");
                    gateVPC.ports_pred.put("internet", pred);
                    gateVPC.ports_pred.put("default", NDD.NDDFalse);
                    gateVPC.ports_pred.put("vxlan", NDD.table.ref(NDD.Not(pred)));
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // System.out.println(System.getProperty("java.version"));
        fw = new FileWriter("/home/zcli/lzc/Field-Decision-Network/DCN_NDD/output/NDD/no_cache/" + args[0] + ".txt",
                false);
        pw = new PrintWriter(fw);

        String mode = "normal";

        Runtime r = Runtime.getRuntime();
        r.gc();
        r.gc();
        long m1 = r.totalMemory() - r.freeMemory();

        FDNNetworkGate n = new FDNNetworkGate(args[0], mode, "base", args[0]);

        Runtime r1 = Runtime.getRuntime();
        r1.gc();
        r1.gc();
        long m2 = r1.totalMemory() - r1.freeMemory();

        pw.println("memory:" + (m2 - m1) / (1024 * 1024) + "MB");
        pw.flush();

        // for(FieldNode d : n.FieldNodes.values())
        // {
        // if(d.name.contains("encap") || d.name.contains("decap"))continue;
        // System.out.println(d.name+" "+d.ports);
        // }

        // n.processDifferentialUpdate("base", "post0");
        // n.processDifferentialUpdate("post0", "post1");
        // n.processDifferentialUpdate("post1", "post2");
        // n.processDifferentialUpdate("post2", "post3");
        // n.processDifferentialUpdate("post3", "post4");

        // pw.println();
        // pw.flush();
        // pw.println("FW_update:" + FW_time / 5);
        // pw.flush();
        // pw.println("VXlan_update:" + VXlan_time / 5);
        // pw.flush();
        // pw.println("VC_update:" + VC_time / 5);
        // pw.flush();
        // pw.println("VNI_update:" + VNI_time / 5);
        // pw.flush();
        // pw.println("EPG_update:" + EPG_time / 5);
        // pw.flush();
        // pw.println("GBP_update:" + GBP_time / 5);
        // pw.flush();
        // pw.println("PBR_update:" + PBR_time / 5);
        // pw.flush();
        // pw.println("Model_update:" + model_realtime / 5);
        // pw.flush();
        // pw.println("Check_update:" + check_realtime / 5);
        // pw.flush();
        // pw.println("Total_update:" + (model_realtime + check_realtime) / 5);
        // pw.flush();
    }
}