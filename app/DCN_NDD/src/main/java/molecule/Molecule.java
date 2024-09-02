package molecule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import javafx.util.*;

import molecule.NodeTable;

import NDD.NDD;
import apkeep.FDN.BDDACLWrapper;
import apkeep.FDN.FieldNodeAP;

public class Molecule {
    public static int fieldNum;
    public static Molecule MTrue = new Molecule();
    public static Molecule MFalse = new Molecule();
    public static BDDACLWrapper bdd;
    public static NodeTable table = new NodeTable();
    public int field;
    public HashMap<Molecule, HashSet<Integer>> edges;

    public static boolean useCache = false;
    public static HashMap<Pair<Molecule, Molecule>, Molecule> Cache = new HashMap<>();

    public static void clear()
    {
        table.clear();
        Cache = new HashMap<>();
        SetFieldNum(fieldNum);
    }

    public static void clearCache()
    {
        Cache = new HashMap<>();
    }

    public static void SetFieldNum(int num) {
        fieldNum = num;
        table.APs = new HashSet[fieldNum];
        for (int i = 0; i < fieldNum; i++) {
            table.APs[i] = new HashSet<>();
            table.APs[i].add(1);
        }
    }

    public Molecule(int field, HashMap<Molecule, HashSet<Integer>> edges) {
        this.field = field;
        this.edges = edges;
    }

    public Molecule() {
        field = fieldNum + 1;
    }

    public boolean equals(Molecule obj) {
        return this == obj;
        // return this == obj || this.equals(obj);
    }

    public boolean is_True() {
        return this == MTrue;
    }

    public boolean is_False() {
        return this == MFalse;
    }

    public static int toBDD(Molecule curr) {
        if (curr.is_True())
            return 1;
        if (curr.is_False())
            return 0;
        int ret = 0;
        for (Molecule next : curr.edges.keySet()) {
            int curr_field = 0;
            for (int ap : curr.edges.get(next)) {
                int old = curr_field;
                curr_field = bdd.getBDD().ref(bdd.getBDD().or(curr_field, ap));
                bdd.getBDD().deref(old);
            }
            int next_field = toBDD(next);
            int old = curr_field;
            curr_field = bdd.getBDD().ref(bdd.getBDD().and(curr_field, next_field));
            bdd.getBDD().deref(old);
            bdd.getBDD().deref(next_field);
            old = ret;
            ret = bdd.getBDD().ref(bdd.getBDD().or(ret, curr_field));
            bdd.getBDD().deref(old);
            bdd.getBDD().deref(curr_field);
        }
        return ret;
    }

    public static Molecule OR(Molecule a, Molecule b) {
        return ORRec(a, b);
    }

