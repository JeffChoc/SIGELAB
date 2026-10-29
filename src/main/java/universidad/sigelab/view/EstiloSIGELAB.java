package universidad.sigelab.view;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Estilo visual común de SIGELAB. No modifica modelos, eventos ni reglas de negocio.
 * Puede usarse con ventanas construidas desde archivos .form de IntelliJ IDEA.
 */
public final class EstiloSIGELAB {
    public static final Color AZUL_MARINO = new Color(23, 54, 93);
    public static final Color AZUL = new Color(42, 91, 145);
    public static final Color VERDE = new Color(40, 122, 82);
    public static final Color ROJO = new Color(176, 32, 32);
    public static final Color FONDO = new Color(242, 245, 249);
    public static final Color BLANCO = Color.WHITE;
    public static final Color TEXTO = new Color(35, 42, 52);
    public static final Color BORDE = new Color(213, 220, 230);
    public static final Color FILA_ALTERNA = new Color(247, 249, 252);
    private static final Font FUENTE_BASE = new Font("Segoe UI", Font.PLAIN, 13);

    private EstiloSIGELAB() { }

    public static void aplicar(JFrame ventana) {
        if (ventana == null) return;
        ventana.getContentPane().setBackground(FONDO);
        ventana.getContentPane().setFont(FUENTE_BASE);
        aplicarRecursivo(ventana.getContentPane());
        JMenuBar barra = ventana.getJMenuBar();
        if (barra != null) {
            barra.setBackground(BLANCO);
            barra.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE));
            barra.setFont(FUENTE_BASE);
            for (MenuElement elemento : barra.getSubElements()) estilizarMenu(elemento);
        }
    }

    private static void aplicarRecursivo(Component componente) {
        if (componente == null) return;
        if (componente instanceof JTable) estilizarTabla((JTable) componente);
        else if (componente instanceof JButton) estilizarBoton((JButton) componente);
        else if (componente instanceof JTextField) estilizarCampo((JTextField) componente);
        else if (componente instanceof JPasswordField) estilizarCampo((JPasswordField) componente);
        else if (componente instanceof JComboBox) estilizarCombo((JComboBox<?>) componente);
        else if (componente instanceof JSpinner) {
            componente.setFont(FUENTE_BASE);
            componente.setForeground(TEXTO);
        } else if (componente instanceof JLabel) {
            componente.setFont(FUENTE_BASE);
            Color colorEtiquetaUI = UIManager.getColor("Label.foreground");
            if (((JLabel) componente).getForeground() == null ||
                    (colorEtiquetaUI != null && colorEtiquetaUI.equals(((JLabel) componente).getForeground()))) {
                componente.setForeground(TEXTO);
            }
        } else if (componente instanceof JTabbedPane) {
            componente.setFont(FUENTE_BASE);
            componente.setForeground(TEXTO);
        } else if (componente instanceof JScrollPane) {
            componente.setBackground(BLANCO);
            ((JScrollPane) componente).getViewport().setBackground(BLANCO);
            ((JScrollPane) componente).setBorder(BorderFactory.createLineBorder(BORDE));
        }

        if (componente instanceof Container) {
            for (Component hijo : ((Container) componente).getComponents()) aplicarRecursivo(hijo);
        }
    }

    private static void estilizarTabla(JTable tabla) {
        tabla.setFont(FUENTE_BASE);
        tabla.setForeground(TEXTO);
        tabla.setBackground(BLANCO);
        tabla.setSelectionBackground(new Color(220, 232, 247));
        tabla.setSelectionForeground(TEXTO);
        tabla.setGridColor(new Color(231, 235, 241));
        tabla.setRowHeight(Math.max(tabla.getRowHeight(), 28));
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setFillsViewportHeight(true);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer celdas = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor,
                    boolean seleccionada, boolean enfocada, int fila, int columna) {
                super.getTableCellRendererComponent(t, valor, seleccionada, enfocada, fila, columna);
                setForeground(TEXTO);
                setBackground(seleccionada ? new Color(220, 232, 247)
                        : (fila % 2 == 0 ? BLANCO : FILA_ALTERNA));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setFont(FUENTE_BASE);
                return this;
            }
        };
        for (int i = 0; i < tabla.getColumnCount(); i++) tabla.getColumnModel().getColumn(i).setCellRenderer(celdas);

        JTableHeader encabezado = tabla.getTableHeader();
        if (encabezado != null) {
            encabezado.setFont(FUENTE_BASE.deriveFont(Font.BOLD));
            encabezado.setForeground(BLANCO);
            encabezado.setBackground(AZUL_MARINO);
            encabezado.setPreferredSize(new Dimension(encabezado.getPreferredSize().width, 34));
            encabezado.setReorderingAllowed(false);
            encabezado.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object valor,
                        boolean seleccionada, boolean enfocada, int fila, int columna) {
                    super.getTableCellRendererComponent(t, valor, seleccionada, enfocada, fila, columna);
                    setOpaque(true);
                    setBackground(AZUL_MARINO);
                    setForeground(BLANCO);
                    setFont(FUENTE_BASE.deriveFont(Font.BOLD));
                    setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                    return this;
                }
            });
        }
    }

    private static void estilizarBoton(JButton boton) {
        String texto = boton.getText() == null ? "" : boton.getText().toLowerCase();
        Color fondo = AZUL;
        if (texto.contains("guardar") || texto.contains("registrar") || texto.contains("confirmar")
                || texto.contains("activar") || texto.contains("ingresar") || texto.contains("aceptar")) {
            fondo = VERDE;
        } else if (texto.contains("eliminar") || texto.contains("desactivar") || texto.contains("cancelar")
                || texto.contains("quitar") || texto.contains("baja")) {
            fondo = ROJO;
        } else if (texto.contains("nuevo") || texto.contains("actualizar") || texto.contains("consultar")
                || texto.contains("buscar") || texto.contains("ver todo") || texto.contains("generar qr")
                || texto.contains("leer qr")) {
            fondo = AZUL_MARINO;
        }
        boton.setFont(FUENTE_BASE.deriveFont(Font.BOLD));
        boton.setForeground(BLANCO);
        boton.setBackground(fondo);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15));
        boton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        boton.setMargin(new Insets(8, 14, 8, 14));
    }

    private static void estilizarCampo(JTextField campo) {
        campo.setFont(FUENTE_BASE);
        campo.setForeground(TEXTO);
        campo.setBackground(BLANCO);
        campo.setCaretColor(AZUL_MARINO);
        campo.setBorder(campoBorde());
    }

    private static void estilizarCombo(JComboBox<?> combo) {
        combo.setFont(FUENTE_BASE);
        combo.setForeground(TEXTO);
        combo.setBackground(BLANCO);
        combo.setBorder(BorderFactory.createLineBorder(BORDE));
    }

    private static Border campoBorde() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    private static void estilizarMenu(MenuElement elemento) {
        Component componente = elemento.getComponent();
        if (componente instanceof JMenu || componente instanceof JMenuItem) {
            componente.setFont(FUENTE_BASE);
            componente.setForeground(TEXTO);
            componente.setBackground(BLANCO);
        }
        for (MenuElement hijo : elemento.getSubElements()) estilizarMenu(hijo);
    }
}
