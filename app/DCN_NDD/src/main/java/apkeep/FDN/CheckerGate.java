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

public class CheckerGate {
    FDNNetwork net;
    Stack<TranverseNode> queue;
    HashSet<String> reach;

    public CheckerGate(FDNNetwork net, HashSet<String> reach, HashMap<String, HashMap<String, NDD>> reachPackets) {
        this.net = net;
        this.reach = new HashSet<String>(reach);
        queue = new Stack<TranverseNode>();

        reach = new HashSet<>();
        for (String device : net.startEndPoint) {
            if (!device.contains("leaf"))
                continue;
            queue.add(new TranverseNode(new PositionTuple(device, "virtual"), net.affected_aps));
            // break;
        }
    }

    public CheckerGate(FDNNetwork net, String device) {
        this.net = net;
        this.reach = new HashSet<String>();
        queue = new Stack<TranverseNode>();
        reach = new HashSet<>();
        queue.add(new TranverseNode(new PositionTuple(device, "virtual"), net.affected_aps));
    }

    public void PropertyCheck() throws IOException {
        // NodeTable.test = true;
        // NDD.setTest(true);
        while (!queue.isEmpty()) {
            TranverseNode curr_node = queue.pop();
            FieldNode curr_device = net.FieldNodes.get(curr_node.curr.getDeviceName());
            // System.out.println(curr_node.source.getDeviceName()+" "+curr_device.name+" "+curr_node.path);
            // System.out.println("before "+curr_node.curr.getDeviceName());
            // NDD.printOut(curr_node.APs);
            // System.out.println();

            // if(curr_node.curr.getDeviceName().contains("encapsrcepg"))
            // {
            // // NDD.printOut(curr_node.APs);
            // continue;
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
                    if(curr_node.curr.getDeviceName().contains("epg"))
                    {
                        for (String port : curr_device.ports_pred.keySet()) {
                            if (port.equals("default")) {
                                NDD forward = NDD.AND(curr_device.ports_pred.get(port), curr_node.APs);
                                NDD old = next_APSet;
                                next_APSet = NDD.table.ref(NDD.OR(next_APSet, forward));
                                NDD.table.deref(old);
                                NDD.table.testAndDelete(forward);
                            } else {
                                NDD match = NDD.AND(curr_device.ports_pred.get(port), curr_node.APs);
                                if (match != NDD.NDDFalse) {
                                    int fieldTag = FDNNetwork.field_num;
                                    if (curr_node.curr.getDeviceName().contains("_encapsrcepg")) {
                                        fieldTag = FDNNetwork.src_epg_field;
                                    } else if (curr_node.curr.getDeviceName().contains("_encapdstepg")) {
                                        fieldTag = FDNNetwork.dst_epg_field;
                                    }
                                    NDD temp = NDD.Exist(match, fieldTag);
                                    NDD temp1 = NDD.AND(temp, curr_device.rule_map.get(port).iterator().next().new_val_ndd);
                                    NDD old = next_APSet;
                                    next_APSet = NDD.table.ref(NDD.OR(next_APSet, temp1));
                                    NDD.table.testAndDelete(match);
                                    NDD.table.testAndDelete(temp);
                                    NDD.table.testAndDelete(temp1);
                                    NDD.table.deref(old);
                                }
                            }
                        }
                    }
                    else if(curr_node.curr.getDeviceName().contains("_encapvni"))
                    {
                        int fieldTag = FDNNetwork.vni_field;
                        NDD encap = NDD.NDDFalse;
                        for (String port : curr_device.ports_pred.keySet()) {
                            if (!port.equals("default"))
                            {
                                encap = curr_device.rule_map.get(port).iterator().next().new_val_ndd;
                                break;
                            }
                        }
                        // net.bdd_engine.getBDD().printDot("/home/zcli/lzc/Field-Decision-Network/DCN_NDD/pic/encapVNI", NDD.toBDD(encap));
                        NDD forward = NDD.AND(curr_device.ports_pred.get("default"), curr_node.APs);
                        NDD Temp = NDD.Exist(forward, fieldTag);
                        NDD Temp1 = NDD.AND(Temp, encap);
                        NDD old = next_APSet;
                        next_APSet = NDD.table.ref(NDD.OR(next_APSet, Temp1));
                        NDD.table.deref(old);
                        NDD.table.testAndDelete(forward);
                        NDD.table.testAndDelete(Temp);
                        NDD.table.testAndDelete(Temp1);
                        NDD residual = NDD.Diff(curr_node.APs, curr_device.ports_pred.get("default"));
                        if(!residual.is_False())
                        {
                            for (String port : curr_device.ports_pred.keySet()) {
                                if (port.equals("default")) {
                                    continue;
                                } else {
                                    NDD match = NDD.AND(curr_device.ports_pred.get(port), curr_node.APs);
                                    if (match != NDD.NDDFalse) {
                                        NDD temp = NDD.Exist(match, fieldTag);
                                        NDD temp1 = NDD.AND(temp, curr_device.rule_map.get(port).iterator().next().new_val_ndd);
                                        NDD oldNext = next_APSet;
                                        next_APSet = NDD.table.ref(NDD.OR(next_APSet, temp1));
                                        NDD.table.testAndDelete(match);
                                        NDD.table.testAndDelete(temp);
                                        NDD.table.testAndDelete(temp1);
                                        NDD.table.deref(oldNext);
                                    }
                                }
                            }
                        }
                        NDD.table.testAndDelete(residual);
                    }
                    else if(curr_node.curr.getDeviceName().contains("_encapip"))
                    {
                        int fieldTag = FDNNetwork.outter_ip_field;
                        NDD encap = FDNNetworkGate.GateIPNDD;
                        NDD forward = NDD.AND(curr_device.ports_pred.get("default"), curr_node.APs);
                        NDD Temp = NDD.Exist(forward, fieldTag);
                        NDD Temp1 = NDD.AND(Temp, encap);
                        NDD old = next_APSet;
                        next_APSet = NDD.table.ref(NDD.OR(next_APSet, Temp1));
                        NDD.table.deref(old);
                        NDD.table.testAndDelete(forward);
                        NDD.table.testAndDelete(Temp);
                        NDD.table.testAndDelete(Temp1);
                        NDD residual = NDD.Diff(curr_node.APs, curr_device.ports_pred.get("default"));
                        if(!residual.is_False())
                        {
                            for (String port : curr_device.ports_pred.keySet()) {
                                if (port.equals("default")) {
                                    continue;
                                } else {
                                    NDD match = NDD.AND(curr_device.ports_pred.get(port), curr_node.APs);
                                    if (match != NDD.NDDFalse) {
                                        NDD temp = NDD.Exist(match, fieldTag);
                                        NDD temp1 = NDD.AND(temp, curr_device.rule_map.get(port).iterator().next().new_val_ndd);
                                        NDD oldNext = next_APSet;
                                        next_APSet = NDD.table.ref(NDD.OR(next_APSet, temp1));
                                        NDD.table.testAndDelete(match);
                                        NDD.table.testAndDelete(temp);
                                        NDD.table.testAndDelete(temp1);
                                        NDD.table.deref(oldNext);
                                    }
                                }
                            }
                        }
                        NDD.table.testAndDelete(residual);
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
                    for (String port : curr_device.ports_pred.keySet()) {
                        if (!curr_device.name.contains("vc") && !curr_device.name.contains("gate") && curr_device.name.split("_").length == 2) {
                            // System.out.println(curr_node.source.getDeviceName()+" "+curr_device.name+" "+curr_node.path);
                            if (!port.equals("default")) {
                                continue;
                            }
                        } else {
                            if (port.equals("default") || port.equals("deny")
                                    || port.equals(curr_node.curr.getPortName())) {
                                continue;
                            }
                        }
                        NDD next_AP = NDD.AND(curr_node.APs, curr_device.ports_pred.get(port));
                        if (next_AP.is_False())
                            continue;
                        PositionTuple output_pt = new PositionTuple(curr_node.curr.getDeviceName(), port);
                        HashSet<PositionTuple> nextpts = net.LinkTransfer(output_pt);
                        if (nextpts == null) {
                            PositionTuple temp_pt = new PositionTuple(output_pt.getDeviceName().split("_")[0], port);
                            curr_node.visited.add(temp_pt.getDeviceName());
                            curr_node.path.add(temp_pt);
                            nextpts = net.LinkTransfer(temp_pt);
                        }
                        for (PositionTuple nextpt : nextpts) {
                            if (net.endEndPoint.contains(nextpt.getDeviceName())) {
                                if (nextpt.getDeviceName().equals("out")) {
                                    reach.add(curr_node.source.getDeviceName() + "->" + nextpt.getDeviceName());
                                }
                                continue;
                            }
                            if (nextpt.getDeviceName().contains("_encapsrcepg")
                                    && curr_node.visited.contains(nextpt.getDeviceName())) {
                                // System.out.println("Loop detected !");
                                continue;
                            }
                            queue.push(new TranverseNode(curr_node.source, nextpt, NDD.table.ref(next_AP),
                                    curr_node.visited, curr_node.path, output_pt));
                        }
                        NDD.table.testAndDelete(next_AP);
                    }
                }
            }
            NDD.table.deref(curr_node.APs);
        }
        // NodeTable.test = false;
        // NDD.setTest(false);
        // net.pw.println("new BDD node: "+NodeTable.mkCount);
        // net.pw.flush();
        // net.pw.println("new NDD node: "+NDD.getMKCount());
        // net.pw.flush();
    }
}
