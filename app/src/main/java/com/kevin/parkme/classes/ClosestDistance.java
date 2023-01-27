package com.kevin.parkme.classes;

import java.util.Comparator;

public class ClosestDistance {
    public double distance;
    public ParkingArea parkingArea;
    public String key;

    public ClosestDistance(){}
    public ClosestDistance(double distance, ParkingArea parkingArea,String key){
        this.distance=distance;
        this.parkingArea=parkingArea;
        this.key=key;
    }

    public static Comparator<ClosestDistance> ClosestDistComparator = new Comparator<ClosestDistance>() {
        public int compare(ClosestDistance s1, ClosestDistance s2) {
            /*ascending order*/
            return Double.compare(s1.distance,s2.distance);
        }
    };
}
