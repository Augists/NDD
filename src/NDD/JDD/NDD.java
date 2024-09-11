package ndd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javafx.util.Pair;

import jdd.bdd.BDD;
import jdd.util.Dot;

public class NDD {
    public static int fieldNum;
    public static int[] upperBound;
    public static double[] div;

    public static NDD NDDTrue = new NDD();
    public static NDD NDDFalse = new NDD();
    public static BDD bdd;
    public static NodeTable table;

    public int field;
    public HashMap<NDD, Integer> edges;
    
    public static int MAX_SIZE = 8000;
    public static int CACHE_SIZE = 400;

    public static void SetFieldNum(int num) {
        table = new NodeTable(MAX_SIZE, CACHE_SIZE);
        fieldNum = num;
        for (int i = 0; i < num; i++) {
            table.NDDs.add(new HashMap<>());
        }
    }

    public static void SetUpperBound(int[] upper) {
        upperBound = upper;
        int varNum = upper[0] + 1;
        for (int i = 1; i < fieldNum; i++) {
            if (upper[i] - upper[i - 1] > varNum)
                varNum = upper[i] - upper[i - 1];
        }
        div = new double[fieldNum];
        div[0] = Math.pow(2.0, varNum - upper[0] - 1);
        for (int i = 1; i < fieldNum; i++) {
            div[i] = Math.pow(2.0, varNum - (upper[i] - upper[i - 1]));
        }
    }

    public static NDD addAtField(int field, HashMap<NDD, Integer> map) {
        return table.mk(field, map);
    }

    public NDD(int field, HashMap<NDD, Integer> edges) {
        this.field = field;
        this.edges = edges;
    }

    public NDD() {
        field = fieldNum + 1;
    }

    public boolean equals(NDD obj) {
        return this == obj;
    }

    public boolean is_True() {
        return this == NDDTrue;
    }

    public boolean is_False() {
        return this == NDDFalse;
    }

    public static NDD ref(NDD ndd) {
        return table.ref(ndd);
    }

    public static void deref(NDD ndd) {
        table.deref(ndd);
    }

    public static NDD andTo(NDD a, NDD b) {
        NDD t = ref(AND(a, b));
        deref(a);
        return t;
    }

    public static NDD orTo(NDD a, NDD b) {
        NDD t = ref(OR(a, b));
        deref(a);
        return t;
    }

    public static HashSet<NDD> toProtect = new HashSet<>();

