import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class Form1 {
    private JPanel panel1;
    private JButton button1;
    private JButton button2;
    private JLabel label1;
    private File selectedFile = null;

    public Form1() {
        JFrame frame = new JFrame("Diss");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(panel1);
        frame.setVisible(true);
        frame.setSize(1250, 750);
        frame.setLocationRelativeTo(null);

        panel1.setLayout(null);
        button1.setBounds(10,10,150,30);
        button2.setBounds(10,50,150,30);
        label1.setBounds(170,10,200, 30);

        button1.addActionListener(new ActionListener() {
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
                }
            }
        });

        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedFile == null) {
                    JOptionPane.showMessageDialog(null, "Select a config file!");
                } else {
                    ParseConfig.getData(selectedFile.getAbsolutePath());
                    new Runner(ParseConfig.inputFolder, ParseConfig.outputFolder, ParseConfig.experimentName);
                }
            }
        });

        button2.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                label1.setText("Output to log file. Please, wait...");
            }
        });
    }

    public static void main(String[] args) {
        new Form1();
    }
}
