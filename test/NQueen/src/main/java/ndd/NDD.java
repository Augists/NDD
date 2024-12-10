package ndd;

import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jdd.bdd.BDD;

public class NDD {
    public static int fieldNum;
    public static int[] upperBound;

    public static NDD NDDTrue = new NDD();
    public static NDD NDDFalse = new NDD();
    public static BDD bdd;
    public static NodeTable table = new NodeTable();

    public static boolean test = false;
    public static long ANDTime = 0L;
    public static long ORTime = 0L;
    public static long NOTTime = 0L;
    public static long ExistTime = 0L;
    public static long ToNDDTime = 0L;

    public int field;
    public HashMap<NDD, Integer> edges;

    // public static void setTest(boolean test) {
    // NodeTable.test = test;
    // }

    // public static int getMKCount() {
    // return NodeTable.mkCount;
    // }

    public static void SetFieldNum(int num) {
        fieldNum = num;
    }

    public static void SetUpperBound(int[] upper) {
        upperBound = upper;
    }

    public NDD(int field, HashMap<NDD, Integer> edges) {
        this.field = field;
        this.edges = edges;
    }

    public NDD() {
        field = fieldNum + 1;
    }

    public boolean equals(NDD obj) {
        // An expression corresponds to a unique NDD instance
        return this == obj;
    }

    public boolean is_True() {
        return this == NDDTrue;
    }

    public boolean is_False() {
        return this == NDDFalse;
    }

    public static NDD AND(NDD a, NDD b) {
        if (test) {
            Long t0 = System.nanoTime();
            NDD ret = ANDRec(a, b);
            Long t1 = System.nanoTime();
            ANDTime += t1 - t0;
            return ret;
        }

        return ANDRec(a, b);
    }

    private static NDD ANDRec(NDD a, NDD b) {
        if (a.is_False() || b.is_False())
            return NDDFalse;
        if (a.is_True())
            return b;
        if (b.is_True())
            return a;
        if (a == b)
            return a;

        if (a.field == b.field) {
            HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
            Iterator itera = a.edges.entrySet().iterator();

            while (itera.hasNext()) {
                Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
                Iterator iterb = b.edges.entrySet().iterator();
                while (iterb.hasNext()) {
                    Map.Entry<NDD, Integer> entryb = (Map.Entry<NDD, Integer>) iterb.next();
                    int intersect = bdd.ref(bdd.and(entrya.getValue(), entryb.getValue()));
                    if (intersect == 0)
                        continue;
                    NDD subRet = ANDRec(entrya.getKey(), entryb.getKey());
                    if (subRet.is_False()) {
                        bdd.deref(intersect);
                        continue;
                    }
                    Integer pred = tempSet.get(subRet);
                    if (pred == null) {
                        pred = 0;
                    }
                    int sum = bdd.ref(bdd.or(pred, intersect));
                    bdd.deref(pred);
                    bdd.deref(intersect);
                    tempSet.put(subRet, sum);
                }
            }

            if (tempSet.size() == 0)
                return NDDFalse;

            if (tempSet.size() == 1) {
                Iterator iter = tempSet.entrySet().iterator();
                Map.Entry<NDD, Integer> entry = (Map.Entry<NDD, Integer>) iter.next();
                if (entry.getValue() == 1) {
                    return entry.getKey();
                }
            }

            return table.mk(a.field, tempSet);
        } else {
            // make sure that a is closer to root than b
            if (a.field > b.field) {
                NDD t = a;
                a = b;
                b = t;
            }

            HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
            Iterator itera = a.edges.entrySet().iterator();

            while (itera.hasNext()) {
                Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
                NDD subRet = ANDRec(entrya.getKey(), b);
                if (subRet.is_False())
                    continue;
                Integer pred = tempSet.get(subRet);
                if (pred == null) {
                    pred = 0;
                }
                int sum = bdd.ref(bdd.or(pred, entrya.getValue()));
                bdd.deref(pred);
                tempSet.put(subRet, sum);
            }

            if (tempSet.size() == 0)
                return NDDFalse;

            if (tempSet.size() == 1) {
                Iterator iter = tempSet.entrySet().iterator();
                Map.Entry<NDD, Integer> entry = (Map.Entry<NDD, Integer>) iter.next();
                if (entry.getValue() == 1) {
                    return entry.getKey();
                }
            }

            return table.mk(a.field, tempSet);
        }
    }

