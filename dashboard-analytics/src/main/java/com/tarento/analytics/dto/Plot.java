package com.tarento.analytics.dto;

import java.math.BigDecimal;

public class Plot {

    private String label;
    private String name;
    private Double value;
    private String symbol;

    public Plot(String name, Double value, String symbol) {
        this.name = name;
        this.value = value;
        this.symbol = symbol;
    }
     public Plot()
     {
    	 
     }

    public String getName() {
        return name;
    }

    public Double getValue() {
        return value;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    public void setValue(Double value) {
        this.value = value;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public void print() {
    	System.out.println("\t"+this.name + " , " + this.value + " , " +this.symbol + " , "+this.label);
    }
    
}
