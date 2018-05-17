package exp;

import cloudsim.*;
import cloudsim.core.CloudSim;
import cloudsim.power.*;
import exp.policy.*;

import java.io.*;
import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static exp.ParseConfig.experimentName;

public class Runner {

    private DatacenterBroker broker;

    private List<Cloudlet> cloudletList;

    private List<Vm> vmList;

    private List<PowerHost> hostList;

    public Runner(String inputFolder, String experimentName, String policyName) throws Exception {
        initLogOutput(experimentName, policyName);
        VmAllocationPolicy vap;
        switch (policyName) {
            case "Qla":
                init(inputFolder + "/" + experimentName);
                PowerVmSelectionPolicy pvsp = new PowerVmSelectionPolicyMinimumMigrationTime();
                PowerVmAllocationPolicyMigrationAbstract fbvsp = new PowerVmAllocationPolicyMigrationStaticThreshold(hostList, pvsp, 0.7);
                PowerVmAllocationPolicyMigrationLocalRegression vaplr = new PowerVmAllocationPolicyMigrationLocalRegression(hostList, pvsp, 1.2, ParseConfig.schedulingInterval, fbvsp);

                vap = new HostPowerModeSelectionPolicyAgent(ParseConfig.learningRate, ParseConfig.discountFactor, ParseConfig.cofImportanceSla, ParseConfig.cofImportancePower, pvsp, vaplr, hostList);
                policyName += " " + ParseConfig.learningRate + " " + ParseConfig.discountFactor + " " + ParseConfig.cofImportanceSla + " " + ParseConfig.cofImportancePower;
                start(experimentName, vap, policyName);
                break;
            case "Npa":
                nonPowerAwareModelling(inputFolder, experimentName, policyName);
                break;
            case "Dvfs":
                init(inputFolder + "/" + experimentName);
                vap = new VmAllocationPolicyNonPowerAware(hostList);
                start(experimentName, vap, policyName);
                break;
            default:
                init(inputFolder + "/" + experimentName);
                vap = getVmAllocationPolicy(policyName.split(" ")[0], policyName.split(" ")[1]);
                start(experimentName, vap, policyName);
                break;
        }
    }

    private void initLogOutput(String experimentName, String policyName) throws IOException {
        Log.enable();
        File folder = new File("output");
        if (!folder.exists()) {
            folder.mkdir();
        }
        File folder2 = new File("output/log");
        if (!folder2.exists()) {
            folder2.mkdir();
        }
        File file = new File("output/log/" + policyName + "_" + experimentName + ".log");
        file.createNewFile();
        Log.setOutput(new FileOutputStream(file));
    }

    private void printResults(String experimentName, String policyName) throws IOException {
        File folder = new File("output");
        if (!folder.exists()) {
            folder.mkdir();
        }
        File folder1 = new File("output/metrics");
        if (!folder1.exists()) {
            folder1.mkdir();
        }

        List<Double> timeList = HostPowerModeSelectionPolicyAgent.getTimeList();
        List<Double> slaViolationTimeList = HostPowerModeSelectionPolicyAgent.getSlaViolationTimeList();
        List<Double> powerConsumptionList = HostPowerModeSelectionPolicyAgent.getPowerConsumptionList();
        List<Double> migrationCountList = HostPowerModeSelectionPolicyAgent.getMigrationCountList();

        File file = new File("output/metrics/" + policyName + "_" + experimentName + "_metric.csv");
        file.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (int i = 0; i < timeList.size(); i++) {
            writer.write(String.format(Locale.US,"%.6f;\t%.6f;\t%.6f;\t%.6f;\t\n", timeList.get(i), slaViolationTimeList.get(i), powerConsumptionList.get(i), migrationCountList.get(i)));
        }
        writer.close();
    }

    /*public static void printResults2(List<HostDynamicWorkload> hosts) {
        StringBuilder Hdata = new StringBuilder();
        String delimeter = ",";
        Hdata.append("HostId;Time;Allocated;Requested;IsActive");
        Hdata.append("\n");
        for (Host _host : hosts) {
            HostDynamicWorkload host = (HostDynamicWorkload) _host;
            for (HostStateHistoryEntry entry : host.getStateHistory()) {
                double Allocated = entry.getAllocatedMips();
                double Requested = entry.getRequestedMips();
                double Time = entry.getTime();
                boolean IsActive = entry.isActive();
                Hdata.append(String.format(host.getId() + delimeter));
                Hdata.append(String.format("" + Time) + delimeter);
                Hdata.append(String.format("" + Allocated) + delimeter);
                Hdata.append(String.format("" + Requested) + delimeter);
                Hdata.append(String.format("" + IsActive) + delimeter);
                Hdata.append("\n");
            }
        }
        Hdata.append("\n");
        writeDataRow(Hdata.toString(), "output/" + experimentName + "_hostsstate.csv");
    }

    public static void writeDataRow(String data, String outputPath) {
        File file = new File(outputPath);
        try {
            file.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
            System.exit(0);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }*/

    private void init(String experimentFolder) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);
        broker = SetupEntities.createBroker();
        int brokerId = broker.getId();
        cloudletList = SetupEntities.createCloudletList(brokerId, experimentFolder);
        vmList = SetupEntities.createVmList(brokerId, cloudletList.size());
        hostList = SetupEntities.createHostList(ParseConfig.hostsCount);
    }

    private void start(String experimentName, VmAllocationPolicy vmAllocationPolicy, String policyName) throws Exception {
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
        printResults(experimentName, policyName);
        Log.printLine("Finished " + experimentName);
    }

    private void nonPowerAwareModelling(String inputFolder, String experimentName, String policyName) throws Exception {
        initLogOutput(experimentName, policyName);
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
        printResults(experimentName, policyName);

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
                        new PowerVmAllocationPolicyMigrationInterQuartileRange(hostList, vmSelectionPolicy, 1.5, fallbackVmSelectionPolicy);
                break;
            case "Mad":
                vmAllocationPolicy =
                        new PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation(hostList, vmSelectionPolicy, 2.5, fallbackVmSelectionPolicy);
                break;
            case "Lr":
                vmAllocationPolicy =
                        new PowerVmAllocationPolicyMigrationLocalRegression(hostList, vmSelectionPolicy, 1.2, ParseConfig.schedulingInterval, fallbackVmSelectionPolicy);
                break;
            case "Lrr":
                vmAllocationPolicy =
                        new PowerVmAllocationPolicyMigrationLocalRegressionRobust(hostList, vmSelectionPolicy, 1.2, ParseConfig.schedulingInterval, fallbackVmSelectionPolicy);
                break;
            case "Thr":
                vmAllocationPolicy =
                        new PowerVmAllocationPolicyMigrationStaticThreshold(hostList, vmSelectionPolicy, 0.8);
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
