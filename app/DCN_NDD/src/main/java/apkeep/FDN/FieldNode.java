package apkeep.FDN;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import NDD.NDD;
import apkeep.core.BDDRuleItem;
import apkeep.core.ChangeItem;
// import apkeep.core.Network;
import apkeep.utils.*;
import common.*;
import javafx.util.*;
import jdd.bdd.BDD;

public class FieldNode {
    public static FDNNetwork network = null;
    public static BDDACLWrapper bdd = null;
    public String name;
    public int type;
    public int field;
    public int actionField;
    public LinkedList<BDDRuleItem<ForwardingRule>> fw_rule; // fw
    public LinkedList<BDDRuleItem<ACLRule>> acl_rule; // acl
    public TrieTree trie; // fw
    public LinkedList<BDDRuleItem<RewriteRule>> rewrite_rules;// rewrite
    public HashMap<String, HashSet<RewriteRule>> rule_map;
    public HashSet<String> ports;
    public HashMap<String, NDD> ports_pred;

    public FieldNode() {
    }

    public FieldNode(String ename, FDNNetwork net, int type, boolean is_decap, int field, int actionField) {
        this.name = ename;
        this.type = type;
        this.field = field;
        this.actionField = actionField;
        ports = new HashSet<String>();
        ports_pred = new HashMap<String, NDD>();
        if (type == 0) {
            trie = new TrieTree();
            fw_rule = new LinkedList<BDDRuleItem<ForwardingRule>>();
        } else if (type == 1) {
            ACLRule rule = new ACLRule();
            rule.permitDeny = "permit";
            // rule.permitDeny = "deny";
            rule.priority = -1;
            BDDRuleItem<ACLRule> new_rule = new BDDRuleItem<ACLRule>(rule, bdd.BDDTrue, bdd.BDDTrue);
            acl_rule = new LinkedList<BDDRuleItem<ACLRule>>();
            acl_rule.add(new_rule);
        } else if (type == -1) {
            ACLRule rule = new ACLRule();
            rule.permitDeny = "deny";
            rule.priority = -1;
            BDDRuleItem<ACLRule> new_rule = new BDDRuleItem<ACLRule>(rule, bdd.BDDTrue, bdd.BDDTrue);
            acl_rule = new LinkedList<BDDRuleItem<ACLRule>>();
            acl_rule.add(new_rule);
        } else if (type == 2) {
            if (is_decap) {
                String default_rule_name = "default";
            } else {
                rewrite_rules = new LinkedList<>();
                rule_map = new HashMap<>();
                String default_rule_name = "default";
                NATRule default_rule = new NATRule(BDDACLWrapper.BDDTrue, BDDACLWrapper.BDDTrue, BDDACLWrapper.BDDTrue,
                        default_rule_name, 0, bdd);
                BDDRuleItem<RewriteRule> default_item = new BDDRuleItem<RewriteRule>(default_rule,
                        BDDACLWrapper.BDDTrue, BDDACLWrapper.BDDTrue);
                rewrite_rules.add(default_item);
                HashSet<RewriteRule> rules = new HashSet<>();
                rules.add(default_rule);
                rule_map.put(default_rule_name, rules);
            }
        }

        bdd = net.bdd_engine;
        if (type == 0) {
            ports.add("default");
            ports_pred.put("default", NDD.NDDTrue);
        } else if (type == 1) {
            ports.add("permit");
            ports_pred.put("permit", NDD.NDDTrue);
            // ports.add("deny");
            // ports_pred.put("deny", NDD.NDDTrue);
        } else if (type == -1) {
            ports.add("deny");
            ports_pred.put("deny", NDD.NDDTrue);
        } else if (type == 2) {
            ports.add("default");
            ports_pred.put("default", NDD.NDDTrue);
        }
    }

    Comparator<PrefixItem> byPriority = new Comparator<PrefixItem>() {
        @Override
        public int compare(PrefixItem p1, PrefixItem p2) {
            return p2.priority - p1.priority;
        }
    };

