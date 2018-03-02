import java.util.Objects;

public class QLearingAgent {

    public static void main(String[] args) {
        String inputFolder = Objects.requireNonNull(QLearingAgent.class.getClassLoader().getResource("planetlab")).getPath();
        new Runner(Parameters.ENABLE_OUTPUT, false, "output", "trace_1",  inputFolder);
    }
}
