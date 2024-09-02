package apkeep.utils;

// import apkeep.checker.Property;

import java.security.PublicKey;
import java.util.HashSet;

public class Parameters {
	
	// public static int BDD_TABLE_SIZE = 200000000;
	public static int BDD_TABLE_SIZE = 20000000;
	// public static int BDD_TABLE_SIZE = 10000000;
	// public static int BDD_TABLE_SIZE = 1;
	public static boolean addvni = true;
	public static boolean addmcs = true;
	public static boolean addpbr = true;
	public static boolean testvni = false;
	public static int hoplen=26;

	public static boolean updated= false;
	public static boolean testepg = false;

	public static int GC_INTERVAL = 1000;

	public static int TOTAL_AP_THRESHOLD = 500;
	public static int LOW_MERGEABLE_AP_THRESHOLD = 10;
	public static int HIGH_MERGEABLE_AP_THRESHOLD = 50;
	public static double FAST_UPDATE_THRESHOLD = 0.25; // ms
	public static double SLOW_UPDATE_THRESHOLD = 100; // ms
	
//	public static HashSet<Property> PROPERTIES_TO_CHECK = new HashSet<Property>(){{add(Property.Reachability);add(Property.LOOP);add(Property.BLACKHOLE);}};
//	public static HashSet<Property> PROPERTIES_TO_CHECK = new HashSet<Property>(){{add(Property.LOOP);add(Property.BLACKHOLE);}};
//	public static HashSet<Property> PROPERTIES_TO_CHECK = new HashSet<Property>(){{add(Property.LOOP);}};
//	public static HashSet<Property> PROPERTIES_TO_CHECK = new HashSet<Property>(){{add(Property.BLACKHOLE);}};
	// public static HashSet<Property> PROPERTIES_TO_CHECK = new HashSet<Property>(){{add(Property.Reachability);}};
//	public static HashSet<Property> PROPERTIES_TO_CHECK = new HashSet<Property>(){{add(Property.LOOP);add(Property.Reachability);}};
//	public static HashSet<Property> PROPERTIES_TO_CHECK = new HashSet<Property>(){{}};
//	
//	public static String root_path = "/home/xliu/mnv/";
	public static String root_path = "D:\\Field Decision Network\\apkatra-main\\src\\dataset\\";
	public static boolean isTiny=true;
	public static String TinyPath="APKeep_6/";
	public static boolean isExper=false;
	public static boolean isAPBox=true;
	public static boolean isDorm=true;
	public static boolean isShow = false;
	public static boolean isTraver=true;
	public static boolean isIncre=true;
	public static boolean isComputeAP=true;
	public static boolean isIP_VNI=true;
	public static boolean use_rewrite_table=false;
	public static String root_path_workplace = "D:\\Field Decision Network\\apkatra-main\\src\\dataset\\";
	public static String root_path_dormitory = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/src/main/java/dataset/";
	public static String dataset_path = root_path + "dataset/";
	public static String result_path = root_path + "results/";
	public static String server_path = root_path + "dataset/";
	public static String log_path = root_path + "templog/";
//	public static String result_path = "/home/xliu/apkeep/results/";
	
	public static int WRITE_RESULT_INTERVAL = 1;
	public static int PRINT_RESULT_INTERVAL = 1000;
	public static int MAX_TIME=86400000;
	public static String network = "test";
	public static String mode = "normal";
	
	public Parameters() {
		// TODO Auto-generated constructor stub
	}

	public static void setNetwork(String network_name) {
		// TODO Auto-generated method stub
		network = network_name;
	}
}