    public ArrayList<PrefixItem> GetAffectedRules(TrieTreeNode node) {
        ArrayList<PrefixItem> affected_rules = new ArrayList<PrefixItem>();
        // add the descendants
        ArrayList<TrieTreeNode> descendants = node.GetDescendant();
        if (descendants != null) {
            Iterator<TrieTreeNode> it = node.GetDescendant().iterator();
            while (it.hasNext()) {
                ArrayList<PrefixItem> items = it.next().GetPrefixItems();
                affected_rules.addAll(items);
            }
        }
        // add the ancestors
        ArrayList<TrieTreeNode> ancestors = node.GetAncestor();
        if (ancestors != null) {
            Iterator<TrieTreeNode> it = node.GetAncestor().iterator();
            while (it.hasNext()) {
                ArrayList<PrefixItem> items = it.next().GetPrefixItems();
                affected_rules.addAll(items);
            }
        }
        // // add the siblings
        affected_rules.addAll(node.GetPrefixItems());
        affected_rules.sort(byPriority);
        return affected_rules;
    }

    public void updateFWRuleBatch(String ip, HashSet<String> to_ports, HashSet<String> from_ports,
            HashMap<String, ArrayList<ChangeTuple>> change_set, HashMap<String, ArrayList<ChangeTuple>> copyto_set,
            HashMap<String, ArrayList<ChangeTuple>> remove_set) {
        // find the exact node in prefixTree
        long destip = Long.parseLong(ip.split("/")[0]);
        int prefixlen = Integer.parseInt(ip.split("/")[1]);
        int priority = prefixlen;
        ArrayList<ChangeTuple> change = new ArrayList<ChangeTuple>();
        ArrayList<ChangeTuple> copyto = new ArrayList<ChangeTuple>();
        ArrayList<ChangeTuple> remove = new ArrayList<ChangeTuple>();
        TrieTreeNode node = trie.Search(destip, prefixlen);
        if (node == null) {
            /*
             * no same prefix was inserted in this element before from_ports must be empty
             */
            node = trie.Insert(destip, prefixlen);
            // /*
            // * if(!from_ports.isEmpty()) {
            // * if(node == null){
            // * System.err.println(name + ", node not found: "+ destip + "/" + prefixlen +
            // "; "
            // * + "outinterface: " + from_ports.toArray()[0] + "; priority: " + priority);
            // * System.exit(1);
            // * }
            // * }
            // */
            int rule_bdd;
            if (name.contains("_vpn")) {
                rule_bdd = bdd.GetPrefixBDD(destip, prefixlen);
            } else {
                // System.out.println("tag1");
                // System.out.println(bdd.BDDSize());
                // System.out.println(name+" "+destip+" "+prefixlen);
                rule_bdd = bdd.GetPrefixBDD_outter(destip, prefixlen);
                // System.out.println("tag2");
            }
            ArrayList<PrefixItem> affected_rules = GetAffectedRules(node);
            int residual = bdd.getBDD().ref(rule_bdd);
            int residual2 = BDDACLWrapper.BDDFalse;
            boolean inserted = false;
            int last_priority = 65535;
            int last_sum = BDDACLWrapper.BDDFalse;
            Iterator<PrefixItem> it = affected_rules.iterator();
            while (it.hasNext()) {
                PrefixItem item = it.next();
                if (item.priority > priority) {
                    residual = bdd.diffto(residual, item.rule_bdd);
                    if (residual == BDDACLWrapper.BDDFalse) {
                        bdd.getBDD().deref(rule_bdd);
                        break;
                    }
                }
                /*
                 * no same prefix was inserted before else if(item.priority == priority) {
                 * System.exit(1); }
                 */
                else {
                    if (!inserted) {
                        residual2 = bdd.getBDD().ref(residual);
                        inserted = true;
                        last_priority = item.priority;
                    }
                    if (last_priority != item.priority) {
                        residual2 = bdd.diffto(residual2, last_sum);
                        last_sum = BDDACLWrapper.BDDFalse;
                        last_priority = item.priority;
                    }
                    if (residual2 == BDDACLWrapper.BDDFalse) {
                        break;
                    }
                    int delta = bdd.getBDD().ref(bdd.getBDD().and(item.matches, residual2));
                    if (delta == BDDACLWrapper.BDDFalse) {
                        continue;
                    }
                    item.matches = bdd.diffto(item.matches, delta);
                    last_sum = bdd.getBDD().orTo(last_sum, delta);
                    bdd.getBDD().deref(delta);
                    HashSet<String> ports = new HashSet<String>();
                    ports.add(item.outinterface);
                    HashSet<Integer> delta_set = new HashSet<Integer>();
                    delta_set.add(bdd.getBDD().ref(delta));
                    ChangeTuple ct = new ChangeTuple(ports, to_ports, delta_set);
                    if (!ct.from_ports.equals(ct.to_ports))
                        change.add(ct);
                }
            }
            if (last_sum != BDDACLWrapper.BDDFalse) {
                residual2 = bdd.diffto(residual2, last_sum);
                last_sum = BDDACLWrapper.BDDFalse;
            }
            /*
             * if (residual2 != BDDACLWrapper.BDDFalse) {
             * System.out.println("not overriding any low-priority rule"); System.exit(0); }
             */

            int hit_bdd = residual;
            for (String port : to_ports) {
                PrefixItem insert_item = new PrefixItem(priority, port, bdd.getBDD().ref(rule_bdd),
                        bdd.getBDD().ref(hit_bdd));
                // check whether the forwarding port exists, if not create it,
                // and initialize the AP set of the port to empty
                if (!ports.contains(port)) {
                    ports.add(port);
                    ports_pred.put(port, NDD.NDDFalse);
                }
                // insert the rule
                node.AddPrefixItem(insert_item);
            }
            bdd.getBDD().deref(rule_bdd);
            bdd.getBDD().deref(hit_bdd);
        } else { // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                 // System.out.println("tag");
                 // we have pre-rules, just use matches field to give a short cut
            ArrayList<PrefixItem> node_rules = node.GetPrefixItems();
            ArrayList<PrefixItem> insert_items = new ArrayList<PrefixItem>();
            ArrayList<PrefixItem> delete_items = new ArrayList<PrefixItem>();
            int rule_bdd = 0;
            int hit_bdd = 0;
            if (!node_rules.isEmpty()) {
                rule_bdd = bdd.getBDD().ref(node_rules.get(0).rule_bdd);
                hit_bdd = bdd.getBDD().ref(node_rules.get(0).matches);
                if (node_rules.size() == 1 && node_rules.get(0).priority == -1) {
                    // we hit the default rule
                    String outinterface = "default";
                    int delta = node_rules.get(0).matches;
                    node_rules.get(0).matches = BDDACLWrapper.BDDFalse;
                    bdd.getBDD().deref(delta);
                    HashSet<String> ports = new HashSet<String>();
                    ports.add(outinterface);
                    HashSet<Integer> delta_set = new HashSet<Integer>();
                    delta_set.add(bdd.getBDD().ref(delta));
                    ChangeTuple ct = new ChangeTuple(ports, to_ports, delta_set);
                    if (!ct.from_ports.equals(ct.to_ports))
                        change.add(ct);

                } else if (node_rules.size() != 1 && node_rules.get(0).priority == -1) {
                    rule_bdd = bdd.getBDD().ref(node_rules.get(1).rule_bdd);
                    hit_bdd = bdd.getBDD().ref(node_rules.get(1).matches);
                    if (from_ports.isEmpty()) {
                        String outinterface = node_rules.get(1).outinterface;
                        HashSet<String> ports = new HashSet<String>();
                        ports.add(outinterface);
                        HashSet<Integer> delta_set = new HashSet<Integer>();
                        delta_set.add(bdd.getBDD().ref(hit_bdd));
                        ChangeTuple ct = new ChangeTuple(ports, to_ports, delta_set);
                        if (!ct.from_ports.containsAll(ct.to_ports))
                            copyto.add(ct);
                    }
                } else {
                    // node_rules.get(0).priority != -1 case
                    if (from_ports.isEmpty()) {
                        String outinterface = node_rules.get(0).outinterface;
                        HashSet<String> ports = new HashSet<String>();
                        ports.add(outinterface);
                        HashSet<Integer> delta_set = new HashSet<Integer>();
                        delta_set.add(bdd.getBDD().ref(hit_bdd));
                        ChangeTuple ct = new ChangeTuple(ports, to_ports, delta_set);
                        if (!ct.from_ports.containsAll(ct.to_ports))
                            copyto.add(ct);
                    }
                }
            }
            /*
             * else { System.err.println("node is invalid!"); System.exit(1); }
             */
            for (String port : to_ports) {
                insert_items.add(new PrefixItem(priority, port, bdd.getBDD().ref(rule_bdd), bdd.getBDD().ref(hit_bdd)));
                if (!ports.contains(port)) {
                    ports.add(port);
                    ports_pred.put(port, NDD.NDDFalse);
                }
            }
            for (String port : from_ports) {
                PrefixItem delete_rule = new PrefixItem(priority, port, rule_bdd, hit_bdd);
                if (!node.HasPrefixItem(delete_rule)) {
                    System.exit(1);
                }
                delete_items.add(delete_rule);
                bdd.getBDD().deref(rule_bdd);
                bdd.getBDD().deref(hit_bdd);
            }
            node_rules.removeAll(delete_items);
            node_rules.addAll(insert_items);

            if (node.IsInValid()) {
                ArrayList<PrefixItem> affected_rules = GetAffectedRules(node);
                int residual = hit_bdd;
                int last_priority = 65535;
                int last_sum = BDDACLWrapper.BDDFalse;
                boolean inserted = false;
                Iterator<PrefixItem> it = affected_rules.iterator();
                while (it.hasNext() && residual != BDDACLWrapper.BDDFalse) {
                    PrefixItem item = it.next();
                    if (item.priority > priority) {
                        continue;
                    } else if (item.priority == priority) {
                        System.exit(1);
                    } else {
                        if (!inserted) {
                            inserted = true;
                        }
                        if (last_priority != item.priority) {
                            residual = bdd.diffto(residual, last_sum);
                            last_sum = BDDACLWrapper.BDDFalse;
                            last_priority = item.priority;
                        }
                        if (residual == BDDACLWrapper.BDDFalse) {
                            break;
                        }
                        int delta = bdd.getBDD().ref(bdd.getBDD().and(residual, item.rule_bdd));
                        if (delta == BDDACLWrapper.BDDFalse) {
                            continue;
                        }
                        item.matches = bdd.getBDD().orTo(item.matches, delta);
                        last_sum = bdd.getBDD().orTo(last_sum, delta);
                        bdd.getBDD().deref(delta);
                        HashSet<String> ports = new HashSet<String>();
                        ports.add(item.outinterface);
                        HashSet<Integer> delta_set = new HashSet<Integer>();
                        delta_set.add(bdd.getBDD().ref(delta));
                        ChangeTuple ct = new ChangeTuple(from_ports, ports, delta_set);
                        if (!ct.from_ports.equals(ct.to_ports))
                            change.add(ct);
                    }
                }
                if (last_sum != BDDACLWrapper.BDDFalse) {
                    residual = bdd.diffto(residual, last_sum);
                    last_sum = BDDACLWrapper.BDDFalse;
                }
                if (residual != BDDACLWrapper.BDDFalse) {
                    System.err.println("not fully deleted");
                    System.exit(1);
                }
                if (name.contains("_vpn")) {
                    bdd.RemovePrefixBDD(destip, prefixlen);
                } else {
                    bdd.RemovePrefixBDD_outter(destip, prefixlen);
                }
                node.Delete();
            } else if (node_rules.size() == 1 && node_rules.get(0).priority == -1) {
                // we hit the default rule
                String outinterface = "default";
                node_rules.get(0).matches = hit_bdd;
                int delta = node_rules.get(0).matches;
                HashSet<String> ports = new HashSet<String>();
                ports.add(outinterface);
                HashSet<Integer> delta_set = new HashSet<Integer>();
                delta_set.add(bdd.getBDD().ref(delta));
                ChangeTuple ct = new ChangeTuple(from_ports, ports, delta_set);
                if (!ct.from_ports.equals(ct.to_ports))
                    change.add(ct);
                bdd.getBDD().ref(delta);
            } else {
                if (from_ports.isEmpty()) {
                    // only insert
                    // do nothing
                } else if (to_ports.isEmpty()) {
                    // only delete
                    HashSet<String> ports = new HashSet<String>();
                    HashSet<Integer> delta_set = new HashSet<Integer>();
                    delta_set.add(bdd.getBDD().ref(hit_bdd));
                    ChangeTuple ct = new ChangeTuple(from_ports, ports, delta_set);
                    remove.add(ct);
                } else {
                    HashSet<Integer> delta_set = new HashSet<Integer>();
                    delta_set.add(bdd.getBDD().ref(hit_bdd));
                    ChangeTuple ct = new ChangeTuple(from_ports, to_ports, delta_set);
                    if (!ct.from_ports.equals(ct.to_ports))
                        change.add(ct);
                }
            }
            bdd.getBDD().deref(rule_bdd);
            bdd.getBDD().deref(hit_bdd);
        }
        if (!change_set.containsKey(name)) {
            change_set.put(name, change);
        } else {
            change_set.get(name).addAll(change);
        }
        if (!copyto_set.containsKey(name)) {
            copyto_set.put(name, copyto);
        } else {
            copyto_set.get(name).addAll(copyto);
        }
        if (!remove_set.containsKey(name)) {
            remove_set.put(name, remove);
        } else {
            remove_set.get(name).addAll(remove);
        }
    }

