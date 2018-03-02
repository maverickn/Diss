import java.util.Objects;

public class QLearingAgent {

    public static void main(String[] args) {
        ParseConfig.getData();
        String path = Objects.requireNonNull(QLearingAgent.class.getClassLoader().getResource(ParseConfig.inputFolder)).getPath();
        new Runner(ParseConfig.enableOutput, ParseConfig.outputLog, ParseConfig.outputFolder, ParseConfig.experimentName, path);
    }
}
