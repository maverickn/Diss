import org.cloudbus.cloudsim.core.CloudSim;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.json.simple.parser.ParseException;
import policy.HostPowerModeSelectionPolicyAgent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    private JPanel mainPanel;
    private JPanel slaPanel;
    private JPanel powerPanel;
    private JPanel migrationPanel;

    private JButton selectConfigButton;
    private JButton runButton;

    private JLabel fileNameLabel;
    private JLabel processingLabel;

    private JComboBox comboBox;

    private File selectedFile = null;

    private XYPlot slaPlot;
    private XYPlot powerPlot;
    private XYPlot migrationPlot;

    private int datasetSlaIndex = 0;
    private int datasetPowerIndex = 0;
    private int datasetMigrationIndex = 0;

    public Main() {
        JFrame frame = new JFrame("Diss");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(mainPanel);
        frame.setSize(1250, 1000);
        frame.setLocationRelativeTo(null);

        setUpPanels();
        setUpButtons();
        setUpLabels();
        setUpComboBox();

        final JFreeChart slaChart = ChartFactory.createXYLineChart("SLA violation time","Time", "SLA Violation Time",
                null, PlotOrientation.VERTICAL, true, true, false);
        slaPlot = slaChart.getXYPlot();
        final ValueAxis axis1 = slaPlot.getDomainAxis();
        axis1.setAutoRange(true);
        final NumberAxis rangeAxis1 = new NumberAxis("Range Axis 1");
        rangeAxis1.setAutoRangeIncludesZero(false);
        final ChartPanel slaChartPanel = new ChartPanel(slaChart);
        slaPanel.add(slaChartPanel);
        slaChartPanel.setDomainZoomable(true);

        final JFreeChart powerChart = ChartFactory.createXYLineChart("Power consumption","Time", "Power consumption",
                null, PlotOrientation.VERTICAL, true, true, false);
        powerPlot = powerChart.getXYPlot();
        final ValueAxis axis2 = powerPlot.getDomainAxis();
        axis2.setAutoRange(true);
        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);
        final ChartPanel powerChartPanel = new ChartPanel(powerChart);
        powerPanel.add(powerChartPanel);
        powerChartPanel.setDomainZoomable(true);

        final JFreeChart migrationChart = ChartFactory.createXYLineChart("Migration count","Time", "Migration count",
                null, PlotOrientation.VERTICAL, true, true, false);
        migrationPlot = migrationChart.getXYPlot();
        final ValueAxis axis3 = migrationPlot.getDomainAxis();
        axis3.setAutoRange(true);
        final NumberAxis rangeAxis3 = new NumberAxis("Range Axis 3");
        rangeAxis3.setAutoRangeIncludesZero(false);
        final ChartPanel migrationChartPanel = new ChartPanel(migrationChart);
        migrationPanel.add(migrationChartPanel);
        migrationChartPanel.setDomainZoomable(true);

        frame.setVisible(true);

        selectConfigButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                jfc.setDialogTitle("Select a config file");
                jfc.setAcceptAllFileFilterUsed(false);
                FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON files", "json");
                jfc.addChoosableFileFilter(filter);
                int returnValue = jfc.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    selectedFile = jfc.getSelectedFile();
                    fileNameLabel.setText(selectedFile.getName());
                    if (comboBox.getSelectedItem() != null) {
                        runButton.setEnabled(true);
                    }
                    processingLabel.setText("");
                }
            }
        });

        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HostPowerModeSelectionPolicyAgent.getTimeList().clear();
                HostPowerModeSelectionPolicyAgent.getSlaViolationTimeList().clear();
                HostPowerModeSelectionPolicyAgent.getPowerConsumptionList().clear();
                HostPowerModeSelectionPolicyAgent.getMigrationCountList().clear();
                try {
                    ParseConfig.getData(selectedFile.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Config file not found.\n" + ex.getMessage() + "\n" + getStackTrace(ex.getStackTrace()),
                            "Config file not found", JOptionPane.ERROR_MESSAGE);
                    processingLabel.setText("");
                    return;
                } catch (ParseException ex) {
                    JOptionPane.showMessageDialog(frame, "Catching exception while parsing a config file.\n" + ex.getMessage() + "\n" + getStackTrace(ex.getStackTrace()),
                            "Parse exception", JOptionPane.ERROR_MESSAGE);
                    processingLabel.setText("");
                    return;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Catching exception:\n" + ex.getMessage() + "\n" + getStackTrace(ex.getStackTrace()),
                            "Exception", JOptionPane.ERROR_MESSAGE);
                    processingLabel.setText("");
                    return;
                }
                try {
                    if (comboBox.getSelectedItem() == "Q-learning agent") {
                        new Runner(ParseConfig.inputFolder, ParseConfig.outputFolder, ParseConfig.experimentName, "qla");
                        plotCharts("Q-learning agent");
                    }
                    if (comboBox.getSelectedItem() == "Non power aware") {
                        Runner.nonPowerAwareModelling(ParseConfig.inputFolder, ParseConfig.outputFolder, ParseConfig.experimentName,"npa");
                        plotCharts("Non power aware");
                    }
                    if (comboBox.getSelectedItem() != null) {
                        processingLabel.setText("Done!");
                        frame.toFront();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Simulation terminated! Catching exception:\n" + ex.getMessage() + "\n" + getStackTrace(ex.getStackTrace()),
                            "Exception", JOptionPane.ERROR_MESSAGE);
                    CloudSim.terminateSimulation();
                    processingLabel.setText("");
                }
            }
        });

        runButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (comboBox.getSelectedItem() != null) {
                    processingLabel.setText("Processing...");
                }
            }
        });

        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedFile != null) {
                    runButton.setEnabled(true);
                }
                if (comboBox.getSelectedItem() == null) {
                    runButton.setEnabled(false);
                }
            }
        });
    }

    private XYDataset createDataset(List<Double> timeList, List<Double> list, String key) {
        final XYSeries series = new XYSeries(key);
        for (int i = 0; i < timeList.size(); i++) {
            series.add(timeList.get(i), list.get(i));
        }
        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        return dataset;
    }

    private void plotCharts(String chartName) {
        List<Double> timeList = HostPowerModeSelectionPolicyAgent.getTimeList();

        datasetSlaIndex++;
        slaPlot.setDataset(datasetSlaIndex, createDataset(timeList, HostPowerModeSelectionPolicyAgent.getSlaViolationTimeList(), chartName));
        slaPlot.setRenderer(datasetSlaIndex, new StandardXYItemRenderer());

        datasetPowerIndex++;
        powerPlot.setDataset(datasetPowerIndex, createDataset(timeList, HostPowerModeSelectionPolicyAgent.getPowerConsumptionList(), chartName));
        powerPlot.setRenderer(datasetPowerIndex, new StandardXYItemRenderer());

        datasetMigrationIndex++;
        migrationPlot.setDataset(datasetMigrationIndex, createDataset(timeList, HostPowerModeSelectionPolicyAgent.getMigrationCountList(), chartName));
        migrationPlot.setRenderer(datasetMigrationIndex, new StandardXYItemRenderer());
    }

    private void setUpPanels() {
        mainPanel.setLayout(null);
        slaPanel.setBounds(170,10,1050,290);
        slaPanel.setLayout(new BorderLayout());
        powerPanel.setBounds(170,320,1050,290);
        powerPanel.setLayout(new BorderLayout());
        migrationPanel.setBounds(170,630,1050,290);
        migrationPanel.setLayout(new BorderLayout());
    }

    private void setUpButtons() {
        selectConfigButton.setBounds(10,10,150,30);
        runButton.setBounds(10,110,150,30);
        runButton.setEnabled(false);
    }

    private void setUpLabels() {
        fileNameLabel.setBounds(10,40,150, 30);
        processingLabel.setBounds(10,140,150, 30);
    }

    private void setUpComboBox() {
        comboBox.setBounds(10,70,150,30);
        comboBox.addItem(null);
        comboBox.addItem("Q-learning agent");
        comboBox.addItem("Non power aware");
        comboBox.addItem("Dvfs");
        comboBox.addItem("Iqr Mc");
        comboBox.addItem("Iqr Mmt");
        comboBox.addItem("Iqr Mu");
        comboBox.addItem("Iqr Rs");
        comboBox.addItem("Lr Mc");
        comboBox.addItem("Lr Mmt");
        comboBox.addItem("Lr Mu");
        comboBox.addItem("Lr Rs");
        comboBox.addItem("Lrr Mc");
        comboBox.addItem("Lrr Mmt");
        comboBox.addItem("Lrr Mu");
        comboBox.addItem("Lrr Rs");
        comboBox.addItem("Mad Mc");
        comboBox.addItem("Mad Mmt");
        comboBox.addItem("Mad Mu");
        comboBox.addItem("Mad Rs");
        comboBox.addItem("Thr Mc");
        comboBox.addItem("Thr Mmt");
        comboBox.addItem("Thr Mu");
        comboBox.addItem("Thr Rs");
    }

    private String getStackTrace(StackTraceElement[] ste) {
        String errMsg = null;
        for (StackTraceElement s : ste) {
            errMsg += s;
            errMsg += "\n\t";
        }
        return errMsg;
    }

    public static void main(String[] args) {
        new Main();
    }
}
