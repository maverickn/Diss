package exp;

import cloudsim.power.models.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

public class ParseConfig {

    public static String inputFolder;
    public static String experimentName;

    public static int schedulingInterval;
    public static int simulationLimit;

    public static double learningRate;
    public static double discountFactor;
    public static double cofImportanceSla;
    public static double cofImportancePower;

    public static int vmTypesCount;
    public static int[] vmMips;
    public static int[] vmPes;
    public static int[] vmRam;
    public static int vmBw;
    public static int vmSize;

    public static int hostTypesCount;
    public static PowerModel[] hostTypes;
    public static int[] hostMips;
    public static int[] hostPes;
    public static int[] hostRam;
    public static int hostBw;
    public static int hostStorage;
    public static int hostsCount;

    public static void getData(String configName) throws IOException, ParseException {
        Object obj = new JSONParser().parse(new FileReader(configName));
        JSONObject jo = (JSONObject) obj;

        inputFolder = (String) jo.get("inputFolder");
        experimentName = (String) jo.get("experimentName");

        schedulingInterval = ((Long) jo.get("schedulingInterval")).intValue();
        simulationLimit = ((Long) jo.get("simulationLimit")).intValue();

        learningRate = (double) jo.get("learningRate");
        discountFactor = (double) jo.get("discountFactor");
        cofImportanceSla = (double) jo.get("cofImportanceSla");
        cofImportancePower = (double) jo.get("cofImportancePower");

        vmTypesCount = ((Long) jo.get("vmTypesCount")).intValue();
        vmMips = new int[vmTypesCount];
        JSONArray ja = (JSONArray) jo.get("vmMips");
        for (int i = 0; i < ja.size(); ++i) {
            vmMips[i] = ((Long) ja.get(i)).intValue();
        }
        vmPes = new int[vmTypesCount];
        ja = (JSONArray) jo.get("vmPes");
        for (int i = 0; i < ja.size(); ++i) {
            vmPes[i] = ((Long) ja.get(i)).intValue();
        }
        vmRam = new int[vmTypesCount];
        ja = (JSONArray) jo.get("vmRam");
        for (int i = 0; i < ja.size(); ++i) {
            vmRam[i] = ((Long) ja.get(i)).intValue();
        }
        vmBw = ((Long) jo.get("vmBw")).intValue();
        vmSize = ((Long) jo.get("vmSize")).intValue();

        hostTypesCount = ((Long) jo.get("hostTypesCount")).intValue();
        hostTypes = new PowerModel[hostTypesCount];
        ja = (JSONArray) jo.get("hostTypes");
        for (int i = 0; i < ja.size(); ++i) {
            String hostType = (String) ja.get(i);
            switch (hostType) {
                case "DellPowerEdgeR640":
                    hostTypes[i] = new DellPowerEdgeR640();
                    break;
                case "DellPowerEdgeR740":
                    hostTypes[i] = new DellPowerEdgeR740();
                    break;
                case "DellPowerEdgeR830":
                    hostTypes[i] = new DellPowerEdgeR830();
                    break;
                case "DellPowerEdgeR940":
                    hostTypes[i] = new DellPowerEdgeR940();
                    break;
                case "FujitsuSiemensComputersPrimergyRX300S4":
                    hostTypes[i] = new FujitsuSiemensComputersPrimergyRX300S4();
                    break;
                case "SuperMicroComputer6025BTR":
                    hostTypes[i] = new SuperMicroComputer6025BTR();
                    break;
                case "SuperMicroComputer6025BTRE5345":
                    hostTypes[i] = new SuperMicroComputer6025BTRE5345();
                    break;
            }

        }
        hostMips = new int[hostTypesCount];
        ja = (JSONArray) jo.get("hostMips");
        for (int i = 0; i < ja.size(); ++i) {
            hostMips[i] = ((Long) ja.get(i)).intValue();
        }
        hostPes = new int[hostTypesCount];
        ja = (JSONArray) jo.get("hostPes");
        for (int i = 0; i < ja.size(); ++i) {
            hostPes[i] = ((Long) ja.get(i)).intValue();
        }
        hostRam = new int[hostTypesCount];
        ja = (JSONArray) jo.get("hostRam");
        for (int i = 0; i < ja.size(); ++i) {
            hostRam[i] = ((Long) ja.get(i)).intValue();
        }
        hostBw = ((Long) jo.get("hostBw")).intValue();
        hostStorage = ((Long) jo.get("hostStorage")).intValue();
        hostsCount = ((Long) jo.get("hostsCount")).intValue();
    }

}