    public void update_ACL(ArrayList<ChangeItem> change_set) {
        if (change_set.size() == 0) {
            return;
        }
        for (ChangeItem item : change_set) {
            String from_port = item.from_port;
            String to_port = item.to_port;
            if(from_port.contains("copy"))System.out.println();
            NDD delta = NDD.table.ref(NDD.toNDD(item.delta));

            NDD oldFrom = ports_pred.get(from_port);
            NDD newFrom = NDD.table.ref(NDD.Diff(oldFrom, delta));
            ports_pred.put(from_port, newFrom);
            NDD oldTo = ports_pred.get(to_port);
            NDD newTo = NDD.table.ref(NDD.OR(oldTo, delta));
            ports_pred.put(to_port, newTo);
            
            NDD.table.deref(oldFrom);
            NDD.table.deref(oldTo);
            NDD.table.deref(delta);
        }
    }

    // public static boolean testACL = false;
    // public static long ACLTime1 = 0L;
    // public static long ACLTime2 = 0L;
    // public static long ACLTime3 = 0L;
    public void update_ACL(HashMap<String, Integer> remove, HashMap<String, Integer> add) {
        for (Entry<String, Integer> entry : remove.entrySet()) {
            String from_port = entry.getKey();
            NDD delta = NDD.table.ref(NDD.toNDD(entry.getValue()));
            NDD oldPred = ports_pred.get(from_port);
            NDD newPred = NDD.table.ref(NDD.Diff(oldPred, delta));
            ports_pred.put(from_port, newPred);
            NDD.table.deref(delta);
            NDD.table.deref(oldPred);
        }
        for (Entry<String, Integer> entry : add.entrySet()) {
            String to_port = entry.getKey();
            NDD delta = NDD.table.ref(NDD.toNDD(entry.getValue()));
            NDD oldPred = ports_pred.get(to_port);
            NDD newPred = NDD.table.ref(NDD.OR(oldPred, delta));
            ports_pred.put(to_port, newPred);
            NDD.table.deref(delta);
            NDD.table.deref(oldPred);
        }
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
            NDD.table.deref(delta);
        }
    }

    public ArrayList<ChangeItem> InsertACLRule(ACLRule rule) {

        ArrayList<ChangeItem> changeset = new ArrayList<ChangeItem>();
        int rule_bdd = bdd.ConvertACLRule(rule);
        BDD thebdd = bdd.getBDD();
        int priority = rule.getPriority();
        int residual = bdd.getBDD().ref(rule_bdd);
        int residual2 = BDDACLWrapper.BDDFalse;
        int cur_position = 0;
        boolean inserted = false;

        BDDRuleItem<ACLRule> default_item = acl_rule.getLast();

        Iterator<BDDRuleItem<ACLRule>> it = acl_rule.iterator();
        while (it.hasNext() && residual != BDDACLWrapper.BDDFalse) {
            BDDRuleItem<ACLRule> item = it.next();
            // TODO: fast check whether the rule is not affected by any rule
            if (item.rule.getPriority() >= priority) {
                if (residual != BDDACLWrapper.BDDFalse
                        && thebdd.and(residual, item.rule_bdd) != BDDACLWrapper.BDDFalse) {
                    residual = bdd.diffto(residual, item.rule_bdd);
                }
                cur_position++;
            } else {
                if (!inserted) {
                    // fast check whether the default rule is the only rule affected
                    int temp = bdd.diff(residual, default_item.matches);
                    if (temp == BDDACLWrapper.BDDFalse) {
                        default_item.matches = bdd.diffto(default_item.matches, residual);
                        if (!default_item.rule.permitDeny.equals(rule.get_type())) {
                            ChangeItem change_item = new ChangeItem(default_item.rule.permitDeny, rule.get_type(),
                                    residual);
                            changeset.add(change_item);
                        }
                        break;
                    }

                    bdd.getBDD().deref(temp);
                    residual2 = bdd.getBDD().ref(residual);
                    inserted = true;
                }

                if (residual2 == BDDACLWrapper.BDDFalse) {
                    break;
                }

                int delta = bdd.getBDD().ref(bdd.getBDD().and(item.matches, residual2));
                if (delta != BDDACLWrapper.BDDFalse) {
                    item.matches = bdd.diffto(item.matches, delta);
                    residual2 = bdd.diffto(residual2, delta);

                    String foward_port = item.rule.get_type();

                    if (!foward_port.equals(rule.get_type())) {
                        ChangeItem change_item = new ChangeItem(foward_port, rule.get_type(), delta);
                        changeset.add(change_item);
                    }
                }
            }
        }

        // add the new rule into the installed forwarding rule list
        BDDRuleItem<ACLRule> new_rule = new BDDRuleItem<ACLRule>(rule, rule_bdd);
        new_rule.matches = residual;
        acl_rule.add(cur_position, new_rule);

        // check whether the forwarding port exists, if not create it,
        // and initialize the AP set of the port to empty
        String iname = rule.get_type();
        if (!ports.contains(iname)) {
            ports.add(iname);
            ports_pred.put(iname, NDD.NDDFalse);
        }

        if (residual2 != BDDACLWrapper.BDDFalse) {
            bdd.getBDD().deref(residual2);
        }

        return changeset;
    }

    public static long insertVxLANT1 = 0L;
    public static long insertVxLANT2 = 0L;
    public static long insertVxLANT3 = 0L;
    public static long insertVxLANT4 = 0L;
    public static long insertVxLANT5 = 0L;
    public static long insertVxLANT6 = 0L;

    public ArrayList<ChangeItem> InsertRewriteRule(RewriteRule rule) {
        ArrayList<ChangeItem> changeset = new ArrayList<ChangeItem>();
        BDD thebdd = bdd.getBDD();

        int rule_bdd = rule.old_pkt_bdd;
        int priority = rule.priority;
        int residual = bdd.ref(rule_bdd);
        int residual2 = common.BDDACLWrapper.BDDFalse;
        int cur_position = 0;
        boolean first_inserted = false;
        boolean inserted = false;

        BDDRuleItem<RewriteRule> default_item = rewrite_rules.getLast();
        Iterator<BDDRuleItem<RewriteRule>> it = rewrite_rules.iterator();

        while (it.hasNext()) {
            if (!first_inserted) // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            {
                // BDDACLWrapper.test = true;
                // Long t2 = System.nanoTime();
                // int temp = bdd.diff(residual, default_item.matches);
                int temp = bdd.and(residual, default_item.matches);
                // Long t3 = System.nanoTime();
                // insertVxLANT1 += t3-t2;
                // BDDACLWrapper.test = false;
                // if (temp == common.BDDACLWrapper.BDDFalse) {
                if (temp == residual) {
                    // Long t4 = System.nanoTime();
                    default_item.matches = bdd.diffto(default_item.matches, residual);
                    // Long t5 = System.nanoTime();
                    // insertVxLANT2 += t5-t4;
                    ChangeItem change_item = new ChangeItem(default_item.rule.name, rule.name, residual);
                    changeset.add(change_item);
                    cur_position = rewrite_rules.size() - 1;
                    break;
                }
                bdd.deref(temp);
                first_inserted = true;
            }

            BDDRuleItem<RewriteRule> item = it.next();
            // TODO: fast check whether the rule is not affected by any rule
            if (item.rule.priority > priority) {
                // Long t6 = System.nanoTime();
                if (thebdd.and(residual, item.rule_bdd) != common.BDDACLWrapper.BDDFalse) {
                    residual = bdd.diffto(residual, item.rule_bdd);
                }
                // Long t7 = System.nanoTime();
                // insertVxLANT3 += t7-t6;
                cur_position++;
            } else if (item.rule.priority == priority) {
                // Long t8 = System.nanoTime();
                if (thebdd.and(residual, item.rule_bdd) != common.BDDACLWrapper.BDDFalse) {
                    if (!item.rule.name.equals(rule.name)) {
                        ChangeItem change_item = new ChangeItem("copy" + item.rule.name, rule.name, bdd.ref(residual));
                        changeset.add(change_item);
                    }
                    cur_position++;
                    // Long t9 = System.nanoTime();
                    // insertVxLANT4 += t9-t8;
                    break;
                } else {
                    cur_position++;
                }
                // Long t9 = System.nanoTime();
                // insertVxLANT4 += t9-t8;
            } else {
                // Long t10 = System.nanoTime();
                if (!inserted) {
                    // fast check whether the default rule is the only rule affected
                    int temp = bdd.diff(residual, default_item.matches);
                    if (temp == common.BDDACLWrapper.BDDFalse) {
                        default_item.matches = bdd.diffto(default_item.matches, residual);
                        ChangeItem change_item = new ChangeItem(default_item.rule.name, rule.name, residual);
                        changeset.add(change_item);
                        // Long t11 = System.nanoTime();
                        // insertVxLANT5 += t11-t10;
                        break;
                    }
                    bdd.deref(temp);
                    residual2 = bdd.ref(residual);
                    inserted = true;
                }
                // Long t11 = System.nanoTime();
                // insertVxLANT5 += t11-t10;

                if (residual2 == common.BDDACLWrapper.BDDFalse) {
                    break;
                }

                int delta = bdd.and(item.matches, residual2);
                if (delta != common.BDDACLWrapper.BDDFalse) {
                    item.matches = bdd.diffto(item.matches, delta);
                    residual2 = bdd.diffto(residual2, delta);
                    if (!item.rule.name.equals(rule.name)) {
                        ChangeItem change_item = new ChangeItem(item.rule.name, rule.name, delta);
                        changeset.add(change_item);
                    }
                }
                // Long t12 = System.nanoTime();
                // insertVxLANT6 += t12-t11;
            }
        }

        // add the new rule into the installed forwarding rule list
        BDDRuleItem<RewriteRule> new_rule = new BDDRuleItem<RewriteRule>(rule, rule_bdd);
        new_rule.matches = residual;
        rewrite_rules.add(cur_position, new_rule);
        if (!rule_map.containsKey(rule.name))
            rule_map.put(rule.name, new HashSet<>());
        rule_map.get(rule.name).add(rule);

        // check whether the forwarding port exists, if not create it,
        // and initialize the AP set of the port to empty
        if (!ports.contains(rule.name)) {
            ports.add(rule.name);
            ports_pred.put(rule.name, NDD.NDDFalse);
        }

        if (residual2 != common.BDDACLWrapper.BDDFalse) {
            bdd.deref(residual2);
        }

        return changeset;
    }

    public ArrayList<ChangeItem> InsertACLRuleBDD(ACLRule rule) {

        ArrayList<ChangeItem> changeset = new ArrayList<ChangeItem>();

        int rule_bdd = rule.get_val_bdd();
        BDD thebdd = bdd.getBDD();
        int priority = rule.getPriority();
        int residual = bdd.ref(rule_bdd);
        int residual2 = common.BDDACLWrapper.BDDFalse;
        int cur_position = 0;
        boolean inserted = false;
        // changes.clear();

        BDDRuleItem<ACLRule> default_item = acl_rule.getLast();

        Iterator<BDDRuleItem<ACLRule>> it = acl_rule.iterator();
        while (it.hasNext() && residual != common.BDDACLWrapper.BDDFalse) {
            BDDRuleItem<ACLRule> item = it.next();
            // TODO: fast check whether the rule is not affected by any rule
            if (item.rule.getPriority() >= priority) {
                if (residual != common.BDDACLWrapper.BDDFalse
                        && thebdd.and(residual, item.rule_bdd) != common.BDDACLWrapper.BDDFalse) {
                    // System.out.println("high-priority rule exists");
                    residual = bdd.diffto(residual, item.rule_bdd);
                }
                cur_position++;
            } else {
                if (!inserted) {
                    // fast check whether the default rule is the only rule affected
                    int temp = bdd.diff(residual, default_item.matches);
                    if (temp == common.BDDACLWrapper.BDDFalse) {
                        // System.out.println("tag "+default_item.rule.permitDeny+" "+rule.get_type());
                        default_item.matches = bdd.diffto(default_item.matches, residual);
                        if (!default_item.rule.permitDeny.equals(rule.get_type())) {
                            ChangeItem change_item = new ChangeItem(default_item.rule.permitDeny, rule.get_type(),
                                    residual);
                            changeset.add(change_item);
                        }
                        break;
                    }

                    bdd.deref(temp);
                    residual2 = bdd.ref(residual);
                    inserted = true;
                }

                if (residual2 == common.BDDACLWrapper.BDDFalse) {
                    break;
                }

                int delta = bdd.and(item.matches, residual2);
                if (delta != common.BDDACLWrapper.BDDFalse) {
                    item.matches = bdd.diffto(item.matches, delta);
                    residual2 = bdd.diffto(residual2, delta);

                    String foward_port = item.rule.get_type();

                    if (!foward_port.equals(rule.get_type())) {
                        ChangeItem change_item = new ChangeItem(foward_port, rule.get_type(), delta);
                        changeset.add(change_item);
                    }
                }
            }
        }

        // add the new rule into the installed forwarding rule list
        BDDRuleItem<ACLRule> new_rule = new BDDRuleItem<ACLRule>(rule, rule_bdd);
        new_rule.matches = residual;
        // System.out.println("matches " + new_rule.matches + ": " +
        // bdd.getBDD().getRef(new_rule.matches));
        acl_rule.add(cur_position, new_rule);

        // check whether the forwarding port exists, if not create it,
        // and initialize the AP set of the port to empty
        String iname = rule.get_type();
        if (!ports.contains(iname)) {
            ports.add(iname);
            ports_pred.put(iname, NDD.NDDFalse);
        }

        if (residual2 != common.BDDACLWrapper.BDDFalse) {
            bdd.deref(residual2);
        }

        // System.out.println("added rule " + rule + " to position " + cur_position + "
        // of " + acl_rule.size());

        return changeset;
    }
}