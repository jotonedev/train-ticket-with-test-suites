package foodsearch.entity;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class TrainFood implements Serializable
{
    private UUID id;

    private String tripId;

    private List<Food> foodList;

    public TrainFood()
    {
        //Default Constructor
    }
}