    public static NDD OR(NDD a, NDD b) {
        if (test) {
            Long t0 = System.nanoTime();
            NDD ret = ORRec(a, b);
            Long t1 = System.nanoTime();
            ORTime += t1 - t0;
            return ret;
        }

        return ORRec(a, b);
    }

    private static NDD ORRec(NDD a, NDD b) {
        if (a.is_True() || b.is_True())
            return NDDTrue;
        if (a.is_False())
            return b;
        if (b.is_False())
            return a;
        if (a == b)
            return a;

        if (a.field == b.field) {
            HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
            HashMap<NDD, Integer> temp_a = new HashMap<NDD, Integer>(a.edges);
            for (int oneBDD : a.edges.values()) {
                bdd.ref(oneBDD); // !!!
            }
            HashMap<NDD, Integer> temp_b = new HashMap<NDD, Integer>(b.edges);
            for (int oneBDD : b.edges.values()) {
                bdd.ref(oneBDD); // !!!
            }
            Iterator itera = a.edges.entrySet().iterator();

            while (itera.hasNext()) {
                Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
                Iterator iterb = b.edges.entrySet().iterator();

                while (iterb.hasNext()) {
                    Map.Entry<NDD, Integer> entryb = (Map.Entry<NDD, Integer>) iterb.next();
                    int intersect = bdd.ref(bdd.and(entrya.getValue(), entryb.getValue()));
                    if (intersect == 0)
                        continue;
                    int t = temp_a.get(entrya.getKey());
                    int n = bdd.ref(bdd.not(intersect));
                    temp_a.put(entrya.getKey(),
                            bdd.ref(bdd.and(t, n)));
                    bdd.deref(t);
                    t = temp_b.get(entryb.getKey());
                    temp_b.put(entryb.getKey(),
                            bdd.ref(bdd.and(t, n)));
                    bdd.deref(t);
                    bdd.deref(n);

                    NDD subRet = ORRec(entrya.getKey(), entryb.getKey());

                    if (subRet.is_False()) {
                        bdd.deref(intersect);
                        continue;
                    }
                    Integer pred = tempSet.get(subRet);
                    if (pred == null) {
                        pred = 0;
                    }
                    int sum = bdd.ref(bdd.or(pred, intersect));
                    bdd.deref(pred);
                    bdd.deref(intersect);
                    tempSet.put(subRet, sum);
                }
            }

            for (Map.Entry<NDD, Integer> entry_a : temp_a.entrySet()) {
                if (entry_a.getValue() != 0) {
                    Integer aps = tempSet.get(entry_a.getKey());
                    if (aps == null)
                        aps = 0;
                    int sum = bdd.ref(bdd.or(aps, entry_a.getValue()));
                    bdd.deref(aps);
                    bdd.deref(entry_a.getValue());
                    tempSet.put(entry_a.getKey(), sum);
                }
            }
            
            for (Map.Entry<NDD, Integer> entry_b : temp_b.entrySet()) {
                if (entry_b.getValue() != 0) {
                    Integer aps = tempSet.get(entry_b.getKey());
                    if (aps == null)
                        aps = 0;
                    int sum = bdd.ref(bdd.or(aps, entry_b.getValue()));
                    bdd.deref(aps);
                    bdd.deref(entry_b.getValue());
                    tempSet.put(entry_b.getKey(), sum);
                }
            }

            if (tempSet.size() == 0)
                return NDDFalse;

            if (tempSet.size() == 1) {
                Iterator iter = tempSet.entrySet().iterator();
                Map.Entry<NDD, Integer> entry = (Map.Entry<NDD, Integer>) iter.next();
                if (entry.getValue() == 1) {
                    return entry.getKey();
                }
            }
            return table.mk(a.field, tempSet);
        } else {
            if (a.field > b.field) {
                NDD t = a;
                a = b;
                b = t;
            }

            HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
            int false_set = 1;
            Iterator itera = a.edges.entrySet().iterator();

            while (itera.hasNext()) {
                Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
                int n = bdd.ref(bdd.not(entrya.getValue()));
                int temp = bdd.ref(bdd.and(false_set, n));
                bdd.deref(false_set);
                bdd.deref(n);
                false_set = temp;
                NDD subRet = ORRec(entrya.getKey(), b);
                if (subRet.is_False())
                    continue;
                Integer pred = tempSet.get(subRet);
                if (pred == null)
                    pred = 0;
                int sum = bdd.ref(bdd.or(pred, entrya.getValue()));
                bdd.deref(pred);
                tempSet.put(subRet, sum);
            }

            if (false_set != 0) {
                Integer aps = tempSet.get(b);
                if (aps == null) {
                    aps = 0;
                }
                int sum = bdd.ref(bdd.or(aps, false_set));
                bdd.deref(aps);
                bdd.deref(false_set);
                tempSet.put(b, sum);
            }

            if (tempSet.size() == 0)
                return NDDFalse;

            if (tempSet.size() == 1) {
                Iterator iter = tempSet.entrySet().iterator();
                Map.Entry<NDD, Integer> entry = (Map.Entry<NDD, Integer>) iter.next();
                if (entry.getValue() == 1) {
                    return entry.getKey();
                }
            }

            return table.mk(a.field, tempSet);
        }
    }

