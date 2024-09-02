package apkeep.FDN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import NDD.NDD;
import apkeep.FDN.FDNNetworkAP_V2;
import apkeep.FDN.FieldNode;
import apkeep.FDN.RewriteRule;
import apkeep.core.ChangeItem;
import apkeep.utils.ChangeTuple;
import common.ACLRule;
import javafx.util.Pair;
import molecule.Molecule;

public class FieldNodeAP extends FieldNode {
    public static FDNNetworkAP_V2 network = null;
    public HashMap<String, Molecule> ports_aps;
    public HashMap<String, NDD> ports_action_pred;
    public HashMap<String, Molecule> ports_action_aps;

    public FieldNodeAP(String ename, FDNNetworkAP_V2 net, int type, boolean is_decap, int field, int actionField) {
        super(ename, net, type, is_decap, field, actionField);
        network = net;
        ports_aps = new HashMap<>();
        if (type == 0) {
            // ports_aps.put("default", Molecule.MTrue); //default and deny
        } else if (type == 1) {
            ports_aps.put("permit", Molecule.MTrue);
        } else if (type == -1) {
            // ports_aps.put("deny", Molecule.MTrue); //default and deny
        } else if (type == 2) {
            // ports_aps.put("default", Molecule.MTrue); //default and deny
        }
        ports_action_pred = new HashMap<>();
        ports_action_aps = new HashMap<>();
    }

    public void updateFWRuleBatch(String ip, HashSet<String> to_ports, HashSet<String> from_ports,
            HashMap<String, ArrayList<ChangeTuple>> change_set, HashMap<String, ArrayList<ChangeTuple>> copyto_set,
            HashMap<String, ArrayList<ChangeTuple>> remove_set) {
        for (String port : to_ports) {
            if (!port.equals("default") && !ports.contains(port)) { // default and deny
                ports_aps.put(port, Molecule.MFalse);
            }
        }
        super.updateFWRuleBatch(ip, to_ports, from_ports, change_set, copyto_set, remove_set);
    }

    public ArrayList<ChangeItem> InsertRewriteRule(RewriteRule rule) {
        if (!rule.name.equals("default") && !ports.contains(rule.name)) { // default and deny
            ports.add(rule.name);
            ports_pred.put(rule.name, NDD.NDDFalse);
            ports_aps.put(rule.name, Molecule.MFalse);
            ports_action_pred.put(rule.name, rule.new_val_ndd);
            Molecule action = Molecule.MTrue;
            if (!rule.new_val_ndd.is_True()) {
                HashSet<Integer> delta_aps = new HashSet<>();
                HashMap<Integer, HashSet<Integer>> split_ap = new HashMap<>();
                Molecule.get_split_ap_one_field(rule.new_val_ndd, delta_aps, split_ap, actionField);
                HashMap<Molecule, HashSet<Integer>> tempMap = new HashMap<>();
                tempMap.put(action, delta_aps);
                action = Molecule.table.ref(Molecule.table.mk(actionField, tempMap));
                network.split_ap_one_field(split_ap, actionField);
                for (int ap : delta_aps) {
                    network.splitMapAction.ap_ports[actionField].get(ap).add(new Pair<>(name, rule.name));
                }
            }
            ports_action_aps.put(rule.name, action);
        }

        return super.InsertRewriteRule(rule);
    }

    public ArrayList<ChangeItem> InsertACLRuleBDD(ACLRule rule) {
        if (!rule.get_type().equals("deny") && !ports.contains(rule.get_type())) { // default and deny
            ports_aps.put(rule.get_type(), Molecule.MFalse);
        }

        return super.InsertACLRuleBDD(rule);
    }

    public ArrayList<ChangeItem> InsertACLRule(ACLRule rule) {
        if (!rule.get_type().equals("deny") && !ports.contains(rule.get_type())) { // default and deny
            ports_aps.put(rule.get_type(), Molecule.MFalse);
        }

        return super.InsertACLRule(rule);
    }

