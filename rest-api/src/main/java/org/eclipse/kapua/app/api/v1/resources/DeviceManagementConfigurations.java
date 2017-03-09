/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.app.api.v1.resources;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kapua.app.api.v1.resources.model.CountResult;
import org.eclipse.kapua.app.api.v1.resources.model.EntityId;
import org.eclipse.kapua.app.api.v1.resources.model.ScopeId;
import org.eclipse.kapua.commons.model.query.predicate.AndPredicate;
import org.eclipse.kapua.commons.model.query.predicate.AttributePredicate;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.service.device.management.configuration.DeviceComponentConfiguration;
import org.eclipse.kapua.service.device.management.configuration.DeviceConfiguration;
import org.eclipse.kapua.service.device.management.configuration.DeviceConfigurationManagementService;
import org.eclipse.kapua.service.device.registry.Device;
import org.eclipse.kapua.service.device.registry.event.DeviceEvent;
import org.eclipse.kapua.service.device.registry.event.DeviceEventFactory;
import org.eclipse.kapua.service.device.registry.event.DeviceEventListResult;
import org.eclipse.kapua.service.device.registry.event.DeviceEventPredicates;
import org.eclipse.kapua.service.device.registry.event.DeviceEventQuery;
import org.eclipse.kapua.service.device.registry.event.DeviceEventService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api("Devices")
@Path("{scopeId}/devices/{deviceId}/configurations")
public class DeviceManagementConfigurations extends AbstractKapuaResource {

    private final KapuaLocator locator = KapuaLocator.getInstance();
    private final DeviceEventService deviceEventService = locator.getService(DeviceEventService.class);
    private final DeviceEventFactory deviceEventFactory = locator.getFactory(DeviceEventFactory.class);
    private final DeviceConfigurationManagementService configurationService = locator.getService(DeviceConfigurationManagementService.class);
    
    /**
     * Returns the current configuration of the device.
     *
     * @param scopeId
     * @param deviceId
     *  The id of the device
     * 
     * @return The requested configurations
     * @since 1.0.0
     */
     @GET
     @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     @ApiOperation(value = "Gets the device configurations", //
     notes = "Returns the current configuration of a device", response = DeviceConfiguration.class)
     public DeviceConfiguration get(
             @PathParam("scopeId") ScopeId scopeId,
             @ApiParam(value = "The id of the device", required = true) @PathParam("deviceId") EntityId deviceId, //
             @QueryParam("timeout") @DefaultValue("") Long timeout) {
     DeviceConfiguration deviceConfiguration = null;
     try {
         deviceConfiguration = getComponent(scopeId, deviceId, null, timeout);
     } catch (Throwable t) {
     handleException(t);
     }
     return returnNotNullEntity(deviceConfiguration);
     }
    
     @PUT
     @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     @ApiOperation(value = "Updates a device component configuration", //
     notes = "Updates a device component configuration", response = DeviceConfiguration.class)
     public Response update(
             @PathParam("scopeId") ScopeId scopeId,
             @ApiParam(value = "The id of the device", required = true) @PathParam("deviceId") EntityId deviceId, //
             @QueryParam("timeout") @DefaultValue("") Long timeout,
             @ApiParam(value = "The configuration to send to the device", required = true)DeviceConfiguration deviceConfiguration) {
     try {
         configurationService.put(scopeId, deviceId, deviceConfiguration, timeout);
     } catch (Throwable t) {
     handleException(t);
     }
    return  Response.ok().build();
     }
     
    /**
     * Returns the configuration of a device or the configuration of the OSGi component
     * identified with specified PID (service's persistent identity).
     * In the OSGi framework, the service's persistent identity is defined as the name attribute of the
     * Component Descriptor XML file; at runtime, the same value is also available
     * in the component.name and in the service.pid attributes of the Component Configuration.
     * 
     *@param scopeId
     * @param deviceId
     * The id of the device
     * @param componentId
     * An optional id of the component to get the configuration for
     * @return The requested configurations
     */
     @GET
     @Path("{componentId}")
     @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     @ApiOperation(value = "Gets the device configurations", //
     notes = "Returns the configuration of a device or the configuration of the OSGi component " +
     "identified with specified PID (service's persistent identity). " +
     "In the OSGi framework, the service's persistent identity is defined as the name attribute of the " +
     "Component Descriptor XML file; at runtime, the same value is also available " +
     "in the component.name and in the service.pid attributes of the Component Configuration.", response = DeviceConfiguration.class)
     public DeviceConfiguration getComponent(
             @PathParam("scopeId") ScopeId scopeId,
             @ApiParam(value = "The id of the device", required = true) @PathParam("deviceId") EntityId deviceId, //
             @ApiParam(value = "An optional id of the component to get the configuration for", required = false) @PathParam("componentId") String componentId,
             @QueryParam("timeout") @DefaultValue("") Long timeout) {
     DeviceConfiguration deviceConfiguration = null;
     try {
         deviceConfiguration = configurationService.get(scopeId, deviceId, null, componentId, timeout);
     } catch (Throwable t) {
     handleException(t);
     }
     return returnNotNullEntity(deviceConfiguration);
     }
     
     @PUT
     @Path("{componentId}")
     @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     @ApiOperation(value = "Updates a device component configuration", //
     notes = "Updates a device component configuration", response = DeviceConfiguration.class)
     public Response updateComponent(
             @PathParam("scopeId") ScopeId scopeId,
             @ApiParam(value = "The id of the device", required = true) @PathParam("deviceId") EntityId deviceId, //
             @ApiParam(value = "The component id to update", required = true) @PathParam("componentId") String componentId,
             @QueryParam("timeout") @DefaultValue("") Long timeout,
             @ApiParam(value = "The component configuration to send to the device", required = true)DeviceComponentConfiguration deviceComponentConfiguration) {
     try {
         deviceComponentConfiguration.setId(componentId);
         configurationService.put(scopeId, deviceId, deviceComponentConfiguration, timeout);
     } catch (Throwable t) {
     handleException(t);
     }
    return  Response.ok().build();
     }
}
