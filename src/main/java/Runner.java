import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.*;
import policy.HostPowerModeSelectionPolicyAgent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class Runner {

    protected static DatacenterBroker broker;

    protected static List<Cloudlet> cloudletList;

    protected static List<Vm> vmList;

    protected static List<PowerHost> hostList;

    public Runner(boolean enableOutput, boolean outputToLogFile, String outputFolder, String experimentName, String inputFolder) {
        try {
            initLogOutput(enableOutput, outputToLogFile, outputFolder, experimentName);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        init(inputFolder + "/" + experimentName);
        VmAllocationPolicy vap = new HostPowerModeSelectionPolicyAgent(0.5, 0.5, 0.5, 0.5, new PowerVmSelectionPolicyMinimumMigrationTime(), hostList);
        start(experimentName, outputFolder, vap);
    }

    protected void initLogOutput(boolean enableOutput, boolean outputToLogFile, String outputFolder, String experimentName) throws IOException {
        Log.setDisabled(!enableOutput);
        if (enableOutput && outputToLogFile) {
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }
            File folder2 = new File(outputFolder + "/log");
            if (!folder2.exists()) {
                folder2.mkdir();
            }
            File file = new File(outputFolder + "/log/" + experimentName + ".txt");
            file.createNewFile();
            Log.setOutput(new FileOutputStream(file));
        }
    }

    protected void init(String experimentFolder) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);
            broker = Environment.createBroker();
            int brokerId = broker.getId();
            cloudletList = Environment.createCloudletList(brokerId, experimentFolder);
            vmList = Environment.createVmList(brokerId, cloudletList.size());
            hostList = Environment.createHostList(Parameters.NUMBER_OF_HOSTS);
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
    }

    protected void start(String experimentName, String outputFolder, VmAllocationPolicy vmAllocationPolicy) {
        System.out.println("Starting " + experimentName);
        try {
            PowerDatacenter datacenter = (PowerDatacenter) Environment.createDatacenter("Datacenter",
                    PowerDatacenter.class, hostList, vmAllocationPolicy);
            datacenter.setDisableMigrations(false);
            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);
            CloudSim.terminateSimulation(Parameters.SIMULATION_LIMIT);
            double lastClock = CloudSim.startSimulation();
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            Log.printLine("Received " + newList.size() + " cloudlets");
            CloudSim.stopSimulation();
            Environment.printResults(datacenter, vmList, lastClock, experimentName, Parameters.OUTPUT_CSV, outputFolder);
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
        Log.printLine("Finished " + experimentName);
    }

}
