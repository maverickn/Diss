package powerModels;

import org.cloudbus.cloudsim.power.models.PowerModelSpecPower;

public class PowerModelSpecPowerFujitsuSiemensComputersPrimergyRX300S4IntelXeonE5440 extends PowerModelSpecPower {

    //http://spec.org/power_ssj2008/results/res2008q1/
    //http://spec.org/power_ssj2008/results/res2008q1/power_ssj2008-20080116-00034.html
    private final double[] power = { 166, 179, 191, 200, 212, 224, 235, 245, 252, 259, 265 };

    @Override
    protected double getPowerData(int index) {
        return power[index];
    }
}
