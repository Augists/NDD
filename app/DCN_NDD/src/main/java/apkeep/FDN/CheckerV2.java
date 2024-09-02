package apkeep.FDN;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import NDD.NDD;
import apkeep.FDN.FDNNetwork;
import apkeep.FDN.FieldNode;
import apkeep.FDN.TranverseNode;
import common.PositionTuple;
import jdd.bdd.NodeTable;

public class CheckerV2 {
    FDNNetwork net;
    Stack<TranverseNode> queue;
    HashSet<String> reach;

    public CheckerV2(FDNNetwork net, HashSet<String> reach, HashMap<String, HashMap<String, NDD>> reachPackets) {
        this.net = net;
        this.reach = new HashSet<String>(reach);
        // this.reachPackets = new HashMap<String, HashMap<String,
        // FieldAPSet>>(reachPackets);
        queue = new Stack<TranverseNode>();
        reach = new HashSet<>();
        for (String device : net.startEndPoint) {
            // if(!device.equals("leaf-0-0_vpn0"))continue;
            // System.out.println(device);
            queue.add(new TranverseNode(new PositionTuple(device, "virtual"), net.affected_aps));
            // break;
        }
    }

    public static Long EncapSrcEPGtime1 = 0L;
    public static Long EncapSrcEPGtime2 = 0L;
    public static Long EncapVNItime = 0L;
    public static Long EncapIPtime = 0L;
    public static Long Phytime = 0L;
    public static Long Virtime = 0L;
    public static Long DecapIPtime = 0L;
    public static Long VCtime = 0L;
    public static Long DecapVNItime = 0L;
    public static Long EncapDstEPGtime = 0L;
    public static Long DecapEPGtime = 0L;
    public static Long GBPtime = 0L;
    public static Long PBRtime = 0L;

