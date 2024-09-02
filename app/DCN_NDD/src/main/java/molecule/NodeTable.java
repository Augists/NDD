package molecule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import NDD.NDD;

import jdd.bdd.BDD;
import molecule.Molecule;

public class NodeTable {
    public BDD bdd;
    public HashMap<HashMap<Molecule, HashSet<Integer>>, Molecule> molecules;
    public HashMap<Molecule, Integer> refCount;
    public HashSet<Integer> [] APs;

    public NodeTable() {
        molecules = new HashMap<HashMap<Molecule, HashSet<Integer>>, Molecule>();
        refCount = new HashMap<Molecule, Integer>();
        APs = new HashSet [Molecule.fieldNum];
    }

    public void clear()
    {
        molecules = new HashMap<HashMap<Molecule, HashSet<Integer>>, Molecule>();
        refCount = new HashMap<Molecule, Integer>();
        APs = new HashSet [Molecule.fieldNum];
    }

    public HashSet<Integer> getAP(int field)
    {
        return APs[field];
    }
    
    public int AP_nodes()
    {
        HashSet<Integer> nodes = new HashSet<>();
        for(int field=0;field<Molecule.fieldNum;field++)
        {
            for(int ap : APs[field])
            {
                collect_nodes(ap, nodes);
            }
        }
        return nodes.size();
    }

    public void collect_nodes(int curr, HashSet<Integer> nodes)
    {
        nodes.add(curr);
        if(curr == 0 || curr == 1)
        {
            return;
        }
        collect_nodes(bdd.getLow(curr), nodes);
        collect_nodes(bdd.getHigh(curr), nodes);
    }

    public Molecule mk(int field, HashMap<Molecule, HashSet<Integer>> port_aps)
    {
        // return new Molecule(field, port_aps);
        if (port_aps.size() == 0)
        {
            return Molecule.MFalse;
        }
        if (port_aps.size() == 1) // redundant node
        {
            Iterator<Map.Entry<Molecule, HashSet<Integer>>> iterator = port_aps.entrySet().iterator();
            Map.Entry<Molecule, HashSet<Integer>> entry = iterator.next();
            if(entry.getValue().size() == APs[field].size())return entry.getKey();
        }
        Molecule node = molecules.get(port_aps);
        if (node == null) // create new node
        {
            Molecule ret = new Molecule(field, port_aps);
            molecules.put(port_aps, ret);
            refCount.put(ret, 0);
            Iterator<Map.Entry<Molecule, HashSet<Integer>>> iterator = port_aps.entrySet().iterator();
            while(iterator.hasNext())
            {
                Map.Entry<Molecule, HashSet<Integer>> entry = iterator.next();
                Molecule next = entry.getKey();
                if (next == Molecule.MFalse || next == Molecule.MTrue) {
                    continue;
                }
                refCount.put(next, refCount.get(next)+1);
            }
            return ret;
        } else // find
        {
            Iterator<Map.Entry<Molecule, HashSet<Integer>>> iterator = port_aps.entrySet().iterator();
            while(iterator.hasNext())
            {
                Map.Entry<Molecule, HashSet<Integer>> entry = iterator.next();
            }
            return node;
        }
    }

    public Molecule ref(Molecule a) {
        if(a == Molecule.MFalse || a == Molecule.MTrue)return a;
        refCount.put(a, refCount.get(a)+1);
        return a;
    }

    public void deref(Molecule a) {
        if(Molecule.useCache)return;
        if(a == Molecule.MFalse || a == Molecule.MTrue)return;
        int newRef = refCount.get(a)-1;
        if (newRef > 0) {
            refCount.put(a, newRef);
        } else if (newRef == 0) {
            delete(a);
        } else {
            System.out.println("error: ret count less than 0");
        }
    }

    public void testAndDelete(Molecule a)
    {
        if(Molecule.useCache)return;
        if(a == Molecule.MFalse || a == Molecule.MTrue)return;
        if(refCount.containsKey(a) && refCount.get(a) == 0)
        {
            delete(a);
        }
    }

