package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import static org.junit.jupiter.api.Assertions.*;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest(){
    	// the user is a regular user of  the parking
    	when(ticketDAO.getNbTicket(anyString())).thenReturn(2); // discount logic when the user come 2 times
    	
        parkingService.processExitingVehicle();
        
        // Ensure ticket is updated and parking spot is free
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));      
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
    }
    
    @Test
    public void testProcessIncomingVehicle() throws Exception {
    	// Simulate user input selection for vehicle type
        when(inputReaderUtil.readSelection()).thenReturn(1); // 1 = CAR for test
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1); // 1 free slot to park the car
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        parkingService.processIncomingVehicle();
        
        // Ensure ticket and parking spot are handled properly
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
    }
    
    @Test
    public void processExitingVehicleTestUnableUpdate() {
    	// Simulate update failure
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        parkingService.processExitingVehicle();
        

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class)); // wont work since update failed
    }
    
    @Test
    public void testGetNextParkingNumberIfAvailable() throws Exception {
    	// Simulate user selecting vehicle type CAR
        when(inputReaderUtil.readSelection()).thenReturn(1); // 1 = CAR for test
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1); // 1 free slot to park the car

        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
        
        assertNotNull(result); // a spot should be returned
        assertEquals(1, result.getId());
        assertEquals(ParkingType.CAR, result.getParkingType());
    }
    
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1); // 1 = CAR for test
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0); // No spot available

        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        assertNull(result); // Should return null
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(3); // Invalid type (not 1 or 2)

        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        assertNull(result); // Should return null for the wrong input
    }


}
