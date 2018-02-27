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

    private static boolean enableOutput;

    protected static DatacenterBroker broker;

    protected static List<Cloudlet> cloudletList;

    protected static List<Vm> vmList;

    protected static List<PowerHost> hostList;

    public Runner(boolean enableOutput, boolean outputToFile, String inputFolder, String outputFolder, String workload, String experiment, String parameter) {
        String experimentName = workload + "_" + experiment + "_" + parameter;
        try {
            initLogOutput(enableOutput, outputToFile, outputFolder, experimentName);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        init(inputFolder + "/" + workload);
        start(experimentName, outputFolder, getVmAllocationPolicyLrMmt(parameter));
    }

    protected void initLogOutput( boolean enableOutput, boolean outputToFile, String outputFolder, String experimentName) throws IOException {
        setEnableOutput(enableOutput);
        Log.setDisabled(!isEnableOutput());
        if (isEnableOutput() && outputToFile) {
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

    protected void init(String inputFolder) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);
            broker = Environment.createBroker();
            int brokerId = broker.getId();
            cloudletList = Environment.createCloudletList(brokerId, inputFolder);
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

            /*HostPowerModeSelectionPolicyAgent a = new HostPowerModeSelectionPolicyAgent(new PowerVmSelectionPolicyMinimumMigrationTime(), hostList);
            int firstState = a.saveState(1234567890);
            int secondState = a.saveState(1234567890);
            int thirdState = a.saveState(a.observeState());

            System.out.println(firstState);
            System.out.println(secondState);
            System.out.println(thirdState);

            List<List<Double>> qTable = a.getQTable();

            for (int i = 0; i < qTable.size(); i++) {
                for (int j = 0; j < qTable.get(0).size(); j++) {
                    if (qTable.get(i).get(j) == Double.MAX_VALUE) {
                        System.out.print("--\t");
                    } else {
                        System.out.print(qTable.get(i).get(j) + "\t");
                    }
                }
                System.out.print("\n");
            }

            List<Integer> listStates = a.getStatesList();
            for (int i = 0; i < listStates.size(); i++) {
                System.out.println(i + "\t" + listStates.get(i));
            }

            List<Boolean> listActions = a.getActionsList();
            for (int i = 0; i < listActions.size(); i++) {
                System.out.println(i + "\t" + listActions.get(i));
            }
            System.out.print(a.getPowerConsumptionList().toString());
            System.out.print(a.getSlaViolationTimeList().toString());*/
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
        Log.printLine("Finished " + experimentName);
    }

    protected VmAllocationPolicy getVmAllocationPolicyLrMmt(String parameterName) {
        PowerVmSelectionPolicy vmSelectionPolicy = new PowerVmSelectionPolicyMinimumMigrationTime();
        double parameter = 0;
        if (!parameterName.isEmpty()) {
            parameter = Double.valueOf(parameterName);
        }
        PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                hostList, vmSelectionPolicy,0.7);
        return new PowerVmAllocationPolicyMigrationLocalRegression(hostList, vmSelectionPolicy, parameter, Parameters.SCHEDULING_INTERVAL, fallbackVmSelectionPolicy);
    }

    public void setEnableOutput(boolean enableOut) {
        enableOutput = enableOut;
    }

    public boolean isEnableOutput() {
        return enableOutput;
    }

}
