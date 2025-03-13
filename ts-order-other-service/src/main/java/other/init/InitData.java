package other.init;

import other.entity.*;
import other.service.OrderOtherService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * @author fdse
 */
@Component
public class InitData implements CommandLineRunner
{
    @Autowired
    OrderOtherService service;

    @Override
    public void run(String... args) throws Exception
    {

        Order order1 = new Order();

        order1.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        order1.setBoughtDate(new Date(System.currentTimeMillis()));
        order1.setCoachNumber(5);
        order1.setContactsDocumentNumber("Test");
        order1.setContactsName("Test");
        order1.setDocumentType(1);
        order1.setFrom("shanghai");
        order1.setId(UUID.fromString("4d2a46c7-71cb-4cf1-c5bb-b68406d9da6f"));
        order1.setPrice("100");
        order1.setSeatClass(SeatClass.FIRSTCLASS.getCode());
        order1.setSeatNumber("6A");
        order1.setStatus(OrderStatus.PAID.getCode());
        order1.setTo("taiyuan");
        order1.setTrainNumber("K1235");
        order1.setTravelDate(new Date(123456799));
        order1.setTravelTime(new Date(123456799));
        service.initOrder(order1, null);

        insertForTravel();
        insertForPreserve();
        insertForRebook();
        insertForInsidePayment();
        insertForCancel();
        insertForExecute();
    }

    private void insertForTravel() {
        Order order2 = new Order();
        order2.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        order2.setBoughtDate(new Date(System.currentTimeMillis()));
        order2.setCoachNumber(5);
        order2.setContactsDocumentNumber("Test");
        order2.setContactsName("Test");
        order2.setDocumentType(1);
        order2.setFrom("shanghai");
        order2.setId(UUID.fromString("8f6fc570-049d-4b1c-8e9e-a0ab520fc58b"));
        order2.setPrice("100");
        order2.setSeatClass(SeatClass.FIRSTCLASS.getCode());
        order2.setSeatNumber("6A");
        order2.setStatus(OrderStatus.PAID.getCode());
        order2.setTo("taiyuan");
        order2.setTrainNumber("Z1234");
        order2.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 3025"));
        order2.setTravelTime(new Date("Mon May 04 09:00:00 GMT+0800 2013"));
        service.initOrder(order2, null);
    }

    private void insertForPreserve() {
        Order order = new Order();
        order.setId(UUID.fromString("5ad7750b-a68b-49c0-a8c0-32776b067703"));
        order.setBoughtDate(new Date());
        order.setTravelDate(new Date(2026, Calendar.JANUARY, 1));
        order.setTravelTime(new Date(2026, Calendar.JANUARY, 1));
        order.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        order.setContactsName("Contacts_One");
        order.setDocumentType(1);
        order.setContactsDocumentNumber("DocumentNumber_One");
        order.setTrainNumber("K1432");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber("FirstClass-30");
        order.setFrom("nanjing");
        order.setTo("shanghai");
        order.setStatus(0);
        order.setPrice("100.0");
        service.initOrder(order, null);
    }

    private void insertForRebook() {
        Order order = new Order();
        order.setId(UUID.fromString("5ac7750c-a68c-49c0-a8c0-32776c067703"));
        order.setBoughtDate(new Date());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        order.setContactsName("Contacts_One");
        order.setDocumentType(1);
        order.setContactsDocumentNumber("DocumentNumber_One");
        order.setTrainNumber("Z1237");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber("FirstClass-30");
        order.setFrom("nanjing");
        order.setTo("shanghai");
        order.setStatus(1);
        order.setPrice("100.0");
        service.initOrder(order, null);
    }

    private void insertForInsidePayment() {
        Order orderNotPaid = new Order();

        orderNotPaid.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        orderNotPaid.setBoughtDate(new Date(System.currentTimeMillis()));
        orderNotPaid.setCoachNumber(5);
        orderNotPaid.setContactsDocumentNumber("Test");
        orderNotPaid.setContactsName("Test");
        orderNotPaid.setDocumentType(1);
        orderNotPaid.setFrom("shanghai");
        orderNotPaid.setId(UUID.fromString("4d2a46c7-70cb-4cf1-c5bb-b68406d9da6e"));
        orderNotPaid.setPrice("100");
        orderNotPaid.setSeatClass(SeatClass.FIRSTCLASS.getCode());
        orderNotPaid.setSeatNumber("6A");
        orderNotPaid.setStatus(OrderStatus.NOTPAID.getCode());
        orderNotPaid.setTo("taiyuan");
        orderNotPaid.setTrainNumber("K1235");
        orderNotPaid.setTravelDate(new Date(123456799));
        orderNotPaid.setTravelTime(new Date(123456799));
        service.initOrder(orderNotPaid, null);

        Order orderNotPaid2 = new Order();

        orderNotPaid2.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        orderNotPaid2.setBoughtDate(new Date(System.currentTimeMillis()));
        orderNotPaid2.setCoachNumber(5);
        orderNotPaid2.setContactsDocumentNumber("Test");
        orderNotPaid2.setContactsName("Test");
        orderNotPaid2.setDocumentType(1);
        orderNotPaid2.setFrom("shanghai");
        orderNotPaid2.setId(UUID.fromString("4d2a46c7-70cb-4ce1-c5bb-b68406d9da6e"));
        orderNotPaid2.setPrice("100");
        orderNotPaid2.setSeatClass(SeatClass.FIRSTCLASS.getCode());
        orderNotPaid2.setSeatNumber("6A");
        orderNotPaid2.setStatus(OrderStatus.NOTPAID.getCode());
        orderNotPaid2.setTo("taiyuan");
        orderNotPaid2.setTrainNumber("K1235");
        orderNotPaid2.setTravelDate(new Date(123456799));
        orderNotPaid2.setTravelTime(new Date(123456799));
        service.initOrder(orderNotPaid2, null);
    }

