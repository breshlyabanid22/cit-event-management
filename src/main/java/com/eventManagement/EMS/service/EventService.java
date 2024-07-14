package com.eventManagement.EMS.service;

import com.eventManagement.EMS.DTO.EventDTO;
import com.eventManagement.EMS.models.*;
import com.eventManagement.EMS.repository.EventRegistrationRepository;
import com.eventManagement.EMS.repository.EventRepository;
import com.eventManagement.EMS.repository.ResourceRepository;
import com.eventManagement.EMS.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    VenueRepository venueRepository;

    @Autowired
    ResourceRepository resourceRepository;

    @Autowired
    NotificationService notificationService;
    @Autowired
    EventRegistrationRepository eventRegistrationRepository;

    public ResponseEntity<String> createEvent(EventDTO eventDTO, MultipartFile imageFile, User user){
        Optional<Venue> eventVenueOpt = venueRepository.findById(eventDTO.getVenueId());
        if(eventDTO.getName() == null || eventDTO.getStartTime() == null || eventDTO.getEndTime() == null){
            return new ResponseEntity<>("Invalid event data", HttpStatus.BAD_REQUEST);
        }
        if(eventVenueOpt.isEmpty()){
            return new ResponseEntity<>("Venue not found", HttpStatus.NOT_FOUND);
        }
        List<Event> conflictingEvents = eventRepository.findByVenueAndTimeRange(
                eventDTO.getVenueId(),
                eventDTO.getStartTime(),
                eventDTO.getEndTime()
        );
        if(!imageFile.isEmpty()){
            try {
                String fileName = imageFile.getOriginalFilename();
                String uploadDir = "event-images/";
                String filePath = uploadDir + fileName;
                File file = new File(filePath);
                imageFile.transferTo(file);
                eventDTO.setImagePath(filePath);
            }catch (IOException e){
                return new ResponseEntity<>("Failed to upload image", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        if(!conflictingEvents.isEmpty()){
            return new ResponseEntity<>("The venue is already reserved for the specified date", HttpStatus.CONFLICT);
        }
        Venue eventVenue = eventVenueOpt.get();
        Event event = new Event();
        event.setVenue(eventVenue);
        event.setName(eventDTO.getName());
        event.setDescription(eventDTO.getDescription());
        event.setStartTime(eventDTO.getStartTime());
        event.setEndTime(eventDTO.getEndTime());
        event.setStatus("Pending");
        event.setOrganizer(user);
        event.setImagePath(eventDTO.getImagePath());
        event.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("E, MMM dd yyyy")));

        List<Resource> resourceList = new ArrayList<>();
        for(Long resourceId : eventDTO.getResources()){
            if (resourceId == null) {
                throw new IllegalArgumentException("Venue manager ID must not be null");
            }
            Resource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid resource Id" + resourceId));
            resourceList.add(resource);
            event.setResources(resourceList);
        }
        eventRepository.save(event);
        return new ResponseEntity<>("Event created successfully", HttpStatus.CREATED);
    }

    public ResponseEntity<List<EventDTO>> getAllEventsByVenue(Long venueId, User user) {
        // Check if user is a venue manager or admin
        if (user.getRole().equals("VENUE_MANAGER") || user.getRole().equals("ADMIN")) {
            // Check if user is assigned to the venue or is an admin
            if (user.getRole().equals("ADMIN") || user.getManagedVenues().stream().anyMatch(v -> v.getId().equals(venueId))) {
                List<Event> events = eventRepository.findByVenueId(venueId);
                List<EventDTO> eventDTOList = new ArrayList<>();
                for(Event event : events){
                    EventDTO eventDTO = new EventDTO();
                    eventDTO.setId(event.getId());
                    eventDTO.setName(event.getName());
                    eventDTO.setDescription(event.getDescription());
                    eventDTO.setStartTime(event.getStartTime());
                    eventDTO.setEndTime(event.getEndTime());
                    eventDTO.setVenueName(event.getVenue().getName());
                    eventDTO.setOrganizer(event.getOrganizer().getUsername());
                    eventDTO.setImagePath(event.getImagePath());
                    eventDTO.setStatus(event.getStatus());
                    eventDTO.setResourceName(event.getResources().stream().map(Resource::getName).toList());
                    eventDTO.setVenueId(event.getVenue().getId());
                    eventDTOList.add(eventDTO);
                }
                return new ResponseEntity<>(eventDTOList, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    public ResponseEntity<List<EventDTO>> getAllEvents(){
        List<Event> events = eventRepository.findAll();
        List<EventDTO> eventDTOList = new ArrayList<>();
        for(Event event : events){
            EventDTO eventDTO = new EventDTO();
            eventDTO.setId(event.getId());
            eventDTO.setName(event.getName());
            eventDTO.setDescription(event.getDescription());
            eventDTO.setStartTime(event.getStartTime());
            eventDTO.setEndTime(event.getEndTime());
            eventDTO.setVenueName(event.getVenue().getName());
            eventDTO.setOrganizer(event.getOrganizer().getUsername());
            eventDTO.setImagePath(event.getImagePath());
            eventDTO.setStatus(event.getStatus());
            eventDTO.setResourceName(event.getResources().stream().map(Resource::getName).toList());
            eventDTO.setVenueId(event.getVenue().getId());
            eventDTOList.add(eventDTO);
        }
        return new ResponseEntity<>(eventDTOList, HttpStatus.OK);
    }

    public ResponseEntity<List<EventDTO>> getAllApprovedEvents() {
        List<Event> events = eventRepository.findAll();

        if (!events.isEmpty()) {
            List<EventDTO> approvedEvents = new ArrayList<>();
            for (Event event : events) {
                EventDTO eventDTO = new EventDTO();
                if (event.getStatus().equals("Approved")) {
                    eventDTO.setId(event.getId());
                    eventDTO.setName(event.getName());
                    eventDTO.setDescription(event.getDescription());
                    eventDTO.setStartTime(event.getStartTime());
                    eventDTO.setEndTime(event.getEndTime());
                    eventDTO.setVenueName(event.getVenue().getName());
                    eventDTO.setOrganizer(event.getOrganizer().getUsername());
                    eventDTO.setImagePath(event.getImagePath());
                    eventDTO.setStatus(event.getStatus());
                    eventDTO.setResourceName(event.getResources().stream().map(Resource::getName).toList());
                    eventDTO.setVenueId(event.getVenue().getId());
                    approvedEvents.add(eventDTO);
                }
            }
            return new ResponseEntity<>(approvedEvents, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    public ResponseEntity<EventDTO> getEventById(Long eventId){
        Optional <Event> eventOpt = eventRepository.findById(eventId);
        if(eventOpt.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Event event = eventOpt.get();
        EventDTO eventDTO = new EventDTO();
        eventDTO.setId(event.getId());
        eventDTO.setName(event.getName());
        eventDTO.setDescription(event.getDescription());
        eventDTO.setStartTime(event.getStartTime());
        eventDTO.setEndTime(event.getEndTime());
        eventDTO.setVenueName(event.getVenue().getName());
        eventDTO.setOrganizer(event.getOrganizer().getUsername());
        eventDTO.setImagePath(event.getImagePath());
        eventDTO.setStatus(event.getStatus());
        eventDTO.setResourceName(event.getResources().stream().map(Resource::getName).toList());
        eventDTO.setVenueId(event.getVenue().getId());
        return new ResponseEntity<>(eventDTO, HttpStatus.OK);
    }

    public ResponseEntity<String> updateEvent(Long eventId, MultipartFile imageFile, EventDTO updatedEventDTO, User user){
        Optional<Event> existingEventOpt = eventRepository.findById(eventId);
        Optional<Venue> venueEventOpt = venueRepository.findById(updatedEventDTO.getVenueId());

        if(existingEventOpt.isPresent()){
            Event existingEvent = existingEventOpt.get();

            //Check if user is admin or organizer
            if(user.getRole().equals("ADMIN") || existingEvent.getOrganizer().getUserID().equals(user.getUserID())){

                List<Event> conflictingEvents =eventRepository.findByVenueAndTimeRange(
                        updatedEventDTO.getVenueId(),
                        updatedEventDTO.getStartTime(),
                        updatedEventDTO.getEndTime()
                );
                if(!imageFile.isEmpty()){
                    try {
                        String fileName = imageFile.getOriginalFilename();
                        String uploadDir = "event-images/";
                        String filePath = uploadDir + fileName;
                        File file = new File(filePath);
                        imageFile.transferTo(file);
                        updatedEventDTO.setImagePath(filePath);
                    }catch (IOException e){
                        return new ResponseEntity<>("Failed to upload image", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                // Exclude the event being updated from conflict checks
                conflictingEvents.removeIf(event -> event.getId().equals(eventId));

                if(!conflictingEvents.isEmpty()){
                    return new ResponseEntity<>("The venue is already reserved for the specified date", HttpStatus.CONFLICT);
                }

                if(venueEventOpt.isPresent()){
                    Venue eventVenue = venueEventOpt.get();
                    existingEvent.setVenue(eventVenue);
                }else{
                    existingEvent.setVenue(existingEvent.getVenue());
                }
                //update the event details
                existingEvent.setName(updatedEventDTO.getName() != null ? updatedEventDTO.getName() : existingEvent.getName());
                existingEvent.setDescription(updatedEventDTO.getDescription() != null ? updatedEventDTO.getDescription() : existingEvent.getDescription());
                existingEvent.setStartTime(updatedEventDTO.getStartTime() != null ? updatedEventDTO.getStartTime() : existingEvent.getStartTime());
                existingEvent.setEndTime(updatedEventDTO.getEndTime() != null ? updatedEventDTO.getEndTime() : existingEvent.getEndTime());
                existingEvent.setStatus(updatedEventDTO.getStatus() != null ? updatedEventDTO.getStatus() : existingEvent.getStatus());

                eventRepository.save(existingEvent);
                return new ResponseEntity<>("Event updated successfully", HttpStatus.OK);
            }else{
                return new ResponseEntity<>("You are not authorized to update this event", HttpStatus.FORBIDDEN);
            }
        }else{
            return new ResponseEntity<>("Event not found", HttpStatus.NOT_FOUND);
        }
    }

    //This is accessed by admin or venue_manager when approving a proposed event
    public ResponseEntity<String> approveEvent(Long userId, User user){
        Optional<Event> eventOptional = eventRepository.findById(userId);

        if(eventOptional.isEmpty()){
            return new ResponseEntity<>("Event does not exist", HttpStatus.NOT_FOUND);
        }
        Event event = eventOptional.get();
        if(user.getRole().equals("VENUE_MANAGER") || user.getRole().equals("ADMIN")){
            event.setStatus("Approved");
            eventRepository.save(event);
            return  new ResponseEntity<>("Event has been approved", HttpStatus.OK);
        }else {
            return new ResponseEntity<>("User is not authorized to approve the event", HttpStatus.UNAUTHORIZED);
        }
    }

    //This is accessed by admin or venue_manager when rejecting a proposed event
    public ResponseEntity<String> rejectEvent(Long userId, User user){
        Optional<Event> eventOptional = eventRepository.findById(userId);

        if(eventOptional.isEmpty()){
            return new ResponseEntity<>("Event does not exist", HttpStatus.NOT_FOUND);
        }
        Event event = eventOptional.get();
        if(user.getRole().equals("VENUE_MANAGER") || user.getRole().equals("ADMIN")){
            event.setStatus("Rejected");
            eventRepository.save(event);
            return  new ResponseEntity<>("Event has been approved", HttpStatus.OK);
        }else {
            return new ResponseEntity<>("User is not authorized to approve the event", HttpStatus.UNAUTHORIZED);
        }
    }

    //If the organizer wants to cancel the event
    public ResponseEntity<String> cancelEvent(Long eventId, User user){
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if(eventOpt.isPresent()){
            Event event = eventOpt.get();

            //if user is the organizer or admin, allow to delete.
            if(user.getRole().equals("ADMIN") || event.getOrganizer().getUserID().equals(user.getUserID())){
                List<EventRegistration> registrations = eventRegistrationRepository.findByEventId(eventId);
                List<User> registeredUsers = registrations.stream().map(EventRegistration::getUser).toList();
                String message = "Sorry, the event " + event.getName() + " has been canceled";

                notificationService.sendNotificationToUser(registeredUsers, message, event);
                event.setStatus("Canceled");
                return new ResponseEntity<>("Event has been cancelled", HttpStatus.OK);
            }else{
                return new ResponseEntity<>("You are not authorized to cancel this event", HttpStatus.FORBIDDEN);
            }
        }else{
            return new ResponseEntity<>("Event not found", HttpStatus.NOT_FOUND);
        }
    }


}
