import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.*;
import policy.HostPowerModeSelectionPolicyAgent;

import java.io.*;
import java.io.File;
import java.util.Calendar;
import java.util.List;

public class Runner {

    protected static DatacenterBroker broker;

    protected static List<Cloudlet> cloudletList;

    protected static List<Vm> vmList;

    protected static List<PowerHost> hostList;

    public Runner(String inputFolder, String outputFolder, String experimentName) throws Exception {
        initLogOutput(outputFolder, experimentName);
        init(inputFolder + "/" + experimentName);
        VmAllocationPolicy vap = new HostPowerModeSelectionPolicyAgent(ParseConfig.learningRate, ParseConfig.discountFactor, ParseConfig.cofImportanceSla, ParseConfig.cofImportancePower,
                new PowerVmSelectionPolicyMinimumMigrationTime(), hostList);
        start(experimentName, outputFolder, vap);
    }

    public static void initLogOutput(String outputFolder, String experimentName) throws IOException {
        Log.enable();
        System.out.println("Output to log file. Please, wait...");
        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        File folder2 = new File(outputFolder + "/log");
        if (!folder2.exists()) {
            folder2.mkdir();
        }
        File file = new File(outputFolder + "/log/" + experimentName + ".log");
        file.createNewFile();
        Log.setOutput(new FileOutputStream(file));
    }

    public static void printResults(String outputFolder, String experimentName) throws IOException {
        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        File folder1 = new File(outputFolder + "/metrics");
        if (!folder1.exists()) {
            folder1.mkdir();
        }

        List<Double> timeList = HostPowerModeSelectionPolicyAgent.getTimeList();
        List<Double> slaViolationTimeList = HostPowerModeSelectionPolicyAgent.getSlaViolationTimeList();
        List<Double> powerConsumptionList = HostPowerModeSelectionPolicyAgent.getPowerConsumptionList();

        File file = new File(outputFolder + "/metrics/" + experimentName + "_metric.csv");
        file.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (int i = 0; i < slaViolationTimeList.size(); i++) {
            writer.write(String.format("%.6f;\t%.6f;\t%.6f;\t\n", timeList.get(i), slaViolationTimeList.get(i), powerConsumptionList.get(i)));
        }
        writer.close();
    }

    protected void init(String experimentFolder) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);
        broker = SetupEntities.createBroker();
        int brokerId = broker.getId();
        cloudletList = SetupEntities.createCloudletList(brokerId, experimentFolder);
        vmList = SetupEntities.createVmList(brokerId, cloudletList.size());
        hostList = SetupEntities.createHostList(ParseConfig.hostsCount);
    }

    protected void start(String experimentName, String outputFolder, VmAllocationPolicy vmAllocationPolicy) throws Exception {
        Log.printLine("Starting " + experimentName);
        PowerDatacenter datacenter = (PowerDatacenter) SetupEntities.createDatacenter("Datacenter",
                PowerDatacenter.class, hostList, vmAllocationPolicy);
        datacenter.setDisableMigrations(false);
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        CloudSim.terminateSimulation(ParseConfig.simulationLimit);
        //double lastClock =
        CloudSim.startSimulation();
        List<Cloudlet> newList = broker.getCloudletReceivedList();
        Log.printLine("Received " + newList.size() + " cloudlets");
        CloudSim.stopSimulation();
        //SetupEntities.printResults(datacenter, vmList, lastClock, experimentName, outputFolder);
        printResults(outputFolder, experimentName);
        Log.printLine("Finished " + experimentName);
        System.out.println("Done!");
    }

}