    public static Molecule ORRec(Molecule a, Molecule b) {
        if (a.is_True() || b.is_True())
            return MTrue;
        if (a.is_False())
            return b;
        if (b.is_False())
            return a;
        // if (a == b || a.equals(b))
        if (a == b)
            return a;
        if (a.field == b.field) {
            HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule, HashSet<Integer>>();
            HashMap<Molecule, HashSet<Integer>> temp_a = new HashMap<Molecule, HashSet<Integer>>();
            HashMap<Molecule, HashSet<Integer>> temp_b = new HashMap<Molecule, HashSet<Integer>>();
            for (Map.Entry<Molecule, HashSet<Integer>> entry_a : a.edges.entrySet()) {
                temp_a.put(entry_a.getKey(), new HashSet<Integer>(entry_a.getValue()));
            }
            for (Map.Entry<Molecule, HashSet<Integer>> entry_b : b.edges.entrySet()) {
                temp_b.put(entry_b.getKey(), new HashSet<Integer>(entry_b.getValue()));
            }
            for (Map.Entry<Molecule, HashSet<Integer>> entry_a : temp_a.entrySet()) {
                for (Map.Entry<Molecule, HashSet<Integer>> entry_b : temp_b.entrySet()) {
                    HashSet<Integer> intersect;
                    if (entry_a.getValue().size() < entry_b.getValue().size()) {
                        intersect = new HashSet<Integer>(entry_a.getValue());
                        intersect.retainAll(entry_b.getValue());
                    } else {
                        intersect = new HashSet<Integer>(entry_b.getValue());
                        intersect.retainAll(entry_a.getValue());
                    }
                    if (intersect.isEmpty())
                        continue;
                    entry_a.getValue().removeAll(intersect);
                    entry_b.getValue().removeAll(intersect);
                    Molecule subRet = ORRec(entry_a.getKey(), entry_b.getKey());
                    if (subRet.is_False())
                        continue;
                    HashSet<Integer> aps = tempSet.get(subRet);
                    if (aps == null) {
                        aps = new HashSet<Integer>();
                    }
                    aps.addAll(intersect);
                    tempSet.put(subRet, aps);
                }
            }
            for (Map.Entry<Molecule, HashSet<Integer>> entry_a : temp_a.entrySet()) {
                if (entry_a.getValue().size() != 0) {
                    HashSet<Integer> aps = tempSet.get(entry_a.getKey());
                    if (aps == null) {
                        aps = new HashSet<Integer>();
                    }
                    aps.addAll(entry_a.getValue());
                    tempSet.put(entry_a.getKey(), aps);
                }
            }
            for (Map.Entry<Molecule, HashSet<Integer>> entry_b : temp_b.entrySet()) {
                if (entry_b.getValue().size() != 0) {
                    HashSet<Integer> aps = tempSet.get(entry_b.getKey());
                    if (aps == null) {
                        aps = new HashSet<Integer>();
                    }
                    aps.addAll(entry_b.getValue());
                    tempSet.put(entry_b.getKey(), aps);
                }
            }
            // if (tempSet.size() == 0)
            // return MFalse;
            // if (tempSet.size() == 1) {
            // for (Molecule key : tempSet.keySet()) {
            // if (tempSet.get(key).size() == table.getAP(a.field).size()) {
            // return key;
            // }
            // }
            // }
            return table.mk(a.field, tempSet);
        } else {
            if (a.field > b.field) {
                Molecule t = a;
                a = b;
                b = t;
            }
            HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule, HashSet<Integer>>();
            HashSet<Integer> false_set = new HashSet<Integer>(table.getAP(a.field));
            for (Map.Entry<Molecule, HashSet<Integer>> entry_a : a.edges.entrySet()) {
                false_set.removeAll(entry_a.getValue());// !!!
                Molecule subRet = ORRec(entry_a.getKey(), b);
                if (subRet.is_False())
                    continue;
                HashSet<Integer> aps = tempSet.get(subRet);
                if (aps == null) {
                    aps = new HashSet<Integer>();
                }
                aps.addAll(entry_a.getValue());
                tempSet.put(subRet, aps);
            }
            if (false_set.size() != 0) {
                HashSet<Integer> aps = tempSet.get(b);
                if (aps == null) {
                    aps = new HashSet<Integer>();
                }
                aps.addAll(false_set);
                tempSet.put(b, aps);
            }
            // if (tempSet.size() == 0)
            // return MFalse;
            // if (tempSet.size() == 1) {
            // for (Molecule key : tempSet.keySet()) {
            // if (tempSet.get(key).size() == table.getAP(a.field).size()) {
            // return key;
            // }
            // }
            // }
            return table.mk(a.field, tempSet);
        }
    }

    public static boolean CheckPred(Molecule curr) {
        if (curr.is_False() || curr.is_True()) {
            return true;
        }
        HashSet<Integer> aps = table.getAP(curr.field);
        for (Map.Entry<Molecule, HashSet<Integer>> entry : curr.edges.entrySet()) {
            if (!aps.containsAll(entry.getValue())) {
                // System.out.println(curr.field+" "+curr+" "+entry.getKey()+"
                // "+aps.toString()+" "+entry.getValue().toString());
                return false;
            }
            if (!CheckPred(entry.getKey())) {
                // System.out.println(curr+" "+entry.getKey());
                return false;
            }
        }
        return true;
    }

