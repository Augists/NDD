package apkeep.FDN;

import apkeep.utils.Parameters;
import common.PositionTuple;
import jdd.bdd.BDDIO;
import jdd.bdd.NodeTable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import NDD.NDD;

public class Checker {
    FDNNetwork net;
    Stack<TranverseNode> queue;
    HashSet<String> reach;
    // HashMap<String, HashMap<String, NDD>> reachPackets;

    public Checker(FDNNetwork net, HashSet<String> reach, HashMap<String, HashMap<String, NDD>> reachPackets) {
        this.net = net;
        this.reach = new HashSet<String>(reach);
        // this.reachPackets = new HashMap<String, HashMap<String,
        // FieldAPSet>>(reachPackets);
        queue = new Stack<TranverseNode>();

        reach = new HashSet<>();
        for (String device : net.startEndPoint) {
            // System.out.println(device);
            queue.add(new TranverseNode(new PositionTuple(device, "virtual"), net.affected_aps));
            // break;
        }

        // if (FDNNetwork.checkCorrectness) {
        // reachPackets = new HashMap<>();
        // for (String device : net.startEndPoint) {
        // HashMap<String, FieldAPSet> subMap = new HashMap<>();
        // subMap.put(device, FieldAPSet.AP_true);
        // reachPackets.put(device, subMap);
        // }
        // }
    }

    public Checker(FDNNetwork net, String device) {
        this.net = net;
        this.reach = new HashSet<String>();
        queue = new Stack<TranverseNode>();
        reach = new HashSet<>();
        queue.add(new TranverseNode(new PositionTuple(device, "virtual"), net.affected_aps));
    }

    public void print_reach() {
        HashMap<String, HashSet<String>> map = new HashMap<>();
        for (String str : reach) {
            String[] tokens = str.split("->");
            HashSet<String> set = map.get(tokens[0]);
            if (set == null)
                set = new HashSet<>();
            set.add(tokens[1]);
            map.put(tokens[0], set);
        }
        for (String key : map.keySet()) {
            System.out.println(key + " " + map.get(key));
        }
    }

