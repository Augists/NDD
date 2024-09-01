package oldndd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FieldElement {
    public static NDDQueens q;
    public boolean is_end;
    public boolean t_f;
    public int field;
    public HashMap<FieldElement, Integer> port_pred;
    public static FieldElement NDDTrue = new FieldElement(true);
    public static FieldElement NDDFalse = new FieldElement(false);
    public static FieldManager table = new FieldManager();

    public FieldElement(Integer field) {
        this.field = field;
        port_pred = new HashMap<FieldElement, Integer>();
        is_end = false;
    }

    // public static int mkCount = 0;
    public FieldElement(int field, HashMap<FieldElement, Integer> port_pred) {
        // mkCount++;
        this.field = field;
        this.port_pred = port_pred;
        is_end = false;
    }

    public FieldElement(boolean t_f) {
        is_end = true;
        this.t_f = t_f;
    }

    public boolean equals(FieldElement obj) {
        if (this.is_end != obj.is_end) {
            return false;
        } else {
            if (this.is_end) {
                return this.t_f == obj.t_f;
            } else {
                return (this.field == obj.field) && (this.port_pred.equals(obj.port_pred));
            }
        }
    }

    public boolean is_True() {
        return is_end && t_f;
    }

    public boolean is_False() {
        return is_end && (!t_f);
    }

    public static FieldElement And(FieldElement a, FieldElement b) {
        if (a.is_False() || b.is_False())
            return NDDFalse;
        if (a.is_True())
            return b;
        if (b.is_True())
            return a;
        if (a == b || a.equals(b))
            return a;
        if (a.field == b.field) {
            HashMap<FieldElement, Integer> tempSet = new HashMap<FieldElement, Integer>();
            for (FieldElement next_a : a.port_pred.keySet()) {
                for (FieldElement next_b : b.port_pred.keySet()) {
                    int intersect = q.bdd.ref(q.bdd.and(a.port_pred.get(next_a), b.port_pred.get(next_b)));
                    if (intersect == 0)
                        continue;
                    FieldElement subRet = And(next_a, next_b);
                    if (subRet.is_False())
                        continue;
                    int aps = 0;
                    if (tempSet.containsKey(subRet)) {
                        aps = tempSet.get(subRet);
                    }
                    tempSet.put(subRet, q.bdd.ref(q.bdd.or(aps, intersect)));
                }
            }
            if (tempSet.size() == 0)
                return NDDFalse;
            if (tempSet.size() == 1) {
                for (FieldElement key : tempSet.keySet()) {
                    if (tempSet.get(key) == 1) {
                        return key;
                    }
                }
            }
            return new FieldElement(a.field, tempSet);
        } else if (a.field < b.field) {
            HashMap<FieldElement, Integer> tempSet = new HashMap<FieldElement, Integer>();
            for (FieldElement next_a : a.port_pred.keySet()) {
                FieldElement subRet = And(next_a, b);
                if (subRet.is_False())
                    continue;
                int aps = 0;
                if (tempSet.containsKey(subRet)) {
                    aps = tempSet.get(subRet);
                }
                tempSet.put(subRet, q.bdd.ref(q.bdd.or(aps, a.port_pred.get(next_a))));
            }
            if (tempSet.size() == 0)
                return NDDFalse;
            if (tempSet.size() == 1) {
                for (FieldElement key : tempSet.keySet()) {
                    if (tempSet.get(key) == 1) {
                        return key;
                    }
                }
            }
            return new FieldElement(a.field, tempSet);
        } else {
            HashMap<FieldElement, Integer> tempSet = new HashMap<FieldElement, Integer>();
            for (FieldElement next_b : b.port_pred.keySet()) {
                FieldElement subRet = And(next_b, a);
                if (subRet.is_False())
                    continue;
                int aps = 0;
                if (tempSet.containsKey(subRet)) {
                    aps = tempSet.get(subRet);
                }
                tempSet.put(subRet, q.bdd.ref(q.bdd.or(aps, b.port_pred.get(next_b))));
            }
            if (tempSet.size() == 0)
                return NDDFalse;
            if (tempSet.size() == 1) {
                for (FieldElement key : tempSet.keySet()) {
                    if (tempSet.get(key) == 1) {
                        return key;
                    }
                }
            }
            return new FieldElement(b.field, tempSet);
        }
    }

    public static FieldElement toNDD(int bddNode) {
        if (bddNode == 0) {
            return FieldElement.NDDFalse;
        } else if (bddNode == 1) {
            return FieldElement.NDDTrue;
        } else {
            HashMap<Integer, HashMap<Integer, Integer>> decomposed_bdd = decompose(bddNode);
            return constructNDD(decomposed_bdd);
        }
    }

    public static FieldElement constructNDD(HashMap<Integer, HashMap<Integer, Integer>> decomposed_bdd) {
        FieldElement ret;
        HashSet<Integer> processed = new HashSet<>();
        HashMap<Integer, FieldElement> map = new HashMap<>();
        processed.add(1);
        map.put(1, FieldElement.NDDTrue);
        while (decomposed_bdd.size() != 0) {
            for (int src : decomposed_bdd.keySet()) {
                if (processed.containsAll(decomposed_bdd.get(src).keySet())) {
                    HashMap<FieldElement, Integer> node = new HashMap<>();
                    for (int dst : decomposed_bdd.get(src).keySet()) {
                        node.put(map.get(dst), decomposed_bdd.get(src).get(dst));
                    }
                    if (decomposed_bdd.size() == 1) {
                        return new FieldElement(bdd_getField(src), node);
                    }
                    processed.add(src);
                    map.put(src, new FieldElement(bdd_getField(src), node));
                    decomposed_bdd.remove(src);
                    break;
                }
            }
        }
        System.out.println("error");
        return FieldElement.NDDFalse;
    }

    public static HashMap<Integer, HashMap<Integer, Integer>> decompose(int a) {
        HashMap<Integer, HashSet<Integer>> boundary_tree = new HashMap<Integer, HashSet<Integer>>();
        ArrayList<HashSet<Integer>> boundary_points = new ArrayList<HashSet<Integer>>();
        HashMap<Integer, HashMap<Integer, Integer>> decomposed_bdd = new HashMap<Integer, HashMap<Integer, Integer>>();

        get_boundary_tree(a, boundary_tree, boundary_points);

        for (int curr_level = 0; curr_level < NDDQueens.field_num - 1; curr_level++) {
            for (int root : boundary_points.get(curr_level)) {
                for (int end_point : boundary_tree.get(root)) {
                    int res = construct_decomposed_bdd(root, end_point, root);
                    q.bdd.ref(res);// ref deref!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    if (!decomposed_bdd.containsKey(root)) {
                        decomposed_bdd.put(root, new HashMap<Integer, Integer>());
                    }
                    decomposed_bdd.get(root).put(end_point, res);
                }
            }
        }

        for (int abdd : boundary_points.get(NDDQueens.field_num - 1)) {
            if (!decomposed_bdd.containsKey(abdd)) {
                decomposed_bdd.put(abdd, new HashMap<Integer, Integer>());
            }
            decomposed_bdd.get(abdd).put(1, abdd);
        }

        return decomposed_bdd;
    }

    public static void get_boundary_tree(int a, HashMap<Integer, HashSet<Integer>> boundary_tree,
            ArrayList<HashSet<Integer>> boundary_points) {
        int start_level;
        start_level = bdd_getField(a);
        for (int curr = 0; curr <= NDDQueens.field_num; curr++) {
            boundary_points.add(new HashSet<Integer>());
        }
        boundary_points.get(start_level).add(a);
        if (start_level == NDDQueens.field_num - 1) {
            boundary_tree.put(a, new HashSet<Integer>());
            boundary_tree.get(a).add(1);
            return;
        }

        for (int curr_level = start_level; curr_level <= NDDQueens.field_num; curr_level++) {
            for (int abdd : boundary_points.get(curr_level)) {
                detect_boundary_point(abdd, abdd, boundary_tree, boundary_points);
            }
        }
    }

    public static int bdd_getField(int a) {
        int va = q.bdd.getVar(a);
        if (a == 1)
            return NDDQueens.field_num + 1;
        if (a == 0)
            return NDDQueens.field_num + 2;
        for (int curr = 0; curr < NDDQueens.field_num; curr++) {
            if (va < NDDQueens.bound[curr]) {
                return curr;
            }
        }
        System.out.println("error1");
        return 0;
    }

    public static void detect_boundary_point(int root, int curr, HashMap<Integer, HashSet<Integer>> boundary_tree,
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
        detect_boundary_point(root, q.bdd.getLow(curr), boundary_tree, boundary_points);
        detect_boundary_point(root, q.bdd.getHigh(curr), boundary_tree, boundary_points);
    }

    public static int construct_decomposed_bdd(int root, int end_point, int curr) {
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

        int low = q.bdd.getLow(curr);
        int high = q.bdd.getHigh(curr);

        int new_low = construct_decomposed_bdd(root, end_point, low);
        q.bdd.ref(new_low);
        int new_high = construct_decomposed_bdd(root, end_point, high);
        q.bdd.ref(new_high);

        int result = q.bdd.mk(q.bdd.getVar(curr), new_low, new_high);
        return result;
    }
    
    public static double satCount(FieldElement curr)
    {
        if(curr.is_False())
        {
            return 0;
        }
        else if(curr.is_True())
        {
            return 1;
        }
        else
        {
            double sum=0;
            for(FieldElement next : curr.port_pred.keySet())
            {
                sum = sum + (satCount(next) * q.bdd.satCount(curr.port_pred.get(next)) / q.div[curr.field]);
            }
            return sum;
        }
    }
}