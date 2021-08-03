import database.MyDatabase;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static database.ConstantsAndData.*;

class AnimeBot extends TelegramLongPollingBot {
    DeleteMessage deleteMessage = new DeleteMessage();
    private static AnimeBot bot;
    private static SendAnime sendAnime;
    private static MyDatabase database;
    private static Commands commands;
    private static CallBack callBack;
    private static User user;

    private final Logger logger = Logger.getLogger(AnimeBot.class);

    private String lastCommand = null;

    private static void initialization() {
        database = new MyDatabase();

        database.createDatabaseIfNotExistsAndConnect(DB_NAME.toLowerCase(), USER_NAME, PASSWORD);

        user = new User();
        commands = new Commands(bot);
        sendAnime = new SendAnime(bot);
        callBack = new CallBack(bot, sendAnime);
    }

    public static void main(String[] args) {

        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            bot = new AnimeBot();
            telegramBotsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        initialization();
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }

    @Override
    public String getBotUsername() {
        return "AnimeBot";
    }

    @Override
    public String getBotToken() {
        return "here_must_be_speacial_token";
    }

    @Override
    public void onUpdateReceived(Update update) {

        // if user pressed button
        if (update.hasCallbackQuery()) {
            String text = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            int callbackId = update.getCallbackQuery().getMessage().getMessageId();

            Message newMessage = new Message();
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId).setReplyToMessageId(newMessage.getMessageId());

            if (text.matches("genre = .*")) {
                callBack.getGenreFromCallbackAndSendAnime(text, sendMessage, update, database);
                deleteMessage(chatId, callbackId);

            } else if (text.matches("listNameToOpen = .*")) {
                callBack.getListFromCallbackAndOpenIt(text, sendMessage, chatId, database);
                deleteMessage(chatId, callbackId);

            } else if (text.matches("listNameToDelete = .*")) {
                callBack.getListFromCallbackAndDeleteIt(text, sendMessage, chatId, database);
                deleteMessage(chatId, callbackId);

                //preparing for adding anime, we still don't know list
            } else if (text.matches("addInList .*")) {
                callBack.getAnimeFromCallbackAndSendIt(text, commands, chatId, database);

                //adding anime in list, we know list
            } else if (text.matches(".* <-> .*")) {
                callBack.getListFromCallBackAndAddAnime(text, sendMessage, database);
                deleteMessage(chatId, callbackId);

                //we don't know userMark, show buttons
            } else if (text.matches("markAnime .*")) {
                callBack.showMarks(text, chatId);

                // we got userMark
            } else if (text.matches(".* markOfAnImE = .*")) {
                callBack.addUserMark(text, sendMessage, chatId, database);
                deleteMessage(chatId, callbackId);
            }
        }

        Message message = update.getMessage();

        if (message != null && message.hasText()) {
            String text = message.getText();

            sendAnime.setUserName(user.getName(message, false)); // for debug
            commands.setUserName(user.getName(message, false)); // for debug
            callBack.setUserName(user.getName(message, false)); // for debug

            if (text.startsWith("/help")) {
                commands.help(message);

            } else if (text.startsWith("/start")) {
                commands.start(message);
                user.addUserIfNeed(message, database);

            } else if (text.startsWith("/genre")) {
                try {
                    execute(commands.showGenres(message, database));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            } else if (text.startsWith("/random")) {
                sendAnime.sendRandomAnime(message, database);

            } else if (text.startsWith("/lists")) {
                commands.listCommands(message, database);

            } else if (text.matches("/createlist")) {
                setLastCommand(text);
                sendMsg(new SendMessage(), message, "Введите имя для нового списка");

            } else if (text.matches("/deletelist")) { // show all list and choose one of them
                try {
                    execute(commands.showLists(message.getChatId(), "listNameToDelete = ",
                            "Выберите список, который хотите удалить", database));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            } else if (text.toLowerCase().equals("/deleteanime")) {
                String listName = lastCommand;
                setLastCommand("/deleteAnime from " + listName);
                sendMsg(new SendMessage(), message, "Чтобы удалить аниме введите его номер в списке");

            } else if (lastCommand != null && lastCommand.equals("/createlist")) {
                commands.createList(text, message, database);

            } else if (lastCommand != null && lastCommand.matches("/deleteAnime from .*")) {
                commands.deleteAnimeFromList(text, message, database);

            } else {
                logger.warn("user " + message.getChatId() + " entered unknown command = " + text);
                sendMsg(new SendMessage(), message, "Я не знаю такой команды");
                commands.help(message);
            }
        }
    }

    private void deleteMessage(long chatId, int messageId) {
        deleteMessage.setChatId(chatId); // user choose genre, removing buttons
        deleteMessage.setMessageId(messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(SendMessage sendMessage, Message message, String text) {
        sendMessage.enableMarkdown(true).setText(text);
        sendMessage.setChatId(message.getChatId().toString());
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("message for user " + message.getChatId() + " wasn't send");
            e.printStackTrace();
        }
    }
}
