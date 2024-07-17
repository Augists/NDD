package bddvector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// import bdd.NDDTCWrapper;
import jdd.bdd.BDD;

public class NDD {
    public static int fieldNum;

    public static int[] upperBound;
    public static int[] varNumList;
    public static int varNumTotal;
    public static int maxVarsLength; // = varNumTotal if not reuse
    // public static double[] div;

    public static NDD NDDTrue = new NDD(true);
    public static NDD NDDFalse = new NDD(false);
    public static BDD bdd;

    public static int[] vars;
    public static int[] otherVars; // convert between bdd and vector

    public static boolean reuse = true;

    public Set<ArrayList<Integer>> vectors;

    public static int[] createVar(int maxNum) {
        maxVarsLength = maxNum;

        // bdd reuse so use max field num
        vars = new int[maxNum];
        if (reuse) {
            otherVars = new int[varNumTotal];

            // first <- last for reuse
            for (int i = maxNum - 1; i >= 0; i--) {
                vars[i] = bdd.createVar();
            }
            // create maxVarsLength + varNumTotal bdd vars
            for (int i = 0; i < varNumTotal; i++) {
                otherVars[i] = bdd.createVar();
            }
        } else {
            for (int i = 0; i < maxNum; i++) {
                vars[i] = bdd.createVar();
            }
        }
        return vars;
    }

    public static void SetFieldNum(int num) {
        fieldNum = num;

        // lazy init for NDD True [1, 1, 1] and False [0, 0, 0]
        for (ArrayList<Integer> vector : NDDTrue.vectors) {
            for (int i = 0; i < fieldNum; i++) {
                vector.add(1);
            }
        }
        for (ArrayList<Integer> vector : NDDFalse.vectors) {
            for (int i = 0; i < fieldNum; i++) {
                vector.add(0);
            }
        }
    }

    public static void SetUpperBound(int[] upper) {
        upperBound = upper;

        varNumTotal = upper[upper.length - 1] + 1;

        varNumList = new int[fieldNum];
        varNumList[0] = upper[0] + 1;
        for (int i = 1; i < fieldNum; i++) {
            varNumList[i] = upper[i] - upper[i - 1];
        }

        // int length = varNumList[0];
        // if (NDDTCWrapper.useToBDD) {
        // // boolean useToBDD = true;
        // // if (useToBDD) {
        // length = upper[fieldNum - 1] + 1;
        // } else {
        // for (int i = 1; i < fieldNum; i++) {
        // if (upper[i] - upper[i - 1] > length)
        // length = upper[i] - upper[i - 1];
        // }
        // }

        // div = new double[fieldNum];
        // div[0] = Math.pow(2.0, length - upper[0] - 1);
        // for (int i = 1; i < fieldNum; i++) {
        // div[i] = Math.pow(2.0, length - (upper[i] - upper[i - 1]));
        // }
    }

    public NDD() {
        vectors = new HashSet<>();
    }

    public NDD(boolean flag) {
        // lazy init NDD True and False
        // field = fieldNum + 1;
        vectors = new HashSet<>();
        ArrayList<Integer> vector = new ArrayList<Integer>();
        vectors.add(vector);
    }

    public NDD(Set<ArrayList<Integer>> ret) {
        vectors = ret;
    }

    public NDD(NDD ret) {
        // deep copy
        this.vectors = new HashSet<>();
        for (ArrayList<Integer> vector : ret.vectors) {
            ArrayList<Integer> newVector = new ArrayList<>(vector);
            this.vectors.add(newVector);
        }
        // ref all vector when using deep copy of NDD vectors
        // ref(this);
    }

    public boolean equals(NDD obj) {
        if (this.vectors.size() != obj.vectors.size())
            return false;
        return this.vectors == obj.vectors;
    }

    public boolean is_True() {
        // return this == NDDTrue;
        if (this == NDDTrue) {
            return true;
        }
        for (ArrayList<Integer> vector : vectors) {
            int i = 0;
            for (; i < vector.size(); i++) {
                if (vector.get(i) != 1) {
                    break;
                }
            }
            if (i == fieldNum) {
                return true;
            }
        }
        return false;
    }

    public boolean is_False() {
        // do not only use pointer compare to judge NDDFalse
        if (this == NDDFalse) {
            return true;
        }
        // only if every vector has 0 can be regarded as False
        for (ArrayList<Integer> vector : vectors) {
            boolean flag = false;
            for (int i = 0; i < vector.size(); i++) {
                if (vector.get(i) == 0) {
                    flag = true;
                    break;
                }
            }
            if (!flag)
                return false;
        }
        return true;
    }

