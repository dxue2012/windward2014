/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * As long as you retain this notice you can do whatever you want with this
 * stuff. If you meet an employee from Windward meet some day, and you think
 * this stuff is worth it, you can buy them a beer in return. Windward Studios
 * ----------------------------------------------------------------------------
 */

package net.windward.Windwardopolis2.api;

import org.dom4j.Element;

import java.awt.*;
import java.util.ArrayList;

public class CoffeeStore {

    private CoffeeStore(Element elemCompany) {
        this.name = elemCompany.attributeValue("name");
        busStop = new Point(Integer.parseInt(elemCompany.attributeValue("bus-stop-x")),
                            Integer.parseInt(elemCompany.attributeValue("bus-stop-y")));
    }

    private String name;
    public String getName() {
        return name;
    }

    private Point busStop;
    public Point getBusStop() {
        return busStop;
    }

    public static java.util.ArrayList<CoffeeStore> FromXml(Element elemStores) {
        java.util.ArrayList<CoffeeStore> stores = new ArrayList<CoffeeStore>();
        java.util.List<Element> storeElements = elemStores.elements("store");

        for(Element elemStoreOn : storeElements) {
            stores.add(new CoffeeStore(elemStoreOn));
        }

        return stores;
    }

    public String toString() {
        return name + "; " + busStop.toString();
    }
}
