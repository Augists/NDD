package apkeep.FDN;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import NDD.NDD;
import apkeep.FDN.FDNNetworkV2;
import apkeep.FDN.FieldNodeAP;
import apkeep.FDN.SplitMap;
import apkeep.core.ChangeItem;
import apkeep.utils.ChangeTuple;
import common.ACLRule;
import javafx.util.Pair;
import molecule.Molecule;
import common.PositionTuple;

public class FDNNetworkAP_V2 extends FDNNetworkV2 {

    public HashMap<String, FieldNodeAP> FieldNodes;
    public SplitMap splitMap;
    public SplitMap splitMapAction;
    protected CheckerAP_V2 checkerV2;
    public Molecule affected_aps;
    public static boolean runOriginDataset = true;

    public FDNNetworkAP_V2(String name, String mode, String postid, String dataset) throws Exception {
        super(name, mode, postid, dataset, true);

        FieldNodes = new HashMap<>();
        Molecule.SetFieldNum(field_num);
        Molecule.bdd = bdd_engine;
        Molecule.table.bdd = bdd_engine.getBDD();
        FieldNodeAP.network = this;
        splitMap = new SplitMap(NDD.fieldNum, this);
        splitMapAction = new SplitMap(NDD.fieldNum, this);
        splitMapAction.SetType(true);

        double t1 = System.nanoTime();

        constructPPMFromFile(postid); // *** generate topology from file to hashmap

        double t2 = System.nanoTime();

        isBatch = true;
        updatePPMSnapshot_Increasement(); // *** port predicate
        affected_aps = Molecule.MTrue;
        UpdateFieldAP();
        double t3 = System.nanoTime();
        // System.out.println("update model:" + (t3 - t2) / 1000000000);
        pw.println("update model:" + (t3 - t2) / 1000000000);
        pw.flush();

        // pw.println("Stage 1");
        pw.flush();
        checkerV2 = new CheckerAP_V2(this, new HashSet<String>(), new HashMap<>());
        // pw.println("Stage 2");
        pw.flush();
        checkerV2.PropertyCheck();
        pw.println("reach size:" + checkerV2.reach.size());
        pw.flush();

        // for(String str : checkerV2.reach)
        // {
        // System.out.println(str);
        // }

        double t4 = System.nanoTime();

        pw.println("property check:" + (t4 - t3) / 1000000000);
        pw.flush();
        pw.println("total:" + (t4 - t2) / 1000000000);
        pw.flush();

        int sum = 0;
        for (int i = 0; i < field_num; i++) {
            sum += Molecule.table.getAP(i).size();
            pw.println("field" + i + " " + Molecule.table.getAP(i).size());
            pw.flush();
        }
        pw.println("AP:" + sum);
        pw.flush();

        int node_num = Molecule.table.AP_nodes();
        pw.println("node for APs:" + node_num);
        pw.flush();
    }

    void addFWDRule(String linestr) {
        // TODO Auto-generated method stub
        String token = " ";
        if (name.equals("campus"))
            token = ",";
        String[] tokens = linestr.split(token);

        String op = tokens[0];
        String element_name = tokens[2];
        if (!FieldNodes.containsKey(element_name))
            return;
        String ipInt = tokens[3];
        String prio = tokens[4];
        String outport = tokens[5];

        String ip = ipInt + "/" + prio;
        // ip和前缀
        HashMap<String, HashSet<Pair<String, String>>> rules = fwd_rules.get(ip);
        if (rules == null) {
            rules = new HashMap<String, HashSet<Pair<String, String>>>();
        }
        HashSet<Pair<String, String>> actions = rules.get(element_name);
        if (actions == null) {
            actions = new HashSet<Pair<String, String>>();
        }
        Pair<String, String> pair = new Pair<String, String>(op, outport);
        // 加或者减，然后是output
        actions.add(pair);
        rules.put(element_name, actions);
        fwd_rules.put(ip, rules);
    }

    void addForwardNode(String eName, int field) {
        if (!FieldNodes.containsKey(eName)) {
            if (eName.contains("vpn")) {
                fe++;
                vpne++;
            } else {
                fe++;
            }

            FieldNodeAP e = new FieldNodeAP(eName, this, 0, false, field, no_field);
            FieldNodes.put(e.name, e);
            // splitMap.AddDefaultPort(eName, field);
        }
    }

    void addACLNode(String eName) {
        if (!FieldNodes.containsKey(eName)) {
            ae++;
            if (runOriginDataset) {
                FieldNodeAP e = new FieldNodeAP(eName, this, 1, false, multi_filed, no_field);
                FieldNodes.put(e.name, e);
            } else {
                FieldNodeAP e = new FieldNodeAP(eName, this, -1, false, multi_filed, no_field);
                FieldNodes.put(e.name, e);
            }
        }
    }

    void addPBRNode(String eName) {
        if (runOriginDataset) {
            addACLNode(eName);
            return;
        }
        if (!FieldNodes.containsKey(eName)) {
            ae++;
            FieldNodeAP e = new FieldNodeAP(eName, this, 1, false, multi_filed, no_field);
            FieldNodes.put(e.name, e);
            addEndHostPBR(eName, "redirect");
        }
    }

