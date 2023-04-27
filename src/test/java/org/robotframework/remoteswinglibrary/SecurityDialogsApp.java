package org.robotframework.remoteswinglibrary;

import javax.swing.*;


public class SecurityDialogsApp extends JFrame {
    public SecurityDialogsApp() {

        super();
        setTitle("Test");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        SecurityWarningContiune();
        SecurityWarningRun();
        SecurityWarningInstall();
        SecurityWarningWithCheckBox();

        setVisible(true);
    }

    private void SecurityWarningContiune() {
        Object[] options = {"Continue", "Cancel"};
        int n = JOptionPane.showOptionDialog(this,
                "Security Warning Dialog with continue button",
                "Security Warning",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
    }

    private void SecurityWarningRun() {
        Object[] options = {"Run", "Cancel"};
        int n = JOptionPane.showOptionDialog(this,
                "Security Warning Dialog with run button",
                "Security Information",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
    }

    private void SecurityWarningInstall(){
        Object[] options = {"Install", "Cancel"};
        int n = JOptionPane.showOptionDialog(this,
                "Security Warning Dialog with install button",
                "Install Java Extension",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
    }

    private void SecurityWarningWithCheckBox(){
        JCheckBox rememberChk = new JCheckBox("I accept the risk and want to run this application.");
        JButton moreInfoButton = new JButton("More Information");
        Object[] options = {"Run",
                "Cancel"};

        Object[] msgContent = {"msg", moreInfoButton, rememberChk};
        int n = JOptionPane.showOptionDialog(this,
                msgContent,
                "Security Warning",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
    }

    public static void main(String[] a) {
        SecurityDialogsApp myapp2 = new SecurityDialogsApp();
    }
}
