package apkeep.FDN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import NDD.NDD;
import common.PositionTuple;
import javafx.util.*;

public class TranverseNode {
    public PositionTuple source;
    public PositionTuple curr;
    public NDD APs;
    HashSet<String> visited;
    ArrayList<PositionTuple> path;
    public TranverseNode()
    {

    }
    public TranverseNode(PositionTuple source, NDD APs)
    {
        this.source = source;
        this.curr = source;
        this.APs = APs;
        visited = new HashSet<String>();
        // if(curr.getDeviceName().split("_").length == 1)
        // {
            visited.add(curr.getDeviceName());
        // }
        path = new ArrayList<>();
        path.add(curr);
    }
    public TranverseNode(PositionTuple source, PositionTuple curr, NDD APs, HashSet<String> visited, ArrayList<PositionTuple> path, PositionTuple next)
    {
        this.source = source;
        this.curr = curr;
        this.APs = APs;
        this.visited = new HashSet<String>(visited);
        // if(curr.getDeviceName().split("_").length == 1)
        // {
            this.visited.add(curr.getDeviceName());
        // }
        this.path = new ArrayList<>(path);
        this.path.add(next);
    }
}
