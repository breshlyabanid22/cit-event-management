package com.eventManagement.EMS.service;

import com.eventManagement.EMS.DTO.VenueDTO;
import com.eventManagement.EMS.models.Event;
import com.eventManagement.EMS.models.User;
import com.eventManagement.EMS.models.Venue;
import com.eventManagement.EMS.repository.UserRepository;
import com.eventManagement.EMS.repository.VenueRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VenueService {

    @Autowired
    VenueRepository venueRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    NotificationService notificationService;

    @Value("${upload.venue.dir}")
    public String uploadDir;

    public ResponseEntity<String> addVenue(VenueDTO venueDTO, List<MultipartFile> imageFiles) {

        if(!imageFiles.isEmpty()){
            List<String> imagePaths = new ArrayList<>();
            for(MultipartFile imageFile : imageFiles){
                if(!imageFile.isEmpty()){
                    try{
                        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                        Files.createDirectories(uploadPath);
                        if(imageFile.getOriginalFilename() != null){
                            Path filePath = uploadPath.resolve(imageFile.getOriginalFilename());
                            imageFile.transferTo(filePath.toFile());
                            imagePaths.add("/venue-images/" + imageFile.getOriginalFilename());
                        }
                    }catch (IOException e){
                        return new ResponseEntity<>("Failed to upload image", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }
            venueDTO.setImagePath(imagePaths);
        }
        Optional<User> userOptional = userRepository.findById(venueDTO.getVenueManagersID());
        if(userOptional.isEmpty()){
            return new ResponseEntity<>("Invalid user id", HttpStatus.NOT_FOUND);
        }
        User foundManager = userOptional.get();
        List<User> managers = new ArrayList<>();
        foundManager.setUserType("VENUE_MANAGER");
        foundManager.setRole("ORGANIZER");
        managers.add(foundManager);

        Venue venue = new Venue();
        venue.setName(venueDTO.getName());
        venue.setLocation(venueDTO.getLocation());
        venue.setMaxCapacity(venueDTO.getMaxCapacity());
        venue.setImagePath(venueDTO.getImagePath());
        venue.setVenueManagers(managers);

        //Then send notification to the user
        String message = "Hi " + foundManager.getFirstName() + ", you have been appointed as the venue manager for " + venueDTO.getName();
        notificationService.regularNotification(foundManager, message);

        venueRepository.save(venue);
        return new ResponseEntity<>("Venue has been changed successfully", HttpStatus.CREATED);
    }
    public ResponseEntity<String> updateVenue(Long venueId, VenueDTO updateVenue, List<MultipartFile> imageFiles){
        Optional<Venue> venueOptional = venueRepository.findById(venueId);

        Venue existingVenue = venueOptional.get();

        if(imageFiles != null && !imageFiles.isEmpty()){
            List<String> imagePaths = new ArrayList<>();
            for(MultipartFile imageFile : imageFiles){
                if(!imageFile.isEmpty()){
                    try{
                        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                        Files.createDirectories(uploadPath);
                        if(imageFile.getOriginalFilename() != null){
                            Path filePath = uploadPath.resolve(imageFile.getOriginalFilename());
                            imageFile.transferTo(filePath.toFile());
                            imagePaths.add("/venue-images/" + imageFile.getOriginalFilename());
                        }
                    }catch (IOException e){
                        return new ResponseEntity<>("Failed to upload image", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }
            existingVenue.setImagePath(imagePaths);
        }
        if(venueOptional.isEmpty()){
            return new ResponseEntity<>("Venue not found", HttpStatus.NOT_FOUND);
        }

        if(updateVenue.getVenueManagersID() != null){
            Optional<User> userOptional = userRepository.findById(updateVenue.getVenueManagersID());
            if(userOptional.isEmpty()){
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }
            User venueManager = userOptional.get();
            List<User> venueManagers = new ArrayList<>();
            venueManager.setUserType("VENUE_MANAGER");
            venueManager.setRole("ORGANIZER");
            venueManagers.add(venueManager);
            existingVenue.setVenueManagers(venueManagers);
        }
        existingVenue.setImagePath(existingVenue.getImagePath());
        existingVenue.setName(updateVenue.getName() != null ? updateVenue.getName() : existingVenue.getName());
        existingVenue.setLocation(updateVenue.getLocation() != null ? updateVenue.getLocation() : existingVenue.getLocation());
        if(updateVenue.getMaxCapacity() >= 0){
            existingVenue.setMaxCapacity(updateVenue.getMaxCapacity());
        }
        existingVenue.setName(updateVenue.getName() != null ? updateVenue.getName() : existingVenue.getName());

        venueRepository.save(existingVenue);
        return new ResponseEntity<>("Venue has been updated", HttpStatus.OK);
    }

    public ResponseEntity<List<VenueDTO>> getAll(){
        List<Venue> venues = venueRepository.findAll();

        List<VenueDTO> venueDTOList = new ArrayList<>();
        for(Venue venue : venues){
            VenueDTO venueDTO = new VenueDTO();
            venueDTO.setId(venue.getId());
            venueDTO.setLocation(venue.getLocation());
            venueDTO.setMaxCapacity(venue.getMaxCapacity());
            venueDTO.setVenueManagers(venue.getVenueManagers().stream().map(User::getFirstName).toList());
            venueDTO.setEvents(venue.getEvents().stream().map(Event::getName).toList());
            venueDTO.setName(venue.getName());
            venueDTO.setImagePath(venue.getImagePath());
            venueDTOList.add(venueDTO);
        }
        return new ResponseEntity<>(venueDTOList, HttpStatus.OK);
    }
    @Transactional
    public ResponseEntity<String> deleteVenue(Long venueId){
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new EntityNotFoundException("Venue not found with id " + venueId));

        if(venue.getVenueManagers() != null){
            for(User manager : venue.getVenueManagers()){
                User foundManager = userRepository.findById(manager.getUserID())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid venue manager ID: " + manager.getUserID()));
                foundManager.setUserType(null);
            }
            venue.getVenueManagers().clear();
        }

        venueRepository.delete(venue);
        return new ResponseEntity<>("Venue successfully deleted", HttpStatus.NO_CONTENT);
    }
    @Transactional//Deletes all venues. It won't be deleted if it has active event held in the event
    public ResponseEntity<String> deleteAll(){
        List<User> venueManagers = userRepository.findByRole("VENUE_MANAGER");

        for(User user: venueManagers){
            user.setUserType(null);
        }
        venueRepository.deleteAll();
        return new ResponseEntity<>("All venues has been deleted", HttpStatus.NO_CONTENT);
    }
    public ResponseEntity<List<VenueDTO>> getAllVenuesByManager(Long userId){
        Optional<User> userOptional = userRepository.findById(userId);
        List<Venue> venues = venueRepository.findAll();
        if(userOptional.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        User manager = userOptional.get();
        List<VenueDTO> managerVenues = new ArrayList<>();
        for(Venue venue: venues){
            if(venue.getVenueManagers().stream().anyMatch(mgr -> mgr.getUserID().equals(manager.getUserID()))){
                VenueDTO venueDTO =  new VenueDTO();
                venueDTO.setName(venue.getName());
                venueDTO.setLocation(venue.getLocation());
                venueDTO.setId(venue.getId());
                venueDTO.setMaxCapacity(venue.getMaxCapacity());
                venueDTO.setImagePath(venue.getImagePath());
                venueDTO.setEvents(venue.getEvents().stream().map(Event::getName).toList());
                managerVenues.add(venueDTO);
            }
        }
        return new ResponseEntity<>(managerVenues, HttpStatus.OK);
    }
}
