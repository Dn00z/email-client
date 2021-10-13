package com.hgdnimantha;

//Dasun Nimantha        190415K

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Email_Client {

    public static void main(String[] args) {

        System.out.println("Welcome.\nPlease wait for few minutes...\n");
        /*create EmailClientHandler for one user, if we want we can use this application for multiple users
        by developing more.
         */
        EmailClientHandler emailClientHandler = new EmailClientHandler();
        emailClientHandler.deserialize(); //deserialize older emails that were sent
        emailClientHandler.loadNCreateRecipient(); //firstly load the previous recipients to the application
        emailClientHandler.findBirthdayRecipients(); //find recipients who has birthday in current day
        emailClientHandler.sendBirthdayWishes(); //send birthday greetings to recipients
        System.out.println("Thank you for waiting.\n");

        Scanner scanner = new Scanner(System.in); //to get user inputs
        while (true) { //use a loop to do multiple activities in one execution
            System.out.println("Enter option type: \n"
                    + "1 - Adding a new recipient\n"
                    + "2 - Sending an email\n"
                    + "3 - Printing out all the recipients who have birthdays\n"
                    + "4 - Printing out details of all the emails sent\n"
                    + "5 - Printing out the number of recipient objects in the application\n"
                    + "6 - Exit the application\n");


            int option = scanner.nextInt();
            switch (option) {

                case 1:
                    System.out.println("Enter recipient details as follows: \n"
                            + "For adding official recipient        -> Official: <name>,<e-mail address>,<designation>\n"
                            + "For adding official friend recipient -> Office_friend: <name>,<e-mail address>,<designation>,<birthday>\n"
                            + "For adding personal recipient        -> Personal: <name>,<nick name>,<e-mail address>,<birthday>");
                    scanner.nextLine();
                    String recipientData = scanner.nextLine();
                    emailClientHandler.addingRecipient(recipientData); //adding new recipient to the application
                    break;

                case 2:
                    System.out.println("Enter details in following format \n Email,Subject,Content");
                    scanner.nextLine();
                    String details = scanner.nextLine();
                    emailClientHandler.sendNewMail(details); //send new mail to the given mail address
                    break;

                case 3:
                    System.out.println("Enter a date in following format\n yyyy/MM/dd:");
                    scanner.nextLine();
                    String date = scanner.nextLine();
                    emailClientHandler.printRecipients(date); //print recipients who have birthdays on given date
                    break;

                case 4:
                    System.out.println("Enter a date in following format\n yyyy/MM/dd:");
                    scanner.nextLine();
                    String date1 = scanner.nextLine();
                    emailClientHandler.printMails(date1); //printing mails that were sent on given date
                    break;
                case 5:
                    System.out.println(Recipient.getRecipientCount()); //print number of recipients in the application
                    break;
                case 6:
                    emailClientHandler.serialize(); //serialize the mail objects
                    emailClientHandler.setClosedTrue();
                    return; //to exit the application
                default:
                    System.out.println("Invalid input");
            }
        }
    }
}

class EmailClientHandler { //this is used to handle recipient objects and mail objects in the application

    private ArrayList<Mail> mailObjects; // store sent mail objects
    private ArrayList<Recipient> recipients; // store the all recipients objects
    private ArrayList<BirthdayWishable> birthdayWishables; //store recipients who can send birthday greetings
    private MyBlockingQueue blockingQueue; //blocking queue for store received mail objects
    private EmailReceiver receiver; //mail receiving
    private Serializer serializer;  //serializing
    private Thread receiverThread; //create new thread to receiving
    private Thread serializingThread; //create new thread to serializing

    public EmailClientHandler() {
        mailObjects = new ArrayList<>();
        recipients = new ArrayList<>();
        birthdayWishables = new ArrayList<>();
        blockingQueue = new MyBlockingQueue(2);
        receiver = new EmailReceiver(blockingQueue);
        serializer = new Serializer(blockingQueue);
        receiverThread = new Thread(receiver, "receiverThread");
        serializingThread = new Thread(serializer,"serializingThread");
        receiverThread.start();
        serializingThread.start();
    }
    public void setClosedTrue() {
        receiver.setClosed(true);
    }