    public void update_FW(ArrayList<ChangeTuple> change_set, ArrayList<ChangeTuple> copyto_set) {
        for (ChangeTuple item : change_set) {
            int sum = 0;
            for (int abdd : item.delta_set) {
                int temp = bdd.getBDD().ref(bdd.getBDD().or(sum, abdd));
                bdd.getBDD().deref(sum);
                sum = temp;
            }
            NDD delta = NDD.table.ref(NDD.toNDD(sum));
            bdd.getBDD().deref(sum);
            for (String from_port : item.from_ports) {
                NDD oldPred = ports_pred.get(from_port);
                NDD newPred = NDD.table.ref(NDD.Diff(oldPred, delta));
                NDD.table.deref(oldPred);
                ports_pred.put(from_port, newPred);
            }
            for (String to_port : item.to_ports) {
                NDD oldPred = ports_pred.get(to_port);
                NDD newPred = NDD.table.ref(NDD.OR(oldPred, delta));
                NDD.table.deref(oldPred);
                ports_pred.put(to_port, newPred);
            }

            HashSet<Integer> delta_aps = new HashSet<>();
            HashMap<Integer, HashSet<Integer>> split_ap = new HashMap<>();
            int d = 0;
            if (delta.is_True()) {
                d = 1;
            } else if (delta.is_False()) {
                d = 0;
            } else {
                for (int pred : delta.edges.values()) {
                    d = pred;
                }
            }
            for (String from_port : item.from_ports) {
                Molecule from_aps = Molecule.MTrue; // default and deny
                if (!from_port.equals("default") && !from_port.equals("deny"))
                    from_aps = ports_aps.get(from_port);
                Molecule.get_split_ap_one_field(from_aps, d, delta_aps, split_ap, field);
                network.split_ap_one_field(split_ap, field);
                break;
            }

            Molecule moved_aps = Molecule.MFalse;
            if (delta_aps.size() == Molecule.table.getAP(field).size()) {
                moved_aps = Molecule.MTrue;
            } else if (delta_aps.size() == 0) {
                moved_aps = Molecule.MFalse;
            } else {
                HashMap<Molecule, HashSet<Integer>> tempMap = new HashMap<>();
                tempMap.put(Molecule.MTrue, delta_aps);
                moved_aps = Molecule.table.mk(field, tempMap);
                Molecule.table.ref(moved_aps);
            }

            for (String from_port : item.from_ports) {
                if (from_port.equals("default") || from_port.equals("deny")) // default and deny
                    continue;
                Molecule old_from_aps = ports_aps.get(from_port);
                Molecule new_from_aps = Molecule.table.ref(Molecule.Diff(old_from_aps, moved_aps));
                HashSet<Integer> toRemove = new HashSet<>();
                HashSet<Integer> toAdd = new HashSet<>();
                Molecule.ChangedAPsSingleField(old_from_aps, new_from_aps, toRemove, toAdd);
                Molecule.table.deref(old_from_aps);
                ports_aps.put(from_port, new_from_aps);

                HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = network.splitMap.ap_ports[field];
                for (int ap : toRemove) {
                    sub_ap_ports.get(ap).remove(new Pair<>(name, from_port));
                }
                for (int ap : toAdd) {
                    sub_ap_ports.get(ap).add(new Pair<>(name, from_port));
                }
            }
            for (String to_port : item.to_ports) {
                if (to_port.equals("default") || to_port.equals("deny")) // default and deny
                    continue;
                Molecule old_to_aps = ports_aps.get(to_port);
                if (old_to_aps == null) {
                    old_to_aps = Molecule.MFalse;
                }
                Molecule new_to_aps = Molecule.table.ref(Molecule.OR(old_to_aps, moved_aps));
                HashSet<Integer> toRemove = new HashSet<>();
                HashSet<Integer> toAdd = new HashSet<>();
                Molecule.ChangedAPsSingleField(old_to_aps, new_to_aps, toRemove, toAdd);
                Molecule.table.deref(old_to_aps);
                ports_aps.put(to_port, new_to_aps);

                HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = network.splitMap.ap_ports[field];
                for (int ap : toRemove) {
                    sub_ap_ports.get(ap).remove(new Pair<>(name, to_port));
                }
                for (int ap : toAdd) {
                    sub_ap_ports.get(ap).add(new Pair<>(name, to_port));
                }
            }
            Molecule.table.deref(moved_aps);

            NDD.table.deref(delta);
        }
        for (ChangeTuple item : copyto_set) {
            int sum = 0;
            for (int abdd : item.delta_set) {
                int temp = bdd.getBDD().ref(bdd.getBDD().or(sum, abdd));
                bdd.getBDD().deref(sum);
                sum = temp;
            }
            NDD delta = NDD.table.ref(NDD.toNDD(sum));
            bdd.getBDD().deref(sum);
            for (String to_port : item.to_ports) {
                NDD oldPred = ports_pred.get(to_port);
                NDD newPred = NDD.table.ref(NDD.OR(oldPred, delta));
                NDD.table.deref(oldPred);
                ports_pred.put(to_port, newPred);
            }

            HashSet<Integer> delta_aps = new HashSet<>();
            HashMap<Integer, HashSet<Integer>> split_ap = new HashMap<>();
            int d = 0;
            if (delta.is_True()) {
                d = 1;
            } else if (delta.is_False()) {
                d = 0;
            } else {
                for (int pred : delta.edges.values()) {
                    d = pred;
                }
            }
            Molecule from_aps = Molecule.MTrue;
            Molecule.get_split_ap_one_field(from_aps, d, delta_aps, split_ap, field);
            network.split_ap_one_field(split_ap, field);

            HashSet<Integer> apSet = new HashSet<>(delta_aps);
            Molecule moved_aps = Molecule.MFalse;
            if (delta_aps.size() == Molecule.table.getAP(field).size()) {
                moved_aps = Molecule.MTrue;
            } else if (delta_aps.size() == 0) {
                moved_aps = Molecule.MFalse;
            } else {
                HashMap<Molecule, HashSet<Integer>> tempMap = new HashMap<>();
                tempMap.put(Molecule.MTrue, delta_aps);
                moved_aps = Molecule.table.mk(field, tempMap);
            }
            for (String to_port : item.to_ports) {
                if (to_port.equals("default") || to_port.equals("deny")) // default and deny
                    continue;
                Molecule old_to_aps = ports_aps.get(to_port);
                if (old_to_aps == null) {
                    old_to_aps = Molecule.MFalse;
                }
                Molecule new_to_aps = Molecule.table.ref(Molecule.OR(old_to_aps, moved_aps));
                Molecule.table.deref(old_to_aps);
                ports_aps.put(to_port, new_to_aps);

                HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = network.splitMap.ap_ports[field];
                for (int ap : apSet) {
                    sub_ap_ports.get(ap).add(new Pair<>(name, to_port));
                }
            }
            Molecule.table.testAndDelete(moved_aps);
            NDD.table.deref(delta);
        }
    }
    
