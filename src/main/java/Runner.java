import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.*;
import policy.*;

import java.io.*;
import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Runner {

    private DatacenterBroker broker;

    private List<Cloudlet> cloudletList;

    private List<Vm> vmList;

    private List<PowerHost> hostList;

    public Runner(String inputFolder, String outputFolder, String experimentName, String policyName) throws Exception {
        initLogOutput(outputFolder, experimentName, policyName);
        VmAllocationPolicy vap;
        switch (policyName) {
            case "Qla":
                init(inputFolder + "/" + experimentName, true);
                PowerVmSelectionPolicy pvsp = new PowerVmSelectionPolicyMinimumMigrationTime();
                PowerVmAllocationPolicyMigrationAbstract fbvsp = new PowerVmAllocationPolicyMigrationStaticThreshold(hostList, pvsp, 0.7);
                VmAllocationPolicyLocalRegression  vaplr = new VmAllocationPolicyLocalRegression(hostList, pvsp, 1.2, ParseConfig.schedulingInterval, fbvsp);

                vap = new HostPowerModeSelectionPolicyAgent(ParseConfig.learningRate, ParseConfig.discountFactor, ParseConfig.cofImportanceSla, ParseConfig.cofImportancePower, pvsp, vaplr, hostList);
                policyName += " lr=" + ParseConfig.learningRate + " df=" + ParseConfig.discountFactor + " cs=" + ParseConfig.cofImportanceSla + " cp=" + ParseConfig.cofImportancePower;
                start(experimentName, outputFolder, vap, policyName);
                break;
            case "Npa":
                nonPowerAwareModelling(inputFolder, outputFolder, experimentName, policyName, true);
                break;
            case "Dvfs":
                init(inputFolder + "/" + experimentName, true);
                vap = new VmAllocationPolicyNonPowerAware(hostList);
                start(experimentName, outputFolder, vap, policyName);
                break;
            default:
                init(inputFolder + "/" + experimentName,false);
                vap = getVmAllocationPolicy(policyName.split(" ")[0], policyName.split(" ")[1]);
                start(experimentName, outputFolder, vap, policyName);
                break;
        }
    }

    private void initLogOutput(String outputFolder, String experimentName, String policyName) throws IOException {
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

    private void printResults(String outputFolder, String experimentName, String policyName) throws IOException {
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
            writer.write(String.format(Locale.US,"%.6f;\t%.6f;\t%.6f;\t%.6f;\t\n", timeList.get(i), slaViolationTimeList.get(i), powerConsumptionList.get(i), migrationCountList.get(i)));
        }
        writer.close();
    }

    private void init(String experimentFolder, boolean utilizationModel) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);
        broker = SetupEntities.createBroker();
        int brokerId = broker.getId();
        cloudletList = SetupEntities.createCloudletList(brokerId, experimentFolder, utilizationModel);
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

    private void nonPowerAwareModelling(String inputFolder, String outputFolder, String experimentName, String policyName, boolean utilizationModel) throws Exception {
        initLogOutput(outputFolder, experimentName, policyName);
        Log.printLine("Starting " + experimentName);

        CloudSim.init(1, Calendar.getInstance(), false);

        DatacenterBroker broker = SetupEntities.createBroker();
        int brokerId = broker.getId();

        List<Cloudlet> cloudletList = SetupEntities.createCloudletList(brokerId, inputFolder + "/" + experimentName, utilizationModel);
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

    private VmAllocationPolicy getVmAllocationPolicy(String vmAllocationPolicyName, String vmSelectionPolicyName) {
        VmAllocationPolicy vmAllocationPolicy = null;
        PowerVmSelectionPolicy vmSelectionPolicy = getVmSelectionPolicy(vmSelectionPolicyName);
        PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy =
                new PowerVmAllocationPolicyMigrationStaticThreshold(hostList, vmSelectionPolicy, 0.7);
        switch (vmAllocationPolicyName) {
            case "Iqr":
                vmAllocationPolicy =
                        new VmAllocationPolicyInterQuartileRange(hostList, vmSelectionPolicy, 1.5, fallbackVmSelectionPolicy);
                break;
            case "Mad":
                vmAllocationPolicy =
                        new VmAllocationPolicyMedianAbsoluteDeviation(hostList, vmSelectionPolicy, 2.5, fallbackVmSelectionPolicy);
                break;
            case "Lr":
                vmAllocationPolicy =
                        new VmAllocationPolicyLocalRegression(hostList, vmSelectionPolicy, 1.2, ParseConfig.schedulingInterval, fallbackVmSelectionPolicy);
                break;
            case "Lrr":
                vmAllocationPolicy =
                        new VmAllocationPolicyLocalRegressionRobust(hostList, vmSelectionPolicy, 1.2, ParseConfig.schedulingInterval, fallbackVmSelectionPolicy);
                break;
            case "Thr":
                vmAllocationPolicy =
                        new VmAllocationPolicyStaticThreshold(hostList, vmSelectionPolicy, 0.8);
                break;
        }
        return vmAllocationPolicy;
    }

    private PowerVmSelectionPolicy getVmSelectionPolicy(String vmSelectionPolicyName) {
        PowerVmSelectionPolicy vmSelectionPolicy = null;
        switch (vmSelectionPolicyName) {
            case "Mc":
                vmSelectionPolicy = new PowerVmSelectionPolicyMaximumCorrelation(new PowerVmSelectionPolicyMinimumMigrationTime());
                break;
            case "Mmt":
                vmSelectionPolicy = new PowerVmSelectionPolicyMinimumMigrationTime();
                break;
            case "Mu":
                vmSelectionPolicy = new PowerVmSelectionPolicyMinimumUtilization();
                break;
            case "Rs":
                vmSelectionPolicy = new PowerVmSelectionPolicyRandomSelection();
                break;
        }
        return vmSelectionPolicy;
    }

}
