package exp;

import cloudsim.core.CloudSim;
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
import exp.policy.HostPowerModeSelectionPolicyAgent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private JPanel mainPanel;
    private JPanel slaPanel;
    private JPanel powerPanel;
    private JPanel migrationPanel;

    private JButton selectConfigButton;
    private JButton runButton;
    private JButton plotSavedButton;

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
        JFrame frame = new JFrame("");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1250, 650);
        frame.setLocationRelativeTo(null);

        setUpPanels();
        setUpButtons();
        setUpLabels();
        setUpComboBox();
        setUpCharts();

        UIManager.put("FileChooser.lookInLabelText", "Директорія:");
        UIManager.put("FileChooser.upFolderToolTipText", "На рівень вище");
        UIManager.put("FileChooser.newFolderToolTipText", "Створити нову директорію");
        UIManager.put("FileChooser.listViewButtonToolTipText", "Список");
        UIManager.put("FileChooser.detailsViewButtonToolTipText", "Деталі");
        UIManager.put("FileChooser.fileNameLabelText", "Ім'я файлу:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Тип файлу:");
        UIManager.put("FileChooser.openButtonText", "Відкрити");
        UIManager.put("FileChooser.openButtonToolTipText", "Відкрити вибраний файл");
        UIManager.put("FileChooser.cancelButtonText", "Відмінити");
        UIManager.put("FileChooser.cancelButtonToolTipText", "Відмінити вибір файлу");

        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) && ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0)
                        && ((e.getModifiers() & KeyEvent.ALT_MASK) != 0) && e.getKeyCode() == KeyEvent.VK_1) {
                    if (plotSavedButton.isVisible()) {
                        plotSavedButton.setVisible(false);
                    } else {
                        plotSavedButton.setVisible(true);
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });

        JScrollPane scrollBar = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        frame.add(scrollBar);
        frame.setFocusable(true);
        frame.setVisible(true);

        selectConfigButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                jfc.setDialogTitle("Виберіть конфігураційний файл");
                jfc.setCurrentDirectory(new File("."));
                jfc.setAcceptAllFileFilterUsed(false);
                FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON файли", "json");
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
                    JOptionPane.showMessageDialog(frame, "Конфігураційний файл не знайдено.\n" + getExceptionMessage(ex),
                            "Конфігураційний файл не знайдено", JOptionPane.ERROR_MESSAGE);
                    processingLabel.setText("");
                    return;
                } catch (ParseException ex) {
                    JOptionPane.showMessageDialog(frame, "Помилка під час читання конфігураційного файлу.\n" + getExceptionMessage(ex),
                            "Помилка під час читання конфігураційного файлу", JOptionPane.ERROR_MESSAGE);
                    processingLabel.setText("");
                    return;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Помилка:\n" + getExceptionMessage(ex),
                            "Помилка", JOptionPane.ERROR_MESSAGE);
                    processingLabel.setText("");
                    return;
                }
                try {
                    String policyName = comboBox.getSelectedItem().toString();
                    new Runner(ParseConfig.inputFolder, ParseConfig.experimentName, policyName);
                    if (policyName.equals("Qla")) {
                        policyName += " " + ParseConfig.learningRate + " " + ParseConfig.discountFactor + " " + ParseConfig.cofImportanceSla + " " + ParseConfig.cofImportancePower;
                    }
                    List<Double> timeList = HostPowerModeSelectionPolicyAgent.getTimeList();
                    List<Double> slaList = HostPowerModeSelectionPolicyAgent.getSlaViolationTimeList();
                    List<Double> powerList = HostPowerModeSelectionPolicyAgent.getPowerConsumptionList();
                    List<Double> migrationCountList = HostPowerModeSelectionPolicyAgent.getMigrationCountList();
                    plotCharts(policyName, timeList, slaList, powerList, migrationCountList);
                    processingLabel.setText("Готово!");
                    frame.toFront();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Моделювання зупинено! Помилка:\n" + getExceptionMessage(ex),
                            "Помилка", JOptionPane.ERROR_MESSAGE);
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
                    processingLabel.setText("Моделювання...");
                }
            }
        });

        plotSavedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetCharts();
                try {
                    plotSavedDatasets();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Файл не знайдено.\n" + getExceptionMessage(ex),
                            "Файл не знайдено", JOptionPane.ERROR_MESSAGE);
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

    private void plotSavedDatasets() throws IOException {
        File metricsFolder = new File("output/metrics");
        File[] files = metricsFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                String metricFile = file.getAbsolutePath();
                String chartName = file.getName().split("_")[0];
                List<Double> timeList = new ArrayList<>();
                List<Double> slaList = new ArrayList<>();
                List<Double> powerList = new ArrayList<>();
                List<Double> migrationList = new ArrayList<>();
                BufferedReader input = new BufferedReader(new FileReader(metricFile));
                String line;
                while ((line = input.readLine()) != null) {
                    String[] elements = line.split(";\t");
                    timeList.add(Double.valueOf(elements[0]));
                    slaList.add(Double.valueOf(elements[1]));
                    powerList.add(Double.valueOf(elements[2]));
                    migrationList.add(Double.valueOf(elements[3]));
                }
                plotCharts(chartName, timeList, slaList, powerList, migrationList);
                input.close();
            }
        }
    }

    private void resetCharts() {
        int indexCap = datasetSlaIndex;
        for (int i = indexCap; i >= 0; i--) {
            slaPlot.setDataset(i, null);
            slaPlot.setRenderer(i, null);
            powerPlot.setDataset(i, null);
            powerPlot.setRenderer(i, null);
            migrationPlot.setDataset(i, null);
            migrationPlot.setRenderer(i, null);
            datasetSlaIndex --;
            datasetPowerIndex --;
            datasetMigrationIndex --;
        }
    }

    private void plotCharts(String chartName, List<Double> timeList, List<Double> slaList, List<Double> powerList, List<Double> migrationCountList) {
        datasetSlaIndex++;
        slaPlot.setDataset(datasetSlaIndex, createDataset(timeList, slaList, chartName));
        slaPlot.setRenderer(datasetSlaIndex, new StandardXYItemRenderer());
        slaPlot.getRenderer().setSeriesStroke(datasetSlaIndex, new BasicStroke(1));
        datasetPowerIndex++;
        powerPlot.setDataset(datasetPowerIndex, createDataset(timeList, powerList, chartName));
        powerPlot.setRenderer(datasetPowerIndex, new StandardXYItemRenderer());
        powerPlot.getRenderer().setSeriesStroke(datasetPowerIndex, new BasicStroke(1));
        datasetMigrationIndex++;
        migrationPlot.setDataset(datasetMigrationIndex, createDataset(timeList, migrationCountList, chartName));
        migrationPlot.setRenderer(datasetMigrationIndex, new StandardXYItemRenderer());
        migrationPlot.getRenderer().setSeriesStroke(datasetMigrationIndex, new BasicStroke(1));
    }

    private void setUpPanels() {
        mainPanel.setPreferredSize(new Dimension(1240,940));
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
        selectConfigButton.setFocusable(false);
        runButton.setBounds(10,110,150,30);
        runButton.setFocusable(false);
        runButton.setEnabled(false);
        plotSavedButton.setBounds(10, 170, 150, 30);
        plotSavedButton.setFocusable(false);
        plotSavedButton.setVisible(false);
    }

    private void setUpLabels() {
        fileNameLabel.setBounds(10,40,150, 30);
        processingLabel.setBounds(10,140,150, 30);
    }

    private void setUpComboBox() {
        comboBox.setBounds(10,70,150,30);
        comboBox.addItem(null);
        comboBox.addItem("Qla");
        //comboBox.addItem("Npa");
        //comboBox.addItem("Dvfs");
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
        comboBox.setFocusable(false);
    }

    private void setUpCharts() {
        final JFreeChart slaChart = ChartFactory.createXYLineChart("Час порушення вимог SLA","Час, секунди", "Час поруш. вимог SLA, сек.",
                null, PlotOrientation.VERTICAL, true, true, false);
        slaPlot = slaChart.getXYPlot();
        slaPlot.setBackgroundPaint(Color.WHITE);
        slaPlot.setDomainGridlinePaint(Color.BLACK);
        slaPlot.setRangeGridlinePaint(Color.BLACK);
        final ValueAxis axis1 = slaPlot.getDomainAxis();
        axis1.setAutoRange(true);
        final NumberAxis rangeAxis1 = new NumberAxis("Range Axis 1");
        rangeAxis1.setAutoRangeIncludesZero(false);
        final ChartPanel slaChartPanel = new ChartPanel(slaChart);
        slaPanel.add(slaChartPanel);
        slaChartPanel.setDomainZoomable(true);

        final JFreeChart powerChart = ChartFactory.createXYLineChart("Споживання електроенергії","Час, секунди", "Спожив. ел. енергії, кВт×год",
                null, PlotOrientation.VERTICAL, true, true, false);
        powerPlot = powerChart.getXYPlot();
        powerPlot.setBackgroundPaint(Color.WHITE);
        powerPlot.setDomainGridlinePaint(Color.BLACK);
        powerPlot.setRangeGridlinePaint(Color.BLACK);
        final ValueAxis axis2 = powerPlot.getDomainAxis();
        axis2.setAutoRange(true);
        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);
        final ChartPanel powerChartPanel = new ChartPanel(powerChart);
        powerPanel.add(powerChartPanel);
        powerChartPanel.setDomainZoomable(true);

        final JFreeChart migrationChart = ChartFactory.createXYLineChart("Кількість міграцій ВМ","Час, секунди", "К-сть міграцій ВМ",
                null, PlotOrientation.VERTICAL, true, true, false);
        migrationPlot = migrationChart.getXYPlot();
        migrationPlot.setBackgroundPaint(Color.WHITE);
        migrationPlot.setDomainGridlinePaint(Color.BLACK);
        migrationPlot.setRangeGridlinePaint(Color.BLACK);
        final ValueAxis axis3 = migrationPlot.getDomainAxis();
        axis3.setAutoRange(true);
        final NumberAxis rangeAxis3 = new NumberAxis("Range Axis 3");
        rangeAxis3.setAutoRangeIncludesZero(false);
        final ChartPanel migrationChartPanel = new ChartPanel(migrationChart);
        migrationPanel.add(migrationChartPanel);
        migrationChartPanel.setDomainZoomable(true);
    }

    private String getExceptionMessage(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static void main(String[] args) {
        new Main();
    }
}
