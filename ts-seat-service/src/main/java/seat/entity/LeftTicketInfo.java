package seat.entity;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author fdse
 */
@Data
public class LeftTicketInfo
{
    @Valid
    @NotNull
    private Set<Ticket> soldTickets;

    public LeftTicketInfo()
    {
        //Default Constructor
    }

    @Override
    public String toString()
    {
        return "LeftTicketInfo{" +
            "soldTickets=" + soldTickets +
            '}';
    }
}
