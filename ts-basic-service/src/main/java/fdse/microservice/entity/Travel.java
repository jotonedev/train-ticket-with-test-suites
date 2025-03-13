package fdse.microservice.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author fdse
 */
@Data
@AllArgsConstructor
public class Travel
{
    private Trip trip;

    private String startingPlace;

    private String endPlace;

    private Date departureTime;

    public Travel()
    {
        //Default Constructor
    }
}
