import org.cloudbus.cloudsim.power.models.PowerModel;
import powerModels.PowerModelSpecPowerFujitsuSiemensComputersPrimergyRX300S4IntelXeonE5440;
import powerModels.PowerModelSpecPowerSuperMicroComputer6025BTRIntelXeon5160;

public class Parameters {

	public final static boolean ENABLE_OUTPUT = true;
	public final static boolean OUTPUT_CSV    = false;

	public final static double SCHEDULING_INTERVAL = 300;
	public final static double SIMULATION_LIMIT = 24 * 60 * 60;

	public final static int CLOUDLET_LENGTH	= 2500 * (int) SIMULATION_LIMIT;
	public final static int CLOUDLET_PES = 1;

	public final static int VM_TYPES = 4;
	public final static int[] VM_MIPS = { 2500, 2000, 1000, 500 };
	public final static int[] VM_PES = { 1, 1, 1, 1 };
	public final static int[] VM_RAM = { 870,  1740, 1740, 613 };
	public final static int VM_BW = 100000; // 100 Mbit/s
	public final static int VM_SIZE = 2500; // 2.5 GB
	public final static int NUMBER_OF_VMS = 50;

	public final static int HOST_TYPES = 2;
	public final static int[] HOST_MIPS = { 2830, 3000 };
	public final static int[] HOST_PES = { 8, 4 };
	public final static int[] HOST_RAM = { 16384, 8192 };
	// TODO:
	public final static int HOST_BW = 1000000; // 1 Gbit/s
	public final static int HOST_STORAGE = 1000000; // 1 GB
	public final static int NUMBER_OF_HOSTS = 100;

	public final static PowerModel[] HOST_POWER = {
			new PowerModelSpecPowerFujitsuSiemensComputersPrimergyRX300S4IntelXeonE5440(),
			new PowerModelSpecPowerSuperMicroComputer6025BTRIntelXeon5160() };

}
