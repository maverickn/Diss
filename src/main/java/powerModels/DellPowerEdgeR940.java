package powerModels;

import org.cloudbus.cloudsim.power.models.PowerModelSpecPower;

public class DellPowerEdgeR940 extends PowerModelSpecPower {

    //spec.org/power_ssj2008/results/res2017q4/power_ssj2008-20171010-00789.html
    //average active power (W), first item - active idle, rest of the items - target load 10%, 20%,...100%
    private final double[] power = { 106, 245, 292, 336, 383, 437, 502, 583, 694, 820, 915 };

    @Override
    protected double getPowerData(int index) {
        return power[index];
    }
}
