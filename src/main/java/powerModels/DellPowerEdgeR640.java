package powerModels;

import org.cloudbus.cloudsim.power.models.PowerModelSpecPower;

public class DellPowerEdgeR640 extends PowerModelSpecPower {

    //spec.org/power_ssj2008/results/res2017q3/power_ssj2008-20170829-00781.html
    //average active power (W), first item - active idle, rest of the items - target load 10%, 20%,...100%
    private final double[] power = { 55, 125, 151, 176, 202, 232, 261, 301, 357, 421, 469 };

    @Override
    protected double getPowerData(int index) {
        return power[index];
    }
}
