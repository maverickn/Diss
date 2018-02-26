import models.power.DellPowerEdgeR640;
import models.power.DellPowerEdgeR740;
import models.power.DellPowerEdgeR830;
import models.power.DellPowerEdgeR940;
import org.cloudbus.cloudsim.power.models.PowerModel;

public class Parameters {

	//matisse.net/bitcalc/
	public final static boolean ENABLE_OUTPUT = true;
	public final static boolean OUTPUT_CSV = false;

	public final static double SCHEDULING_INTERVAL = 300; //5 minutes
	public final static double SIMULATION_LIMIT = 24 * 60 * 60;

	public final static int CLOUDLET_LENGTH	= 2500 * (int) SIMULATION_LIMIT;
	public final static int CLOUDLET_PES = 1;

	public final static int VM_TYPES = 4;
	public final static int[] VM_MIPS = { 2500, 2000, 1000, 500 };
	public final static int[] VM_PES = { 1, 1, 1, 1 };
	public final static int[] VM_RAM = { 870,  1740, 1740, 650 }; // MB
	public final static int VM_BW = 100000; // Kbit 100 Mbit (kilobyte = 1000 bytes)
	public final static int VM_SIZE = 2500; // MB 2.5 GB (kilobyte = 1000 bytes)

	public final static int HOST_TYPES = 4;
	public final static int[] HOST_MIPS = { 2500, 2500, 2200, 2500 };
	public final static int[] HOST_PES = { 56, 56, 88, 112 };
	public final static int[] HOST_RAM = { 196608, 196608, 262144, 393216 }; // MB
	public final static int HOST_BW = 10000000; //  Kbit/s 10 Gbit/s (kilobyte = 1000 bytes)
	public final static int HOST_STORAGE = 8000000; // MB, 1 TB (kilobyte = 1000 bytes)

	public final static int NUMBER_OF_HOSTS = 15;

	public final static PowerModel[] HOST_POWER = {
			new DellPowerEdgeR640(),
			new DellPowerEdgeR740(),
			new DellPowerEdgeR830(),
			new DellPowerEdgeR940() };
}
