package seat.entity;

import javax.validation.Valid;

import lombok.Data;

/**
 * @author fdse
 */
@Data
public class TrainType
{
    @Valid
    private String id;

    @Valid
    private int economyClass;

    @Valid
    private int confortClass;

    private int averageSpeed;

    public TrainType()
    {
        //Default Constructor
    }

    public TrainType(String id, int economyClass, int confortClass)
    {
        this.id = id;
        this.economyClass = economyClass;
        this.confortClass = confortClass;
    }

    public TrainType(String id, int economyClass, int confortClass, int averageSpeed)
    {
        this.id = id;
        this.economyClass = economyClass;
        this.confortClass = confortClass;
        this.averageSpeed = averageSpeed;
    }
}
