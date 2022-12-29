/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cruldra.pdmviewer;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;

import kotlin.text.StringsKt;
import org.cruldra.pdmviewer.utils.*;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * @author ice
 */
public class PDMViewerApplication extends javax.swing.JFrame {

    /**
     * Creates new form PDMViewerApplication
     */
    private void showPdmObjectExplorerContextMenu(MouseEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) pdmObjectExplorer
                .getLastSelectedPathComponent();

        if (e.isPopupTrigger()) {
            selectNodeAtMousePosition(e);
            if (node.isLeaf()) {
                pdmObjectExplorerContextMenu.show(e.getComponent(), e.getX(), e.getY());
            }

        }
    }

    private void selectNodeAtMousePosition(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        TreePath path = pdmObjectExplorer.getPathForLocation(x, y);
        if (path != null) {
            pdmObjectExplorer.setSelectionPath(path);
        }
    }

    private void pdmObjectExplorerMousePressed(MouseEvent e) {
        showPdmObjectExplorerContextMenu(e);

    }

    private void pdmObjectExplorerMouseReleased(MouseEvent e) {
        showPdmObjectExplorerContextMenu(e);
        selectNodeAtMousePosition(e);
    }

    private void generateExistByPrimaryKeyBlockMenu(ActionEvent e) {


        DefaultMutableTreeNode node = (DefaultMutableTreeNode) pdmObjectExplorer
                .getLastSelectedPathComponent();

        if (node == null)
            return;

        Object object = node.getUserObject();
        if (node.isLeaf()) {
            PDMTable pdmt = (PDMTable) object;
            String pkCode = pdmt.getPrimaryKey().getColumns().get(0).getCode();

            String mybatisSelectStatement = " <select id=\"existByPrimaryKey\" resultType=\"java.lang.Boolean\">\n" +
                    "\n" +
                    "    select exists(select " + pkCode + " from " + pdmt.getCode() + " where  " + pkCode + "   = #{" + pkCode + "}  )\n" +
                    "\n" +
                    "  </select>";

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(mybatisSelectStatement);
            clipboard.setContents(selection, null);

        }
    }

    /**
     * 在pdm对象资源管理器上选中具体表时在右边表格中显示详细的列的信息
     *
     * @param e
     */
    private void pdmObjectExplorerValueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) pdmObjectExplorer
                .getLastSelectedPathComponent();

        if (node == null)
            return;

        Object object = node.getUserObject();
        if (node.isLeaf()) {
            PDMTable pdmt = (PDMTable) object;
            ArrayList<PDMColumn> cols = pdmt.getColumns();
            String[] columnNames = {"名称", "代码", "数据类型", "备注"};
            cols.trimToSize();
            Object[][] data = new Object[cols.size()][columnNames.length];

            int i = 0;
            for (PDMColumn col : cols) {
                data[i][0] = col.getName();
                data[i][1] = col.getCode();
                data[i][2] = col.getDataType();
                data[i][3] = col.getComment();
                i++;
            }
            TableModel dataMode = new DefaultTableModel(data, columnNames);
            pdmTableTable.setModel(dataMode);
        }
    }

    private String formatXML(Element element) {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setIndent("  ");
        StringWriter writer = new StringWriter();
        XMLWriter xmlWriter = new XMLWriter(writer, format);
        try {
            xmlWriter.write(element);
            xmlWriter.flush();
            xmlWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return writer.toString();
    }

    private void generateInsertSelectiveBlockMenu(ActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) pdmObjectExplorer
                .getLastSelectedPathComponent();

        if (node == null)
            return;

        Object object = node.getUserObject();
        if (node.isLeaf()) {
            PDMTable pdmt = (PDMTable) object;

            Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("insert");
            root.addAttribute("id", "insertSelective");
            root.addAttribute("parameterType", "");
            root.addText("\ninsert into " + pdmt.getCode());
            Element trim1 = root.addElement("trim");
            trim1.addAttribute("prefix", "(");
            trim1.addAttribute("suffix", ")");
            trim1.addAttribute("suffixOverrides", ",");
            pdmt.getColumns().forEach(pdmColumn -> {
                Element if1 = trim1.addElement("if");
                if1.addAttribute("test", pdmColumn.getCode() + " != null");
                if1.addText(pdmColumn.getCode() + ",");
            });

            Element trim2 = root.addElement("trim");
            trim2.addAttribute("prefix", "values (");
            trim2.addAttribute("suffix", ")");
            trim2.addAttribute("suffixOverrides", ",");
            pdmt.getColumns().forEach(pdmColumn -> {
                Element if2 = trim2.addElement("if");
                if2.addAttribute("test", pdmColumn.getCode() + " != null");
                if2.addText("#{" + pdmColumn.getCode() + ",jdbcType=" + StringsKt.substringBefore(pdmColumn.getDataType(), "(", pdmColumn.getDataType()) + "},");
            });

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(formatXML(root));
            clipboard.setContents(selection, null);
        }
    }

    private void generateUpdateByPrimaryKeySelectiveBlockMenu(ActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) pdmObjectExplorer
                .getLastSelectedPathComponent();

        if (node == null)
            return;

        Object object = node.getUserObject();
        if (node.isLeaf()) {
            PDMTable pdmt = (PDMTable) object;
            PDMColumn pkColumn = pdmt.getPrimaryKey().getColumns().get(0);
            Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("update");
            root.addAttribute("id", "updateByPrimaryKeySelective");
            root.addAttribute("parameterType", "");
            root.addText("\nupdate " + pdmt.getCode());
            Element set = root.addElement("set");
            pdmt.getColumns().forEach(pdmColumn -> {
                Element if1 = set.addElement("if");
                if1.addAttribute("test", pdmColumn.getCode() + " != null");
                if1.addText(pdmColumn.getCode() + " = #{" + StrUtilsKt.toCamelCase(pdmColumn.getCode()) + ",jdbcType=" + StringsKt.substringBefore(pdmColumn.getDataType(), "(", pdmColumn.getDataType()) + "},");
            });
            root.addText("\nwhere "+pkColumn.getCode()+" = #{"+pkColumn.getCode()+",jdbcType="+ StringsKt.substringBefore(pkColumn.getDataType() ,"(" ,pkColumn.getDataType()  ) +"}");
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(formatXML(root));
            clipboard.setContents(selection, null);
        }
    }

    public PDMViewerApplication() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    // Generated using JFormDesigner Evaluation license - cruldra
    private void initComponents() {
        mainMenu = new JMenuBar();
        fileMenu = new JMenu();
        jMenuItem1 = new JMenuItem();
        jMenuItem2 = new JMenuItem();
        aboutMenu = new JMenu();
        jScrollPane2 = new JScrollPane();
        pdmObjectExplorer = new JTree();
        jScrollPane3 = new JScrollPane();
        pdmTableTable = new JTable();
        pdmObjectExplorerContextMenu = new JPopupMenu();
        menu1 = new JMenu();
        generateExistByPrimaryKeyBlockMenu = new JMenuItem();
        generateInsertSelectiveBlockMenu = new JMenuItem();
        generateUpdateByPrimaryKeySelectiveBlockMenu = new JMenuItem();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(6);
        Container contentPane = getContentPane();

        //======== mainMenu ========
        {

            //======== fileMenu ========
            {
                fileMenu.setText("\u6587\u4ef6");

                //---- jMenuItem1 ----
                jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
                jMenuItem1.setText("\u6253\u5f00");
                jMenuItem1.addActionListener(e -> jMenuItem1ActionPerformed(e));
                fileMenu.add(jMenuItem1);

                //---- jMenuItem2 ----
                jMenuItem2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
                jMenuItem2.setText("\u9000\u51fa");
                jMenuItem2.addActionListener(e -> jMenuItem2ActionPerformed(e));
                fileMenu.add(jMenuItem2);
            }
            mainMenu.add(fileMenu);

            //======== aboutMenu ========
            {
                aboutMenu.setText("\u5173\u4e8e");
            }
            mainMenu.add(aboutMenu);
        }
        setJMenuBar(mainMenu);

        //======== jScrollPane2 ========
        {

            //---- pdmObjectExplorer ----
            pdmObjectExplorer.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    pdmObjectExplorerMousePressed(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pdmObjectExplorerMouseReleased(e);
                }
            });
            pdmObjectExplorer.addTreeSelectionListener(e -> pdmObjectExplorerValueChanged(e));
            jScrollPane2.setViewportView(pdmObjectExplorer);
        }

        //======== jScrollPane3 ========
        {

            //---- pdmTableTable ----
            pdmTableTable.setModel(new DefaultTableModel());
            jScrollPane3.setViewportView(pdmTableTable);
        }

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
                                .addContainerGap())
        );
        contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup()
                        .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 457, Short.MAX_VALUE)
                        .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, 457, Short.MAX_VALUE)
        );
        pack();
        setLocationRelativeTo(getOwner());

        //======== pdmObjectExplorerContextMenu ========
        {

            //======== menu1 ========
            {
                menu1.setText("mybatis");

                //---- generateExistByPrimaryKeyBlockMenu ----
                generateExistByPrimaryKeyBlockMenu.setText("\u751f\u6210existByPrimaryKey\u5757");
                generateExistByPrimaryKeyBlockMenu.addActionListener(e -> generateExistByPrimaryKeyBlockMenu(e));
                menu1.add(generateExistByPrimaryKeyBlockMenu);

                //---- generateInsertSelectiveBlockMenu ----
                generateInsertSelectiveBlockMenu.setText("\u751f\u6210insertSelective\u5757");
                generateInsertSelectiveBlockMenu.addActionListener(e -> generateInsertSelectiveBlockMenu(e));
                menu1.add(generateInsertSelectiveBlockMenu);

                //---- generateUpdateByPrimaryKeySelectiveBlockMenu ----
                generateUpdateByPrimaryKeySelectiveBlockMenu.setText("\u751f\u6210updateByPrimaryKeySelective\u5757");
                generateUpdateByPrimaryKeySelectiveBlockMenu.addActionListener(e -> generateUpdateByPrimaryKeySelectiveBlockMenu(e));
                menu1.add(generateUpdateByPrimaryKeySelectiveBlockMenu);
            }
            pdmObjectExplorerContextMenu.add(menu1);
        }
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PowerDesigner PDM File", "pdm");
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setFileFilter(filter);
        File file = null;
        if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(this)) {
            file = jfc.getSelectedFile();
            try {
                PDM p = new Parser().pdmParser(file.getPath());
                DefaultMutableTreeNode top = new DefaultMutableTreeNode("表");


                for (PDMTable t : p.getTables()) {
                    System.out.println("table-->" + t.getName() + ", code-->" + t.getCode());
                    DefaultMutableTreeNode child = new DefaultMutableTreeNode(t);
                    top.add(child);
                }
                pdmObjectExplorer.setModel(new DefaultTreeModel(top));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PDMViewerApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PDMViewerApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PDMViewerApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PDMViewerApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new PDMViewerApplication().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - cruldra
    private JMenuBar mainMenu;
    private JMenu fileMenu;
    private JMenuItem jMenuItem1;
    private JMenuItem jMenuItem2;
    private JMenu aboutMenu;
    private JScrollPane jScrollPane2;
    private JTree pdmObjectExplorer;
    private JScrollPane jScrollPane3;
    private JTable pdmTableTable;
    private JPopupMenu pdmObjectExplorerContextMenu;
    private JMenu menu1;
    private JMenuItem generateExistByPrimaryKeyBlockMenu;
    private JMenuItem generateInsertSelectiveBlockMenu;
    private JMenuItem generateUpdateByPrimaryKeySelectiveBlockMenu;
    // End of variables declaration//GEN-END:variables
}
