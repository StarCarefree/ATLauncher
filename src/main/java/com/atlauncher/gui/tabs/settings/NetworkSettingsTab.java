/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.gui.tabs.settings;

import java.awt.event.ItemEvent;

import javax.swing.ImageIcon;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.data.CheckState;
import com.atlauncher.data.ProxyType;
import com.atlauncher.gui.components.JLabelWithHover;
import com.atlauncher.gui.md3.input.MD3ComboBox;
import com.atlauncher.gui.md3.input.MD3Switch;
import com.atlauncher.gui.md3.input.MD3TextField;
import com.atlauncher.listener.StatefulTextKeyAdapter;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.Utils;
import com.atlauncher.viewmodel.impl.settings.NetworkSettingsViewModel;

public class NetworkSettingsTab extends AbstractSettingsTab {
    private final NetworkSettingsViewModel viewModel;
    private JLabelWithHover proxyCheckIndicator;
    private MD3TextField proxyHost;
    private JSpinner proxyPort;
    private MD3ComboBox<ComboItem<ProxyType>> proxyType;

    public NetworkSettingsTab(NetworkSettingsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    protected void onShow() {
        // Concurrent Connection Settings
        SpinnerNumberModel concurrentConnectionsModel = new SpinnerNumberModel(App.settings.concurrentConnections, null,
            null, 1);
        concurrentConnectionsModel.setMinimum(1);
        concurrentConnectionsModel.setMaximum(100);
        concurrentConnectionsModel.addChangeListener(changeEvent ->
            viewModel.setConcurrentConnections((Integer) concurrentConnectionsModel.getValue()));
        addDisposable(viewModel.getConcurrentConnections().subscribe(concurrentConnectionsModel::setValue));
        JSpinner concurrentConnections = new JSpinner(concurrentConnectionsModel);

        addRow(GetText.tr("Concurrent Connections"),
            GetText.tr("This determines how many connections will be made when downloading files."),
            concurrentConnections);

        // Connection Timeout Settings
        SpinnerNumberModel connectionTimeoutModel = new SpinnerNumberModel(App.settings.connectionTimeout, null, null,
            1);
        connectionTimeoutModel.setMinimum(1);
        connectionTimeoutModel.setMaximum(600);
        connectionTimeoutModel.addChangeListener(changeEvent ->
            viewModel.setConnectionTimeout((Integer) connectionTimeoutModel.getValue()));
        addDisposable(viewModel.getConnectionTimeout().subscribe(connectionTimeoutModel::setValue));
        JSpinner connectionTimeout = new JSpinner(connectionTimeoutModel);

        addRow(GetText.tr("Connection Timeout"),
            GetText.tr("This determines how long connections will wait before timing out."), connectionTimeout);

        // Modrinth Api Key Settings
        MD3TextField modrinthApiKey = new MD3TextField(40);
        modrinthApiKey.putClientProperty("JTextField.showClearButton", true);
        modrinthApiKey.putClientProperty("JTextField.clearCallback", (Runnable) () -> viewModel.setModrinthAPIKey(""));
        modrinthApiKey.addKeyListener(new StatefulTextKeyAdapter(
            (e) -> viewModel.setModrinthAPIKey(modrinthApiKey.getText())
        ));
        addDisposable(viewModel.getModrinthAPIKey().subscribe(apiKey -> {
            if (!modrinthApiKey.getText().equals(apiKey)) {
                modrinthApiKey.setText(apiKey);
            }
        }));

        addWideRow(GetText.tr("Modrinth Api Key"), GetText.tr(
                "Api key to use when making requests to Modrinth. This is unecessary to set unless you want to access private data."),
            modrinthApiKey);

        // Enable Proxy
        MD3Switch enableProxy = new MD3Switch();
        enableProxy.addItemListener(itemEvent ->
            viewModel.setEnableProxy(itemEvent.getStateChange() == ItemEvent.SELECTED));

        proxyCheckIndicator = new JLabelWithHover("", null, null);
        resetProxyCheck();
        addDisposable(viewModel.getProxyCheckState().subscribe(this::setProxyCheckState));

        addRow(GetText.tr("Enable Proxy"),
            GetText.tr("If you use a proxy to connect to the internet you can enable it here."),
            group(proxyCheckIndicator, enableProxy));

        // Proxy Host Settings
        proxyHost = new MD3TextField(20);
        proxyHost.addKeyListener(new StatefulTextKeyAdapter(
            (e) -> viewModel.setProxyHost(proxyHost.getText())
        ));
        addDisposable(viewModel.getProxyHost().subscribe(proxyHostText -> {
            if (!proxyHost.getText().equals(proxyHostText)) {
                proxyHost.setText(proxyHostText);
            }
        }));

        addRow(GetText.tr("Proxy Host"), GetText.tr("This is the IP/hostname used to connect to the proxy."),
            proxyHost);

        // Proxy Port Settings
        SpinnerNumberModel proxyPortModel = new SpinnerNumberModel(App.settings.proxyPort, null, null, 1);
        proxyPortModel.setMinimum(1);
        proxyPortModel.setMaximum(65535);
        proxyPortModel.addChangeListener(changeEvent ->
            viewModel.setProxyPort((Integer) proxyPortModel.getValue()));
        addDisposable(viewModel.getProxyPort().subscribe(proxyPortModel::setValue));
        proxyPort = new JSpinner(proxyPortModel);
        proxyPort.setEditor(new JSpinner.NumberEditor(proxyPort, "#"));
        if (!enableProxy.isSelected()) {
            proxyPort.setEnabled(false);
        }

        addRow(GetText.tr("Proxy Port"), GetText.tr("This is the port used to connect to the proxy."), proxyPort);

        // Proxy Type Settings
        proxyType = new MD3ComboBox<>();
        proxyType.addItem(new ComboItem<>(ProxyType.HTTP, "HTTP"));
        proxyType.addItem(new ComboItem<>(ProxyType.SOCKS, "SOCKS"));
        proxyType.addItem(new ComboItem<>(ProxyType.DIRECT, "DIRECT"));
        proxyType.addItemListener(itemEvent -> {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    @SuppressWarnings("unchecked")
                    ComboItem<ProxyType> item =
                        (ComboItem<ProxyType>) itemEvent.getItem();
                    viewModel.setProxyType(item.getValue());
                }
            }
        );
        addDisposable(viewModel.getProxyType().subscribe(proxyType::setSelectedIndex));

        addRow(GetText.tr("Proxy Type"),
            GetText.tr("This is the type of connection the proxy uses. Either HTTP, SOCKS or DIRECT."), proxyType);

        addDisposable(viewModel.getEnableProxy().subscribe(enabled -> {
            enableProxy.setSelected(enabled);
            proxyHost.setEnabled(enabled);
            proxyPort.setEnabled(enabled);
            proxyType.setEnabled(enabled);
        }));
    }