    public static NDD ref(NDD ndd) {
        for (ArrayList<Integer> vector : ndd.vectors) {
            for (int i = 0; i < vector.size(); i++) {
                bdd.ref(vector.get(i));
            }
        }
        return ndd;
    }

    public static void deref(NDD ndd) {
        for (ArrayList<Integer> vector : ndd.vectors) {
            for (int i = 0; i < vector.size(); i++) {
                bdd.deref(vector.get(i));
            }
        }
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

    public static boolean checkCorrectness = false;

    public static NDD AND(NDD a, NDD b) {
        NDD ret = ANDRec(a, b);
        return ret;
    }

    private static NDD ANDRec(NDD a, NDD b) {
        if (a.is_False() || b.is_False())
            return NDDFalse;
        if (a.is_True())
            return ref(new NDD(b));
        if (b.is_True())
            return ref(new NDD(a));
        if (a.equals(b))
            return ref(new NDD(a));

        NDD ret = new NDD();
        for (ArrayList<Integer> vectorA : a.vectors) {
            for (ArrayList<Integer> vectorB : b.vectors) {
                ArrayList<Integer> vectorC = new ArrayList<>();
                // remove vector with any idx pointed to bdd false
                boolean flag = false;
                for (int i = 0; i < vectorA.size(); i++) {
                    int idx = bdd.and(vectorA.get(i), vectorB.get(i));
                    if (idx == 0) {
                        flag = true;
                    }
                    vectorC.add(idx);
                }
                if (!flag) {
                    // ref the whole vector if will be added
                    for (int i = 0; i < vectorC.size(); i++) {
                        bdd.ref(vectorC.get(i));
                    }
                    ret.vectors.add(vectorC);
                }
            }
        }
        // already ref in process
        return ret;
    }

    public static NDD OR(NDD a, NDD b) {
        return ORRec(a, b);
    }

    private static NDD ORRec(NDD a, NDD b) {
        if (a.is_True() || b.is_True())
            return NDDTrue;
        if (a.is_False())
            return ref(new NDD(b));
        if (b.is_False())
            return ref(new NDD(a));
        if (a.equals(b))
            return ref(new NDD(a));

        NDD ret = new NDD();
        ret.vectors.addAll(a.vectors);
        ret.vectors.addAll(b.vectors);
        return ref(ret);
    }

    public static NDD Not(NDD a) {
        // return NotRec(a);
        // return NotRecDirectly(a);
        return NotRecBackBDD(a);
    }

    private static NDD NotRecBackBDD(NDD a) {
        ref(a);
        int bddidx = bdd.ref(toBDD(a));
        int idx = bdd.ref(bdd.not(bddidx));
        NDD ret = toNDD(idx);
        bdd.deref(bddidx);
        bdd.deref(idx);
        deref(a);
        return ref(ret);
    }

    private static NDD NotRecDirectly(NDD a) {
        if (a.is_True())
            return NDDFalse;
        if (a.is_False())
            return NDDTrue;

        if (a.vectors.size() == 1) {
            return new NDD(NotRecSingleVector(a.vectors.iterator().next()));
        }

        // ret: vectors
        // vector: vector to be split and bdd.and only at the same idx
        HashSet<ArrayList<Integer>> ret = null;
        for (ArrayList<Integer> vector : a.vectors) {
            if (ret == null) {
                ret = NotRecSingleVector(vector);
                continue;
            }
            HashSet<ArrayList<Integer>> retNext = new HashSet<>();
            for (int i = 0; i < fieldNum; i++) {
                if (vector.get(i) == 1) {
                    continue;
                }

                int vectorIdxRootNot = bdd.ref(bdd.not(vector.get(i)));
                for (ArrayList<Integer> retVector : ret) {
                    int idx = bdd.and(retVector.get(i), vectorIdxRootNot);
                    if (idx == 0)
                        continue;
                    ArrayList<Integer> temp = new ArrayList<>(retVector);
                    temp.set(i, bdd.ref(idx));
                    retNext.add(temp);
                }
                bdd.deref(vectorIdxRootNot);
            }

            // vector [1, 1, 1]
            if (retNext.isEmpty())
                continue;

            ret.clear();
            ret = retNext;
        }
        return ref(new NDD(ret));
    }

    private static HashSet<ArrayList<Integer>> NotRecSingleVector(ArrayList<Integer> vector) {
        HashSet<ArrayList<Integer>> ret = new HashSet<>();

        for (int i = 0; i < fieldNum; i++) {
            // ignore 1 for its not will be 0 which cannot be in vectors
            if (vector.get(i) == 1) {
                continue;
            }

            ArrayList<Integer> temp = new ArrayList<>();
            for (int j = 0; j < fieldNum; j++) {
                temp.add(1);
            }
            temp.set(i, bdd.ref(bdd.not(vector.get(i))));
            ret.add(temp);
        }

        if (ret.isEmpty()) {
            // [1, 1, 1] in and [0, 0, 0] out
            ArrayList<Integer> temp = new ArrayList<>();
            for (int i = 0; i < fieldNum; i++) {
                temp.add(0);
            }
        }
        return ret;
    }

    private static NDD NotRec(NDD a) {
        if (a.is_True())
            return NDDFalse;
        if (a.is_False())
            return NDDTrue;

        NDD ret = NDD.NDDTrue;

        for (ArrayList<Integer> vector : a.vectors) {
            NDD tmp = new NDD();
            for (int i = 0; i < vector.size(); i++) {
                if (vector.get(i) == 1)
                    continue;
                ArrayList<Integer> temp = new ArrayList<>(); // not 1 1 1 1
                for (int j = 0; j < vector.size(); j++) {
                    temp.add(1);
                }
                temp.set(i, bdd.ref(bdd.not(vector.get(i))));
                tmp.vectors.add(temp);
            }
            NDD t = ret;
            ret = NDD.ref(NDD.AND(ret, tmp));
            NDD.deref(t);
            NDD.deref(tmp);
        }

        return ref(ret);
    }

    public static NDD Diff(NDD a, NDD b) {
        return ref(AND(a, Not(b)));
    }

    public static NDD Exist(NDD a, int field) {
        return ExistRec(a, field);
    }

    private static NDD ExistRec(NDD a, int field) {
        if (a.is_True() || a.is_False())
            return a;

        NDD ret = new NDD(a);
        for (ArrayList<Integer> vector : ret.vectors) {
            vector.set(field, 1);
        }
        return ref(ret);
        // for (ArrayList<Integer> vector : a.vectors) {
        // Set<ArrayList<Integer>> vectorSet = new HashSet<>();
        // for (int i = 0; i < field; i++) {
        // vector.set(i, 0);
        // }
        // vectorSet.add(vector);
        // NDD temp = new NDD(vectorSet);
        // ret = NDD.ORRec(ret, temp);
        // }
        // NDD temp = new NDD(a);
        // for (int i = field; i < fieldNum; i++) {
        // for (ArrayList<Integer> vector : temp.vectors) {
        // vector.set(i, 0);
        // }
        // }
        // return NDD.ORRec(ret, temp);
    }

    private static int bdd_getField(int a) {
        if (reuse)
            return bdd_getField_reuse(a);
        return bdd_getField_not_reuse(a);
    }

    // not reuse
    private static int bdd_getField_not_reuse(int a) {
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

    // reuse
    private static int bdd_getField_reuse(int a) {
        int va = bdd.getVar(a);
        if (a == 1 || a == 0)
            return fieldNum;
        int curr = 0;
        while (curr < fieldNum) {
            if (va - maxVarsLength <= upperBound[curr]) {
                break;
            }
            curr++;
        }
        return curr;
    }

    public static int toBDD(NDD n) {
        if (reuse)
            return toBDDReuse(n);
        return toBDDNotReuse(n);
    }

    // bdd not reuse
    public static int toBDDNotReuse(NDD n) {
        ArrayList<Integer> bdds = new ArrayList<>();
        for (ArrayList<Integer> vector : n.vectors) {
            int lastIdx = 1;
            for (int i = vector.size() - 1; i >= 0; i--) {
                int idx = vector.get(i);
                int temp = lastIdx;
                lastIdx = bdd.and(idx, lastIdx);
                bdd.ref(lastIdx);
                bdd.deref(temp);
                bdd.deref(idx);
            }
            bdds.add(lastIdx);
        }

        int ret = 0;
        for (int i = 0; i < bdds.size(); i++) {
            int temp = ret;
            int idx = bdds.get(i);
            ret = bdd.ref(bdd.or(ret, idx));
            bdd.deref(idx);
            bdd.deref(temp);
        }
        return ret;
    }

    // bdd reuse
    public static int toBDDReuse(NDD n) {
        if (n.is_True()) {
            return 1;
        }

        ArrayList<Integer> bdds = new ArrayList<>();
        for (ArrayList<Integer> vector : n.vectors) {
            int lastIdx = 1;
            for (int i = 0; i < vector.size(); i++) {
                int idx = vector.get(i);
                if (idx == 1)
                    continue;

                int length = varNumList[i];
                int[] from = Arrays.copyOfRange(vars, 0, length);
                // reverse from for its initialize from last to first
                for (int j = 0; j < length / 2; j++) {
                    int temp = from[j];
                    from[j] = from[length - j - 1];
                    from[length - j - 1] = temp;
                }
                int[] to = Arrays.copyOfRange(otherVars, upperBound[i] + 1 - length, upperBound[i] + 1);

                jdd.bdd.Permutation perm = bdd.createPermutation(from, to);
                idx = bdd.ref(bdd.replace(idx, perm));

                int temp = lastIdx;
                lastIdx = bdd.ref(bdd.and(idx, lastIdx));
                bdd.deref(temp);
                bdd.deref(idx); // deref?
            }
            bdds.add(lastIdx);
        }

        int ret = 0;
        for (int i = 0; i < bdds.size(); i++) {
            int temp = ret;
            int idx = bdds.get(i);
            ret = bdd.ref(bdd.or(ret, idx));
            bdd.deref(idx);
            bdd.deref(temp);
        }
        return ret;
    }

    public static NDD toNDD(int a) {
        return toNDDRec(a);
    }

    private static NDD toNDDRec(int a) {
        // decomposed: from idx -> {to idx -> bdd}
        HashMap<Integer, HashMap<Integer, Integer>> decomposed = decompose(a);

        ArrayList<Integer> temp = new ArrayList<>();
        Set<ArrayList<Integer>> vectors = new HashSet<>();

        for (int i = 0; i < bdd_getField(a); i++) {
            temp.add(1);
        }

        toBDDvectorDFS(vectors, decomposed, a, temp);

        // replace
        if (reuse) {
            for (ArrayList<Integer> vector : vectors) {
                for (int field = 0; field < fieldNum; field++) {
                    if (vector.get(field) == 1 || vector.get(field) == 0) {
                        continue;
                    }

                    int length = varNumList[field];
                    int[] from = Arrays.copyOfRange(otherVars, upperBound[field] + 1 - length,
                            upperBound[field] + 1);
                    // reverse from for its initialize from last to first
                    for (int j = 0; j < length / 2; j++) {
                        int t = from[j];
                        from[j] = from[length - j - 1];
                        from[length - j - 1] = t;
                    }
                    int[] to = Arrays.copyOfRange(vars, 0, length);

                    jdd.bdd.Permutation perm = bdd.createPermutation(from, to);
                    int idx = 0;
                    try {
                        idx = bdd.ref(bdd.replace(vector.get(field), perm));
                    } catch (Exception e) {
                        // deal with var -1 but cannot reproduce at every time
                    }
                    bdd.deref(vector.get(field));

                    vector.set(field, idx);
                }
            }
        }

        return NDD.ref(new NDD(vectors));
    }

    private static void toBDDvectorDFS(Set<ArrayList<Integer>> vectors,
            HashMap<Integer, HashMap<Integer, Integer>> data, int root, ArrayList<Integer> vector) {
        // in case of root == 1 in the first iterator
        if (root == 1) {
            ArrayList<Integer> vectorRet = new ArrayList<>(vector);
            vectors.add(vectorRet);
            return;
        }
        HashMap<Integer, Integer> map = data.get(root);
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            int end = entry.getKey();

            int idx = entry.getValue();
            int fieldDiff = bdd_getField(end) - bdd_getField(root) - 1;

            // iterator end here
            if (end == 1) {
                ArrayList<Integer> vectorRet = new ArrayList<>(vector);
                vectorRet.add(idx);
                for (int i = 0; i < fieldDiff; i++) {
                    vectorRet.add(1);
                }
                vectors.add(vectorRet);
                continue;
            }

            vector.add(idx);

            for (int i = 0; i < fieldDiff; i++) {
                vector.add(1);
            }

            toBDDvectorDFS(vectors, data, end, vector);

            for (int i = 0; i <= fieldDiff; i++) {
                vector.remove(vector.size() - 1);
            }
        }
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

        // boundary points: field -> idx list
        // boundary tree: idx -> child idx list
        get_boundary_tree(a, boundary_tree, boundary_points);

        for (int curr_level = 0; curr_level < fieldNum - 1; curr_level++) {
            for (int root : boundary_points.get(curr_level)) {
                for (int end_point : boundary_tree.get(root)) {
                    int res = bdd.ref(construct_decomposed_bdd(root, end_point, root));
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
        if (curr == 0) {
            return curr;
        } else if (curr == 1) {
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

        int new_low = bdd.ref(construct_decomposed_bdd(root, end_point, bdd.getLow(curr)));
        int new_high = bdd.ref(construct_decomposed_bdd(root, end_point, bdd.getHigh(curr)));

        // int field = bdd_getField(curr);
        // int result = 0;
        // if (field == 0) {
        // result = bdd.mk(bdd.getVar(curr), new_low, new_high);
        // } else {
        // result = bdd.mk(bdd.getVar(curr) - upperBound[field - 1] - 1, new_low,
        // new_high);
        // }
        int result = bdd.mk(bdd.getVar(curr), new_low, new_high);
        bdd.deref(new_low);
        bdd.deref(new_high);
        return result;
    }

    public static int toZero(NDD n) {
        ref(n);
        int idx = bdd.ref(toBDD(n));
        int ret = bdd.toZero(idx);
        bdd.deref(idx);
        deref(n);
        return ret;

    }

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
            // bdd with k and only k failures
            int pred = bdd.ref(encodeBDD(bdd, vars, fieldSize - 1, 0, i));
            NDD next = encodeAtMostKFailureVarsSortedRec(bdd, vars, endField, currField + 1, k - i);
            int nextPred = 0;
            if (map.containsKey(next))
                nextPred = map.get(next);
            bdd.ref(pred);
            int t = bdd.ref(bdd.or(pred, nextPred));
            bdd.deref(pred);
            bdd.deref(nextPred);
            nextPred = t;
            map.put(next, nextPred);
        }
        return NDD.addAtField(currField, map);
        // return NDD.table.mk(currField, map);
    }

    // replacement of NDD.table.mk(currField, map)
    public static NDD addAtField(int field, HashMap<NDD, Integer> map) {
        NDD ret = new NDD();
        for (Map.Entry<NDD, Integer> entry : map.entrySet()) {
            NDD n = entry.getKey();
            int b = entry.getValue();
            for (ArrayList<Integer> vector : n.vectors) {
                ArrayList<Integer> temp = new ArrayList<>(vector);
                temp.set(field, b);
                ret.vectors.add(temp);
            }
        }
        return ret;
    }

    private static int encodeBDD(BDD bdd, int[] vars, int endVar, int currVar, int k) {
        // cache? link num, k -> bdd
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
        // return bdd.mk(currVar, low, high);
        return bdd.mk(bdd.getVar(vars[endVar - currVar]), low, high);
    }

    public static boolean satCountTest = false;

    public static double satCount(NDD curr) {
        int idx = bdd.ref(toBDD(curr));
        double ret = bdd.satCount(idx);
        if (reuse)
            ret = ret / Math.pow(2.0, maxVarsLength);
        bdd.deref(idx);
        return ret;
        // return satCountRec(curr);
    }

    // can not deal with [[3, 1, 1], [1, 1, 2]] which has overlay
    // private static double satCountRec(NDD curr) {
    // double ret = 0.0;
    // for (ArrayList<Integer> vector : curr.vectors) {
    // double subRet = 1.0;
    // for (int i = fieldNum - 1; i >= 0; i--) {
    // subRet = subRet * (bdd.satCount(vector.get(i)) / div[i]);
    // }
    // ret += subRet;
    // }
    // return ret;
    // }

    public static void printDot(String path, NDD curr) {
        for (ArrayList<Integer> vector : curr.vectors) {
            for (int i = 0; i < fieldNum; i++) {
                int idx = vector.get(i);
                if (idx != 1 && idx != 0) {
                    bdd.printDot(path + "/" + idx, idx);
                }
            }
        }
    }

    public static void nodeCount(NDD node) {
        HashSet<Integer> BDDRootSet = new HashSet<Integer>();
        HashSet<Integer> BDDSet = new HashSet<Integer>();
        for (ArrayList<Integer> vector : node.vectors) {
            for (int i = 0; i < vector.size(); i++) {
                BDDRootSet.add(vector.get(i));
            }
        }
        for (int BDDRoot : BDDRootSet) {
            detectBDD(BDDRoot, BDDSet);
        }
        System.out.println("NDD node:" + node.vectors.size() + " BDD node:" +
                BDDSet.size());
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

    // public static void showStatestic() {
    // System.out.println("And:" + ANDTime / 1000000000.0);
    // System.out.println("Or:" + ORTime / 1000000000.0);
    // System.out.println("Not:" + NOTTime / 1000000000.0);
    // System.out.println("Exist:" + ExistTime / 1000000000.0);
    // System.out.println("ToNDD:" + ToNDDTime / 1000000000.0);
    // }

    // public static void showMemoryUsage() {
    // System.out.println("NDD memory usage:" +
    // (ObjectSizeCalculator.getObjectSize(bdd) / 8 / 1024 / 1024) + "MB");
    // }
}