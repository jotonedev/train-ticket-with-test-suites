package plan.entity;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author fdse
 */
@Data
public class RoutePlanInfo {
    @NotNull(message = "From Station Name cannot be null")
    private String formStationName;

    @NotNull(message = "To Station Name cannot be null")
    private String toStationName;

    @NotNull(message = "Travel Date cannot be null")
    private Date travelDate;

    @NotNull(message = "Number of passengers cannot be null")
    private Integer num;

    public RoutePlanInfo() {
        //Empty Constructor
    }

    public RoutePlanInfo(String formStationName, String toStationName, Date travelDate, int num) {
        this.formStationName = formStationName;
        this.toStationName = toStationName;
        this.travelDate = travelDate;
        this.num = num;
    }
}
