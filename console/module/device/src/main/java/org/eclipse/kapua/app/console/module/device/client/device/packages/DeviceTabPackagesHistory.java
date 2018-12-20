/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.app.console.module.device.client.device.packages;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BasePagingLoadConfig;
import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.eclipse.kapua.app.console.module.api.client.messages.ConsoleMessages;
import org.eclipse.kapua.app.console.module.api.client.resources.icons.IconSet;
import org.eclipse.kapua.app.console.module.api.client.resources.icons.KapuaIcon;
import org.eclipse.kapua.app.console.module.api.client.ui.button.Button;
import org.eclipse.kapua.app.console.module.api.client.ui.tab.KapuaTabItem;
import org.eclipse.kapua.app.console.module.api.client.ui.widget.DateRangeSelector;
import org.eclipse.kapua.app.console.module.api.client.ui.widget.KapuaPagingToolBar;
import org.eclipse.kapua.app.console.module.api.shared.model.session.GwtSession;
import org.eclipse.kapua.app.console.module.device.client.messages.ConsoleDeviceMessages;
import org.eclipse.kapua.app.console.module.device.shared.model.GwtDevice;
import org.eclipse.kapua.app.console.module.device.shared.model.device.management.registry.GwtDeviceManagementOperation;
import org.eclipse.kapua.app.console.module.device.shared.model.device.management.registry.GwtDeviceManagementOperationQuery;
import org.eclipse.kapua.app.console.module.device.shared.service.GwtDeviceManagementOperationService;
import org.eclipse.kapua.app.console.module.device.shared.service.GwtDeviceManagementOperationServiceAsync;

import java.util.ArrayList;
import java.util.List;

public class DeviceTabPackagesHistory extends KapuaTabItem<GwtDevice> {

    private static final ConsoleMessages MSGS = GWT.create(ConsoleMessages.class);
    private static final ConsoleDeviceMessages DEVICES_MSGS = GWT.create(ConsoleDeviceMessages.class);

    private static final GwtDeviceManagementOperationServiceAsync DEVICE_MANAGEMENT_SERVICE = GWT.create(GwtDeviceManagementOperationService.class);

    private static final int PAGE_SIZE = 250;

    private boolean initialized;

    private ToolBar toolBar;

    private Button refreshButton;
    private Button export;

    private DateRangeSelector dateRangeSelector;
    private Grid<GwtDeviceManagementOperation> grid;
    private KapuaPagingToolBar pagingToolBar;
    private BasePagingLoader<PagingLoadResult<GwtDeviceManagementOperation>> loader;

    protected boolean refreshProcess;

    public DeviceTabPackagesHistory(GwtSession currentSession) {
        super(currentSession, DEVICES_MSGS.deviceInstallTabHistory(), new KapuaIcon(IconSet.CLOCK_O));

        initialized = false;
    }

    @Override
    public void setEntity(GwtDevice gwtDevice) {
        super.setEntity(gwtDevice);
        super.setDirty(true);
    }

    @Override
    protected void onRender(Element parent, int index) {

        super.onRender(parent, index);
        setLayout(new FitLayout());
        setBorders(false);

        // init components
        initGrid();

        ContentPanel devicesPackageHistoryPanel = new ContentPanel();
        devicesPackageHistoryPanel.setBorders(false);
        devicesPackageHistoryPanel.setBodyBorder(true);
        devicesPackageHistoryPanel.setHeaderVisible(false);
        devicesPackageHistoryPanel.setLayout(new FitLayout());
        devicesPackageHistoryPanel.setScrollMode(Scroll.AUTO);
        devicesPackageHistoryPanel.setTopComponent(toolBar);
        devicesPackageHistoryPanel.add(grid);
        devicesPackageHistoryPanel.setBottomComponent(pagingToolBar);

        add(devicesPackageHistoryPanel);
        initialized = true;

        loader.load();
        pagingToolBar.enable();
    }

