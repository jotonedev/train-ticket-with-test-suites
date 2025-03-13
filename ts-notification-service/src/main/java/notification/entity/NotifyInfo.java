package notification.entity;

import lombok.Data;

/**
 * @author fdse
 */
@Data
public class NotifyInfo
{
    private String email;

    private String orderNumber;

    private String username;

    private String startingPlace;

    private String endPlace;

    private String startingTime;

    private String date;

    private String seatClass;

    private String seatNumber;

    private String price;

    public NotifyInfo()
    {
        //Default Constructor
    }
}
