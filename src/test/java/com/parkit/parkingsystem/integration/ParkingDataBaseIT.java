package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

//import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        System.out.println("-------------------");
        System.out.println("value of testParkingACar.->verify if the ticket has been saved");
        System.out.println("-------------------");
        parkingService.processIncomingVehicle();
        
        // verify if the ticket has been saved
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        
        assertNotNull(ticket, "The ticket must be registered in the database");
        assertNull(ticket.getOutTime(), "Out time should be null on entry");
        assertTrue(ticket.getInTime() != null, "In time should be set");
        
        // verify is the parking spot is no longer free
        int parkingNumber = ticket.getParkingSpot().getId();
        boolean isAvailable = parkingSpotDAO.getNextAvailableSlot(ticket.getParkingSpot().getParkingType()) != parkingNumber;
        assertTrue(isAvailable, "Parking spot should no longer be available");
        
    }

    @Test
    public void testParkingLotExit() throws Exception{
    	System.out.println("-------------------");
        System.out.println("value of testParkingLotExit");
        System.out.println("-------------------");

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        ticketDAO.inTimetest("ABCDEF", 60);  // 1h before
        
        Thread.sleep(1000); // avoid any problem during the test so inTime value will be different than outTime
        parkingService.processExitingVehicle();
        
        Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(updatedTicket.getOutTime(),"The exit time must be entered");
        assertTrue(updatedTicket.getOutTime().after(updatedTicket.getInTime()), "outTime should be after inTime"); // verify if outTime>inTime
        assertTrue(updatedTicket.getPrice() > 0);            
    }
    
    @Test
    public void testParkingLotExitRecurringUser() throws Exception {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        System.out.println("-------------------");
        System.out.println("value of testParkingLotExitRecurringUser");
        System.out.println("-------------------");
        
        // Simulates a first usage
        parkingService.processIncomingVehicle();
        ticketDAO.inTimetest("ABCDEF", 60);  // 1h before
        Thread.sleep(1000);
        parkingService.processExitingVehicle(); // leave the parking
        
        // Simulates a second  usage( the user is now recurrent one)
        parkingService.processIncomingVehicle();
        ticketDAO.inTimetest("ABCDEF", 60); // 1h before
        
        parkingService.processExitingVehicle();

        // get the ticket from DB
        Ticket updatedSecondTicket = ticketDAO.getTicket("ABCDEF");
        
        assertNotNull(updatedSecondTicket, "The ticket must not be null"); // return the text if null
        assertNotNull(updatedSecondTicket.getInTime(), "The entry time must be entered");
        assertNotNull(updatedSecondTicket.getOutTime(), "The exit time must be entered");
        
        System.out.println("----------"+updatedSecondTicket.getOutTime()+"---------");
        
        double expectedPrice = 1.5 * 0.95; // apply discount on the normal price 
        assertEquals(expectedPrice, updatedSecondTicket.getPrice(), 0.01, "The 5% discount was not applied correctly");
    }


}
