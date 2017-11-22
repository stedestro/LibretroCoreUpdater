import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.CRC32;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LibretroCore {

	public static class LibCore {
		public String name;
		public String crc32;
		public String date;
		public String size;
	}

	private static final String LIBRETRO_URL = "https://buildbot.libretro.com/nightly/windows/x86_64/latest/";
	private static final int CONNECTION_TIMEOUT = 5000;
	private static final int READ_TIMEOUT = 10000;
	private static final String SEP = System.getProperties().getProperty("file.separator");

	/**
	 * Get the crc32 of a file
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static long getCrc32(String filename) throws IOException {
		File f = new File(filename);
		FileInputStream fis = new FileInputStream(f);
		CRC32 crcMaker = new CRC32();
		byte[] buffer = new byte[65536];
		int bytesRead;
		while ((bytesRead = fis.read(buffer)) != -1) {
			crcMaker.update(buffer, 0, bytesRead);
		}
		fis.close();
		return crcMaker.getValue();
	}

	/**
	 * Download a core
	 * @param coreName
	 * @param destination
	 * @return
	 * @throws MalformedURLException
	 */
	public static boolean downloadCore(String coreName, String destination) throws MalformedURLException {		
		URL url = new URL(LIBRETRO_URL + coreName + ".zip");
		File dest = new File(destination);
		try {
			FileUtils.copyURLToFile(url, dest, CONNECTION_TIMEOUT, READ_TIMEOUT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
		return true;
	}

	/**
	 * Download list of cores available on server
	 * @return
	 * @throws MalformedURLException
	 */
	public static boolean downloadIndexExtended() throws MalformedURLException {
		URL url = new URL(LIBRETRO_URL + ".index-extended");
		File f = new File("./index-extended");
		try {
			FileUtils.copyURLToFile(url, f, CONNECTION_TIMEOUT, READ_TIMEOUT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
		return true;
	}

	/**
	 * List all local cores
	 * @param path
	 * @return
	 */
	public static ArrayList<String> listLocalCores(String path) {
		ArrayList<String> list = new ArrayList<>();
		File dir = new File(path);
		File[] fileList = dir.listFiles();

		for (File f : fileList) {
			if (f.getName().toLowerCase().endsWith(".dll") || f.getName().toLowerCase().endsWith(".so")) {
				list.add(f.getName());
			}
		}
		Collections.sort(list);
		return list;
	}
	
	/**
	 * Scan a single core
	 * @param file
	 * @param corename
	 * @return LibCore object
	 * @throws IOException
	 */
	public static LibCore scanCore(String file, String corename) throws IOException {
		LibCore core = new LibCore();
		core.name = corename;
		core.crc32 = String.format("%08x", getCrc32(file)).toLowerCase();
		File f = new File(file);
		core.size = Long.toString(f.length());		
		return core; 		
	}

	/**
	 * Scan all local cores
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static String scanLocalCores(String path) throws IOException { 
		ArrayList<String> localCores = listLocalCores(path);
		ArrayList<LibCore> coreList = new ArrayList<>();
		for (String core : localCores) {
			String corePath = path + SEP + core;
			LibCore c = scanCore(corePath, core);
			coreList.add(c);
		}
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting().serializeNulls();
		Gson gson = builder.create();

		return gson.toJson(coreList);
	}
	
	/**
	 * Load local cores information from json file
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<LibCore> loadLocalCores(String jsonFile) throws IOException{
		LibCore[] coreArray;
		
		String json = new String(Files.readAllBytes(Paths.get(jsonFile)));
		coreArray = new Gson().fromJson(json, LibCore[].class);

		return new ArrayList<>(Arrays.asList(coreArray));
	}
	
	/**
	 * Convert index-extended file to list of cores
	 * @param sorted
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<LibCore> loadServerCores(boolean sorted) throws IOException{
		List<String> lines = Files.readAllLines(Paths.get("./index-extended"));
		ArrayList<LibCore> coreList = new ArrayList<>();
		for(String core : lines) {
			String[] splitted = core.trim().split(" ");
			LibCore c = new LibCore();
			c.name = splitted[2].replace(".zip", "");
			c.crc32 = splitted[1];
			c.date = splitted[0];
			coreList.add(c);
		}
		if(sorted) {
			Collections.sort(coreList, new Comparator<LibCore>() {
				@Override
				public int compare(LibCore o1, LibCore o2) {
					return o1.name.compareTo(o2.name);
				}
			});
		}		
		return coreList;
	}
}
