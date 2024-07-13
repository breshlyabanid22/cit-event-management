package com.eventManagement.EMS.DTO;

import java.util.ArrayList;
import java.util.List;

public class VenueDTO {

    private Long id;

    private String name;
    private String location;

    private int maxCapacity;

    private List<String> events;


    private List<String> venueManagers = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public List<String> getVenueManagers() {
        return venueManagers;
    }

    public void setVenueManagers(List<String> venueManagers) {
        this.venueManagers = venueManagers;
    }
}