    public static boolean CheckPred(Molecule curr, HashSet<Integer>[] APs) {
        if (curr.is_False() || curr.is_True()) {
            return true;
        }
        HashSet<Integer> aps = APs[curr.field];
        for (Map.Entry<Molecule, HashSet<Integer>> entry : curr.edges.entrySet()) {
            if (!aps.containsAll(entry.getValue())) {
                // System.out.println(curr+" "+entry.getKey()+" "+aps.toString()+"
                // "+entry.getValue().toString());
                return false;
            }
            if (!CheckPred(entry.getKey(), APs)) {
                // System.out.println(curr+" "+entry.getKey());
                return false;
            }
        }
        // System.out.println(curr+" true");
        return true;
    }

    public static Molecule AND(Molecule a, Molecule b) {
        return ANDRec(a, b);
    }

    public static Molecule ANDRec(Molecule a, Molecule b) {
        if (a.is_False() || b.is_False())
            return MFalse;
        if (a.is_True())
            return b;
        if (b.is_True())
            return a;
        // if (a == b || a.equals(b))
        if (a == b)
            return a;
        if (useCache) {
            Molecule ret = Cache.get(new Pair<Molecule, Molecule>(a, b));
            if (ret != null)
                return ret;
            ret = Cache.get(new Pair<Molecule, Molecule>(a, b));
            if (ret != null)
                return ret;
        }
        if (a.field == b.field) {
            HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule, HashSet<Integer>>();
            for (Map.Entry<Molecule, HashSet<Integer>> entry_a : a.edges.entrySet()) {
                for (Map.Entry<Molecule, HashSet<Integer>> entry_b : b.edges.entrySet()) {
                    // System.out.println("a: "+entry_a.getValue().toString());
                    // System.out.println("b: "+entry_b.getValue().toString());
                    HashSet<Integer> intersect;
                    if (entry_a.getValue().size() < entry_b.getValue().size()) {
                        intersect = new HashSet<Integer>(entry_a.getValue());
                        intersect.retainAll(entry_b.getValue());
                    } else {
                        intersect = new HashSet<Integer>(entry_b.getValue());
                        intersect.retainAll(entry_a.getValue());
                    }
                    if (intersect.isEmpty())
                        continue;
                    Molecule subRet = ANDRec(entry_a.getKey(), entry_b.getKey());
                    if (subRet.is_False())
                        continue;
                    HashSet<Integer> aps = tempSet.get(subRet);
                    if (aps == null) {
                        aps = new HashSet<Integer>();
                    }
                    aps.addAll(intersect);
                    tempSet.put(subRet, aps);
                }
            }
            Molecule ret = table.mk(a.field, tempSet);
            if (useCache) {
                Cache.put(new Pair(a, b), ret);
            }
            return ret;
        } else {
            if (a.field > b.field) {
                Molecule t = a;
                a = b;
                b = t;
            }
            HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule, HashSet<Integer>>();
            for (Map.Entry<Molecule, HashSet<Integer>> entry_a : a.edges.entrySet()) {
                Molecule subRet = ANDRec(entry_a.getKey(), b);
                if (subRet.is_False())
                    continue;
                HashSet<Integer> aps = tempSet.get(subRet);
                if (aps == null) {
                    aps = new HashSet<Integer>();
                }
                aps.addAll(entry_a.getValue());
                tempSet.put(subRet, aps);
            }
            Molecule ret = table.mk(a.field, tempSet);
            if (useCache) {
                Cache.put(new Pair(a, b), ret);
            }
            return ret;
        }
    }

