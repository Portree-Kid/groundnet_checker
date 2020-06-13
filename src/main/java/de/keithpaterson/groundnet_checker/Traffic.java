package de.keithpaterson.groundnet_checker;

import java.io.Serializable;

public class Traffic implements Serializable{
  private String icao = null;
  private long flights = 0;
  
  public Traffic(final String icao) {
	  this.icao = icao; 
}

public String getIcao() {
	return icao;
}

public void incrementFlights() {
	++flights;
}

public long getFlights() {
	return flights;
}

@Override
public String toString() {
	return "Traffic [icao=" + icao + ", flights=" + flights + "]";
}
  
 

}
