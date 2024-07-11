package com.eventManagement.EMS.controller;


import com.eventManagement.EMS.DTO.EventDTO;
import com.eventManagement.EMS.config.UserInfoDetails;
import com.eventManagement.EMS.models.Event;
import com.eventManagement.EMS.models.User;
import com.eventManagement.EMS.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/events")
@RestController
public class EventController {

    @Autowired
    EventService eventService;


    @PostMapping("/create") //Create an event and wait for approval
    public ResponseEntity<String> createEvent(
            @RequestBody EventDTO eventDTO,
            MultipartFile imageFile,
            @AuthenticationPrincipal UserInfoDetails userDetails){
        if (userDetails == null) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        return eventService.createEvent(eventDTO, imageFile, user);
    }

    @GetMapping("/venue/{venueId}") //Fetch an event by Venue
    @PreAuthorize("hasAuthority('VENUE_MANAGER') || hasAuthority('ADMIN')") //Only accessible by venue_managers or admin
    public ResponseEntity<List<Event>> getAllEventsByVenue(
            @PathVariable Long venueId,
            @AuthenticationPrincipal UserInfoDetails userDetails){
        User user = userDetails.getUser();
        return eventService.getAllEventsByVenue(venueId, user);
    }


    @GetMapping("/all") // Get all events
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Event>> getAllEvents(){
        return eventService.getAllEvents();
    }


    @GetMapping("/approvedEvents") //fetches all approved events.These events are displayed in the page and browsed by user
    public ResponseEntity<List<Event>> getAllApproveEvents(){
        return eventService.getAllApprovedEvents();
    }


    @PutMapping("/update/{eventId}") //Update an event
    public ResponseEntity<String> updateEvent(@PathVariable Long eventId, @RequestBody EventDTO updatedEventDTO, @AuthenticationPrincipal UserInfoDetails userDetails){
        User user = userDetails.getUser();
        return eventService.updateEvent(eventId, updatedEventDTO, user);
    }

    @PutMapping("/approval/{eventId}")
    @PreAuthorize("hasAuthority('VENUE_MANAGER') || hasAuthority('ADMIN')") //An admin or venue_manager can approve the proposed event
    public ResponseEntity<String> approveEventProposal(@PathVariable Long eventId, @AuthenticationPrincipal UserInfoDetails userInfoDetails){
        if(userInfoDetails == null){
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        User user = userInfoDetails.getUser();
        return eventService.approveEvent(eventId, user);
    }
    @PutMapping("/reject/{eventId}")
    @PreAuthorize("hasAuthority('VENUE_MANAGER') || hasAuthority('ADMIN')")
    public ResponseEntity<String> rejectEventProposal(@PathVariable Long eventId, @AuthenticationPrincipal UserInfoDetails userInfoDetails){
        if(userInfoDetails == null){
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        User user = userInfoDetails.getUser();
        return eventService.rejectEvent(eventId, user);
    }

    @DeleteMapping("/cancel/{eventId}") //Cancel or delete an event
    public ResponseEntity<String> cancelEvent(@PathVariable Long eventId, @AuthenticationPrincipal UserInfoDetails userDetails){
        User user = userDetails.getUser();
        return eventService.cancelEvent(eventId, user);
    }

    @GetMapping("/{eventId}") //Endpoint to display the details of a single event
    public ResponseEntity<Event> getEventById(@PathVariable Long eventId){
        return eventService.getEventById(eventId);
    }
}