    void addNATNode(String eName, int field, int actionField) {
        if (!FieldNodes.containsKey(eName)) {
            if (eName.contains("epg")) {
                epge++;
            } else if (eName.contains("vni")) {
                // vni这个不能修改
                vnie++;
            } else if (eName.contains("ip")) {
                ipe++;
            }
            ne++;
            if (false & eName.contains("decap")) {
                FieldNodeAP e = new FieldNodeAP(eName, this, 2, true, field, actionField);
                FieldNodes.put(e.name, e);
            } else {
                FieldNodeAP e = new FieldNodeAP(eName, this, 2, false, field, actionField);
                FieldNodes.put(e.name, e);
            }
        }
    }

    public void updateFWRuleBatch_Increasement(HashMap<String, HashMap<String, HashSet<Pair<String, String>>>> fw_rules,
            HashMap<String, HashSet<Integer>> moved_fwd_aps) {
        // TODO Auto-generated method stub
        ArrayList<String> ips = new ArrayList<String>();
        HashSet<String> update_points = new HashSet<String>();

        for (String ip : fw_rules.keySet()) {
            ips.add(ip);
        }
        Collections.sort(ips, new sortRulesByPriority());

        // first step - get the change_set & copy_set & remove_set
        HashMap<String, ArrayList<ChangeTuple>> change_set = new HashMap<String, ArrayList<ChangeTuple>>();
        HashMap<String, ArrayList<ChangeTuple>> remove_set = new HashMap<String, ArrayList<ChangeTuple>>();
        HashMap<String, ArrayList<ChangeTuple>> copyto_set = new HashMap<String, ArrayList<ChangeTuple>>();

        for (String ip : ips) {
            for (String element_name : fw_rules.get(ip).keySet()) {
                HashSet<Pair<String, String>> actions = fw_rules.get(ip).get(element_name);
                HashSet<String> to_ports = new HashSet<String>();
                HashSet<String> from_ports = new HashSet<String>();

                for (Pair<String, String> pair : actions) {
                    FW_num++;
                    if (pair.getKey().equals("+"))
                        to_ports.add(pair.getValue());
                    else if (pair.getKey().equals("-"))
                        from_ports.add(pair.getValue());
                }
                // batch move atom
                HashSet<String> retained = new HashSet<String>(to_ports);
                retained.retainAll(from_ports);
                if (!retained.isEmpty()) {
                    to_ports.removeAll(retained);
                    from_ports.removeAll(retained);
                }

                FieldNodeAP e = FieldNodes.get(element_name);
                e.updateFWRuleBatch(ip, to_ports, from_ports, change_set, copyto_set, remove_set);
                update_points.add(element_name);
            }
        }

        if (isBatch) {
            mergeChangeTuple(change_set);
            mergeChangeTuple(copyto_set);
            mergeChangeTuple(remove_set);
        }

        for (String device : remove_set.keySet()) {
            if (remove_set.get(device).size() != 0) {
                System.out.println("remove not implemented");
            }
        }
        for (String element_name : update_points) {
            FieldNodeAP e = FieldNodes.get(element_name);
            e.update_FW(change_set.get(element_name), copyto_set.get(element_name));
        }
    }

    public void mergeChangeTuple(HashMap<String, ArrayList<ChangeTuple>> changes) {
        HashMap<String, HashMap<Pair<HashSet<String>, HashSet<String>>, HashSet<Integer>>> map = new HashMap<>();
        for (Map.Entry<String, ArrayList<ChangeTuple>> entry : changes.entrySet()) {
            HashMap<Pair<HashSet<String>, HashSet<String>>, HashSet<Integer>> subMap = new HashMap<>();
            for (ChangeTuple change : entry.getValue()) {
                Pair<HashSet<String>, HashSet<String>> pair = new Pair<HashSet<String>, HashSet<String>>(
                        change.from_ports, change.to_ports);
                HashSet<Integer> aps = subMap.get(pair);
                if (aps == null) {
                    aps = new HashSet<>();
                    aps.addAll(change.delta_set);
                    subMap.put(pair, aps);
                } else {
                    aps.addAll(change.delta_set);
                }
            }
            map.put(entry.getKey(), subMap);
        }

        changes.clear();
        for (Map.Entry<String, HashMap<Pair<HashSet<String>, HashSet<String>>, HashSet<Integer>>> entry : map
                .entrySet()) {
            ArrayList<ChangeTuple> arrayList = new ArrayList<>();
            for (Map.Entry<Pair<HashSet<String>, HashSet<String>>, HashSet<Integer>> entry1 : entry.getValue()
                    .entrySet()) {
                ChangeTuple change = new ChangeTuple(entry1.getKey().getKey(), entry1.getKey().getValue(),
                        entry1.getValue());
                arrayList.add(change);
            }
            changes.put(entry.getKey(), arrayList);
        }
    }

