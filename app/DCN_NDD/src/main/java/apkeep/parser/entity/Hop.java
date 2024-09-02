package apkeep.parser.entity;

import apkeep.parser.entity.FWRule;
import apkeep.utils.IPConvertor;
import org.json.JSONObject;

public class Hop {
  private String node;
  private String inPort;
  //  public String aclIn;
  private FWRule fwRule;
  private String outPort;
//  public String aclOut;

  public Hop(String node, String inPort, String fwRule, String outPort) {
    this.node = node;
    this.inPort = inPort;
    this.fwRule = genFwRulle(fwRule);
    this.outPort = outPort;
  }

  private FWRule genFwRulle(String fwRule) {
    String[] fwElements = fwRule.split("\\s+");
    String prefix = String.format("%s/%s", IPConvertor.numToIP(Long.parseLong(fwElements[1])), fwElements[2]);
    String nhit = fwElements[3];
    return new FWRule(prefix, null, nhit);
  }

  public String getNode() {
    return node;
  }

  public String getInPort() {
    return inPort;
  }

  public FWRule getFwRule() {
    return fwRule;
  }

  public String getOutPort() {
    return outPort;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public void setInPort(String inPort) {
    this.inPort = inPort;
  }

  public void setFwRule(FWRule fwRule) {
    this.fwRule = fwRule;
  }

  public void setOutPort(String outPort) {
    this.outPort = outPort;
  }

  @Override
  public String toString() {
    return "Hop{" +
            "node='" + node + '\'' +
            ", inPort='" + inPort + '\'' +
            ", fwRule=" + fwRule +
            ", outPort='" + outPort + '\'' +
            '}';
  }

  public static void main(String[] args) {
    Hop hop = new Hop("A","B", "A 232456765 30 ge2/0 30","C");
    JSONObject jsonObject = new JSONObject(hop);
    System.out.println(jsonObject.toString());
  }
}
