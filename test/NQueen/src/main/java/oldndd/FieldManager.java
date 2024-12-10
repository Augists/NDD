package oldndd;

import java.util.HashMap;
import java.util.HashSet;

import jdd.bdd.BDD;
import javafx.util.*;

public class FieldManager {
//     public int field;
    public static BDD bdd;
    public HashMap<HashMap<FieldElement, Integer>, Pair<FieldElement, Integer>> Fieldelements;
//     public NDDQueens q;

    public FieldManager()
    {
        Fieldelements = new HashMap<HashMap<FieldElement, Integer>, Pair<FieldElement, Integer>>();
    }

    public FieldElement FindOrCreate(int field, HashMap<FieldElement, Integer> port_pred)
    {
        if (port_pred.size() == 1) //merge
        {
            for(FieldElement key : port_pred.keySet())
            {
                if(port_pred.get(key) == 1)return key;
            }
        }
        if(!Fieldelements.containsKey(port_pred))
        {
            FieldElement ret = new FieldElement(field, port_pred);
            Fieldelements.put(port_pred, new Pair<FieldElement, Integer>(ret, 0));
            return ret;
        }
        else //find
        {
            return Fieldelements.get(port_pred).getKey();
        }
    }

//     public FieldManager(int field, NDDQueens q)
//     {
//         this.field = field;
//         this.q = q;
//         this.bdd = q.bdd;
//         Fieldelements = new HashMap<HashMap<FieldElement, Integer>, Pair<FieldElement, Integer>>();
//     }
//     public FieldElement FindOrCreate(HashMap<FieldElement, Integer> port_pred)
//     {
//         if (port_pred.size() == 1) //merge
//         {
//             for(FieldElement key : port_pred.keySet())
//             {
//                 if(port_pred.get(key) == 1)return key;
//             }
//         }
//         if(!Fieldelements.containsKey(port_pred))
//         {
//             FieldElement ret = new FieldElement(field, port_pred);
//             Fieldelements.put(port_pred, new Pair<FieldElement, Integer>(ret, 0));
//             for(FieldElement key : port_pred.keySet())
//             {
//                 if (key.is_end)
//                     continue;

//                 Pair<FieldElement, Integer> iter = q.fieldManagers[key.field].Fieldelements.get(key.port_pred);
//                 Pair<FieldElement, Integer> iter_new = new Pair<FieldElement, Integer>(iter.getKey(), iter.getValue()+1);
//                 q.fieldManagers[key.field].Fieldelements.put(key.port_pred, iter_new);
//             }
//             return ret;
//         }
//         else //find
//         {
//             return Fieldelements.get(port_pred).getKey();
//         }
//     }
//     public void increase_ref(FieldElement inc)
//     {
//         if(Fieldelements.containsKey(inc.port_pred))
//         {
//             Fieldelements.put(inc.port_pred, new Pair<FieldElement, Integer>(Fieldelements.get(inc.port_pred).getKey(), Fieldelements.get(inc.port_pred).getValue()+1));
//         }
//     }
//     public void decrease_ref(FieldElement dec)
//     {
//         if(Fieldelements.containsKey(dec.port_pred))
//         {
//             Fieldelements.put(dec.port_pred, new Pair<FieldElement, Integer>(Fieldelements.get(dec.port_pred).getKey(), Fieldelements.get(dec.port_pred).getValue()-1));
//         }
//         if(Fieldelements.get(dec.port_pred).getValue() == 0)
//         {
//             for(FieldElement key : dec.port_pred.keySet())
//             {
//                 if(key.is_end)
//                 {
//                     continue;
//                 }
//                 q.fieldManagers[key.field].decrease_ref(key);
//             }
//             Fieldelements.remove(dec.port_pred);
//         }
//     }
}