    private void insertForCancel() {
        Order order1 = new Order();

        order1.setAccountId(UUID.fromString("f8e2dc60-bd59-4af9-bf15-507f3b6572d6"));
        order1.setBoughtDate(new Date(System.currentTimeMillis()));
        order1.setCoachNumber(5);
        order1.setContactsDocumentNumber("Test");
        order1.setContactsName("Test");
        order1.setDocumentType(1);
        order1.setFrom("shanghai");
        order1.setId(UUID.fromString("f8e2dc60-bd59-4af9-bf15-507f3b6572d6"));
        order1.setPrice("100");
        order1.setSeatClass(SeatClass.FIRSTCLASS.getCode());
        order1.setSeatNumber("6A");
        order1.setStatus(0);
        order1.setTo("taiyuan");
        order1.setTrainNumber("K1235");
        order1.setTravelDate(new Date(123456799));
        order1.setTravelTime(new Date(123456799));
        service.initOrder(order1, null);

        Order order2 = new Order();

        order2.setAccountId(UUID.fromString("f8e2dc60-bd59-4af9-bf15-507f3b6572d5"));
        order2.setBoughtDate(new Date(System.currentTimeMillis()));
        order2.setCoachNumber(5);
        order2.setContactsDocumentNumber("Test");
        order2.setContactsName("Test");
        order2.setDocumentType(1);
        order2.setFrom("shanghai");
        order2.setId(UUID.fromString("f8e2dc60-bd59-4af9-bf15-507f3b6572d5"));
        order2.setPrice("100");
        order2.setSeatClass(SeatClass.FIRSTCLASS.getCode());
        order2.setSeatNumber("6A");
        order2.setStatus(1);
        order2.setTo("taiyuan");
        order2.setTrainNumber("Z1234");
        order2.setTravelDate(new Date(123456799));
        order2.setTravelTime(new Date(123456799));
        service.initOrder(order2, null);

        Order order3 = new Order();

        order3.setAccountId(UUID.fromString("f8e2dc60-bd59-4af9-bf15-507f3b6572d4"));
        order3.setBoughtDate(new Date(System.currentTimeMillis()));
        order3.setCoachNumber(5);
        order3.setContactsDocumentNumber("Test");
        order3.setContactsName("Test");
        order3.setDocumentType(1);
        order3.setFrom("shanghai");
        order3.setId(UUID.fromString("f8e2dc60-bd59-4af9-bf15-507f3b6572d4"));
        order3.setPrice("100");
        order3.setSeatClass(SeatClass.FIRSTCLASS.getCode());
        order3.setSeatNumber("6A");
        order3.setStatus(2);
        order3.setTo("taiyuan");
        order3.setTrainNumber("Z1234");
        order3.setTravelDate(new Date(123456799));
        order3.setTravelTime(new Date(123456799));
        service.initOrder(order3, null);
    }

    private void insertForExecute() {
        Order orderStatusPaid = new Order();

        orderStatusPaid.setAccountId(UUID.fromString("4d2a46c7-61cb-4cf1-b5bb-b68406d9da6f"));
        orderStatusPaid.setBoughtDate(new Date(System.currentTimeMillis()));
        orderStatusPaid.setCoachNumber(5);
        orderStatusPaid.setContactsDocumentNumber("Test");
        orderStatusPaid.setContactsName("Test");
        orderStatusPaid.setDocumentType(1);
        orderStatusPaid.setFrom("shanghai");
        orderStatusPaid.setId(UUID.fromString("4d2a46c7-71cb-4cf1-c5bb-b68406d9da6f"));
        orderStatusPaid.setPrice("100");
        orderStatusPaid.setSeatClass(SeatClass.FIRSTCLASS.getCode());
        orderStatusPaid.setSeatNumber("6A");
        orderStatusPaid.setStatus(OrderStatus.PAID.getCode());
        orderStatusPaid.setTo("taiyuan");
        orderStatusPaid.setTrainNumber("K1235");
        orderStatusPaid.setTravelDate(new Date(123456799));
        orderStatusPaid.setTravelTime(new Date(123456799));
        service.initOrder(orderStatusPaid, null);

        Order orderStatusCollected = new Order();
        orderStatusCollected.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6e"));
        orderStatusCollected.setBoughtDate(new Date(System.currentTimeMillis()));
        orderStatusCollected.setCoachNumber(5);
        orderStatusCollected.setContactsDocumentNumber("Test");
        orderStatusCollected.setContactsName("Test");
        orderStatusCollected.setDocumentType(1);
        orderStatusCollected.setFrom("shanghai");
        orderStatusCollected.setId(UUID.fromString("4d2a46c7-71cb-4cf1-a5bb-b68406d9da6e"));
        orderStatusCollected.setPrice("100");
        orderStatusCollected.setSeatClass(SeatClass.FIRSTCLASS.getCode());
        orderStatusCollected.setSeatNumber("6A");
        orderStatusCollected.setStatus(OrderStatus.COLLECTED.getCode());
        orderStatusCollected.setTo("taiyuan");
        orderStatusCollected.setTrainNumber("K1235");
        orderStatusCollected.setTravelDate(new Date(123456799));
        orderStatusCollected.setTravelTime(new Date(123456799));
        service.initOrder(orderStatusCollected, null);
    }
}