    public static boolean testVxLAN = false;
    public static Long Time0 = 0L;
    public static Long Time1 = 0L;
    public static Long Time2 = 0L;
    public static Long Time3 = 0L;
    public static Long Time4 = 0L;
    public static int Count1 = 0;
    public static int Count2 = 0;
    public void update_ACL(HashMap<String, Integer> remove, HashMap<String, Integer> add) {
        for (Entry<String, Integer> entry : remove.entrySet()) {
            String from_port = entry.getKey();
            NDD delta = NDD.table.ref(NDD.toNDD(entry.getValue()));

            NDD oldPred = ports_pred.get(from_port);
            NDD newPred = NDD.table.ref(NDD.Diff(oldPred, delta));
            ports_pred.put(from_port, newPred);
            NDD.table.deref(oldPred);

            ArrayList<int[]> change_bdd = NDD.ToArray(delta);
            for (int[] bdd_vec : change_bdd) {
                // if(testVxLAN)Count1++;
                // update aps
                ArrayList<HashSet<Integer>> delta_aps = new ArrayList<>();
                ArrayList<HashMap<Integer, HashSet<Integer>>> split_ap = new ArrayList<>();
                for (int curr = 0; curr < NDD.fieldNum; curr++) {
                    delta_aps.add(new HashSet<>());
                    split_ap.add(new HashMap<>());
                }
                Molecule from_aps = Molecule.MTrue; // default and deny
                if (!from_port.equals("default") && !from_port.equals("deny"))
                    from_aps = ports_aps.get(from_port);
                Molecule.get_split_ap_multi_field(from_aps, bdd_vec, delta_aps, split_ap, 0);
                // split
                network.split_ap_multi_field(split_ap);
                // transfer
                if (!from_port.equals("default") && !from_port.equals("deny")) // default and deny
                {
                    Molecule moved_aps = Molecule.MTrue;
                    for (int curr_field = NDD.fieldNum - 1; curr_field >= 0; curr_field--) {
                        if (delta_aps.get(curr_field).size() == Molecule.table.getAP(curr_field).size()) {
                            continue;
                        }
                        HashMap<Molecule, HashSet<Integer>> tempMap = new HashMap<>();
                        tempMap.put(moved_aps, delta_aps.get(curr_field));
                        moved_aps = Molecule.table.mk(curr_field, tempMap);
                    }
                    Molecule.table.ref(moved_aps);
                    Molecule old_from_aps = ports_aps.get(from_port);
                    Molecule new_from_aps = Molecule.table.ref(Molecule.Diff(old_from_aps, moved_aps));
                    Molecule.table.deref(old_from_aps);
                    ports_aps.put(from_port, new_from_aps);
    
                    HashSet<Integer>[] toAdd = new HashSet[Molecule.fieldNum];
                    HashSet<Integer>[] toRemove = new HashSet[Molecule.fieldNum];
                    Molecule.ChangedAPs(old_from_aps, new_from_aps, toRemove, toAdd);
                    for (int field = 0; field < NDD.fieldNum; field++) {
                        HashSet<Integer> removeAP = toRemove[field];
                        HashSet<Integer> addAP = toAdd[field];
                        HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = network.splitMap.ap_ports[field];
                        for (int ap : removeAP) {
                            sub_ap_ports.get(ap).remove(new Pair<>(name, from_port));
                        }
                        for (int ap : addAP) {
                            sub_ap_ports.get(ap).add(new Pair<>(name, from_port));
                        }
                    }
                    Molecule.table.deref(moved_aps);
                }
            }
            NDD.table.deref(delta);
        }

        for (Entry<String, Integer> entry : add.entrySet()) {
            // Long t0 = System.nanoTime();
            String to_port = entry.getKey();
            NDD delta = NDD.table.ref(NDD.toNDD(entry.getValue()));
            NDD oldPred = ports_pred.get(to_port);
            NDD newPred = NDD.table.ref(NDD.OR(oldPred, delta));
            ports_pred.put(to_port, newPred);
            NDD.table.deref(oldPred);
            // Long t1 = System.nanoTime();

            ArrayList<int[]> change_bdd = NDD.ToArray(delta);

            // Long t2 = System.nanoTime();

            // if(testVxLAN)
            // {
            //     Time0 += t1-t0;
            //     Time1 += t2-t1;
            // }

            for (int[] bdd_vec : change_bdd) {
                // if(testVxLAN)Count2++;
                // update aps
                ArrayList<HashSet<Integer>> delta_aps = new ArrayList<>();
                ArrayList<HashMap<Integer, HashSet<Integer>>> split_ap = new ArrayList<>();
                for (int curr = 0; curr < NDD.fieldNum; curr++) {
                    delta_aps.add(new HashSet<>());
                    split_ap.add(new HashMap<>());
                }
                Molecule from_aps = Molecule.MTrue;
                // Long t3 = System.nanoTime();
                // if(testVxLAN)System.out.println(bdd_vec[0]+" "+bdd_vec[1]+" "+bdd_vec[2]+" "+bdd_vec[3]+" "+bdd_vec[4]+" "+bdd_vec[5]);
                Molecule.get_split_ap_multi_field(from_aps, bdd_vec, delta_aps, split_ap, 0);
                // Long t4 = System.nanoTime();

                // split
                network.split_ap_multi_field(split_ap);
                // Long t5 = System.nanoTime();

                // transfer
                if (!to_port.equals("default") && !to_port.equals("deny")) // default and deny
                {
                    Molecule moved_aps = Molecule.MTrue;
                    for (int curr_field = NDD.fieldNum - 1; curr_field >= 0; curr_field--) {
                        if (delta_aps.get(curr_field).size() == Molecule.table.getAP(curr_field).size()) {
                            continue;
                        }
                        HashMap<Molecule, HashSet<Integer>> tempMap = new HashMap<>();
                        tempMap.put(moved_aps, delta_aps.get(curr_field));
                        moved_aps = Molecule.table.mk(curr_field, tempMap);
                    }
                    Molecule.table.ref(moved_aps);
    
                    Molecule old_to_aps = ports_aps.get(to_port);
                    Molecule new_to_aps = Molecule.table.ref(Molecule.OR(old_to_aps, moved_aps));
                    Molecule.table.deref(old_to_aps);
                    ports_aps.put(to_port, new_to_aps);
    
                    HashSet<Integer>[] toAdd = new HashSet[Molecule.fieldNum];
                    HashSet<Integer>[] toRemove = new HashSet[Molecule.fieldNum];
                    Molecule.ChangedAPs(old_to_aps, new_to_aps, toRemove, toAdd);
    
                    for (int field = 0; field < NDD.fieldNum; field++) {
                        HashSet<Integer> removeAP = toRemove[field];
                        HashSet<Integer> addAP = toAdd[field];
                        HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = network.splitMap.ap_ports[field];
                        for (int ap : removeAP) {
                            sub_ap_ports.get(ap).remove(new Pair<>(name, to_port));
                        }
                        for (int ap : addAP) {
                            sub_ap_ports.get(ap).add(new Pair<>(name, to_port));
                        }
                    }
                    Molecule.table.deref(moved_aps);
                }
                // Long t6 = System.nanoTime();
                // if(testVxLAN)
                // {
                //     Time2 += t4-t3;
                //     Time3 += t5-t4;
                //     Time4 += t6-t5;
                // }

            }
            NDD.table.deref(delta);
        }
    }