    public Recipient createRecipient(String recipientData) { //create recipients
        Recipient newRecipient = null;
        try {
            String[] details = recipientData.split(":")[1].trim().split(",");

            if (recipientData.startsWith("Official")) { //create official recipients
                OfficialRecipient recipient = new OfficialRecipient(details[0], details[1], details[2], "Offcial");
                newRecipient = (Recipient) recipient;
                recipients.add(recipient);

            }
            else if (recipientData.startsWith("Office_friend")) { //create office friends
                OfficialCloseRecipients recipient = new OfficialCloseRecipients(details[0], details[1], details[2], details[3], "Office_friend");
                newRecipient = (Recipient) recipient;
                recipients.add(recipient);
            }
            else if (recipientData.startsWith("Personal")) { //create personal recipients
                PersonalRecipient recipient = new PersonalRecipient(details[0], details[1], details[2], details[3], "Personal");
                newRecipient = (Recipient) recipient;
                recipients.add(recipient);
            }
        }
        catch (ArrayIndexOutOfBoundsException exception) {
            System.out.println("Invalid Input");
        }
        return newRecipient;
    }

    public void serialize() { //serialize array list of mail objects
        try(FileOutputStream output = new FileOutputStream("input.txt");
            ObjectOutputStream out = new ObjectOutputStream(output);){
            out.writeObject(mailObjects);
        }
        catch (IOException ioException){
            System.out.println("Something went wrong(can not be serialized), please try again");
        }
    }

    public void deserialize() { //deserialize array list of mail objects
        try (FileInputStream input = new FileInputStream("input.txt");
             ObjectInputStream in = new ObjectInputStream(input);){
            ArrayList<Mail> deserializedMail = (ArrayList<Mail>) in.readObject();
            mailObjects.addAll(deserializedMail);
        }
        catch (IOException ioException) {}

        catch (ClassNotFoundException classNotFoundException) {
            System.out.println("Something went wrong");
        }
    }
    public void storeRecipientDetails(String recipientData) { //store recipient details into a file
        try (FileWriter fw = new FileWriter("clientList.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter clientList = new PrintWriter(bw);){
            clientList.println(recipientData);
        }
        catch (IOException ioException) {
            System.out.println("Something went wrong(client.txt can not be created), please try again");
        }
    }

    public void loadNCreateRecipient() { //load details that were saved and create recipients
        try (FileReader fr = new FileReader("clientList.txt");
             BufferedReader reader = new BufferedReader(fr);){
            String line;
            while ((line = reader.readLine()) != null) {
                createRecipient(line);
            }
        }
        catch (IOException ioException) {
            System.out.println("Something went wrong(client.txt can not be found), please try again");
        }

    }

    public void findBirthdayRecipients() { //find recipients who have birthdays
        for (Recipient recipientObj : recipients) {
            if (recipientObj instanceof BirthdayWishable) {
                birthdayWishables.add((BirthdayWishable) recipientObj);
            }
        }
    }

    public void sendBirthdayWishes()  { //send birthday wishes
        System.out.println("Preparing to send birthday wishes\n");
        LocalDate todayDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY/MM/dd");
        String today = formatter.format(todayDate);

        for (BirthdayWishable friend : birthdayWishables) {
            if ((friend.getBirthday().substring(5)).equals(today.substring(5))) {
                Recipient tempRecipient = (Recipient) friend;
                Mail newWish = new Mail("Birthday Greeting", friend.createWishBody(tempRecipient.getName()));
                mailObjects.add(newWish);
                newWish.sendMail(newWish, tempRecipient.getEmailAddress());
            }
        }
        System.out.println("Wishes sent successfully\n");
    }
    public void sendNewMail(String details) { //send new mail
        String[] detailList = details.split(",");
        try {
            Mail newMail = new Mail(detailList[1], detailList[2]);
            mailObjects.add(newMail);
            System.out.println("Preparing to send your mail please wait...\n");
            newMail.sendMail(newMail, detailList[0]);
            System.out.println("Mail sent successfully\n");
        } catch (ArrayIndexOutOfBoundsException exception) {
            System.out.println("Invalid Input");
        }
    }

    public void printRecipients (String date) { //find and print recipients who have birthdays on given date
        Boolean isAnyone = false;
        findBirthdayRecipients();
        for (BirthdayWishable friend : birthdayWishables) {
            if ((friend.getBirthday().equals(date))) {
                isAnyone = true;
                System.out.println(((Recipient) friend).getTypeOfRecipient() + ": " + ((Recipient) friend).getName() + "," + ((Recipient) friend).getEmailAddress());
            }
        }
        if (isAnyone == false) {
            System.out.println("There is no one or check weather your input date is in correct format!");
        }
    }

    public void printMails(String date) { //print mail that are sent on given date
        for (Mail mail : mailObjects) {
            if ((mail.getDate()).equals(date)) {
                System.out.println(mail.getSubject() + "\n" + mail.getBody());
                System.out.println();
            }
        }
    }

    public void addingRecipient(String recipientDetail) {
        Recipient recipient = createRecipient(recipientDetail);
        if (recipient != null) {
            storeRecipientDetails(recipientDetail);
        }
    }

}

abstract class Recipient {//recipients
    private static int recipientCount = 0;
    private String emailAddress;
    private String name;
    private String typeOfRecipient;