    public void updateVxLANEncapDecap_Increasement_Batch(HashMap<String, HashSet<Integer>> moved_fwd_aps) {
        HashMap<String, HashMap<String, HashSet<Integer>>> toRemove = new HashMap<>(); // device from_port delta
        HashMap<String, HashMap<String, HashSet<Integer>>> toAdd = new HashMap<>(); // device to_port delta

        // Long t0 = System.nanoTime();
        for (String vpn : ip_encap_rules.keySet()) {
            FieldNodeAP encap_element = FieldNodes.get(vpn.split("_")[0] + "_encapip");

            if (!toRemove.containsKey(encap_element.name))
                toRemove.put(encap_element.name, new HashMap());
            if (!toAdd.containsKey(encap_element.name))
                toAdd.put(encap_element.name, new HashMap());
            HashMap<String, HashSet<Integer>> subToRemove = toRemove.get(encap_element.name);
            HashMap<String, HashSet<Integer>> subToAdd = toAdd.get(encap_element.name);

            ArrayList<String> iporder = new ArrayList<>();
            for (String origin_ip : ip_encap_rules.get(vpn).keySet()) {
                if (iporder.isEmpty())
                    iporder.add(origin_ip);
                else {
                    boolean isInser = false;
                    for (int i = 0; i < iporder.size(); i++) {
                        if (Integer.valueOf(origin_ip.split("/")[1]) < Integer.valueOf(iporder.get(i).split("/")[1])) {
                            iporder.add(i, origin_ip);
                            isInser = true;
                            break;
                        }
                    }
                    if (!isInser) {
                        iporder.add(origin_ip);
                    }
                }
            }

            for (String origin_ip : iporder) {
                for (Pair<String, String> pair : ip_encap_rules.get(vpn).get(origin_ip)) {
                    VXlan_num++;
                    String op = pair.getKey();
                    String outter_ip = pair.getValue();

                    long ipaddr = Long.parseLong(origin_ip.split("/")[0]);
                    int prefixlen = Integer.parseInt(origin_ip.split("/")[1]);

                    int field_bdd = bdd_engine.get_field_bdd("outterIP");

                    int dstIP = bdd_engine.encodeDstIPPrefix(ipaddr, prefixlen);

                    int vni = Integer.parseInt(origin_ip.split("/")[2]);
                    vni = 50000 + vni;
                    int vnibdd = bdd_engine.encodeVNI(String.valueOf(vni));
                    dstIP = bdd_engine.and(dstIP, vnibdd);
                    int newHeader = bdd_engine.encodeDstIPPrefix_outter(Long.parseLong(outter_ip), 32);
                    EncapsulationIPRule encap_r = new EncapsulationIPRule(dstIP, field_bdd, newHeader, outter_ip,
                            prefixlen, bdd_engine);
                    ArrayList<ChangeItem> changelist = encap_element.InsertRewriteRule(encap_r);
                    for (ChangeItem item : changelist) {
                        if (!subToRemove.containsKey(item.from_port)) {
                            HashSet<Integer> set = new HashSet<>();
                            set.add(item.delta);
                            subToRemove.put(item.from_port, set);
                        } else {
                            HashSet<Integer> set = subToRemove.get(item.from_port);
                            set.add(item.delta);
                        }
                        if (!subToAdd.containsKey(item.to_port)) {
                            HashSet<Integer> set = new HashSet<>();
                            set.add(item.delta);
                            subToAdd.put(item.to_port, set);
                        } else {
                            HashSet<Integer> set = subToAdd.get(item.to_port);
                            set.add(item.delta);
                        }
                    }
                }
            }
        }
        // Long t1 = System.nanoTime();
        HashMap<String, HashMap<String, Integer>> mergedRemove = mergeChange(toRemove);
        HashMap<String, HashMap<String, Integer>> mergedAdd = mergeChange(toAdd);
        // Long t2 = System.nanoTime();
        // FieldNodeAP.testVxLAN = true;
        for (Entry<String, HashMap<String, Integer>> entry : mergedRemove.entrySet()) {
            FieldNodeAP device = FieldNodes.get(entry.getKey());
            device.update_ACL(entry.getValue(), mergedAdd.get(entry.getKey()));
        }
        // FieldNodeAP.testVxLAN = false;
        // Long t3 = System.nanoTime();
        // System.out.println("VxLAN t1:"+(t1-t0)/1000000000.0);
        // System.out.println("VxLAN t2:"+(t2-t1)/1000000000.0);
        // System.out.println("VxLAN t3:"+(t3-t2)/1000000000.0);
        // System.out.println("Update ACL t0:"+FieldNodeAP.Time0/1000000000.0);
        // System.out.println("Update ACL t1:"+FieldNodeAP.Time1/1000000000.0);
        // System.out.println("Update ACL t2:"+FieldNodeAP.Time2/1000000000.0);
        // System.out.println("Update ACL t3:"+FieldNodeAP.Time3/1000000000.0);
        // System.out.println("Update ACL t4:"+FieldNodeAP.Time4/1000000000.0);
        // System.out.println("Update ACL c1:"+FieldNodeAP.Count1);
        // System.out.println("Update ACL c2:"+FieldNodeAP.Count2);
        // System.out.println("Get Split t0:"+Molecule.splitTime1/1000000000.0);
        // System.out.println("Get Split t1:"+Molecule.splitTime2/1000000000.0);
        // System.out.println("Get Split t2:"+Molecule.splitTime3/1000000000.0);
        // System.out.println("Get Split t3:"+Molecule.splitTime4/1000000000.0);
        // System.out.println("Get Split t4:"+Molecule.splitTime5/1000000000.0);
        // System.out.println("Get Split t5:"+Molecule.splitTime6/1000000000.0);
        // System.out.println("Intersect Count:"+Molecule.Count);
    }

