package exp.policy;

import cloudsim.Host;
import cloudsim.HostDynamicWorkload;
import cloudsim.HostStateHistoryEntry;
import cloudsim.power.*;
import exp.Runner;

import java.text.DecimalFormat;
import java.util.*;

public class HostPowerModeSelectionPolicyAgent extends VmAllocationPolicyMigrationAgent {

    /**
     * The learning rate
     */
    private double learningRate;

    /**
     * The discount factor
     */
    private double discountFactor;

    /**
     * The coefficient of SLA importance
     */
    private double cofImportanceSla;

    /**
     * The coefficient of power consumption importance
     */
    private double cofImportancePower;

    /**
     * A list of states
     */
    private final List<String> statesList = new ArrayList<>();

    /**
     * A list of actions
     */
    private final List<Integer> actionsList = new ArrayList<>();

    /**
     * A table with Q-values
     */
    private List<List<Double>> qTable = new ArrayList<>();

    /**
     * The sla violation time list
     */
    private static final List<Double> slaViolationTimeList = new ArrayList<>();

    /**
     * The power consumption list
     */
    private static final List<Double> powerConsumptionList = new ArrayList<>();

    /**
     * The VM migration count list
     */
    private static final List<Double> migrationCountList = new ArrayList<>();

    /**
     * The power consumption list
     */
    private static final List<Double> timeList = new ArrayList<>();

    /**
     * The previous Q-value
     */
    private Double previousQValue = 0.0;

    //private int cnter = 0;

    /**
     * Instantiates a new HostPowerModeSelectionPolicyAgent.
     *
     * @param learningRate the learning rate
     * @param discountFactor the discount Factor
     * @param cofImportanceSla the coefficient of SLA importance importance
     * @param cofImportancePower the coefficient of power consumption importance
     * @param vmSelectionPolicy the vm selection policy
     * @param hostList the host list
     */
    public HostPowerModeSelectionPolicyAgent(double learningRate, double discountFactor, double cofImportanceSla, double cofImportancePower,
                                             PowerVmSelectionPolicy vmSelectionPolicy, PowerVmAllocationPolicyMigrationLocalRegression vmAllocationPolicy, List<? extends Host> hostList) {
        super(hostList, vmSelectionPolicy, vmAllocationPolicy);
        setLearningRate(learningRate);
        setDiscountFactor(discountFactor);
        setCofImportanceSla(cofImportanceSla);
        setCofImportancePower(cofImportancePower);
    }

