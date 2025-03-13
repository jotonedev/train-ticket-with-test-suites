package order.init;

import order.entity.Order;
import order.service.OrderService;

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
    OrderService service;

    String accountId = "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f";

    String contactName = "Contacts_One";

    String contactDocumentNumber = "DocumentNumber_One";

    String firstClass = "FirstClass-30";

    String price = "100.0";

    @Override
    public void run(String... args) throws Exception
    {
        Order order = new Order();
        order.setId(UUID.fromString("5ad7750b-a68b-49c0-a8c0-32776b067703"));
        order.setBoughtDate(new Date());
        order.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        order.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013")); //NOSONAR
        order.setAccountId(UUID.fromString(accountId));
        order.setContactsName(contactName);
        order.setDocumentType(1);
        order.setContactsDocumentNumber(contactDocumentNumber);
        order.setTrainNumber("G1237");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber(firstClass);
        order.setFrom("nanjing");
        order.setTo("shanghaihongqiao");
        order.setStatus(0);
        order.setPrice(price);
        service.initOrder(order, null);

        Order orderTwo = new Order();
        orderTwo.setId(UUID.fromString("8177ac5a-61ac-42f4-83f4-bd7b394d0531"));
        orderTwo.setBoughtDate(new Date());
        orderTwo.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        orderTwo.setTravelTime(new Date("Mon May 04 09:01:00 GMT+0800 2013")); //NOSONAR
        orderTwo.setAccountId(UUID.fromString(accountId));
        orderTwo.setContactsName(contactName);
        orderTwo.setDocumentType(1);
        orderTwo.setContactsDocumentNumber(contactDocumentNumber);
        orderTwo.setTrainNumber("G1234");
        orderTwo.setCoachNumber(5);
        orderTwo.setSeatClass(2);
        orderTwo.setSeatNumber(firstClass);
        orderTwo.setFrom("shanghai");
        orderTwo.setTo("beijing");
        orderTwo.setStatus(0);
        orderTwo.setPrice(price);
        service.initOrder(orderTwo, null);

        Order orderThree = new Order();
        orderThree.setId(UUID.fromString("d3c91694-d5b8-424c-9974-e14c89226e49"));
        orderThree.setBoughtDate(new Date());
        orderThree.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        orderThree.setTravelTime(new Date("Mon May 04 09:00:00 GMT+0800 2013")); //NOSONAR
        orderThree.setAccountId(UUID.fromString(accountId));
        orderThree.setContactsName(contactName);
        orderThree.setDocumentType(1);
        orderThree.setContactsDocumentNumber(contactDocumentNumber);
        orderThree.setTrainNumber("G1235");
        orderThree.setCoachNumber(5);
        orderThree.setSeatClass(2);
        orderThree.setSeatNumber(firstClass);
        orderThree.setFrom("shanghai");
        orderThree.setTo("beijing");
        orderThree.setStatus(0);
        orderThree.setPrice(price);
        service.initOrder(orderThree, null);

        insertForTravel();
        insertForPreserve();
        insertForRebook();
        insertForInsidePayment();
        insertForCancel();
        insertForExecute();
    }

    private void insertForTravel() {
        Order orderNewDate = new Order();
        orderNewDate.setId(UUID.fromString("8177ac5a-61ac-42f4-83f4-bd7b394d9999"));
        orderNewDate.setBoughtDate(new Date());
        orderNewDate.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 3025")); //NOSONAR
        orderNewDate.setTravelTime(new Date("Mon May 04 09:00:00 GMT+0800 2013")); //NOSONAR
        orderNewDate.setAccountId(UUID.fromString(accountId));
        orderNewDate.setContactsName(contactName);
        orderNewDate.setDocumentType(1);
        orderNewDate.setContactsDocumentNumber(contactDocumentNumber);
        orderNewDate.setTrainNumber("G1234");
        orderNewDate.setCoachNumber(5);
        orderNewDate.setSeatClass(2);
        orderNewDate.setSeatNumber(firstClass);
        orderNewDate.setFrom("shanghai");
        orderNewDate.setTo("beijing");
        orderNewDate.setStatus(1);
        orderNewDate.setPrice(price);
        service.initOrder(orderNewDate, null);
    }

    private void insertForPreserve() {
        Order order = new Order();
        order.setId(UUID.fromString("5ad7750b-a68b-49c0-a8c0-32776b067703"));
        order.setBoughtDate(new Date());
        order.setTravelDate(new Date(2026, Calendar.JANUARY, 1));
        order.setTravelTime(new Date(2026, Calendar.JANUARY, 1));
        order.setAccountId(UUID.fromString(accountId));
        order.setContactsName(contactName);
        order.setDocumentType(1);
        order.setContactsDocumentNumber(contactDocumentNumber);
        order.setTrainNumber("G1432");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber(firstClass);
        order.setFrom("nanjing");
        order.setTo("shanghai");
        order.setStatus(0);
        order.setPrice(price);
        service.initOrder(order, null);
    }

    private void insertForRebook() {
        Order order = new Order();
        order.setId(UUID.fromString("5ac7750c-a68c-49c0-a8c0-32776c067703"));
        order.setBoughtDate(new Date());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setAccountId(UUID.fromString(accountId));
        order.setContactsName(contactName);
        order.setDocumentType(1);
        order.setContactsDocumentNumber(contactDocumentNumber);
        order.setTrainNumber("G9997");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber(firstClass);
        order.setFrom("nanjing");
        order.setTo("shanghai");
        order.setStatus(1);
        order.setPrice(price);
        service.initOrder(order, null);

        Order order2 = new Order();
        order2.setId(UUID.fromString("5ac7750c-a68c-49c0-a8c0-32776c067704"));
        order2.setBoughtDate(new Date());
        order2.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        order2.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013")); //NOSONAR
        order2.setAccountId(UUID.fromString(accountId));
        order2.setContactsName(contactName);
        order2.setDocumentType(1);
        order2.setContactsDocumentNumber(contactDocumentNumber);
        order2.setTrainNumber("G9998");
        order2.setCoachNumber(5);
        order2.setSeatClass(2);
        order2.setSeatNumber(firstClass);
        order2.setFrom("nanjing");
        order2.setTo("shanghai");
        order2.setStatus(1);
        order2.setPrice(price);
        service.initOrder(order2, null);

        Order order3 = new Order();
        order3.setId(UUID.fromString("5ac7750c-a68c-49c0-a8c0-32776c067705"));
        order3.setBoughtDate(new Date());
        order3.setTravelDate(2026, 1, 1); //NOSONAR
        order3.setTravelTime(new Date("Mon Jan 01 09:02:00 GMT+0800 2026")); //NOSONAR
        order3.setAccountId(UUID.fromString(accountId));
        order3.setContactsName(contactName);
        order3.setDocumentType(1);
        order3.setContactsDocumentNumber(contactDocumentNumber);
        order3.setTrainNumber("G1234");
        order3.setCoachNumber(5);
        order3.setSeatClass(2);
        order3.setSeatNumber(firstClass);
        order3.setFrom("nanjing");
        order3.setTo("shanghai");
        order3.setStatus(1);
        order3.setPrice(price);
        service.initOrder(order3, null);

        Order order4 = new Order();
        order4.setId(UUID.fromString("5ad7750b-a68b-49c0-a8c0-32776b067701"));
        order4.setBoughtDate(new Date());
        order4.setTravelDate(2026, 1, 1); //NOSONAR
        order4.setTravelTime(new Date("Mon Jan 01 09:02:00 GMT+0800 2026")); //NOSONAR
        order4.setAccountId(UUID.fromString(accountId));
        order4.setContactsName(contactName);
        order4.setDocumentType(1);
        order4.setContactsDocumentNumber(contactDocumentNumber);
        order4.setTrainNumber("G1234");
        order4.setCoachNumber(5);
        order4.setSeatClass(2);
        order4.setSeatNumber(firstClass);
        order4.setFrom("nanjing");
        order4.setTo("shanghai");
        order4.setStatus(1);
        order4.setPrice(price);
        service.initOrder(order4, null);
    }

    private void insertForInsidePayment() {
        Order orderNotPaid = new Order();
        orderNotPaid.setId(UUID.fromString("5ad6650b-a68b-49c0-a8c0-32776b067703"));
        orderNotPaid.setBoughtDate(new Date());
        orderNotPaid.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        orderNotPaid.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013")); //NOSONAR
        orderNotPaid.setAccountId(UUID.fromString(accountId));
        orderNotPaid.setContactsName(contactName);
        orderNotPaid.setDocumentType(1);
        orderNotPaid.setContactsDocumentNumber(contactDocumentNumber);
        orderNotPaid.setTrainNumber("G1237");
        orderNotPaid.setCoachNumber(5);
        orderNotPaid.setSeatClass(2);
        orderNotPaid.setSeatNumber(firstClass);
        orderNotPaid.setFrom("nanjing");
        orderNotPaid.setTo("shanghaihongqiao");
        orderNotPaid.setStatus(0);
        orderNotPaid.setPrice(price);
        service.initOrder(orderNotPaid, null);
    }

    private void insertForCancel() {
        Order orderTwo = new Order();
        orderTwo.setId(UUID.fromString("5f50d821-5f22-44f6-b2de-5e79d4b29c68"));
        orderTwo.setBoughtDate(new Date());
        orderTwo.setTravelDate(new Date(257089287)); //NOSONAR
        orderTwo.setTravelTime(new Date(123456799)); //NOSONAR
        orderTwo.setAccountId(UUID.fromString(accountId));
        orderTwo.setContactsName(contactName);
        orderTwo.setDocumentType(1);
        orderTwo.setContactsDocumentNumber(contactDocumentNumber);
        orderTwo.setTrainNumber("G1234");
        orderTwo.setCoachNumber(5);
        orderTwo.setSeatClass(2);
        orderTwo.setSeatNumber(firstClass);
        orderTwo.setFrom("shanghai");
        orderTwo.setTo("beijing");
        orderTwo.setStatus(1);
        orderTwo.setPrice(price);
        service.initOrder(orderTwo, null);

        Order orderThree = new Order();
        orderThree.setId(UUID.fromString("f8e2dc60-bd59-4af9-bf15-507f3b6572d7"));
        orderThree.setBoughtDate(new Date());
        orderThree.setTravelDate(new Date(257089287)); //NOSONAR
        orderThree.setTravelTime(new Date(123456799)); //NOSONAR
        orderThree.setAccountId(UUID.fromString(accountId));
        orderThree.setContactsName(contactName);
        orderThree.setDocumentType(1);
        orderThree.setContactsDocumentNumber(contactDocumentNumber);
        orderThree.setTrainNumber("G1235");
        orderThree.setCoachNumber(5);
        orderThree.setSeatClass(2);
        orderThree.setSeatNumber(firstClass);
        orderThree.setFrom("shanghai");
        orderThree.setTo("beijing");
        orderThree.setStatus(2);
        orderThree.setPrice(price);
        service.initOrder(orderThree, null);

        Order orderFour = new Order();
        orderFour.setId(UUID.fromString("e9cb042e-54d3-4b56-b5fa-67ff1c1c992b"));
        orderFour.setBoughtDate(new Date());
        orderFour.setTravelDate(new Date(257089287)); //NOSONAR
        orderFour.setTravelTime(new Date(123456799)); //NOSONAR
        orderFour.setAccountId(UUID.fromString(accountId));
        orderFour.setContactsName(contactName);
        orderFour.setDocumentType(1);
        orderFour.setContactsDocumentNumber(contactDocumentNumber);
        orderFour.setTrainNumber("G1235");
        orderFour.setCoachNumber(5);
        orderFour.setSeatClass(0);
        orderFour.setSeatNumber(firstClass);
        orderFour.setFrom("shanghai");
        orderFour.setTo("beijing");
        orderFour.setStatus(0);
        orderFour.setPrice(price);
        service.initOrder(orderFour, null);

        Order orderFive = new Order();
        orderFive.setId(UUID.fromString("e9cb042e-54d3-4b56-b5fa-67ff1c1c992c"));
        orderFive.setBoughtDate(new Date());
        orderFive.setTravelDate(new Date(257089287)); //NOSONAR
        orderFive.setTravelTime(new Date(123456799)); //NOSONAR
        orderFive.setAccountId(UUID.randomUUID());
        orderFive.setContactsName(contactName);
        orderFive.setDocumentType(1);
        orderFive.setContactsDocumentNumber(contactDocumentNumber);
        orderFive.setTrainNumber("G1235");
        orderFive.setCoachNumber(5);
        orderFive.setSeatClass(0);
        orderFive.setSeatNumber(firstClass);
        orderFive.setFrom("shanghai");
        orderFive.setTo("beijing");
        orderFive.setStatus(0);
        orderFive.setPrice(price);
        service.initOrder(orderFive, null);
    }

    private void insertForExecute() {
        Order orderStatusPaid = new Order();
        orderStatusPaid.setId(UUID.fromString("5ad7750b-a67b-49c0-a8c0-32776b067701"));
        orderStatusPaid.setBoughtDate(new Date());
        orderStatusPaid.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017"));
        orderStatusPaid.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013"));
        orderStatusPaid.setAccountId(UUID.fromString(accountId));
        orderStatusPaid.setContactsName(contactName);
        orderStatusPaid.setDocumentType(1);
        orderStatusPaid.setContactsDocumentNumber(contactDocumentNumber);
        orderStatusPaid.setTrainNumber("G1237");
        orderStatusPaid.setCoachNumber(5);
        orderStatusPaid.setSeatClass(2);
        orderStatusPaid.setSeatNumber(firstClass);
        orderStatusPaid.setFrom("nanjing");
        orderStatusPaid.setTo("shanghaihongqiao");
        orderStatusPaid.setStatus(1);
        orderStatusPaid.setPrice(price);
        service.initOrder(orderStatusPaid, null);

        Order orderStatusCollected = new Order();
        orderStatusCollected.setId(UUID.fromString("d3c91694-d5b8-424c-8674-e14c89226e49"));
        orderStatusCollected.setBoughtDate(new Date());
        orderStatusCollected.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017")); //NOSONAR
        orderStatusCollected.setTravelTime(new Date("Mon May 04 09:00:00 GMT+0800 2013")); //NOSONAR
        orderStatusCollected.setAccountId(UUID.fromString(accountId));
        orderStatusCollected.setContactsName(contactName);
        orderStatusCollected.setDocumentType(1);
        orderStatusCollected.setContactsDocumentNumber(contactDocumentNumber);
        orderStatusCollected.setTrainNumber("G1235");
        orderStatusCollected.setCoachNumber(5);
        orderStatusCollected.setSeatClass(2);
        orderStatusCollected.setSeatNumber(firstClass);
        orderStatusCollected.setFrom("shanghai");
        orderStatusCollected.setTo("beijing");
        orderStatusCollected.setStatus(2);
        orderStatusCollected.setPrice(price);
        service.initOrder(orderStatusCollected, null);
    }
}
