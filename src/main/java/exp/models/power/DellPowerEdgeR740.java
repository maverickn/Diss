package exp.models.power;

import cloudsim.power.models.PowerModelSpecPower;

public class DellPowerEdgeR740 extends PowerModelSpecPower {

    //spec.org/power_ssj2008/results/res2017q3/power_ssj2008-20170829-00780.html
    //average active power (W), first item - active idle, rest of the items - target load 10%, 20%,...100%
    private final double[] power = { 52.3, 129, 147, 170, 195, 224, 255, 292, 344, 408, 457 };

    @Override
    protected double getPowerData(int index) {
        return power[index];
    }
}