    public static Molecule Diff(Molecule a, Molecule b) {
        Molecule n = Not(b);
        // System.out.println("not bdd:"+bdd.getBDD().not(toBDD(b))+" ndd:"+toBDD(n));
        Molecule ret = AND(a, n);
        if (n != ret)
            table.testAndDelete(n);
        // System.out.println("diff bdd:"+bdd.diff(toBDD(a), toBDD(b))+"
        // ndd:"+toBDD(ret));
        return ret;
    }

    public static Molecule Not(Molecule a) {
        return NotRec(a);
    }

    public static Molecule NotRec(Molecule a) {
        if (a.is_True())
            return MFalse;
        if (a.is_False())
            return MTrue;
        HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule, HashSet<Integer>>();
        HashSet<Integer> false_set = new HashSet<Integer>(table.getAP(a.field));
        for (Map.Entry<Molecule, HashSet<Integer>> entry : a.edges.entrySet()) {
            false_set.removeAll(entry.getValue());
            Molecule subRet = NotRec(entry.getKey());
            if (subRet.is_False())
                continue;
            HashSet<Integer> aps = tempSet.get(subRet);
            if (aps == null) {
                aps = new HashSet<Integer>();
            }
            aps.addAll(entry.getValue());
            tempSet.put(subRet, aps);
        }
        if (false_set.size() != 0) {
            HashSet<Integer> aps = tempSet.get(MTrue);
            if (aps == null) {
                aps = new HashSet<Integer>();
            }
            aps.addAll(false_set);
            tempSet.put(MTrue, aps);
        }
        // if (tempSet.size() == 0)
        // return MFalse;
        // if (tempSet.size() == 1) {
        // for (Molecule key : tempSet.keySet()) {
        // if (tempSet.get(key).size() == table.getAP(a.field).size()) {
        // return key;
        // }
        // }
        // }
        Molecule to_ret = table.mk(a.field, tempSet);
        return to_ret;
    }

    public static Molecule Exist(Molecule a, int field) {
        return ExistRec(a, field);
    }

    public static Molecule ExistRec(Molecule a, int field) {
        if (a.is_True() || a.is_False()) {
            return a;
        }
        if (a.field > field) {
            return a;
        }
        if (a.field == field) {
            Molecule sum = null;
            for (Molecule next : a.edges.keySet()) {
                if (sum == null) {
                    sum = next;
                } else {
                    Molecule old = sum;
                    sum = Molecule.OR(sum, next);
                    if (old != sum)
                        table.testAndDelete(old);
                }
            }
            return sum;
        } else {
            HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule, HashSet<Integer>>();
            for (Map.Entry<Molecule, HashSet<Integer>> entry : a.edges.entrySet()) {
                Molecule subRet = ExistRec(entry.getKey(), field);
                if (subRet.is_False())
                    continue;
                HashSet<Integer> aps = tempSet.get(subRet);
                if (aps == null) {
                    aps = new HashSet<Integer>();
                }
                aps.addAll(entry.getValue());
                tempSet.put(subRet, aps);
            }
            // if (tempSet.size() == 0)
            // return MFalse;
            // if (tempSet.size() == 1) {
            // for (Molecule key : tempSet.keySet()) {
            // if (tempSet.get(key).size() == table.getAP(a.field).size()) {
            // return key;
            // }
            // }
            // }
            Molecule to_ret = table.mk(a.field, tempSet);
            return to_ret;
        }
    }

