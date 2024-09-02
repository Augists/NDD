package apkeep.FDN;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import apkeep.FDN.FDNNetworkAP_V2;
import apkeep.FDN.FieldNodeAP;
import apkeep.FDN.TranverseNodeAP;
import molecule.Molecule;
import common.PositionTuple;

public class CheckerAP_V2 {
    FDNNetworkAP_V2 net;
    Stack<TranverseNodeAP> queue;
    HashSet<String> reach;

    public CheckerAP_V2(FDNNetworkAP_V2 net, HashSet<String> reach, HashMap<String, HashMap<String, Molecule>> reachPackets) {
        this.net = net;
        this.reach = new HashSet<String>(reach);
        queue = new Stack<TranverseNodeAP>();
        reach = new HashSet<>();
        for (String device : net.startEndPoint) {
            // if(!device.equals("leaf-0-5_vpn1"))continue;
            queue.add(new TranverseNodeAP(new PositionTuple(device, "virtual"), net.affected_aps));
            // System.out.println(device);
            // break;
        }
    }

    public void PropertyCheck() throws IOException {
        // System.out.println("tag1");
        if(FDNNetworkAP_V2.runOriginDataset)
        {
            RepairDefaultPort();
        }
        // System.out.println("tag2");
        Molecule.useCache = true;
        while (!queue.isEmpty()) {
            TranverseNodeAP curr_node = queue.pop();
            FieldNodeAP curr_device = net.FieldNodes.get(curr_node.curr.getDeviceName());

            // if(curr_device.name.contains("core-0-0_vpn7_encapdstepg"))System.out.println(curr_device.name);
            // System.out.println();
            // System.out.println(curr_device.name);

            if (curr_node.curr.getDeviceName().contains("decap")) {
                // System.out.println("tag3");
                Molecule next_APSet = curr_node.APs;
                if (curr_node.curr.getDeviceName().contains("_decapepg")) {
                    Molecule temp = Molecule.Exist(next_APSet, FDNNetwork.dst_epg_field);
                    next_APSet = Molecule.table.ref(Molecule.Exist(temp, FDNNetwork.src_epg_field));
                    Molecule.table.testAndDelete(temp);
                } else if (curr_node.curr.getDeviceName().contains("_decapvni")) {
                    next_APSet = Molecule.table.ref(Molecule.Exist(next_APSet, FDNNetwork.vni_field));
                } else if (curr_node.curr.getDeviceName().contains("_decapip")) {
                    next_APSet = Molecule.table.ref(Molecule.Exist(next_APSet, FDNNetwork.outter_ip_field));
                }

                if (!next_APSet.is_False()) {
                    PositionTuple output_pt = new PositionTuple(curr_node.curr.getDeviceName(), "default");
                    HashSet<PositionTuple> nextpts = net.LinkTransfer(output_pt);
                    for (PositionTuple nextpt : nextpts) {
                        queue.push(new TranverseNodeAP(curr_node.source, nextpt, Molecule.table.ref(next_APSet),
                                curr_node.visited, curr_node.path, output_pt));
                    }
                    Molecule.table.deref(next_APSet);
                }
                // System.out.println("tag4");
            } else {
                if (curr_device.type == 2) {
                    // System.out.println("tag5");
                    Molecule next_APSet = Molecule.MFalse;
                    // if(curr_device.name.contains("core-0-0_vpn7_encapdstepg"))
                    // {
                    //     System.out.println(curr_device.ports_aps.keySet().toString());
                    // }
                    for (String port : curr_device.ports_aps.keySet()) {
                        if (port.equals("default")) {
                            if(FDNNetworkAP_V2.runOriginDataset)
                            {
                                Molecule forward = Molecule.AND(curr_device.ports_aps.get(port), curr_node.APs);
                                Molecule old = next_APSet;
                                next_APSet = Molecule.table.ref(Molecule.OR(next_APSet, forward));
                                Molecule.table.deref(old);
                                Molecule.table.testAndDelete(forward);
                            }
                            continue;
                        } else {
                            Molecule match = Molecule.AND(curr_device.ports_aps.get(port), curr_node.APs);
                            if (match != Molecule.MFalse) {
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
                                Molecule temp = Molecule.Exist(match, fieldTag);
                                Molecule temp1 = Molecule.AND(temp, curr_device.ports_action_aps.get(port));
                                Molecule old = next_APSet;
                                next_APSet = Molecule.table.ref(Molecule.OR(next_APSet, temp1));
                                Molecule.table.testAndDelete(match);
                                Molecule.table.testAndDelete(temp);
                                Molecule.table.testAndDelete(temp1);
                                Molecule.table.deref(old);
                            }
                        }
                    }
                    // if(next_APSet.is_False())
                    // {
                    //     System.out.println("tag");
                    // }
                    if (!next_APSet.is_False()) {
                        PositionTuple output_pt = new PositionTuple(curr_node.curr.getDeviceName(), "default");
                        HashSet<PositionTuple> nextpts = net.LinkTransfer(output_pt);
                        for (PositionTuple nextpt : nextpts) {
                            queue.push(new TranverseNodeAP(curr_node.source, nextpt, Molecule.table.ref(next_APSet), curr_node.visited,
                                    curr_node.path, output_pt));
                        }
                        Molecule.table.deref(next_APSet);
                    }
                    // System.out.println("tag6");
                } else {
                    // System.out.println("tag7");
                    for (String port : curr_device.ports_aps.keySet()) {
                        // System.out.println(port);
                        if (port.equals("default") || port.equals("deny")
                                || port.equals(curr_node.curr.getPortName())) {
                            continue;
                        }
                        Molecule next_AP = Molecule.AND(curr_node.APs, curr_device.ports_aps.get(port));
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
                            queue.push(new TranverseNodeAP(curr_node.source, nextpt, Molecule.table.ref(next_AP), curr_node.visited,
                                    curr_node.path, output_pt));
                        }
                        Molecule.table.testAndDelete(next_AP);
                    }
                    // System.out.println("tag8");
                }
            }
            Molecule.table.deref(curr_node.APs);
            // System.out.println("tag9");
        }
        Molecule.useCache = false;
        // System.out.println(reach.toString());
    }

    public void RepairDefaultPort()
    {
        for(FieldNodeAP device : net.FieldNodes.values())
        {
            if(device.name.contains("encap"))
            {
                Molecule residual = Molecule.MTrue;
                Molecule sum = Molecule.MFalse;
                for(String port : device.ports_aps.keySet())
                {
                    if(port.equals("default"))
                    {
                        Molecule.table.deref(device.ports_aps.get(port));
                        continue;
                    }
                    Molecule old = sum;
                    sum = Molecule.OR(sum, device.ports_aps.get(port));
                    Molecule.table.testAndDelete(old);
                }
                residual = Molecule.Diff(residual, sum);
                Molecule.table.testAndDelete(sum);
                Molecule.table.ref(residual);
                device.ports_aps.put("default", residual);
            }
        }
    }
}