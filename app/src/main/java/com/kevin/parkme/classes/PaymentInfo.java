package com.kevin.parkme.classes;

public class PaymentInfo {
    public String month, year, cardNumber, cvv;

    public PaymentInfo(){}

    public PaymentInfo(String month, String year, String cardNumber, String cvv){
        this.month=month;
        this.year=year;
        this.cardNumber=cardNumber;
        this.cvv=cvv;
    }
}