    private void setLabelState(String tooltip, String path) {
        try {
            proxyCheckIndicator.setToolTipText(tooltip);
            ImageIcon icon = Utils.getIconImage(path);
            if (icon != null) {
                proxyCheckIndicator.setIcon(icon);
                icon.setImageObserver(proxyCheckIndicator);
            }
        } catch (NullPointerException ignored) {
            // ignored
        }
    }

    private void resetProxyCheck() {
        proxyCheckIndicator.setText("");
        proxyCheckIndicator.setIcon(null);
        proxyCheckIndicator.setToolTipText(null);
    }

    private void setProxyCheckState(CheckState state) {
        if (state == CheckState.NotChecking) {
            resetProxyCheck();
        } else if (state == CheckState.CheckPending) {
            setLabelState("Proxy check pending", "/assets/icon/warning.png");
        } else if (state == CheckState.Checking) {
            setLabelState("Checking Proxy", "/assets/image/loading-bars-small.gif");
        } else if (state instanceof CheckState.Checked) {
            if (((CheckState.Checked) state).valid) {
                resetProxyCheck();
            } else {
                setLabelState("Invalid!", "/assets/icon/error.png");
                DialogManager.okDialog()
                    .setTitle(GetText.tr("Help"))
                    .setContent(GetText.tr("Cannot connect to proxy. Please check the settings and try again."))
                    .setType(DialogManager.ERROR).show();
            }
        }
    }

    @Override
    public String getTitle() {
        return GetText.tr("Network");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Network";
    }

    @Override
    protected void createViewModel() {
    }

    @Override
    protected void onDestroy() {
        removeAll();
        proxyCheckIndicator = null;
        proxyHost = null;
        proxyPort = null;
        proxyType = null;
    }
}
