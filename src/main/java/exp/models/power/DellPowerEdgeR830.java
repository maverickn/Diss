package exp.models.power;

import cloudsim.power.models.PowerModelSpecPower;

public class DellPowerEdgeR830 extends PowerModelSpecPower {

    //spec.org/power_ssj2008/results/res2016q3/power_ssj2008-20160705-00737.html
    //average active power (W), first item - active idle, rest of the items - target load 10%, 20%,...100%
    private final double[] power = { 86.4, 181, 215, 253, 287, 315, 340, 377, 421, 483, 562 };

    @Override
    protected double getPowerData(int index) {
        return power[index];
    }
}
