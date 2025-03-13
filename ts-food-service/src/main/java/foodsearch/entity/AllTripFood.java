package foodsearch.entity;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class AllTripFood
{
    private List<TrainFood> trainFoodList;

    private Map<String, List<FoodStore>> foodStoreListMap;

    public AllTripFood()
    {
        //Default Constructor
    }
}
