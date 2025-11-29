package com.duckblade.osrs.sailing;

import com.duckblade.osrs.sailing.features.barracudatrials.GwenithGlideSplits;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import com.duckblade.osrs.sailing.features.barracudatrials.GwenithGlideSplits.RunRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SailingPanel extends PluginPanel
{
    private final JLabel pbLabel = new JLabel("PB: N/A");

    // Container for all runs (one RunPanel per run)
    private final JPanel runView = new JPanel();
    private final JPanel noRunsPanel = new JPanel(new BorderLayout());

    public SailingPanel()
    {
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        // Top: PB label
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(0, 0, 6, 0));
        pbLabel.setForeground(Color.WHITE);
        pbLabel.setFont(FontManager.getRunescapeSmallFont());
        top.add(pbLabel, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        // Center: run list (no local scrollpane; RuneLite wraps PluginPanel itself)
        runView.setLayout(new BoxLayout(runView, BoxLayout.Y_AXIS));
        runView.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // "No runs" placeholder
        JLabel noRunsLabel = new JLabel("<html><center>No Gwenith Glide runs logged yet.<br/>Complete a run to see it here.</center></html>");
        noRunsLabel.setForeground(Color.GRAY);
        noRunsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        noRunsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        noRunsPanel.add(noRunsLabel, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        centerPanel.add(runView, BorderLayout.NORTH); // content grows vertically

        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Called by the plugin whenever history or PB changes.
     */
    public void updateData(List<RunRecord> runs, int personalBestTicks)
    {
        // PB label
        if (personalBestTicks > 0)
        {
            pbLabel.setText("PB: " + personalBestTicks + " ticks");
        }
        else
        {
            pbLabel.setText("PB: N/A");
        }

        runView.removeAll();

        if (runs == null || runs.isEmpty())
        {
            runView.add(noRunsPanel);
        }
        else
        {
            // reverse display: newest KC at top
            for (int i = 0; i < runs.size(); i++)
            {
                RunRecord r = runs.get(i);
                RunPanel panel = new RunPanel(r);
                runView.add(panel);
                runView.add(Box.createRigidArea(new Dimension(0, 4)));
            }
        }

        runView.revalidate();
        runView.repaint();
    }


    /**
     * One expandable panel per run (KC).
     */
    private static class RunPanel extends JPanel
    {
        private final RunRecord record;
        private final JPanel header = new JPanel(new BorderLayout());
        private final JPanel splitsPanel = new JPanel();
        private boolean expanded = false;

        RunPanel(RunRecord record)
        {
            this.record = record;

            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(2, 2, 2, 2));
            setBackground(ColorScheme.DARKER_GRAY_COLOR);

            buildHeader();
            buildSplits();

            add(header, BorderLayout.NORTH);
        }

        private void buildHeader()
        {
            header.setBorder(new EmptyBorder(4, 4, 4, 4));
            header.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            String kcText = (record.kc >= 0 ? String.valueOf(record.kc) : "?");
            JLabel title = new JLabel("KC " + kcText + " - " + record.totalTicks + " ticks");
            title.setForeground(Color.WHITE);
            title.setFont(FontManager.getRunescapeSmallFont());

            JLabel expandLabel = new JLabel(">");
            expandLabel.setForeground(Color.LIGHT_GRAY);
            expandLabel.setFont(FontManager.getRunescapeSmallFont());
            expandLabel.setBorder(new EmptyBorder(0, 0, 0, 4));

            header.add(expandLabel, BorderLayout.WEST);
            header.add(title, BorderLayout.CENTER);

            // Popup menu for right-click
            JPopupMenu popup = new JPopupMenu();
            JMenuItem copyItem = new JMenuItem("Copy time + splits");
            copyItem.addActionListener(e -> copyRunToClipboard());
            popup.add(copyItem);

            MouseAdapter mouse = new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    if (SwingUtilities.isRightMouseButton(e))
                    {
                        popup.show(e.getComponent(), e.getX(), e.getY());
                        return;
                    }

                    if (SwingUtilities.isLeftMouseButton(e))
                    {
                        toggleExpanded(expandLabel);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e)
                {
                    header.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    setCursor(Cursor.getDefaultCursor());
                }
            };

            header.addMouseListener(mouse);
            title.addMouseListener(mouse);
            expandLabel.addMouseListener(mouse);
        }

        private void buildSplits()
        {
            splitsPanel.setLayout(new BoxLayout(splitsPanel, BoxLayout.Y_AXIS));
            splitsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
            splitsPanel.setBorder(new EmptyBorder(0, 16, 4, 4));

            List<String> splits = record.getSplits();
            if (splits != null)
            {
                for (String s : splits)
                {
                    JLabel splitLabel = new JLabel(s);
                    splitLabel.setForeground(Color.LIGHT_GRAY);
                    splitLabel.setFont(FontManager.getRunescapeSmallFont());
                    splitsPanel.add(splitLabel);
                }
            }
        }

        private void toggleExpanded(JLabel expandLabel)
        {
            expanded = !expanded;

            if (expanded)
            {
                expandLabel.setText("v");
                add(splitsPanel, BorderLayout.CENTER);
            }
            else
            {
                expandLabel.setText(">");
                remove(splitsPanel);
            }

            revalidate();
            repaint();
        }

        private void copyRunToClipboard()
        {
            String kcText = (record.kc >= 0 ? String.valueOf(record.kc) : "?");
            StringBuilder sb = new StringBuilder();

            sb.append("KC ")
                    .append(kcText)
                    .append(" - total: ")
                    .append(record.totalTicks)
                    .append(" ticks\n");

            List<String> splits = record.getSplits();
            if (splits != null)
            {
                for (String s : splits)
                {
                    sb.append(s).append('\n');
                }
            }

            StringSelection selection = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        }
    }
}
