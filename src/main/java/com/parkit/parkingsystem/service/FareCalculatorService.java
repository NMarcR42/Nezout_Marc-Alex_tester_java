package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }
        
        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        
        // set duration in hour. milliseconds -> seconds -> minutes -> hours
        double duration = (outHour - inHour) / (1000.0 * 60 * 60);
        
        // Free if less than 30min
        if (duration < 0.5) { // we want to calcul "duration" in hour so 1h is "1.0" and 30min is "0.5"
            ticket.setPrice(0.0);
            return; 
        }
        // set a price to verify if the customer need a discount or not
        double price = 0;
        
        // if the ticket price is not free then
        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {            	
            	price = duration * Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                price = duration * Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
        
        // Apply 5% discount
        if (discount) {
            price *= 0.95;
        }
        ticket.setPrice(price);
    }
    
    // default price without discount
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}