    private void delete(Molecule a) {
        Iterator<Map.Entry<Molecule, HashSet<Integer>>> iterator = a.edges.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<Molecule, HashSet<Integer>> entry = iterator.next();
            Molecule next = entry.getKey();
            if (next == Molecule.MFalse || next == Molecule.MTrue) {
                continue;
            }
            deref(next);
        }
        molecules.remove(a.edges);
        refCount.remove(a);
    }

    public HashMap<NDD, Molecule> Atomization(HashSet<NDD> NDDPreds, HashMap<NDD, HashSet<Integer> []> ndd_aps)
    {
        //collect preds
        HashSet<Integer> [] FieldPreds = new HashSet [Molecule.fieldNum];
        for(int i=0;i<Molecule.fieldNum;i++)
        {
            FieldPreds[i] = new HashSet<>();
        }
        for(NDD NDDPred : NDDPreds)
        {
            CollectFieldPreds(NDDPred, FieldPreds);
        }
        
        //update AP
        for(int field=0;field<Molecule.fieldNum;field++)
        {
            HashSet<Integer> AP = new HashSet<>();
            AP.add(1);
            HashSet<Integer> new_ap = new HashSet<>();
            for (Integer abdd : FieldPreds[field])
            {
                new_ap.clear();
                for (Integer one_ap : AP)
                {
                    Integer intersect = bdd.and(abdd, one_ap);
                    if (intersect != 0)
                    {
                        if(!new_ap.contains(intersect))
                        {
                            bdd.ref(intersect);
                            new_ap.add(intersect);
                        }
                    }
                    int n_abdd = bdd.ref(bdd.not(abdd));
                    intersect = bdd.ref(bdd.and(n_abdd, one_ap));
                    bdd.deref(n_abdd);
                    if (intersect != 0)
                    {
                        if(!new_ap.contains(intersect))
                        {
                            bdd.ref(intersect);
                            new_ap.add(intersect);
                        }
                    }
                    bdd.deref(one_ap);
                }
                AP = new HashSet<Integer>(new_ap);
            }
            APs[field] = AP;
        }

        //atomize bdd pred
        HashMap<Integer, HashSet<Integer>> bdd_aps = new HashMap<>();
        for(int field=0;field<Molecule.fieldNum;field++)
        {
            for(int bddPred : FieldPreds[field])
            {
                HashSet<Integer> set = new HashSet<>();
                for(int ap : APs[field])
                {
                    if(bdd.and(bddPred, ap) != 0)
                    {
                        set.add(ap);
                    }
                }
                bdd_aps.put(bddPred, set);
            }
        }

        //atomize ndd pred
        HashMap<NDD, Molecule> ndd_mol = new HashMap<>();
        for(NDD nddPred : NDDPreds)
        {
            HashSet<Integer> [] apSet = new HashSet [Molecule.fieldNum];
            for(int field=0;field<Molecule.fieldNum;field++)
            {
                apSet[field] = new HashSet<>();
            }
            Molecule molecule = AtomizeNDD(nddPred, bdd_aps);
            ndd_mol.put(nddPred, molecule);
            CollectAtoms(molecule, apSet);
            ndd_aps.put(nddPred, apSet);
        }
        return ndd_mol;
    }

    private void CollectFieldPreds(NDD curr, HashSet<Integer> [] FieldPreds)
    {
        if(curr.is_False() || curr.is_True())
            return;
        for(Map.Entry<NDD, Integer> entry : curr.edges.entrySet())
        {
            FieldPreds[curr.field].add(entry.getValue());
            CollectFieldPreds(entry.getKey(), FieldPreds);
        }
    }
    
    private Molecule AtomizeNDD(NDD curr, HashMap<Integer, HashSet<Integer>> bdd_aps)
    {
        if(curr.is_False())
            return Molecule.MFalse;
        if(curr.is_True())
            return Molecule.MTrue;
        HashMap<Molecule, HashSet<Integer>> tempSet = new HashMap<>();
        for(Map.Entry<NDD, Integer> entry : curr.edges.entrySet())
        {
            Molecule subRet = AtomizeNDD(entry.getKey(), bdd_aps);
            tempSet.put(subRet, new HashSet<>(bdd_aps.get(entry.getValue())));
        }
        return mk(curr.field, tempSet);
    }

    private void CollectAtoms(Molecule curr, HashSet<Integer> [] apSet)
    {
        if(curr.is_False() || curr.is_True())
            return;
        for(Map.Entry<Molecule, HashSet<Integer>> entry : curr.edges.entrySet())
        {
            apSet[curr.field].addAll(entry.getValue());
            CollectAtoms(entry.getKey(), apSet);
        }
    }

    public static void GetToRemove(Molecule curr, HashSet<Integer> [] toRemove)
    {
        if(curr.is_False() || curr.is_True())
        {
            return;
        }
        HashSet<Integer> set = toRemove[curr.field];
        for(Map.Entry<Molecule, HashSet<Integer>> entry : curr.edges.entrySet())
        {
            set.removeAll(entry.getValue());
            GetToRemove(entry.getKey(), toRemove);
        }
    }

    public static void GetToRemove(Molecule curr, HashSet<Integer> toRemove)
    {
        if(curr.is_False() || curr.is_True())
        {
            return;
        }
        for(Map.Entry<Molecule, HashSet<Integer>> entry : curr.edges.entrySet())
        {
            toRemove.removeAll(entry.getValue());
        }
    }
}