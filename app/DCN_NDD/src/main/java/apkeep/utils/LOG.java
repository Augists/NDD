package apkeep.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LOG {
	
	static boolean IS_DEBUG_INFO = false;
	static boolean IS_DEBUG_CHECK = false;
	static boolean IS_DEBUG_WARNING = true;
	static boolean IS_DEBUG_LOG = true;
	public static boolean IS_DEBUG_FILE = true;
	public static boolean IS_REVERSE = false;

	static File log_file = Parameters.isExper? new File(Parameters.server_path+"log.txt"):Parameters.isDorm ? new File(Parameters.root_path_dormitory + "log.txt"):new File(Parameters.root_path_workplace + "log.txt");
//	static File log_file = new File(Parameters.root_path + "log.txt");

	public LOG() throws IOException {
		// TODO Auto-generated constructor stub

	}
	public static String getFilePath(){
		return log_file.getAbsolutePath();
	}
	public static void ERROR (String info)
	{
		System.err.println(info);
	}
	
	public static void INFO (String info)
	{
		if (IS_DEBUG_INFO) {
//			System.out.println(info);
			try {
				FileWriter output_writer = new FileWriter(log_file, true);
				output_writer.write(info + "\n");
				output_writer.close();
			}catch(IOException e) {
				System.out.println(e.toString());
			}
		}else {
			System.out.println(info);
		}
	}
	
	public static void LOGFILE (String info)
	{
		if (IS_DEBUG_FILE) {
//			System.out.println(info);
			try {
				FileWriter output_writer = new FileWriter(log_file, true);
				output_writer.write(info + "\n");
				output_writer.close();
			}catch(IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
	public static void CHECK (String info)
	{
		if (IS_DEBUG_CHECK) {
			System.out.println(info);
		}
	}
	
	public static void WARNING (String info)
	{
		if (IS_DEBUG_WARNING) {
			System.out.println(info);
		}
	}

	public static void log(String info) {
		// TODO Auto-generated method stub
		if(IS_DEBUG_LOG) {
			System.out.println(info);
		}
	}

	public static void write(FileWriter log_writer, String info) throws IOException {
		// TODO Auto-generated method stub
		if(!IS_REVERSE && IS_DEBUG_LOG) {
			log_writer.write(info+"\n");
		}
	}

	public static void ClearLogFile() {
		// TODO Auto-generated method stub
		if(log_file.exists()) {
			log_file.delete();
		}
		log_file = new File(Parameters.root_path + "log.txt");
	}

	public static void setLOGFile(String file_name) {
		// TODO Auto-generated method stub
		log_file = new File(Parameters.root_path + file_name);
	}
}
