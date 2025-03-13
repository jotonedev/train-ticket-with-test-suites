package foodsearch.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class Food implements Serializable
{
    private String foodName;

    private double price;

    public Food()
    {
        //Default Constructor
    }
}