    public static NDD Not(NDD a) {
        if (test) {
            Long t0 = System.nanoTime();
            NDD ret = NotRec(a);
            Long t1 = System.nanoTime();
            NOTTime += t1 - t0;
            return ret;
        }

        return NotRec(a);
    }

    private static NDD NotRec(NDD a) {
        if (a.is_True())
            return NDDFalse;
        if (a.is_False())
            return NDDTrue;

        HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
        Integer false_set = 1;
        Iterator itera = a.edges.entrySet().iterator();

        while (itera.hasNext()) {
            Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
            int n = bdd.ref(bdd.not(entrya.getValue()));
            int temp = bdd.ref(bdd.and(false_set, n));
            bdd.deref(false_set);
            bdd.deref(n);
            false_set = temp;
            NDD subRet = NotRec(entrya.getKey());
            if (subRet.is_False())
                continue;
            Integer pred = tempSet.get(subRet);
            if (pred == null)
                pred = 0;
            int sum = bdd.ref(bdd.or(pred, entrya.getValue()));
            bdd.deref(pred);
            tempSet.put(subRet, sum);
        }

        if (false_set != 0) {
            Integer aps = tempSet.get(NDDTrue);
            if (aps == null)
                aps = 0;
            int sum = bdd.ref(bdd.or(aps, false_set));
            bdd.deref(aps);
            bdd.deref(false_set);
            tempSet.put(NDDTrue, sum);
        }

        if (tempSet.size() == 0)
            return NDDFalse;

        if (tempSet.size() == 1) {
            Iterator iter = tempSet.entrySet().iterator();
            Map.Entry<NDD, Integer> entry = (Map.Entry<NDD, Integer>) iter.next();
            if (entry.getValue() == 1) {
                return entry.getKey();
            }
        }

        return table.mk(a.field, tempSet);
    }

    public static NDD Diff(NDD a, NDD b) {
        NDD n = Not(b);
        NDD ret = AND(a, n);
        if (n != ret)
            table.testAndDelete(n);

        return ret;
    }

    public static NDD Exist(NDD a, int field) {
        if (test) {
            Long t0 = System.nanoTime();
            NDD ret = ExistRec(a, field);
            Long t1 = System.nanoTime();
            ExistTime += t1 - t0;
            return ret;
        }

        return ExistRec(a, field);
    }

    private static NDD ExistRec(NDD a, int field) {
        if (a.is_True() || a.is_False())
            return a;
        if (a.field > field)
            return a;

        if (a.field == field) {
            NDD sum = null;

            for (NDD next : a.edges.keySet()) {
                if (sum == null) {
                    sum = next;
                } else {
                    NDD old = sum;
                    sum = NDD.OR(sum, next);
                    if (old != sum)
                        table.testAndDelete(old);
                }
            }

            return sum;
        } else {
            HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();

            for (NDD next : a.edges.keySet()) {
                NDD subRet = ExistRec(next, field);
                if (subRet.is_False())
                    continue;
                int aps = 0;
                if (tempSet.containsKey(subRet))
                    aps = tempSet.get(subRet);
                int t = aps;
                aps = bdd.ref(bdd.or(aps, a.edges.get(next)));
                bdd.deref(t);
                tempSet.put(subRet, aps);
            }

            if (tempSet.size() == 0)
                return NDDFalse;

            if (tempSet.size() == 1) {
                for (NDD key : tempSet.keySet()) {
                    if (tempSet.get(key) == 1) {
                        return key;
                    }
                }
            }

            return table.mk(a.field, tempSet);
        }
    }

    public static ArrayList<int[]> ToArray(NDD curr) {
        ArrayList<int[]> array = new ArrayList<>();
        int[] vec = new int[fieldNum];

        ToArray_rec(curr, array, vec, 0);

        return array;
    }

