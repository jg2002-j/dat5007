package com.st20313779.controller;

import com.st20313779.model.DayPlan;
import com.st20313779.repository.dto.DayMealsDto;
import com.st20313779.service.PlannerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import java.time.LocalDate;

@Path("planner")
@ApplicationScoped
public class PlannerEndpoint {

    private final PlannerService service;

    @Inject
    public PlannerEndpoint(final PlannerService service) {
        this.service = service;
    }

    @POST
    @Path("/save/day")
    public DayPlan saveDayPlan(@RequestBody final DayMealsDto dto) {
        return service.saveDayPlan(dto);
    }

    @GET
    @Path("/view/day/{date}")
    public DayPlan getDayPlan(@PathParam("date") final LocalDate date) {
        return service.getDayPlan(date);
    }

}