    /**
     * Gets host id and power mode.
     *
     * @return action index and host power mode
     */
    public int[] getHostPowerMode() {
        Random rand = new Random();
        int randomAction = rand.nextInt(5);

        //Runner.printResults2(getHostList());

        String state = observeState();
        //System.out.println("state: " + state);

        int stateIndex = saveState(state);
        //System.out.println("stateIndex: " + stateIndex);

        double penalty = getPenalty();
        //System.out.println("penalty: " + penalty);

        double minQValue = Collections.min(getQTable().get(stateIndex));
        //System.out.println("minQValue: " + minQValue);

        /*System.out.println("getSlaViolationTimeList:");
        for (int i = 0; i < getSlaViolationTimeList().size(); i++) {
            System.out.println(i + "\t" + getSlaViolationTimeList().get(i));
        }

        System.out.println("getPowerConsumptionList:");
        for (int i = 0; i < getPowerConsumptionList().size(); i++) {
            System.out.println(i + "\t" + getPowerConsumptionList().get(i));
        }*/

        int actionIndex = 0;
        if (randomAction != 0) {
            if (minQValue == Double.MAX_VALUE) {
                int counter = 0;
                for (PowerHost host : this.<PowerHost> getHostList()) {
                    if (host.getVmList().size() != 0 && host.getUtilizationOfCpu() > 0.0 && host.getUtilizationOfCpu() < 0.4) {
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
        } else {
            actionIndex = rand.nextInt(getActionsList().size());
        }

        //System.out.println("actionIndex: " + actionIndex);

        double newQvalue = getNewQValue(penalty, minQValue);
        //System.out.println("newQvalue: " + newQvalue);

        getQTable().get(stateIndex).set(actionIndex, newQvalue);
        /*for (int i = 0; i < qTable.size(); i++) {
            for (int j = 0; j < qTable.get(0).size(); j++) {
                if (qTable.get(i).get(j) == Double.MAX_VALUE) {
                    System.out.print("-\t\t\t\t\t\t");
                } else {
                    System.out.print(qTable.get(i).get(j) + "\t");
                }
            }
            System.out.print("\n");
        }*/

        previousQValue = newQvalue;

        int powerMode = getActionsList().get(actionIndex);
        //System.out.println("powerMode: " + powerMode);

        /*if (cnter == 4) {
            CloudSim.terminateSimulation();
        } else {
            cnter ++;
        }*/

        return new int[] {actionIndex, powerMode};
    }

    /**
     * Observe state (get sum cpu, sum ram, sum bw utilizations of each host, convert utilization values to intervals).
     *
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

        /*System.out.println("cpuUtilPercent: " + cpuUtilPercent);
        System.out.println("ramUtilPercent: " + ramUtilPercent);
        System.out.println("bwUtilPercent: " + bwUtilPercent);
        System.out.println();*/

        DecimalFormat df = new DecimalFormat("#.##");
        state = df.format(cpuUtilPercent) + df.format(ramUtilPercent) + df.format(bwUtilPercent);
        return state;
    }

    /**
     * Save state in statesList, add row to qTable with Double.MAX_VALUE.
     *
     * @param state state
     * @return index of state in statesList
     */
    private int saveState(String state) {
        if (getStatesList().isEmpty()) {
            for (int i = 0; i < getHostList().size(); i++) {
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
     * Gets total SLA violation time per each active host.
     */
    public static void getSlaViolationTime(List<HostDynamicWorkload> hosts) {
        double slaViolationTimePerHost = 0;
        for (HostDynamicWorkload host : hosts) {
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
     * Gets datacenter power and migration count.
     */
    public static void getTotalPowerAndMigrationCount(List<Host> hosts) {
        Host host = hosts.get(0);
        PowerDatacenter datacenter = (PowerDatacenter) host.getDatacenter();
        getPowerConsumptionList().add(datacenter.getPower() / (3600 * 1000));
        getMigrationCountList().add((double) datacenter.getMigrationCount());
    }

    /**
     * Gets SLA violation penalty or power consumption penalty.
     *
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
            } else if (Double.isInfinite(penalty)) {
                return Double.MAX_VALUE;
            } else {
                return penalty;
            }
        }
    }

    /**
     * Gets total penalty (SLA violation penalty + power consumption penalty).
     *
     * @return total penalty
     */
    private double getPenalty() {
        return (cofImportanceSla * getPartPenalty(getSlaViolationTimeList()) + cofImportancePower * getPartPenalty(getPowerConsumptionList()));
    }

    /**
     * Gets new Q-value.
     *
     * @param penalty total penalty
     * @param estimateOptimalFutureValue estimate of optimal future value
     * @return new Q-value
     */
    private double getNewQValue(double penalty, double estimateOptimalFutureValue) {
        double qValue = ((1 - learningRate) * previousQValue + learningRate * (penalty + discountFactor * estimateOptimalFutureValue));
        if (Double.isInfinite(qValue)) {
            return Double.MAX_VALUE;
        } else {
            return qValue;
        }
    }

    /**
     * Sets the learning rate.
     *
     * @param learningRate the learning rate
     */
    public void setLearningRate(double learningRate) {
        if (learningRate <= 0 || learningRate > 1) {
            throw new IllegalArgumentException("Швидкість навчання має бути між 0 та 1");
        } else {
            this.learningRate = learningRate;
        }
    }

    /**
     * Sets the discount factor.
     *
     * @param discountFactor the discount factor
     */
    public void setDiscountFactor(double discountFactor) {
        if (discountFactor < 0 || discountFactor > 1) {
            throw new IllegalArgumentException("Коефіцієнт знецінювання має бути між 0 та 1");
        } else {
            this.discountFactor = discountFactor;
        }
    }

    /**
     * Sets the coefficient of SLA importance.
     *
     * @param cofImportanceSla the coefficient of SLA importance
     */
    public void setCofImportanceSla(double cofImportanceSla) {
        if (cofImportanceSla < 0 || cofImportanceSla > 1) {
            throw new IllegalArgumentException("Коефіцієнт що визначає відносну важливість штрафу за порушення SLA має бути між 0 та 1");
        } else {
            this.cofImportanceSla = cofImportanceSla;
        }
    }

    /**
     * Sets the coefficient of power consumption importance.
     *
     * @param cofImportancePower the coefficient of power consumption importance
     */
    public void setCofImportancePower(double cofImportancePower) {
        if (cofImportancePower < 0 || cofImportancePower > 1) {
            throw new IllegalArgumentException("Коефіцієнт що визначає відносну важливість штрафу за споживання електроенергії має бути між 0 та 1");
        } else {
            this.cofImportancePower = cofImportancePower;
        }
    }

    /**
     * Gets a list of states.
     *
     * @return a list of states
     */
    public List<String> getStatesList() {
        return statesList;
    }

    /**
     * Gets a list of actions.
     *
     * @return a list of actions
     */
    public List<Integer> getActionsList() {
        return actionsList;
    }

    /**
     * Gets a table with Q-values.
     *
     * @return a table with Q-values
     */
    public List<List<Double>> getQTable() {
        return qTable;
    }

    /**
     * Gets the sla violation time list
     *
     * @return the sla violation time list
     */
    public static List<Double> getSlaViolationTimeList() {
        return slaViolationTimeList;
    }

    /**
     * Gets the power consumption list
     *
     * @return the power consumption list
     */
    public static List<Double> getPowerConsumptionList() {
        return powerConsumptionList;
    }

    /**
     * Gets the VM migration count list
     *
     * @return the VM migration count list
     */
    public static List<Double> getMigrationCountList() {
        return migrationCountList;
    }

    /**
     * Gets the time list
     *
     * @return the time list
     */
    public static List<Double> getTimeList() {
        return timeList;
    }
}