    private static void ToArray_rec(NDD curr, ArrayList<int[]> array, int[] vec, int currField) {
        if (curr.is_False())
            return;

        if (curr.is_True()) {
            for (int i = currField; i < fieldNum; i++) {
                vec[i] = 1;
            }
            int[] temp = new int[fieldNum];
            for (int i = 0; i < fieldNum; i++) {
                temp[i] = vec[i];
            }
            array.add(temp);
            return;
        }

        for (int i = currField; i < curr.field; i++) {
            vec[i] = 1;
        }
        Iterator iter = curr.edges.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<NDD, Integer> entry = (Map.Entry<NDD, Integer>) iter.next();
            vec[curr.field] = entry.getValue();

            ToArray_rec(entry.getKey(), array, vec, curr.field + 1);
        }
    }

    public static int toBDD(NDD curr) {
        if (curr.is_True())
            return 1;
        if (curr.is_False())
            return 0;

        int ret = 0;
        for (NDD next : curr.edges.keySet()) {
            int curr_field = curr.edges.get(next);
            int next_field = toBDD(next);
            curr_field = bdd.ref(bdd.and(curr_field, next_field));
            bdd.deref(next_field);
            int t = ret;
            ret = bdd.ref(bdd.or(t, curr_field));
            bdd.deref(t);
            bdd.deref(curr_field);
        }

        return ret;
    }

    public static NDD toNDD(int a) {
        if (test) {
            Long t0 = System.nanoTime();
            NDD ret = toNDDFunc(a);
            Long t1 = System.nanoTime();
            ToNDDTime += t1 - t0;
            return ret;
        }

        return toNDDFunc(a);
    }

    private static NDD toNDDFunc(int a) {
        HashMap<Integer, HashMap<Integer, Integer>> decomposed = decompose(a);
        HashMap<Integer, NDD> converted = new HashMap<>();
        converted.put(1, NDDTrue);

        while (decomposed.size() != 0) {
            Set<Integer> finished = converted.keySet();
            for (Map.Entry<Integer, HashMap<Integer, Integer>> entry : decomposed.entrySet()) {
                if (finished.containsAll(entry.getValue().keySet())) {
                    int field = bdd_getField(entry.getKey());
                    HashMap<NDD, Integer> map = new HashMap<>();

                    for (Map.Entry<Integer, Integer> entry1 : entry.getValue().entrySet()) {
                        map.put(converted.get(entry1.getKey()), bdd.ref(entry1.getValue()));
                    }

                    NDD n = table.mk(field, map);
                    converted.put(entry.getKey(), n);
                    decomposed.remove(entry.getKey());
                    break;
                }
            }
        }

        for (HashMap<Integer, Integer> map : decomposed.values()) {
            for (Integer pred : map.values()) {
                bdd.deref(pred);
            }
        }

        return converted.get(a);
    }

    private static HashMap<Integer, HashMap<Integer, Integer>> decompose(int a) {
        HashMap<Integer, HashMap<Integer, Integer>> decomposed_bdd = new HashMap<Integer, HashMap<Integer, Integer>>();
        if (a == 0)
            return decomposed_bdd;
        if (a == 1) {
            HashMap<Integer, Integer> map = new HashMap<>();
            map.put(1, 1);
            decomposed_bdd.put(1, map);
            return decomposed_bdd;
        }
        HashMap<Integer, HashSet<Integer>> boundary_tree = new HashMap<Integer, HashSet<Integer>>();
        ArrayList<HashSet<Integer>> boundary_points = new ArrayList<HashSet<Integer>>();

        get_boundary_tree(a, boundary_tree, boundary_points);

        for (int curr_level = 0; curr_level < fieldNum - 1; curr_level++) {
            for (int root : boundary_points.get(curr_level)) {
                for (int end_point : boundary_tree.get(root)) {
                    int res = construct_decomposed_bdd(root, end_point, root);
                    bdd.ref(res); // ref deref!!!
                    if (!decomposed_bdd.containsKey(root)) {
                        decomposed_bdd.put(root, new HashMap<Integer, Integer>());
                    }
                    decomposed_bdd.get(root).put(end_point, res);
                }
            }
        }

        for (int abdd : boundary_points.get(fieldNum - 1)) {
            if (!decomposed_bdd.containsKey(abdd)) {
                decomposed_bdd.put(abdd, new HashMap<Integer, Integer>());
            }
            decomposed_bdd.get(abdd).put(1, bdd.ref(abdd));
        }

        return decomposed_bdd;
    }

    private static void get_boundary_tree(int a, HashMap<Integer, HashSet<Integer>> boundary_tree,
            ArrayList<HashSet<Integer>> boundary_points) {
        int start_level = bdd_getField(a);

        for (int curr = 0; curr < fieldNum; curr++) {
            boundary_points.add(new HashSet<Integer>());
        }

        boundary_points.get(start_level).add(a);
        if (start_level == fieldNum - 1) {
            boundary_tree.put(a, new HashSet<Integer>());
            boundary_tree.get(a).add(1);
            return;
        }

        for (int curr_level = start_level; curr_level < fieldNum; curr_level++) {
            for (int abdd : boundary_points.get(curr_level)) {
                detect_boundary_point(abdd, abdd, boundary_tree, boundary_points);
            }
        }
    }

    private static int bdd_getField(int a) {
        int va = bdd.getVar(a);
        if (a == 1 || a == 0)
            return fieldNum;

        int curr = 0;
        while (curr < fieldNum) {
            if (va <= upperBound[curr]) {
                break;
            }
            curr++;
        }

        return curr;
    }

    private static void detect_boundary_point(int root, int curr, HashMap<Integer, HashSet<Integer>> boundary_tree,
            ArrayList<HashSet<Integer>> boundary_points) {
        if (curr == 0)
            return;
        if (curr == 1) {
            if (!boundary_tree.containsKey(root)) {
                boundary_tree.put(root, new HashSet<Integer>());
            }
            boundary_tree.get(root).add(1);
            return;
        }

        if (bdd_getField(root) != bdd_getField(curr)) {
            if (!boundary_tree.containsKey(root)) {
                boundary_tree.put(root, new HashSet<Integer>());
            }
            boundary_tree.get(root).add(curr);
            boundary_points.get(bdd_getField(curr)).add(curr);
            return;
        }

        detect_boundary_point(root, bdd.getLow(curr), boundary_tree, boundary_points);
        detect_boundary_point(root, bdd.getHigh(curr), boundary_tree, boundary_points);
    }

    private static int construct_decomposed_bdd(int root, int end_point, int curr) {
        if (curr == 0)
            return curr;
        if (curr == 1) {
            if (end_point == 1)
                return 1;
            else
                return 0;
        } else if (bdd_getField(root) != bdd_getField(curr)) {
            if (end_point == curr)
                return 1;
            else
                return 0;
        }

        int low = bdd.getLow(curr);
        int high = bdd.getHigh(curr);

        int new_low = construct_decomposed_bdd(root, end_point, low);
        bdd.ref(new_low);
        int new_high = construct_decomposed_bdd(root, end_point, high);
        bdd.ref(new_high);

        int result = bdd.mk(bdd.getVar(curr), new_low, new_high);
        bdd.deref(new_low);
        bdd.deref(new_high);
        return result;
    }

    public static double satCount(NDD curr, double[] div) {
        if (curr.is_False()) {
            return 0;
        } else if (curr.is_True()) {
            return 1;
        } else {
            double sum = 0;
            for (NDD next : curr.edges.keySet()) {
                double subCurr = bdd.satCount(curr.edges.get(next)) / div[curr.field];
                double subNext = satCount(next, div);
                sum = sum + (subNext * subCurr);
            }

            return sum;
        }
    }

    public static void printOut(NDD a) {
        printOutRec(a);
        System.out.println();
    }

    private static void printOutRec(NDD curr) {
        if (curr.is_True()) {
            System.out.println("T");
            return;
        }
        if (curr.is_False()) {
            System.out.println("F");
            return;
        }
        
        System.out.print(curr + " " + curr.field + " " + curr.edges.size());
        for (NDD next : curr.edges.keySet()) {
            if (next.is_True()) {
                System.out.print(" T");
            } else if (next.is_False()) {
                System.out.print(" F");
            } else {
                System.out.print(" " + next);
            }
        }
        System.out.println();
        for (NDD next : curr.edges.keySet()) {
            printOutRec(next);
        }
    }

    public static void showStatestic() {
        System.out.println("And:" + ANDTime / 1000000000.0);
        System.out.println("Or:" + ORTime / 1000000000.0);
        System.out.println("Not:" + NOTTime / 1000000000.0);
        System.out.println("Exist:" + ExistTime / 1000000000.0);
        System.out.println("ToNDD:" + ToNDDTime / 1000000000.0);
    }
}