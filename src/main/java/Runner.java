import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.*;
import policy.HostPowerModeSelectionPolicyAgent;
import policy.VmAllocationPolicyLocalRegression;
import policy.VmAllocationPolicyNonPowerAware;

import java.io.*;
import java.io.File;
import java.util.Calendar;
import java.util.List;

public class Runner {

    private DatacenterBroker broker;

    private List<Cloudlet> cloudletList;

    private List<Vm> vmList;

    private List<PowerHost> hostList;

    public Runner(String inputFolder, String outputFolder, String experimentName, String policyName) throws Exception {
        initLogOutput(outputFolder, experimentName, policyName);
        init(inputFolder + "/" + experimentName);
        VmAllocationPolicy vap = new HostPowerModeSelectionPolicyAgent(ParseConfig.learningRate, ParseConfig.discountFactor, ParseConfig.cofImportanceSla, ParseConfig.cofImportancePower,
                new PowerVmSelectionPolicyMinimumMigrationTime(), hostList);

        /*VmAllocationPolicy vap = new VmAllocationPolicyLocalRegression(hostList,
                new PowerVmSelectionPolicyMinimumMigrationTime(),
                1.2,
                ParseConfig.schedulingInterval,
                new PowerVmAllocationPolicyMigrationStaticThreshold(
                        hostList,
                        new PowerVmSelectionPolicyMinimumMigrationTime(),
                        0.7));*/
        start(experimentName, outputFolder, vap, policyName);
    }

    public static void initLogOutput(String outputFolder, String experimentName, String policyName) throws IOException {
        Log.enable();
        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        File folder2 = new File(outputFolder + "/log");
        if (!folder2.exists()) {
            folder2.mkdir();
        }
        File file = new File(outputFolder + "/log/" + policyName + "_" + experimentName + ".log");
        file.createNewFile();
        Log.setOutput(new FileOutputStream(file));
    }

    public static void printResults(String outputFolder, String experimentName, String policyName) throws IOException {
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
        List<Double> migrationCountList = HostPowerModeSelectionPolicyAgent.getMigrationCountList();

        File file = new File(outputFolder + "/metrics/" + policyName + "_" + experimentName + "_metric.csv");
        file.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (int i = 0; i < timeList.size(); i++) {
            writer.write(String.format("%.6f;\t%.6f;\t%.6f;\t%.6f;\t\n", timeList.get(i), slaViolationTimeList.get(i), powerConsumptionList.get(i), migrationCountList.get(i)));
        }
        writer.close();
    }

    private void init(String experimentFolder) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);
        broker = SetupEntities.createBroker();
        int brokerId = broker.getId();
        cloudletList = SetupEntities.createCloudletList(brokerId, experimentFolder);
        vmList = SetupEntities.createVmList(brokerId, cloudletList.size());
        hostList = SetupEntities.createHostList(ParseConfig.hostsCount);
    }

    private void start(String experimentName, String outputFolder, VmAllocationPolicy vmAllocationPolicy, String policyName) throws Exception {
        Log.printLine("Starting " + experimentName);
        PowerDatacenter datacenter = (PowerDatacenter) SetupEntities.createDatacenter("Datacenter",
                PowerDatacenter.class, hostList, vmAllocationPolicy);
        datacenter.setDisableMigrations(false);
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        CloudSim.terminateSimulation(ParseConfig.simulationLimit);
        CloudSim.startSimulation();
        List<Cloudlet> newList = broker.getCloudletReceivedList();
        Log.printLine("Received " + newList.size() + " cloudlets");
        CloudSim.stopSimulation();
        printResults(outputFolder, experimentName, policyName);
        Log.printLine("Finished " + experimentName);
    }

    public static void nonPowerAwareModelling(String inputFolder, String outputFolder, String experimentName, String policyName) throws Exception {
        Runner.initLogOutput(outputFolder, experimentName, policyName);
        Log.printLine("Starting " + experimentName);

        CloudSim.init(1, Calendar.getInstance(), false);

        DatacenterBroker broker = SetupEntities.createBroker();
        int brokerId = broker.getId();

        List<Cloudlet> cloudletList = SetupEntities.createCloudletList(brokerId, inputFolder + "/" + experimentName);
        List<Vm> vmList = SetupEntities.createVmList(brokerId, cloudletList.size());
        List<PowerHost> hostList = SetupEntities.createHostList(ParseConfig.hostsCount);

        PowerDatacenterNonPowerAware datacenter = (PowerDatacenterNonPowerAware) SetupEntities.createDatacenter(
                "Datacenter",
                PowerDatacenterNonPowerAware.class,
                hostList,
                new VmAllocationPolicyNonPowerAware(hostList));

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        CloudSim.terminateSimulation(ParseConfig.simulationLimit);
        CloudSim.startSimulation();

        List<Cloudlet> newList = broker.getCloudletReceivedList();
        Log.printLine("Received " + newList.size() + " cloudlets");

        CloudSim.stopSimulation();
        printResults(outputFolder, experimentName, policyName);

        Log.printLine("Finished " + experimentName);
    }

}