    public void updateVCRuleBatch_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        // TODO Auto-generated method stub
        // vni device <op,vpn4>
        for (String vni_id : vc_rules.keySet()) {
            int vniBDD = bdd_engine.encodeVNI(vni_id);
            for (String element : vc_rules.get(vni_id).keySet()) {
                FieldNodeAP e = FieldNodes.get(element);
                ArrayList<ChangeItem> changeList = new ArrayList<>();
                for (Pair<String, String> pair : vc_rules.get(vni_id).get(element)) {
                    VC_num++;
                    String port = pair.getValue();
                    // port vpn4，
                    bdd_engine.ref(vniBDD);
                    if (pair.getKey().equals("+")) {
                        e.ports.add(port);
                        e.ports_pred.put(port, NDD.NDDFalse);
                        e.ports_aps.put(port, Molecule.MFalse);
                        ChangeItem ci = new ChangeItem("default", port, vniBDD);
                        changeList.add(ci);
                    } else {
                        ChangeItem ci = new ChangeItem(port, "default", vniBDD);
                        changeList.add(ci);
                    }
                }
                if (isBatch) {
                    mergeChangeItem(changeList);
                }
                e.update_ACL(changeList);
            }
        }
    }

    public void mergeChangeItem(ArrayList<ChangeItem> changes) {
        HashMap<Pair<String, String>, Integer> map = new HashMap<>();
        for (ChangeItem change : changes) {
            Pair<String, String> pair = new Pair<String, String>(change.from_port, change.to_port);
            int delta = 0;
            if (map.containsKey(pair)) {
                delta = map.get(pair);
            }
            int mergeDelta = bdd_engine.getBDD().ref(bdd_engine.getBDD().or(delta, change.delta));
            bdd_engine.getBDD().deref(delta);
            bdd_engine.getBDD().deref(change.delta);
            map.put(pair, mergeDelta);
        }

        changes.clear();
        for (Map.Entry<Pair<String, String>, Integer> entry : map.entrySet()) {
            ChangeItem change = new ChangeItem(entry.getKey().getKey(), entry.getKey().getValue(), entry.getValue());
            changes.add(change);
        }
    }

    public void updateVNIEncapDecap_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        HashSet<ChangeItem> applyList = new HashSet<>();
        ArrayList<String> iporder = new ArrayList<>();
        for (String origin_ip : fwd_rules.keySet()) {
            if (iporder.isEmpty())
                iporder.add(origin_ip);
            else {
                boolean isInser = false;
                for (int i = 0; i < iporder.size(); i++) {
                    if (Integer.valueOf(origin_ip.split("/")[1]) < Integer.valueOf(iporder.get(i).split("/")[1])) {
                        iporder.add(i, origin_ip);
                        isInser = true;
                        break;
                    }
                }
                if (!isInser) {
                    iporder.add(origin_ip);
                }
            }
        }

        if (!isBatch) {
            for (String ipPrefix : iporder) {
                Long ipaddr = Long.valueOf(ipPrefix.split("/")[0]);
                int prefixlen = Integer.valueOf(ipPrefix.split("/")[1]);
                int ipBDD = bdd_engine.encodeDstIPPrefix(ipaddr, prefixlen);
                for (String vpn : fwd_rules.get(ipPrefix).keySet()) {
                    if (!vpn.contains("_vpn"))
                        continue;
                    if (!vni_table.containsKey(vpn))
                        continue;
                    String vni_id = vni_table.get(vpn);
                    int vniBDD = bdd_engine.encodeVNI(vni_id);
                    int field_bdd = bdd_engine.get_field_bdd("vni");
                    FieldNodeAP encap_element = FieldNodes.get(vpn + "_encapvni");
                    for (Pair<String, String> pair : fwd_rules.get(ipPrefix).get(vpn)) {
                        VNI_num++;
                        String op = pair.getKey();
                        String port = pair.getValue();
                        if (!port.equals("vxlan"))
                            continue;
                        EncapsulationVNIRule encap_r = new EncapsulationVNIRule(bdd_engine.ref(ipBDD), field_bdd,
                                bdd_engine.ref(vniBDD), vni_id, prefixlen, bdd_engine);

                        if (op.equals("+")) {
                            ArrayList<ChangeItem> changeset = encap_element.InsertRewriteRule(encap_r);
                            encap_element.update_ACL(changeset);
                        } else if (op.equals("-")) {
                            System.out.println("not implemented yet");
                        }
                    }
                }
            }
        } else {
            HashMap<String, ArrayList<ChangeItem>> allChange = new HashMap<>();
            for (String ipPrefix : iporder) {
                Long ipaddr = Long.valueOf(ipPrefix.split("/")[0]);
                int prefixlen = Integer.valueOf(ipPrefix.split("/")[1]);
                int ipBDD = bdd_engine.encodeDstIPPrefix(ipaddr, prefixlen);
                for (String vpn : fwd_rules.get(ipPrefix).keySet()) {
                    if (!vpn.contains("_vpn"))
                        continue;
                    if (!vni_table.containsKey(vpn))
                        continue;
                    String vni_id = vni_table.get(vpn);
                    int vniBDD = bdd_engine.encodeVNI(vni_id);
                    int field_bdd = bdd_engine.get_field_bdd("vni");
                    String dName = vpn + "_encapvni";
                    FieldNodeAP encap_element = FieldNodes.get(dName);
                    for (Pair<String, String> pair : fwd_rules.get(ipPrefix).get(vpn)) {
                        VNI_num++;
                        String op = pair.getKey();
                        String port = pair.getValue();
                        if (!port.equals("vxlan"))
                            continue;
                        EncapsulationVNIRule encap_r = new EncapsulationVNIRule(bdd_engine.ref(ipBDD), field_bdd,
                                bdd_engine.ref(vniBDD), vni_id, prefixlen, bdd_engine);

                        if (op.equals("+")) {
                            ArrayList<ChangeItem> changeset = encap_element.InsertRewriteRule(encap_r);
                            ArrayList<ChangeItem> arrayList = allChange.get(dName);
                            if (arrayList == null) {
                                allChange.put(dName, changeset);
                            } else {
                                arrayList.addAll(changeset);
                            }
                        } else if (op.equals("-")) {
                            System.out.println("not implemented yet");
                        }
                    }
                }
            }

            for (Map.Entry<String, ArrayList<ChangeItem>> entry : allChange.entrySet()) {
                mergeChangeItem(entry.getValue());
                FieldNodeAP encap_element = FieldNodes.get(entry.getKey());
                encap_element.update_ACL(entry.getValue());
            }
        }
    }

    public void updateEPGEncapDecap_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        for (String element : FieldNodes.keySet()) {
            if (!element.endsWith("epg"))
                continue;
            FieldNodeAP epg_element = FieldNodes.get(element);
            String vpn = element.split("_")[0] + "_" + element.split("_")[1];
            if (element.endsWith("_encapsrcepg")) {
                if (!epg_table.containsKey(vpn))
                    continue;
                if (!isBatch) {
                    for (String epg_id : epg_table.get(vpn).keySet()) {
                        int srcEPGBDD = bdd_engine.encodeSrcEPG(epg_id);
                        for (Pair<String, String> pair : epg_table.get(vpn).get(epg_id)) {
                            EPG_num++;
                            String op = pair.getKey();
                            String ipPrefix = pair.getValue();
                            long ipaddr = Long.parseLong(ipPrefix.split("/")[0]);
                            int prefixLen = Integer.parseInt(ipPrefix.split("/")[1]);
                            int srcBDD = bdd_engine.encodeSrcIPPrefix(ipaddr, prefixLen);
                            int field_bdd = bdd_engine.get_field_bdd("srcepg");
                            bdd_engine.ref(srcEPGBDD);
                            EncapsulationEPGRule encap_r = new EncapsulationEPGRule(srcBDD, field_bdd, srcEPGBDD,
                                    epg_id,
                                    65535, bdd_engine);

                            if (op.equals("+")) {
                                epg_element.update_ACL(epg_element.InsertRewriteRule(encap_r));
                            } else {
                                System.out.println("not implemented yet");
                            }
                        }
                    }
                } else {
                    ArrayList<ChangeItem> changes = new ArrayList<>();
                    for (String epg_id : epg_table.get(vpn).keySet()) {
                        int srcEPGBDD = bdd_engine.encodeSrcEPG(epg_id);
                        for (Pair<String, String> pair : epg_table.get(vpn).get(epg_id)) {
                            EPG_num++;
                            String op = pair.getKey();
                            String ipPrefix = pair.getValue();
                            long ipaddr = Long.parseLong(ipPrefix.split("/")[0]);
                            int prefixLen = Integer.parseInt(ipPrefix.split("/")[1]);
                            int srcBDD = bdd_engine.encodeSrcIPPrefix(ipaddr, prefixLen);
                            int field_bdd = bdd_engine.get_field_bdd("srcepg");
                            bdd_engine.ref(srcEPGBDD);
                            EncapsulationEPGRule encap_r = new EncapsulationEPGRule(srcBDD, field_bdd, srcEPGBDD,
                                    epg_id,
                                    65535, bdd_engine);

                            if (op.equals("+")) {
                                changes.addAll(epg_element.InsertRewriteRule(encap_r));
                            } else {
                                System.out.println("not implemented yet");
                            }
                        }
                    }
                    mergeChangeItem(changes);
                    epg_element.update_ACL(changes);
                }
            } else if (element.endsWith("_encapdstepg")) {
                if (!epg_table.containsKey(vpn))
                    continue;
                if (!isBatch) {
                    for (String epg_id : epg_table.get(vpn).keySet()) {
                        int dstEPGBDD = bdd_engine.encodeDstEPG(epg_id);
                        for (Pair<String, String> pair : epg_table.get(vpn).get(epg_id)) {
                            EPG_num++;
                            String op = pair.getKey();
                            String ipPrefix = pair.getValue();

                            long ipaddr = Long.valueOf(ipPrefix.split("/")[0]);
                            int prefixLen = Integer.valueOf(ipPrefix.split("/")[1]);

                            int dstBDD = bdd_engine.encodeDstIPPrefix(ipaddr, prefixLen);
                            int field_bdd = bdd_engine.get_field_bdd("dstepg");
                            bdd_engine.ref(dstEPGBDD);
                            EncapsulationEPGRule encap_r = new EncapsulationEPGRule(dstBDD, field_bdd, dstEPGBDD,
                                    epg_id,
                                    65535, bdd_engine);

                            if (op.equals("+")) {
                                epg_element.update_ACL(epg_element.InsertRewriteRule(encap_r));
                            } else {
                                System.out.println("not implemented yet");
                            }
                        }
                    }
                } else {
                    ArrayList<ChangeItem> changes = new ArrayList<>();
                    for (String epg_id : epg_table.get(vpn).keySet()) {
                        int dstEPGBDD = bdd_engine.encodeDstEPG(epg_id);
                        for (Pair<String, String> pair : epg_table.get(vpn).get(epg_id)) {
                            EPG_num++;
                            String op = pair.getKey();
                            String ipPrefix = pair.getValue();

                            long ipaddr = Long.valueOf(ipPrefix.split("/")[0]);
                            int prefixLen = Integer.valueOf(ipPrefix.split("/")[1]);

                            int dstBDD = bdd_engine.encodeDstIPPrefix(ipaddr, prefixLen);
                            int field_bdd = bdd_engine.get_field_bdd("dstepg");
                            bdd_engine.ref(dstEPGBDD);
                            EncapsulationEPGRule encap_r = new EncapsulationEPGRule(dstBDD, field_bdd, dstEPGBDD,
                                    epg_id,
                                    65535, bdd_engine);

                            if (op.equals("+")) {
                                changes.addAll(epg_element.InsertRewriteRule(encap_r));
                            } else {
                                System.out.println("not implemented yet");
                            }
                        }
                    }
                    mergeChangeItem(changes);
                    epg_element.update_ACL(changes);
                }
            }
        }
    }

    public void updateGBPRuleBatch_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        for (String key : gbp_rules.keySet()) {
            String srcEPG = key.split("/")[0];
            String dstEPG = key.split("/")[1];

            int srcEPGBDD = bdd_engine.encodeSrcEPG(srcEPG);
            int dstEPGBDD = bdd_engine.encodeDstEPG(dstEPG);
            int epgBDD = bdd_engine.and(srcEPGBDD, dstEPGBDD);

            for (String policy : gbp_rules.get(key).keySet()) {

                FieldNodeAP policy_element = FieldNodes.get(policy);

                for (Pair<String, String> pair : gbp_rules.get(key).get(policy)) {
                    GBP_num++;
                    String op = pair.getKey();
                    String port = pair.getValue();

                    ACLRule rule = new ACLRule();
                    rule.setType(port);
                    rule.set_priority(65535);

                    bdd_engine.ref(epgBDD);
                    if (port.equals("permit")) {
                        rule.insert_vals(epgBDD, epgBDD, 0);
                    } else if (port.equals("deny")) {
                        rule.insert_vals(epgBDD, 0, epgBDD);
                    }

                    if (op.equals("+")) {
                        policy_element.update_ACL(policy_element.InsertACLRuleBDD(rule));
                    } else if (op.equals("-")) {
                        System.out.println("not implemented yet");
                    }
                }
            }
            bdd_engine.DerefInBatch(new int[] { srcEPGBDD, dstEPGBDD, epgBDD });
        }
    }

    public void updatePBRRuleBatch_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        for (String element : acl_rules.keySet()) {
            for (ACLRule rule : acl_rules.get(element)) {
                PBR_num++;
                FieldNodeAP e = FieldNodes.get(element);
                change_set = e.InsertACLRule(rule);
                e.update_ACL(change_set);
            }
        }
    }

    public void split_ap_one_field(HashMap<Integer, HashSet<Integer>> split_ap, int field) {
        if (split_ap.size() == 0)
            return;

        for (Map.Entry<Integer, HashSet<Integer>> entry : split_ap.entrySet()) {
            Molecule.table.getAP(field).remove(entry.getKey());
            Molecule.table.getAP(field).addAll(entry.getValue());
            bdd_engine.getBDD().deref(entry.getKey());
        }

        HashSet<Pair<String, String>> finished = new HashSet<>();
        HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = splitMap.ap_ports[field];
        HashMap<Integer, HashSet<Integer>> apToSplit = split_ap;
        for (int ap : apToSplit.keySet()) {
            if (ap == 1)
                continue;
            HashSet<Pair<String, String>> ports = sub_ap_ports.get(ap);
            for (Pair<String, String> p : ports) {
                if (!finished.contains(p)) {
                    FieldNodeAP device = FieldNodes.get(p.getKey());
                    if (device.field != multi_filed && device.field != no_field) {
                        Molecule aps = device.ports_aps.get(p.getValue());
                        Molecule new_aps = Molecule.split_port_ap_1_1(split_ap, aps);
                        if (!new_aps.is_False()) {
                            Molecule.table.ref(new_aps);
                            Molecule.table.deref(aps);
                            device.ports_aps.put(p.getValue(), new_aps);
                        }
                    } else {
                        Molecule aps = device.ports_aps.get(p.getValue());
                        Pair<Boolean, Molecule> ret = Molecule.split_port_ap_1_n(split_ap, aps, field);
                        if (ret.getKey()) {
                            Molecule.table.ref(ret.getValue());
                            Molecule.table.deref(aps);
                            device.ports_aps.put(p.getValue(), ret.getValue());
                        }
                    }
                    finished.add(p);
                }
            }
        }

        if (field != src_ip_field && field != dst_ip_field) {
            finished = new HashSet<>();
            sub_ap_ports = splitMapAction.ap_ports[field];
            for (int ap : apToSplit.keySet()) {
                if (ap == 1)
                    continue;
                HashSet<Pair<String, String>> ports = sub_ap_ports.get(ap);
                for (Pair<String, String> p : ports) {
                    if (!finished.contains(p)) {
                        FieldNodeAP device = FieldNodes.get(p.getKey());
                        Molecule aps = device.ports_action_aps.get(p.getValue());
                        Molecule new_aps = Molecule.split_port_ap_1_1(split_ap, aps);
                        if (!new_aps.is_False()) {
                            Molecule.table.ref(new_aps);
                            Molecule.table.deref(aps);
                            device.ports_action_aps.put(p.getValue(), new_aps);
                        }
                        finished.add(p);
                    }
                }
            }
        }

        splitMap.split(split_ap, field);
        splitMapAction.split(split_ap, field);
    }

    public void split_ap_multi_field(ArrayList<HashMap<Integer, HashSet<Integer>>> split_ap) {
        boolean empty = true;
        for (int curr = 0; curr < field_num; curr++) {
            if (split_ap.get(curr).size() > 0) {
                empty = false;
            }
        }
        if (empty)
            return;

        for (int curr_field = 0; curr_field < field_num; curr_field++) {
            for (Map.Entry<Integer, HashSet<Integer>> entry : split_ap.get(curr_field).entrySet()) {
                Molecule.table.getAP(curr_field).remove(entry.getKey());
                Molecule.table.getAP(curr_field).addAll(entry.getValue());
                bdd_engine.getBDD().deref(entry.getKey());
            }
        }

        HashSet<Pair<String, String>> finished = new HashSet<>();
        for (int field = 0; field < field_num; field++) {
            HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = splitMap.ap_ports[field];
            HashMap<Integer, HashSet<Integer>> apToSplit = split_ap.get(field);
            for (int ap : apToSplit.keySet()) {
                if (ap == 1)
                    continue;
                HashSet<Pair<String, String>> ports = sub_ap_ports.get(ap);
                for (Pair<String, String> port : ports) {
                    if (!finished.contains(port)) {
                        FieldNodeAP device = FieldNodes.get(port.getKey());
                        Molecule moleculeToSplit = device.ports_aps.get(port.getValue());
                        Pair<Boolean, Molecule> ret = Molecule.split_port_ap_n_n(split_ap, moleculeToSplit);
                        if (ret.getKey()) {
                            Molecule.table.ref(ret.getValue());
                            Molecule.table.deref(moleculeToSplit);
                            device.ports_aps.put(port.getValue(), ret.getValue());
                        }
                        finished.add(port);
                    }
                }
            }
        }

        for (int field = 0; field < field_num; field++) {
            if (field == src_ip_field || field == dst_ip_field)
                continue;
            finished = new HashSet<>();
            HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = splitMapAction.ap_ports[field];
            HashMap<Integer, HashSet<Integer>> apToSplit = split_ap.get(field);
            for (int ap : apToSplit.keySet()) {
                if (ap == 1)
                    continue;
                HashSet<Pair<String, String>> ports = sub_ap_ports.get(ap);
                for (Pair<String, String> p : ports) {
                    if (!finished.contains(p)) {
                        FieldNodeAP device = FieldNodes.get(p.getKey());
                        Molecule aps = device.ports_action_aps.get(p.getValue());
                        Pair<Boolean, Molecule> ret = Molecule.split_port_ap_n_n(split_ap, aps);
                        if (ret.getKey()) {
                            Molecule.table.ref(ret.getValue());
                            Molecule.table.deref(aps);
                            device.ports_action_aps.put(p.getValue(), ret.getValue());
                        }
                        finished.add(p);
                    }
                }
            }
        }

        splitMap.split(split_ap);
        splitMapAction.split(split_ap);
    }

    public void UpdateFieldAP() {
        HashSet<NDD> preds = new HashSet<>();
        for (FieldNodeAP device : FieldNodes.values()) {
            for (NDD pred : device.ports_pred.values()) {
                preds.add(pred);
            }
            if (device.type == 2) {
                if (device.actionField == no_field)
                    continue;
                for (NDD pred : device.ports_action_pred.values()) {
                    preds.add(pred);
                }
            }
        }

        HashMap<NDD, HashSet<Integer>[]> ndd_aps = new HashMap<>();
        HashMap<NDD, Molecule> ndd_mol = Molecule.table.Atomization(preds, ndd_aps);

        splitMap.clear();
        splitMapAction.clear();
        HashMap<Integer, HashSet<Pair<String, String>>>[] ap_ports = splitMap.ap_ports;
        HashMap<Integer, HashSet<Pair<String, String>>>[] ap_ports_action = splitMapAction.ap_ports;

        for (FieldNodeAP device : FieldNodes.values()) {
            String dName = device.name;
            HashSet<Molecule> oldSet = new HashSet<>(device.ports_aps.values());
            for (Map.Entry<String, NDD> entry : device.ports_pred.entrySet()) {
                String pName = entry.getKey();
                if (pName.equals("default") || pName.equals("deny")) // default and deny
                    continue;
                device.ports_aps.put(entry.getKey(), Molecule.table.ref(ndd_mol.get(entry.getValue())));
                HashSet<Integer>[] apSet = ndd_aps.get(entry.getValue());
                for (int field = 0; field < NDD.fieldNum; field++) {
                    HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = ap_ports[field];
                    for (int ap : apSet[field]) {
                        HashSet<Pair<String, String>> ports = sub_ap_ports.get(ap);
                        if (ports == null) {
                            ports = new HashSet<>();
                            ports.add(new Pair<String, String>(dName, pName));
                            sub_ap_ports.put(ap, ports);
                        } else {
                            ports.add(new Pair<String, String>(dName, pName));
                        }
                    }
                }
            }
            for (Molecule molecule : oldSet) {
                Molecule.table.deref(molecule);
            }
            if (device.type == 2) {
                if (device.actionField == no_field)
                    continue;
                HashSet<Molecule> oldSetAction = new HashSet<>(device.ports_action_aps.values());
                for (Map.Entry<String, NDD> entry : device.ports_action_pred.entrySet()) {
                    String pName = entry.getKey();
                    if (pName.equals("default") || pName.equals("deny")) // default and deny
                        continue;
                    Molecule MPred = Molecule.MTrue;
                    if (!entry.getValue().is_True()) {
                        MPred = Molecule.table.ref(ndd_mol.get(entry.getValue()));
                        HashSet<Integer>[] apSet = ndd_aps.get(entry.getValue());
                        HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = ap_ports_action[device.actionField];
                        for (int ap : apSet[device.actionField]) {
                            HashSet<Pair<String, String>> ports = sub_ap_ports.get(ap);
                            if (ports == null) {
                                ports = new HashSet<>();
                                ports.add(new Pair<String, String>(dName, pName));
                                sub_ap_ports.put(ap, ports);
                            } else {
                                ports.add(new Pair<String, String>(dName, pName));
                            }
                        }
                    }
                    device.ports_action_aps.put(entry.getKey(), MPred);
                }
                for (Molecule molecule : oldSetAction) {
                    Molecule.table.deref(molecule);
                }
            }
        }
        for (int i = 0; i < field_num; i++) {
            if (Molecule.table.getAP(i).size() == 1) {
                ap_ports[i].put(1, new HashSet<>());
                if (i <= 1)
                    ap_ports_action[i].put(1, new HashSet<>());
            } else {
                if (i != src_ip_field && i != dst_ip_field) {
                    for (int ap : Molecule.table.getAP(i)) {
                        if (!ap_ports_action[i].containsKey(ap)) {
                            ap_ports_action[i].put(ap, new HashSet<>());
                        }
                    }
                }
                for (int ap : Molecule.table.getAP(i)) {
                    if (!ap_ports[i].containsKey(ap)) {
                        ap_ports[i].put(ap, new HashSet<>());
                    }
                }
            }
        }
    }

    public HashSet<PositionTuple> LinkTransfer(PositionTuple pt) {
        FieldNode e = FieldNodes.get(pt.getDeviceName());
        if (e.type == 2) {
            return topology.get(new PositionTuple(e.name, "default"));
        } else {
            return topology.get(pt);
        }
    }

    public void processDifferentialUpdate(String pre_state, String curr_state) throws Exception {
        // System.out.println();
        // System.out.println("update: " + pre_state + " to " + curr_state);
        pw.println();
        pw.flush();
        pw.println("update: " + pre_state + " to " + curr_state);
        pw.flush();
        // time1 = 0;
        // time2 = 0;
        // time3 = 0;
        // time4 = 0;
        // time5 = 0;

        affected_aps = Molecule.MFalse;
        currStage = curr_state;
        double t1 = System.nanoTime();
        updatePPMFromFile(pre_state, curr_state);
        double t2 = System.nanoTime();
        // System.out.println("parse file:" + (t2 - t1) / 1000000000);
        pw.println("parse file:" + (t2 - t1) / 1000000000);
        pw.flush();
        updatePPMSnapshot_Increasement();
        double t3 = System.nanoTime();
        // System.out.println("update model:" + (t3 - t2) / 1000000000);
        pw.println("update model:" + (t3 - t2) / 1000000000);
        pw.flush();

        model_realtime += (t3 - t2) / 1000000000;

        affected_aps = Molecule.MTrue;
        Molecule.useCache = true;
        checkerV2 = new CheckerAP_V2(this, new HashSet<String>(), new HashMap<>());
        checkerV2.PropertyCheck();
        pw.println("reach size:" + checkerV2.reach.size());
        pw.flush();
        Molecule.useCache = false;

        double t4 = System.nanoTime();
        check_realtime += (t4 - t3) / 1000000000;

        pw.println("property check:" + (t4 - t3) / 1000000000);
        pw.flush();
        pw.println("total:" + (t4 - t2) / 1000000000);
        pw.flush();

        int sum = 0;
        for (int i = 0; i < field_num; i++) {
            sum += Molecule.table.getAP(i).size();
            pw.println("field" + i + " " + Molecule.table.getAP(i).size());
            pw.flush();
        }
        pw.println("AP:" + sum);
        pw.flush();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(args[0] + " " + args[1]);
        orderPlan = Integer.parseInt(args[1]);

        fw = new FileWriter(path + "/DCN_NDD/output/NDD_AP/" + args[0] + "_order"
                + orderPlan + ".txt", false);
        pw = new PrintWriter(fw);

        String mode = "normal";

        Runtime r = Runtime.getRuntime();
        r.gc();
        r.gc();
        long m1 = r.totalMemory() - r.freeMemory();

        FDNNetworkAP_V2 n = new FDNNetworkAP_V2(args[0], mode, "base", args[0]);

        Runtime r1 = Runtime.getRuntime();
        r1.gc();
        r1.gc();
        long m2 = r1.totalMemory() - r1.freeMemory();

        pw.println("memory:" + (m2 - m1) / (1024 * 1024) + "MB");
        pw.flush();

        if (runOriginDataset) {
            n.processDifferentialUpdate("base", "post0");
            n.processDifferentialUpdate("post0", "post1");
            n.processDifferentialUpdate("post1", "post2");
            n.processDifferentialUpdate("post2", "post3");
            n.processDifferentialUpdate("post3", "post4");
        }
    }
}