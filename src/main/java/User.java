import database.MyDatabase;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.meta.api.objects.Message;

public class User {

    private final static Logger logger = Logger.getLogger(User.class);

    void addUserIfNeed(Message message, MyDatabase database) {

        logger.info("\n-------------------------------------");

        logger.info("chatId = " + message.getChatId());
        logger.info("-------------------------------------\n");

        database.addUserIfNeed(String.valueOf(getName(message, true)), message.getChatId());
    }

    StringBuilder getName(Message message, boolean showInfo){
        String firstName = message.getChat().getFirstName();
        String lastName = message.getChat().getLastName();

        StringBuilder userName = new StringBuilder();
        if (firstName != null) {
            userName.append(firstName);
            if (showInfo) logger.info("firstName = " + firstName);
        }
        if (lastName != null) {
            userName.append(" ").append(lastName);
            if (showInfo) logger.info("lastName = " + lastName);
        }
        return userName;
    }
}
