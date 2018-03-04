import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerDatacenterNonPowerAware;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;

import java.util.Calendar;
import java.util.List;

public class NonPowerAware {

    public static void main(String[] args) {
        ParseConfig.getData("dc_config_bitbrains.json");

        Log.setDisabled(!ParseConfig.enableOutput);
        Log.printLine("Starting " + ParseConfig.experimentName);

        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            DatacenterBroker broker = Environment.createBroker();
            int brokerId = broker.getId();

            List<Cloudlet> cloudletList = Environment.createCloudletList(brokerId, ParseConfig.inputFolder + "/" + ParseConfig.experimentName);
            List<Vm> vmList = Environment.createVmList(brokerId, cloudletList.size());
            List<PowerHost> hostList = Environment.createHostList(ParseConfig.hostsCount);

            PowerDatacenterNonPowerAware datacenter = (PowerDatacenterNonPowerAware) Environment.createDatacenter(
                    "Datacenter",
                    PowerDatacenterNonPowerAware.class,
                    hostList,
                    new PowerVmAllocationPolicySimple(hostList));

            datacenter.setDisableMigrations(true);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.terminateSimulation(ParseConfig.simulationLimit);
            double lastClock = CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            Log.printLine("Received " + newList.size() + " cloudlets");

            CloudSim.stopSimulation();

            Environment.printResults(datacenter, vmList, lastClock, ParseConfig.experimentName, ParseConfig.outputCsv, ParseConfig.outputFolder);

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
        Log.printLine("Finished " + ParseConfig.experimentName);
    }

}