    public void update_ACL(ArrayList<ChangeItem> change_set) {
        if (change_set.size() == 0) {
            return;
        }
        for (ChangeItem item : change_set) {
            String from_port = item.from_port;
            String to_port = item.to_port;
            if (from_port.contains("copy"))
                System.out.println("tag");
            NDD delta = NDD.table.ref(NDD.toNDD(item.delta));

            NDD oldFrom = ports_pred.get(from_port);
            NDD newFrom = NDD.table.ref(NDD.Diff(oldFrom, delta));
            ports_pred.put(from_port, newFrom);
            NDD oldTo = ports_pred.get(to_port);
            NDD newTo = NDD.table.ref(NDD.OR(oldTo, delta));
            ports_pred.put(to_port, newTo);
            NDD.table.deref(oldFrom);
            NDD.table.deref(oldTo);

            ArrayList<int[]> change_bdd = NDD.ToArray(delta);

            for (int[] bdd_vec : change_bdd) {
                // update aps
                ArrayList<HashSet<Integer>> delta_aps = new ArrayList<>();
                ArrayList<HashMap<Integer, HashSet<Integer>>> split_ap = new ArrayList<>();
                for (int curr = 0; curr < NDD.fieldNum; curr++) {
                    delta_aps.add(new HashSet<>());
                    split_ap.add(new HashMap<>());
                }
                Molecule from_aps = Molecule.MTrue; // default and deny
                if (!from_port.equals("default") && !from_port.equals("deny"))
                    from_aps = ports_aps.get(from_port);
                Molecule.get_split_ap_multi_field(from_aps, bdd_vec, delta_aps, split_ap, 0);

                // split
                network.split_ap_multi_field(split_ap);

                // transfer
                Molecule moved_aps = Molecule.MTrue;
                for (int curr_field = NDD.fieldNum - 1; curr_field >= 0; curr_field--) {
                    if (delta_aps.get(curr_field).size() == Molecule.table.getAP(curr_field).size()) {
                        continue;
                    }
                    HashMap<Molecule, HashSet<Integer>> tempMap = new HashMap<>();
                    tempMap.put(moved_aps, delta_aps.get(curr_field));
                    moved_aps = Molecule.table.mk(curr_field, tempMap);
                }
                Molecule.table.ref(moved_aps);

                if (!from_port.equals("default") && !from_port.equals("deny")) // default and deny
                {
                    Molecule old_from_aps = ports_aps.get(from_port);
                    Molecule new_from_aps = Molecule.table.ref(Molecule.Diff(old_from_aps, moved_aps));
                    Molecule.table.deref(old_from_aps);
                    ports_aps.put(from_port, new_from_aps);

                    HashSet<Integer>[] toAdd = new HashSet[Molecule.fieldNum];
                    HashSet<Integer>[] toRemove = new HashSet[Molecule.fieldNum];
                    Molecule.ChangedAPs(old_from_aps, new_from_aps, toRemove, toAdd);
    
                    for (int field = 0; field < NDD.fieldNum; field++) {
                        HashSet<Integer> removeAP = toRemove[field];
                        HashSet<Integer> addAP = toAdd[field];
                        HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = network.splitMap.ap_ports[field];
                        for (int ap : removeAP) {
                            sub_ap_ports.get(ap).remove(new Pair<>(name, from_port));
                        }
                        for (int ap : addAP) {
                            sub_ap_ports.get(ap).add(new Pair<>(name, from_port));
                        }
                    }
                }

                if (!to_port.equals("default") && !to_port.equals("deny")) // default and deny
                {
                    Molecule old_to_aps = ports_aps.get(to_port);
                    Molecule new_to_aps = Molecule.table.ref(Molecule.OR(old_to_aps, moved_aps));
                    Molecule.table.deref(old_to_aps);
                    ports_aps.put(to_port, new_to_aps);
    
                    HashSet<Integer>[] toAdd = new HashSet[Molecule.fieldNum];
                    HashSet<Integer>[] toRemove = new HashSet[Molecule.fieldNum];
                    Molecule.ChangedAPs(old_to_aps, new_to_aps, toRemove, toAdd);
    
                    for (int field = 0; field < NDD.fieldNum; field++) {
                        HashSet<Integer> removeAP = toRemove[field];
                        HashSet<Integer> addAP = toAdd[field];
                        HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = network.splitMap.ap_ports[field];
                        for (int ap : removeAP) {
                            sub_ap_ports.get(ap).remove(new Pair<>(name, to_port));
                        }
                        for (int ap : addAP) {
                            sub_ap_ports.get(ap).add(new Pair<>(name, to_port));
                        }
                    }
                }

                Molecule.table.deref(moved_aps);
            }

            NDD.table.deref(delta);
        }
    }
}