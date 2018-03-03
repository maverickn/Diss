package policy;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.HostDynamicWorkload;
import org.cloudbus.cloudsim.HostStateHistoryEntry;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class HostPowerModeSelectionPolicyAgent extends VmAllocationPolicyMigrationAbstract {

    private double learningRate;

    private double discountFactor;

    private double cofImportanceSla;

    private double cofImportancePower;

    private final List<String> statesList = new ArrayList<>();

    private List<Integer> actionsList = new ArrayList<>();

    private List<List<Double>> qTable = new ArrayList<>();

    private final List<Double> slaViolationTimeList = new ArrayList<>();

    private final List<Double> powerConsumptionList = new ArrayList<>();

    private Double oldQValue = 0.0;

    private int cnter = 0;

    public HostPowerModeSelectionPolicyAgent(double learningRate, double discountFactor, double cofImportanceSla, double cofImportancePower,
                                             PowerVmSelectionPolicy vmSelectionPolicy, List<? extends Host> hostList) {
        super(hostList, vmSelectionPolicy);
        setLearningRate(learningRate);
        setDiscountFactor(discountFactor);
        setCofImportanceSla(cofImportanceSla);
        setCofImportancePower(cofImportancePower);
    }

    /**
     * Get host id and power mode
     * @return
     */
    public int[] getHostPowerMode() {
        Random rand = new Random();

        String state = observeState();
        System.out.println("state: " + state);

        int stateIndex = saveState(state);
        System.out.println("stateIndex: " + stateIndex);

        double penalty = getPenalty();
        System.out.println("penalty: " + penalty);

        double minQValue = Collections.min(getQTable().get(stateIndex));
        System.out.println("minQValue: " + minQValue);

        for (int i = 0; i < qTable.size(); i++) {
            for (int j = 0; j < qTable.get(0).size(); j++) {
                if (qTable.get(i).get(j) == Double.MAX_VALUE) {
                    System.out.print("-\t");
                } else {
                    System.out.print(qTable.get(i).get(j) + "\t");
                }
            }
            System.out.print("\n");
        }

        System.out.println("getSlaViolationTimeList:");
        for (int i = 0; i < getSlaViolationTimeList().size(); i++) {
            System.out.println(i + "\t" + getSlaViolationTimeList().get(i));
        }

        System.out.println("getPowerConsumptionList:");
        for (int i = 0; i < getPowerConsumptionList().size(); i++) {
            System.out.println(i + "\t" + getPowerConsumptionList().get(i));
        }

        int actionIndex = 0;
        if (minQValue == Double.MAX_VALUE) {
            int counter = 0;
            for (PowerHost host : this.<PowerHost> getHostList()) {
                if (host.getUtilizationOfCpu() < 0.4) {
                    actionIndex = host.getId() * 2 + 1;
                    break;
                }
                counter ++;
            }
            if (counter == this.<PowerHost> getHostList().size()) {
                actionIndex = rand.nextInt(getActionsList().size());
            }
        } else {
            actionIndex = getQTable().get(stateIndex).indexOf(minQValue);
        }
        System.out.println("actionIndex: " + actionIndex);

        double newQvalue = getNewQValue(penalty, minQValue);
        System.out.println("newQvalue: " + newQvalue);

        getQTable().get(stateIndex).set(actionIndex, newQvalue);
        for (int i = 0; i < qTable.size(); i++) {
            for (int j = 0; j < qTable.get(0).size(); j++) {
                if (qTable.get(i).get(j) == Double.MAX_VALUE) {
                    System.out.print("-\t");
                } else {
                    System.out.print(qTable.get(i).get(j) + "\t");
                }
            }
            System.out.print("\n");
        }

        oldQValue = getQTable().get(stateIndex).get(actionIndex);

        int powerMode = getActionsList().get(actionIndex);
        System.out.println("powerMode: " + powerMode);

        if (cnter == 4) {
            System.exit(0);
        } else {
            cnter ++;
        }

        return new int[] {actionIndex, powerMode};
    }

    /**
     * Observe state (get sum cpu, sum ram, sum bw utilizations of each host, convert utilization values to intervals)
     * @return hashcode of hosts state
     */
    private String observeState() {
        String state;
        double cpuUtilPercent = 0;
        double ramUtilPercent = 0;
        double bwUtilPercent = 0;

        double ramUtilAbsolute = 0;
        double bwUtilAbsolute = 0;

        double cpuUtilTotalPercent = 0;
        double ramTotalAbsolute = 0;
        double bwTotalAbsolute = 0;

        int maxCpuUtil = this.<PowerHost> getHostList().size();
        for (PowerHost host : this.<PowerHost> getHostList()) {
            cpuUtilTotalPercent += host.getUtilizationOfCpu();
            ramUtilAbsolute += host.getUtilizationOfRam();
            bwUtilAbsolute += host.getUtilizationOfBw();

            ramTotalAbsolute += host.getRam();
            bwTotalAbsolute += host.getBw();
        }
        cpuUtilPercent = cpuUtilTotalPercent / maxCpuUtil;
        ramUtilPercent = ramUtilAbsolute / ramTotalAbsolute;
        bwUtilPercent = bwUtilAbsolute / bwTotalAbsolute;

        System.out.println("cpuUtilPercent: " + cpuUtilPercent);
        System.out.println("ramUtilPercent: " + ramUtilPercent);
        System.out.println("bwUtilPercent: " + bwUtilPercent);
        System.out.println();

        DecimalFormat df = new DecimalFormat("#.#");
        state = df.format(cpuUtilPercent) + df.format(ramUtilPercent) + df.format(bwUtilPercent);
        getSlaViolationTime();
        getTotalPower();
        return state;
    }

    /**
     * Save state in statesList, add row to qTable with Double.MAX_VALUE
     * @param state state
     * @return index of state in statesList
     */
    private int saveState(String state) {
        if (getStatesList().isEmpty()) {
            for (int i = 0; i < this.getHostList().size(); i++) {
                getActionsList().add(1);
                getActionsList().add(-1);
            }
            getStatesList().add(state);
            getQTable().add(new ArrayList<>());
            for (int i = 0; i < getActionsList().size(); i++) {
                getQTable().get(0).add(Double.MAX_VALUE);
            }
            return 0;
        } else if (getStatesList().contains(state)) {
            return getStatesList().indexOf(state);
        }
        else {
            getStatesList().add(state);
            int index = getStatesList().indexOf(state);
            getQTable().add(new ArrayList<>());
            for (int i = 0; i < getActionsList().size(); i++) {
                getQTable().get(index).add(Double.MAX_VALUE);
            }
            return index;
        }
    }

    /**
     * Get total SLA violation time per each active host
     */
    private void getSlaViolationTime() {
        double slaViolationTimePerHost = 0;
        for (HostDynamicWorkload host : this.<HostDynamicWorkload> getHostList()) {
            double previousTime = -1;
            double previousAllocated = 0;
            double previousRequested = 0;
            boolean previousIsActive = true;
            for (HostStateHistoryEntry entry : host.getStateHistory()) {
                if (previousTime != -1 && previousIsActive) {
                    double timeDiff = entry.getTime() - previousTime;
                    if (previousAllocated < previousRequested) {
                        slaViolationTimePerHost += timeDiff;
                    }
                }
                previousAllocated = entry.getAllocatedMips();
                previousRequested = entry.getRequestedMips();
                previousTime = entry.getTime();
                previousIsActive = entry.isActive();
            }
        }
        getSlaViolationTimeList().add(slaViolationTimePerHost);
    }

    /**
     * Get datacenter power
     */
    private void getTotalPower() {
        Host host = getHostList().get(0);
        PowerDatacenter datacenter = (PowerDatacenter) host.getDatacenter();
        getPowerConsumptionList().add(datacenter.getPower() / (3600 * 1000));
    }

    /**
     * Get SLA violation penalty or power consumption penalty
     * @param list list with SLA violation time or power consumption
     * @return penalty
     */
    private double getPartPenalty(List<Double> list) {
        int size = list.size();
        if (size < 3) {
            return 1.0;
        } else {
            double penalty = ((list.get(size - 1) - list.get(size - 2))/(list.get(size - 2) - list.get(size - 3)));
            if (Double.isNaN(penalty)) {
                return 1.0;
            } else {
                return penalty;
            }
        }
    }

    /**
     * Get total penalty (SLA violation penalty + power consumption penalty)
     * @return total penalty
     */
    private double getPenalty() {
        return (cofImportanceSla * getPartPenalty(getSlaViolationTimeList()) + cofImportancePower * getPartPenalty(getPowerConsumptionList()));
    }

    /**
     * Get new Q-value
     * @param penalty total penalty
     * @param estimateOptimalFutureValue estimate of optimal future value
     * @return new Q-value
     */
    private double getNewQValue(double penalty, double estimateOptimalFutureValue) {
        return ((1 - learningRate) * oldQValue + learningRate * (penalty + discountFactor * estimateOptimalFutureValue));
    }

    public void setLearningRate(double learningRate) {
        if (learningRate < 0 || learningRate > 1) {
            throw new IllegalArgumentException("Learning rate must be between 0 and 1");
        } else {
            this.learningRate = learningRate;
        }
    }

    public void setDiscountFactor(double discountFactor) {
        if (discountFactor < 0 || discountFactor > 1) {
            throw new IllegalArgumentException("Discount factor must be between 0 and 1");
        } else {
            this.discountFactor = discountFactor;
        }
    }

    public void setCofImportanceSla(double cofImportanceSla) {
        if (cofImportanceSla < 0 || cofImportanceSla > 1) {
            throw new IllegalArgumentException("Coefficient of SLA importance must be between 0 and 1");
        } else {
            this.cofImportanceSla = cofImportanceSla;
        }
    }

    public void setCofImportancePower(double cofImportancePower) {
        if (cofImportancePower < 0 || cofImportancePower > 1) {
            throw new IllegalArgumentException("Coefficient of power importance must be between 0 and 1");
        } else {
            this.cofImportancePower = cofImportancePower;
        }
    }

    public List<String> getStatesList() {
        return statesList;
    }

    public List<Integer> getActionsList() {
        return actionsList;
    }

    public List<List<Double>> getQTable() {
        return qTable;
    }

    public List<Double> getSlaViolationTimeList() {
        return slaViolationTimeList;
    }

    public List<Double> getPowerConsumptionList() {
        return powerConsumptionList;
    }
}
