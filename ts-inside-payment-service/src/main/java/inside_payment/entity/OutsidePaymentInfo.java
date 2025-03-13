package inside_payment.entity;

import lombok.Data;

/**
 * @author fdse
 */
@Data
public class OutsidePaymentInfo
{
    private String orderId;

    private String price;

    private String userId;

    public OutsidePaymentInfo()
    {
        //Default Constructor
    }
}