    private void initGrid() {
        List<ColumnConfig> configs = new ArrayList<ColumnConfig>();

        ColumnConfig column = new ColumnConfig();
        column.setId("id");
        column.setHeader(DEVICES_MSGS.deviceInstallTabHistoryTableId());
        column.setAlignment(HorizontalAlignment.CENTER);
        column.setWidth(60);
        column.setSortable(false);
        column.setHidden(true);
        configs.add(column);

        column = new ColumnConfig();
        column.setId("startedOnFormatted");
        column.setHeader(DEVICES_MSGS.deviceInstallTabHistoryTableStartedOn());
        column.setWidth(200);
        configs.add(column);

        column = new ColumnConfig();
        column.setId("endedOnFormatted");
        column.setHeader(DEVICES_MSGS.deviceInstallTabHistoryTableEndedOn());
        column.setWidth(200);
        column.setAlignment(HorizontalAlignment.CENTER);
        configs.add(column);

        column = new ColumnConfig();
        column.setId("inputProperty_kapuapackagedownloadname");
        column.setHeader(DEVICES_MSGS.deviceInstallTabHistoryTableName());
        column.setWidth(200);
        configs.add(column);

        column = new ColumnConfig();
        column.setId("inputProperty_kapuapackagedownloadversion");
        column.setHeader(DEVICES_MSGS.deviceInstallTabHistoryTableVersion());
        column.setWidth(200);
        configs.add(column);

        column = new ColumnConfig();
        column.setId("inputProperty_kapuapackagedownloaduri");
        column.setHeader(DEVICES_MSGS.deviceInstallTabHistoryTableURI());
        column.setWidth(200);
        configs.add(column);

        GridCellRenderer renderer = new GridCellRenderer<GwtDeviceManagementOperation>() {

            @Override
            public Object render(GwtDeviceManagementOperation model, String property, ColumnData config, int rowIndex, int colIndex, ListStore<GwtDeviceManagementOperation> store, Grid<GwtDeviceManagementOperation> grid) {
                switch (model.getStatusEnum()) {
                    case COMPLETED:
                        return DEVICES_MSGS.deviceInstallTabHistoryTableStatusCompleted();
                    case RUNNING:
                        return DEVICES_MSGS.deviceInstallTabHistoryTableStatusRunning();
                    case STALE:
                        return DEVICES_MSGS.deviceInstallTabHistoryTableStatusStale();
                    case FAILED:
                        return DEVICES_MSGS.deviceInstallTabHistoryTableStatusFailed();
                }
                return null;
            }

        };

        column = new ColumnConfig();
        column.setId("status");
        column.setHeader(DEVICES_MSGS.deviceInstallTabHistoryTableStatus());
        column.setWidth(200);
        column.setAlignment(HorizontalAlignment.CENTER);
        column.setRenderer(renderer);
        configs.add(column);

        // loader and store
        RpcProxy<PagingLoadResult<GwtDeviceManagementOperation>> proxy = new RpcProxy<PagingLoadResult<GwtDeviceManagementOperation>>() {

            @Override
            public void load(Object loadConfig, AsyncCallback<PagingLoadResult<GwtDeviceManagementOperation>> callback) {
                if (selectedEntity != null) {
                    BasePagingLoadConfig pagingConfig = (BasePagingLoadConfig) loadConfig;
                    pagingConfig.setLimit(PAGE_SIZE);

                    GwtDeviceManagementOperationQuery query = new GwtDeviceManagementOperationQuery();
                    query.setScopeId(selectedEntity.getScopeId());
                    query.setDeviceId(selectedEntity.getId());

                    DEVICE_MANAGEMENT_SERVICE.query(pagingConfig, query, callback);
                } else {
                    callback.onSuccess(new BasePagingLoadResult<GwtDeviceManagementOperation>(new ArrayList<GwtDeviceManagementOperation>()));
                }
            }
        };
        loader = new BasePagingLoader<PagingLoadResult<GwtDeviceManagementOperation>>(proxy);
        loader.setSortDir(SortDir.DESC);
        loader.setSortField("startedOnFormatted");
        loader.setRemoteSort(true);

        ListStore<GwtDeviceManagementOperation> store = new ListStore<GwtDeviceManagementOperation>(loader);

        grid = new Grid<GwtDeviceManagementOperation>(store, new ColumnModel(configs));
        grid.setBorders(false);
        grid.setStateful(false);
        grid.setLoadMask(true);
        grid.setStripeRows(true);
        grid.setTrackMouseOver(false);
        grid.setAutoExpandColumn("package");
        grid.disableTextSelection(false);
        grid.getView().setAutoFill(true);
        grid.getView().setForceFit(true);
        grid.getView().setEmptyText(DEVICES_MSGS.deviceInstallTabHistoryTableEmpty());

        pagingToolBar = new KapuaPagingToolBar(PAGE_SIZE);
        pagingToolBar.bind(loader);

        GridSelectionModel<GwtDeviceManagementOperation> selectionModel = new GridSelectionModel<GwtDeviceManagementOperation>();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        grid.setSelectionModel(selectionModel);

        loader.load(0, PAGE_SIZE);
    }

    // --------------------------------------------------------------------------------------
    //
    // Device Operation List Management
    //
    // --------------------------------------------------------------------------------------

    @Override
    public void doRefresh() {
        if (initialized) {
            if (selectedEntity == null) {
                // clear the table
                grid.getStore().removeAll();
            } else {
                loader.load();
            }
        }
    }

    public void reload() {
        loader.load();
    }
}