    public static NDD AND(NDD a, NDD b) {
        if (table.lazyGC)
            toProtect.clear();

        NDD ret = ANDRec(a, b);
        return ret;
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

        int hash = 0;
        if (table.lazyGC) {
            if (table.andCache.lookup(a, b))
                return table.andCache.answer;
            hash = table.andCache.hashValue;
        }

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
                    if (pred == null)
                        pred = 0;
                    int sum = bdd.ref(bdd.or(pred, intersect));
                    bdd.deref(pred);
                    bdd.deref(intersect);
                    tempSet.put(subRet, sum);
                }
            }
            NDD ret = table.mk(a.field, tempSet);
            if (table.lazyGC) {
                toProtect.add(ret);
                table.andCache.insert(hash, a, b, ret);
            }
            return ret;
        } else {
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
                if (pred == null)
                    pred = 0;
                int sum = bdd.ref(bdd.or(pred, entrya.getValue()));
                bdd.deref(pred);
                tempSet.put(subRet, sum);
            }
            NDD ret = table.mk(a.field, tempSet);
            if (table.lazyGC) {
                toProtect.add(ret);
                table.andCache.insert(hash, a, b, ret);
            }
            return ret;
        }
    }

    public static NDD OR(NDD a, NDD b) {
        if (table.lazyGC)
            toProtect.clear();

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

        int hash = 0;
        if (table.lazyGC) {
            if (table.orCache.lookup(a, b))
                return table.orCache.answer;
            hash = table.orCache.hashValue;
        }

        if (a.field == b.field) {
            HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
            HashMap<NDD, Integer> temp_a = new HashMap<NDD, Integer>(a.edges);
            for (int oneBDD : a.edges.values()) {
                bdd.ref(oneBDD);// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            }
            HashMap<NDD, Integer> temp_b = new HashMap<NDD, Integer>(b.edges);
            for (int oneBDD : b.edges.values()) {
                bdd.ref(oneBDD);// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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
                    if (pred == null)
                        pred = 0;
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
            NDD ret = table.mk(a.field, tempSet);
            if (table.lazyGC) {
                toProtect.add(ret);
                table.orCache.insert(hash, a, b, ret);
            }
            return ret;
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
                if (aps == null)
                    aps = 0;
                int sum = bdd.ref(bdd.or(aps, false_set));
                bdd.deref(aps);
                bdd.deref(false_set);
                tempSet.put(b, sum);
            }
            NDD ret = table.mk(a.field, tempSet);
            if (table.lazyGC) {
                toProtect.add(ret);
                table.orCache.insert(hash, a, b, ret);
            }
            return ret;
        }
    }

    public static NDD Not(NDD a) {
        if (table.lazyGC)
            toProtect.clear();

        return NotRec(a);
    }

    private static NDD NotRec(NDD a) {
        if (a.is_True())
            return NDDFalse;
        if (a.is_False())
            return NDDTrue;

        int hash = 0;
        if (table.lazyGC) {
            if (table.notCache.lookup(a))
                return table.notCache.answer;
            hash = table.notCache.hashValue;
        }

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
        NDD ret = table.mk(a.field, tempSet);
        if (table.lazyGC) {
            toProtect.add(ret);
            table.notCache.insert(hash, a, ret);
        }
        return ret;
    }

    public static NDD Diff(NDD a, NDD b) {
        NDD n = Not(b);
        if (table.lazyGC)
            ref(n);
        NDD ret = AND(a, n);
        if (table.lazyGC) {
            deref(n);
        } else if (n != ret) {
            table.testAndDelete(n);
        }
        return ret;
    }

    public static NDD Exist(NDD a, int field) {
        if (table.lazyGC)
            toProtect.clear();

        return ExistRec(a, field);
    }

    private static NDD ExistRec(NDD a, int field) { // not updated with lazyGC
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
                    if (tempSet.get(key) == 1)
                        return key;
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
            curr_field = bdd.ref(bdd.replace(curr_field, NDDController.bddManager.permutations[curr.field]));
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
                    bdd.ref(res);// ref deref!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    if (!decomposed_bdd.containsKey(root))
                        decomposed_bdd.put(root, new HashMap<Integer, Integer>());
                    decomposed_bdd.get(root).put(end_point, res);
                }
            }
        }

        for (int abdd : boundary_points.get(fieldNum - 1)) {
            if (!decomposed_bdd.containsKey(abdd))
                decomposed_bdd.put(abdd, new HashMap<Integer, Integer>());
            decomposed_bdd.get(abdd).put(1, bdd.ref(abdd));
        }

        return decomposed_bdd;
    }

    private static void get_boundary_tree(int a, HashMap<Integer, HashSet<Integer>> boundary_tree,
            ArrayList<HashSet<Integer>> boundary_points) {
        int start_level;
        start_level = bdd_getField(a);
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
            if (va <= upperBound[curr])
                break;
            curr++;
        }
        return curr;
    }

    private static void detect_boundary_point(int root, int curr, HashMap<Integer, HashSet<Integer>> boundary_tree,
            ArrayList<HashSet<Integer>> boundary_points) {
        if (curr == 0)
            return;
        if (curr == 1) {
            if (!boundary_tree.containsKey(root))
                boundary_tree.put(root, new HashSet<Integer>());
            boundary_tree.get(root).add(1);
            return;
        }
        if (bdd_getField(root) != bdd_getField(curr)) {
            if (!boundary_tree.containsKey(root))
                boundary_tree.put(root, new HashSet<Integer>());
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

        // changed!!!!!!!!!!!!
        int field = bdd_getField(curr);
        int result = 0;
        if (field == 0) {
            result = bdd.mk(bdd.getVar(curr), new_low, new_high);
        } else {
            result = bdd.mk(bdd.getVar(curr) - upperBound[field - 1] - 1, new_low, new_high);
        }
        bdd.deref(new_low);
        bdd.deref(new_high);
        return result;
    }

    // <varNum, k> -> bdd
    private static HashMap<Pair<Integer, Integer>, Integer> cache = new HashMap<Pair<Integer, Integer>, Integer>();

    public static NDD encodeAtMostKFailureVarsSorted(BDD bdd, int[] vars, int startField, int endField, int k) {
        return encodeAtMostKFailureVarsSortedRec(bdd, vars, endField, startField, k);
    }

    private static NDD encodeAtMostKFailureVarsSortedRec(BDD bdd, int[] vars, int endField, int currField, int k) {
        if (currField > endField)
            return NDDTrue;
        int fieldSize = upperBound[0] + 1;
        if (currField > 0)
            fieldSize = upperBound[currField] - upperBound[currField - 1];
        HashMap<NDD, Integer> map = new HashMap<NDD, Integer>();
        for (int i = 0; i <= k; i++) {
            int pred = 0;
            if (cache.containsKey(new Pair<Integer, Integer>(fieldSize, i))) {
                pred = cache.get(new Pair<Integer, Integer>(fieldSize, i));
            } else {
                pred = bdd.ref(encodeBDD(bdd, vars, fieldSize - 1, 0, i));
                cache.put(new Pair<Integer, Integer>(fieldSize, i), pred);
            }
            NDD next = encodeAtMostKFailureVarsSortedRec(bdd, vars, endField, currField + 1, k - i);
            int nextPred = 0;
            if (map.containsKey(next))
                nextPred = map.get(next);
            else
                nextPred = 0;
            bdd.ref(pred);
            int t = bdd.ref(bdd.or(pred, nextPred));
            bdd.deref(pred);
            bdd.deref(nextPred);
            nextPred = t;
            map.put(next, nextPred);
        }
        return NDD.table.mk(currField, map);
    }

    private static int encodeBDD(BDD bdd, int[] vars, int endVar, int currVar, int k) {
        if (k < 0) {
            return 0;
        }
        if (currVar > endVar) {
            if (k > 0)
                return 0;
            else
                return 1;
        }
        int low = encodeBDD(bdd, vars, endVar, currVar + 1, k - 1);
        int high = encodeBDD(bdd, vars, endVar, currVar + 1, k);
        return bdd.mk(bdd.getVar(vars[endVar - currVar]), low, high);
    }

    public static double satCount(NDD curr) {
        return satCountRec(curr, 0);
    }

    private static double satCountRec(NDD curr, int field) {
        if (curr.is_False())
            return 0;
        else if (curr.is_True()) {
            if (field == fieldNum)
                return 1;
            else {
                int len = 0;
                if (field == 0)
                    len = upperBound[field] + 1;
                else
                    len = upperBound[field] - upperBound[field - 1];
                double ret = Math.pow(2.0, len) * satCountRec(curr, field + 1);
                return ret;
            }
        } else {
            if (field == curr.field) {
                double sum = 0;
                for (NDD next : curr.edges.keySet()) {
                    double subCurr = bdd.satCount(curr.edges.get(next)) / div[curr.field];
                    double subNext = satCountRec(next, field + 1);
                    sum = sum + (subNext * subCurr);
                }
                return sum;
            } else {
                int len = 0;
                if (field == 0)
                    len = upperBound[field] + 1;
                else
                    len = upperBound[field] - upperBound[field - 1];
                double ret = Math.pow(2.0, len) * satCountRec(curr, field + 1);
                return ret;
            }
        }
    }

    public static void nodeCount(NDD node) {
        HashSet<NDD> NDDSet = new HashSet<NDD>();
        HashSet<Integer> BDDRootSet = new HashSet<Integer>();
        HashSet<Integer> BDDSet = new HashSet<Integer>();
        detectNDD(node, NDDSet, BDDRootSet);
        int count = 0;
        for (int BDDRoot : BDDRootSet) {
            detectBDD(BDDRoot, BDDSet);
        }
        System.out.println("NDD node:" + NDDSet.size() + " BDD node:" + BDDSet.size());
    }

    private static void detectNDD(NDD node, HashSet<NDD> NDDset, HashSet<Integer> BDDRootSet) {
        if (node.is_True() || node.is_False()) {
            return;
        } else {
            NDDset.add(node);
            for (Map.Entry<NDD, Integer> entry : node.edges.entrySet()) {
                BDDRootSet.add(entry.getValue());
                detectNDD(entry.getKey(), NDDset, BDDRootSet);
            }
        }
    }

    private static void detectBDD(int node, HashSet<Integer> BDDSet) {
        if (node == 1 || node == 0)
            return;
        else {
            if (!BDDSet.contains(node)) {
                BDDSet.add(node);
                detectBDD(bdd.getHigh(node), BDDSet);
                detectBDD(bdd.getLow(node), BDDSet);
            }
        }
    }

    public static int toZero(NDD tc) {
        NDD t = ref(NDD.Not(tc));
        Set<NDD> vi = new HashSet();
        Map<NDD, Integer> dis = new HashMap();
        HashMap<Integer, Integer> cache = new HashMap<>();
        dis.put(t, 0);
        int ans = toZero_rec(t, vi, dis, 0, cache);
        deref(t);
        return ans;
    }

    private static int toZero_rec(NDD tc, Set<NDD> vi, Map<NDD, Integer> dis, int d, HashMap<Integer, Integer> cache) {
        if (tc == NDD.NDDTrue) {
            return d;
        } else {
            vi.add(tc);
            for (Map.Entry<NDD, Integer> entry : tc.edges.entrySet()) {
                NDD next = entry.getKey();
                int pred = entry.getValue();
                int count = 0;
                if (cache.containsKey(pred)) {
                    count = cache.get(pred);
                } else {
                    count = bdd.toOne(pred);// !!!!!!!!!!!!!!!!!
                    cache.put(pred, count);
                }
                if (!dis.containsKey(next) || dis.get(next) > d + count) {
                    dis.put(next, d + count);
                }
            }

            NDD nxtV = NDD.NDDFalse;
            int nxtD = 2147483647;
            Iterator var9 = dis.entrySet().iterator();

            while (var9.hasNext()) {
                Map.Entry<NDD, Integer> entry = (Map.Entry) var9.next();
                if (!vi.contains(entry.getKey()) && (Integer) entry.getValue() < nxtD) {
                    nxtD = (Integer) entry.getValue();
                    nxtV = entry.getKey();
                }
            }

            return toZero_rec(nxtV, vi, dis, nxtD, cache);
        }
    }

    public static void printOut(NDD a) {
        printOutRec(a);
        System.out.println();
    }

    private static void printOutRec(NDD curr) {
        if (curr.is_True()) {
            System.out.println("Node: T");
            return;
        }
        if (curr.is_False()) {
            System.out.println("Node: F");
            return;
        }
        System.out.print(
                "Node: " + curr + " Field: " + curr.field + " Num of edges: " + curr.edges.size() + " Descendants: ");
        for (NDD next : curr.edges.keySet()) {
            if (next.is_True()) {
                System.out.print(" <" + curr.edges.get(next) + ",T>");
            } else if (next.is_False()) {
                System.out.print(" <" + curr.edges.get(next) + ",F>");
            } else {
                System.out.print(" <" + curr.edges.get(next) + "," + next + ">");
            }
        }
        System.out.println();
        for (NDD next : curr.edges.keySet()) {
            printOutRec(next);
        }
    }

    private static PrintStream ps;
    private static HashMap<Pair<NDD, NDD>, Boolean> printFlag = new HashMap<>();

    // pass a new directory would be better
    // NDD.printDot will create a dot for NDD and plenty of dots for BDD
    public static void printDot(String filename, NDD a) {
        try {
            ps = new PrintStream(filename);

            ps.println("digraph G {");

            String[] pathList = filename.split("/");
            String path = String.join("/", Arrays.copyOfRange(pathList, 0, pathList.length - 1));
            printDotRec(path + "/", a);

            ps.println("1 [shape=box, label=\"" + "true" + "\", style=filled, shape=box, height=0.3, width=0.3];");
            ps.println("}");
            ps.close();

            Dot.showDot(filename);
        } catch (IOException exx) {
            System.out.println("NDD.printDOT failed: " + exx);
        }
    }

    private static void printDotRec(String path, NDD a) throws IOException {
        ps.println(a.hashCode() + "[label=\"f" + a.field + "\"];");

        for (NDD next : a.edges.keySet()) {
            Pair<NDD, NDD> p = new Pair<NDD, NDD>(a, next);
            if (printFlag.containsKey(p)) {
                continue;
            }
            printFlag.put(p, true);
            int idx = a.edges.get(next);

            String bdd_path = path + idx;
            File file = new File(bdd_path + ".png");
            if (!file.exists()) {
                file.createNewFile();
                bdd.printDot(bdd_path, idx);
            }

            if (next.is_True()) {
                ps.println(a.hashCode() + " -> " + 1 + " [style=filled label=" + idx + "];");
                continue;
            }
            ps.println(a.hashCode() + " -> " + next.hashCode() + " [style=filled label=" + idx + "];");

            NDD.printDotRec(path, next);
        }
    }

}