    public void PropertyCheck() throws IOException {
        // Long total1=0L;
        // Long total2=0L;
        // Long total3=0L;
        // Long total4=0L;
        // Long total5=0L;
        // Long total6=0L;
        // HashMap<Integer, Integer> map = new HashMap<>();
        // NodeTable.test = true;
        // NDD.setTest(true);
        // NDD.test = true;
        while (!queue.isEmpty()) {
            TranverseNode curr_node = queue.pop();
            FieldNode curr_device = net.FieldNodes.get(curr_node.curr.getDeviceName());
            // System.out.println("before "+curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // System.out.println();

            // if(curr_node.curr.getDeviceName().contains("encapsrcepg"))
            // {
            //     // NDD.printOut(curr_node.APs);
            //     continue;
            // }
            // if(curr_node.curr.getDeviceName().contains("encapvni"))continue;
            // if(curr_node.curr.getDeviceName().contains("encapip"))continue;
            // if(!curr_node.curr.getDeviceName().contains("encapip"))
            // {
            // boolean state=false;
            // for(String str : curr_node.visited)
            // {
            // if(str.contains("encapip"))
            // {
            // // System.out.println(curr_node.curr.getDeviceName()+" "+curr_node.visited);
            // state = true;
            // }
            // }
            // if(state)continue;
            // }
            // if(curr_node.path.size()>=3 &&
            // curr_node.path.get(curr_node.path.size()-3).getDeviceName().contains("encapip"))
            // {
            // // System.out.println(curr_node.path);
            // continue;
            // }
            // if(curr_node.curr.getDeviceName().contains("decapip"))continue;
            // if(curr_node.curr.getDeviceName().contains("vc"))continue;
            // if(curr_node.curr.getDeviceName().contains("decapvni"))continue;
            // if(curr_node.curr.getDeviceName().contains("encapdstepg"))continue;
            // if(curr_node.curr.getDeviceName().contains("decapepg"))continue;

            // if(curr_node.source.getDeviceName().equals("Leaf-0-15_vpn12"))
            // {
            // if(curr_node.curr.getDeviceName().equals("Leaf-0-15_vpn12_encapsrcepg"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Leaf-0-15_vpn12_encapvni"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Leaf-0-15_encapip"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Leaf-0-15"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Spine-0-1"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Core-0-0"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Core-0-0_decapip"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Core-0-0_vc"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Core-0-0_vpn12_decapvni"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Core-0-0_vpn12_encapdstepg"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Core-0-0_vpn12_decapepg"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // else if(curr_node.curr.getDeviceName().equals("Core-0-0_bd12"))
            // {
            // System.out.println(curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // }
            // }

            if (curr_node.curr.getDeviceName().contains("decap")) {
                NDD next_APSet = curr_node.APs;
                if (curr_node.curr.getDeviceName().contains("_decapepg")) {
                    NDD temp = NDD.Exist(next_APSet, FDNNetwork.dst_epg_field);
                    next_APSet = NDD.Exist(temp, FDNNetwork.src_epg_field);
                    NDD.table.testAndDelete(temp);
                } else if (curr_node.curr.getDeviceName().contains("_decapvni")) {
                    next_APSet = NDD.Exist(next_APSet, FDNNetwork.vni_field);
                } else if (curr_node.curr.getDeviceName().contains("_decapip")) {
                    next_APSet = NDD.Exist(next_APSet, FDNNetwork.outter_ip_field);
                }

                if (!next_APSet.is_False()) {
                    PositionTuple output_pt = new PositionTuple(curr_node.curr.getDeviceName(), "default");
                    HashSet<PositionTuple> nextpts = net.LinkTransfer(output_pt);
                    for (PositionTuple nextpt : nextpts) {
                        queue.push(new TranverseNode(curr_node.source, nextpt, NDD.table.ref(next_APSet),
                                curr_node.visited, curr_node.path, output_pt));
                    }
                }
            } else {
                if (curr_device.type == 2) {
                    NDD next_APSet = NDD.NDDFalse;
                    // System.out.println();
                    // if(curr_node.curr.getDeviceName().contains("encapip"))NDD.printOut(curr_node.APs);
                    for (String port : curr_device.ports_pred.keySet()) {
                        // if(curr_node.curr.getDeviceName().contains("encapdstepg") && curr_node.curr.getDeviceName().contains("leaf-0-29"))System.out.println(curr_device.name+" "+port+" "+curr_device.rule_map.get(port).iterator().next().new_val_ndd);
                        // if(curr_node.curr.getDeviceName().contains("encapip"))System.out.println(curr_device.name+" "+port+" "+curr_device.rule_map.get(port).iterator().next().new_val_ndd);
                        // System.out.println();
                        // System.out.print(curr_device.name+" "+port+" ");
                        if (port.equals("default")) {
                            // if(curr_device.name.contains("epg"))
                            // {
                                NDD forward = NDD.AND(curr_device.ports_pred.get(port), curr_node.APs);
                                NDD old = next_APSet;
                                // Long t1 = System.nanoTime();
                                next_APSet = NDD.table.ref(NDD.OR(next_APSet, forward));
                                // Long t2 = System.nanoTime();
                                // System.out.println((t2-t1)/1000000000.0);
                                NDD.table.deref(old);
                                NDD.table.testAndDelete(forward);
                            // }
                            // else
                            // {
                            //     continue;
                            // }
                        } else {
                            NDD match = NDD.AND(curr_device.ports_pred.get(port), curr_node.APs);
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
                                NDD temp = NDD.Exist(match, fieldTag);
                                NDD temp1 = NDD.AND(temp, curr_device.rule_map.get(port).iterator().next().new_val_ndd);
                                NDD old = next_APSet;
                                // Long t1 = System.nanoTime();
                                next_APSet = NDD.table.ref(NDD.OR(next_APSet, temp1));
                                // Long t2 = System.nanoTime();
                                // System.out.println((t2-t1)/1000000000.0);
                                NDD.table.testAndDelete(match);
                                NDD.table.testAndDelete(temp);
                                NDD.table.testAndDelete(temp1);
                                NDD.table.deref(old);
                            }
                        }
                    }
                    if (!next_APSet.is_False()) {
                        // if(curr_node.curr.getDeviceName().contains("encapip"))NDD.printOut(next_APSet);
                        PositionTuple output_pt = new PositionTuple(curr_node.curr.getDeviceName(), "default");
                        HashSet<PositionTuple> nextpts = net.LinkTransfer(output_pt);
                        for (PositionTuple nextpt : nextpts) {
                            queue.push(new TranverseNode(curr_node.source, nextpt, NDD.table.ref(next_APSet), curr_node.visited,
                                    curr_node.path, output_pt));
                        }
                        NDD.table.deref(next_APSet);
                    }
                } else {
                    for (String port : curr_device.ports_pred.keySet()) {
                        if (port.equals("default") || port.equals("deny")
                                || port.equals(curr_node.curr.getPortName())) {
                            continue;
                        }
                        // long t1 = System.nanoTime();
                        NDD next_AP = NDD.AND(curr_node.APs, curr_device.ports_pred.get(port));
                        // long t2 = System.nanoTime();
                        // if(curr_device.name.contains("_vc"))total1 = total1 + t2 - t1;
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
                                // int len = curr_node.path.size();
                                // if(map.containsKey(len))
                                // {
                                // map.put(len, map.get(len)+1);
                                // }
                                // else
                                // {
                                // map.put(len, 1);
                                // }
                                // if (curr_node.path.size() == 26)
                                //     System.out.println(curr_node.path);
                                // System.out.println(curr_node.path.size());
                                if (!reach.contains(curr_node.source.getDeviceName() + "->" + nextpt.getDeviceName())) {
                                    reach.add(curr_node.source.getDeviceName() + "->" + nextpt.getDeviceName());
                                }
                                // if(FDNNetwork.checkCorrectness)
                                // {
                                // if (!reachPackets.containsKey(curr_node.source.getDeviceName())) {
                                // reachPackets.put(curr_node.source.getDeviceName(),
                                // new HashMap<String, FieldAPSet>());
                                // }
                                // FieldAPSet subReach = reachPackets.get(curr_node.source.getDeviceName())
                                // .get(nextpt.getDeviceName());
                                // if (subReach == null) {
                                // subReach = FieldAPSet.AP_false;
                                // }
                                // FieldAPSet new_reach = FieldAPSet.Or(subReach, next_AP);
                                // reachPackets.get(curr_node.source.getDeviceName()).put(nextpt.getDeviceName(),
                                // new_reach);
                                // }
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
        // NDD.test = false;
        // NDD.showStatestic();
        // NodeTable.test = false;
        // NDD.setTest(false);
        // net.pw.println("new BDD node: "+NodeTable.mkCount);
        // net.pw.flush();
        // net.pw.println("new NDD node: "+NDD.getMKCount());
        // net.pw.flush();
        // System.out.println("total1:"+total1/1000000000.0);
        // System.out.println("total2:"+total2/1000000000.0);
        // System.out.println("total3:"+total3/1000000000.0);
        // System.out.println("total4:"+total4/1000000000.0);
        // System.out.println("total5:"+total5/1000000000.0);
        // System.out.println("total6:"+total6/1000000000.0);
        // System.out.println("T1:"+NDD.T1/1000000000.0);
        // System.out.println("T2:"+NDD.T2/1000000000.0);
        // System.out.println("T3:"+NDD.T3/1000000000.0);
        // System.out.println("T4:"+NDD.T4/1000000000.0);
        // System.out.println("T5:"+NDD.T5/1000000000.0);
        // System.out.println("T6:"+NDD.T6/1000000000.0);
        // System.out.println(net.bdd_engine.getBDD().stat_gc_time);
        // System.out.println(map);
    }

    public void print_to_file(String fileName) throws IOException {
        path = "Field-Decision-Network"
        FileWriter fw = new FileWriter(path + "/apkatra-main/pic/" + fileName, false);
        PrintWriter pw = new PrintWriter(fw);
        HashMap<String, HashSet<String>> map = new HashMap<>();
        for (String str : reach) {
            String[] tokens = str.split("->");
            HashSet<String> subset = map.get(tokens[0]);
            if (subset == null) {
                subset = new HashSet<>();
            }
            subset.add(tokens[1]);
            map.put(tokens[0], subset);
        }
        for (String from : map.keySet()) {
            pw.print(from);
            for (String to : map.get(from)) {
                pw.print(" " + to);
            }
            pw.println();
            pw.flush();
        }
    }

    public void OutputReachBDD() throws IOException {
        // for(String src : reachPackets.keySet())
        // {
        // for(String dst : reachPackets.get(src).keySet())
        // {
        // BDDIO.save(net.bdd_engine.getBDD(),
        // FieldAPSet.toBDD(reachPackets.get(src).get(dst)),
        // "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/correctness/"+net.name+"/NDD/"+net.currStage+"/"+src+"_"+dst);
        // }
        // }
    }
}
