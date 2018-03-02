import org.cloudbus.cloudsim.Log;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

public class ParseConfig {

    public static boolean enableOutput;
    public static boolean outputLog;
    public static String outputFolder;
    public static String inputFolder;
    public static String experimentName;
    public static boolean outputCsv;

    public static double schedulingInterval;
    public static double simulationLimit;

    public static int vmTypes;
    public static int[] vmMips;
    public static int[] vmPes;
    public static int[] vmRam;
    public static int vmBw;
    public static int vmSize;

    public static int hostTypes;
    public static int[] hostMpis;
    public static int[] hostPes;
    public static int[] hostRam;
    public static int hostBw;
    public static int hostStorage;

    public static int hostsCount;

    public static void getData() {
        try {
            Object obj = new JSONParser().parse(new FileReader("dc_config.json"));
            JSONObject jo = (JSONObject) obj;

            enableOutput = (boolean) jo.get("enableOutput");
            outputLog = (boolean) jo.get("outputLog");
            outputFolder = (String) jo.get("outputFolder");
            inputFolder = (String) jo.get("inputFolder");
            experimentName = (String) jo.get("experimentName");
            outputCsv = (boolean) jo.get("outputCsv");

            schedulingInterval = ((Long) jo.get("schedulingInterval")).intValue();
            simulationLimit = ((Long) jo.get("simulationLimit")).intValue();

            vmTypes = ((Long) jo.get("vmTypes")).intValue();
            vmMips = new int[vmTypes];
            JSONArray ja = (JSONArray) jo.get("vmMips");
            for (int i = 0; i < ja.size(); ++i) {
                vmMips[i] = ((Long) ja.get(i)).intValue();
            }
            vmPes = new int[vmTypes];
            ja = (JSONArray) jo.get("vmPes");
            for (int i = 0; i < ja.size(); ++i) {
                vmPes[i] = ((Long) ja.get(i)).intValue();
            }
            vmRam = new int[vmTypes];
            ja = (JSONArray) jo.get("vmRam");
            for (int i = 0; i < ja.size(); ++i) {
                vmRam[i] = ((Long) ja.get(i)).intValue();
            }
            vmBw = ((Long) jo.get("vmBw")).intValue();
            vmSize = ((Long) jo.get("vmSize")).intValue();

            hostTypes = ((Long) jo.get("hostTypes")).intValue();
            hostMpis = new int[hostTypes];
            ja = (JSONArray) jo.get("hostMpis");
            for (int i = 0; i < ja.size(); ++i) {
                hostMpis[i] = ((Long) ja.get(i)).intValue();
            }
            hostPes = new int[hostTypes];
            ja = (JSONArray) jo.get("hostPes");
            for (int i = 0; i < ja.size(); ++i) {
                hostPes[i] = ((Long) ja.get(i)).intValue();
            }
            hostRam = new int[hostTypes];
            ja = (JSONArray) jo.get("hostRam");
            for (int i = 0; i < ja.size(); ++i) {
                hostRam[i] = ((Long) ja.get(i)).intValue();
            }
            hostBw = ((Long) jo.get("hostBw")).intValue();
            hostStorage = ((Long) jo.get("hostStorage")).intValue();
            hostsCount = ((Long) jo.get("hostsCount")).intValue();

        } catch (ParseException | IOException e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
    }

}