    public Recipient(String name, String emailAddress, String typeOfRecipient) { //create recipients
        this.name = name;
        this.emailAddress = emailAddress;
        this.typeOfRecipient = typeOfRecipient;
        recipientCount++;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getName() {
        return name;
    }

    public String getTypeOfRecipient() {
        return typeOfRecipient;
    }

    public static int getRecipientCount() {
        return recipientCount;
    }
}

class OfficialRecipient extends Recipient { //for official recipients
    private String designation;

    public OfficialRecipient(String name, String emailAddress, String designation, String typeOfRecipient) {
        super(name, emailAddress, typeOfRecipient);
        this.designation = designation;
    }

    public String getDesignation() {
        return designation;
    }
}

class PersonalRecipient extends Recipient implements BirthdayWishable { //for personal recipients
    private String nickName;
    private String birthday;

    public PersonalRecipient(String name,String nickName, String emailAddress, String birthday,String typeofRecipient) {
        super(name, emailAddress,typeofRecipient);
        this.nickName = nickName;
        this.birthday = birthday;
    }

    @Override
    public String createWishBody(String name) {
        return "Hug and love on your Birthday " + name;
    }

    @Override
    public String getBirthday() {
        return birthday;
    }
}

class OfficialCloseRecipients extends OfficialRecipient implements BirthdayWishable { //for close recipients
    private String birthday;

    public OfficialCloseRecipients(String name, String emailAddress, String designation, String birthday, String typeOfRecipient) {
        super(name, emailAddress, designation, typeOfRecipient);
        this.birthday = birthday;
    }

    @Override
    public String createWishBody(String name) {
        return "Happy Birthday " + name;
    }

    @Override
    public String getBirthday() {
        return birthday;
    }
}

class Mail implements Serializable { //for mail objects
    private String subject;
    private String body;
    private String date;
    private Object content;
    private Address[] from;
    private Date receivedDate;

    public Mail(String subject, String body) { //create sent mail object
        this.subject = subject;
        this.body = body;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY/MM/dd");
        date = formatter.format(LocalDate.now());

    }

    public Mail(String subject, Object content, Date receivedDate, Address[] from) { //create received mail
        this.subject = subject;
        this.content = content;
        this.receivedDate = receivedDate;
        this.from = from;
    }

    public Message prepareEmail(Session session, String sender, String emailAddress) { //prepare mail properties
        Message message = new MimeMessage(session);
        try{
            try {
                message.setFrom(new InternetAddress(sender));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
            }catch (AddressException addressException) {
                System.out.println("Invalid e-mail address");
            }
            message.setSubject(subject);
            message.setText(body);
        }catch (MessagingException messagingException) {
            System.out.println("Couldn't make message");
        }
        return message;
    }

    public static void sendMail(Mail mail, String emailAddress) { //send mail process
        Properties properties = new Properties();
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.host","smtp.gmail.com");
        properties.put("mail.smtp.port","587");
        String sender = "oop.assignment1@gmail.com";
        String password = "@oop@1234";

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, password);
            }
        };
        Session session = Session.getInstance(properties, authenticator);

        Message message = mail.prepareEmail(session, sender, emailAddress);
        try {
            Transport.send(message);
        }catch (MessagingException messagingException) {
            System.out.println("Couldn't send email");
        }
    }

    public String getDate() {
        return date;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}

abstract class Observer { // observer that interested in email notification

    private Date time;
    public abstract void update();

    public Date getTime() {
        return time;
    }
    public void setTime(Date time) {
        this.time = time;
    }
}

class EmailStateRecorder extends Observer { //for writing in console

    @Override
    public void update() {
        System.out.println("an email is received at " + getTime());
    }
}

class EmailStatePrinter extends Observer { //for writing to a file