    public void PropertyCheck() throws IOException {
        NDD.test = true;
        NDD.setTest(true);
        NodeTable.test = true;
        while (!queue.isEmpty()) {
            TranverseNode curr_node = queue.pop();
            FieldNode curr_device = net.FieldNodes.get(curr_node.curr.getDeviceName());

            // if(curr_device.name.contains("decapepg"))continue;
            // if(curr_device.name.split("_").length==2 && !curr_device.name.contains("vc") && !curr_device.name.contains("segment") && !curr_device.name.equals(curr_node.source.getDeviceName()) && !curr_device.name.contains("cap"))
            //     continue;

            if (curr_node.curr.getDeviceName().contains("decap")) {
                NDD next_APSet = curr_node.APs;
                if (curr_node.curr.getDeviceName().contains("_decapepg")) {
                    Long t2 = System.nanoTime();
                    NDD temp = NDD.Exist(next_APSet, FDNNetwork.dst_epg_field);
                    next_APSet = NDD.table.ref(NDD.Exist(temp, FDNNetwork.src_epg_field));
                    NDD.table.testAndDelete(temp);
                    Long t3 = System.nanoTime();
                    DecapEPGtime += t3-t2;
                } else if (curr_node.curr.getDeviceName().contains("_decapvni")) {
                    Long t2 = System.nanoTime();
                    next_APSet = NDD.table.ref(NDD.Exist(next_APSet, FDNNetwork.vni_field));
                    Long t3 = System.nanoTime();
                    DecapVNItime += t3-t2;
                } else if (curr_node.curr.getDeviceName().contains("_decapip")) {
                    Long t2 = System.nanoTime();
                    next_APSet = NDD.table.ref(NDD.Exist(next_APSet, FDNNetwork.outter_ip_field));
                    Long t3 = System.nanoTime();
                    DecapIPtime += t3-t2;
                }

                if (!next_APSet.is_False()) {
                    PositionTuple output_pt = new PositionTuple(curr_node.curr.getDeviceName(), "default");
                    HashSet<PositionTuple> nextpts = net.LinkTransfer(output_pt);
                    for (PositionTuple nextpt : nextpts) {
                        queue.push(new TranverseNode(curr_node.source, nextpt, NDD.table.ref(next_APSet),
                                curr_node.visited, curr_node.path, output_pt));
                    }
                    NDD.table.deref(next_APSet);
                }
            } else {
                if (curr_device.type == 2) {
                    NDD next_APSet = NDD.NDDFalse;
                    for (String port : curr_device.ports_pred.keySet()) {
                        if (port.equals("default")) {
                                if(curr_device.name.contains("epg"))continue;
                                Long t0 = System.nanoTime();
                                NDD forward = NDD.AND(curr_device.ports_pred.get(port), curr_node.APs);
                                NDD old = next_APSet;
                                next_APSet = NDD.table.ref(NDD.OR(next_APSet, forward));
                                NDD.table.deref(old);
                                NDD.table.testAndDelete(forward);
                                Long t1 = System.nanoTime();
                                if(curr_device.name.contains("vni"))
                                {
                                    EncapVNItime += t1-t0;
                                }
                                else if(curr_device.name.contains("ip"))
                                {
                                    EncapIPtime += t1-t0;
                                }
                        } else {
                            Long t2 = System.nanoTime();
                            NDD match = NDD.AND(curr_device.ports_pred.get(port), curr_node.APs);
                            Long t3 = System.nanoTime();
                            if(curr_device.name.contains("vni"))
                            {
                                EncapVNItime += t3-t2;
                            }
                            else if(curr_device.name.contains("ip"))
                            {
                                EncapIPtime += t3-t2;
                            }
                            else if(curr_device.name.contains("srcepg"))
                            {
                                EncapSrcEPGtime1 += t3-t2;
                            }
                            else if(curr_device.name.contains("dstepg"))
                            {
                                EncapDstEPGtime += t3-t2;
                            }
                            if (match != NDD.NDDFalse) {
                                int fieldTag = FDNNetwork.field_num;
                                if (curr_node.curr.getDeviceName().contains("_encapsrcepg")) {
                                    fieldTag = FDNNetwork.src_epg_field;
                                } else if (curr_node.curr.getDeviceName().contains("_encapdstepg")) {
                                    fieldTag = FDNNetwork.dst_epg_field;
                                } else if (curr_node.curr.getDeviceName().contains("_encapvni")) {
                                    fieldTag = FDNNetwork.vni_field;
                                } else if (curr_node.curr.getDeviceName().contains("_encapip")) {
                                    fieldTag = FDNNetwork.outter_ip_field;
                                }
                                Long t4 = System.nanoTime();
                                NDD temp = NDD.Exist(match, fieldTag);
                                NDD temp1 = NDD.AND(temp, curr_device.rule_map.get(port).iterator().next().new_val_ndd);
                                NDD old = next_APSet;
                                next_APSet = NDD.table.ref(NDD.OR(next_APSet, temp1));
                                NDD.table.testAndDelete(match);
                                NDD.table.testAndDelete(temp);
                                NDD.table.testAndDelete(temp1);
                                NDD.table.deref(old);
                                Long t5 = System.nanoTime();
                                if (curr_node.curr.getDeviceName().contains("_encapsrcepg")) {
                                    EncapSrcEPGtime2 += t5-t4;
                                } else if (curr_node.curr.getDeviceName().contains("_encapdstepg")) {
                                    EncapDstEPGtime += t5-t4;
                                } else if (curr_node.curr.getDeviceName().contains("_encapvni")) {
                                    EncapVNItime += t5-t4;
                                } else if (curr_node.curr.getDeviceName().contains("_encapip")) {
                                    EncapIPtime += t5-t4;
                                }
                            }
                        }
                    }
                    if (!next_APSet.is_False()) {
                        PositionTuple output_pt = new PositionTuple(curr_node.curr.getDeviceName(), "default");
                        HashSet<PositionTuple> nextpts = net.LinkTransfer(output_pt);
                        for (PositionTuple nextpt : nextpts) {
                            queue.push(new TranverseNode(curr_node.source, nextpt, NDD.table.ref(next_APSet), curr_node.visited,
                                    curr_node.path, output_pt));
                        }
                        NDD.table.deref(next_APSet);
                    }
                } else {
                    // if(curr_device.name.split("_").length==2 && !curr_device.name.contains("vc") && !curr_device.name.contains("segment") && !curr_device.name.equals(curr_node.source.getDeviceName()))
                    //     tag = true;
                    for (String port : curr_device.ports_pred.keySet()) {
                        if (port.equals("default") || port.equals("deny")
                                || port.equals(curr_node.curr.getPortName())) {
                            continue;
                        }
                        // if(curr_device.name.equals("leaf-0-2_tpvpn0_pbr") && port.equals("redirect"))
                        // {
                        //     net.bdd_engine.getBDD().printDot("/home/zcli/lzc/Field-Decision-Network/DCN_NDD/pic/reach", NDD.toBDD(curr_node.APs));
                        //     net.bdd_engine.getBDD().printDot("/home/zcli/lzc/Field-Decision-Network/DCN_NDD/pic/pred", NDD.toBDD(curr_node.APs));
                        // }
                        Long t0 = System.nanoTime();
                        NDD next_AP = NDD.AND(curr_node.APs, curr_device.ports_pred.get(port));
                        Long t1 = System.nanoTime();
                        if(curr_device.name.contains("vc"))
                        {
                            VCtime += t1-t0;
                        }
                        else if(curr_device.name.split("_").length==2 && !curr_device.name.contains("segment"))
                        {
                            Virtime += t1-t0;
                        }
                        else if(!curr_device.name.contains("segment") && !curr_device.name.contains("pbr"))
                        {
                            Phytime += t1-t0;
                        }
                        else if(curr_device.name.contains("segment"))
                        {
                            GBPtime += t1-t0;
                        }
                        else if(curr_device.name.contains("pbr"))
                        {
                            PBRtime += t1-t0;
                        }
                        else
                        {
                            System.out.println(curr_device.name);
                        }
                        if (next_AP.is_False())
                            continue;
                        PositionTuple output_pt = new PositionTuple(curr_node.curr.getDeviceName(), port);
                        HashSet<PositionTuple> nextpts = net.LinkTransfer(output_pt);
                        if (nextpts == null) {
                            // continue;
                            PositionTuple temp_pt = new PositionTuple(output_pt.getDeviceName().split("_")[0], port);
                            curr_node.visited.add(temp_pt.getDeviceName());
                            curr_node.path.add(temp_pt);
                            nextpts = net.LinkTransfer(temp_pt);
                        }
                        for (PositionTuple nextpt : nextpts) {
                            if (net.endEndPoint.contains(nextpt.getDeviceName())) {
                                if (!reach.contains(curr_node.source.getDeviceName() + "->" + nextpt.getDeviceName())) {
                                    reach.add(curr_node.source.getDeviceName() + "->" + nextpt.getDeviceName());
                                }
                                continue;
                            }
                            if (nextpt.getDeviceName().contains("_encapsrcepg")
                                    && curr_node.visited.contains(nextpt.getDeviceName())) {
                                // System.out.println("Loop detected !");
                                continue;
                            }
                            queue.push(new TranverseNode(curr_node.source, nextpt, NDD.table.ref(next_AP), curr_node.visited,
                                    curr_node.path, output_pt));
                        }
                        NDD.table.testAndDelete(next_AP);
                    }
                }
            }
            NDD.table.deref(curr_node.APs);
        }
        NDD.test = false;
        NDD.setTest(false);
        NodeTable.test = false;
        FDNNetwork.pw.println("NDD time:"+NDD.Time/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("EncapSrcEPGtime 1:"+EncapSrcEPGtime1/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("EncapSrcEPGtime 2:"+EncapSrcEPGtime2/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("EncapVNItime:"+EncapVNItime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("EncapIPtime:"+EncapIPtime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("Phytime:"+Phytime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("Virtime:"+Virtime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("DecapIPtime:"+DecapIPtime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("VCtime:"+VCtime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("DecapVNItime:"+DecapVNItime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("EncapDstEPGtime:"+EncapDstEPGtime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("DecapEPGtime:"+DecapEPGtime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("GBPtime:"+GBPtime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("PBRtime:"+PBRtime/1000000000.0);
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("NDD node:"+NDD.getMKCount());
        FDNNetwork.pw.flush();
        FDNNetwork.pw.println("BDD node:"+NodeTable.mkCount);
        FDNNetwork.pw.flush();
    }
}