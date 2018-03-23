import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerDatacenterNonPowerAware;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class NonPowerAware {

    public static void main(String[] args) {
        try {
            ParseConfig.getData("dc_config_bitbrains.json");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(0);
        }
        try {
            Runner.initLogOutput(ParseConfig.outputFolder, ParseConfig.experimentName);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        Log.printLine("Starting " + ParseConfig.experimentName);

        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            DatacenterBroker broker = SetupEntities.createBroker();
            int brokerId = broker.getId();

            List<Cloudlet> cloudletList = SetupEntities.createCloudletList(brokerId, ParseConfig.inputFolder + "/" + ParseConfig.experimentName);
            List<Vm> vmList = SetupEntities.createVmList(brokerId, cloudletList.size());
            List<PowerHost> hostList = SetupEntities.createHostList(ParseConfig.hostsCount);

            PowerDatacenterNonPowerAware datacenter = (PowerDatacenterNonPowerAware) SetupEntities.createDatacenter(
                    "Datacenter",
                    PowerDatacenterNonPowerAware.class,
                    hostList,
                    new PowerVmAllocationPolicySimple(hostList));

            datacenter.setDisableMigrations(true);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.terminateSimulation(ParseConfig.simulationLimit);
            //double lastClock =
            CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            Log.printLine("Received " + newList.size() + " cloudlets");

            CloudSim.stopSimulation();

            //SetupEntities.printResults(datacenter, vmList, lastClock, ParseConfig.experimentName, ParseConfig.outputFolder);

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
        Log.printLine("Finished " + ParseConfig.experimentName);
        System.out.println("Done!");
    }

}