    @Override
    public void update() {
        try (FileWriter fw = new FileWriter("emailState.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter emailState = new PrintWriter(bw);){
            emailState.println("an email is received at " + getTime());
        }
        catch (IOException ioException) {
            System.out.println("Something went wrong(client.txt can not be created), please try again");
        }
    }
}

class MyBlockingQueue { //for implement blocking queue
    private int maxQueueSize;
    private static Queue<Mail> emailQueue = new LinkedList<>();

    public MyBlockingQueue(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public synchronized void enqueue(Mail message) { //for enqueuing mail objects
        while (emailQueue.size() == maxQueueSize) {
            try {
                wait(); //wait until blocking queue has a vacancy
            }catch (InterruptedException interruptedException) {
                System.out.println("InterruptedException");
            }
        }
        emailQueue.add(message);
        notifyAll(); //notifying serializing thread
    }

    public synchronized Mail dequeue() {
        while (emailQueue.size() == 0) {
            try {
                wait(); //wait until blocking queue has a element
            }catch (InterruptedException interruptedException) {
                System.out.println("InterruptedException");
            }
        }
        notifyAll(); //notify the receiving thread
        return emailQueue.remove();
    }

    public Queue<Mail> getEmailQueue() {
        return emailQueue;
    }
}

class Serializer implements Runnable { //for serializing pocess
    private volatile boolean isClosed1;
    private MyBlockingQueue blockingQueue;

    public Serializer(MyBlockingQueue blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    private void serialize(Mail receivedMailObject) { //serialize mail object
        try(FileOutputStream output = new FileOutputStream("serialized.txt",true);
            ObjectOutputStream out = new ObjectOutputStream(output);){
            out.writeObject(receivedMailObject);
        }
        catch (IOException ioException){
            ioException.printStackTrace();
            System.out.println("Something went wrong(can not be serialized), please try again");
        }
    }

    @Override
    public void run() {
        while(true) {
            Mail message = blockingQueue.dequeue(); //dequeue the mail objects
            serialize(message); //serialize mail objects
        }
    }
}

class EmailReceiver implements Runnable { //for email receiving process
    private volatile boolean isClosed;
    private ArrayList<Observer> notificationObservers;
    private MyBlockingQueue blockingQueue;

    public EmailReceiver(MyBlockingQueue blockingQueue) {
        this.blockingQueue = blockingQueue;
        isClosed = false;
        notificationObservers = new ArrayList<>();
        notificationObservers.add(new EmailStatePrinter());
        notificationObservers.add(new EmailStateRecorder());
    }

    private void receiveMail(MyBlockingQueue queue) {
        Properties prop = new Properties();
        prop.setProperty("mail.store.protocol", "imaps");
        Session emailSession = Session.getDefaultInstance(prop);
        try {
            Store emailStore = emailSession.getStore("imaps");
            emailStore.connect("imap.gmail.com", "oop.assignment1@gmail.com", "@oop@1234");
            Folder emailFolder = emailStore.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);
            if (emailFolder.getUnreadMessageCount() > 0) {
                Message messages[] = emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                for (int i = 0; i < messages.length; i++) {
                    Message message = messages[i];
                    notifyAllObservers(message.getReceivedDate());  //notify observers
                    try {
                        //enqueue the mail object to rhe blocking queue
                        queue.enqueue(new Mail(message.getSubject(), message.getContent().toString(), message.getReceivedDate(), message.getFrom()));

                    } catch (MessagingException messagingException) {
                        System.out.println("Messeging exception raised on receiving process");
                    } catch (IOException ioException) {
                        System.out.println("Io exception raised on receiving process");
                    }
                    message.setFlags(new Flags(Flags.Flag.SEEN), true);
                }
            }
            emailFolder.close(false);
            emailStore.close();
        } catch (MessagingException messagingException) {
            System.out.println("Messaging Exception raised");
        }


    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    } //set isClosed state when we want to exit from the system

    @Override
    public void run() {
        while (!isClosed) {
            receiveMail(blockingQueue);
        }
        while (blockingQueue.getEmailQueue().size() != 0) {} //waiting for serializing thread to complete serializing process
        System.exit(0); //exit from system

    }

    private void notifyAllObservers(Date time) { //update all observers when mail is received
        for (Observer observer : notificationObservers) {
            observer.setTime(time);
            observer.update();
        }
    }
}

interface BirthdayWishable { //all the recipients have not birthdays given
    String createWishBody(String name);
    String getBirthday();
}
