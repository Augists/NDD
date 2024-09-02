package apkeep.utils;


public class IPConvertor {

  public static String numToIP(long ip) {
    StringBuilder sb = new StringBuilder();
    for (int i = 3; i >= 0; i--) {
      sb.append((ip >>> (i * 8)) & 0x000000ff);
      if (i != 0) {
        sb.append('.');
      }
    }
    //System.out.println(sb);
    return sb.toString();

  }

  public static long ipToNum(String ip) {
    long num = 0;
    String[] sections = ip.split("\\.");
    int i = 3;
    for (String str : sections) {
      num += (Long.parseLong(str) << (i * 8));
      i--;
    }
    //	System.out.println(num);
    return num;
  }

  public static void main(String[] args) {

    long sd = 92305930;
    String str = numToIP(sd);
    System.out.println(str);
    System.out.println(ipToNum(str));

  }
}