    public static void get_split_ap_one_field(Molecule aps, int delta, HashSet<Integer> delta_aps,
            HashMap<Integer, HashSet<Integer>> split_ap, int field) {
        bdd.getBDD().ref(delta);
        if (aps.is_True()) {
            HashSet<Integer> field_aps = table.getAP(field);
            for (int one_ap : field_aps) {
                if (delta_aps.contains(one_ap) || split_ap.containsKey(one_ap))
                    continue;
                int intersect = bdd.getBDD().and(delta, one_ap);
                if (intersect != bdd.BDDFalse) {
                    delta_aps.add(intersect);
                    if (intersect != one_ap) {
                        bdd.getBDD().ref(intersect);
                        int diff = bdd.diff(one_ap, intersect);
                        HashSet<Integer> tempSet = new HashSet<>();
                        tempSet.add(intersect);
                        tempSet.add(diff);
                        split_ap.put(one_ap, tempSet);
                    }
                    delta = bdd.diffto(delta, intersect);
                    if (delta == bdd.BDDFalse) {
                        return;
                    }
                }
            }
        } else {
            for (HashSet<Integer> set : aps.edges.values()) {
                for (int one_ap : set) {
                    int intersect = bdd.getBDD().and(delta, one_ap);
                    if (intersect != bdd.BDDFalse) {
                        if (intersect == one_ap) {
                            delta_aps.add(one_ap);
                        } else {
                            HashSet<Integer> tempSet = new HashSet<>();
                            bdd.getBDD().ref(intersect);
                            tempSet.add(intersect);
                            tempSet.add(bdd.diff(one_ap, intersect));
                            split_ap.put(one_ap, tempSet);
                            delta_aps.add(intersect);
                        }
                        int temp = bdd.diff(delta, intersect);
                        bdd.getBDD().deref(delta);
                        delta = temp;
                        if (delta == bdd.BDDFalse) {
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void get_split_ap_one_field(NDD deltaNDD, HashSet<Integer> delta_aps,
            HashMap<Integer, HashSet<Integer>> split_ap, int field) {
        int delta = 0;
        for(int pred : deltaNDD.edges.values())
        {
            delta = pred;
        }
        HashSet<Integer> allAP = table.getAP(field);
        if(allAP.contains(delta))
        {
            delta_aps.add(delta);
            return;
        }
        for(int ap : allAP)
        {
            int intersect = bdd.getBDD().and(delta, ap);
            if (intersect != bdd.BDDFalse) {
                delta_aps.add(intersect);
                if (intersect != ap) {
                    bdd.getBDD().ref(intersect);
                    int diff = bdd.diff(ap, intersect);
                    HashSet<Integer> tempSet = new HashSet<>();
                    tempSet.add(intersect);
                    tempSet.add(diff);
                    split_ap.put(ap, tempSet);
                }
                delta = bdd.diffto(delta, intersect);
                if (delta == bdd.BDDFalse) {
                    return;
                }
            }
        }
    }

    public static Long splitTime1 = 0L;
    public static Long splitTime2 = 0L;
    public static Long splitTime3 = 0L;
    public static Long splitTime4 = 0L;
    public static Long splitTime5 = 0L;
    public static Long splitTime6 = 0L;
    public static int Count = 0;
    public static void get_split_ap_multi_field(Molecule curr, int[] delta_vec, ArrayList<HashSet<Integer>> delta_aps,
            ArrayList<HashMap<Integer, HashSet<Integer>>> split_ap, int field) {
        if (field == fieldNum) {
            return;
        } else if (delta_vec[field] == bdd.BDDTrue) {
            delta_aps.get(field).addAll(table.getAP(field));
            get_split_ap_multi_field(curr, delta_vec, delta_aps, split_ap, field + 1);
        } else if (curr.is_True()) {
            for (int curr_field = field; curr_field < fieldNum; curr_field++) {
                HashSet<Integer> field_aps = table.getAP(curr_field);
                int pred = bdd.getBDD().ref(delta_vec[curr_field]);
                if (pred == bdd.BDDTrue) {
                    delta_aps.get(curr_field).addAll(table.getAP(curr_field));
                } else {
                    for (int one_ap : field_aps) {
                        // if (delta_aps.get(curr_field).contains(one_ap) ||
                        // split_ap.get(curr_field).containsKey(one_ap))
                        // continue;
                        // Long t0 = System.nanoTime();
                        int intersect = bdd.getBDD().and(pred, one_ap);
                        // Long t1 = System.nanoTime();
                        // if(FieldNodeAP.testVxLAN)Count++;
                        // if(FieldNodeAP.testVxLAN && curr_field==0)splitTime1+=t1-t0;
                        // if(FieldNodeAP.testVxLAN && curr_field==1)splitTime2+=t1-t0;
                        // if(FieldNodeAP.testVxLAN && curr_field==2)splitTime3+=t1-t0;
                        // if(FieldNodeAP.testVxLAN && curr_field==3)splitTime4+=t1-t0;
                        // if(FieldNodeAP.testVxLAN && curr_field==4)splitTime5+=t1-t0;
                        // if(FieldNodeAP.testVxLAN && curr_field==5)splitTime6+=t1-t0;
                        if (intersect != bdd.BDDFalse) {
                            delta_aps.get(curr_field).add(intersect);
                            if (intersect != one_ap) {
                                bdd.getBDD().ref(intersect);
                                int diff = bdd.diff(one_ap, intersect);
                                HashSet<Integer> tempSet = new HashSet<>();
                                tempSet.add(intersect);
                                tempSet.add(diff);
                                split_ap.get(curr_field).put(one_ap, tempSet);
                            }
                            pred = bdd.diffto(pred, intersect);
                            if (pred == bdd.BDDFalse) {
                                break;
                            }
                        }
                    }
                }
            }
        } else if (curr.field < field) {
            for (Molecule next : curr.edges.keySet()) {
                get_split_ap_multi_field(next, delta_vec, delta_aps, split_ap, field);
            }
        } else if (curr.field > field) {
            HashSet<Integer> field_aps = table.getAP(field);
            int pred = bdd.getBDD().ref(delta_vec[field]);
            for (int one_ap : field_aps) {
                // if (delta_aps.get(field).contains(one_ap) ||
                // split_ap.get(field).containsKey(one_ap))
                // continue;
                int intersect = bdd.getBDD().and(pred, one_ap);
                if (intersect != bdd.BDDFalse) {
                    delta_aps.get(field).add(intersect);
                    if (intersect != one_ap) {
                        bdd.getBDD().ref(intersect);
                        int diff = bdd.diff(one_ap, intersect);
                        HashSet<Integer> tempSet = new HashSet<>();
                        tempSet.add(intersect);
                        tempSet.add(diff);
                        split_ap.get(field).put(one_ap, tempSet);
                    }
                    pred = bdd.diffto(pred, intersect);
                    if (pred == bdd.BDDFalse) {
                        break;
                    }
                }
            }
            get_split_ap_multi_field(curr, delta_vec, delta_aps, split_ap, field + 1);
        } else if (curr.field == field) {
            int pred = bdd.getBDD().ref(delta_vec[field]);
            for (Map.Entry<Molecule, HashSet<Integer>> entry : curr.edges.entrySet()) {
                if (pred == bdd.BDDFalse)
                    break;
                Boolean match = false;
                HashSet<Integer> apSet = entry.getValue();
                for (int one_ap : apSet) {
                    if (pred == bdd.BDDFalse)
                        break;
                    // if (delta_aps.get(field).contains(one_ap) ||
                    // split_ap.get(field).containsKey(one_ap))
                    // continue;
                    int intersect = bdd.getBDD().and(pred, one_ap);
                    if (intersect != bdd.BDDFalse) {
                        match = true;
                        delta_aps.get(field).add(intersect);
                        if (intersect != one_ap) {
                            bdd.getBDD().ref(intersect);
                            int diff = bdd.diff(one_ap, intersect);
                            HashSet<Integer> tempSet = new HashSet<>();
                            tempSet.add(intersect);
                            tempSet.add(diff);
                            split_ap.get(field).put(one_ap, tempSet);
                        }
                        pred = bdd.diffto(pred, intersect);
                    }
                }
                if (match)
                    get_split_ap_multi_field(entry.getKey(), delta_vec, delta_aps, split_ap, field + 1);
            }
        } else {
            System.out.println("error");
            System.exit(1);
        }
    }

    // deref split_ap !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public static Molecule split_port_ap_1_1(HashMap<Integer, HashSet<Integer>> split_ap, Molecule old_aps) {
        if (old_aps.is_False() || old_aps.is_True())
            return MFalse;
        boolean change = false;
        for (Map.Entry<Molecule, HashSet<Integer>> entry : old_aps.edges.entrySet()) {
            HashSet<Integer> field_aps = new HashSet<>(entry.getValue());
            for (Map.Entry<Integer, HashSet<Integer>> entry1 : split_ap.entrySet()) {
                if (field_aps.contains(entry1.getKey())) {
                    change = true;
                    field_aps.remove(entry1.getKey());
                    field_aps.addAll(entry1.getValue());
                }
            }
            if (change) {
                HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<>();
                tempSet.put(MTrue, field_aps);
                return table.mk(old_aps.field, tempSet);
            }
        }
        return MFalse;
    }

    public static Pair<Boolean, Molecule> split_port_ap_1_n(HashMap<Integer, HashSet<Integer>> split_ap, Molecule curr,
            int field) {
        if (curr.is_False() || curr.is_True())
            return new Pair<Boolean, Molecule>(false, curr);
        else if (curr.field > field)
            return new Pair<Boolean, Molecule>(false, curr);
        else if (curr.field == field) {
            Boolean change = false;
            // HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule,
            // HashSet<Integer>>(curr.edges);
            HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule, HashSet<Integer>>();
            for (Map.Entry<Molecule, HashSet<Integer>> entry : curr.edges.entrySet()) {
                Boolean sub_change = false;
                HashSet<Integer> field_aps = new HashSet<>(entry.getValue());
                for (Map.Entry<Integer, HashSet<Integer>> entry1 : split_ap.entrySet()) {
                    if (field_aps.contains(entry1.getKey())) {
                        sub_change = true;
                        field_aps.remove(entry1.getKey());
                        field_aps.addAll(entry1.getValue());
                    }
                }
                if (sub_change) {
                    change = true;
                    tempSet.put(entry.getKey(), field_aps);
                } else {
                    tempSet.put(entry.getKey(), entry.getValue());
                }
            }
            if (change) {
                return new Pair<Boolean, Molecule>(true, table.mk(field, tempSet));
            } else {
                return new Pair<Boolean, Molecule>(false, curr);
            }
        } else {
            Boolean change = false;
            // HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule,
            // HashSet<Integer>>(curr.edges);
            HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule, HashSet<Integer>>();
            for (Map.Entry<Molecule, HashSet<Integer>> entry : curr.edges.entrySet()) {
                Pair<Boolean, Molecule> ret = split_port_ap_1_n(split_ap, entry.getKey(), field);
                if (ret.getKey()) {
                    change = true;
                    tempSet.put(ret.getValue(), entry.getValue());
                } else {
                    tempSet.put(entry.getKey(), entry.getValue());
                }
            }
            if (change) {
                return new Pair<Boolean, Molecule>(true, table.mk(curr.field, tempSet));
            } else {
                return new Pair<Boolean, Molecule>(false, curr);
            }
        }
    }

    public static Pair<Boolean, Molecule> split_port_ap_n_n(ArrayList<HashMap<Integer, HashSet<Integer>>> split_ap,
            Molecule curr) {
        if (curr.is_False() || curr.is_True()) {
            return new Pair<Boolean, Molecule>(false, curr);
        } else {
            boolean change = false;
            // HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule,
            // HashSet<Integer>>(curr.edges);
            HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<Molecule, HashSet<Integer>>();
            for (Map.Entry<Molecule, HashSet<Integer>> entry : curr.edges.entrySet()) {
                boolean sub_change = false;
                Pair<Boolean, Molecule> ret = split_port_ap_n_n(split_ap, entry.getKey());
                if (ret.getKey())
                    sub_change = true;
                HashSet<Integer> field_aps = new HashSet<>(entry.getValue());
                for (Map.Entry<Integer, HashSet<Integer>> entry1 : split_ap.get(curr.field).entrySet()) {
                    if (field_aps.contains(entry1.getKey())) {
                        sub_change = true;
                        field_aps.remove(entry1.getKey());
                        field_aps.addAll(entry1.getValue());
                    }
                }
                if (sub_change) {
                    change = true;
                    // tempSet.put(entry.getKey(), field_aps);
                    tempSet.put(ret.getValue(), field_aps);
                } else {
                    tempSet.put(entry.getKey(), entry.getValue());
                }
            }
            if (change) {
                return new Pair<Boolean, Molecule>(true, table.mk(curr.field, tempSet));
            } else {
                return new Pair<Boolean, Molecule>(false, curr);
            }
        }
    }

    public static void ChangedAPs(Molecule oldPred, Molecule newPred, HashSet<Integer>[] remove,
            HashSet<Integer>[] add) {
        HashSet<Integer>[] array1 = new HashSet[Molecule.fieldNum];
        HashSet<Integer>[] array2 = new HashSet[Molecule.fieldNum];
        for (int i = 0; i < Molecule.fieldNum; i++) {
            array1[i] = new HashSet<>();
            array2[i] = new HashSet<>();
        }
        CollectAPs(oldPred, array1);
        CollectAPs(newPred, array2);
        for (int i = 0; i < Molecule.fieldNum; i++) {
            remove[i] = new HashSet<>(array1[i]);
            add[i] = new HashSet<>(array2[i]);
            remove[i].removeAll(array2[i]);
            add[i].removeAll(array1[i]);
        }
    }

    private static void CollectAPs(Molecule pred, HashSet<Integer>[] aps) {
        if (pred.is_False() || pred.is_True())
            return;
        HashSet<Integer> set = aps[pred.field];
        for (Map.Entry<Molecule, HashSet<Integer>> entry : pred.edges.entrySet()) {
            set.addAll(entry.getValue());
            CollectAPs(entry.getKey(), aps);
        }
    }

    public static void ChangedAPsSingleField(Molecule oldPred, Molecule newPred, HashSet<Integer> remove,
            HashSet<Integer> add) {
        HashSet<Integer> set1 = new HashSet<>();
        HashSet<Integer> set2 = new HashSet<>();
        CollectAPsSingleField(oldPred, set1);
        CollectAPsSingleField(newPred, set2);
        remove.addAll(set1);
        add.addAll(set2);
        remove.removeAll(set2);
        add.removeAll(set1);
    }

    private static void CollectAPsSingleField(Molecule pred, HashSet<Integer> aps) {
        if (pred.is_False() || pred.is_True())
            return;
        for (Map.Entry<Molecule, HashSet<Integer>> entry : pred.edges.entrySet()) {
            aps.addAll(entry.getValue());
        }
    }

    public static void printOut(Molecule a) {
        printOutRec(a);
        System.out.println();
    }

    private static void printOutRec(Molecule curr) {
        if (curr.is_True()) {
            System.out.println("T");
            return;
        }
        if (curr.is_False()) {
            System.out.println("F");
            return;
        }
        System.out.print(curr + " field:" + curr.field + " edgeNum:" + curr.edges.size());
        System.out.print(" edges:");
        for (Molecule next : curr.edges.keySet()) {
            if (next.is_True()) {
                System.out.print(" T");
            } else if (next.is_False()) {
                System.out.print(" F");
            } else {
                System.out.print(" " + next);
            }
            System.out.print(" " + curr.edges.get(next).size() + " " + curr.edges.get(next).toString());
        }
        System.out.println();
        for (Molecule next : curr.edges.keySet()) {
            printOutRec(next);
        }
    }
}