/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * As long as you retain this notice you can do whatever you want with this
 * stuff. If you meet an employee from Windward some day, and you think this
 * stuff is worth it, you can buy them a beer in return. Windward Studios
 * ----------------------------------------------------------------------------
 */

package net.windward.Windwardopolis2.api;


import java.awt.*;
import org.dom4j.Element;
public class Company
{
	private Company(Element elemCompany)
	{
		setName(elemCompany.attributeValue("name"));
		setBusStop(new Point(Integer.parseInt(elemCompany.attributeValue("bus-stop-x")), Integer.parseInt(elemCompany.attributeValue("bus-stop-y"))));
		setPassengers(new java.util.ArrayList<Passenger>());
	}

	/** 
	 The name of the company.
	*/
	private String privateName;
	public final String getName()
	{
		return privateName;
	}
	private void setName(String value)
	{
		privateName = value;
	}

	/** 
	 The tile with the company's bus stop.
	*/
	private Point privateBusStop;
	public final Point getBusStop()
	{
		return privateBusStop;
	}
	private void setBusStop(Point value)
	{
		privateBusStop = value;
	}

	/** 
	 The name of the passengers waiting at this company's bus stop for a ride.
	*/
	private java.util.List<Passenger> privatePassengers;
	public java.util.List<Passenger> getPassengers()
	{
		return privatePassengers;
	}
	public void setPassengers(java.util.List<Passenger> value)
	{
		privatePassengers = value;
	}

	public static java.util.ArrayList<Company> FromXml(Element elemCompanies)
	{
		java.util.ArrayList<Company> companies = new java.util.ArrayList<Company>();
		for (Object objCmpyOn : elemCompanies.selectNodes("company"))
		{
            Element elemCmpyOn = (Element) objCmpyOn;
			companies.add(new Company(elemCmpyOn));
		}
		return companies;
	}

	@Override
	public String toString()
	{
		return String.format("%1$s; %2$s", getName(), getBusStop());
	}
}